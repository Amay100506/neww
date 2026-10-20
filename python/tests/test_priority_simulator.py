"""Unit tests for PrioSort ML components and priority simulator."""

import os
import sys
import pytest

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), "..")))

from priosort_ml.preprocessing import TextPreprocessor
from priosort_ml.classifier import NotificationClassifier
from priosort_ml.priority_simulator import PrioritySimulator, sort_notifications


@pytest.fixture
def preprocessor():
    return TextPreprocessor()


@pytest.fixture
def simulator():
    return PrioritySimulator(tau_hours=24.0)


def test_otp_detection_positive(preprocessor):
    """Verifies that genuine OTPs are detected accurately."""
    texts = [
        "Your verification code is 482731. Do not share it with anyone.",
        "Use 918234 as your secret OTP for HDFC NetBanking. Valid for 10 mins.",
        "Your Google verification code is G-819203. Never share this code.",
        "Uber: Your one-time login passcode is 1048. Do not share.",
        "612345 is your security code to reset your Instagram password."
    ]
    for text in texts:
        is_otp, code, reason = preprocessor.detect_otp(text)
        assert is_otp is True, f"Failed to detect OTP in: '{text}'"
        assert code is not None


def test_otp_detection_false_positives(preprocessor):
    """Verifies that non-OTP numbers (order IDs, tracking IDs) are rejected."""
    non_otps = [
        "Your order #482731 has shipped and will arrive tomorrow via BlueDart.",
        "Tracking ID: MYN982142 is out for delivery with your courier agent.",
        "PNR 281920 Confirmed! Coach B3 Seat 42, Departure at 18:30 hrs.",
        "Flight Booking Ref: MMT482910 to Mumbai on 25th Oct.",
        "Get flat 50% off on all items! Use promo code SAVE50."
    ]
    for text in non_otps:
        is_otp, code, reason = preprocessor.detect_otp(text)
        assert is_otp is False, f"False positive OTP detected in: '{text}'"


def test_priority_scoring_formula(simulator, preprocessor):
    """Tests formula: S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R."""
    # Custom values:
    # U=100, A=100, D=100, I=100, R=100 -> S = 100.0
    features = {
        "is_otp": True,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": True,
        "has_action": True,
        "has_transaction": False,
        "is_important_transaction": False,
        "is_promo": False
    }
    decision = simulator.evaluate(
        features=features,
        predicted_category="SECURITY_AND_VERIFICATION",
        user_app_importance=100.0,
        deadline_hours_remaining=0.5,
        received_timestamp_ms=1000,
        current_timestamp_ms=1000  # R=100
    )
    assert decision.urgency_score == 100.0
    assert decision.action_score == 100.0
    assert decision.deadline_score == 100.0
    assert decision.importance_score == 100.0
    assert decision.recency_score == 100.0
    assert decision.base_score == 100.0
    assert decision.priority_level == "P0"


def test_otp_rule_override(simulator):
    """Tests that an OTP is promoted to P0 Critical with explicit override explanation."""
    features = {
        "is_otp": True,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": True,
        "is_promo": False,
        "has_transaction": False,
        "is_important_transaction": False
    }
    decision = simulator.evaluate(
        features=features,
        predicted_category="SECURITY_AND_VERIFICATION",
        user_app_importance=50.0
    )
    assert decision.priority_level == "P0"
    assert decision.is_overridden is True
    assert "OTP" in decision.override_reason


def test_promotional_demotion(simulator):
    """Tests that promotional notifications are demoted to P4 Minimal."""
    features = {
        "is_otp": False,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": False,
        "is_promo": True,
        "has_transaction": False,
        "is_important_transaction": False
    }
    decision = simulator.evaluate(
        features=features,
        predicted_category="PROMOTIONAL",
        user_app_importance=30.0
    )
    assert decision.priority_level == "P4"
    assert decision.is_overridden is True
    assert "Promotional" in decision.override_reason


def test_smart_inbox_sorting():
    """Verifies that sorting respects priority rank above arrival time."""
    notifications = [
        {"id": "promo", "is_pinned": False, "priority_rank": 1, "deadline_score": 0, "received_at": 5000},  # Arrived newest, rank 1
        {"id": "msg", "is_pinned": False, "priority_rank": 3, "deadline_score": 0, "received_at": 4000},
        {"id": "txn", "is_pinned": False, "priority_rank": 4, "deadline_score": 0, "received_at": 3000},
        {"id": "deadline", "is_pinned": False, "priority_rank": 4, "deadline_score": 90, "received_at": 2000},
        {"id": "otp", "is_pinned": False, "priority_rank": 5, "deadline_score": 0, "received_at": 1000}    # Arrived oldest, rank 5
    ]
    sorted_res = sort_notifications(notifications)
    sorted_ids = [r["id"] for r in sorted_res]

    # OTP (rank 5) MUST be first! Promo (rank 1) MUST be last!
    assert sorted_ids[0] == "otp"
    assert sorted_ids[-1] == "promo"
    # Deadline (rank 4 with deadline 90) outranks txn (rank 4 with deadline 0)
    assert sorted_ids[1] == "deadline"
    assert sorted_ids[2] == "txn"
    assert sorted_ids[3] == "msg"


def test_focus_mode_work(simulator):
    """Tests that WORK mode elevates work tasks and demotes promotional/social alerts."""
    features = {
        "is_otp": False,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": False,
        "has_transaction": False,
        "is_important_transaction": False,
        "is_promo": False
    }

    decision = simulator.evaluate(
        features=features,
        predicted_category="WORK_AND_EDUCATION",
        user_app_importance=50.0,
        focus_mode="WORK"
    )
    assert decision.priority_level == "P1"
    assert "Work Mode active" in decision.override_reason


def test_focus_mode_sleep_critical_immune(simulator):
    """Tests that P0 Critical notifications are NEVER suppressed under SLEEP mode."""
    features = {
        "is_otp": True,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": True,
        "has_transaction": False,
        "is_important_transaction": False,
        "is_promo": False
    }

    decision = simulator.evaluate(
        features=features,
        predicted_category="SECURITY_AND_VERIFICATION",
        user_app_importance=50.0,
        focus_mode="SLEEP"
    )
    assert decision.priority_level == "P0"


def test_custom_rule_matching(simulator):
    """Tests user custom rule overrides."""
    features = {
        "normalized_text": "Your electricity bill payment of Rs 1,800 is due.",
        "is_otp": False,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": False,
        "has_transaction": False,
        "is_important_transaction": False,
        "is_promo": False
    }

    rules = [
        {"keyword": "electricity bill", "priority": "P1"}
    ]

    decision = simulator.evaluate(
        features=features,
        predicted_category="FINANCIAL_AND_TRANSACTIONAL",
        custom_rules=rules
    )
    assert decision.priority_level == "P1"
    assert "User Custom Rule matched" in decision.override_reason


def test_vip_contact_elevation(simulator):
    """Tests that messages from VIP contacts are promoted to P1 High."""
    features = {
        "is_otp": False,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": False,
        "has_transaction": False,
        "is_important_transaction": False,
        "is_promo": False
    }

    decision = simulator.evaluate(
        features=features,
        predicted_category="PERSONAL_AND_SOCIAL",
        user_app_importance=50.0,
        is_vip=True
    )
    assert decision.priority_level == "P1"
    assert "VIP Contact" in decision.override_reason


def test_delivery_out_for_delivery_elevation(simulator):
    """Tests that packages out for delivery are promoted to P1 High."""
    features = {
        "is_otp": False,
        "is_fraud_alert": False,
        "is_payment_fail": False,
        "has_deadline": False,
        "has_action": False,
        "has_transaction": False,
        "is_important_transaction": False,
        "is_promo": False
    }

    decision = simulator.evaluate(
        features=features,
        predicted_category="GENERAL_INFORMATIONAL",
        user_app_importance=50.0,
        is_out_for_delivery=True
    )
    assert decision.priority_level == "P1"
    assert "out for delivery" in decision.override_reason
