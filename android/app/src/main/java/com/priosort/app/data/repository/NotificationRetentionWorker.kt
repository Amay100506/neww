package com.priosort.app.data.repository

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.priosort.app.PrioSortApplication
import java.util.concurrent.TimeUnit

class NotificationRetentionWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val app = applicationContext as PrioSortApplication
        app.runMaintenance()
        Result.success()
    } catch (_: Exception) {
        Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "notification_retention_cleanup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<NotificationRetentionWorker>(
                1, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
