package com.priosort.app

import com.priosort.app.data.local.entities.SoundRuleEntity
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import org.junit.Assert.assertEquals
import org.junit.Test

class SoundRulePersistenceTest {
    @Test fun soundRuleEntityRoundTripsThroughDatabaseMapper() {
        val original = SoundRule(
            19, "Work chats", SoundMatchType.CONTACT, "Asha", "content://sound/1", .75f, true, false, true,
            vibrationPatternMs = listOf(0L, 100L, 120L, 200L), cooldownMs = 3_000, repeatCount = 2,
            allowDuringQuietHours = true
        )
        assertEquals(original, SoundRuleEntity.fromDomain(original).toDomain())
    }

    @Test fun invalidPersistedMatchTypeFallsBackSafely() {
        val entity = SoundRuleEntity(1, "Legacy", "UNKNOWN", "x", null, 1f, true, false, true)
        assertEquals(SoundMatchType.KEYWORD, entity.toDomain().matchType)
    }
}
