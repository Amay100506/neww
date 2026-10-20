package com.priosort.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.priosort.app.data.local.entities.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications WHERE isExcluded = 0 AND isArchived = 0 AND (snoozedUntilMs IS NULL OR snoozedUntilMs <= :currentTimeMs) ORDER BY isPinned DESC, receivedAt DESC")
    fun getAllActiveNotifications(currentTimeMs: Long = System.currentTimeMillis()): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isArchived = 1 ORDER BY receivedAt DESC")
    fun getArchivedNotifications(): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE isExcluded = 0 AND isArchived = 0 AND snoozedUntilMs > :currentTimeMs ORDER BY snoozedUntilMs ASC")
    fun getSnoozedNotifications(currentTimeMs: Long = System.currentTimeMillis()): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications WHERE id = :id LIMIT 1")
    suspend fun getNotificationById(id: Long): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE eventKey = :eventKey LIMIT 1")
    suspend fun getNotificationByEventKey(eventKey: String): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(entity: NotificationEntity): Long

    @Update
    suspend fun updateNotification(entity: NotificationEntity)

    @Query("UPDATE notifications SET priorityLevel = :newPriority, priorityOverrideReason = :reason WHERE id = :id")
    suspend fun overridePriority(id: Long, newPriority: String, reason: String)

    @Query("UPDATE notifications SET category = :newCategory WHERE id = :id")
    suspend fun overrideCategory(id: Long, newCategory: String)

    @Query("UPDATE notifications SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE notifications SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE notifications SET isSnoozed = 1, snoozedUntilMs = :untilMs WHERE id = :id")
    suspend fun snoozeNotification(id: Long, untilMs: Long)

    @Query("UPDATE notifications SET isSnoozed = 0, snoozedUntilMs = null WHERE id = :id")
    suspend fun unsnoozeNotification(id: Long)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("DELETE FROM notifications WHERE retentionExpiresAt IS NOT NULL AND retentionExpiresAt < :currentTimeMs")
    suspend fun deleteExpiredRecords(currentTimeMs: Long): Int

    // Analytics queries
    @Query("SELECT COUNT(*) FROM notifications WHERE isExcluded = 0 AND isArchived = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE priorityLevel = :priority AND isExcluded = 0 AND isArchived = 0")
    fun getCountByPriority(priority: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE otpDetected = 1 AND isExcluded = 0")
    fun getOtpCount(): Flow<Int>

    // Financial Ledger Analytics
    @Query("SELECT SUM(transactionAmount) FROM notifications WHERE transactionType = 'DEBIT' AND isExcluded = 0")
    fun getTotalDebits(): Flow<Double?>

    @Query("SELECT SUM(transactionAmount) FROM notifications WHERE transactionType = 'CREDIT' AND isExcluded = 0")
    fun getTotalCredits(): Flow<Double?>

    @Query("SELECT * FROM notifications WHERE transactionAmount IS NOT NULL AND isExcluded = 0 ORDER BY receivedAt DESC LIMIT 20")
    fun getRecentTransactions(): Flow<List<NotificationEntity>>
}
