"""Notification Category Classifier using TF-IDF and Logistic Regression."""

import os
from typing import List, Dict, Any, Tuple, Optional
import joblib
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.pipeline import Pipeline


class NotificationClassifier:
    """Lightweight text classifier for categorizing notifications with confidence scoring."""

    CATEGORIES = [
        "SECURITY_AND_VERIFICATION",
        "FINANCIAL_AND_TRANSACTIONAL",
        "WORK_AND_EDUCATION",
        "PERSONAL_AND_SOCIAL",
        "SYSTEM_AND_DEVICE",
        "PROMOTIONAL",
        "GENERAL_INFORMATIONAL",
        "UNKNOWN"
    ]

    def __init__(self, confidence_threshold: float = 0.40):
        self.confidence_threshold = confidence_threshold
        self.pipeline: Optional[Pipeline] = None
        self.is_trained: bool = False

    def build_pipeline(self) -> Pipeline:
        """Constructs an scikit-learn TF-IDF + Logistic Regression pipeline."""
        return Pipeline([
            ("tfidf", TfidfVectorizer(
                ngram_range=(1, 2),
                max_features=2500,
                sublinear_tf=True,
                lowercase=True,
                strip_accents="unicode"
            )),
            ("clf", LogisticRegression(
                C=2.0,
                max_iter=500,
                class_weight="balanced",
                random_state=42
            ))
        ])

    def train(self, texts: List[str], labels: List[str]):
        """Trains the classifier on labeled notification texts."""
        self.pipeline = self.build_pipeline()
        self.pipeline.fit(texts, labels)
        self.is_trained = True

    def predict(self, text: str) -> Tuple[str, float, Dict[str, float]]:
        """Predicts the category of a notification with confidence scoring.

        Returns:
            Tuple of (predicted_category, confidence_score, all_probabilities)
        """
        if not self.is_trained or self.pipeline is None:
            raise RuntimeError("Classifier has not been trained or loaded.")

        if not text or not text.strip():
            return "UNKNOWN", 0.0, {c: 0.0 for c in self.CATEGORIES}

        probs = self.pipeline.predict_proba([text])[0]
        classes = self.pipeline.classes_

        prob_dict = {classes[i]: float(probs[i]) for i in range(len(classes))}
        top_idx = int(np.argmax(probs))
        top_class = str(classes[top_idx])
        top_prob = float(probs[top_idx])

        # Flag uncertain predictions
        if top_prob < self.confidence_threshold:
            return "UNKNOWN", top_prob, prob_dict

        return top_class, top_prob, prob_dict

    def predict_batch(self, texts: List[str]) -> List[Tuple[str, float]]:
        """Batched prediction for efficiency."""
        if not self.is_trained or self.pipeline is None:
            raise RuntimeError("Classifier has not been trained or loaded.")

        if not texts:
            return []

        probs = self.pipeline.predict_proba(texts)
        classes = self.pipeline.classes_
        results = []

        for row in probs:
            top_idx = int(np.argmax(row))
            top_class = str(classes[top_idx])
            top_prob = float(row[top_idx])
            if top_prob < self.confidence_threshold:
                results.append(("UNKNOWN", top_prob))
            else:
                results.append((top_class, top_prob))

        return results

    def save(self, filepath: str):
        """Saves trained pipeline to disk."""
        os.makedirs(os.path.dirname(filepath), exist_ok=True)
        joblib.dump({
            "pipeline": self.pipeline,
            "threshold": self.confidence_threshold
        }, filepath)

    @classmethod
    def load(cls, filepath: str) -> "NotificationClassifier":
        """Loads trained pipeline from disk."""
        data = joblib.load(filepath)
        clf = cls(confidence_threshold=data.get("threshold", 0.40))
        clf.pipeline = data["pipeline"]
        clf.is_trained = True
        return clf
