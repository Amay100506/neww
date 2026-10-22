package com.priosort.app.ui.simulator

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priosort.app.PrioSortApplication
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityDecision
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.SoundRule
import com.priosort.app.domain.LightRule
import com.priosort.app.intelligence.ActionDetector
import com.priosort.app.intelligence.BankingClassifier
import com.priosort.app.intelligence.CustomRuleMatcher
import com.priosort.app.intelligence.DeadlineDetector
import com.priosort.app.intelligence.DeliveryTracker
import com.priosort.app.intelligence.FocusModeEngine
import com.priosort.app.intelligence.LightRuleResolver
import com.priosort.app.intelligence.NotificationClassifier
import com.priosort.app.intelligence.OtpDetector
import com.priosort.app.intelligence.PriorityEngine
import com.priosort.app.intelligence.SoundRuleMatcher
import com.priosort.app.intelligence.SoundDecisionResolver
import com.priosort.app.data.repository.SoundPlayback
import com.priosort.app.intelligence.VipManager
import java.time.LocalTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class RuleSimulationResult(
    val category: NotificationCategory,
    val confidence: Float,
    val priority: PriorityLevel,
    val score: Float,
    val breakdown: PriorityDecision,
    val soundMatches: List<SoundRule>,
    val lightMatches: List<LightRule>,
    val soundDecision: String,
    val lightDecision: String,
    val warnings: List<String>
)

data class RuleSimulatorState(val loading: Boolean = false, val result: RuleSimulationResult? = null, val error: String? = null)

class RuleSimulatorViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as PrioSortApplication
    private val _state = MutableStateFlow(RuleSimulatorState())
    val state: StateFlow<RuleSimulatorState> = _state.asStateFlow()

    fun simulate(title: String, body: String, packageName: String, sender: String) {
        if (title.isBlank() && body.isBlank()) {
            _state.value = RuleSimulatorState(error = "Enter a title or body to run the dry-run.")
            return
        }
        _state.value = RuleSimulatorState(loading = true)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val settings = app.appSettingsRepository.settings.first()
                val text = listOf(title, body).filter(String::isNotBlank).joinToString(" - ")
                val otp = OtpDetector.detectOtp(text)
                val financial = BankingClassifier.classifyFinancialEvent(text)
                val deadline = DeadlineDetector.detectDeadline(text)
                val delivery = DeliveryTracker.parseDelivery(text, packageName)
                val transaction = BankingClassifier.FinancialEventType.ROUTINE_TRANSACTION == financial ||
                    financial == BankingClassifier.FinancialEventType.IMPORTANT_TRANSACTION
                val local = NotificationClassifier.classify(text, packageName, otp.isOtp, financial)
                val customRules = app.database.customRuleDao().getAllRules().first().map { it.toDomain() }
                val custom = CustomRuleMatcher.evaluateRules(title, body, packageName, customRules)
                val category = custom.overriddenCategory ?: local.category
                val action = ActionDetector.detectAction(
                    text,
                    otp.isOtp,
                    financial == BankingClassifier.FinancialEventType.FRAUD_ALERT || financial == BankingClassifier.FinancialEventType.PAYMENT_FAILURE
                )
                var decision = PriorityEngine().evaluate(
                    category = category,
                    isOtp = otp.isOtp,
                    financialType = financial,
                    actionResult = action,
                    deadlineResult = deadline,
                    userAppImportance = app.userPreferencesRepository.getPackageImportance(packageName),
                    receivedAtMs = System.currentTimeMillis()
                )
                if (!otp.isOtp && financial != BankingClassifier.FinancialEventType.FRAUD_ALERT) {
                    settings.categoryPriorityOverrides[category.name]?.let(PriorityLevel::fromCode)?.let { level ->
                        decision = decision.copy(priorityLevel = level, isOverridden = true, overrideReason = "Category preference: ${level.displayName}")
                    }
                    custom.matchedRule?.assignedPriority?.let { level ->
                        decision = decision.copy(priorityLevel = level, isOverridden = true, overrideReason = "Custom priority rule '${custom.matchedRule.ruleName}'")
                    }
                }
                val focus = settings.focusModeConfig.resolveActiveMode()
                val allowlisted = packageName in settings.focusModeConfig.allowlistedPackageNames || category in settings.focusModeConfig.allowlistedCategories
                val focused = FocusModeEngine.applyFocusMode(decision, category, focus, allowlisted)
                if (focused.wasAdjusted) decision = decision.copy(priorityLevel = focused.adjustedLevel, isOverridden = true, overrideReason = focused.focusReason)
                val vip = VipManager.checkVip(sender.ifBlank { title }, body, settings.vipContacts.toSet())
                if (vip.isVip && decision.priorityLevel.rank < PriorityLevel.HIGH.rank) {
                    decision = decision.copy(priorityLevel = PriorityLevel.HIGH, isOverridden = true, overrideReason = "VIP contact elevated to High")
                }
                if (delivery.status == DeliveryTracker.DeliveryStatus.OUT_FOR_DELIVERY && decision.priorityLevel.rank < PriorityLevel.HIGH.rank) {
                    decision = decision.copy(priorityLevel = PriorityLevel.HIGH, isOverridden = true, overrideReason = "Out for delivery elevated to High")
                }
                val soundRules = app.soundRuleRepository.rules.first()
                val soundMatches = SoundRuleMatcher.matching(soundRules, packageName, category, sender.ifBlank { null }, text, decision.priorityLevel, focus.name)
                val lightRules = app.lightRuleRepository.rules.first()
                val lightMatches = LightRuleResolver.matching(lightRules, packageName, category, decision.priorityLevel, sender.ifBlank { null }, text)
                val soundWinner = soundMatches.firstOrNull()
                val quiet = settings.quietHoursEnabled && SoundRuleMatcher.isWithinQuietHours(
                    LocalTime.now(), LocalTime.of(settings.quietHoursStart / 60, settings.quietHoursStart % 60),
                    LocalTime.of(settings.quietHoursEnd / 60, settings.quietHoursEnd % 60)
                )
                val resolvedSound = SoundDecisionResolver.resolve(
                    rule = soundWinner,
                    soundEnabled = settings.soundEnabled,
                    inQuietHours = quiet,
                    audioAllowed = SoundPlayback.canPlayAudio(app),
                    vibrationEnabled = settings.vibrationEnabled,
                    vibrationAllowed = SoundPlayback.canVibrate(app)
                )
                val cooldownRemaining = soundWinner?.let { SoundPlayback.cooldownRemainingMs(it) } ?: 0L
                val soundDecision = if (cooldownRemaining > 0L && (resolvedSound.playAudio || resolvedSound.vibrate)) {
                    "Suppressed: '${soundWinner?.name}' cooldown has ${((cooldownRemaining + 999) / 1000)}s remaining"
                } else resolvedSound.explanation
                val lightWinner = lightMatches.firstOrNull()
                val lightDecision = when {
                    !settings.edgeLightsEnabled -> "Suppressed: live edge alerts are disabled"
                    !app.edgeLightOverlayController.canDrawOverlay() -> "Suppressed: overlay permission is not granted"
                    lightWinner == null -> "No enabled light rule matches"
                    else -> "Would show '${lightWinner.name}' (${lightWinner.pattern.name.lowercase().replace('_', ' ')})"
                }
                val warnings = buildList {
                    if (soundMatches.size > 1) add("${soundMatches.size - 1} matching sound rule(s) are skipped; the highest-specificity rule wins.")
                    if (lightMatches.size > 1) add("${lightMatches.size - 1} matching light rule(s) are skipped; the highest-specificity rule wins.")
                    if (custom.matchedRule != null) add("Priority/category rule matched: ${custom.matchedRule.ruleName}")
                    if (otp.isOtp) add("OTP detector matched contextual verification evidence; OTP values are not shown here.")
                    if (transaction) add("Financial transaction signal detected.")
                }
                RuleSimulationResult(local.category.let { custom.overriddenCategory ?: it }, local.confidence, decision.priorityLevel, decision.scoreBreakdown.baseScore, decision, soundMatches, lightMatches, soundDecision, lightDecision, warnings)
            }.onSuccess { result ->
                withContext(Dispatchers.Main) { _state.value = RuleSimulatorState(result = result) }
            }.onFailure { error ->
                withContext(Dispatchers.Main) { _state.value = RuleSimulatorState(error = error.message ?: "Could not simulate these rules.") }
            }
        }
    }
}
