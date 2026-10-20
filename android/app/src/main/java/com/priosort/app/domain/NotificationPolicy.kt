package com.priosort.app.domain

/**
 * Configurable policy parameters for priority evaluation and privacy retention.
 */
data class NotificationPolicy(
    val tauHours: Float = 24.0f,
    val criticalThreshold: Float = 80.0f,
    val highThreshold: Float = 60.0f,
    val mediumThreshold: Float = 35.0f,
    val lowThreshold: Float = 15.0f,
    val isRedactionEnabled: Boolean = true,
    val retentionDurationDays: Int = 7
)
