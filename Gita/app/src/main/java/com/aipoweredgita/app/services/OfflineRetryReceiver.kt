package com.aipoweredgita.app.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles retry action from incomplete download notification.
 * Enqueues WorkManager to resume/download missing verses in background.
 */
class OfflineRetryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_RETRY_MISSING) {
            try {
                OfflineVerseDownloadWorker.scheduleBackgroundDownload(context)
                Log.d(TAG, "Retry missing triggered from notification")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule retry: ${e.message}", e)
            }
        }
    }

    companion object {
        const val ACTION_RETRY_MISSING = "com.aipoweredgita.app.action.RETRY_MISSING"
        private const val TAG = "OfflineRetryReceiver"
    }
}

