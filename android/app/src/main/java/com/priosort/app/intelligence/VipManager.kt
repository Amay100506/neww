package com.priosort.app.intelligence

import com.priosort.app.domain.PriorityLevel

/**
 * VIP Senders & Critical Contacts Manager.
 * Guarantees that communications from designated VIP contacts are elevated to High priority.
 */
object VipManager {

    private val DEFAULT_VIP_KEYWORDS = setOf(
        "mom",
        "dad",
        "boss",
        "doctor",
        "hospital",
        "emergency",
        "principal",
        "director"
    )

    data class VipCheckResult(
        val isVip: Boolean,
        val matchedVipName: String?,
        val elevatedPriority: PriorityLevel?
    )

    fun checkVip(
        title: String?,
        body: String?,
        userVipKeywords: Set<String> = emptySet()
    ): VipCheckResult {
        val combinedKeywords = DEFAULT_VIP_KEYWORDS + userVipKeywords.map { it.lowercase() }
        val checkText = "${title ?: ""} ${body ?: ""}".lowercase()

        for (vip in combinedKeywords) {
            val pattern = "\\b${Regex.escape(vip)}\\b".toRegex(RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(checkText)) {
                return VipCheckResult(
                    isVip = true,
                    matchedVipName = vip.replaceFirstChar { it.uppercase() },
                    elevatedPriority = PriorityLevel.HIGH
                )
            }
        }

        return VipCheckResult(false, null, null)
    }
}
