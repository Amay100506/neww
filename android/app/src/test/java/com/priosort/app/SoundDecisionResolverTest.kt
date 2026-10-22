package com.priosort.app

import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import com.priosort.app.intelligence.SoundDecisionResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SoundDecisionResolverTest {
    private val rule = SoundRule(
        id = 1,
        name = "Priority alert",
        matchType = SoundMatchType.PRIORITY,
        matchValue = "P1",
        soundUri = "content://sounds/alert",
        vibrationEnabled = true
    )

    @Test fun globalDisableSuppressesBothEffects() {
        val result = SoundDecisionResolver.resolve(rule, false, false, true, true, true)
        assertFalse(result.playAudio)
        assertFalse(result.vibrate)
        assertTrue(result.explanation.contains("disabled"))
    }

    @Test fun quietHoursSuppressRuleUnlessItExplicitlyOptsIn() {
        val suppressed = SoundDecisionResolver.resolve(rule, true, true, true, true, true)
        val allowed = SoundDecisionResolver.resolve(rule.copy(allowDuringQuietHours = true), true, true, true, true, true)
        assertFalse(suppressed.playAudio)
        assertFalse(suppressed.vibrate)
        assertTrue(allowed.playAudio)
        assertTrue(allowed.vibrate)
    }

    @Test fun deviceRingerCanBlockAudioWhileVibrationRemainsAvailable() {
        val result = SoundDecisionResolver.resolve(rule, true, false, false, true, true)
        assertFalse(result.playAudio)
        assertTrue(result.vibrate)
        assertTrue(result.explanation.contains("blocked by device"))
        assertTrue(result.explanation.contains("vibrate"))
    }

    @Test fun silentRuleAndUnavailableEffectsAreReportedAsSuppressed() {
        val silent = SoundDecisionResolver.resolve(rule.copy(silent = true), true, false, true, true, true)
        val unavailable = SoundDecisionResolver.resolve(rule, true, false, false, true, false)
        assertEquals(false, silent.playAudio || silent.vibrate)
        assertTrue(silent.explanation.contains("silent rule"))
        assertEquals(false, unavailable.playAudio || unavailable.vibrate)
        assertTrue(unavailable.explanation.contains("Suppressed"))
    }

    @Test fun missingRuleDoesNotClaimAnEffectWouldRun() {
        val result = SoundDecisionResolver.resolve(null, true, false, true, true, true)
        assertFalse(result.playAudio)
        assertFalse(result.vibrate)
        assertEquals("No enabled sound rule matches", result.explanation)
    }
}
