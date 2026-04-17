package com.aipoweredgita.app.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import android.content.pm.ServiceInfo
import androidx.work.*
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.repository.OfflineCacheRepository
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit

class OfflineVerseDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val tag = "OfflineVerseWorker"

    override suspend fun doWork(): Result {
        return try {
            val db = GitaDatabase.getDatabase(applicationContext)
            val repo = OfflineCacheRepository(db.cachedVerseDao())
            val notifications = OfflineDownloadNotificationManager(applicationContext)

            // Show background in-progress notification
            try { notifications.showInProgressNotification() } catch (_: Exception) {}

            // Promote to foreground for long-running work (Android 12+ requirements)
            try {
                val notification = NotificationCompat.Builder(applicationContext, "offline_download_channel")
                    .setSmallIcon(com.aipoweredgita.app.R.drawable.ic_menu_book)
                    .setContentTitle("Downloading verses")
                    .setContentText("Download is running in background")
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .build()
                // Modern approach for foreground service compatibility
                // For Android 12+ (S+) use setForeground(), for older versions use setForegroundAsync()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setForeground(ForegroundInfo(7003, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC))
                } else {
                    setForegroundAsync(ForegroundInfo(7003, notification))  // For backward compatibility with older Android versions
                }
            } catch (_: Exception) { }

            // Collect progress until completion
            repo.downloadAllVerses().collect { progress ->
                setProgressAsync(
                    workDataOf(
                        "current" to progress.current,
                        "total" to progress.total,
                        "percentage" to progress.percentage,
                        "status" to progress.status.name
                    )
                )
            }

            // Cancel in-progress and show completion/incomplete notifications
            try { notifications.cancelInProgressNotification() } catch (_: Exception) {}

            return if (repo.isFullyCached()) {
                try { notifications.showCompletionNotification() } catch (_: Exception) {}
                Result.success()
            } else {
                val missing = try { repo.getMissingVerses() } catch (_: Exception) { emptyList() }
                try { notifications.showIncompleteNotification(missing.size) } catch (_: Exception) {}
                // Succeed but notify incomplete to let user retry
                Result.success()
            }
        } catch (e: Exception) {
            Log.e(tag, "Offline verse download failed: ${e.message}", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "offline_verse_download"

        fun scheduleBackgroundDownload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(
                    if (com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_UNMETERED)
                        NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresCharging(com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_CHARGING)
                .setRequiresBatteryNotLow(com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_BATTERY_NOT_LOW)
                .build()

            val request = OneTimeWorkRequestBuilder<OfflineVerseDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
