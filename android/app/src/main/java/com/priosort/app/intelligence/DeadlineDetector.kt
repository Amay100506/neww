package com.priosort.app.intelligence

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle
import java.util.Locale
import java.util.regex.Pattern

/** Conservative deadline extraction. A signal is not treated as a resolved date unless its time can be derived from the text. */
object DeadlineDetector {

    data class DeadlineResult(
        val hasDeadline: Boolean,
        val deadlineScore: Float,
        val deadlineText: String?,
        val estimatedHoursRemaining: Float?,
        val resolutionStatus: String,
        val deadlineAtMs: Long? = null
    )

    private val relativeDeadline = Pattern.compile(
        "(?i)\\b(?:expires?\\s+in|closes?\\s+in|due\\s+in|deadline\\s+in|valid\\s+for)\\s+(\\d+)\\s*(minutes?|mins?|hours?|hrs?|days?)\\b"
    )
    private val dateDeadline = Pattern.compile(
        "(?i)\\b(?:due|deadline|submit by|respond by|submission deadline|expires? (?:on|by))\\s+(\\d{4}-\\d{2}-\\d{2})\\b"
    )
    private val explicitTime = Pattern.compile("(?i)\\b(?:at|by)\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)\\b")
    private val vagueDeadline = Pattern.compile("(?i)\\b(?:deadlines?|due date|due today|due tonight|due tomorrow|closing soon|submit by|respond by)\\b")
    private val overdueSignal = Pattern.compile("(?i)\\b(?:overdue|past due|deadline has passed)\\b")
    private val zonedDateTime = DateTimeFormatter.ofPattern("uuuu-MM-dd", Locale.ROOT)
        .withResolverStyle(ResolverStyle.STRICT)

    /** [referenceTimeMs] should be the notification's post time when evaluating a captured record. */
    fun detectDeadline(text: String?, referenceTimeMs: Long = System.currentTimeMillis()): DeadlineResult {
        if (text.isNullOrBlank()) return DeadlineResult(false, 0f, null, null, "ABSENT")
        val zone = ZoneId.systemDefault()
        val reference = java.time.Instant.ofEpochMilli(referenceTimeMs).atZone(zone)

        overdueSignal.matcher(text).takeIf { it.find() }?.let { matcher ->
            return DeadlineResult(true, 100f, matcher.group(), -0.001f, "RESOLVED", referenceTimeMs - 1)
        }

        relativeDeadline.matcher(text).takeIf { it.find() }?.let { matcher ->
            val amount = matcher.group(1)?.toLongOrNull()
                ?: return DeadlineResult(true, 0f, matcher.group(), null, "AMBIGUOUS")
            val unit = matcher.group(2)?.lowercase(Locale.ROOT).orEmpty()
            val deadlineAt = runCatching {
                val duration = when {
                    unit.startsWith("min") -> Duration.ofMinutes(amount)
                    unit.startsWith("hour") || unit.startsWith("hr") -> Duration.ofHours(amount)
                    else -> Duration.ofDays(amount)
                }
                Math.addExact(referenceTimeMs, duration.toMillis())
            }.getOrNull() ?: return DeadlineResult(true, 0f, matcher.group(), null, "AMBIGUOUS")
            return resolved(matcher.group(), deadlineAt, referenceTimeMs)
        }

        dateDeadline.matcher(text).takeIf { it.find() }?.let { matcher ->
            val date = try {
                LocalDate.parse(matcher.group(1), zonedDateTime)
            } catch (_: DateTimeParseException) {
                return DeadlineResult(true, 0f, matcher.group(), null, "AMBIGUOUS")
            }
            val (time, invalidTime) = explicitTimeAfter(text, matcher.end())
            if (invalidTime) return DeadlineResult(true, 0f, matcher.group(), null, "AMBIGUOUS")
            val deadlineAt = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
            return resolved(matcher.group(), deadlineAt, referenceTimeMs)
        }

        val relativeDayPattern = Regex("(?i)\\bdue\\s+today\\b|\\bdue\\s+tonight\\b|\\bby midnight\\b|\\bdue\\s+tomorrow\\b")
        val relativeDayMatch = relativeDayPattern.find(text)
        val relativeDay = when {
            relativeDayMatch?.value?.contains("tomorrow", ignoreCase = true) == true -> reference.toLocalDate().plusDays(1)
            relativeDayMatch != null -> reference.toLocalDate()
            else -> null
        }
        if (relativeDay != null) {
            // A date-only deadline is interpreted as the end of that explicitly stated local calendar day.
            val (explicitTime, invalidTime) = explicitTimeAfter(text, relativeDayMatch!!.range.last + 1)
            if (invalidTime) return DeadlineResult(true, 0f, relativeDayMatch.value, null, "AMBIGUOUS")
            val dateEnd = relativeDay.atTime(explicitTime).atZone(zone).toInstant().toEpochMilli()
            return resolved(
                relativeDayMatch!!.value,
                dateEnd,
                referenceTimeMs
            )
        }

        vagueDeadline.matcher(text).takeIf { it.find() }?.let { matcher ->
            return DeadlineResult(true, 0f, matcher.group(), null, "UNRESOLVED")
        }
        return DeadlineResult(false, 0f, null, null, "ABSENT")
    }

    private fun resolved(label: String, deadlineAtMs: Long, referenceTimeMs: Long): DeadlineResult {
        val remainingMs = deadlineAtMs - referenceTimeMs
        val hours = remainingMs / 3_600_000f
        val score = when {
            remainingMs < 0L -> 100f
            remainingMs < Duration.ofHours(1).toMillis() -> 100f
            remainingMs < Duration.ofHours(24).toMillis() -> 90f
            remainingMs <= Duration.ofHours(48).toMillis() -> 70f
            remainingMs <= Duration.ofDays(7).toMillis() -> 40f
            else -> 20f
        }
        return DeadlineResult(true, score, label, hours, "RESOLVED", deadlineAtMs)
    }

    /** Returns the parsed clock time and whether a clock expression was present but invalid. */
    private fun explicitTimeAfter(text: String, startIndex: Int): Pair<LocalTime, Boolean> {
        val clause = text.substring(startIndex.coerceIn(0, text.length)).takeWhile { it !in ".;,\n" }
        val matcher = explicitTime.matcher(clause)
        if (!matcher.find()) return LocalTime.MAX to false
        val hour = matcher.group(1)?.toIntOrNull() ?: return LocalTime.MAX to true
        val minute = matcher.group(2)?.toIntOrNull() ?: 0
        val meridiem = matcher.group(3)?.lowercase(Locale.ROOT) ?: return LocalTime.MAX to true
        if (hour !in 1..12 || minute !in 0..59) return LocalTime.MAX to true
        return LocalTime.of((hour % 12) + if (meridiem == "pm") 12 else 0, minute) to false
    }
}
