package com.priosort.app.ui.inbox

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.capture.CaptureStateManager
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.sorting.NotificationSorter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmartInboxUiState(
    val notifications: List<NotificationRecord> = emptyList(),
    val accessGranted: Boolean = false,
    val serviceConnected: Boolean = false,
    val disclosureAccepted: Boolean = false,
    val isCapturePaused: Boolean = false,
    val isLoading: Boolean = true,
    val searchText: String = "",
    val selectedPriority: PriorityLevel? = null,
    val selectedCategory: NotificationCategory? = null,
    val selectedApp: String? = null,
    val availableApps: List<Pair<String, String>> = emptyList()
)

class SmartInboxViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrioSortApplication
    private val query = MutableStateFlow("")
    private val priorityFilter = MutableStateFlow<PriorityLevel?>(null)
    private val categoryFilter = MutableStateFlow<NotificationCategory?>(null)
    private val appFilter = MutableStateFlow<String?>(null)
    private val loaded = MutableStateFlow(false)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { app.runMaintenance() }
        }
    }

    private val baseState = combine(
        app.notificationRepository.activeNotifications.onEach { loaded.value = true },
        CaptureStateManager.status,
        app.userPreferencesRepository.hasAcceptedNotificationDisclosure,
        app.userPreferencesRepository.isCapturePaused,
        loaded
    ) { notifications, capture, disclosureAccepted, paused, hasLoaded ->
        SmartInboxUiState(
            notifications = NotificationSorter.sortNotifications(notifications),
            accessGranted = capture.accessGranted,
            serviceConnected = capture.serviceConnected,
            disclosureAccepted = disclosureAccepted,
            isCapturePaused = paused,
            isLoading = !hasLoaded
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartInboxUiState())

    private val filteredState = combine(baseState, query, priorityFilter, categoryFilter, appFilter) { base, q, priority, category, app ->
        base.copy(
            notifications = base.notifications.filter { item ->
                (q.isBlank() || listOfNotNull(item.title, item.body, item.applicationName, item.packageName)
                    .any { it.contains(q, ignoreCase = true) }) &&
                    (priority == null || item.priorityLevel == priority) &&
                    (category == null || item.category == category) &&
                    (app == null || item.packageName == app)
            },
            searchText = q,
            selectedPriority = priority,
            selectedCategory = category,
            selectedApp = app
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartInboxUiState())

    val uiState: StateFlow<SmartInboxUiState> = combine(
        filteredState,
        app.notificationRepository.activeNotifications.map { records -> records.distinctBy { it.packageName }.map { it.packageName to (it.applicationName ?: it.packageName) } }
    ) { state, apps -> state.copy(availableApps = apps) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SmartInboxUiState())

    fun refreshCaptureStatus() = CaptureStateManager.refreshPermission(getApplication())

    suspend fun acceptNotificationDisclosure() {
        app.userPreferencesRepository.acceptNotificationDisclosure()
    }

    fun setSearch(value: String) { query.value = value }
    fun setPriorityFilter(value: PriorityLevel?) { priorityFilter.value = value }
    fun setCategoryFilter(value: NotificationCategory?) { categoryFilter.value = value }
    fun setAppFilter(value: String?) { appFilter.value = value }
    fun setRead(id: Long, isRead: Boolean) = viewModelScope.launch { app.notificationRepository.setRead(id, isRead) }
    fun togglePinned(item: NotificationRecord) = viewModelScope.launch {
        app.notificationRepository.setPinned(item.id, !item.isPinned)
    }
    fun snooze(id: Long, durationMs: Long) = viewModelScope.launch {
        app.notificationRepository.snoozeNotification(id, System.currentTimeMillis() + durationMs)
    }
}
