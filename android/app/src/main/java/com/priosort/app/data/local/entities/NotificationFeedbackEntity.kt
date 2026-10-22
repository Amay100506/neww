package com.priosort.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notification_feedback",
    foreignKeys = [ForeignKey(
        entity = NotificationEntity::class,
        parentColumns = ["id"],
        childColumns = ["notificationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["notificationId"]), Index(value = ["feedbackType"])]
)
data class NotificationFeedbackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationId: Long,
    val feedbackType: String,
    val createdAt: Long = System.currentTimeMillis()
)
