"""Evaluation module for PrioSort ML models and priority engines."""

import time
from typing import List, Dict, Any, Tuple, Optional
import numpy as np
from sklearn.metrics import classification_report, confusion_matrix, precision_recall_fscore_support


class ModelEvaluator:
    """Evaluates classification accuracy, OTP precision/recall, and latency."""

    @staticmethod
    def evaluate_category_classifier(
        y_true: List[str],
        y_pred: List[str],
        labels: Optional[List[str]] = None
    ) -> Dict[str, Any]:
        """Calculates classification report, macro-F1, per-class precision and recall."""
        if labels is None:
            labels = sorted(list(set(y_true) | set(y_pred)))

        precision, recall, f1, support = precision_recall_fscore_support(
            y_true, y_pred, labels=labels, zero_division=0
        )

        per_class = {}
        for i, lbl in enumerate(labels):
            per_class[lbl] = {
                "precision": float(precision[i]),
                "recall": float(recall[i]),
                "f1_score": float(f1[i]),
                "support": int(support[i])
            }

        macro_p, macro_r, macro_f1, _ = precision_recall_fscore_support(
            y_true, y_pred, average="macro", zero_division=0
        )

        acc = float(np.mean([yt == yp for yt, yp in zip(y_true, y_pred)]))
        cm = confusion_matrix(y_true, y_pred, labels=labels).tolist()

        return {
            "accuracy": round(acc, 4),
            "macro_precision": round(float(macro_p), 4),
            "macro_recall": round(float(macro_r), 4),
            "macro_f1": round(float(macro_f1), 4),
            "per_class": per_class,
            "confusion_matrix": cm,
            "labels": labels
        }

    @staticmethod
    def evaluate_otp_detector(
        true_is_otp: List[bool],
        pred_is_otp: List[bool]
    ) -> Dict[str, Any]:
        """Evaluates OTP detection precision, recall, false positive rate, and false alarms."""
        tp = sum(1 for yt, yp in zip(true_is_otp, pred_is_otp) if yt and yp)
        fp = sum(1 for yt, yp in zip(true_is_otp, pred_is_otp) if not yt and yp)
        fn = sum(1 for yt, yp in zip(true_is_otp, pred_is_otp) if yt and not yp)
        tn = sum(1 for yt, yp in zip(true_is_otp, pred_is_otp) if not yt and not yp)

        precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        f1 = 2 * precision * recall / (precision + recall) if (precision + recall) > 0 else 0.0
        fpr = fp / (fp + tn) if (fp + tn) > 0 else 0.0

        return {
            "otp_precision": round(precision, 4),
            "otp_recall": round(recall, 4),
            "otp_f1": round(f1, 4),
            "false_positive_rate": round(fpr, 4),
            "true_positives": tp,
            "false_positives": fp,
            "false_negatives": fn,
            "true_negatives": tn
        }

    @staticmethod
    def benchmark_latency(func, sample_inputs: List[Any], runs: int = 100) -> Dict[str, float]:
        """Benchmarks inference/scoring latency in milliseconds."""
        latencies = []
        for _ in range(runs):
            for item in sample_inputs:
                start = time.perf_counter()
                func(item)
                elapsed_ms = (time.perf_counter() - start) * 1000.0
                latencies.append(elapsed_ms)

        latencies_arr = np.array(latencies)
        return {
            "mean_ms": round(float(np.mean(latencies_arr)), 3),
            "median_ms": round(float(np.median(latencies_arr)), 3),
            "p95_ms": round(float(np.percentile(latencies_arr, 95)), 3),
            "p99_ms": round(float(np.percentile(latencies_arr, 99)), 3)
        }
