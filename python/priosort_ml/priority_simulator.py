"""Priority scoring and sorting simulator matching PrioSort Android specifications."""

import math
import time
from typing import Dict, Any, List, Optional, Tuple
from dataclasses import dataclass, field


@dataclass
class PriorityDecision:
    """Holds the complete, explainable priority decision."""
    base_score: float
    urgency_score: float
    action_score: float
    deadline_score: float
    importance_score: float
    recency_score: float
    priority_level: str  # "P0", "P1", "P2", "P3", "P4"
    priority_name: str   # "Critical", "High", "Medium", "Low", "Minimal"
    priority_rank: int   # 5 down to 1
    is_overridden: bool
    override_reason: Optional[str]
    explanations: List[str]


class PrioritySimulator:
    """Calculates explainable priority score and assigns priority levels with Focus Mode and Custom Rules support."""

    PRIORITY_RANKS = {
        "P0": 5,
        "P1": 4,
        "P2": 3,
        "P3": 2,
        "P4": 1
    }

    PRIORITY_NAMES = {
        "P0": "Critical",
        "P1": "High",
        "P2": "Medium",
        "P3": "Low",
        "P4": "Minimal"
    }

    def __init__(self, tau_hours: float = 24.0):
        self.tau_hours = tau_hours

    def apply_focus_mode(self, priority_level: str, category: str, focus_mode: str) -> Tuple[str, Optional[str]]:
        """Applies contextual Focus Mode modifiers (WORK, SLEEP, PERSONAL, OFF)."""
        if focus_mode == "OFF" or priority_level == "P0":
            # P0 Critical OTPs/Fraud are NEVER suppressed in any mode
            return priority_level, None

        if focus_mode == "WORK":
            if category == "WORK_AND_EDUCATION":
                return "P1", "Work Mode active: Elevated work & education alert to High (P1)"
            elif category == "PROMOTIONAL":
                return "P4", "Work Mode active: Promotional advertisement suppressed to Minimal (P4)"
            elif category == "PERSONAL_AND_SOCIAL":
                return "P3", "Work Mode active: Casual messaging demoted during working hours"

        elif focus_mode == "SLEEP":
            return "P3", "Sleep / DND Mode active: Suppressed to prevent night-time disturbance"

        elif focus_mode == "PERSONAL":
            if category == "PERSONAL_AND_SOCIAL":
                return "P1", "Personal Mode active: Elevated social communication to High (P1)"
            elif category == "WORK_AND_EDUCATION":
                return "P3", "Personal Mode active: Work alert demoted during leisure hours"

        return priority_level, None

    def calculate_recency(self, received_timestamp_ms: int, current_timestamp_ms: Optional[int] = None) -> float:
        """Calculates recency score using exponential decay: R = 100 * exp(-age_hours / tau)."""
        if current_timestamp_ms is None:
            current_timestamp_ms = int(time.time() * 1000)

        age_ms = max(0, current_timestamp_ms - received_timestamp_ms)
        age_hours = age_ms / (1000.0 * 3600.0)
        return float(100.0 * math.exp(-age_hours / self.tau_hours))

    def evaluate(
        self,
        features: Dict[str, Any],
        predicted_category: str,
        category_confidence: float = 1.0,
        user_app_importance: float = 50.0,
        deadline_hours_remaining: Optional[float] = None,
        received_timestamp_ms: Optional[int] = None,
        current_timestamp_ms: Optional[int] = None,
        focus_mode: str = "OFF",
        custom_rules: Optional[List[Dict[str, Any]]] = None,
        is_vip: bool = False,
        is_out_for_delivery: bool = False
    ) -> PriorityDecision:
        """Evaluates a notification according to Section 12 scoring formula and Section 8/9 rules."""
        explanations: List[str] = []

        # 1. Urgency score (0 - 100)
        if features.get("is_otp", False):
            urgency = 100.0
            explanations.append("Time-sensitive verification code detected (Urgency: 100)")
        elif features.get("is_fraud_alert", False):
            urgency = 95.0
            explanations.append("Security / fraud alert detected (Urgency: 95)")
        elif features.get("is_payment_fail", False):
            urgency = 85.0
            explanations.append("Financial payment failure requires attention (Urgency: 85)")
        elif features.get("has_deadline", False):
            urgency = 80.0
            explanations.append("Explicit deadline detected (Urgency: 80)")
        elif predicted_category == "FINANCIAL_AND_TRANSACTIONAL":
            urgency = 65.0
            explanations.append("Financial transaction notification (Urgency: 65)")
        elif predicted_category == "WORK_AND_EDUCATION":
            urgency = 60.0
            explanations.append("Work/education notification (Urgency: 60)")
        elif predicted_category == "PERSONAL_AND_SOCIAL":
            urgency = 45.0
            explanations.append("Personal/social communication (Urgency: 45)")
        elif predicted_category == "PROMOTIONAL" or features.get("is_promo", False):
            urgency = 10.0
            explanations.append("Promotional advertisement detected (Urgency: 10)")
        else:
            urgency = 30.0
            explanations.append("Routine informational content (Urgency: 30)")

        # 2. Action requirement (0, 50, 100)
        if features.get("is_otp", False):
            action = 100.0
            explanations.append("Requires prompt code entry (Action: 100)")
        elif features.get("is_fraud_alert", False) or features.get("is_payment_fail", False):
            action = 100.0
            explanations.append("Immediate user intervention required (Action: 100)")
        elif features.get("has_action", False):
            action = 75.0
            explanations.append("Action required keywords detected (Action: 75)")
        elif features.get("has_deadline", False):
            action = 50.0
            explanations.append("Potential submission/task action required (Action: 50)")
        else:
            action = 0.0

        # 3. Deadline proximity (0, 20, 40, 70, 90, 100)
        if deadline_hours_remaining is not None:
            if deadline_hours_remaining <= 1.0:
                deadline = 100.0
                explanations.append(f"Imminent deadline in {deadline_hours_remaining:.1f}h (Deadline: 100)")
            elif deadline_hours_remaining <= 24.0:
                deadline = 90.0
                explanations.append(f"Deadline in less than 24h ({deadline_hours_remaining:.1f}h) (Deadline: 90)")
            elif deadline_hours_remaining <= 48.0:
                deadline = 70.0
                explanations.append(f"Deadline within 2 days ({deadline_hours_remaining:.1f}h) (Deadline: 70)")
            elif deadline_hours_remaining <= 168.0:
                deadline = 40.0
                explanations.append("Deadline between 3 and 7 days (Deadline: 40)")
            else:
                deadline = 20.0
                explanations.append("Deadline more than 7 days away (Deadline: 20)")
        elif features.get("has_deadline", False):
            # Deadline keyword present without exact parsed hours
            deadline = 70.0
            explanations.append(f"Deadline expression detected: '{features.get('deadline_text')}' (Deadline: 70)")
        else:
            deadline = 0.0

        # 4. User importance (0 - 100)
        importance = max(0.0, min(100.0, user_app_importance))

        # 5. Recency (0 - 100)
        if received_timestamp_ms is not None:
            recency = self.calculate_recency(received_timestamp_ms, current_timestamp_ms)
        else:
            recency = 100.0  # Freshly arrived

        # Scoring formula: S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R
        base_score = (0.30 * urgency) + (0.25 * action) + (0.25 * deadline) + (0.15 * importance) + (0.05 * recency)
        base_score = round(base_score, 2)

        # Standard score-to-priority mapping
        if base_score >= 80.0:
            mapped_level = "P0"
        elif base_score >= 60.0:
            mapped_level = "P1"
        elif base_score >= 35.0:
            mapped_level = "P2"
        elif base_score >= 15.0:
            mapped_level = "P3"
        else:
            mapped_level = "P4"

        # Apply Rule-Based Overrides
        final_level = mapped_level
        is_overridden = False
        override_reason = None

        if features.get("is_otp", False):
            final_level = "P0"
            is_overridden = True
            override_reason = "Confidently identified OTP / verification code promoted to Critical (P0)"
            explanations.append(override_reason)
        elif features.get("is_fraud_alert", False):
            final_level = "P0"
            is_overridden = True
            override_reason = "Credible fraud / account security alert promoted to Critical (P0)"
            explanations.append(override_reason)
        elif features.get("is_payment_fail", False):
            if final_level not in ["P0", "P1"]:
                final_level = "P1"
                is_overridden = True
                override_reason = "Payment failure promoted to High (P1)"
                explanations.append(override_reason)
        elif features.get("is_important_transaction", False):
            if final_level not in ["P0", "P1"]:
                final_level = "P1"
                is_overridden = True
                override_reason = "Important financial transaction alert promoted to High (P1)"
                explanations.append(override_reason)
        elif (predicted_category == "PROMOTIONAL" or features.get("is_promo", False)) and user_app_importance <= 50.0:
            final_level = "P4"
            is_overridden = True
            override_reason = "Promotional advertisement demoted to Minimal (P4)"
            explanations.append(override_reason)
        elif features.get("has_deadline", False) and deadline >= 70.0:
            if final_level in ["P2", "P3", "P4"]:
                final_level = "P1"
                is_overridden = True
                override_reason = "Imminent work/education submission deadline promoted to High (P1)"
                explanations.append(override_reason)
        elif features.get("has_transaction", False) and final_level in ["P3", "P4"]:
            final_level = "P2"
            is_overridden = True
            override_reason = "Routine financial transaction receipt assigned to Medium (P2)"
            explanations.append(override_reason)
        elif predicted_category == "PERSONAL_AND_SOCIAL" and final_level in ["P3", "P4"]:
            final_level = "P2"
            is_overridden = True
            override_reason = "Personal messaging notification assigned to Medium (P2)"
            explanations.append(override_reason)

        # Custom rules override check
        if custom_rules:
            text_lower = features.get("normalized_text", "").lower()
            for rule in custom_rules:
                kw = rule.get("keyword", "").lower()
                if kw and kw in text_lower:
                    override_prio = rule.get("priority")
                    if override_prio in self.PRIORITY_RANKS:
                        final_level = override_prio
                        is_overridden = True
                        override_reason = f"User Custom Rule matched: '{kw}' -> {final_level}"
                        explanations.append(override_reason)
                        break

        # Apply Focus Mode adjustments
        if focus_mode != "OFF":
            adj_level, focus_reason = self.apply_focus_mode(final_level, predicted_category, focus_mode)
            if focus_reason:
                final_level = adj_level
                is_overridden = True
                override_reason = focus_reason
                explanations.append(focus_reason)

        # VIP contact promotion
        if is_vip and final_level in ["P2", "P3", "P4"]:
            final_level = "P1"
            is_overridden = True
            override_reason = "VIP Contact detected: elevated to High (P1)"
            explanations.append(override_reason)

        # Out for delivery promotion
        if is_out_for_delivery and final_level in ["P2", "P3", "P4"]:
            final_level = "P1"
            is_overridden = True
            override_reason = "Package is out for delivery today: elevated to High (P1)"
            explanations.append(override_reason)

        rank = self.PRIORITY_RANKS[final_level]
        name = self.PRIORITY_NAMES[final_level]

        return PriorityDecision(
            base_score=base_score,
            urgency_score=round(urgency, 2),
            action_score=round(action, 2),
            deadline_score=round(deadline, 2),
            importance_score=round(importance, 2),
            recency_score=round(recency, 2),
            priority_level=final_level,
            priority_name=name,
            priority_rank=rank,
            is_overridden=is_overridden,
            override_reason=override_reason,
            explanations=explanations
        )


def sort_notifications(records: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    """Sorts notifications according to Section 13 rules:

    1. User-pinned descending
    2. Effective priority rank descending (P0=5 down to P4=1)
    3. Deadline score / urgency descending
    4. Received timestamp descending (newest first)
    5. Stable identifier ascending
    """
    def sort_key(item: Dict[str, Any]):
        is_pinned = 1 if item.get("is_pinned", False) else 0
        rank = item.get("priority_rank", 1)
        deadline = item.get("deadline_score", 0.0)
        timestamp = item.get("received_at", 0)
        stable_id = str(item.get("id", ""))

        # Multi-key sort:
        # Pinned (desc: negate for int), Rank (desc: negate), Deadline (desc: negate),
        # Timestamp (desc: negate), Stable ID (asc: regular string)
        return (-is_pinned, -rank, -deadline, -timestamp, stable_id)

    return sorted(records, key=sort_key)
