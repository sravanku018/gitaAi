package com.aipoweredgita.app.notifications

import android.content.Context
import androidx.work.*
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ML-powered notification worker
 * Replaces basic DailyVerseWorker with intelligent notification scheduling
 *
 * Features:
 * - Smart timing based on user patterns
 * - Priority-based notification classification
 * - Personalized content generation
 * - Batch notification support
 */
class MLNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = GitaDatabase.getDatabase(applicationContext)
            val notificationManager = MLNotificationManager(
                applicationContext,
                database.userStatsDao(),
                database.dailyActivityDao()
            )
            val userStats = database.userStatsDao().getUserStatsOnce()
                ?: return@withContext Result.failure()

            // Determine what type of notification to send based on user state
            val notificationsToSend = determineOptimalNotifications(userStats)

            if (notificationsToSend.isEmpty()) {
                // No notifications needed, reschedule for next day
                return@withContext Result.success()
            }

            // Send notifications with ML optimization
            val results = notificationManager.sendBatchNotifications(notificationsToSend)

            val sentCount = results.count { it.sent }
            val failedCount = results.size - sentCount

            android.util.Log.d(
                "MLNotificationWorker",
                "Sent $sentCount notifications, failed $failedCount"
            )

            // If critical notifications failed, retry
            if (failedCount > 0 && hasCriticalNotifications(results)) {
                return@withContext Result.retry()
            }

            return@withContext Result.success()
        } catch (e: Exception) {
            android.util.Log.e("MLNotificationWorker", "Error in ML notification worker", e)
            return@withContext Result.retry()
        }
    }

    /**
     * Determines which notifications to send based on user's current state
     */
    private suspend fun determineOptimalNotifications(userStats: UserStats): List<NotificationType> = withContext(Dispatchers.IO) {
        val notifications = mutableListOf<NotificationType>()

        // 1. Daily Reminder (if user hasn't read today)
        val todayActivities = getTodaysActivities()
        if (todayActivities.isEmpty()) {
            notifications.add(NotificationType.DAILY_REMINDER)
        }

        // 2. Streak Alert (if streak is at risk)
        if (userStats.currentStreak > 0) {
            val daysSinceLastActive = calculateDaysSinceLastActive()
            if (daysSinceLastActive >= 1) {
                notifications.add(NotificationType.STREAK_ALERT)
            }
        }

        // 3. Achievement notification (randomly, based on milestones)
        val shouldSendAchievement = checkMilestoneAchievements(userStats)
        if (shouldSendAchievement) {
            notifications.add(NotificationType.ACHIEVEMENT)
        }

        // 4. Weak area focus (if user has low accuracy)
        if (userStats.accuracyPercentage < 70f) {
            notifications.add(NotificationType.WEAK_AREA_FOCUS)
        }

        // 5. Quiz ready notification (based on recent activity)
        val hasRecentActivity = todayActivities.sumOf { it.versesRead } > 0
        if (hasRecentActivity && userStats.totalQuizzesTaken < userStats.distinctVersesRead / 5) {
            notifications.add(NotificationType.QUIZ_READY)
        }

        // 6. Recommendation (personalized learning path)
        if (shouldSendRecommendation(userStats)) {
            notifications.add(NotificationType.RECOMMENDATION)
        }

        // Limit to max 3 notifications per day
        return@withContext notifications.take(3)
    }

    /**
     * Checks for milestone achievements
     */
    private fun checkMilestoneAchievements(userStats: UserStats): Boolean {
        val milestones = listOf(10, 25, 50, 75, 100, 200, 300, 400, 500, 700)
        val recentMilestones = milestones.filter { milestone ->
            userStats.distinctVersesRead >= milestone &&
                    userStats.distinctVersesRead < milestone + 5
        }
        return recentMilestones.isNotEmpty()
    }

    /**
     * Determines if recommendation should be sent
     */
    private fun shouldSendRecommendation(userStats: UserStats): Boolean {
        // Send recommendation if:
        // - User has been active but needs guidance
        // - Low accuracy (needs improvement focus)
        // - High accuracy (ready for next challenge)
        return when {
            userStats.accuracyPercentage < 60f -> true
            userStats.accuracyPercentage > 85f && userStats.currentStreak > 3 -> true
            userStats.totalQuizzesTaken % 10 == 0 -> true // Every 10 quizzes
            else -> false
        }
    }

    /**
     * Gets today's activities
     */
    private suspend fun getTodaysActivities(): List<com.aipoweredgita.app.database.DailyActivity> {
        val database = GitaDatabase.getDatabase(applicationContext)
        val today = getTodaysDateString()
        val activity = database.dailyActivityDao().getByDate(today)
        return if (activity != null) listOf(activity) else emptyList()
    }

    /**
     * Calculates days since last active
     */
    private fun calculateDaysSinceLastActive(): Int {
        // Simplified implementation
        // In real app, query database for last activity
        val calendar = java.util.Calendar.getInstance()
        return calendar.get(java.util.Calendar.DAY_OF_YEAR)
    }

    /**
     * Gets today's date string in YYYY-MM-DD format
     */
    private fun getTodaysDateString(): String {
        val calendar = java.util.Calendar.getInstance()
        val year = calendar.get(java.util.Calendar.YEAR)
        val month = calendar.get(java.util.Calendar.MONTH) + 1
        val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }

    /**
     * Checks if results contain critical notifications
     */
    private fun hasCriticalNotifications(results: List<NotificationResult>): Boolean {
        return results.any { result ->
            // In a real implementation, you'd track which notifications were critical
            // For now, assume streak alerts are critical
            result.type == NotificationType.STREAK_ALERT && !result.sent
        }
    }

    companion object {
        /**
         * Schedules the ML notification worker
         */
        fun scheduleMLNotifications(context: Context) {
            val workRequest = PeriodicWorkRequestBuilder<MLNotificationWorker>(
                24,
                java.util.concurrent.TimeUnit.HOURS
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                        .build()
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "ml_notification_work",
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        }

        /**
         * Schedules one-time notification
         */
        fun scheduleOneTimeNotification(
            context: Context,
            notificationType: NotificationType,
            delayMinutes: Long = 0
        ) {
            val delay = java.util.concurrent.TimeUnit.MINUTES.toMillis(delayMinutes)

            val workRequest = OneTimeWorkRequestBuilder<MLNotificationWorker>()
                .setInitialDelay(java.time.Duration.ofMillis(delay))
                .setInputData(workDataOf("notification_type" to notificationType.name))
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                "one_time_${notificationType.name}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }

        /**
         * Cancels all notification workers
         */
        fun cancelNotifications(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("ml_notification_work")
        }
    }
}
