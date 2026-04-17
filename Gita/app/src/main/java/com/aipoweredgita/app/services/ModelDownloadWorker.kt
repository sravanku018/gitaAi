package com.aipoweredgita.app.services

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.work.*
import android.content.pm.ServiceInfo
import java.util.concurrent.TimeUnit

/**
 * WorkManager Worker for downloading ML models in background
 * - Respects device battery state and network conditions
 * - Persists even if app is killed
 * - Can be scheduled periodically or one-time
 */
class ModelDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val TAG = "ModelDownloadWorker"

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "ModelDownloadWorker started — TFLite models no longer required")

            // TFLite models (MiniLM, DistilBERT, MobileBERT) removed — keyword-based
            // ranking is used instead. This worker now does nothing.
            Log.d(TAG, "TFLite models skipped — using keyword ranking")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in ModelDownloadWorker: ${e.message}", e)
            Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "model_download"
        private const val WORK_NAME_IMMEDIATE = "model_download_immediate"

        /**
         * Schedule background model download
         * - Runs only when device is plugged in and has WiFi
         * - Respects doze and battery saver modes
         */
        fun scheduleBackgroundDownload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_CHARGING)
                .setRequiredNetworkType(
                    if (com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_UNMETERED)
                        NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_BATTERY_NOT_LOW)
                .build()
            val modelDownloadRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15,  // Initial backoff 15 minutes
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,  // Don't enqueue if already exists
                modelDownloadRequest
            )

            Log.d("ModelDownloadWorker", "Scheduled background download")
        }

        /**
         * Schedule immediate background download on any network (including 4G/5G).
         * This bypasses WiFi-only constraints and starts as soon as possible.
         * Perfect for "Try Now" button scenarios.
         */
        fun scheduleImmediateDownload(context: Context) {
            // Relaxed constraints - allow any network, no charging requirement
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // Any network (WiFi or cellular)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false) // Don't require charging
                .setRequiresBatteryNotLow(false) // Allow even if battery is low
                .build()

            val immediateRequest = OneTimeWorkRequestBuilder<ModelDownloadWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    5,  // Shorter initial backoff (5 minutes)
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE, // Replace any existing immediate download
                immediateRequest
            )

            Log.d("ModelDownloadWorker", "Scheduled immediate download (any network)")
        }

        /**
         * Schedule periodic model updates (weekly)
         */
        fun schedulePeriodicDownload(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_CHARGING)
                .setRequiredNetworkType(
                    if (com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_UNMETERED)
                        NetworkType.UNMETERED else NetworkType.CONNECTED
                )
                .setRequiresBatteryNotLow(com.aipoweredgita.app.util.FeatureFlags.DOWNLOADS_REQUIRE_BATTERY_NOT_LOW)
                .build()
            val periodicRequest = PeriodicWorkRequestBuilder<ModelDownloadWorker>(
                7,  // Repeat every 7 days
                TimeUnit.DAYS
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    15,
                    TimeUnit.MINUTES
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_periodic",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )

            Log.d("ModelDownloadWorker", "Scheduled periodic model updates")
        }

        /**
         * Cancel pending downloads
         */
        fun cancelDownload(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.d("ModelDownloadWorker", "Cancelled download")
        }

        /**
         * Get download status using modern flow-based API
         */
        fun getDownloadStatusLive(context: Context): androidx.lifecycle.LiveData<WorkInfo?> {
            val live = WorkManager.getInstance(context)
                .getWorkInfosForUniqueWorkLiveData(WORK_NAME)
            val out = androidx.lifecycle.MediatorLiveData<WorkInfo?>()
            out.addSource(live) { list -> out.value = list.firstOrNull() }
            return out
        }
    }
}
