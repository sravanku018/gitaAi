package com.aipoweredgita.app.examples

import android.content.Context
import com.aipoweredgita.app.notifications.*
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats

/**
 * Example usage of ML-powered Notification Engine
 *
 * This file demonstrates how to integrate and use the ML notification system
 * in your Gita learning app.
 */
class NotificationEngineExample {

    /**
     * Example 1: Basic daily notification setup
     * Call this from your MainActivity during app initialization
     */
    fun setupDailyNotifications(context: Context) {
        // Replace the old DailyVerseWorker with MLNotificationWorker
        MLNotificationWorker.scheduleMLNotifications(context)

        println("✅ ML-powered notifications scheduled!")
    }

    /**
     * Example 2: Send immediate notification
     * Use this when user achieves a milestone or needs streak alert
     */
    fun sendImmediateStreakAlert(context: Context) {
        MLNotificationWorker.scheduleOneTimeNotification(
            context = context,
            notificationType = NotificationType.STREAK_ALERT,
            delayMinutes = 0 // Send immediately
        )
        println("📱 Streak alert scheduled to send immediately")
    }

    /**
     * Example 3: Personalized daily notification
     * Call this to send a highly personalized daily reminder
     */
    suspend fun sendPersonalizedDailyReminder(context: Context) {
        val notificationManager = MLNotificationManager(context)
        val database = GitaDatabase.getDatabase(context)
        val userStats = database.userStatsDao().getUserStats()

        // The manager will automatically:
        // 1. Analyze user's engagement patterns
        // 2. Classify priority based on streak, accuracy, etc.
        // 3. Generate personalized content
        // 4. Send at optimal time

        val sent = notificationManager.sendNotification(
            type = NotificationType.DAILY_REMINDER
        )

        if (sent) {
            println("✨ Personalized notification sent!")
        } else {
            println("⏰ Notification scheduled for optimal time")
        }
    }

    /**
     * Example 4: Batch notifications for multiple events
     * Use this when user completes a quiz, achieves milestone, etc.
     */
    suspend fun sendBatchNotifications(context: Context) {
        val notificationManager = MLNotificationManager(context)

        val notifications = listOf(
            NotificationType.MILESTONE,
            NotificationType.QUIZ_READY,
            NotificationType.RECOMMENDATION
        )

        val results = notificationManager.sendBatchNotifications(notifications)

        results.forEach { result ->
            if (result.sent) {
                println("✅ Sent: ${result.type}")
            } else {
                println("⏰ Scheduled: ${result.type}")
            }
        }
    }

    /**
     * Example 5: Smart timing prediction
     * Check when the next optimal notification time is
     */
    suspend fun getOptimalNotificationTime(context: Context): String {
        val predictor = SmartTimingPredictor(context)
        val database = GitaDatabase.getDatabase(context)
        val userStats = database.userStatsDao().getUserStats()

        val optimalTime = predictor.predictOptimalTime(
            minGapHours = 4,
            streakDays = userStats.currentStreak
        )

        return "Next optimal time: $optimalTime"
    }

    /**
     * Example 6: Check user's active hours
     * Useful for scheduling notifications within user's active window
     */
    suspend fun getUserActiveHours(context: Context): List<Int> {
        val predictor = SmartTimingPredictor(context)
        return predictor.getTodaysActiveHours()
    }

    /**
     * Example 7: Custom notification with specific context
     * Use this for custom scenarios like "user hasn't read in 3 days"
     */
    suspend fun sendCustomStreakRecoveryNotification(context: Context) {
        val notificationManager = MLNotificationManager(context)
        val database = GitaDatabase.getDatabase(context)
        val userStats = database.userStatsDao().getUserStats()

        // Build custom context for streak recovery
        val customContext = NotificationContext(
            type = NotificationType.STREAK_RECOVERY,
            currentStreak = 0, // Streak broken
            longestStreak = userStats.longestStreak,
            daysSinceLastActive = 3,
            accuracy = userStats.accuracyPercentage,
            versesRead = userStats.distinctVersesRead,
            totalQuizzes = userStats.totalQuizzesTaken,
            chapterProgress = userStats.chaptersCompleted,
            weakAreas = listOf("Consistency"),
            userLevel = calculateUserLevel(userStats)
        )

        notificationManager.sendNotification(
            type = NotificationType.STREAK_RECOVERY,
            customContext = customContext
        )

        println("🎯 Custom streak recovery notification sent!")
    }

    /**
     * Example 8: Advanced - Integrating with UI events
     * Call this when user completes a quiz
     */
    suspend fun onQuizCompleted(context: Context, score: Int, totalQuestions: Int) {
        val notificationManager = MLNotificationManager(context)
        val database = GitaDatabase.getDatabase(context)
        val userStats = database.userStatsDao().getUserStats()

        val accuracy = (score.toFloat() / totalQuestions) * 100

        // Send appropriate notification based on score
        val notificationType = when {
            accuracy >= 90f -> NotificationType.ACHIEVEMENT
            accuracy >= 70f -> NotificationType.QUIZ_READY
            else -> NotificationType.RECOMMENDATION
        }

        notificationManager.sendNotification(notificationType)
        println("📊 Quiz completed - sent $notificationType notification")
    }

    /**
     * Example 9: Cancel all notifications
     * Use this when user disables notifications in settings
     */
    fun cancelAllNotifications(context: Context) {
        MLNotificationWorker.cancelNotifications(context)
        println("🛑 All notifications cancelled")
    }

    /**
     * Example 10: Testing notification priority classification
     * Useful for debugging why notifications are prioritized
     */
    suspend fun testPriorityClassification(context: Context) {
        val classifier = NotificationPriorityClassifier(context)
        val database = GitaDatabase.getDatabase(context)
        val userStats = database.userStatsDao().getUserStats()

        val context = NotificationContext(
            type = NotificationType.STREAK_ALERT,
            currentStreak = 7,
            longestStreak = 10,
            daysSinceLastActive = 1,
            accuracy = 75f,
            versesRead = 50,
            totalQuizzes = 20,
            chapterProgress = 3,
            weakAreas = emptyList(),
            userLevel = 5
        )

        val priority = classifier.classifyPriority(context)
        val action = classifier.getRecommendedAction(priority, NotificationType.STREAK_ALERT)

        println("Priority: $priority")
        println("Recommended Action: $action")
    }

    /**
     * Example 11: Full integration in MainActivity
     * This shows how to replace the old worker
     */
    fun updateMainActivityIntegration() {
        // OLD CODE (in MainActivity.kt):
        /*
        private fun scheduleDailyVerseWorker() {
            val work = PeriodicWorkRequestBuilder<DailyVerseWorker>(24, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "daily_verse_work",
                ExistingPeriodicWorkPolicy.KEEP,
                work
            )
        }
        */

        // NEW CODE (replace with):
        /*
        private fun scheduleMLNotifications() {
            MLNotificationWorker.scheduleMLNotifications(this)

            // Optional: Get optimal timing for debugging
            lifecycleScope.launch {
                val predictor = SmartTimingPredictor(this@MainActivity)
                val activeHours = predictor.getTodaysActiveHours()
                Log.d("MainActivity", "User's active hours: $activeHours")
            }
        }
        */
    }

    // Helper function
    private fun calculateUserLevel(userStats: UserStats): Int {
        val totalActivity = userStats.versesRead + userStats.totalQuizzesTaken
        return (totalActivity / 50) + 1
    }
}

/**
 * Usage Example in Activities/ViewModels:
 *
 * In QuizViewModel.kt, when quiz completes:
 */
object QuizNotificationExample {
    /*
    fun onQuizCompleted(score: Int, totalQuestions: Int) {
        viewModelScope.launch {
            val notificationManager = MLNotificationManager(context)
            val database = GitaDatabase.getDatabase(context)
            val userStats = database.userStatsDao().getUserStats()

            val accuracy = (score.toFloat() / totalQuestions) * 100

            // Update user stats first
            updateUserStatsAfterQuiz(score, totalQuestions)

            // Then send smart notification
            val notificationType = when {
                accuracy >= 90f -> NotificationType.ACHIEVEMENT
                userStats.currentStreak > 0 && userStats.currentStreak % 7 == 0 -> NotificationType.STREAK_ALERT
                else -> NotificationType.RECOMMENDATION
            }

            notificationManager.sendNotification(notificationType)
        }
    }
    */
}

/**
 * Usage Example in Settings:
 *
 * In UserPreferences settings:
 */
object SettingsNotificationExample {
    /*
    fun enableNotifications(enabled: Boolean) {
        if (enabled) {
            MLNotificationWorker.scheduleMLNotifications(context)
            notificationPreferences.setEnabled(true)
        } else {
            MLNotificationWorker.cancelNotifications(context)
            notificationPreferences.setEnabled(false)
        }
    }
    */
}

/**
 * Notification Types Summary:
 *
 * 1. DAILY_REMINDER - "Time for your daily verse"
 * 2. STREAK_ALERT - "Don't lose your X-day streak!"
 * 3. ACHIEVEMENT - "Quiz Master achievement unlocked!"
 * 4. RECOMMENDATION - "Focus on Karma Yoga today"
 * 5. MILESTONE - "100 verses completed! 🎉"
 * 6. WEAK_AREA_FOCUS - "Time to practice your weak areas"
 * 7. STREAK_RECOVERY - "Start fresh - new beginning!"
 * 8. QUIZ_READY - "Test your knowledge"
 * 9. CHAPTER_COMPLETE - "Chapter mastered!"
 *
 * Priority Levels:
 * - CRITICAL: Immediate, with vibration (streak alerts, major achievements)
 * - HIGH: Send within 15 min (achievements, milestones)
 * - MEDIUM: Batch send (recommendations, quiz ready)
 * - LOW: During optimal times (daily reminders)
 */
