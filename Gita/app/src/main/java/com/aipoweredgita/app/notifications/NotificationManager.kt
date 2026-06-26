package com.aipoweredgita.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aipoweredgita.app.R
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.database.UserStatsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date

/**
 * ML-powered Notification Manager
 * Integrates smart timing, priority classification, and content personalization
 */
class MLNotificationManager(
    private val context: Context,
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao
) {

    private val timingPredictor = SmartTimingPredictor(context, dailyActivityDao)
    private val priorityClassifier = NotificationPriorityClassifier(context, userStatsDao, dailyActivityDao)
    private val contentPersonalizer = ContentPersonalizationEngine(context)

    /**
     * Main entry point for sending intelligent notifications
     */
    suspend fun sendNotification(
        type: NotificationType,
        customContext: NotificationContext? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val userStats = userStatsDao.getUserStatsOnce()
                ?: return@withContext false

            // Build notification context
            val notificationContext = customContext ?: buildNotificationContext(userStats)

            // Step 1: Classify priority
            val priority = priorityClassifier.classifyPriority(notificationContext)

            // Step 2: Check if should send now
            val shouldSend = priorityClassifier.shouldSendNow(priority, type)

            if (!shouldSend && priority != NotificationPriority.CRITICAL) {
                // Schedule for later optimal time
                scheduleForLater(type, priority)
                return@withContext false
            }

            // Step 3: Generate personalized content
            val content = contentPersonalizer.generatePersonalizedContent(type, userStats)

            // Step 4: Send notification
            sendWithPriority(type, content, priority)

            return@withContext true
        } catch (e: Exception) {
            android.util.Log.e("MLNotificationManager", "Error sending notification", e)
            return@withContext false
        }
    }

    /**
     * Sends batch notifications with ML optimization
     */
    suspend fun sendBatchNotifications(
        notifications: List<NotificationType>
    ): List<NotificationResult> = withContext(Dispatchers.IO) {
        val userStats = userStatsDao.getUserStatsOnce()
            ?: return@withContext emptyList()
        val notificationContext = buildNotificationContext(userStats)

        val results = mutableListOf<NotificationResult>()

        // Calculate priorities first (outside the sortedByDescending lambda)
        val notificationPriorities = notifications.associateWith { type ->
            priorityClassifier.classifyPriority(
                notificationContext.copy(type = type)
            ).level
        }

        // Sort by priority (highest first)
        val sortedNotifications = notifications.sortedByDescending { type ->
            notificationPriorities[type] ?: 0
        }

        for (notificationType in sortedNotifications) {
            val result = try {
                val sent = sendNotification(notificationType, notificationContext)
                NotificationResult(
                    type = notificationType,
                    sent = sent,
                    timestamp = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                NotificationResult(
                    type = notificationType,
                    sent = false,
                    error = e.message,
                    timestamp = System.currentTimeMillis()
                )
            }
            results.add(result)
        }

        return@withContext results
    }

    /**
     * Schedules notification for optimal time
     */
    private suspend fun scheduleForLater(type: NotificationType, priority: NotificationPriority) {
        val userStats = userStatsDao.getUserStatsOnce()
            ?: return

        val optimalTime = timingPredictor.predictOptimalTime(
            minGapHours = getMinGapHours(priority),
            streakDays = userStats.currentStreak
        )

        // In real implementation, you'd schedule using WorkManager with delay
        android.util.Log.d("MLNotificationManager", "Scheduled $type for ${optimalTime}")
    }

    /**
     * Sends notification with appropriate priority and styling
     */
    private fun sendWithPriority(
        type: NotificationType,
        content: PersonalizedContent,
        priority: NotificationPriority
    ) {
        createNotificationChannel(priority)

        val notificationBuilder = NotificationCompat.Builder(context, getChannelId(priority))
            .setSmallIcon(R.drawable.ic_menu_book)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(priority.androidPriority)
            .setAutoCancel(true)

        // Add big text style if available
        content.bigText?.let {
            notificationBuilder.setStyle(NotificationCompat.BigTextStyle().bigText(it))
        }

        // Add action button if suggested action exists
        content.suggestedAction?.let { action ->
            // In real app, add pending intent for action
            notificationBuilder.addAction(0, action, null)
        }

        val notification = notificationBuilder.build()

        // Add notification category for better organization
        notificationBuilder.setCategory(getNotificationCategory(type))

        val notificationId = generateNotificationId(type)
        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            android.util.Log.w("MLNotificationManager", "Notification permission not granted")
        }
    }

    /**
     * Builds comprehensive notification context from user stats
     */
    private suspend fun buildNotificationContext(userStats: UserStats): NotificationContext {
        // Get recent activities (last 7 days)
        val activities = getRecentActivities(7)
        val daysSinceLastActive = calculateDaysSinceLastActive(activities)
        val weakAreas = extractWeakAreas(userStats)
        val userLevel = calculateUserLevel(userStats)

        return NotificationContext(
            type = NotificationType.DAILY_REMINDER,
            currentStreak = userStats.currentStreak,
            longestStreak = userStats.longestStreak,
            daysSinceLastActive = daysSinceLastActive,
            accuracy = userStats.accuracyPercentage,
            versesRead = userStats.distinctVersesRead,
            totalQuizzes = userStats.totalQuizzesTaken,
            chapterProgress = userStats.chaptersCompleted,
            weakAreas = weakAreas,
            userLevel = userLevel
        )
    }

    /**
     * Gets recent activities for last N days
     */
    private suspend fun getRecentActivities(days: Int): List<com.aipoweredgita.app.database.DailyActivity> {
        val activities = mutableListOf<com.aipoweredgita.app.database.DailyActivity>()

        val calendar = Calendar.getInstance()
        repeat(days) {
            val dateString = "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            val activity = dailyActivityDao.getByDate(dateString)
            if (activity != null) {
                activities.add(activity)
            }
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        return activities
    }

    /**
     * Creates notification channel based on priority
     */
    private fun createNotificationChannel(priority: NotificationPriority) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = getChannelId(priority)
            val name = when (priority) {
                NotificationPriority.CRITICAL -> "Critical Notifications"
                NotificationPriority.HIGH -> "High Priority"
                NotificationPriority.MEDIUM -> "Regular Notifications"
                NotificationPriority.LOW -> "Low Priority"
            }

            val description = when (priority) {
                NotificationPriority.CRITICAL -> "Critical learning notifications - urgent messages"
                NotificationPriority.HIGH -> "High priority learning notifications"
                NotificationPriority.MEDIUM -> "Regular learning notifications"
                NotificationPriority.LOW -> "Optional learning notifications"
            }

            val importance = when (priority) {
                NotificationPriority.CRITICAL -> NotificationManager.IMPORTANCE_HIGH
                NotificationPriority.HIGH -> NotificationManager.IMPORTANCE_HIGH
                NotificationPriority.MEDIUM -> NotificationManager.IMPORTANCE_DEFAULT
                NotificationPriority.LOW -> NotificationManager.IMPORTANCE_LOW
            }

            val channel = NotificationChannel(channelId, name, importance).apply {
                setDescription(description)
                enableVibration(priority >= NotificationPriority.HIGH)
            }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Calculates days since last active
     */
    private fun calculateDaysSinceLastActive(activities: List<com.aipoweredgita.app.database.DailyActivity>): Int {
        if (activities.isEmpty()) return 7

        val lastActivityDate = activities.maxOfOrNull { activity ->
            // Parse date string to timestamp (simplified)
            activity.date
        } ?: return 7

        // Simplified calculation - in real app, parse properly
        return Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - lastActivityDate.toInt()
    }

    /**
     * Extracts weak areas from user stats
     */
    private fun extractWeakAreas(userStats: UserStats): List<String> {
        val weakAreas = mutableListOf<String>()
        if (userStats.accuracyPercentage < 70f) {
            weakAreas.add("Low Accuracy Areas")
        }
        if (userStats.currentStreak == 0) {
            weakAreas.add("Consistency")
        }
        return weakAreas
    }

    /**
     * Calculates user level
     */
    private fun calculateUserLevel(userStats: UserStats): Int {
        val totalActivity = userStats.versesRead + userStats.totalQuizzesTaken
        return (totalActivity / 50) + 1
    }

    /**
     * Gets minimum gap hours between notifications based on priority
     */
    private fun getMinGapHours(priority: NotificationPriority): Int {
        return when (priority) {
            NotificationPriority.CRITICAL -> 0
            NotificationPriority.HIGH -> 2
            NotificationPriority.MEDIUM -> 4
            NotificationPriority.LOW -> 8
        }
    }

    /**
     * Gets notification category for Android
     */
    private fun getNotificationCategory(type: NotificationType): String {
        return when (type) {
            NotificationType.STREAK_ALERT,
            NotificationType.STREAK_RECOVERY,
            NotificationType.ACHIEVEMENT,
            NotificationType.MILESTONE -> NotificationCompat.CATEGORY_REMINDER

            NotificationType.WEAK_AREA_FOCUS,
            NotificationType.RECOMMENDATION,
            NotificationType.QUIZ_READY -> NotificationCompat.CATEGORY_RECOMMENDATION

            NotificationType.DAILY_REMINDER -> NotificationCompat.CATEGORY_REMINDER
            NotificationType.CHAPTER_COMPLETE -> NotificationCompat.CATEGORY_STATUS
        }
    }

    /**
     * Generates unique notification ID
     */
    private fun generateNotificationId(type: NotificationType): Int {
        return type.hashCode() + System.currentTimeMillis().toInt()
    }

    /**
     * Gets channel ID for priority
     */
    private fun getChannelId(priority: NotificationPriority): String {
        return when (priority) {
            NotificationPriority.CRITICAL -> "critical_notifications"
            NotificationPriority.HIGH -> "high_priority_notifications"
            NotificationPriority.MEDIUM -> "regular_notifications"
            NotificationPriority.LOW -> "low_priority_notifications"
        }
    }
}

data class NotificationResult(
    val type: NotificationType,
    val sent: Boolean,
    val scheduledFor: Date? = null,
    val error: String? = null,
    val timestamp: Long
)
