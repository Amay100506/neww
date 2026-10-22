package com.priosort.app.ui.sound

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.data.preferences.AppSettings
import com.priosort.app.domain.SoundRule
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SoundStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrioSortApplication
    val rules: StateFlow<List<SoundRule>> = app.soundRuleRepository.rules.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val settings: StateFlow<AppSettings> = app.appSettingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )
    val observedApps = app.notificationRepository.activeNotifications.map { records ->
        records.distinctBy { it.packageName }.map { it.packageName to (it.applicationName ?: it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveRule(rule: SoundRule) = viewModelScope.launch { app.soundRuleRepository.save(rule) }
    fun toggleRule(rule: SoundRule, enabled: Boolean) = viewModelScope.launch { app.soundRuleRepository.setEnabled(rule, enabled) }
    fun deleteRule(id: Long) = viewModelScope.launch { app.soundRuleRepository.delete(id) }
    fun setSoundsEnabled(enabled: Boolean) = viewModelScope.launch { app.appSettingsRepository.setSoundEnabled(enabled) }
    fun setVolume(volume: Float) = viewModelScope.launch { app.appSettingsRepository.setGlobalVolume(volume) }
    fun setVibration(enabled: Boolean) = viewModelScope.launch { app.appSettingsRepository.setVibrationEnabled(enabled) }
    fun setQuietHours(enabled: Boolean, start: Int, end: Int) = viewModelScope.launch {
        app.appSettingsRepository.setQuietHours(enabled, start, end)
    }
    fun resetDefaults() = viewModelScope.launch {
        app.soundRuleRepository.clear()
        app.appSettingsRepository.setSoundEnabled(false)
        app.appSettingsRepository.setGlobalVolume(0.8f)
        app.appSettingsRepository.setVibrationEnabled(true)
        app.appSettingsRepository.setQuietHours(false, 22 * 60, 7 * 60)
    }
}
