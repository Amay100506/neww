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
        val suppressionMessage: String?
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
            it.packageName == newPackageName && (nowMs - it.receivedAt) <= windowMs
        }

        if (sameAppRecents.isEmpty()) {
            return BurstAnalysisResult(false, 1, null)
        }

        // Count similar notifications in window
        var matchingCount = 1
        for (rec in sameAppRecents) {
            val titleMatch = !newTitle.isNullOrBlank() && rec.title.equals(newTitle, ignoreCase = true)
            val bodySimilar = !newBody.isNullOrBlank() && !rec.body.isNullOrBlank() && (
                    rec.body.contains(newBody, ignoreCase = true) || newBody.contains(rec.body, ignoreCase = true)
                    )

            if (titleMatch || bodySimilar) {
                matchingCount++
            }
        }

        return if (matchingCount >= 3) {
            BurstAnalysisResult(
                isBurst = true,
                burstCount = matchingCount,
                suppressionMessage = "Notification burst detected ($matchingCount alerts from ${sameAppRecents.firstOrNull()?.applicationName ?: newPackageName} in <3m)"
            )
        } else {
            BurstAnalysisResult(false, matchingCount, null)
        }
    }
}
