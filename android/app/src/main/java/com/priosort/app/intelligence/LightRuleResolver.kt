package com.priosort.app.intelligence

import com.priosort.app.domain.LightMatchType
import com.priosort.app.domain.LightRule
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel

object LightRuleResolver {
    fun resolve(
        rules: List<LightRule>,
        packageName: String,
        category: NotificationCategory,
        priority: PriorityLevel,
        senderIdentity: String?,
        text: String
    ): LightRule? = matching(rules, packageName, category, priority, senderIdentity, text).firstOrNull()

    fun matching(
        rules: List<LightRule>,
        packageName: String,
        category: NotificationCategory,
        priority: PriorityLevel,
        senderIdentity: String?,
        text: String
    ): List<LightRule> = rules.asSequence()
        .filter { it.enabled }
        .filter { rule ->
            when (rule.matchType) {
                LightMatchType.APP -> rule.matchValue.equals(packageName, ignoreCase = true)
                LightMatchType.CATEGORY -> rule.matchValue.equals(category.name, ignoreCase = true) || rule.matchValue.equals(category.displayName, ignoreCase = true)
                LightMatchType.PRIORITY -> rule.matchValue.equals(priority.code, ignoreCase = true) || rule.matchValue.equals(priority.displayName, ignoreCase = true)
                LightMatchType.CONTACT -> !senderIdentity.isNullOrBlank() && senderIdentity.contains(rule.matchValue, ignoreCase = true)
                LightMatchType.KEYWORD -> rule.matchValue.isNotBlank() && text.contains(rule.matchValue, ignoreCase = true)
            }
        }
        .sortedWith(compareByDescending<LightRule> { specificity(it.matchType) }.thenByDescending { it.id })
        .toList()

    fun resolve(rules: List<LightRule>, record: NotificationRecord): LightRule? = resolve(
        rules, record.packageName, record.category, record.priorityLevel, null,
        listOfNotNull(record.title, record.body).joinToString(" ")
    )

    private fun specificity(type: LightMatchType): Int = when (type) {
        LightMatchType.CONTACT -> 5
        LightMatchType.KEYWORD -> 4
        LightMatchType.PRIORITY -> 3
        LightMatchType.CATEGORY -> 2
        LightMatchType.APP -> 1
    }
}
