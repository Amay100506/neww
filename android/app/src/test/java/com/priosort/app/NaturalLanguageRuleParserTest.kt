package com.priosort.app

import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.NaturalLanguageRuleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NaturalLanguageRuleParserTest {
    @Test
    fun parsesPriorityRuleIntoTypedDraft() {
        val result = NaturalLanguageRuleParser.parse("When body contains \"invoice\", set priority to High")
        val rule = result.rule!!
        assertEquals("BODY", rule.targetField)
        assertEquals("invoice", rule.keywordPattern)
        assertEquals(PriorityLevel.HIGH, rule.assignedPriority)
        assertNull(rule.assignedCategory)
        assertTrue(result.message.contains("Review"))
    }

    @Test
    fun parsesCategoryRuleAndAppScope() {
        val result = NaturalLanguageRuleParser.parse("If app includes \"com.bank\" then set category to Finance")
        val rule = result.rule!!
        assertEquals("PACKAGE", rule.targetField)
        assertEquals(NotificationCategory.FINANCIAL_AND_TRANSACTIONAL, rule.assignedCategory)
    }

    @Test
    fun rejectsUnrecognizedFreeFormText() {
        val result = NaturalLanguageRuleParser.parse("Make things important")
        assertNull(result.rule)
        assertTrue(result.message.startsWith("Use:"))
    }
}
