package com.priosort.app.intelligence

import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel

object CustomRuleMatcher {

    data class RuleMatchResult(
        val isMatched: Boolean,
        val matchedRule: CustomRule?,
        val overriddenPriority: PriorityLevel?,
        val overriddenCategory: NotificationCategory?
    )

    fun evaluateRules(
        title: String?,
        body: String?,
        packageName: String,
        rules: List<CustomRule>
    ): RuleMatchResult {
        val cleanTitle = title?.lowercase() ?: ""
        val cleanBody = body?.lowercase() ?: ""
        val cleanPkg = packageName.lowercase()

        for (rule in rules) {
            if (!rule.isEnabled) continue
            val pattern = rule.keywordPattern.lowercase()

            val matches = when (rule.targetField.uppercase()) {
                "TITLE" -> cleanTitle.contains(pattern)
                "BODY" -> cleanBody.contains(pattern)
                "PACKAGE" -> cleanPkg.contains(pattern)
                else -> cleanTitle.contains(pattern) || cleanBody.contains(pattern) || cleanPkg.contains(pattern)
            }

            if (matches) {
                return RuleMatchResult(
                    isMatched = true,
                    matchedRule = rule,
                    overriddenPriority = rule.assignedPriority,
                    overriddenCategory = rule.assignedCategory
                )
            }
        }

        return RuleMatchResult(false, null, null, null)
    }
}
