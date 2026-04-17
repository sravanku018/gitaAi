package com.aipoweredgita.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.aipoweredgita.app.MainActivity
import com.aipoweredgita.app.R

/**
 * Manages notification for offline verse download completion.
 */
class OfflineDownloadNotificationManager(private val context: Context) {

    private val CHANNEL_ID = "offline_download_channel"
    private val NOTIFICATION_ID = 7001
    private val INCOMPLETE_NOTIFICATION_ID = 7002
    private val IN_PROGRESS_NOTIFICATION_ID = 7003

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Offline Downloads",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for verse downloads"
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

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
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle("All verses downloaded")
            .setContentText("You can read offline now")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    fun showIncompleteNotification(missingCount: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action: Retry missing via WorkManager
        val retryIntent = Intent(OfflineRetryReceiver.ACTION_RETRY_MISSING).apply {
            `package` = context.packageName
        }
        val retryPending = PendingIntent.getBroadcast(
            context,
            1,
            retryIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (missingCount > 0) "Incomplete offline download" else "Offline download status"
        val body = if (missingCount > 0) {
            "$missingCount verses missing. Tap to resume."
        } else {
            "Some verses may be missing."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_menu_book, context.getString(R.string.generic_retry), retryPending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(INCOMPLETE_NOTIFICATION_ID, notification)
    }

    fun showInProgressNotification() {
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
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle("Downloading verses")
            .setContentText("Download is running in background")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(IN_PROGRESS_NOTIFICATION_ID, notification)
    }

    fun cancelInProgressNotification() {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(IN_PROGRESS_NOTIFICATION_ID)
    }
}
