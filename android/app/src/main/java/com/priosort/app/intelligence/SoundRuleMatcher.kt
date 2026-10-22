package com.priosort.app.intelligence

import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import com.priosort.app.domain.PriorityLevel
import java.time.LocalTime

object SoundRuleMatcher {
    fun match(
        rules: List<SoundRule>,
        packageName: String,
        category: NotificationCategory,
        senderIdentity: String?,
        text: String,
        priority: PriorityLevel? = null,
        focusMode: String? = null
    ): SoundRule? = matching(rules, packageName, category, senderIdentity, text, priority, focusMode).firstOrNull()

    fun matching(
        rules: List<SoundRule>,
        packageName: String,
        category: NotificationCategory,
        senderIdentity: String?,
        text: String,
        priority: PriorityLevel? = null,
        focusMode: String? = null
    ): List<SoundRule> = rules.asSequence()
        .filter { it.enabled }
        .filter { rule ->
            when (rule.matchType) {
                SoundMatchType.APP -> rule.matchValue.equals(packageName, ignoreCase = true)
                SoundMatchType.CATEGORY -> rule.matchValue.equals(category.name, ignoreCase = true) ||
                    rule.matchValue.equals(category.displayName, ignoreCase = true)
                SoundMatchType.PRIORITY -> priority != null && (rule.matchValue.equals(priority.code, ignoreCase = true) || rule.matchValue.equals(priority.displayName, ignoreCase = true))
                SoundMatchType.FOCUS_MODE -> !focusMode.isNullOrBlank() && rule.matchValue.equals(focusMode, ignoreCase = true)
                SoundMatchType.CONTACT -> !senderIdentity.isNullOrBlank() &&
                    senderIdentity.contains(rule.matchValue, ignoreCase = true)
                SoundMatchType.KEYWORD -> rule.matchValue.isNotBlank() &&
                    text.contains(rule.matchValue, ignoreCase = true)
            }
        }
        .sortedWith(compareByDescending<SoundRule> { specificity(it.matchType) }.thenByDescending { it.id })
        .toList()

    fun isWithinQuietHours(now: LocalTime, start: LocalTime, end: LocalTime): Boolean {
        if (start == end) return false
        return if (start < end) now >= start && now < end else now >= start || now < end
    }

    private fun specificity(type: SoundMatchType): Int = when (type) {
        SoundMatchType.CONTACT -> 6
        SoundMatchType.KEYWORD -> 5
        SoundMatchType.PRIORITY -> 4
        SoundMatchType.FOCUS_MODE -> 3
        SoundMatchType.CATEGORY -> 2
        SoundMatchType.APP -> 1
    }

}
