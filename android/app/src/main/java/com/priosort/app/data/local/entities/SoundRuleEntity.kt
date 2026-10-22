package com.priosort.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule

@Entity(tableName = "sound_rules")
data class SoundRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val matchType: String,
    val matchValue: String,
    val soundUri: String?,
    val volume: Float,
    val vibrationEnabled: Boolean,
    val silent: Boolean,
    val enabled: Boolean,
    @ColumnInfo(defaultValue = "'0,140'") val vibrationPatternMs: String = "0,140",
    @ColumnInfo(defaultValue = "0") val cooldownMs: Long = 0,
    @ColumnInfo(defaultValue = "1") val repeatCount: Int = 1,
    @ColumnInfo(defaultValue = "0") val allowDuringQuietHours: Boolean = false
) {
    fun toDomain() = SoundRule(
        id, name, runCatching { SoundMatchType.valueOf(matchType) }.getOrDefault(SoundMatchType.KEYWORD),
        matchValue, soundUri, volume, vibrationEnabled, silent, enabled,
        vibrationPatternMs.split(',').mapNotNull(String::toLongOrNull).take(8).ifEmpty { listOf(0L, 140L) },
        cooldownMs.coerceIn(0, 300_000), repeatCount.coerceIn(1, 4), allowDuringQuietHours
    )

    companion object {
        fun fromDomain(rule: SoundRule) = SoundRuleEntity(
            rule.id, rule.name, rule.matchType.name, rule.matchValue, rule.soundUri,
            rule.volume.coerceIn(0f, 1f), rule.vibrationEnabled, rule.silent, rule.enabled,
            rule.vibrationPatternMs.take(8).joinToString(",") { it.coerceIn(0, 5_000).toString() },
            rule.cooldownMs.coerceIn(0, 300_000), rule.repeatCount.coerceIn(1, 4), rule.allowDuringQuietHours
        )
    }
}
