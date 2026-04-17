package com.aipoweredgita.app.notifications

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
import com.aipoweredgita.app.R
import com.aipoweredgita.app.database.GitaDatabase

class DailyReflectionWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val context = applicationContext
            createNotificationChannel(context)

            val db = GitaDatabase.getDatabase(context)
            val stats = db.userStatsDao().getUserStatsOnce()
            val level = com.aipoweredgita.app.ui.components.LotusLevelManager.levelFor(stats)

            val prompt = when (level) {
                in 0..1 -> "What duty can you perform today without attachment to the result?"
                2 -> "Recall a moment you practiced devotion. How did it change your day?"
                3 -> "What truth have you learned recently, and how will you apply it?"
                else -> "How will you balance action, devotion, and knowledge today?"
            }

            // Create intent to open app when notification is tapped
            val intent = Intent(context, com.aipoweredgita.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_book)
                .setContentTitle("Daily Reflection")
                .setContentText(prompt)
                .setStyle(NotificationCompat.BigTextStyle().bigText(prompt))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            // Show notification with permission handling
            try {
                NotificationManagerCompat.from(context).notify(NOTIF_ID, notification)
            } catch (e: SecurityException) {
                // Permission not granted or other security issue, skip notification
                return Result.success()
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Daily Reflection", NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = "Daily spiritual reflection prompts"
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_reflection_channel"
        const val NOTIF_ID = 2002
    }
}

