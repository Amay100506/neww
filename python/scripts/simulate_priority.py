"""Simulates the Executive Summary notification scenario (Section 1.3).

Demonstrates the core principle:
'Importance should determine presentation order, not arrival time alone.'
"""

import os
import sys
import time

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from priosort_ml.preprocessing import TextPreprocessor
from priosort_ml.classifier import NotificationClassifier
from priosort_ml.priority_simulator import PrioritySimulator, sort_notifications


def run_executive_summary_simulation():
    preprocessor = TextPreprocessor()
    simulator = PrioritySimulator()

    # Load model if available, else rule-based fallback
    model_path = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "models", "category_classifier.joblib"))
    classifier = None
    if os.path.exists(model_path):
        classifier = NotificationClassifier.load(model_path)

    # Base timestamp: 10 minutes ago
    base_time = int(time.time() * 1000) - (10 * 60 * 1000)

    # Define the 5 arrival-ordered notifications exactly matching Section 1.3
    notifications_input = [
        {
            "id": "notif-001",
            "arrival_order": 1,
            "received_at": base_time + (1 * 60 * 1000),  # Arrived 1st (earliest)
            "package_name": "com.flipkart.android",
            "app_name": "Flipkart",
            "title": "Mega Sale is LIVE!",
            "body": "Flat 50% discount on electronics and fashion. Limited time offer, shop now!",
            "deadline_hours": None
        },
        {
            "id": "notif-002",
            "arrival_order": 2,
            "received_at": base_time + (2 * 60 * 1000),  # Arrived 2nd
            "package_name": "com.whatsapp",
            "app_name": "WhatsApp",
            "title": "Rahul",
            "body": "Hey, what time are we meeting for coffee today?",
            "deadline_hours": None
        },
        {
            "id": "notif-003",
            "arrival_order": 3,
            "received_at": base_time + (3 * 60 * 1000),  # Arrived 3rd
            "package_name": "com.snapwork.hdfc",
            "app_name": "HDFC Bank",
            "title": "Account Debited",
            "body": "Rs 12,500.00 debited from a/c **4921 towards Rent Payment via NEFT. Avl Bal: Rs 28,100.",
            "deadline_hours": None
        },
        {
            "id": "notif-004",
            "arrival_order": 4,
            "received_at": base_time + (4 * 60 * 1000),  # Arrived 4th
            "package_name": "com.google.android.apps.classroom",
            "app_name": "Google Classroom",
            "title": "Assignment Submission Deadline",
            "body": "Operating Systems Lab 4 is due today at 11:59 PM. Submit by deadline to receive credit.",
            "deadline_hours": 4.0  # Due in 4 hours
        },
        {
            "id": "notif-005",
            "arrival_order": 5,
            "received_at": base_time + (5 * 60 * 1000),  # Arrived 5th (latest)
            "package_name": "com.snapwork.hdfc",
            "app_name": "HDFC Bank",
            "title": "HDFC NetBanking OTP",
            "body": "Your verification code is 482731 for NetBanking transaction. Do not share it with anyone.",
            "deadline_hours": 0.16  # Expires in 10 mins
        }
    ]

    print("=" * 80)
    print(" PRIOSORT: SIMULATION OF EXECUTIVE SUMMARY (SECTION 1.3)")
    print("=" * 80)
    print("\n[CHRONOLOGICAL ARRIVAL ORDER (Traditional Android shade)]")
    print(f"{'Arrival':<8} {'App':<16} {'Title':<28} {'Received Time'}")
    print("-" * 80)
    for n in notifications_input:
        print(f"#{n['arrival_order']:<7} {n['app_name']:<16} {n['title']:<28} +{n['arrival_order']} min")

    # Process and classify
    processed_records = []
    for item in notifications_input:
        features = preprocessor.extract_features(item["title"], item["body"], item["package_name"])

        if classifier:
            pred_cat, conf, _ = classifier.predict(features["normalized_text"])
            if pred_cat == "UNKNOWN" and features.get("package_category_hint"):
                pred_cat = features["package_category_hint"]
                conf = 0.70
        else:
            # Deterministic fallback
            if features["is_otp"]:
                pred_cat, conf = "SECURITY_AND_VERIFICATION", 0.99
            elif "debited" in features["normalized_text"].lower():
                pred_cat, conf = "FINANCIAL_AND_TRANSACTIONAL", 0.95
            elif features["has_deadline"]:
                pred_cat, conf = "WORK_AND_EDUCATION", 0.95
            elif features["is_promo"]:
                pred_cat, conf = "PROMOTIONAL", 0.95
            else:
                pred_cat, conf = "PERSONAL_AND_SOCIAL", 0.85

        decision = simulator.evaluate(
            features=features,
            predicted_category=pred_cat,
            category_confidence=conf,
            deadline_hours_remaining=item["deadline_hours"],
            received_timestamp_ms=item["received_at"],
            current_timestamp_ms=base_time + (6 * 60 * 1000)
        )

        record = {
            **item,
            "category": pred_cat,
            "confidence": conf,
            "base_score": decision.base_score,
            "priority_level": decision.priority_level,
            "priority_name": decision.priority_name,
            "priority_rank": decision.priority_rank,
            "deadline_score": decision.deadline_score,
            "override_reason": decision.override_reason,
            "explanations": decision.explanations
        }
        processed_records.append(record)

    # Sort using PrioSort Smart Inbox Reordering Algorithm
    sorted_records = sort_notifications(processed_records)

    print("\n" + "=" * 80)
    print(" [PRIOSORT SMART INBOX: DYNAMICALLY REORDERED]")
    print("=" * 80)
    print(f"{'Position':<10} {'Arrival':<9} {'Priority':<12} {'Score':<8} {'App':<16} {'Notification Title'}")
    print("-" * 80)
    for idx, item in enumerate(sorted_records, 1):
        prio_str = f"{item['priority_level']} ({item['priority_name']})"
        print(f"Position #{idx:<2} Arrival #{item['arrival_order']:<2} {prio_str:<12} {item['base_score']:<8} {item['app_name']:<16} {item['title']}")
        print(f"          |-- Reason: {item['override_reason'] or item['explanations'][0]}")

    print("=" * 80)
    print("VERIFICATION CHECK AGAINST SPECIFICATION:")
    expected_order = ["notif-005", "notif-003", "notif-004", "notif-002", "notif-001"]
    actual_order = [r["id"] for r in sorted_records]

    # Note: notif-003 and notif-004 are both High (P1), tie-broken by deadline urgency
    # notif-004 has deadline score 90 (due today), so it may rank before or next to notif-003
    print(f"Top 1 Notification: {sorted_records[0]['title']} ({sorted_records[0]['priority_level']}) -> Matches OTP (Critical P0)? {sorted_records[0]['id'] == 'notif-005'}")
    print(f"Last Notification:  {sorted_records[-1]['title']} ({sorted_records[-1]['priority_level']}) -> Matches Promo (Minimal P4)? {sorted_records[-1]['id'] == 'notif-001'}")
    print("=" * 80)


if __name__ == "__main__":
    run_executive_summary_simulation()
