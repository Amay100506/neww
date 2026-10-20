package com.priosort.app.data.preferences

import com.priosort.app.data.local.PreferenceDao
import com.priosort.app.data.local.entities.PackageSettingEntity
import com.priosort.app.data.local.entities.UserPreferenceEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class UserPreferencesRepository(private val preferenceDao: PreferenceDao) {

    val isCapturePaused: Flow<Boolean> = preferenceDao.observePreference("capture_paused")
        .map { it?.value?.toBoolean() ?: false }

    val isRedactionEnabled: Flow<Boolean> = preferenceDao.observePreference("redaction_enabled")
        .map { it?.value?.toBoolean() ?: true }

    val retentionDays: Flow<Int> = preferenceDao.observePreference("retention_days")
        .map { it?.value?.toIntOrNull() ?: 7 }

    val packageSettings: Flow<List<PackageSettingEntity>> = preferenceDao.getAllPackageSettings()

    suspend fun setCapturePaused(paused: Boolean) {
        preferenceDao.setPreference(UserPreferenceEntity("capture_paused", paused.toString()))
    }

    suspend fun setRedactionEnabled(enabled: Boolean) {
        preferenceDao.setPreference(UserPreferenceEntity("redaction_enabled", enabled.toString()))
    }

    suspend fun setRetentionDays(days: Int) {
        preferenceDao.setPreference(UserPreferenceEntity("retention_days", days.toString()))
    }

    suspend fun setPackageImportance(packageName: String, appName: String?, importance: Float) {
        val existing = preferenceDao.getPackageSetting(packageName)
        val updated = existing?.copy(importanceScore = importance, applicationName = appName ?: existing.applicationName)
            ?: PackageSettingEntity(packageName, appName, importanceScore = importance)
        preferenceDao.setPackageSetting(updated)
    }

    suspend fun setPackageExcluded(packageName: String, appName: String?, isExcluded: Boolean) {
        val existing = preferenceDao.getPackageSetting(packageName)
        val updated = existing?.copy(isExcluded = isExcluded, applicationName = appName ?: existing.applicationName)
            ?: PackageSettingEntity(packageName, appName, isExcluded = isExcluded)
        preferenceDao.setPackageSetting(updated)
    }

    suspend fun isPackageExcluded(packageName: String): Boolean {
        return preferenceDao.getPackageSetting(packageName)?.isExcluded ?: false
    }

    suspend fun getPackageImportance(packageName: String): Float {
        return preferenceDao.getPackageSetting(packageName)?.importanceScore ?: 50.0f
    }
}
