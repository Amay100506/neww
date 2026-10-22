package com.priosort.app.data.local

data class DuplicateNotificationContent(
    val packageName: String,
    val title: String?,
    val body: String?,
    val receivedAt: Long
)
