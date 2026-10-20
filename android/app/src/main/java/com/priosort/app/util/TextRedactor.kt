package com.priosort.app.util

import java.util.regex.Pattern

/**
 * Utility for masking sensitive tokens (OTPs, card numbers, accounts)
 * to uphold privacy guarantees without losing contextual metadata.
 */
object TextRedactor {

    private val OTP_PATTERNS = listOf(
        // Preceded by OTP / code / verification keywords
        Pattern.compile("(?i)(?:otp|code|password|passcode|pin)[:\\s-]*([0-9]{4,8})\\b"),
        // Preceded by code and followed by verification phrase
        Pattern.compile("(?i)\\b([0-9]{4,8})\\b(?=.*(?:verification|security code|otp))")
    )

    private val CARD_ACCOUNT_PATTERN = Pattern.compile("(?i)(?:card|a/c|acct|account)[\\s#]*(?:ending|no|number)?[\\s:*]*([0-9]{4,16})")

    /**
     * Redacts numeric verification code from body text if redaction is enabled.
     */
    fun redactOtp(text: String?): String {
        if (text.isNullOrBlank()) return ""
        var redacted = text

        for (pattern in OTP_PATTERNS) {
            val matcher = pattern.matcher(redacted)
            val sb = StringBuffer()
            while (matcher.find()) {
                val groupVal = matcher.group(1)
                if (groupVal != null) {
                    val masked = "•".repeat(groupVal.length)
                    matcher.appendReplacement(sb, matcher.group(0).replace(groupVal, masked))
                }
            }
            matcher.appendTail(sb)
            redacted = sb.toString()
        }

        return redacted
    }

    /**
     * Masks card or bank account numbers to last 4 digits for privacy.
     */
    fun maskAccountNumbers(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val matcher = CARD_ACCOUNT_PATTERN.matcher(text)
        val sb = StringBuffer()
        while (matcher.find()) {
            val num = matcher.group(1)
            if (num != null && num.length > 4) {
                val masked = "••••" + num.takeLast(4)
                matcher.appendReplacement(sb, matcher.group(0).replace(num, masked))
            }
        }
        matcher.appendTail(sb)
        return sb.toString()
    }
}
