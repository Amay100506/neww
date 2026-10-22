package com.priosort.app.ui.light

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.data.preferences.AppSettings
import com.priosort.app.domain.LightRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LightStudioViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrioSortApplication
    val rules: StateFlow<List<LightRule>> = app.lightRuleRepository.rules.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList()
    )
    val settings: StateFlow<AppSettings> = app.appSettingsRepository.settings.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings()
    )
    val observedApps = app.notificationRepository.activeNotifications.map { records ->
        records.distinctBy { it.packageName }.map { it.packageName to (it.applicationName ?: it.packageName) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveRule(rule: LightRule) = viewModelScope.launch { app.lightRuleRepository.save(rule) }
    fun toggleRule(rule: LightRule, enabled: Boolean) = viewModelScope.launch { app.lightRuleRepository.setEnabled(rule.id, enabled) }
    fun deleteRule(id: Long) = viewModelScope.launch { app.lightRuleRepository.delete(id) }
    fun setEnabled(enabled: Boolean) = viewModelScope.launch { app.appSettingsRepository.setEdgeLightsEnabled(enabled) }
    fun preview(rule: LightRule) = viewModelScope.launch(Dispatchers.Main.immediate) { app.edgeLightOverlayController.show(rule) }
    fun stopPreview() = app.edgeLightOverlayController.stop()
    fun canDrawOverlay(): Boolean = app.edgeLightOverlayController.canDrawOverlay()
}
