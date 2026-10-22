package com.priosort.app.domain

data class SoundRule(
    val id: Long = 0,
    val name: String,
    val matchType: SoundMatchType,
    val matchValue: String,
    val soundUri: String? = null,
    val volume: Float = 0.8f,
    val vibrationEnabled: Boolean = true,
    val silent: Boolean = false,
    val enabled: Boolean = true,
    val vibrationPatternMs: List<Long> = listOf(0L, 140L),
    val cooldownMs: Long = 0L,
    val repeatCount: Int = 1,
    val allowDuringQuietHours: Boolean = false
)

enum class SoundMatchType(val label: String) {
    APP("App"), CATEGORY("Category"), PRIORITY("Priority"), FOCUS_MODE("Focus mode"), CONTACT("Contact"), KEYWORD("Matching rule")
}
