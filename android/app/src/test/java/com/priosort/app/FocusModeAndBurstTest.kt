package com.priosort.app

import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.FocusModeType
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityDecision
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.domain.ScoreBreakdown
import com.priosort.app.intelligence.BurstDetector
import com.priosort.app.intelligence.CustomRuleMatcher
import com.priosort.app.intelligence.FocusModeEngine
import com.priosort.app.util.DataExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusModeAndBurstTest {

    @Test
    fun testFocusModeWorkElevation() {
        val dummyDecision = PriorityDecision(
            priorityLevel = PriorityLevel.MEDIUM,
            scoreBreakdown = ScoreBreakdown(50f, 0f, 0f, 50f, 100f, 40f),
            isOverridden = false,
            overrideReason = null,
            applicableRule = null,
            explanations = emptyList()
        )

        val result = FocusModeEngine.applyFocusMode(
            decision = dummyDecision,
            category = NotificationCategory.WORK_AND_EDUCATION,
            activeMode = FocusModeType.WORK
        )

        assertTrue(result.wasAdjusted)
        assertEquals(PriorityLevel.HIGH, result.adjustedLevel)
        assertTrue(result.focusReason?.contains("Work Mode") == true)
    }

    @Test
    fun testFocusModeSleepImmunityForCritical() {
        val criticalDecision = PriorityDecision(
            priorityLevel = PriorityLevel.CRITICAL,
            scoreBreakdown = ScoreBreakdown(100f, 100f, 100f, 100f, 100f, 100f),
            isOverridden = true,
            overrideReason = "OTP",
            applicableRule = "RULE_OTP",
            explanations = emptyList()
        )

        // Sleep mode MUST NOT demote Critical OTPs or Fraud alerts
        val result = FocusModeEngine.applyFocusMode(
            decision = criticalDecision,
            category = NotificationCategory.SECURITY_AND_VERIFICATION,
            activeMode = FocusModeType.SLEEP
        )

        assertFalse(result.wasAdjusted)
        assertEquals(PriorityLevel.CRITICAL, result.adjustedLevel)
    }

    @Test
    fun testBurstSuppressionDetection() {
        val now = 1000000L
        val recentNotifs = listOf(
            createRecord(id = 1, pkg = "com.whatsapp", title = "Group Chat", time = now - 30000),
            createRecord(id = 2, pkg = "com.whatsapp", title = "Group Chat", time = now - 20000)
        )

        val burstResult = BurstDetector.analyzeBurst(
            newTitle = "Group Chat",
            newBody = "Another message",
            newPackageName = "com.whatsapp",
            recentNotifications = recentNotifs,
            windowMs = 180000L,
            nowMs = now
        )

        // 2 prior + 1 new = 3 -> Triggers burst warning!
        assertTrue(burstResult.isBurst)
        assertEquals(3, burstResult.burstCount)
    }

    @Test
    fun testCustomRuleMatching() {
        val rules = listOf(
            CustomRule(
                id = 1,
                ruleName = "Electricity Bill High Priority",
                keywordPattern = "electricity bill",
                targetField = "ANY",
                assignedPriority = PriorityLevel.HIGH,
                assignedCategory = NotificationCategory.FINANCIAL_AND_TRANSACTIONAL
            )
        )

        val match = CustomRuleMatcher.evaluateRules(
            title = "Bill Alert",
            body = "Your electricity bill for Oct is generated.",
            packageName = "com.bills",
            rules = rules
        )

        assertTrue(match.isMatched)
        assertEquals(PriorityLevel.HIGH, match.overriddenPriority)
        assertEquals(NotificationCategory.FINANCIAL_AND_TRANSACTIONAL, match.overriddenCategory)
    }

    @Test
    fun testJsonExportWithRedaction() {
        val record = createRecord(id = 1, pkg = "com.bank", title = "OTP", body = "Your code is 981240. Do not share.")
        val exportedJson = DataExporter.exportToJson(listOf(record), enforceRedaction = true)

        assertFalse("Raw OTP must be redacted in JSON export", exportedJson.contains("981240"))
        assertTrue("Masked bullet pattern should appear", exportedJson.contains("••••••"))

        // Roundtrip import
        val imported = DataExporter.importFromJson(exportedJson)
        assertEquals(1, imported.size)
        assertEquals("OTP", imported[0].title)
    }

    private fun createRecord(id: Long, pkg: String, title: String, body: String = "Body", time: Long = 1000L): NotificationRecord {
        return NotificationRecord(
            id = id,
            eventKey = "key_$id",
            packageName = pkg,
            applicationName = pkg,
            title = title,
            body = body,
            normalizedText = "$title - $body",
            category = NotificationCategory.GENERAL_INFORMATIONAL,
            categoryConfidence = 0.9f,
            priorityLevel = if (body.contains("code is")) PriorityLevel.CRITICAL else PriorityLevel.MEDIUM,
            basePriorityScore = 50f,
            urgencyScore = 50f,
            actionScore = 0f,
            deadlineScore = 0f,
            importanceScore = 50f,
            recencyScore = 100f,
            otpDetected = body.contains("code is"),
            actionDetected = false,
            actionDescription = null,
            deadlineText = null,
            deadlineAt = null,
            deadlineResolutionStatus = "NONE",
            priorityOverrideReason = null,
            explanationJson = "{}",
            receivedAt = time,
            processedAt = time
        )
    }
}
