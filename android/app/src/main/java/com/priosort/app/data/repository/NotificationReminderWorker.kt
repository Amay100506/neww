package com.priosort.app.data.repository

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.priosort.app.PrioSortApplication
import com.priosort.app.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** Posts only a generic local reminder; it never puts notification message text in the reminder payload. */
class NotificationReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val notificationId = inputData.getLong(KEY_NOTIFICATION_ID, -1L)
        if (notificationId <= 0L) return@withContext Result.success()
        val app = applicationContext as? PrioSortApplication ?: return@withContext Result.failure()
        val record = app.database.notificationDao().getNotificationById(notificationId)
            ?: return@withContext Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) return@withContext Result.success()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
            ?: return@withContext Result.retry()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "PrioSort reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val appName = record.applicationName?.takeIf(String::isNotBlank) ?: "an app"
        val openApp = PendingIntent.getActivity(
            applicationContext,
            notificationId.hashCode(),
            Intent(applicationContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("PrioSort reminder")
            .setContentText("You asked to review an alert from $appName.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("You asked to review an alert from $appName. Open PrioSort when you're ready."))
            .setAutoCancel(true)
            .setContentIntent(openApp)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(applicationContext).notify(notificationId.toInt(), notification) }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.success() })
    }

    companion object {
        internal const val KEY_NOTIFICATION_ID = "notification_id"
        internal const val CHANNEL_ID = "priosort_reminders"
    }
}

object NotificationReminderScheduler {
    fun schedule(context: Context, notificationId: Long, remindAtMs: Long) {
        val delay = (remindAtMs - System.currentTimeMillis()).coerceAtLeast(0L)
        val request = androidx.work.OneTimeWorkRequestBuilder<NotificationReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(
                androidx.work.workDataOf(
                    NotificationReminderWorker.KEY_NOTIFICATION_ID to notificationId
                )
            )
            .build()
        androidx.work.WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "priosort_reminder_$notificationId",
            androidx.work.ExistingWorkPolicy.REPLACE,
            request
        )
    }
}
