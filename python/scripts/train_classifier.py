"""Trains the NotificationCategoryClassifier and saves the model artifact."""

import os
import sys
import pandas as pd

# Add python directory to sys.path
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from priosort_ml.classifier import NotificationClassifier
from priosort_ml.evaluation import ModelEvaluator


def main():
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
    train_path = os.path.join(root_dir, "data", "train_notifications.csv")
    test_path = os.path.join(root_dir, "data", "test_notifications.csv")
    model_path = os.path.join(root_dir, "models", "category_classifier.joblib")

    if not os.path.exists(train_path):
        print(f"Error: Train data not found at {train_path}. Run generate_dataset.py first.")
        sys.exit(1)

    train_df = pd.read_csv(train_path)
    test_df = pd.read_csv(test_path)

    print(f"Loaded {len(train_df)} training samples and {len(test_df)} test samples.")

    # Prepare features
    X_train = train_df["full_text"].fillna("").tolist()
    y_train = train_df["category"].tolist()

    X_test = test_df["full_text"].fillna("").tolist()
    y_test = test_df["category"].tolist()

    # Initialize and train
    classifier = NotificationClassifier(confidence_threshold=0.35)
    print("Training TF-IDF + Logistic Regression category classifier...")
    classifier.train(X_train, y_train)

    # Save model
    classifier.save(model_path)
    print(f"Model saved to: {model_path}")

    # Evaluate on test set
    predictions = classifier.predict_batch(X_test)
    y_pred = [p[0] for p in predictions]

    eval_results = ModelEvaluator.evaluate_category_classifier(y_test, y_pred)
    print("\n" + "=" * 50)
    print("TEST SET EVALUATION RESULTS")
    print("=" * 50)
    print(f"Accuracy:         {eval_results['accuracy']:.4f}")
    print(f"Macro Precision:  {eval_results['macro_precision']:.4f}")
    print(f"Macro Recall:     {eval_results['macro_recall']:.4f}")
    print(f"Macro F1-Score:   {eval_results['macro_f1']:.4f}")
    print("=" * 50)


if __name__ == "__main__":
    main()
