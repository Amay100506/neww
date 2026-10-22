package com.priosort.app.data.export

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.priosort.app.domain.RuleBackup
import com.priosort.app.domain.SoundMatchType

data class RuleBackupPreview(
    val soundRuleCount: Int,
    val priorityRuleCount: Int,
    val lightRuleCount: Int
)

/** Versioned, bounded rule backup. Notification content and device-specific sound URIs are excluded. */
object RuleBackupCodec {
    private const val MAX_BYTES = 1_000_000
    private const val MAX_RULES_PER_KIND = 200
    private val gson = Gson()

    fun encode(backup: RuleBackup): String {
        validate(backup)
        return gson.toJson(backup.copy(
            formatVersion = 2,
            soundRules = backup.soundRules.map { it.copy(soundUri = null) }
        ))
    }

    fun preview(json: String): RuleBackupPreview {
        val backup = decode(json)
        return RuleBackupPreview(backup.soundRules.size, backup.priorityRules.size, backup.lightRules.size)
    }

    fun decodeForImport(json: String): RuleBackup {
        val backup = decode(json)
        return backup.copy(soundRules = backup.soundRules.map { it.copy(soundUri = null) })
    }

    private fun decode(json: String): RuleBackup {
        require(json.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "Rules file is too large." }
        val root = runCatching { JsonParser.parseString(json).asJsonObject }.getOrNull() ?: error("Invalid rules file.")
        val version = root.get("formatVersion")?.asInt ?: 0
        require(version in 1..2) { "This rules file version is not supported." }
        addDefaults(root, "soundRules", mapOf(
            "id" to "0", "soundUri" to "null", "volume" to "0.8", "vibrationEnabled" to "true",
            "silent" to "false", "enabled" to "true", "vibrationPatternMs" to "[0,140]",
            "cooldownMs" to "0", "repeatCount" to "1", "allowDuringQuietHours" to "false"
        ))
        addDefaults(root, "priorityRules", mapOf("id" to "0", "targetField" to "\"ANY\"", "isEnabled" to "true"))
        addDefaults(root, "lightRules", mapOf(
            "id" to "0", "colorArgb" to "-45158", "pattern" to "\"PULSE\"",
            "repeatPolicy" to "\"UNTIL_TIMEOUT\"", "speed" to "1.0", "durationMs" to "5000",
            "intensity" to "0.8", "edges" to "[\"TOP\",\"RIGHT\",\"BOTTOM\",\"LEFT\"]", "enabled" to "true"
        ))
        val raw = runCatching { gson.fromJson(root, RuleBackup::class.java) }
            .getOrNull() ?: error("Invalid rules file.")
        val backup = raw.copy(
            formatVersion = 2,
            soundRules = raw.soundRules.orEmpty(),
            priorityRules = raw.priorityRules.orEmpty(),
            lightRules = raw.lightRules.orEmpty()
        )
        validate(backup)
        return backup
    }

    private fun addDefaults(root: JsonObject, field: String, defaults: Map<String, String>) {
        val rules = root.get(field) ?: JsonArray().also { root.add(field, it) }
        require(rules.isJsonArray) { "Invalid $field list." }
        rules.asJsonArray.forEach { item ->
            require(item.isJsonObject) { "Invalid rule entry." }
            val rule = item.asJsonObject
            defaults.forEach { (name, value) ->
                if (!rule.has(name)) rule.add(name, JsonParser.parseString(value))
            }
        }
    }

    private fun validate(backup: RuleBackup) {
        require(backup.soundRules.size <= MAX_RULES_PER_KIND && backup.priorityRules.size <= MAX_RULES_PER_KIND && backup.lightRules.size <= MAX_RULES_PER_KIND) {
            "Rules file contains too many rules."
        }
        require(backup.soundRules.all {
            it.id >= 0 && it.name.isNotBlank() && it.name.length <= 100 &&
                it.matchValue.isNotBlank() && it.matchValue.length <= 200 &&
                it.matchType in SoundMatchType.entries && it.volume.isFinite() && it.volume in 0f..1f &&
                it.cooldownMs in 0L..300_000L && it.repeatCount in 1..4 &&
                it.vibrationPatternMs.size <= 8 && it.vibrationPatternMs.all { duration -> duration in 0L..5_000L } &&
                (it.soundUri == null || it.soundUri.startsWith("content://") || it.soundUri.startsWith("android.resource://"))
        }) { "A sound rule has invalid fields." }
        require(backup.priorityRules.all {
            it.id >= 0 && it.ruleName.isNotBlank() && it.ruleName.length <= 100 &&
                it.keywordPattern.isNotBlank() && it.keywordPattern.length <= 200 &&
                it.targetField.uppercase() in setOf("ANY", "TITLE", "BODY", "PACKAGE")
        }) { "A priority rule has invalid fields." }
        require(backup.lightRules.all {
            it.id >= 0 && it.name.isNotBlank() && it.name.length <= 100 &&
                it.matchValue.isNotBlank() && it.matchValue.length <= 200 && it.edges.isNotEmpty() &&
                it.speed.isFinite() && it.speed in 0.2f..3f && it.intensity.isFinite() && it.intensity in 0f..1f &&
                it.durationMs in 1_000L..20_000L
        }) { "A light rule has invalid fields." }
    }
}
