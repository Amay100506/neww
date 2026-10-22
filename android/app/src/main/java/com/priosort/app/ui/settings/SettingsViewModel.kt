package com.priosort.app.ui.settings

import android.app.Application
import androidx.room.withTransaction
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.capture.CaptureStateManager
import com.priosort.app.data.local.entities.CustomRuleEntity
import com.priosort.app.data.local.entities.PackageSettingEntity
import com.priosort.app.data.export.RuleBackupCodec
import com.priosort.app.data.export.RuleBackupPreview
import com.priosort.app.data.preferences.AppSettings
import com.priosort.app.data.preferences.ThemeMode
import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.FocusModeConfig
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.RuleBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isCapturePaused: Boolean = false,
    val isRedactionEnabled: Boolean = true,
    val retentionDays: Int = 7,
    val packageSettings: List<PackageSettingEntity> = emptyList(),
    val disclosureAccepted: Boolean = false,
    val captureAccessGranted: Boolean = false,
    val serviceConnected: Boolean = false,
    val appSettings: AppSettings = AppSettings(),
    val customRules: List<CustomRule> = emptyList()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrioSortApplication
    private val preferencesRepo = app.userPreferencesRepository
    private val notificationRepo = app.notificationRepository
    private val _search = MutableStateFlow("")
    val search: StateFlow<String> = _search.asStateFlow()

    private val baseState = combine(
        preferencesRepo.isCapturePaused,
        preferencesRepo.isRedactionEnabled,
        preferencesRepo.retentionDays,
        preferencesRepo.packageSettings,
        preferencesRepo.hasAcceptedNotificationDisclosure
    ) { paused, redaction, retention, packages, disclosureAccepted ->
        SettingsUiState(
            isCapturePaused = paused,
            isRedactionEnabled = redaction,
            retentionDays = retention,
            packageSettings = packages,
            disclosureAccepted = disclosureAccepted
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    val uiState: StateFlow<SettingsUiState> = combine(
        baseState,
        app.appSettingsRepository.settings,
        CaptureStateManager.status,
        app.database.customRuleDao().getAllRules().map { rows -> rows.map(CustomRuleEntity::toDomain) }
    ) { base, appSettings, capture, customRules ->
        base.copy(
            appSettings = appSettings,
            captureAccessGranted = capture.accessGranted,
            serviceConnected = capture.serviceConnected,
            customRules = customRules
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setSearch(value: String) { _search.value = value }
    fun refreshCaptureStatus() = CaptureStateManager.refreshPermission(getApplication())
    fun toggleCapturePaused(paused: Boolean) = viewModelScope.launch { preferencesRepo.setCapturePaused(paused) }
    fun toggleRedaction(enabled: Boolean) = viewModelScope.launch {
        preferencesRepo.setRedactionEnabled(enabled)
        if (enabled) notificationRepo.redactExistingSensitiveNotifications()
    }
    suspend fun acceptNotificationDisclosure() = preferencesRepo.acceptNotificationDisclosure()
    fun setRetentionDays(days: Int) = viewModelScope.launch { preferencesRepo.setRetentionDays(days) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { app.appSettingsRepository.setThemeMode(mode) }
    fun setReduceMotion(enabled: Boolean) = viewModelScope.launch { app.appSettingsRepository.setReduceMotion(enabled) }
    fun setFontScale(scale: Float) = viewModelScope.launch { app.appSettingsRepository.setFontScale(scale) }
    fun setBiometricLockEnabled(enabled: Boolean) = viewModelScope.launch { app.appSettingsRepository.setBiometricLockEnabled(enabled) }
    fun setVipContacts(names: List<String>) = viewModelScope.launch { app.appSettingsRepository.setVipContacts(names) }
    fun setCategoryPriority(category: String, priority: PriorityLevel?) = viewModelScope.launch {
        app.appSettingsRepository.setCategoryPriority(category, priority?.code)
    }
    fun setFocusModeConfig(config: FocusModeConfig) = viewModelScope.launch {
        app.appSettingsRepository.setFocusModeConfig(config)
    }
    fun togglePackageExcluded(packageName: String, isExcluded: Boolean) = viewModelScope.launch {
        preferencesRepo.setPackageExcluded(packageName, null, isExcluded)
    }
    fun addCustomRule(rule: CustomRule) = viewModelScope.launch {
        app.database.customRuleDao().insertRule(CustomRuleEntity.fromDomain(rule))
    }
    fun deleteCustomRule(id: Long) = viewModelScope.launch { app.database.customRuleDao().deleteRule(id) }
    fun clearLocalDatabase() = viewModelScope.launch { notificationRepo.clearAllNotifications() }

    suspend fun exportRulesJson(): String {
        val soundRules = app.soundRuleRepository.rules.first()
        val priorityRules = app.database.customRuleDao().getAllRules().first().map(CustomRuleEntity::toDomain)
        val lightRules = app.lightRuleRepository.rules.first()
        return RuleBackupCodec.encode(RuleBackup(soundRules = soundRules, priorityRules = priorityRules, lightRules = lightRules))
    }

    fun previewRulesJson(json: String): RuleBackupPreview = RuleBackupCodec.preview(json)

    suspend fun importRulesJson(json: String) {
        val backup = RuleBackupCodec.decodeForImport(json)
        val soundRules = backup.soundRules
        val priorityRules = backup.priorityRules
        val lightRules = backup.lightRules
        app.database.withTransaction {
            app.soundRuleRepository.clear()
            app.database.customRuleDao().deleteAllRules()
            app.lightRuleRepository.clear()
            soundRules.forEach { app.soundRuleRepository.save(it.copy(id = 0)) }
            priorityRules.forEach { app.database.customRuleDao().insertRule(CustomRuleEntity.fromDomain(it.copy(id = 0))) }
            lightRules.forEach { app.lightRuleRepository.save(it.copy(id = 0)) }
        }
    }
}
