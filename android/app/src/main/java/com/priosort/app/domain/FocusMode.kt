package com.priosort.app.domain

import java.util.Calendar

/**
 * Focus Mode types for time-of-day contextual prioritization.
 */
enum class FocusModeType(val displayName: String, val description: String) {
    OFF("Standard", "Default prioritization policy"),
    WORK("Work Mode", "Prioritize Work & Education; suppress Promotional & Social"),
    SLEEP("Sleep / DND", "Strict filter: Only Critical P0 (OTPs & Fraud) alert"),
    PERSONAL("Personal / Leisure", "Prioritize Personal & Social; demote Work")
}

data class FocusModeConfig(
    val type: FocusModeType = FocusModeType.OFF,
    val autoScheduleEnabled: Boolean = false,
    val workStartHour: Int = 9,
    val workEndHour: Int = 18,
    val sleepStartHour: Int = 22,
    val sleepEndHour: Int = 7
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
