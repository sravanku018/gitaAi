package com.aipoweredgita.app.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.ml.BhagavadGitaQAImporter
import java.util.concurrent.TimeUnit

class QuestionIngestionWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "QuestionIngestionWorker"
        private const val WORK_NAME = "question_ingestion"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresDeviceIdle(false)
                .setRequiresCharging(false)
                .setRequiresBatteryNotLow(false)
                .build()

            val request = OneTimeWorkRequestBuilder<QuestionIngestionWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Question ingestion scheduled")
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val database = GitaDatabase.getDatabase(applicationContext)
            val dao = database.quizQuestionBankDao()

            // Check if questions already exist
            val existingCount = dao.getTotalCount()
            if (existingCount > 200) {
                Log.d(TAG, "Questions already exist ($existingCount), skipping ingestion")
                return Result.success()
            }

            Log.d(TAG, "Starting automatic question ingestion from dataset...")
            val importer = BhagavadGitaQAImporter(applicationContext, dao)

            val imported = importer.importDataset(
                language = "english",
                batchSize = 500
            ) { count, total ->
                Log.d(TAG, "Importing: $count / $total")
            }

            Log.d(TAG, "✓ Auto-ingestion complete: $imported questions saved")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Auto-ingestion failed: ${e.message}", e)
            Result.retry()
        }
    }
}
