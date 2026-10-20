package com.priosort.app.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.data.local.entities.PackageSettingEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isCapturePaused: Boolean = false,
    val isRedactionEnabled: Boolean = true,
    val retentionDays: Int = 7,
    val packageSettings: List<PackageSettingEntity> = emptyList()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PrioSortApplication
    private val preferencesRepo = app.userPreferencesRepository
    private val notificationRepo = app.notificationRepository

    val uiState: StateFlow<SettingsUiState> = combine(
        preferencesRepo.isCapturePaused,
        preferencesRepo.isRedactionEnabled,
        preferencesRepo.retentionDays,
        preferencesRepo.packageSettings
    ) { paused, redaction, retention, packages ->
        SettingsUiState(
            isCapturePaused = paused,
            isRedactionEnabled = redaction,
            retentionDays = retention,
            packageSettings = packages
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SettingsUiState()
    )

    fun toggleCapturePaused(paused: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setCapturePaused(paused)
        }
    }

    fun toggleRedaction(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setRedactionEnabled(enabled)
        }
    }

    fun setRetentionDays(days: Int) {
        viewModelScope.launch {
            preferencesRepo.setRetentionDays(days)
        }
    }

    fun setPackageImportance(packageName: String, importance: Float) {
        viewModelScope.launch {
            preferencesRepo.setPackageImportance(packageName, null, importance)
        }
    }

    fun togglePackageExcluded(packageName: String, isExcluded: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setPackageExcluded(packageName, null, isExcluded)
        }
    }

    fun clearLocalDatabase() {
        viewModelScope.launch {
            notificationRepo.clearAllNotifications()
        }
    }
}
