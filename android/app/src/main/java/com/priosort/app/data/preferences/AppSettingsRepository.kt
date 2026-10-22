package com.priosort.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.priosort.app.domain.FocusModeConfig
import com.priosort.app.domain.FocusModeType
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.prioSortDataStore by preferencesDataStore(name = "priosort_settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val soundEnabled: Boolean = false,
    val globalVolume: Float = 0.8f,
    val vibrationEnabled: Boolean = true,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: Int = 22 * 60,
    val quietHoursEnd: Int = 7 * 60,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val reduceMotion: Boolean = false,
    val fontScale: Float = 1f,
    val biometricLockEnabled: Boolean = false,
    val vipContacts: List<String> = emptyList(),
    val categoryPriorityOverrides: Map<String, String> = emptyMap(),
    val focusModeConfig: FocusModeConfig = FocusModeConfig(),
    val edgeLightsEnabled: Boolean = false
)

class AppSettingsRepository(private val context: Context) {
    private object Keys {
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val globalVolume = floatPreferencesKey("global_volume")
        val vibration = booleanPreferencesKey("vibration_enabled")
        val quietEnabled = booleanPreferencesKey("quiet_hours_enabled")
        val quietStart = intPreferencesKey("quiet_hours_start")
        val quietEnd = intPreferencesKey("quiet_hours_end")
        val theme = stringPreferencesKey("theme_mode")
        val reduceMotion = booleanPreferencesKey("reduce_motion")
        val fontScale = floatPreferencesKey("font_scale")
        val biometricLockEnabled = booleanPreferencesKey("biometric_lock_enabled")
        val vipContacts = stringPreferencesKey("vip_contacts")
        val categoryPriorities = stringPreferencesKey("category_priority_overrides")
        val focusMode = stringPreferencesKey("focus_mode")
        val focusAutoSchedule = booleanPreferencesKey("focus_auto_schedule")
        val workStartHour = intPreferencesKey("focus_work_start_hour")
        val workEndHour = intPreferencesKey("focus_work_end_hour")
        val sleepStartHour = intPreferencesKey("focus_sleep_start_hour")
        val sleepEndHour = intPreferencesKey("focus_sleep_end_hour")
        val edgeLightsEnabled = booleanPreferencesKey("edge_lights_enabled")
        val focusAllowedPackages = stringPreferencesKey("focus_allowed_packages")
        val focusAllowedCategories = stringPreferencesKey("focus_allowed_categories")
    }

    val settings: Flow<AppSettings> = context.prioSortDataStore.data.map { values ->
        AppSettings(
            soundEnabled = values[Keys.soundEnabled] ?: false,
            globalVolume = (values[Keys.globalVolume] ?: 0.8f).coerceIn(0f, 1f),
            vibrationEnabled = values[Keys.vibration] ?: true,
            quietHoursEnabled = values[Keys.quietEnabled] ?: false,
            quietHoursStart = (values[Keys.quietStart] ?: 22 * 60).coerceIn(0, 1439),
            quietHoursEnd = (values[Keys.quietEnd] ?: 7 * 60).coerceIn(0, 1439),
            themeMode = runCatching { ThemeMode.valueOf(values[Keys.theme] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM),
            reduceMotion = values[Keys.reduceMotion] ?: false,
            fontScale = (values[Keys.fontScale] ?: 1f).coerceIn(0.85f, 1.3f),
            biometricLockEnabled = values[Keys.biometricLockEnabled] ?: false,
            vipContacts = values[Keys.vipContacts].orEmpty().split('|').map(String::trim).filter(String::isNotBlank),
            categoryPriorityOverrides = values[Keys.categoryPriorities].orEmpty().split(';')
                .mapNotNull { entry -> entry.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
                .toMap(),
            focusModeConfig = FocusModeConfig(
                type = runCatching { FocusModeType.valueOf(values[Keys.focusMode] ?: "OFF") }.getOrDefault(FocusModeType.OFF),
                autoScheduleEnabled = values[Keys.focusAutoSchedule] ?: false,
                workStartHour = (values[Keys.workStartHour] ?: 9).coerceIn(0, 23),
                workEndHour = (values[Keys.workEndHour] ?: 18).coerceIn(0, 23),
                sleepStartHour = (values[Keys.sleepStartHour] ?: 22).coerceIn(0, 23),
                sleepEndHour = (values[Keys.sleepEndHour] ?: 7).coerceIn(0, 23),
                allowlistedPackageNames = values[Keys.focusAllowedPackages].orEmpty().split(';').map(String::trim).filter(String::isNotBlank).toSet(),
                allowlistedCategories = values[Keys.focusAllowedCategories].orEmpty().split(';').mapNotNull { value -> runCatching { com.priosort.app.domain.NotificationCategory.valueOf(value) }.getOrNull() }.toSet()
            ),
            edgeLightsEnabled = values[Keys.edgeLightsEnabled] ?: false
        )
    }

    suspend fun setSoundEnabled(value: Boolean) = context.prioSortDataStore.edit { it[Keys.soundEnabled] = value }
    suspend fun setGlobalVolume(value: Float) = context.prioSortDataStore.edit { it[Keys.globalVolume] = value.coerceIn(0f, 1f) }
    suspend fun setVibrationEnabled(value: Boolean) = context.prioSortDataStore.edit { it[Keys.vibration] = value }
    suspend fun setQuietHours(enabled: Boolean, start: Int, end: Int) = context.prioSortDataStore.edit {
        it[Keys.quietEnabled] = enabled
        it[Keys.quietStart] = start.coerceIn(0, 1439)
        it[Keys.quietEnd] = end.coerceIn(0, 1439)
    }
    suspend fun setThemeMode(value: ThemeMode) = context.prioSortDataStore.edit { it[Keys.theme] = value.name }
    suspend fun setReduceMotion(value: Boolean) = context.prioSortDataStore.edit { it[Keys.reduceMotion] = value }
    suspend fun setFontScale(value: Float) = context.prioSortDataStore.edit { it[Keys.fontScale] = value.coerceIn(0.85f, 1.3f) }
    suspend fun setBiometricLockEnabled(value: Boolean) = context.prioSortDataStore.edit { it[Keys.biometricLockEnabled] = value }
    suspend fun setVipContacts(value: List<String>) = context.prioSortDataStore.edit {
        it[Keys.vipContacts] = value.map(String::trim).filter(String::isNotBlank).distinct().joinToString("|")
    }
    suspend fun setCategoryPriority(category: String, priorityCode: String?) {
        val current = settings.first().categoryPriorityOverrides.toMutableMap()
        if (priorityCode.isNullOrBlank()) current.remove(category) else current[category] = priorityCode
        context.prioSortDataStore.edit { prefs ->
            prefs[Keys.categoryPriorities] = current.entries.joinToString(";") { "${it.key}=${it.value}" }
        }
    }

    suspend fun setFocusModeConfig(value: FocusModeConfig) = context.prioSortDataStore.edit {
        it[Keys.focusMode] = value.type.name
        it[Keys.focusAutoSchedule] = value.autoScheduleEnabled
        it[Keys.workStartHour] = value.workStartHour.coerceIn(0, 23)
        it[Keys.workEndHour] = value.workEndHour.coerceIn(0, 23)
        it[Keys.sleepStartHour] = value.sleepStartHour.coerceIn(0, 23)
        it[Keys.sleepEndHour] = value.sleepEndHour.coerceIn(0, 23)
        it[Keys.focusAllowedPackages] = value.allowlistedPackageNames.map(String::trim).filter(String::isNotBlank).distinct().joinToString(";")
        it[Keys.focusAllowedCategories] = value.allowlistedCategories.joinToString(";") { category -> category.name }
    }

    suspend fun setEdgeLightsEnabled(value: Boolean) = context.prioSortDataStore.edit {
        it[Keys.edgeLightsEnabled] = value
    }
}
