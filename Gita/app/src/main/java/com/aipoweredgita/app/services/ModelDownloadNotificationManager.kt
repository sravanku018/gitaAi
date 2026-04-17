package com.aipoweredgita.app.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aipoweredgita.app.MainActivity
import com.aipoweredgita.app.R

/**
 * Manages notifications for ML model downloads
 * Shows progress updates without being intrusive
 */
class ModelDownloadNotificationManager(private val context: Context) {

    private val TAG = "ModelDownloadNotification"
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "model_download_channel"
    private val NOTIFICATION_ID = 42

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Model Download Progress",
                NotificationManager.IMPORTANCE_LOW  // Low importance: won't interrupt
            ).apply {
                description = "Shows progress of AI model downloads"
                setShowBadge(false)  // Don't show badge
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Created notification channel")
        }
    }

    /**
     * Show progress notification for ongoing download
     */
    fun showProgressNotification(
        modelName: String,
        progress: Int,
        totalModels: Int,
        currentModelIndex: Int
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Downloading AI Models")
            .setContentText("$modelName: $progress% (Model $currentModelIndex/$totalModels)")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)  // Only alert once per update
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Showing progress: $modelName - $progress%")
    }

    /**
     * Show completion notification
     */
    fun showCompletionNotification() {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("AI Models Downloaded")
            .setContentText("All models are ready for use")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)  // Dismiss when clicked
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVibrate(longArrayOf(500))  // Vibrate on completion
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.d(TAG, "Showing completion notification")
    }

    /**
     * Show error notification
     */
    fun showErrorNotification(error: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Model Download Failed")
            .setContentText(error)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(500, 300, 500))  // Pattern vibration
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
        Log.e(TAG, "Showing error notification: $error")
    }

    /**
     * Dismiss notification
     */
    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
        Log.d(TAG, "Dismissed notification")
    }

    /**
     * Show persistent foreground notification (for service)
     * Use when running ModelDownloadService in foreground
     */
    fun getForegroundNotification(
        modelName: String = "AI Models",
        progress: Int = 0
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Preparing AI Models")
            .setContentText("$modelName: $progress%")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .build()
    }
}
