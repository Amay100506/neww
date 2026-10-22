package com.priosort.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.priosort.app.domain.LightEdge
import com.priosort.app.domain.LightMatchType
import com.priosort.app.domain.LightPattern
import com.priosort.app.domain.LightRepeatPolicy
import com.priosort.app.domain.LightRule

@Entity(tableName = "light_rules", indices = [Index(value = ["enabled", "matchType"])])
data class LightRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val matchType: String,
    val matchValue: String,
    val colorArgb: Int,
    val pattern: String,
    val repeatPolicy: String,
    val speed: Float,
    val durationMs: Long,
    val intensity: Float,
    val edges: String,
    val enabled: Boolean
) {
    fun toDomain() = LightRule(
        id = id,
        name = name,
        matchType = runCatching { LightMatchType.valueOf(matchType) }.getOrDefault(LightMatchType.KEYWORD),
        matchValue = matchValue,
        colorArgb = colorArgb,
        pattern = runCatching { LightPattern.valueOf(pattern) }.getOrDefault(LightPattern.PULSE),
        repeatPolicy = runCatching { LightRepeatPolicy.valueOf(repeatPolicy) }.getOrDefault(LightRepeatPolicy.UNTIL_TIMEOUT),
        speed = speed.coerceIn(0.25f, 3f),
        durationMs = durationMs.coerceIn(1_000, 20_000),
        intensity = intensity.coerceIn(0.1f, 1f),
        edges = edges.split(',').mapNotNull { runCatching { LightEdge.valueOf(it) }.getOrNull() }.toSet().ifEmpty { LightEdge.entries.toSet() },
        enabled = enabled
    )

    companion object {
        fun fromDomain(rule: LightRule) = LightRuleEntity(
            id = rule.id,
            name = rule.name.trim().take(100),
            matchType = rule.matchType.name,
            matchValue = rule.matchValue.trim().take(200),
            colorArgb = rule.colorArgb,
            pattern = rule.pattern.name,
            repeatPolicy = rule.repeatPolicy.name,
            speed = rule.speed.coerceIn(0.25f, 3f),
            durationMs = rule.durationMs.coerceIn(1_000, 20_000),
            intensity = rule.intensity.coerceIn(0.1f, 1f),
            edges = rule.edges.ifEmpty { LightEdge.entries.toSet() }.joinToString(",") { it.name },
            enabled = rule.enabled
        )
    }
}
