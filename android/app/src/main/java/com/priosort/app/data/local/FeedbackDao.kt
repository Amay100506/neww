package com.priosort.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.priosort.app.data.local.entities.NotificationFeedbackEntity
import com.priosort.app.domain.NotificationFeedbackCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(feedback: NotificationFeedbackEntity): Long

    @Query("SELECT feedbackType, COUNT(*) AS count FROM notification_feedback GROUP BY feedbackType")
    fun observeCounts(): Flow<List<NotificationFeedbackCount>>

    @Query("DELETE FROM notification_feedback")
    suspend fun clear()
}
