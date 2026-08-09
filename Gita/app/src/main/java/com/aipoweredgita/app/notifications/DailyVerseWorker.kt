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
import com.aipoweredgita.app.network.GitaApi
import com.aipoweredgita.app.util.GitaConstants

class DailyVerseWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            val context = applicationContext
            createNotificationChannel(context)

            val database = GitaDatabase.getDatabase(context)
            
            // Get a meaningful random verse that hasn't been shown
            var verseData = database.cachedVerseDao().getRandomVerse()
            
            // If we ran out of verses, clear history and try again
            if (verseData == null && database.cachedVerseDao().getCachedCount() > 0) {
                 database.randomVerseHistoryDao().clearHistory()
                 verseData = database.cachedVerseDao().getRandomVerse()
            }

            val (chapter, verse) = if (verseData != null) {
                // Record history
                database.randomVerseHistoryDao().insertShownVerse(
                    com.aipoweredgita.app.database.RandomVerseHistory(
                        chapterNo = verseData.chapterNo,
                        verseNo = verseData.verseNo
                    )
                )
                verseData.chapterNo to verseData.verseNo
            } else {
                // Fallback if DB completely empty
                (2 to 47) 
            }

            // Try network first (optional, but we have text in DB usually)
            // Actually, if we got it from DB, use that text!
            val verseText: String = verseData?.verse?.replace("\\n", " ")?.replace("\n", " ")
                ?: try {
                    val v = GitaApi.retrofitService.getVerse(GitaConstants.DEFAULT_LANGUAGE, chapter, verse)
                    v.verse.replace("\\n", " ").replace("\n", " ")
                } catch (e: Exception) {
                    "Open app to read verse $chapter:$verse"
                }

            val content = if (verseText.length > 140) verseText.take(137) + "..." else verseText

            // Create intent to open app when notification is tapped
            val intent = Intent(context, com.aipoweredgita.app.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("NAVIGATE_TO", "random_sloka")
                putExtra("CHAPTER", chapter)
                putExtra("VERSE", verse)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_menu_book)
                .setContentTitle("Daily Gita Verse: $chapter:$verse")
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
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

            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }



    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Daily Verse"
            val descriptionText = "Daily Bhagavad Gita verse notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_verse_channel"
        const val NOTIF_ID = 1001
    }
}
