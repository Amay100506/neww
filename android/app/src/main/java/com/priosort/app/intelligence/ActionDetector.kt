package com.priosort.app.intelligence

import java.util.regex.Pattern

object ActionDetector {

    private val EXPLICIT_ACTION_PATTERNS = listOf(
        Pattern.compile("(?i)\\baction required\\b"),
        Pattern.compile("(?i)\\bplease verify\\b"),
        Pattern.compile("(?i)\\bconfirm now\\b"),
        Pattern.compile("(?i)\\bpay now\\b"),
        Pattern.compile("(?i)\\bcomplete submission\\b"),
        Pattern.compile("(?i)\\bclick here to verify\\b"),
        Pattern.compile("(?i)\\bapprove\\b"),
        Pattern.compile("(?i)\\brespond by\\b"),
        Pattern.compile("(?i)\\breview now\\b"),
        Pattern.compile("(?i)\\bcheck in now\\b")
    )

    data class ActionResult(
        val hasAction: Boolean,
        val actionScore: Float, // 100, 75, 50, 0
        val description: String?
    )

    fun detectAction(text: String?, isOtp: Boolean, isFraudOrFail: Boolean): ActionResult {
        if (isOtp) {
            return ActionResult(true, 100f, "Enter verification passcode")
        }
        if (isFraudOrFail) {
            return ActionResult(true, 100f, "Immediate user review / intervention required")
        }
        if (text.isNullOrBlank()) {
            return ActionResult(false, 0f, null)
        }

        val lower = text.lowercase()
        for (pattern in EXPLICIT_ACTION_PATTERNS) {
            val matcher = pattern.matcher(lower)
            if (matcher.find()) {
                val matched = matcher.group(0)
                return ActionResult(true, 75f, "Action requested: $matched")
            }
        }

        return ActionResult(false, 0f, null)
    }
}
