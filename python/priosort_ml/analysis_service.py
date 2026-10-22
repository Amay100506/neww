"""Local, privacy-conscious notification analysis facade."""

from typing import Any, Dict, List, Optional

from .classifier import NotificationClassifier
from .preprocessing import TextPreprocessor
from .priority_simulator import PrioritySimulator


class NotificationAnalysisService:
    """Combines category inference, deterministic features, and priority scoring.

    This service is deliberately transport-agnostic and performs no network or
    persistence operations. Callers can use it from the dashboard, a CLI, or a
    future local API without duplicating the analysis pipeline.
    """

    def __init__(
        self,
        classifier: Optional[NotificationClassifier] = None,
        preprocessor: Optional[TextPreprocessor] = None,
        simulator: Optional[PrioritySimulator] = None,
    ) -> None:
        self.classifier = classifier
        self.preprocessor = preprocessor or TextPreprocessor()
        self.simulator = simulator or PrioritySimulator()

    @classmethod
    def from_model(cls, model_path: str) -> "NotificationAnalysisService":
        """Load a trained classifier artifact and construct the service."""
        return cls(classifier=NotificationClassifier.load(model_path))

    def analyze(
        self,
        title: Optional[str],
        body: Optional[str],
        *,
        package_name: str = "",
        user_app_importance: float = 50.0,
        deadline_hours_remaining: Optional[float] = None,
        received_timestamp_ms: Optional[int] = None,
        current_timestamp_ms: Optional[int] = None,
        focus_mode: str = "OFF",
        custom_rules: Optional[List[Dict[str, Any]]] = None,
        is_vip: bool = False,
        is_out_for_delivery: bool = False,
    ) -> Dict[str, Any]:
        """Analyze one notification and return a JSON-serializable result.

        Notification text and extracted OTP values are used only internally;
        neither is included in the result. Category probabilities are included
        when a trained model is available.
        """
        features = self.preprocessor.extract_features(title, body, package_name)
        category, confidence, probabilities, source = self._classify(features)

        decision = self.simulator.evaluate(
            features=features,
            predicted_category=category,
            category_confidence=confidence,
            user_app_importance=user_app_importance,
            deadline_hours_remaining=deadline_hours_remaining,
            received_timestamp_ms=received_timestamp_ms,
            current_timestamp_ms=current_timestamp_ms,
            focus_mode=focus_mode,
            custom_rules=custom_rules,
            is_vip=is_vip,
            is_out_for_delivery=is_out_for_delivery,
        )

        return {
            "category": category,
            "category_confidence": round(float(confidence), 4),
            "category_source": source,
            "category_probabilities": probabilities,
            "priority": {
                "level": decision.priority_level,
                "name": decision.priority_name,
                "rank": decision.priority_rank,
                "base_score": decision.base_score,
                "overridden": decision.is_overridden,
                "override_reason": decision.override_reason,
                "score_breakdown": {
                    "urgency": decision.urgency_score,
                    "action": decision.action_score,
                    "deadline": decision.deadline_score,
                    "importance": decision.importance_score,
                    "recency": decision.recency_score,
                },
                "explanations": decision.explanations,
            },
            "signals": {
                "is_otp": bool(features["is_otp"]),
                "is_fraud_alert": bool(features["is_fraud_alert"]),
                "is_payment_fail": bool(features["is_payment_fail"]),
                "is_promotional": bool(features["is_promo"]),
                "has_deadline": bool(features["has_deadline"]),
                "has_transaction": bool(features["has_transaction"]),
                "is_important_transaction": bool(features["is_important_transaction"]),
                "has_action": bool(features["has_action"]),
            },
        }

    def _classify(self, features: Dict[str, Any]):
        text = features["normalized_text"]
        probabilities = {}
        if self.classifier is not None:
            category, confidence, probabilities = self.classifier.predict(text)
            if category != "UNKNOWN":
                return category, confidence, probabilities, "model"

        # Deterministic fallback keeps inference useful before training and for
        # low-confidence model output, while preferring explicit signals.
        if features["is_otp"]:
            category, confidence = "SECURITY_AND_VERIFICATION", 0.99
        elif features["is_fraud_alert"] or features["is_payment_fail"]:
            category, confidence = "FINANCIAL_AND_TRANSACTIONAL", 0.95
        elif features["has_deadline"]:
            category, confidence = "WORK_AND_EDUCATION", 0.90
        elif features["has_transaction"]:
            category, confidence = "FINANCIAL_AND_TRANSACTIONAL", 0.90
        elif features["is_promo"]:
            category, confidence = "PROMOTIONAL", 0.90
        elif features.get("package_category_hint"):
            category, confidence = features["package_category_hint"], 0.70
        else:
            category, confidence = "GENERAL_INFORMATIONAL", 0.50

        source = "rules_fallback" if self.classifier is not None else "rules"
        return category, confidence, probabilities, source
