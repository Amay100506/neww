package com.priosort.app.capture

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.SoundRuleMatcher
import com.priosort.app.intelligence.SoundDecisionResolver
import com.priosort.app.intelligence.ActionDetector
import com.priosort.app.intelligence.BankingClassifier
import com.priosort.app.intelligence.DeadlineDetector
import com.priosort.app.intelligence.ExplanationBuilder
import com.priosort.app.intelligence.CustomRuleMatcher
import com.priosort.app.intelligence.NotificationClassifier
import com.priosort.app.intelligence.OtpDetector
import com.priosort.app.intelligence.PriorityEngine
import com.priosort.app.util.TextRedactor
import com.priosort.app.data.repository.SoundPlayback
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Real-time notification listener service capturing supported notification events (FR-01).
 */
class NotificationCaptureService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val priorityEngine = PriorityEngine()

    override fun onListenerConnected() {
        super.onListenerConnected()
        CaptureStateManager.setServiceConnected(true, applicationContext)
        Log.i(TAG, "NotificationCaptureService connected successfully.")
        // Notifications already visible when access is enabled should appear too.
        activeNotifications?.forEach(::onNotificationPosted)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        CaptureStateManager.setServiceConnected(false, applicationContext)
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
                val appSettings = app.appSettingsRepository.settings.first()
                val vipResult = com.priosort.app.intelligence.VipManager.checkVip(
                    event.senderIdentity ?: event.title,
                    event.body,
                    appSettings.vipContacts.toSet()
                )

                val localClassification = NotificationClassifier.classify(
                    text = combinedText,
                    packageName = event.packageName,
                    isOtp = otpResult.isOtp,
                    financialType = financialType
                )
                val priorityRules = app.database.customRuleDao().getAllRules().first().map { it.toDomain() }
                val customRuleResult = CustomRuleMatcher.evaluateRules(
                    event.title, event.body, event.packageName, priorityRules
                )
                val classification = localClassification.copy(
                    category = customRuleResult.overriddenCategory ?: localClassification.category,
                    confidence = if (customRuleResult.overriddenCategory != null) 1f else localClassification.confidence,
                    detectedRule = customRuleResult.matchedRule?.ruleName ?: localClassification.detectedRule
                )

                val isFraudOrFail = (financialType == BankingClassifier.FinancialEventType.FRAUD_ALERT ||
                        financialType == BankingClassifier.FinancialEventType.PAYMENT_FAILURE)

                val actionResult = ActionDetector.detectAction(
                    text = combinedText,
                    isOtp = otpResult.isOtp,
                    isFraudOrFail = isFraudOrFail
                )

                val deadlineResult = DeadlineDetector.detectDeadline(combinedText, event.postTimeMs)
                val appImportance = preferencesRepo.getPackageImportance(event.packageName)
                val burstResult = com.priosort.app.intelligence.BurstDetector.analyzeBurst(
                    newTitle = event.title,
                    newBody = event.body,
                    newPackageName = event.packageName,
                    recentNotifications = notificationRepo.getRecentForBurst(
                        event.packageName,
                        event.eventKey,
                        event.postTimeMs - 3 * 60 * 1000L,
                        event.postTimeMs
                    ),
                    nowMs = event.postTimeMs
                )
                if (burstResult.isBurst) {
                    notificationRepo.markBurstGroup(burstResult.matchingEventKeys, burstResult.burstCount)
                }

                var decision = priorityEngine.evaluate(
                    category = classification.category,
                    isOtp = otpResult.isOtp,
                    financialType = financialType,
                    actionResult = actionResult,
                    deadlineResult = deadlineResult,
                    userAppImportance = appImportance,
                    receivedAtMs = event.postTimeMs
                )

                if (!otpResult.isOtp && financialType != BankingClassifier.FinancialEventType.FRAUD_ALERT) {
                    appSettings.categoryPriorityOverrides[classification.category.name]?.let(PriorityLevel::fromCode)?.let { level ->
                        decision = decision.copy(
                            priorityLevel = level,
                            isOverridden = true,
                            overrideReason = "Your ${classification.category.displayName} category preference set this to ${level.displayName}"
                        )
                    }
                    customRuleResult.matchedRule?.let { matchedRule ->
                        matchedRule.assignedPriority?.let { level ->
                            decision = decision.copy(
                                priorityLevel = level,
                                isOverridden = true,
                                overrideReason = "Custom rule '${matchedRule.ruleName}' set this to ${level.displayName}"
                            )
                        }
                    }
                }

                val focusMode = appSettings.focusModeConfig.resolveActiveMode()
                val focusAdjustment = com.priosort.app.intelligence.FocusModeEngine.applyFocusMode(
                    decision = decision,
                    category = classification.category,
                    activeMode = focusMode,
                    allowlisted = event.packageName in appSettings.focusModeConfig.allowlistedPackageNames ||
                        classification.category in appSettings.focusModeConfig.allowlistedCategories
                )
                if (focusAdjustment.wasAdjusted) {
                    decision = decision.copy(
                        priorityLevel = focusAdjustment.adjustedLevel,
                        isOverridden = true,
                        overrideReason = focusAdjustment.focusReason
                    )
                }

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
                fun redact(value: String?): String? =
                    if (isRedactionEnabled) value?.let(TextRedactor::redactSensitiveText) else value

                val storedTitle = redact(event.title)
                val storedBody = redact(event.body)

                val retentionDays = preferencesRepo.retentionDays.first()
                val retentionExpires = System.currentTimeMillis() + (retentionDays.toLong() * 24 * 3600 * 1000)

                // 7. Persist to Room Database
                val record = NotificationRecord(
                    eventKey = event.eventKey,
                    packageName = event.packageName,
                    applicationName = event.applicationName,
                    title = storedTitle,
                    body = storedBody,
                    normalizedText = redact(combinedText).orEmpty(),
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
                    actionDescription = redact(actionResult.description),
                    deadlineText = redact(deadlineResult.deadlineText),
                    deadlineAt = deadlineResult.deadlineAtMs,
                    deadlineResolutionStatus = deadlineResult.resolutionStatus,
                    priorityOverrideReason = redact(decision.overrideReason),
                    explanationJson = redact(explanationJson).orEmpty(),
                    receivedAt = event.postTimeMs,
                    processedAt = System.currentTimeMillis(),
                    isVip = vipResult.isVip,
                    deliveryStatus = if (deliveryInfo.isDelivery) deliveryInfo.status.displayName else null,
                    transactionAmount = transactionInfo.amount,
                    transactionType = if (transactionInfo.isTransaction) transactionInfo.type.name else null,
                    transactionCurrency = transactionInfo.currency,
                    transactionMerchant = redact(transactionInfo.merchantOrEntity),
                    retentionExpiresAt = retentionExpires,
                    isBurst = burstResult.isBurst,
                    burstCount = burstResult.burstCount
                )

                val isNewRecord = notificationRepo.saveNotification(record)
                if (isNewRecord && !burstResult.isBurst) {
                    playConfiguredSound(app, event, classification.category, record.priorityLevel, combinedText)
                    playConfiguredLight(app, event, record, combinedText)
                }
                CaptureStateManager.markNotificationCaptured(event.postTimeMs)
                Log.d(TAG, "Processed event from ${event.packageName} -> Assigned ${decision.priorityLevel.code} (${decision.scoreBreakdown.baseScore})")

            } catch (e: Exception) {
                // Avoid writing notification-derived exception details to logcat.
                Log.e(TAG, "Error processing notification event (${e::class.simpleName ?: "unknown"})")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn == null) return
        val eventKey = NotificationEventMapper.map(applicationContext, sbn).eventKey
        serviceScope.launch {
            runCatching {
                (applicationContext as? PrioSortApplication)?.notificationRepository?.markRemovedFromSystem(eventKey)
            }.onFailure { Log.w(TAG, "Could not update the inbox record after a system notification was removed") }
        }
    }

    private suspend fun playConfiguredSound(
        app: PrioSortApplication,
        event: NotificationEventMapper.RawNotificationEvent,
        category: com.priosort.app.domain.NotificationCategory,
        priority: PriorityLevel,
        text: String
    ) {
        val settings = app.appSettingsRepository.settings.first()
        if (!settings.soundEnabled) return
        val now = LocalTime.now()
        val inQuietHours = settings.quietHoursEnabled && SoundRuleMatcher.isWithinQuietHours(
                now,
                LocalTime.of(settings.quietHoursStart / 60, settings.quietHoursStart % 60),
                LocalTime.of(settings.quietHoursEnd / 60, settings.quietHoursEnd % 60)
            )
        val rule = SoundRuleMatcher.match(
            rules = app.soundRuleRepository.enabledRules(),
            packageName = event.packageName,
            category = category,
            senderIdentity = event.senderIdentity,
            text = text,
            priority = priority,
            focusMode = settings.focusModeConfig.resolveActiveMode().name
        ) ?: return
        val soundAllowed = SoundPlayback.canPlayAudio(this)
        val vibrationAllowed = rule.vibrationEnabled && settings.vibrationEnabled && SoundPlayback.canVibrate(this)
        val decision = SoundDecisionResolver.resolve(
            rule = rule,
            soundEnabled = settings.soundEnabled,
            inQuietHours = inQuietHours,
            audioAllowed = soundAllowed,
            vibrationEnabled = settings.vibrationEnabled,
            vibrationAllowed = vibrationAllowed
        )
        if (!decision.playAudio && !decision.vibrate) return
        if (!SoundPlayback.acquireCooldown(rule)) return
        if (decision.playAudio) {
            val played = SoundPlayback.play(this, rule, settings.globalVolume)
            if (!played) Log.w(TAG, "Selected sound is unavailable; check the sound rule in Sound Studio")
        }
        if (decision.vibrate) SoundPlayback.vibrate(this, rule.vibrationPatternMs)
    }

    private suspend fun playConfiguredLight(
        app: PrioSortApplication,
        event: NotificationEventMapper.RawNotificationEvent,
        record: NotificationRecord,
        text: String
    ) {
        if (!app.appSettingsRepository.settings.first().edgeLightsEnabled) return
        val rule = com.priosort.app.intelligence.LightRuleResolver.resolve(
            app.lightRuleRepository.enabledRules(),
            event.packageName,
            record.category,
            record.priorityLevel,
            event.senderIdentity,
            text
        ) ?: return
        val shown = withContext(Dispatchers.Main.immediate) { app.edgeLightOverlayController.show(rule) }
        if (!shown) Log.w(TAG, "Edge light could not be shown; check overlay permission and device restrictions")
    }

    companion object {
        private const val TAG = "PrioSortCapture"
    }
}
