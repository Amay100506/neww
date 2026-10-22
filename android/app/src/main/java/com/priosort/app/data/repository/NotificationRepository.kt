package com.priosort.app.data.repository

import com.priosort.app.data.local.NotificationDao
import com.priosort.app.data.local.DailyNotificationCount
import com.priosort.app.data.local.DuplicateNotificationContent
import com.priosort.app.data.local.entities.NotificationEntity
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import com.priosort.app.intelligence.OtpDetector
import com.priosort.app.util.TextRedactor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.ZoneId

class NotificationRepository(
    private val notificationDao: NotificationDao
) {

    val activeNotifications: Flow<List<NotificationRecord>> =
        notificationDao.getAllActiveNotifications().map { entities ->
            entities.map { it.toDomain() }
        }

    val archivedNotifications: Flow<List<NotificationRecord>> =
        notificationDao.getArchivedNotifications().map { entities ->
            entities.map { it.toDomain() }
        }

    val snoozedNotifications: Flow<List<NotificationRecord>> =
        notificationDao.getSnoozedNotifications().map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getNotificationById(id: Long): NotificationRecord? {
        return notificationDao.getNotificationById(id)?.toDomain()
    }

    suspend fun saveNotification(record: NotificationRecord): Boolean {
        val incoming = NotificationEntity.fromDomain(record)
        val existing = notificationDao.getNotificationByEventKey(record.eventKey)
        if (existing == null) {
            val insertedId = notificationDao.insertNotificationIfNew(incoming)
            if (insertedId != -1L) return true
        }
        val current = existing ?: notificationDao.getNotificationByEventKey(record.eventKey) ?: return false
        val update = incoming.copy(
            id = current.id,
            isRead = current.isRead,
            isPinned = current.isPinned,
            isArchived = current.isArchived,
            isSnoozed = current.isSnoozed,
            snoozedUntilMs = current.snoozedUntilMs,
            isManuallyOverridden = current.isManuallyOverridden,
            priorityLevel = if (current.isManuallyOverridden) current.priorityLevel else incoming.priorityLevel,
            category = if (current.isManuallyOverridden) current.category else incoming.category,
            priorityOverrideReason = if (current.isManuallyOverridden) current.priorityOverrideReason else incoming.priorityOverrideReason,
            isBurst = current.isBurst || incoming.isBurst,
            burstCount = maxOf(current.burstCount, incoming.burstCount),
            isActiveOnSystem = true
        )
        notificationDao.updateNotification(update)
        return false
    }

    suspend fun getNotificationByEventKey(eventKey: String): NotificationRecord? =
        notificationDao.getNotificationByEventKey(eventKey)?.toDomain()

    suspend fun getRecentForBurst(packageName: String, eventKey: String, sinceMs: Long, untilMs: Long): List<NotificationRecord> =
        notificationDao.getRecentByPackage(packageName, eventKey, sinceMs, untilMs).map { it.toDomain() }

    suspend fun markBurstGroup(eventKeys: List<String>, burstCount: Int) {
        if (eventKeys.isNotEmpty()) notificationDao.markBurstGroup(eventKeys, burstCount.coerceAtLeast(2))
    }

    suspend fun setRead(id: Long, isRead: Boolean) = notificationDao.setRead(id, isRead)

    suspend fun overridePriority(id: Long, newPriority: PriorityLevel, reason: String) {
        notificationDao.overridePriority(id, newPriority.code, reason)
    }

    suspend fun overrideCategory(id: Long, newCategory: NotificationCategory) {
        notificationDao.overrideCategory(id, newCategory.name)
    }

    suspend fun setPinned(id: Long, isPinned: Boolean) {
        notificationDao.setPinned(id, isPinned)
    }

    suspend fun setArchived(id: Long, isArchived: Boolean) {
        notificationDao.setArchived(id, isArchived)
    }

    suspend fun snoozeNotification(id: Long, untilMs: Long) {
        notificationDao.snoozeNotification(id, untilMs)
    }

    suspend fun unsnoozeNotification(id: Long) {
        notificationDao.unsnoozeNotification(id)
    }

    suspend fun deleteNotification(id: Long) {
        notificationDao.deleteById(id)
    }

    suspend fun clearAllNotifications() {
        notificationDao.clearAllNotifications()
    }

    suspend fun cleanupExpiredRecords(currentTimeMs: Long): Int {
        return notificationDao.deleteExpiredRecords(currentTimeMs)
    }

    suspend fun unsnoozeExpired(currentTimeMs: Long): Int = notificationDao.unsnoozeExpired(currentTimeMs)

    suspend fun markRemovedFromSystem(eventKey: String): Int = notificationDao.markRemovedFromSystem(eventKey)

    suspend fun redactExistingSensitiveNotifications(): Int {
        var updated = 0
        notificationDao.getAllNotificationsForPrivacyUpdate().forEach { entity ->
            val searchableText = listOfNotNull(entity.title, entity.body, entity.normalizedText).joinToString(" ")
            if (OtpDetector.detectOtp(searchableText).isOtp) {
                fun redact(value: String?): String? = value?.let(TextRedactor::redactSensitiveText)
                notificationDao.updateNotification(entity.copy(
                    title = redact(entity.title),
                    body = redact(entity.body),
                    normalizedText = redact(entity.normalizedText).orEmpty(),
                    actionDescription = redact(entity.actionDescription),
                    deadlineText = redact(entity.deadlineText),
                    priorityOverrideReason = redact(entity.priorityOverrideReason),
                    transactionMerchant = redact(entity.transactionMerchant),
                    explanationJson = redact(entity.explanationJson).orEmpty()
                ))
                updated++
            }
        }
        return updated
    }

    // Analytics
    val totalCount: Flow<Int> = notificationDao.getTotalCount()
    val otpCount: Flow<Int> = notificationDao.getOtpCount()
    val burstCount: Flow<Int> = notificationDao.getBurstCount()
    val manualOverrideCount: Flow<Int> = notificationDao.getManualOverrideCount()

    val transactionCurrencyTotals = notificationDao.getTransactionCurrencyTotals()
    fun searchTransactions(query: String): Flow<List<NotificationRecord>> = notificationDao.searchTransactions(query.trim()).map { entities ->
        entities.map { it.toDomain() }
    }
    val dailyNotificationCounts: Flow<List<DailyNotificationCount>> = notificationDao.getDailyNotificationCounts(
        LocalDate.now().minusDays(6).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    )
    val duplicateAnalysisRecords: Flow<List<DuplicateNotificationContent>> = notificationDao.getDuplicateAnalysisRecords()

    fun getCountByPriority(level: PriorityLevel): Flow<Int> {
        return notificationDao.getCountByPriority(level.code)
    }
}
