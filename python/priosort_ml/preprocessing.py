"""Preprocessing and contextual feature extraction for PrioSort notifications."""

import re
from typing import Dict, Any, Optional, Tuple


class TextPreprocessor:
    """Preprocesses notification title and body, extracting structural features."""

    # Explicit OTP and verification patterns
    OTP_KEYWORDS = [
        r"\botp\b",
        r"\bone[- ]time password\b",
        r"\bverification code\b",
        r"\bverify your\b",
        r"\bsecurity code\b",
        r"\bauth code\b",
        r"\blogin code\b",
        r"\bpasscode\b",
        r"\bsecret code\b",
        r"\bvalid for \d+ min",
        r"\bdo not share\b",
        r"\bnever share\b"
    ]

    # False positive OTP signals (order tracking, ticket ids, invoice numbers)
    OTP_NEGATIVE_KEYWORDS = [
        r"\border\s*(?:id|#|no|number)",
        r"\btracking\s*(?:id|#|no|number)",
        r"\bshipment\s*(?:id|#|no|number)",
        r"\bbooking\s*(?:id|#|no|number)",
        r"\bref(?:erence)?\s*(?:id|#|no|number)",
        r"\bpnr\b",
        r"\binvoice\b",
        r"\bcoupon code\b",
        r"\bpromo code\b",
        r"\bdiscount code\b",
        r"\bvoucher code\b"
    ]

    # Financial urgent vs promotional patterns
    FRAUD_KEYWORDS = [
        r"\bfraud\b",
        r"\bsuspicious\b",
        r"\bunauthorized\b",
        r"\bunrecogni[sz]ed\b",
        r"\bblocked\b",
        r"\bsecurity alert\b",
        r"\baccount locked\b",
        r"\bcompromised\b"
    ]

    PAYMENT_FAIL_KEYWORDS = [
        r"\bpayment failed\b",
        r"\btransaction failed\b",
        r"\bdeclined\b",
        r"\bauto-debit failed\b",
        r"\binsufficient funds?\b",
        r"\bunsuccessful transaction\b"
    ]

    PROMO_KEYWORDS = [
        r"\b\d+%\s*off\b",
        r"\bdiscount\b",
        r"\bhurry\b",
        r"\blimited time offer\b",
        r"\bflat\s*rs\b",
        r"\bcashback\b",
        r"\bsale is live\b",
        r"\bbuy\s*\d+\s*get\s*\d+\b",
        r"\bexclusive deal\b",
        r"\bshop now\b",
        r"\bfree delivery\b",
        r"\bcredit card pre-approved\b",
        r"\bpersonal loan approved\b",
        r"\binstant loan\b"
    ]

    # Deadline indicators
    DEADLINE_KEYWORDS = [
        r"\bdue (?:today|tomorrow|by|at|on)\b",
        r"\bdeadline\b",
        r"\bsubmit by\b",
        r"\bsubmission deadline\b",
        r"\bexpires in\b",
        r"\blast date\b",
        r"\bends today\b",
        r"\bbefore \d{1,2}(?::\d{2})?\s*(?:am|pm)?\b",
        r"\bin \d+\s*(?:hours?|hrs?|mins?|minutes?)\b"
    ]

    TRANSACTION_KEYWORDS = [
        r"\bdebited\b",
        r"\bcredited\b",
        r"\btransferred\b",
        r"\bpaid\b",
        r"\bwithdrawn\b",
        r"\bspent\b"
    ]

    ACTION_KEYWORDS = [
        r"\baction required\b",
        r"\bplease verify\b",
        r"\bconfirm now\b",
        r"\bpay now\b",
        r"\bcomplete submission\b",
        r"\bclick here to verify\b",
        r"\bapprove\b",
        r"\brespond by\b",
        r"\breview now\b"
    ]

    PACKAGE_CATEGORY_HINTS = {
        "com.whatsapp": "PERSONAL_AND_SOCIAL",
        "org.telegram.messenger": "PERSONAL_AND_SOCIAL",
        "com.discord": "PERSONAL_AND_SOCIAL",
        "com.snapchat.android": "PERSONAL_AND_SOCIAL",
        "com.google.android.apps.messaging": "PERSONAL_AND_SOCIAL",
        "com.google.android.apps.classroom": "WORK_AND_EDUCATION",
        "com.instructure.cndv": "WORK_AND_EDUCATION",
        "com.microsoft.teams": "WORK_AND_EDUCATION",
        "com.Slack": "WORK_AND_EDUCATION",
        "android": "SYSTEM_AND_DEVICE",
        "com.android.systemui": "SYSTEM_AND_DEVICE",
        "com.android.settings": "SYSTEM_AND_DEVICE",
        "com.flipkart.android": "PROMOTIONAL",
        "in.amazon.mShop.android.shopping": "PROMOTIONAL",
        "in.swiggy.android": "PROMOTIONAL",
        "com.application.zomato": "PROMOTIONAL"
    }

    def __init__(self):
        self._otp_regexes = [re.compile(p, re.IGNORECASE) for p in self.OTP_KEYWORDS]
        self._otp_neg_regexes = [re.compile(p, re.IGNORECASE) for p in self.OTP_NEGATIVE_KEYWORDS]
        self._fraud_regexes = [re.compile(p, re.IGNORECASE) for p in self.FRAUD_KEYWORDS]
        self._payment_fail_regexes = [re.compile(p, re.IGNORECASE) for p in self.PAYMENT_FAIL_KEYWORDS]
        self._promo_regexes = [re.compile(p, re.IGNORECASE) for p in self.PROMO_KEYWORDS]
        self._deadline_regexes = [re.compile(p, re.IGNORECASE) for p in self.DEADLINE_KEYWORDS]
        self._transaction_regexes = [re.compile(p, re.IGNORECASE) for p in self.TRANSACTION_KEYWORDS]
        self._action_regexes = [re.compile(p, re.IGNORECASE) for p in self.ACTION_KEYWORDS]

    def clean_text(self, text: Optional[str]) -> str:
        """Normalizes whitespace and lowercases string."""
        if not text:
            return ""
        # Collapse whitespace
        text = re.sub(r"\s+", " ", text)
        return text.strip()

    def combine_title_body(self, title: Optional[str], body: Optional[str]) -> str:
        """Combines title and body into a single normalized text string."""
        clean_title = self.clean_text(title)
        clean_body = self.clean_text(body)
        if clean_title and clean_body:
            return f"{clean_title} - {clean_body}"
        return clean_title or clean_body

    def detect_otp(self, text: str) -> Tuple[bool, Optional[str], str]:
        """Detects if text contains an OTP / verification code.

        Returns:
            Tuple of (is_otp, extracted_code, explanation)
        """
        lower = text.lower()

        # Check negative signals first (order numbers, tracking IDs, promo codes)
        for neg in self._otp_neg_regexes:
            if neg.search(lower):
                # If it's explicitly an order/tracking code without strong OTP phrasing, reject
                has_strong_otp = bool(re.search(r"\b(otp|one[- ]time password|verification code)\b", lower))
                if not has_strong_otp:
                    return False, None, "Contains tracking/order identifier rather than security OTP"

        # Check positive OTP indicators
        matched_indicators = [r.pattern for r in self._otp_regexes if r.search(lower)]
        if not matched_indicators:
            return False, None, "No verification keywords detected"

        # Look for 4-8 digit numeric or alphanumeric code
        # Pattern 1: preceded or followed by code, is, :, -
        code_match = re.search(r"(?:code|otp|is|pin|password)[:\s-]*([0-9]{4,8})\b", text, re.IGNORECASE)
        if code_match:
            code = code_match.group(1)
            return True, code, f"Matched verification keywords and extracted code ({len(code)} digits)"

        # Pattern 2: code appears before keyword (e.g., '612345 is your security code')
        code_before_match = re.search(r"\b([0-9]{4,8})\b.*?(?:security code|verification code|otp|one[- ]time password|code to reset)", text, re.IGNORECASE)
        if code_before_match:
            code = code_before_match.group(1)
            return True, code, f"Matched verification keywords preceded by code ({len(code)} digits)"

        # Pattern 3: any 4 to 8 digit number with explicit verification indicator
        num_matches = re.findall(r"\b([0-9]{4,8})\b", text)
        if num_matches and len(matched_indicators) >= 1:
            return True, num_matches[0], "Matched verification indicator with numeric passcode"

        return False, None, "Keywords detected but lacks verification code pattern"

    def extract_features(self, title: Optional[str], body: Optional[str], package_name: str = "") -> Dict[str, Any]:
        """Extracts contextual features for intelligence scoring."""
        combined = self.combine_title_body(title, body)
        lower = combined.lower()

        is_otp, otp_code, otp_reason = self.detect_otp(combined)

        has_fraud = any(r.search(lower) for r in self._fraud_regexes)
        has_payment_fail = any(r.search(lower) for r in self._payment_fail_regexes)
        has_promo = any(r.search(lower) for r in self._promo_regexes)
        has_deadline = any(r.search(lower) for r in self._deadline_regexes)
        has_action = any(r.search(lower) for r in self._action_regexes)
        has_transaction = any(r.search(lower) for r in self._transaction_regexes) and not has_promo

        # Detect transaction amount if present
        is_high_value_tx = False
        amount_match = re.search(r"(?:rs\.?|inr|usd|\$)\s*([\d,]+(?:\.\d{2})?)", lower)
        if amount_match and has_transaction:
            try:
                amt_str = amount_match.group(1).replace(",", "")
                amt = float(amt_str)
                if amt >= 2000.0:  # Transactions above 2000 treated as important alerts
                    is_high_value_tx = True
            except ValueError:
                pass

        # Detect deadline text snippet
        deadline_text = None
        for r in self._deadline_regexes:
            m = r.search(combined)
            if m:
                deadline_text = m.group(0)
                break

        pkg_hint = self.PACKAGE_CATEGORY_HINTS.get(package_name, None)

        return {
            "normalized_text": combined,
            "package_name": package_name,
            "package_category_hint": pkg_hint,
            "is_otp": is_otp,
            "otp_code": otp_code,
            "otp_reason": otp_reason,
            "is_fraud_alert": has_fraud,
            "is_payment_fail": has_payment_fail,
            "is_promo": has_promo,
            "has_deadline": has_deadline,
            "deadline_text": deadline_text,
            "has_transaction": has_transaction,
            "is_important_transaction": is_high_value_tx,
            "has_action": has_action or is_otp or has_fraud or has_payment_fail
        }
