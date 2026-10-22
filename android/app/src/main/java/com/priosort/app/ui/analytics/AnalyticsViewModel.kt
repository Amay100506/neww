package com.priosort.app.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.ExactDuplicateAnalyzer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val totalCount: Int = 0,
    val feedbackCount: Int = 0,
    val burstCount: Int = 0,
    val manualOverrideCount: Int = 0,
    val otpCount: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val minimalCount: Int = 0,
    val transactionTotals: List<com.priosort.app.data.local.TransactionCurrencyTotal> = emptyList(),
    val recentTransactions: List<com.priosort.app.domain.NotificationRecord> = emptyList(),
    val dailyNotificationCounts: List<com.priosort.app.data.local.DailyNotificationCount> = emptyList(),
    val duplicateCount: Int = 0,
    val transactionSearch: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PrioSortApplication
    private val notificationRepo = app.notificationRepository
    private val _transactionSearch = MutableStateFlow("")
    val transactionSearch = _transactionSearch.asStateFlow()
    private val searchedTransactions = _transactionSearch.flatMapLatest(notificationRepo::searchTransactions)

    fun setTransactionSearch(query: String) { _transactionSearch.value = query }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        notificationRepo.totalCount,
        notificationRepo.otpCount,
        notificationRepo.burstCount,
        notificationRepo.manualOverrideCount,
        notificationRepo.getCountByPriority(PriorityLevel.CRITICAL),
        notificationRepo.getCountByPriority(PriorityLevel.HIGH),
        notificationRepo.getCountByPriority(PriorityLevel.MEDIUM),
        notificationRepo.getCountByPriority(PriorityLevel.LOW),
        notificationRepo.getCountByPriority(PriorityLevel.MINIMAL),
        notificationRepo.transactionCurrencyTotals,
        searchedTransactions,
        notificationRepo.dailyNotificationCounts,
        notificationRepo.duplicateAnalysisRecords,
        app.feedbackDao.observeCounts()
    ) { args: Array<Any> ->
        val total = args[0] as Int
        val otp = args[1] as Int
        val bursts = args[2] as Int
        val overrides = args[3] as Int
        val crit = args[4] as Int
        val hi = args[5] as Int
        val med = args[6] as Int
        val low = args[7] as Int
        val min = args[8] as Int
        @Suppress("UNCHECKED_CAST")
        val totals = args[9] as List<com.priosort.app.data.local.TransactionCurrencyTotal>
        @Suppress("UNCHECKED_CAST")
        val txns = args[10] as List<com.priosort.app.domain.NotificationRecord>
        @Suppress("UNCHECKED_CAST")
        val dailyCounts = args[11] as List<com.priosort.app.data.local.DailyNotificationCount>
        @Suppress("UNCHECKED_CAST")
        val duplicateRecords = args[12] as List<com.priosort.app.data.local.DuplicateNotificationContent>
        @Suppress("UNCHECKED_CAST")
        val feedbackCounts = args[13] as List<com.priosort.app.domain.NotificationFeedbackCount>

        AnalyticsUiState(
            totalCount = total,
            feedbackCount = feedbackCounts.sumOf { it.count },
            burstCount = bursts,
            manualOverrideCount = overrides,
            otpCount = otp,
            criticalCount = crit,
            highCount = hi,
            mediumCount = med,
            lowCount = low,
            minimalCount = min,
            transactionTotals = totals,
            recentTransactions = txns,
            dailyNotificationCounts = dailyCounts,
            duplicateCount = ExactDuplicateAnalyzer.redundantCopyCount(duplicateRecords),
            transactionSearch = _transactionSearch.value
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsUiState()
    )
}
