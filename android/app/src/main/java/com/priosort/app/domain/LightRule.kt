package com.priosort.app.domain

data class LightRule(
    val id: Long = 0,
    val name: String,
    val matchType: LightMatchType,
    val matchValue: String,
    val colorArgb: Int = 0xFFFF4F9A.toInt(),
    val pattern: LightPattern = LightPattern.PULSE,
    val repeatPolicy: LightRepeatPolicy = LightRepeatPolicy.UNTIL_TIMEOUT,
    val speed: Float = 1f,
    val durationMs: Long = 5_000,
    val intensity: Float = 0.8f,
    val edges: Set<LightEdge> = LightEdge.entries.toSet(),
    val enabled: Boolean = true
)

enum class LightMatchType { APP, CATEGORY, PRIORITY, CONTACT, KEYWORD }
enum class LightPattern { STATIC, PULSE, BREATHING, DOUBLE_FLASH, CHASE, WAVE_RAINBOW }
enum class LightRepeatPolicy { ONCE, TWICE, UNTIL_TIMEOUT }
enum class LightEdge { TOP, RIGHT, BOTTOM, LEFT }
