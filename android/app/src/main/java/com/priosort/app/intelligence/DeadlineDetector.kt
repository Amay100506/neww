package com.priosort.app.intelligence

import java.util.regex.Pattern

object DeadlineDetector {

    data class DeadlineResult(
        val hasDeadline: Boolean,
        val deadlineScore: Float,
        val deadlineText: String?,
        val estimatedHoursRemaining: Float?,
        val resolutionStatus: String
    )

    private val DEADLINE_PATTERNS = listOf(
        Pattern.compile("(?i)\\bdue (?:today|tonight)\\b"),
        Pattern.compile("(?i)\\bdue tomorrow\\b"),
        Pattern.compile("(?i)\\bdeadline\\b"),
        Pattern.compile("(?i)\\bsubmit by\\b"),
        Pattern.compile("(?i)\\bsubmission deadline\\b"),
        Pattern.compile("(?i)\\bclosing soon\\b"),
        Pattern.compile("(?i)\\bexpires in (\\d+)\\s*(?:mins?|minutes?)\\b"),
        Pattern.compile("(?i)\\bexpires in (\\d+)\\s*(?:hours?|hrs?)\\b"),
        Pattern.compile("(?i)\\bin (\\d+)\\s*(?:hours?|hrs?)\\b")
    )

    fun detectDeadline(text: String?): DeadlineResult {
        if (text.isNullOrBlank()) {
            return DeadlineResult(false, 0f, null, null, "NONE")
        }

        val lower = text.lowercase()

        // 1. Minute expiry (e.g. OTP or transient window)
        val minMatcher = Pattern.compile("(?i)\\bexpires in (\\d+)\\s*(?:mins?|minutes?)\\b").matcher(lower)
        if (minMatcher.find()) {
            val mins = minMatcher.group(1)?.toFloatOrNull() ?: 10f
            val hours = mins / 60f
            return DeadlineResult(true, 100f, minMatcher.group(0), hours, "RESOLVED_IMMINENT")
        }

        // 2. Hour expiry (e.g. in 2 hours, closes in 4 hours)
        val hrMatcher = Pattern.compile("(?i)(?:in|expires in|closes in) (\\d+)\\s*(?:hours?|hrs?)\\b").matcher(lower)
        if (hrMatcher.find()) {
            val hrs = hrMatcher.group(1)?.toFloatOrNull() ?: 2f
            val score = if (hrs <= 1f) 100f else if (hrs <= 24f) 90f else 70f
            return DeadlineResult(true, score, hrMatcher.group(0), hrs, "RESOLVED_HOURS")
        }

        // 3. Due today / tonight
        if (lower.contains("due today") || lower.contains("due tonight") || lower.contains("by midnight")) {
            return DeadlineResult(true, 90f, "Due today", 6.0f, "RESOLVED_TODAY")
        }

        // 4. Due tomorrow
        if (lower.contains("due tomorrow")) {
            return DeadlineResult(true, 70f, "Due tomorrow", 24.0f, "RESOLVED_TOMORROW")
        }

        // 5. General deadline keyword
        for (pattern in DEADLINE_PATTERNS) {
            val matcher = pattern.matcher(lower)
            if (matcher.find()) {
                val matched = matcher.group(0)
                return DeadlineResult(true, 70f, matched, null, "APPROXIMATE_DEADLINE")
            }
        }

        return DeadlineResult(false, 0f, null, null, "NONE")
    }
}
