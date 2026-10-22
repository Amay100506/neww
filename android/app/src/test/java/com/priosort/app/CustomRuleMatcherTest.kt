package com.priosort.app

import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.CustomRuleMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CustomRuleMatcherTest {
    @Test fun matchesConfiguredFieldIgnoringCase() {
        val rule = CustomRule(1, "Bank alert", "fraud", "BODY", PriorityLevel.CRITICAL, NotificationCategory.FINANCIAL_AND_TRANSACTIONAL)
        val result = CustomRuleMatcher.evaluateRules("Notice", "Possible FRAUD detected", "com.bank", listOf(rule))
        assertEquals(true, result.isMatched)
        assertEquals(PriorityLevel.CRITICAL, result.overriddenPriority)
        assertEquals(NotificationCategory.FINANCIAL_AND_TRANSACTIONAL, result.overriddenCategory)
    }

    @Test fun ignoresDisabledAndNonMatchingRules() {
        val rule = CustomRule(1, "Disabled", "urgent", "TITLE", PriorityLevel.HIGH, null, isEnabled = false)
        assertFalse(CustomRuleMatcher.evaluateRules("Sale", "urgent", "com.shop", listOf(rule)).isMatched)
    }
}
