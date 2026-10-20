package com.priosort.app.ui.analytics

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.PriorityLevel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class AnalyticsUiState(
    val totalCount: Int = 0,
    val otpCount: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val mediumCount: Int = 0,
    val lowCount: Int = 0,
    val minimalCount: Int = 0,
    val totalDebits: Double = 0.0,
    val totalCredits: Double = 0.0,
    val recentTransactions: List<com.priosort.app.domain.NotificationRecord> = emptyList()
)

class AnalyticsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PrioSortApplication
    private val notificationRepo = app.notificationRepository

    val uiState: StateFlow<AnalyticsUiState> = combine(
        notificationRepo.totalCount,
        notificationRepo.otpCount,
        notificationRepo.getCountByPriority(PriorityLevel.CRITICAL),
        notificationRepo.getCountByPriority(PriorityLevel.HIGH),
        notificationRepo.getCountByPriority(PriorityLevel.MEDIUM),
        notificationRepo.getCountByPriority(PriorityLevel.LOW),
        notificationRepo.getCountByPriority(PriorityLevel.MINIMAL),
        notificationRepo.totalDebits,
        notificationRepo.totalCredits,
        notificationRepo.recentTransactions
    ) { args: Array<Any> ->
        val total = args[0] as Int
        val otp = args[1] as Int
        val crit = args[2] as Int
        val hi = args[3] as Int
        val med = args[4] as Int
        val low = args[5] as Int
        val min = args[6] as Int
        val debits = args[7] as Double
        val credits = args[8] as Double
        @Suppress("UNCHECKED_CAST")
        val txns = args[9] as List<com.priosort.app.domain.NotificationRecord>

        AnalyticsUiState(
            totalCount = total,
            otpCount = otp,
            criticalCount = crit,
            highCount = hi,
            mediumCount = med,
            lowCount = low,
            minimalCount = min,
            totalDebits = debits,
            totalCredits = credits,
            recentTransactions = txns
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AnalyticsUiState()
    )
}
