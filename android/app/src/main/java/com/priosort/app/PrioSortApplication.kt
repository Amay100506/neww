package com.priosort.app

import android.app.Application
import androidx.work.Configuration
import com.priosort.app.data.local.PrioSortDatabase
import com.priosort.app.data.preferences.AppSettingsRepository
import com.priosort.app.data.preferences.UserPreferencesRepository
import com.priosort.app.data.repository.NotificationRepository
import com.priosort.app.data.repository.NotificationRetentionWorker
import com.priosort.app.data.repository.SoundRuleRepository
import com.priosort.app.data.repository.LightRuleRepository
import kotlinx.coroutines.flow.first

class PrioSortApplication : Application(), Configuration.Provider {

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    val database by lazy { PrioSortDatabase.getInstance(this) }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }
    val userPreferencesRepository by lazy { UserPreferencesRepository(database.preferenceDao()) }
    val soundRuleRepository by lazy { SoundRuleRepository(database.soundRuleDao()) }
    val lightRuleRepository by lazy { LightRuleRepository(database.lightRuleDao()) }
    val feedbackDao by lazy { database.feedbackDao() }
    val edgeLightOverlayController by lazy { com.priosort.app.platform.EdgeLightOverlayController(this) }
    val appSettingsRepository by lazy { AppSettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        // Ensure the private database parent exists before startup work can race first Room access.
        getDatabasePath("priosort_database").parentFile?.let { directory ->
            if (!directory.exists()) directory.mkdirs()
        }
        NotificationRetentionWorker.schedule(this)
    }

    suspend fun runMaintenance() {
        val now = System.currentTimeMillis()
        notificationRepository.cleanupExpiredRecords(now)
        notificationRepository.unsnoozeExpired(now)
        if (userPreferencesRepository.isRedactionEnabled.first()) {
            notificationRepository.redactExistingSensitiveNotifications()
        }
        database.notificationDao().deleteSyntheticNotifications()
    }
}
