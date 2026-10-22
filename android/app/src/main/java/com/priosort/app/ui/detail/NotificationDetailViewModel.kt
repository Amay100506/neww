package com.priosort.app.ui.detail

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.NotificationFeedbackType
import com.priosort.app.domain.CustomRule
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
    private val _feedbackSent = MutableStateFlow<NotificationFeedbackType?>(null)
    val feedbackSent: StateFlow<NotificationFeedbackType?> = _feedbackSent.asStateFlow()
    private val _appImportance = MutableStateFlow(50f)
    val appImportance: StateFlow<Float> = _appImportance.asStateFlow()
    private var categoryCorrectedForFeedback = false

    fun loadNotification(id: Long) {
        viewModelScope.launch {
            categoryCorrectedForFeedback = false
            _feedbackSent.value = null
            _notification.value = null
            _notification.value = notificationRepo.getNotificationById(id)
            _notification.value?.let { _appImportance.value = preferencesRepo.getPackageImportance(it.packageName) }
        }
    }

    fun updateAppImportance(value: Float) {
        _appImportance.value = value.coerceIn(0f, 100f)
    }

    fun saveAppImportance() {
        val current = _notification.value ?: return
        val bounded = _appImportance.value.coerceIn(0f, 100f)
        viewModelScope.launch {
            preferencesRepo.setPackageImportance(current.packageName, current.applicationName, bounded)
        }
    }

    fun updatePriority(newLevel: PriorityLevel) {
        val current = _notification.value ?: return
        _notification.value = current.copy(
            priorityLevel = newLevel,
            isManuallyOverridden = true,
            priorityOverrideReason = "User manual priority override to ${newLevel.displayName}"
        )
        viewModelScope.launch {
            notificationRepo.overridePriority(current.id, newLevel, "User manual priority override to ${newLevel.displayName}")
            _notification.value = notificationRepo.getNotificationById(current.id)
        }
    }

    fun updateCategory(newCategory: NotificationCategory) {
        val current = _notification.value ?: return
        categoryCorrectedForFeedback = true
        _notification.value = current.copy(category = newCategory, isManuallyOverridden = true)
        viewModelScope.launch {
            notificationRepo.overrideCategory(current.id, newCategory)
            _notification.value = notificationRepo.getNotificationById(current.id)
        }
    }

    fun sendFeedback(type: NotificationFeedbackType) {
        val current = _notification.value ?: return
        viewModelScope.launch {
            app.feedbackDao.insert(com.priosort.app.data.local.entities.NotificationFeedbackEntity(
                notificationId = current.id,
                feedbackType = type.name
            ))
            _feedbackSent.value = type
        }
    }

    fun suggestedRule(type: NotificationFeedbackType): CustomRule? {
        val current = _notification.value ?: return null
        if (current.packageName.isBlank()) return null
        val suggestedPriority = when (type) {
            NotificationFeedbackType.TOO_HIGH -> PriorityLevel.entries.firstOrNull { it.rank == current.priorityLevel.rank - 1 }
            NotificationFeedbackType.TOO_LOW -> PriorityLevel.entries.firstOrNull { it.rank == current.priorityLevel.rank + 1 }
            NotificationFeedbackType.WRONG_CATEGORY -> null
            NotificationFeedbackType.WRONG_RULE -> current.priorityLevel.takeIf { current.isManuallyOverridden }
        }
        val suggestedCategory = if (type == NotificationFeedbackType.WRONG_CATEGORY && categoryCorrectedForFeedback) current.category else null
        if (suggestedPriority == null && suggestedCategory == null) return null
        return CustomRule(
            ruleName = "Suggested for ${current.applicationName ?: current.packageName}",
            keywordPattern = current.packageName,
            targetField = "PACKAGE",
            assignedPriority = suggestedPriority,
            assignedCategory = suggestedCategory
        )
    }

    fun saveSuggestedRule(rule: CustomRule) {
        viewModelScope.launch {
            app.database.customRuleDao().insertRule(com.priosort.app.data.local.entities.CustomRuleEntity.fromDomain(rule))
            categoryCorrectedForFeedback = false
        }
    }

    fun scheduleReminder(notificationId: Long, remindAtMs: Long) {
        viewModelScope.launch {
            if (notificationRepo.getNotificationById(notificationId) == null) return@launch
            com.priosort.app.data.repository.NotificationReminderScheduler.schedule(
                getApplication(), notificationId, remindAtMs
            )
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
