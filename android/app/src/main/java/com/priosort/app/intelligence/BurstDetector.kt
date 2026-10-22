package com.priosort.app.intelligence

import com.priosort.app.domain.NotificationRecord
import kotlin.math.max

/**
 * Detects rapid burst notifications and near-duplicate alerts within a configurable time window.
 */
object BurstDetector {

    data class BurstAnalysisResult(
        val isBurst: Boolean,
        val burstCount: Int,
        val suppressionMessage: String?,
        val matchingEventKeys: List<String> = emptyList()
    )

    /**
     * Inspects recently received notifications from the same package within windowMs (default 3 minutes).
     */
    fun analyzeBurst(
        newTitle: String?,
        newBody: String?,
        newPackageName: String,
        recentNotifications: List<NotificationRecord>,
        windowMs: Long = 3 * 60 * 1000L,
        nowMs: Long = System.currentTimeMillis()
    ): BurstAnalysisResult {
        val sameAppRecents = recentNotifications.filter {
            val ageMs = nowMs - it.receivedAt
            it.packageName == newPackageName && ageMs in 0..windowMs
        }

        if (sameAppRecents.isEmpty()) {
            return BurstAnalysisResult(false, 1, null)
        }

        // Count similar notifications in window
        val matching = sameAppRecents.filter { rec ->
            val titleMatch = !newTitle.isNullOrBlank() && rec.title.equals(newTitle, ignoreCase = true)
            val bodySimilar = !newBody.isNullOrBlank() && !rec.body.isNullOrBlank() && (
                    rec.body.contains(newBody, ignoreCase = true) || newBody.contains(rec.body, ignoreCase = true)
                    )
            titleMatch || bodySimilar
        }
        val matchingCount = matching.size + 1

        return if (matchingCount >= 3) {
            BurstAnalysisResult(
                isBurst = true,
                burstCount = matchingCount,
                suppressionMessage = "Notification burst detected ($matchingCount alerts from ${matching.firstOrNull()?.applicationName ?: newPackageName} in <3m)",
                matchingEventKeys = matching.map { it.eventKey }
            )
        } else {
            BurstAnalysisResult(false, matchingCount, null)
        }
    }
}
