package com.priosort.app.capture

import android.app.Notification
import android.content.Context
import android.content.pm.PackageManager
import android.service.notification.StatusBarNotification

object NotificationEventMapper {

    data class RawNotificationEvent(
        val eventKey: String,
        val packageName: String,
        val applicationName: String?,
        val title: String?,
        val body: String?,
        val senderIdentity: String?,
        val postTimeMs: Long
    )

    fun map(context: Context, sbn: StatusBarNotification): RawNotificationEvent {
        val packageName = sbn.packageName ?: "unknown.package"
        val postTime = if (sbn.postTime > 0) sbn.postTime else System.currentTimeMillis()
        val key = "${packageName}_${sbn.id}_${sbn.tag ?: "default"}"

        val notification = sbn.notification
        val extras = notification?.extras

        var title: String? = null
        var text: String? = null
        var senderIdentity: String? = null

        if (extras != null) {
            title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

            // If text is null, check BigText
            if (text.isNullOrBlank()) {
                text = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            }
            @Suppress("DEPRECATION")
            val messages = Notification.MessagingStyle.Message.getMessagesFromBundleArray(
                extras.getParcelableArray(Notification.EXTRA_MESSAGES)
            )
            senderIdentity = messages.lastOrNull()?.senderPerson?.name?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
                ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        }

        // Safe resolution of Application Name
        val appName = try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }

        return RawNotificationEvent(
            eventKey = key,
            packageName = packageName,
            applicationName = appName,
            title = title?.trim(),
            body = text?.trim(),
            senderIdentity = senderIdentity?.trim(),
            postTimeMs = postTime
        )
    }
}
