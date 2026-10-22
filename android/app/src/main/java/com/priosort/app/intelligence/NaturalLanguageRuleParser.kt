package com.priosort.app.intelligence

import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import java.util.Locale

/** Small deterministic grammar for local, review-before-save rule drafting. */
object NaturalLanguageRuleParser {
    data class Result(val rule: CustomRule?, val message: String)

    private val grammar = Regex(
        """(?i)^\s*(?:when|if)\s+(package|app|title|body|text|message)\s+(?:contains|includes|matches)\s+["“](.{1,200}?)["”]\s*,?\s*(?:then\s+)?(?:set|make|assign)\s+(priority|category)\s+(?:to|as)\s+(.+?)\s*$"""
    )

    fun parse(description: String): Result {
        if (description.length > 500) return Result(null, "Keep the description under 500 characters.")
        val match = grammar.matchEntire(description.trim())
            ?: return Result(null, "Use: When body contains \"invoice\", set priority to High")
        val field = match.groupValues[1].lowercase(Locale.ROOT)
        val phrase = match.groupValues[2].trim()
        val effect = match.groupValues[3].lowercase(Locale.ROOT)
        val value = match.groupValues[4].trim().trimEnd('.', '!')
        if (phrase.isBlank()) return Result(null, "Add a non-empty match phrase in quotes.")

        val priority = if (effect == "priority") PriorityLevel.entries.firstOrNull {
            it.displayName.equals(value, true) || it.code.equals(value, true) || it.name.equals(value, true)
        } else null
        val category = if (effect == "category") resolveCategory(value) else null
        if (effect == "priority" && priority == null) return Result(null, "Priority must be Critical, High, Medium, Low, or Minimal.")
        if (effect == "category" && category == null) return Result(null, "Category must match one of the PrioSort categories.")

        val targetField = when (field) {
            "package", "app" -> "PACKAGE"
            "title" -> "TITLE"
            "body" -> "BODY"
            else -> "ANY"
        }
        val targetLabel = when (targetField) {
            "PACKAGE" -> "app package"
            "TITLE" -> "title"
            "BODY" -> "body"
            else -> "notification text"
        }
        val effectLabel = priority?.displayName ?: category?.displayName.orEmpty()
        val rule = CustomRule(
            ruleName = "$targetLabel contains $phrase → $effectLabel".take(100),
            keywordPattern = phrase,
            targetField = targetField,
            assignedPriority = priority,
            assignedCategory = category
        )
        return Result(rule, "Parsed locally. Review the fields below, then explicitly save the rule.")
    }

    private fun resolveCategory(value: String): NotificationCategory? {
        NotificationCategory.entries.firstOrNull { it != NotificationCategory.UNKNOWN &&
            (it.displayName.equals(value, true) || it.name.equals(value.replace(' ', '_'), true))
        }?.let { return it }
        val normalized = value.lowercase(Locale.ROOT).replace(Regex("[^a-z]+"), " ").trim()
        return when (normalized) {
            "security", "verification", "security verification", "otp" -> NotificationCategory.SECURITY_AND_VERIFICATION
            "finance", "financial", "transaction", "transactions", "finance transactions" -> NotificationCategory.FINANCIAL_AND_TRANSACTIONAL
            "work", "education", "work education" -> NotificationCategory.WORK_AND_EDUCATION
            "personal", "social", "personal social" -> NotificationCategory.PERSONAL_AND_SOCIAL
            "system", "device", "system device" -> NotificationCategory.SYSTEM_AND_DEVICE
            "promotion", "promotional", "marketing", "offer", "offers" -> NotificationCategory.PROMOTIONAL
            "general", "information", "informational", "general info", "info" -> NotificationCategory.GENERAL_INFORMATIONAL
            else -> null
        }
    }
}
