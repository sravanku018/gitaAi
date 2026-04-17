package com.aipoweredgita.app.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.ml.DatasetIngestionPipeline
import java.util.concurrent.TimeUnit

class QuestionIngestionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "QuestionIngestionWorker"
        private const val WORK_NAME = "question_ingestion"
        private const val NOTIFICATION_ID = 8010

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<QuestionIngestionWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP, // Keep existing so it doesn't re-run if already successful/running
                request
            )
            Log.d(TAG, "Question Ingestion scheduled")
        }
    }

    override suspend fun doWork(): Result {
        val database = GitaDatabase.getDatabase(applicationContext)
        val dao = database.quizQuestionBankDao()
        val pipeline = DatasetIngestionPipeline(applicationContext, dao)

        // Only ingest if we don't have enough questions
        if (pipeline.hasQuestions()) {
            Log.d(TAG, "Questions already present, skipping ingestion")
            return Result.success()
        }

        return try {
            Log.d(TAG, "Starting automatic question ingestion...")
            showNotification("Preparing Quiz Content", "Setting up Bhagavad Gita questions...")

            val importedCount = pipeline.ingestDataset(language = "english") { imported, total ->
                val progress = if (total > 0) (imported * 100 / total) else 0
                updateNotification("Downloading Quiz Content", "$imported questions imported...", progress)
                setProgressAsync(workDataOf("progress" to progress, "imported" to imported))
            }

            Log.d(TAG, "Successfully imported $importedCount questions")
            showCompletionNotification(importedCount)
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Question ingestion failed", e)
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    private fun showNotification(title: String, content: String) {
        val channelId = "question_ingestion_channel"
        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(title: String, content: String, progress: Int) {
        val channelId = "question_ingestion_channel"
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setProgress(100, progress, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(count: Int) {
        val channelId = "question_ingestion_channel"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Quiz Content Ready")
            .setContentText("$count Bhagavad Gita questions are ready for you.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Content Updates"
            val descriptionText = "Notifications for quiz content updates"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
