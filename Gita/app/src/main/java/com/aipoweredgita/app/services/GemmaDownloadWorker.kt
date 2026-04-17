package com.aipoweredgita.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.io.File
import java.util.concurrent.TimeUnit

class GemmaDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "GemmaDownloadWorker"
        private const val WORK_NAME_GEMMA = "gemma_download"

        fun scheduleImmediateDownload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .build()

            val request = OneTimeWorkRequestBuilder<GemmaDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_GEMMA,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.d(TAG, "Scheduled immediate Gemma download")
        }

        fun scheduleBackgroundDownload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .build()

            val request = OneTimeWorkRequestBuilder<GemmaDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_GEMMA,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Scheduled background Gemma download (WiFi)")
        }

        fun cancelDownload(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_GEMMA)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(7010)
            notificationManager.cancel(7011)
            Log.d(TAG, "Cancelled Gemma download")
        }

        fun getDownloadStatusLive(context: Context): androidx.lifecycle.LiveData<WorkInfo?> {
            val live = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(WORK_NAME_GEMMA)
            val out = androidx.lifecycle.MediatorLiveData<WorkInfo?>()
            out.addSource(live) { list -> out.value = list.firstOrNull() }
            return out
        }

        fun isDownloading(context: Context): Boolean {
            val workInfos = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(WORK_NAME_GEMMA)
                .get()
            return workInfos?.any {
                it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
            } == true
        }
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "GemmaDownloadWorker started (attempt ${runAttemptCount + 1})")

            val manager = com.aipoweredgita.app.ml.ModelDownloadManager(applicationContext)
            val modelInfo = manager.getModelInfo("Gemma 4 2B") ?: return Result.failure()
            val gemmaFile = File(applicationContext.filesDir, "ml_models/${modelInfo.fileName}")
            val minSize = (modelInfo.expectedBytes * 0.9).toLong()

            if (gemmaFile.exists() && gemmaFile.length() >= minSize) {
                Log.d(TAG, "Gemma already downloaded (${gemmaFile.length() / (1024 * 1024)} MB), skipping")
                setProgressAsync(workDataOf("overallProgress" to 100))
                return Result.success()
            }

            val displayName = "${modelInfo.name} (${modelInfo.size})"
            startForegroundNotification(displayName)

            val success = manager.downloadModel(modelInfo.name) { prog ->
                setProgressAsync(workDataOf(
                    "currentModel" to prog.fileName,
                    "modelProgress" to prog.percent,
                    "overallProgress" to prog.percent
                ))
                updateForegroundNotification(displayName, prog.percent)
            }

            if (!success) {
                Log.w(TAG, "Gemma download incomplete or failed (attempt ${runAttemptCount + 1})")
                return if (runAttemptCount < 5) Result.retry() else Result.failure()
            }

            if (gemmaFile.exists() && gemmaFile.length() >= minSize) {
                Log.d(TAG, "Gemma downloaded successfully (${gemmaFile.length() / (1024 * 1024)} MB)")
                setProgressAsync(workDataOf("overallProgress" to 100))
                showCompletionNotification(displayName)
                Result.success()
            } else {
                Log.w(TAG, "Gemma file validation failed after download")
                if (runAttemptCount < 5) Result.retry() else Result.failure()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error downloading Gemma: ${e.message}", e)
            if (runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    private suspend fun startForegroundNotification(displayName: String) {
        val channelId = "gemma_download_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                channelId,
                "Model Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of AI model download"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading AI Engine")
            .setContentText("$displayName - Starting...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, 0, true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            setForeground(ForegroundInfo(
                7010,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            ))
        } else {
            setForeground(ForegroundInfo(7010, notification))
        }
    }

    private fun updateForegroundNotification(displayName: String, progress: Int) {
        val channelId = "gemma_download_channel"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("Downloading AI Engine")
            .setContentText("$displayName - $progress% complete")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(7010, notification)
    }

    private fun showCompletionNotification(displayName: String) {
        val channelId = "gemma_download_channel"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("AI Engine Ready")
            .setContentText("$displayName downloaded successfully. Features are now available.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(7011, notification)
    }
}
