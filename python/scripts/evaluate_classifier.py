"""Comprehensive evaluation script for PrioSort ML components."""

import os
import sys
import json
import pandas as pd

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from priosort_ml.classifier import NotificationClassifier
from priosort_ml.preprocessing import TextPreprocessor
from priosort_ml.evaluation import ModelEvaluator


def main():
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    test_path = os.path.join(root_dir, "data", "test_notifications.csv")
    model_path = os.path.join(root_dir, "models", "category_classifier.joblib")
    report_path = os.path.join(root_dir, "data", "evaluation_report.json")

    if not os.path.exists(model_path):
        print(f"Error: Model not found at {model_path}. Run train_classifier.py first.")
        sys.exit(1)

    test_df = pd.read_csv(test_path)
    classifier = NotificationClassifier.load(model_path)
    preprocessor = TextPreprocessor()

    texts = test_df["full_text"].fillna("").tolist()
    y_true = test_df["category"].tolist()
    true_otps = test_df["is_otp"].tolist()

    # 1. Evaluate Category Classifier
    predictions = classifier.predict_batch(texts)
    y_pred = [p[0] for p in predictions]
    cat_eval = ModelEvaluator.evaluate_category_classifier(y_true, y_pred)

    # 2. Evaluate OTP Detection
    pred_otps = []
    for text in texts:
        is_otp, _, _ = preprocessor.detect_otp(text)
        pred_otps.append(is_otp)

    otp_eval = ModelEvaluator.evaluate_otp_detector(true_otps, pred_otps)

    # 3. Latency Benchmarking
    latency_eval = ModelEvaluator.benchmark_latency(
        lambda t: (preprocessor.extract_features(t, ""), classifier.predict(t)),
        texts[:20],
        runs=50
    )

    full_report = {
        "dataset_size": len(test_df),
        "category_classification": cat_eval,
        "otp_detection": otp_eval,
        "latency_benchmarks_ms": latency_eval
    }

    with open(report_path, "w") as f:
        json.dump(full_report, f, indent=2)

    print("=" * 60)
    print("           PRIOSORT EVALUATION REPORT")
    print("=" * 60)
    print(f"Test Set Size:       {len(test_df)} samples")
    print(f"Category Accuracy:   {cat_eval['accuracy'] * 100:.2f}%")
    print(f"Category Macro-F1:   {cat_eval['macro_f1']:.4f}")
    print("-" * 60)
    print(f"OTP Precision:       {otp_eval['otp_precision'] * 100:.2f}%")
    print(f"OTP Recall:          {otp_eval['otp_recall'] * 100:.2f}%")
    print(f"OTP False Pos Rate:  {otp_eval['false_positive_rate'] * 100:.2f}%")
    print(f"OTP False Positives: {otp_eval['false_positives']}")
    print("-" * 60)
    print(f"Mean Latency:        {latency_eval['mean_ms']:.3f} ms")
    print(f"Median Latency:      {latency_eval['median_ms']:.3f} ms")
    print(f"P95 Latency:         {latency_eval['p95_ms']:.3f} ms")
    print("=" * 60)
    print(f"Full report saved to: {report_path}")


if __name__ == "__main__":
    main()
