package com.priosort.app

import com.priosort.app.data.export.RuleBackupCodec
import com.priosort.app.domain.CustomRule
import com.priosort.app.domain.LightRule
import com.priosort.app.domain.RuleBackup
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class RuleBackupCodecTest {
    @Test fun exportOmitsDeviceSoundUrisAndImportNeedsReselection() {
        val encoded = RuleBackupCodec.encode(RuleBackup(soundRules = listOf(
            SoundRule(name = "Work", matchType = SoundMatchType.APP, matchValue = "example.app", soundUri = "content://private/document/7")
        )))

        assertFalse(encoded.contains("content://private/document/7"))
        assertFalse(encoded.contains("soundUri"))
        assertNull(RuleBackupCodec.decodeForImport(encoded).soundRules.single().soundUri)
    }

    @Test fun backupRetainsRuleDefinitionsButNotRuleDatabaseIds() {
        val original = RuleBackup(
            soundRules = listOf(SoundRule(id = 4, name = "Work", matchType = SoundMatchType.KEYWORD, matchValue = "invoice")),
            priorityRules = listOf(CustomRule(id = 9, ruleName = "Urgent", keywordPattern = "invoice", assignedPriority = null, assignedCategory = null)),
            lightRules = listOf(LightRule(id = 12, name = "Glow", matchType = com.priosort.app.domain.LightMatchType.APP, matchValue = "example.app"))
        )
        val restored = RuleBackupCodec.decodeForImport(RuleBackupCodec.encode(original))

        assertEquals("Work", restored.soundRules.single().name)
        assertEquals("Urgent", restored.priorityRules.single().ruleName)
        assertEquals("Glow", restored.lightRules.single().name)
        assertEquals(3, RuleBackupCodec.preview(RuleBackupCodec.encode(original)).let { it.soundRuleCount + it.priorityRuleCount + it.lightRuleCount })
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsUnsupportedBackupVersion() {
        RuleBackupCodec.decodeForImport("""{"formatVersion":99,"soundRules":[],"priorityRules":[],"lightRules":[]}""")
    }

    @Test fun upgradesReferenceSnapshotBackupDefaults() {
        val legacy = """{"formatVersion":1,"soundRules":[{"id":3,"name":"Work","matchType":"APP","matchValue":"example.app","soundUri":"content://old/install/uri"}],"priorityRules":[],"lightRules":[]}"""

        val upgraded = RuleBackupCodec.decodeForImport(legacy)

        assertEquals(2, upgraded.formatVersion)
        assertEquals(0.8f, upgraded.soundRules.single().volume)
        assertEquals(listOf(0L, 140L), upgraded.soundRules.single().vibrationPatternMs)
        assertNull(upgraded.soundRules.single().soundUri)
    }
}
