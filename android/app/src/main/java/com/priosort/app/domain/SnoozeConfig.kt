package com.priosort.app.domain

import java.util.Calendar

enum class SnoozePreset(val displayName: String, val durationMs: Long) {
    MINUTES_15("15 Minutes", 15 * 60 * 1000L),
    HOUR_1("1 Hour", 60 * 60 * 1000L),
    HOURS_3("3 Hours", 3 * 60 * 60 * 1000L),
    TOMORROW_MORNING("Tomorrow 9 AM", calculateTomorrowMorningMs())
}

private fun calculateTomorrowMorningMs(): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return cal.timeInMillis - System.currentTimeMillis()
}
