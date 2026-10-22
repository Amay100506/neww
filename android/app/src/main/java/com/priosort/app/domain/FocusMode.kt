package com.priosort.app.domain

import java.util.Calendar

/**
 * Focus Mode types for time-of-day contextual prioritization.
 */
enum class FocusModeType(val displayName: String, val description: String) {
    OFF("Off", "Use your regular priority settings"),
    STUDY("Study", "Raise study and education alerts; lower promotions and casual messages"),
    WORK("Work", "Raise work alerts; lower promotions and casual messages"),
    SLEEP("Sleep", "Lower non-critical alerts while preserving Critical alerts"),
    PERSONAL("Personal", "Raise personal messages and lower work alerts"),
    CUSTOM("Custom", "Use your saved category priority preferences")
}

data class FocusModeConfig(
    val type: FocusModeType = FocusModeType.OFF,
    val autoScheduleEnabled: Boolean = false,
    val workStartHour: Int = 9,
    val workEndHour: Int = 18,
    val sleepStartHour: Int = 22,
    val sleepEndHour: Int = 7,
    val allowlistedPackageNames: Set<String> = emptySet(),
    val allowlistedCategories: Set<NotificationCategory> = emptySet()
) {
    /**
     * Resolves the active focus mode based on current time if auto-scheduling is enabled.
     */
    fun resolveActiveMode(currentHour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): FocusModeType {
        if (!autoScheduleEnabled) return type

        return when {
            sleepStartHour > sleepEndHour -> {
                // E.g. 22:00 to 07:00 wraps around midnight
                if (currentHour >= sleepStartHour || currentHour < sleepEndHour) FocusModeType.SLEEP
                else if (currentHour in workStartHour until workEndHour) FocusModeType.WORK
                else FocusModeType.PERSONAL
            }
            else -> {
                if (currentHour in sleepStartHour until sleepEndHour) FocusModeType.SLEEP
                else if (currentHour in workStartHour until workEndHour) FocusModeType.WORK
                else FocusModeType.PERSONAL
            }
        }
    }
}
