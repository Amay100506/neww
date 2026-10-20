package com.priosort.app.ui.inbox

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.capture.CaptureStateManager
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.ScoreBreakdown
import com.priosort.app.intelligence.ExplanationBuilder
import com.priosort.app.sorting.NotificationSorter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SmartInboxUiState(
    val notifications: List<NotificationRecord> = emptyList(),
    val totalCount: Int = 0,
    val criticalCount: Int = 0,
    val highCount: Int = 0,
    val searchQuery: String = "",
    val selectedPriority: PriorityLevel? = null,
    val selectedCategory: NotificationCategory? = null,
    val isAccessGranted: Boolean = false,
    val isServiceConnected: Boolean = false,
    val isGroupedView: Boolean = false,
    val activeFocusMode: com.priosort.app.domain.FocusModeType = com.priosort.app.domain.FocusModeType.OFF
)

class SmartInboxViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as PrioSortApplication
    private val notificationRepo = app.notificationRepository

    private val searchQuery = MutableStateFlow("")
    private val selectedPriority = MutableStateFlow<PriorityLevel?>(null)
    private val selectedCategory = MutableStateFlow<NotificationCategory?>(null)
    private val isGroupedView = MutableStateFlow(false)
    private val activeFocusMode = MutableStateFlow(com.priosort.app.domain.FocusModeType.OFF)

    val uiState: StateFlow<SmartInboxUiState> = combine(
        notificationRepo.activeNotifications,
        searchQuery,
        selectedPriority,
        selectedCategory,
        CaptureStateManager.isServiceConnected,
        isGroupedView,
        activeFocusMode
    ) { notifications, query, prioFilter, catFilter, serviceConnected, grouped, focusMode ->
        val accessGranted = CaptureStateManager.isNotificationAccessGranted(getApplication())

        // 1. Dynamic sorting according to PrioSort Reordering Algorithm (Section 13)
        val sortedList = NotificationSorter.sortNotifications(notifications)

        // 2. Filter by search query, priority, and category
        val filteredList = sortedList.filter { record ->
            val matchesQuery = query.isBlank() ||
                    (record.title?.contains(query, ignoreCase = true) == true) ||
                    (record.body?.contains(query, ignoreCase = true) == true) ||
                    (record.applicationName?.contains(query, ignoreCase = true) == true)

            val matchesPriority = prioFilter == null || record.priorityLevel == prioFilter
            val matchesCategory = catFilter == null || record.category == catFilter

            matchesQuery && matchesPriority && matchesCategory
        }

        val critCount = notifications.count { it.priorityLevel == PriorityLevel.CRITICAL }
        val hiCount = notifications.count { it.priorityLevel == PriorityLevel.HIGH }

        SmartInboxUiState(
            notifications = filteredList,
            totalCount = notifications.size,
            criticalCount = critCount,
            highCount = hiCount,
            searchQuery = query,
            selectedPriority = prioFilter,
            selectedCategory = catFilter,
            isAccessGranted = accessGranted,
            isServiceConnected = serviceConnected,
            isGroupedView = grouped,
            activeFocusMode = focusMode
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SmartInboxUiState()
    )

    fun toggleGroupedView() {
        isGroupedView.value = !isGroupedView.value
    }

    fun setFocusMode(mode: com.priosort.app.domain.FocusModeType) {
        activeFocusMode.value = mode
    }

    /**
     * Injects an arbitrary test notification into the system and evaluates priority in real-time.
     */
    fun injectCustomNotification(
        title: String,
        body: String,
        appName: String,
        packageName: String
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val combined = "$title - $body"

            val otpRes = com.priosort.app.intelligence.OtpDetector.detectOtp(combined)
            val finType = com.priosort.app.intelligence.BankingClassifier.classifyFinancialEvent(combined)
            val classification = com.priosort.app.intelligence.NotificationClassifier.classify(
                text = combined,
                packageName = packageName,
                isOtp = otpRes.isOtp,
                financialType = finType
            )

            val actionRes = com.priosort.app.intelligence.ActionDetector.detectAction(
                text = combined,
                isOtp = otpRes.isOtp,
                isFraudOrFail = (finType == com.priosort.app.intelligence.BankingClassifier.FinancialEventType.FRAUD_ALERT ||
                        finType == com.priosort.app.intelligence.BankingClassifier.FinancialEventType.PAYMENT_FAILURE)
            )

            val deadlineRes = com.priosort.app.intelligence.DeadlineDetector.detectDeadline(combined)
            val engine = com.priosort.app.intelligence.PriorityEngine()

            val decision = engine.evaluate(
                category = classification.category,
                isOtp = otpRes.isOtp,
                financialType = finType,
                actionResult = actionRes,
                deadlineResult = deadlineRes,
                userAppImportance = 50f,
                receivedAtMs = now
            )

            val explanationJson = com.priosort.app.intelligence.ExplanationBuilder.buildJson(
                category = classification.category,
                level = decision.priorityLevel,
                score = decision.scoreBreakdown,
                isOverridden = decision.isOverridden,
                overrideReason = decision.overrideReason,
                explanations = decision.explanations
            )

            val record = NotificationRecord(
                eventKey = "custom_${System.currentTimeMillis()}",
                packageName = packageName,
                applicationName = appName,
                title = title,
                body = body,
                normalizedText = combined,
                category = classification.category,
                categoryConfidence = classification.confidence,
                priorityLevel = decision.priorityLevel,
                basePriorityScore = decision.scoreBreakdown.baseScore,
                urgencyScore = decision.scoreBreakdown.urgencyScore,
                actionScore = decision.scoreBreakdown.actionScore,
                deadlineScore = decision.scoreBreakdown.deadlineScore,
                importanceScore = decision.scoreBreakdown.importanceScore,
                recencyScore = decision.scoreBreakdown.recencyScore,
                otpDetected = otpRes.isOtp,
                actionDetected = actionRes.hasAction,
                actionDescription = actionRes.description,
                deadlineText = deadlineRes.deadlineText,
                deadlineAt = null,
                deadlineResolutionStatus = deadlineRes.resolutionStatus,
                priorityOverrideReason = decision.overrideReason,
                explanationJson = explanationJson,
                receivedAt = now,
                processedAt = now
            )

            notificationRepo.saveNotification(record)
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        searchQuery.value = newQuery
    }

    fun onPriorityFilterSelected(priority: PriorityLevel?) {
        selectedPriority.value = priority
    }

    fun onCategoryFilterSelected(category: NotificationCategory?) {
        selectedCategory.value = category
    }

    fun togglePin(notification: NotificationRecord) {
        viewModelScope.launch {
            notificationRepo.setPinned(notification.id, !notification.isPinned)
        }
    }

    fun snoozeNotification(notification: NotificationRecord, untilMs: Long) {
        viewModelScope.launch {
            notificationRepo.snoozeNotification(notification.id, untilMs)
        }
    }

    fun archiveNotification(notification: NotificationRecord) {
        viewModelScope.launch {
            notificationRepo.setArchived(notification.id, true)
        }
    }

    fun deleteNotification(notification: NotificationRecord) {
        viewModelScope.launch {
            notificationRepo.deleteNotification(notification.id)
        }
    }

    /**
     * Seeds the 5 notifications from the Executive Summary (Section 1.3)
     * Arrival 1: Flipkart sale (earliest)
     * Arrival 2: WhatsApp casual message
     * Arrival 3: HDFC bank transaction
     * Arrival 4: College assignment deadline
     * Arrival 5: Banking OTP (latest)
     */
    fun simulateExecutiveSummaryScenario() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val minMs = 60 * 1000L

            val notifs = listOf(
                NotificationRecord(
                    eventKey = "sim_flipkart_${System.currentTimeMillis()}",
                    packageName = "com.flipkart.android",
                    applicationName = "Flipkart",
                    title = "Shopping Sale: Flat 50% Off",
                    body = "Mega sale on electronics and shoes. Limited period offer, shop now!",
                    normalizedText = "Shopping Sale: Flat 50% Off - Mega sale on electronics and shoes.",
                    category = NotificationCategory.PROMOTIONAL,
                    categoryConfidence = 0.95f,
                    priorityLevel = PriorityLevel.MINIMAL,
                    basePriorityScore = 15.48f,
                    urgencyScore = 10f,
                    actionScore = 0f,
                    deadlineScore = 0f,
                    importanceScore = 50f,
                    recencyScore = 95f,
                    otpDetected = false,
                    actionDetected = false,
                    actionDescription = null,
                    deadlineText = null,
                    deadlineAt = null,
                    deadlineResolutionStatus = "NONE",
                    priorityOverrideReason = "Promotional advertisement demoted to Minimal (P4)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.PROMOTIONAL,
                        PriorityLevel.MINIMAL,
                        ScoreBreakdown(10f, 0f, 0f, 50f, 95f, 15.48f),
                        true,
                        "Promotional advertisement demoted to Minimal (P4)",
                        listOf("Promotional keywords detected", "Demoted to Minimal P4")
                    ),
                    receivedAt = now - (10 * minMs), // Arrived 1st
                    processedAt = now
                ),
                NotificationRecord(
                    eventKey = "sim_whatsapp_${System.currentTimeMillis()}",
                    packageName = "com.whatsapp",
                    applicationName = "WhatsApp",
                    title = "Rahul",
                    body = "Hey, what time are we meeting for coffee today?",
                    normalizedText = "Rahul - Hey, what time are we meeting for coffee today?",
                    category = NotificationCategory.PERSONAL_AND_SOCIAL,
                    categoryConfidence = 0.85f,
                    priorityLevel = PriorityLevel.MEDIUM,
                    basePriorityScore = 25.99f,
                    urgencyScore = 45f,
                    actionScore = 0f,
                    deadlineScore = 0f,
                    importanceScore = 50f,
                    recencyScore = 96f,
                    otpDetected = false,
                    actionDetected = false,
                    actionDescription = null,
                    deadlineText = null,
                    deadlineAt = null,
                    deadlineResolutionStatus = "NONE",
                    priorityOverrideReason = "Personal messaging communication set to Medium (P2)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.PERSONAL_AND_SOCIAL,
                        PriorityLevel.MEDIUM,
                        ScoreBreakdown(45f, 0f, 0f, 50f, 96f, 25.99f),
                        true,
                        "Personal messaging communication set to Medium (P2)",
                        listOf("Personal communication", "Routine social message")
                    ),
                    receivedAt = now - (8 * minMs), // Arrived 2nd
                    processedAt = now
                ),
                NotificationRecord(
                    eventKey = "sim_hdfc_${System.currentTimeMillis()}",
                    packageName = "com.snapwork.hdfc",
                    applicationName = "HDFC Bank",
                    title = "Account Debited",
                    body = "Rs 12,500.00 debited from a/c **4921 towards Rent Payment via NEFT.",
                    normalizedText = "Account Debited - Rs 12,500.00 debited from a/c **4921 towards Rent Payment via NEFT.",
                    category = NotificationCategory.FINANCIAL_AND_TRANSACTIONAL,
                    categoryConfidence = 0.95f,
                    priorityLevel = PriorityLevel.HIGH,
                    basePriorityScore = 31.99f,
                    urgencyScore = 75f,
                    actionScore = 0f,
                    deadlineScore = 0f,
                    importanceScore = 80f,
                    recencyScore = 97f,
                    otpDetected = false,
                    actionDetected = false,
                    actionDescription = null,
                    deadlineText = null,
                    deadlineAt = null,
                    deadlineResolutionStatus = "NONE",
                    priorityOverrideReason = "Important financial transaction alert promoted to High (P1)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.FINANCIAL_AND_TRANSACTIONAL,
                        PriorityLevel.HIGH,
                        ScoreBreakdown(75f, 0f, 0f, 80f, 97f, 31.99f),
                        true,
                        "Important financial transaction alert promoted to High (P1)",
                        listOf("Transaction debit alert (Rs 12,500)", "Promoted to High P1")
                    ),
                    receivedAt = now - (6 * minMs), // Arrived 3rd
                    processedAt = now
                ),
                NotificationRecord(
                    eventKey = "sim_classroom_${System.currentTimeMillis()}",
                    packageName = "com.google.android.apps.classroom",
                    applicationName = "Google Classroom",
                    title = "Assignment Submission Deadline",
                    body = "Operating Systems Lab 4 is due today at 11:59 PM. Submit by deadline to receive credit.",
                    normalizedText = "Assignment Submission Deadline - Operating Systems Lab 4 is due today at 11:59 PM.",
                    category = NotificationCategory.WORK_AND_EDUCATION,
                    categoryConfidence = 0.94f,
                    priorityLevel = PriorityLevel.HIGH,
                    basePriorityScore = 71.49f,
                    urgencyScore = 80f,
                    actionScore = 50f,
                    deadlineScore = 90f,
                    importanceScore = 70f,
                    recencyScore = 98f,
                    otpDetected = false,
                    actionDetected = true,
                    actionDescription = "Submit lab assignment",
                    deadlineText = "Due today at 11:59 PM",
                    deadlineAt = null,
                    deadlineResolutionStatus = "RESOLVED_TODAY",
                    priorityOverrideReason = "Imminent submission deadline promoted to High (P1)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.WORK_AND_EDUCATION,
                        PriorityLevel.HIGH,
                        ScoreBreakdown(80f, 50f, 90f, 70f, 98f, 71.49f),
                        true,
                        "Imminent submission deadline promoted to High (P1)",
                        listOf("Imminent submission deadline due today", "Action required")
                    ),
                    receivedAt = now - (4 * minMs), // Arrived 4th
                    processedAt = now
                ),
                NotificationRecord(
                    eventKey = "sim_otp_${System.currentTimeMillis()}",
                    packageName = "com.snapwork.hdfc",
                    applicationName = "HDFC Bank",
                    title = "HDFC NetBanking OTP",
                    body = "Your verification code is 482731 for NetBanking transaction. Do not share it with anyone.",
                    normalizedText = "HDFC NetBanking OTP - Your verification code is 482731 for NetBanking transaction. Do not share it with anyone.",
                    category = NotificationCategory.SECURITY_AND_VERIFICATION,
                    categoryConfidence = 0.99f,
                    priorityLevel = PriorityLevel.CRITICAL,
                    basePriorityScore = 92.5f,
                    urgencyScore = 100f,
                    actionScore = 100f,
                    deadlineScore = 100f,
                    importanceScore = 90f,
                    recencyScore = 99f,
                    otpDetected = true,
                    actionDetected = true,
                    actionDescription = "Enter passcode 482731",
                    deadlineText = "Expires in 10 minutes",
                    deadlineAt = null,
                    deadlineResolutionStatus = "RESOLVED_IMMINENT",
                    priorityOverrideReason = "Confidently identified OTP / verification code promoted to Critical (P0)",
                    explanationJson = ExplanationBuilder.buildJson(
                        NotificationCategory.SECURITY_AND_VERIFICATION,
                        PriorityLevel.CRITICAL,
                        ScoreBreakdown(100f, 100f, 100f, 90f, 99f, 92.5f),
                        true,
                        "Confidently identified OTP / verification code promoted to Critical (P0)",
                        listOf("Verification code 482731 detected", "Promoted to Critical P0")
                    ),
                    receivedAt = now - (1 * minMs), // Arrived 5th (LATEST)
                    processedAt = now
                )
            )

            notifs.forEach { notificationRepo.saveNotification(it) }
        }
    }
}
