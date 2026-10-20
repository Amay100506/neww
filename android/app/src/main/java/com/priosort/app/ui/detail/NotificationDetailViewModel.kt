package com.priosort.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PrioSortApplication
    private val notificationRepo = app.notificationRepository
    private val preferencesRepo = app.userPreferencesRepository

    private val _notification = MutableStateFlow<NotificationRecord?>(null)
    val notification: StateFlow<NotificationRecord?> = _notification.asStateFlow()

    fun loadNotification(id: Long) {
        viewModelScope.launch {
            _notification.value = notificationRepo.getNotificationById(id)
        }
    }

    fun updatePriority(newLevel: PriorityLevel) {
        val current = _notification.value ?: return
        viewModelScope.launch {
            notificationRepo.overridePriority(current.id, newLevel, "User manual priority override to ${newLevel.displayName}")
            _notification.value = notificationRepo.getNotificationById(current.id)
        }
    }

    fun updateCategory(newCategory: NotificationCategory) {
        val current = _notification.value ?: return
        viewModelScope.launch {
            notificationRepo.overrideCategory(current.id, newCategory)
            _notification.value = notificationRepo.getNotificationById(current.id)
        }
    }

    fun togglePin() {
        val current = _notification.value ?: return
        viewModelScope.launch {
            notificationRepo.setPinned(current.id, !current.isPinned)
            _notification.value = notificationRepo.getNotificationById(current.id)
        }
    }

    fun excludeApplication() {
        val current = _notification.value ?: return
        viewModelScope.launch {
            preferencesRepo.setPackageExcluded(current.packageName, current.applicationName, true)
        }
    }

    fun deleteNotification(onDeleted: () -> Unit) {
        val current = _notification.value ?: return
        viewModelScope.launch {
            notificationRepo.deleteNotification(current.id)
            onDeleted()
        }
    }
}
