package com.priosort.app

import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.ActionDetector
import com.priosort.app.intelligence.BankingClassifier
import com.priosort.app.intelligence.DeadlineDetector
import com.priosort.app.intelligence.PriorityEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriorityEngineTest {

    private val engine = PriorityEngine()

    @Test
    fun testMathematicalFormulaCalculation() {
        // Formula: S = 0.30U + 0.25A + 0.25D + 0.15I + 0.05R
        // If U=100, A=100, D=100, I=100, R=100 -> S = 100.0
        val actionResult = ActionDetector.ActionResult(true, 100f, "Required")
        val deadlineResult = DeadlineDetector.DeadlineResult(true, 100f, "1 hour", 1.0f, "RESOLVED_IMMINENT")

        val decision = engine.evaluate(
            category = NotificationCategory.SECURITY_AND_VERIFICATION,
            isOtp = true,
            financialType = BankingClassifier.FinancialEventType.NONE,
            actionResult = actionResult,
            deadlineResult = deadlineResult,
            userAppImportance = 100f,
            receivedAtMs = 1000L,
            nowMs = 1000L // R=100
        )

        assertEquals(100.0f, decision.scoreBreakdown.baseScore, 0.01f)
        assertEquals(PriorityLevel.CRITICAL, decision.priorityLevel)
    }

    @Test
    fun testOtpRuleOverridePromotedToCritical() {
        val actionResult = ActionDetector.ActionResult(true, 100f, "Passcode")
        val deadlineResult = DeadlineDetector.DeadlineResult(false, 0f, null, null, "NONE")

        val decision = engine.evaluate(
            category = NotificationCategory.SECURITY_AND_VERIFICATION,
            isOtp = true,
            financialType = BankingClassifier.FinancialEventType.NONE,
            actionResult = actionResult,
            deadlineResult = deadlineResult,
            userAppImportance = 50f,
            receivedAtMs = 1000L,
            nowMs = 1000L
        )

        assertEquals(PriorityLevel.CRITICAL, decision.priorityLevel)
        assertTrue(decision.isOverridden)
        assertTrue(decision.overrideReason?.contains("OTP") == true)
    }

    @Test
    fun testPromotionalDemotedToMinimal() {
        val actionResult = ActionDetector.ActionResult(false, 0f, null)
        val deadlineResult = DeadlineDetector.DeadlineResult(false, 0f, null, null, "NONE")

        val decision = engine.evaluate(
            category = NotificationCategory.PROMOTIONAL,
            isOtp = false,
            financialType = BankingClassifier.FinancialEventType.NONE,
            actionResult = actionResult,
            deadlineResult = deadlineResult,
            userAppImportance = 40f,
            receivedAtMs = 1000L,
            nowMs = 1000L
        )

        assertEquals(PriorityLevel.MINIMAL, decision.priorityLevel)
        assertTrue(decision.isOverridden)
        assertTrue(decision.overrideReason?.contains("Promotional") == true)
    }
}
