package com.priosort.app.intelligence

import com.priosort.app.domain.SoundRule

/** Pure policy for deciding which configured sound-rule effects may run. */
object SoundDecisionResolver {
    data class Decision(
        val playAudio: Boolean,
        val vibrate: Boolean,
        val explanation: String
    )

    fun resolve(
        rule: SoundRule?,
        soundEnabled: Boolean,
        inQuietHours: Boolean,
        audioAllowed: Boolean,
        vibrationEnabled: Boolean,
        vibrationAllowed: Boolean
    ): Decision {
        if (!soundEnabled) return Decision(false, false, "Suppressed: custom sound and vibration effects are disabled")
        if (rule == null) return Decision(false, false, "No enabled sound rule matches")
        if (rule.silent) return Decision(false, false, "Suppressed by silent rule '${rule.name}'")
        if (inQuietHours && !rule.allowDuringQuietHours) {
            return Decision(false, false, "Suppressed: quiet hours are active")
        }

        val playAudio = !rule.soundUri.isNullOrBlank() && audioAllowed
        val vibrate = rule.vibrationEnabled && vibrationEnabled && vibrationAllowed
        if (!playAudio && !vibrate) {
            val reason = when {
                !rule.soundUri.isNullOrBlank() && !audioAllowed -> "Device ringer or Do Not Disturb blocks sound"
                rule.vibrationEnabled && vibrationEnabled && !vibrationAllowed -> "Device settings block vibration"
                else -> "No permitted sound or vibration action is configured"
            }
            return Decision(false, false, "Suppressed: $reason")
        }

        val actions = buildList {
            if (playAudio) add("Would play '${rule.name}'")
            else if (!rule.soundUri.isNullOrBlank()) add("Sound blocked by device ringer or Do Not Disturb")
            if (vibrate) add("would vibrate")
            else if (rule.vibrationEnabled && vibrationEnabled) add("vibration blocked by device settings")
        }
        return Decision(playAudio, vibrate, actions.joinToString(" · "))
    }
}
