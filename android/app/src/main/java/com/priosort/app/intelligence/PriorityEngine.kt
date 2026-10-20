package com.priosort.app.intelligence

import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationPolicy
import com.priosort.app.domain.PriorityDecision
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.ScoreBreakdown
import kotlin.math.exp
import kotlin.math.max

/**
 * Priority Scoring Engine implementing:
 * S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R
 * and explainable rule precedence hierarchy (Section 12).
 */
class PriorityEngine(
    private val policy: NotificationPolicy = NotificationPolicy()
) {

    fun calculateRecency(receivedAtMs: Long, nowMs: Long = System.currentTimeMillis()): Float {
        val ageMs = max(0L, nowMs - receivedAtMs)
        val ageHours = ageMs / (1000.0f * 3600.0f)
        return (100.0f * exp(-ageHours / policy.tauHours)).coerceIn(0.0f, 100.0f)
    }

    fun evaluate(
        category: NotificationCategory,
        isOtp: Boolean,
        financialType: BankingClassifier.FinancialEventType,
        actionResult: ActionDetector.ActionResult,
        deadlineResult: DeadlineDetector.DeadlineResult,
        userAppImportance: Float = 50.0f,
        receivedAtMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ): PriorityDecision {
        val explanations = mutableListOf<String>()

        // 1. Urgency (0 - 100)
        val urgencyScore: Float = when {
            isOtp -> {
                explanations.add("Time-sensitive verification code detected (Urgency: 100)")
                100.0f
            }
            financialType == BankingClassifier.FinancialEventType.FRAUD_ALERT -> {
                explanations.add("Suspicious activity / fraud alert detected (Urgency: 95)")
                95.0f
            }
            financialType == BankingClassifier.FinancialEventType.PAYMENT_FAILURE -> {
                explanations.add("Payment failure requires prompt attention (Urgency: 85)")
                85.0f
            }
            deadlineResult.hasDeadline -> {
                explanations.add("Active deadline requires attention (Urgency: 80)")
                80.0f
            }
            financialType == BankingClassifier.FinancialEventType.IMPORTANT_TRANSACTION -> {
                explanations.add("High-value transaction alert (Urgency: 75)")
                75.0f
            }
            category == NotificationCategory.FINANCIAL_AND_TRANSACTIONAL -> {
                explanations.add("Financial transaction notification (Urgency: 65)")
                65.0f
            }
            category == NotificationCategory.WORK_AND_EDUCATION -> {
                explanations.add("Work or education event (Urgency: 60)")
                60.0f
            }
            category == NotificationCategory.PERSONAL_AND_SOCIAL -> {
                explanations.add("Personal communication (Urgency: 45)")
                45.0f
            }
            category == NotificationCategory.PROMOTIONAL -> {
                explanations.add("Promotional advertisement (Urgency: 10)")
                10.0f
            }
            else -> {
                explanations.add("Routine informational content (Urgency: 30)")
                30.0f
            }
        }

        // 2. Action Score (0 - 100)
        val actionScore = actionResult.actionScore
        if (actionResult.hasAction) {
            explanations.add(actionResult.description ?: "Explicit user action required")
        }

        // 3. Deadline Score (0 - 100)
        val deadlineScore = deadlineResult.deadlineScore
        if (deadlineResult.hasDeadline) {
            explanations.add("Deadline proximity: ${deadlineResult.deadlineText} (Score: $deadlineScore)")
        }

        // 4. User Importance (0 - 100)
        val importanceScore = userAppImportance.coerceIn(0.0f, 100.0f)

        // 5. Recency (0 - 100)
        val recencyScore = calculateRecency(receivedAtMs, nowMs)

        // Formula: S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R
        val baseScore = (0.30f * urgencyScore) +
                (0.25f * actionScore) +
                (0.25f * deadlineScore) +
                (0.15f * importanceScore) +
                (0.05f * recencyScore)

        val roundedBaseScore = Math.round(baseScore * 100f) / 100f

        val breakdown = ScoreBreakdown(
            urgencyScore = urgencyScore,
            actionScore = actionScore,
            deadlineScore = deadlineScore,
            importanceScore = importanceScore,
            recencyScore = Math.round(recencyScore * 100f) / 100f,
            baseScore = roundedBaseScore
        )

        // Standard mapping to Priority Level
        val mappedLevel = when {
            roundedBaseScore >= policy.criticalThreshold -> PriorityLevel.CRITICAL
            roundedBaseScore >= policy.highThreshold -> PriorityLevel.HIGH
            roundedBaseScore >= policy.mediumThreshold -> PriorityLevel.MEDIUM
            roundedBaseScore >= policy.lowThreshold -> PriorityLevel.LOW
            else -> PriorityLevel.MINIMAL
        }

        // Apply Rule-Based Overrides
        var finalLevel = mappedLevel
        var isOverridden = false
        var overrideReason: String? = null
        var applicableRule: String? = null

        when {
            isOtp -> {
                finalLevel = PriorityLevel.CRITICAL
                isOverridden = true
                overrideReason = "Confidently identified OTP / verification code promoted to Critical (P0)"
                applicableRule = "RULE_OTP_CRITICAL_OVERRIDE"
                explanations.add(overrideReason)
            }
            financialType == BankingClassifier.FinancialEventType.FRAUD_ALERT -> {
                finalLevel = PriorityLevel.CRITICAL
                isOverridden = true
                overrideReason = "Credible fraud / security alert promoted to Critical (P0)"
                applicableRule = "RULE_FRAUD_CRITICAL_OVERRIDE"
                explanations.add(overrideReason)
            }
            financialType == BankingClassifier.FinancialEventType.PAYMENT_FAILURE -> {
                if (finalLevel.rank < PriorityLevel.HIGH.rank) {
                    finalLevel = PriorityLevel.HIGH
                    isOverridden = true
                    overrideReason = "Payment failure promoted to High (P1)"
                    applicableRule = "RULE_PAYMENT_FAIL_HIGH"
                    explanations.add(overrideReason)
                }
            }
            financialType == BankingClassifier.FinancialEventType.IMPORTANT_TRANSACTION -> {
                if (finalLevel.rank < PriorityLevel.HIGH.rank) {
                    finalLevel = PriorityLevel.HIGH
                    isOverridden = true
                    overrideReason = "Important financial transaction alert promoted to High (P1)"
                    applicableRule = "RULE_IMPORTANT_TX_HIGH"
                    explanations.add(overrideReason)
                }
            }
            category == NotificationCategory.PROMOTIONAL && userAppImportance <= 50.0f -> {
                finalLevel = PriorityLevel.MINIMAL
                isOverridden = true
                overrideReason = "Promotional advertisement demoted to Minimal (P4)"
                applicableRule = "RULE_PROMO_DEMOTION_MINIMAL"
                explanations.add(overrideReason)
            }
            deadlineResult.hasDeadline && deadlineScore >= 70.0f && finalLevel.rank < PriorityLevel.HIGH.rank -> {
                finalLevel = PriorityLevel.HIGH
                isOverridden = true
                overrideReason = "Imminent submission deadline promoted to High (P1)"
                applicableRule = "RULE_DEADLINE_HIGH_PROMOTION"
                explanations.add(overrideReason)
            }
            category == NotificationCategory.PERSONAL_AND_SOCIAL && finalLevel.rank < PriorityLevel.MEDIUM.rank -> {
                finalLevel = PriorityLevel.MEDIUM
                isOverridden = true
                overrideReason = "Personal messaging communication set to Medium (P2)"
                applicableRule = "RULE_PERSONAL_MSG_MEDIUM"
                explanations.add(overrideReason)
            }
            financialType == BankingClassifier.FinancialEventType.ROUTINE_TRANSACTION && finalLevel.rank < PriorityLevel.MEDIUM.rank -> {
                finalLevel = PriorityLevel.MEDIUM
                isOverridden = true
                overrideReason = "Routine transaction receipt set to Medium (P2)"
                applicableRule = "RULE_ROUTINE_TX_MEDIUM"
                explanations.add(overrideReason)
            }
        }

        return PriorityDecision(
            priorityLevel = finalLevel,
            scoreBreakdown = breakdown,
            isOverridden = isOverridden,
            overrideReason = overrideReason,
            applicableRule = applicableRule,
            explanations = explanations
        )
    }
}
