package com.priosort.app.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.intelligence.ActionDetector
import com.priosort.app.intelligence.BankingClassifier
import com.priosort.app.intelligence.DeadlineDetector
import com.priosort.app.intelligence.ExplanationBuilder
import com.priosort.app.intelligence.NotificationClassifier
import com.priosort.app.intelligence.OtpDetector
import com.priosort.app.intelligence.PriorityEngine
import com.priosort.app.util.TextRedactor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Real-time notification listener service capturing supported notification events (FR-01).
 */
class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val priorityEngine = PriorityEngine()

    override fun onListenerConnected() {
        super.onListenerConnected()
        CaptureStateManager.setServiceConnected(true)
        Log.i(TAG, "NotificationCaptureService connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        CaptureStateManager.setServiceConnected(false)
        Log.i(TAG, "NotificationCaptureService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        serviceScope.launch {
            try {
                val app = applicationContext as? PrioSortApplication ?: return@launch
                val preferencesRepo = app.userPreferencesRepository
                val notificationRepo = app.notificationRepository

                // 1. Check if capture is globally paused
                val isPaused = preferencesRepo.isCapturePaused.first()
                if (isPaused) {
                    return@launch
                }

                // 2. Map notification event
                val event = NotificationEventMapper.map(applicationContext, sbn)

                // Ignore PrioSort's own notifications
                if (event.packageName == packageName) {
                    return@launch
                }

                // 3. Check if app is excluded
                val isExcluded = preferencesRepo.isPackageExcluded(event.packageName)
                if (isExcluded) {
                    return@launch
                }

                // 4. Combine text for intelligence analysis
                val combinedText = buildString {
                    if (!event.title.isNullOrBlank()) append(event.title)
                    if (!event.body.isNullOrBlank()) {
                        if (isNotEmpty()) append(" - ")
                        append(event.body)
                    }
                }

                if (combinedText.isBlank()) {
                    return@launch
                }

                // 5. Intelligent Analysis Pipeline
                val otpResult = OtpDetector.detectOtp(combinedText)
                val financialType = BankingClassifier.classifyFinancialEvent(combinedText)
                val deliveryInfo = com.priosort.app.intelligence.DeliveryTracker.parseDelivery(combinedText, event.packageName)
                val transactionInfo = com.priosort.app.intelligence.FinanceLedger.parse(combinedText)
                val vipResult = com.priosort.app.intelligence.VipManager.checkVip(event.title, event.body)

                val classification = NotificationClassifier.classify(
                    text = combinedText,
                    packageName = event.packageName,
                    isOtp = otpResult.isOtp,
                    financialType = financialType
                )

                val isFraudOrFail = (financialType == BankingClassifier.FinancialEventType.FRAUD_ALERT ||
                        financialType == BankingClassifier.FinancialEventType.PAYMENT_FAILURE)

                val actionResult = ActionDetector.detectAction(
                    text = combinedText,
                    isOtp = otpResult.isOtp,
                    isFraudOrFail = isFraudOrFail
                )

                val deadlineResult = DeadlineDetector.detectDeadline(combinedText)
                val appImportance = preferencesRepo.getPackageImportance(event.packageName)

                var decision = priorityEngine.evaluate(
                    category = classification.category,
                    isOtp = otpResult.isOtp,
                    financialType = financialType,
                    actionResult = actionResult,
                    deadlineResult = deadlineResult,
                    userAppImportance = appImportance,
                    receivedAtMs = event.postTimeMs
                )

                // VIP Elevation
                if (vipResult.isVip && decision.priorityLevel.rank < com.priosort.app.domain.PriorityLevel.HIGH.rank) {
                    decision = decision.copy(
                        priorityLevel = com.priosort.app.domain.PriorityLevel.HIGH,
                        isOverridden = true,
                        overrideReason = "VIP Contact detected (${vipResult.matchedVipName}) elevated to High (P1)"
                    )
                }

                // Delivery Status Urgency
                if (deliveryInfo.status == com.priosort.app.intelligence.DeliveryTracker.DeliveryStatus.OUT_FOR_DELIVERY &&
                    decision.priorityLevel.rank < com.priosort.app.domain.PriorityLevel.HIGH.rank
                ) {
                    decision = decision.copy(
                        priorityLevel = com.priosort.app.domain.PriorityLevel.HIGH,
                        isOverridden = true,
                        overrideReason = "Package out for delivery today from ${deliveryInfo.courierOrService}"
                    )
                }

                val explanationJson = ExplanationBuilder.buildJson(
                    category = classification.category,
                    level = decision.priorityLevel,
                    score = decision.scoreBreakdown,
                    isOverridden = decision.isOverridden,
                    overrideReason = decision.overrideReason,
                    explanations = decision.explanations
                )

                // 6. Privacy Redaction
                val isRedactionEnabled = preferencesRepo.isRedactionEnabled.first()
                val storedBody = if (isRedactionEnabled && otpResult.isOtp) {
                    TextRedactor.redactOtp(event.body)
                } else {
                    event.body
                }

                val retentionDays = preferencesRepo.retentionDays.first()
                val retentionExpires = System.currentTimeMillis() + (retentionDays.toLong() * 24 * 3600 * 1000)

                // 7. Persist to Room Database
                val record = NotificationRecord(
                    eventKey = event.eventKey,
                    packageName = event.packageName,
                    applicationName = event.applicationName,
                    title = event.title,
                    body = storedBody,
                    normalizedText = combinedText,
                    category = classification.category,
                    categoryConfidence = classification.confidence,
                    priorityLevel = decision.priorityLevel,
                    basePriorityScore = decision.scoreBreakdown.baseScore,
                    urgencyScore = decision.scoreBreakdown.urgencyScore,
                    actionScore = decision.scoreBreakdown.actionScore,
                    deadlineScore = decision.scoreBreakdown.deadlineScore,
                    importanceScore = decision.scoreBreakdown.importanceScore,
                    recencyScore = decision.scoreBreakdown.recencyScore,
                    otpDetected = otpResult.isOtp,
                    actionDetected = actionResult.hasAction,
                    actionDescription = actionResult.description,
                    deadlineText = deadlineResult.deadlineText,
                    deadlineAt = null,
                    deadlineResolutionStatus = deadlineResult.resolutionStatus,
                    priorityOverrideReason = decision.overrideReason,
                    explanationJson = explanationJson,
                    receivedAt = event.postTimeMs,
                    processedAt = System.currentTimeMillis(),
                    isVip = vipResult.isVip,
                    deliveryStatus = if (deliveryInfo.isDelivery) deliveryInfo.status.displayName else null,
                    transactionAmount = transactionInfo.amount,
                    transactionType = if (transactionInfo.isTransaction) transactionInfo.type.name else null,
                    retentionExpiresAt = retentionExpires
                )

                notificationRepo.saveNotification(record)
                Log.d(TAG, "Processed event from ${event.packageName} -> Assigned ${decision.priorityLevel.code} (${decision.scoreBreakdown.baseScore})")

            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification event", e)
            }
        }
    }

    companion object {
        private const val TAG = "PrioSortCapture"
    }
}
