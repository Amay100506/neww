package com.priosort.app

import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import com.priosort.app.intelligence.SoundRuleMatcher
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundRuleMatcherTest {
    @Test fun mostSpecificEnabledRuleWins() {
        val rules = listOf(
            SoundRule(1, "category", SoundMatchType.CATEGORY, NotificationCategory.GENERAL_INFORMATIONAL.name),
            SoundRule(2, "app", SoundMatchType.APP, "com.chat"),
            SoundRule(3, "keyword", SoundMatchType.KEYWORD, "meeting"),
            SoundRule(4, "contact", SoundMatchType.CONTACT, "Asha"),
            SoundRule(5, "disabled", SoundMatchType.CONTACT, "Asha", enabled = false)
        )
        val winner = SoundRuleMatcher.match(rules, "com.chat", NotificationCategory.GENERAL_INFORMATIONAL, "Asha", "meeting")
        assertEquals("contact", winner?.name)
    }

    @Test fun appAndKeywordRulesMatchCaseInsensitively() {
        val rule = SoundRule(2, "app", SoundMatchType.APP, "COM.CHAT")
        assertEquals(rule, SoundRuleMatcher.match(listOf(rule), "com.chat", NotificationCategory.SECURITY_AND_VERIFICATION, null, ""))
        assertNull(SoundRuleMatcher.match(listOf(SoundRule(3, "word", SoundMatchType.KEYWORD, "meeting")), "x", NotificationCategory.SECURITY_AND_VERIFICATION, null, "Sale"))
    }

    @Test fun customConditionBeatsCategoryAndAppDefaultsDeterministically() {
        val rules = listOf(
            SoundRule(1, "category", SoundMatchType.CATEGORY, NotificationCategory.GENERAL_INFORMATIONAL.name),
            SoundRule(2, "app", SoundMatchType.APP, "com.chat"),
            SoundRule(3, "condition", SoundMatchType.KEYWORD, "meeting"),
            SoundRule(4, "condition newer", SoundMatchType.KEYWORD, "meeting")
        )
        val matches = SoundRuleMatcher.matching(
            rules, "com.chat", NotificationCategory.GENERAL_INFORMATIONAL, null, "meeting starts now"
        )
        assertEquals(listOf("condition newer", "condition", "category", "app"), matches.map { it.name })
    }

    @Test fun quietHoursHandleOvernightAndEqualBounds() {
        assertTrue(SoundRuleMatcher.isWithinQuietHours(LocalTime.of(23, 0), LocalTime.of(22, 0), LocalTime.of(7, 0)))
        assertTrue(SoundRuleMatcher.isWithinQuietHours(LocalTime.of(6, 59), LocalTime.of(22, 0), LocalTime.of(7, 0)))
        assertEquals(false, SoundRuleMatcher.isWithinQuietHours(LocalTime.NOON, LocalTime.of(22, 0), LocalTime.of(7, 0)))
        assertEquals(false, SoundRuleMatcher.isWithinQuietHours(LocalTime.NOON, LocalTime.NOON, LocalTime.NOON))
    }
}
