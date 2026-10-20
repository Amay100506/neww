package com.priosort.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel

@Entity(
    tableName = "notifications",
    indices = [
        Index(value = ["eventKey"], unique = true),
        Index(value = ["packageName"]),
        Index(value = ["priorityLevel"]),
        Index(value = ["receivedAt"]),
        Index(value = ["isExcluded"])
    ]
)
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val eventKey: String,
    val packageName: String,
    val applicationName: String?,
    val title: String?,
    val body: String?,
    val normalizedText: String,
    val category: String,
    val categoryConfidence: Float?,
    val priorityLevel: String,
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
) {
    fun toDomain(): NotificationRecord {
        return NotificationRecord(
            id = id,
            eventKey = eventKey,
            packageName = packageName,
            applicationName = applicationName,
            title = title,
            body = body,
            normalizedText = normalizedText,
            category = NotificationCategory.fromString(category),
            categoryConfidence = categoryConfidence,
            priorityLevel = PriorityLevel.fromCode(priorityLevel),
            basePriorityScore = basePriorityScore,
            urgencyScore = urgencyScore,
            actionScore = actionScore,
            deadlineScore = deadlineScore,
            importanceScore = importanceScore,
            recencyScore = recencyScore,
            otpDetected = otpDetected,
            actionDetected = actionDetected,
            actionDescription = actionDescription,
            deadlineText = deadlineText,
            deadlineAt = deadlineAt,
            deadlineResolutionStatus = deadlineResolutionStatus,
            priorityOverrideReason = priorityOverrideReason,
            explanationJson = explanationJson,
            receivedAt = receivedAt,
            processedAt = processedAt,
            isRead = isRead,
            isPinned = isPinned,
            isExcluded = isExcluded,
            isArchived = isArchived,
            isSnoozed = isSnoozed,
            snoozedUntilMs = snoozedUntilMs,
            isVip = isVip,
            deliveryStatus = deliveryStatus,
            transactionAmount = transactionAmount,
            transactionType = transactionType,
            retentionExpiresAt = retentionExpiresAt
        )
    }

    companion object {
        fun fromDomain(record: NotificationRecord): NotificationEntity {
            return NotificationEntity(
                id = record.id,
                eventKey = record.eventKey,
                packageName = record.packageName,
                applicationName = record.applicationName,
                title = record.title,
                body = record.body,
                normalizedText = record.normalizedText,
                category = record.category.name,
                categoryConfidence = record.categoryConfidence,
                priorityLevel = record.priorityLevel.code,
                basePriorityScore = record.basePriorityScore,
                urgencyScore = record.urgencyScore,
                actionScore = record.actionScore,
                deadlineScore = record.deadlineScore,
                importanceScore = record.importanceScore,
                recencyScore = record.recencyScore,
                otpDetected = record.otpDetected,
                actionDetected = record.actionDetected,
                actionDescription = record.actionDescription,
                deadlineText = record.deadlineText,
                deadlineAt = record.deadlineAt,
                deadlineResolutionStatus = record.deadlineResolutionStatus,
                priorityOverrideReason = record.priorityOverrideReason,
                explanationJson = record.explanationJson,
                receivedAt = record.receivedAt,
                processedAt = record.processedAt,
                isRead = record.isRead,
                isPinned = record.isPinned,
                isExcluded = record.isExcluded,
                isArchived = record.isArchived,
                isSnoozed = record.isSnoozed,
                snoozedUntilMs = record.snoozedUntilMs,
                isVip = record.isVip,
                deliveryStatus = record.deliveryStatus,
                transactionAmount = record.transactionAmount,
                transactionType = record.transactionType,
                retentionExpiresAt = record.retentionExpiresAt
            )
        }
    }
}
