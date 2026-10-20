package com.priosort.app.data.repository

import com.priosort.app.data.local.NotificationDao
import com.priosort.app.data.local.entities.NotificationEntity
import com.priosort.app.domain.NotificationCategory
import com.priosort.app.domain.NotificationRecord
import com.priosort.app.domain.PriorityLevel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

    suspend fun saveNotification(record: NotificationRecord): Long {
        val entity = NotificationEntity.fromDomain(record)
        return notificationDao.insertNotification(entity)
    }

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

    // Analytics
    val totalCount: Flow<Int> = notificationDao.getTotalCount()
    val otpCount: Flow<Int> = notificationDao.getOtpCount()

    val totalDebits: Flow<Double> = notificationDao.getTotalDebits().map { it ?: 0.0 }
    val totalCredits: Flow<Double> = notificationDao.getTotalCredits().map { it ?: 0.0 }
    val recentTransactions: Flow<List<NotificationRecord>> = notificationDao.getRecentTransactions().map { entities ->
        entities.map { it.toDomain() }
    }

    fun getCountByPriority(level: PriorityLevel): Flow<Int> {
        return notificationDao.getCountByPriority(level.code)
    }
}
