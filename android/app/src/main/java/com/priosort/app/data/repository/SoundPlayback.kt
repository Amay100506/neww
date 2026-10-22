package com.priosort.app.data.repository

import android.content.Context
import android.app.NotificationManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import com.priosort.app.domain.SoundMatchType
import com.priosort.app.domain.SoundRule
import java.util.concurrent.ConcurrentHashMap

object SoundPlayback {
    private val lastAlertAt = ConcurrentHashMap<Long, Long>()

    private fun isDndBlocking(context: Context): Boolean {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!manager.isNotificationPolicyAccessGranted) return false
        return manager.currentInterruptionFilter in setOf(
            NotificationManager.INTERRUPTION_FILTER_NONE,
            NotificationManager.INTERRUPTION_FILTER_ALARMS,
            NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
    }

    fun canPlayAudio(context: Context): Boolean = runCatching {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
        if (isDndBlocking(context)) return false
        true
    }.getOrDefault(false)

    fun canVibrate(context: Context): Boolean = runCatching {
        val audio = context.getSystemService(AudioManager::class.java)
        if (audio?.ringerMode == AudioManager.RINGER_MODE_SILENT || isDndBlocking(context)) return false
        context.getSystemService(Vibrator::class.java)?.hasVibrator() == true
    }.getOrDefault(false)

    @Synchronized
    fun acquireCooldown(rule: SoundRule, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (rule.cooldownMs <= 0 || rule.id <= 0) return true
        val last = lastAlertAt[rule.id]
        if (last != null && nowMs - last < rule.cooldownMs) return false
        lastAlertAt[rule.id] = nowMs
        return true
    }

    fun cooldownRemainingMs(rule: SoundRule, nowMs: Long = System.currentTimeMillis()): Long {
        if (rule.cooldownMs <= 0 || rule.id <= 0) return 0L
        val last = lastAlertAt[rule.id] ?: return 0L
        return (rule.cooldownMs - (nowMs - last)).coerceAtLeast(0L)
    }

    fun play(context: Context, rule: SoundRule, globalVolume: Float): Boolean {
        if (rule.silent || rule.soundUri.isNullOrBlank() || !canPlayAudio(context)) return false
        val player = MediaPlayer()
        val audioManager = context.getSystemService(AudioManager::class.java)
            ?: return runCatching { player.release() }.let { false }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val focusListener = AudioManager.OnAudioFocusChangeListener { }
        val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .build()
        if (audioManager.requestAudioFocus(focusRequest) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            player.release()
            return false
        }
        fun cleanup() {
            runCatching { audioManager.abandonAudioFocusRequest(focusRequest) }
            runCatching { player.release() }
        }
        return try {
            player.setAudioAttributes(attributes)
            player.setDataSource(context, Uri.parse(rule.soundUri))
            val volume = (rule.volume * globalVolume).coerceIn(0f, 1f)
            player.setVolume(volume, volume)
            var remaining = rule.repeatCount.coerceIn(1, 4)
            player.setOnCompletionListener {
                remaining--
                if (remaining > 0) runCatching { it.seekTo(0); it.start() }.onFailure { cleanup() }
                else cleanup()
            }
            player.setOnErrorListener { _, _, _ -> cleanup(); true }
            player.setOnPreparedListener { it.start() }
            player.prepareAsync()
            true
        } catch (_: Exception) {
            cleanup()
            false
        }
    }

    fun vibrate(context: Context, pattern: List<Long> = listOf(0L, 140L)) {
        runCatching {
            if (!canVibrate(context)) return
            val waveform = pattern.take(8).map { it.coerceIn(0, 5_000) }.toLongArray().takeIf { it.size >= 2 } ?: longArrayOf(0, 140)
            context.getSystemService(Vibrator::class.java)?.vibrate(
                VibrationEffect.createWaveform(waveform, -1)
            )
        }
    }

    fun preview(context: Context, soundUri: String, volume: Float, onError: () -> Unit = {}) {
        val previewRule = SoundRule(
            name = "Preview", matchType = SoundMatchType.KEYWORD,
            matchValue = "", soundUri = soundUri, volume = volume
        )
        if (!play(context, previewRule, 1f)) onError()
    }
}
