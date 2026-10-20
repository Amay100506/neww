package com.priosort.app.intelligence

import java.util.regex.Pattern

/**
 * High-confidence OTP and verification code detector.
 * Complies with Section 10 and enforces false-positive prevention.
 */
object OtpDetector {

    data class OtpDetectionResult(
        val isOtp: Boolean,
        val extractedCode: String?,
        val explanation: String
    )

    private val POSITIVE_KEYWORDS = listOf(
        Pattern.compile("(?i)\\botp\\b"),
        Pattern.compile("(?i)\\bone[- ]time password\\b"),
        Pattern.compile("(?i)\\bverification code\\b"),
        Pattern.compile("(?i)\\bverify your\\b"),
        Pattern.compile("(?i)\\bsecurity code\\b"),
        Pattern.compile("(?i)\\bauth code\\b"),
        Pattern.compile("(?i)\\blogin code\\b"),
        Pattern.compile("(?i)\\bpasscode\\b"),
        Pattern.compile("(?i)\\bsecret code\\b"),
        Pattern.compile("(?i)\\bvalid for \\d+ min"),
        Pattern.compile("(?i)\\bdo not share\\b"),
        Pattern.compile("(?i)\\bnever share\\b")
    )

    private val NEGATIVE_KEYWORDS = listOf(
        Pattern.compile("(?i)\\border\\s*(?:id|#|no|number)"),
        Pattern.compile("(?i)\\btracking\\s*(?:id|#|no|number)"),
        Pattern.compile("(?i)\\bshipment\\s*(?:id|#|no|number)"),
        Pattern.compile("(?i)\\bbooking\\s*(?:id|#|no|number)"),
        Pattern.compile("(?i)\\bref(?:erence)?\\s*(?:id|#|no|number)"),
        Pattern.compile("(?i)\\bpnr\\b"),
        Pattern.compile("(?i)\\binvoice\\b"),
        Pattern.compile("(?i)\\bcoupon code\\b"),
        Pattern.compile("(?i)\\bpromo code\\b"),
        Pattern.compile("(?i)\\bdiscount code\\b")
    )

    private val CODE_AFTER_KEYWORD = Pattern.compile(
        "(?i)(?:code|otp|is|pin|password)[:\\s-]*([0-9]{4,8})\\b"
    )

    private val CODE_BEFORE_KEYWORD = Pattern.compile(
        "(?i)\\b([0-9]{4,8})\\b.*?(?:security code|verification code|otp|one[- ]time password|code to reset)"
    )

    private val STANDALONE_NUMBER = Pattern.compile("\\b([0-9]{4,8})\\b")

    fun detectOtp(text: String?): OtpDetectionResult {
        if (text.isNullOrBlank()) {
            return OtpDetectionResult(false, null, "Empty text")
        }

        val lower = text.lowercase()

        // 1. Guard against false positives (Order IDs, tracking numbers, courier codes)
        for (neg in NEGATIVE_KEYWORDS) {
            if (neg.matcher(lower).find()) {
                val hasExplicitOtp = Pattern.compile("(?i)\\b(otp|one[- ]time password|verification code)\\b").matcher(lower).find()
                if (!hasExplicitOtp) {
                    return OtpDetectionResult(false, null, "Contains tracking/order identifier rather than security OTP")
                }
            }
        }

        // 2. Count positive indicators
        val matchCount = POSITIVE_KEYWORDS.count { it.matcher(lower).find() }
        if (matchCount == 0) {
            return OtpDetectionResult(false, null, "No verification keywords detected")
        }

        // 3. Match code pattern after keyword
        val afterMatcher = CODE_AFTER_KEYWORD.matcher(text)
        if (afterMatcher.find()) {
            val code = afterMatcher.group(1)
            return OtpDetectionResult(true, code, "Matched verification wording with code ($code)")
        }

        // 4. Match code pattern before keyword
        val beforeMatcher = CODE_BEFORE_KEYWORD.matcher(text)
        if (beforeMatcher.find()) {
            val code = beforeMatcher.group(1)
            return OtpDetectionResult(true, code, "Matched code preceding verification phrase ($code)")
        }

        // 5. Match standalone 4-8 digit number when explicit OTP keywords exist
        val numMatcher = STANDALONE_NUMBER.matcher(text)
        if (numMatcher.find() && matchCount >= 1) {
            val code = numMatcher.group(1)
            return OtpDetectionResult(true, code, "Matched verification indicator with numeric code")
        }

        return OtpDetectionResult(false, null, "Keywords present but missing verification code pattern")
    }
}
