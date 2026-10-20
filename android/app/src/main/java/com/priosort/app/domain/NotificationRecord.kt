package com.priosort.app.domain

/**
 * Domain representation of a prioritized notification record.
 */
data class NotificationRecord(
    val id: Long = 0,
    val eventKey: String,
    val packageName: String,
    val applicationName: String?,
    val title: String?,
    val body: String?,
    val normalizedText: String,
    val category: NotificationCategory,
    val categoryConfidence: Float?,
    val priorityLevel: PriorityLevel,
    val basePriorityScore: Float,
    val urgencyScore: Float,
    val actionScore: Float,
    val deadlineScore: Float,
    val importanceScore: Float,
    val recencyScore: Float,
    val otpDetected: Boolean,
    val actionDetected: Boolean,
    val actionDescription: String?,
    val deadlineText: String?,
    val deadlineAt: Long?,
    val deadlineResolutionStatus: String,
    val priorityOverrideReason: String?,
    val explanationJson: String,
    val receivedAt: Long,
    val processedAt: Long,
    val isRead: Boolean = false,
    val isPinned: Boolean = false,
    val isExcluded: Boolean = false,
    val isArchived: Boolean = false,
    val isSnoozed: Boolean = false,
    val snoozedUntilMs: Long? = null,
    val isVip: Boolean = false,
    val deliveryStatus: String? = null,
    val transactionAmount: Double? = null,
    val transactionType: String? = null,
    val retentionExpiresAt: Long? = null
)
