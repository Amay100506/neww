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

    @Query("SELECT * FROM notifications WHERE otpDetected = 1")
    suspend fun getDetectedOtpNotifications(): List<NotificationEntity>

    @Query("SELECT * FROM notifications")
    suspend fun getAllNotificationsForPrivacyUpdate(): List<NotificationEntity>

    @Query("SELECT * FROM notifications WHERE eventKey = :eventKey LIMIT 1")
    suspend fun getNotificationByEventKey(eventKey: String): NotificationEntity?

    @Query("SELECT * FROM notifications WHERE packageName = :packageName AND eventKey != :excludeEventKey AND receivedAt BETWEEN :sinceMs AND :untilMs ORDER BY receivedAt DESC LIMIT 100")
    suspend fun getRecentByPackage(packageName: String, excludeEventKey: String, sinceMs: Long, untilMs: Long): List<NotificationEntity>

    @Query("UPDATE notifications SET isBurst = 1, burstCount = :burstCount WHERE eventKey IN (:eventKeys)")
    suspend fun markBurstGroup(eventKeys: List<String>, burstCount: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(entity: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationIfNew(entity: NotificationEntity): Long

    @Update
    suspend fun updateNotification(entity: NotificationEntity)

    @Query("UPDATE notifications SET priorityLevel = :newPriority, priorityOverrideReason = :reason, isManuallyOverridden = 1 WHERE id = :id")
    suspend fun overridePriority(id: Long, newPriority: String, reason: String)

    @Query("UPDATE notifications SET category = :newCategory, isManuallyOverridden = 1 WHERE id = :id")
    suspend fun overrideCategory(id: Long, newCategory: String)

    @Query("UPDATE notifications SET isPinned = :isPinned WHERE id = :id")
    suspend fun setPinned(id: Long, isPinned: Boolean)

    @Query("UPDATE notifications SET isRead = :isRead WHERE id = :id")
    suspend fun setRead(id: Long, isRead: Boolean)

    @Query("UPDATE notifications SET isArchived = :isArchived WHERE id = :id")
    suspend fun setArchived(id: Long, isArchived: Boolean)

    @Query("UPDATE notifications SET isActiveOnSystem = 0 WHERE eventKey = :eventKey")
    suspend fun markRemovedFromSystem(eventKey: String): Int

    @Query("UPDATE notifications SET isSnoozed = 1, snoozedUntilMs = :untilMs WHERE id = :id")
    suspend fun snoozeNotification(id: Long, untilMs: Long)

    @Query("UPDATE notifications SET isSnoozed = 0, snoozedUntilMs = null WHERE id = :id")
    suspend fun unsnoozeNotification(id: Long)

    @Query("UPDATE notifications SET isSnoozed = 0, snoozedUntilMs = null WHERE snoozedUntilMs IS NOT NULL AND snoozedUntilMs <= :currentTimeMs")
    suspend fun unsnoozeExpired(currentTimeMs: Long): Int

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notifications")
    suspend fun clearAllNotifications()

    @Query("DELETE FROM notifications WHERE eventKey LIKE 'demo_%' OR eventKey LIKE 'sim_%' OR eventKey LIKE 'custom_%'")
    suspend fun deleteSyntheticNotifications()

    @Query("DELETE FROM notifications WHERE retentionExpiresAt IS NOT NULL AND retentionExpiresAt < :currentTimeMs")
    suspend fun deleteExpiredRecords(currentTimeMs: Long): Int

    // Analytics queries
    @Query("SELECT COUNT(*) FROM notifications WHERE isExcluded = 0 AND isArchived = 0")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE priorityLevel = :priority AND isExcluded = 0 AND isArchived = 0")
    fun getCountByPriority(priority: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE otpDetected = 1 AND isExcluded = 0 AND isArchived = 0")
    fun getOtpCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isBurst = 1 AND isExcluded = 0 AND isArchived = 0")
    fun getBurstCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notifications WHERE isManuallyOverridden = 1 AND isExcluded = 0 AND isArchived = 0")
    fun getManualOverrideCount(): Flow<Int>

    // Financial Ledger Analytics
    @Query("SELECT transactionCurrency, transactionType, SUM(transactionAmount) AS total FROM notifications WHERE transactionAmount IS NOT NULL AND isExcluded = 0 AND isArchived = 0 GROUP BY transactionCurrency, transactionType ORDER BY transactionCurrency, transactionType")
    fun getTransactionCurrencyTotals(): Flow<List<TransactionCurrencyTotal>>

    @Query("SELECT * FROM notifications WHERE transactionAmount IS NOT NULL AND isExcluded = 0 AND isArchived = 0 AND (:query = '' OR LOWER(COALESCE(transactionMerchant, '')) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(applicationName, '')) LIKE '%' || LOWER(:query) || '%' OR LOWER(packageName) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(title, '')) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(body, '')) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(transactionType, '')) LIKE '%' || LOWER(:query) || '%' OR LOWER(COALESCE(transactionCurrency, '')) LIKE '%' || LOWER(:query) || '%' OR CAST(transactionAmount AS TEXT) LIKE '%' || :query || '%') ORDER BY receivedAt DESC")
    fun searchTransactions(query: String): Flow<List<NotificationEntity>>

    @Query("SELECT date(receivedAt / 1000, 'unixepoch', 'localtime') AS day, COUNT(*) AS count FROM notifications WHERE isExcluded = 0 AND isArchived = 0 AND receivedAt >= :startTimeMs GROUP BY day ORDER BY day")
    fun getDailyNotificationCounts(startTimeMs: Long): Flow<List<DailyNotificationCount>>

    @Query("SELECT packageName, title, body, receivedAt FROM notifications WHERE isExcluded = 0 AND isArchived = 0")
    fun getDuplicateAnalysisRecords(): Flow<List<DuplicateNotificationContent>>
}
