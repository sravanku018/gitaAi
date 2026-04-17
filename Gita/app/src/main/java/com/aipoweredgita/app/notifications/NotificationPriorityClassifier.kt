package com.aipoweredgita.app.notifications

import android.content.Context
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.database.UserStatsDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.round

/**
 * ML-powered notification priority classifier
 * Determines urgency level based on user data, learning patterns, and context
 */
enum class NotificationPriority(val level: Int, val androidPriority: Int, val channelImportance: Int) {
    CRITICAL(4, android.app.NotificationManager.IMPORTANCE_HIGH, 4),
    HIGH(3, android.app.NotificationManager.IMPORTANCE_HIGH, 3),
    MEDIUM(2, android.app.NotificationManager.IMPORTANCE_DEFAULT, 2),
    LOW(1, android.app.NotificationManager.IMPORTANCE_LOW, 1);

    companion object {
        fun fromLevel(level: Int): NotificationPriority {
            return when {
                level >= 4 -> CRITICAL
                level >= 3 -> HIGH
                level >= 2 -> MEDIUM
                else -> LOW
            }
        }
    }
}

enum class NotificationType {
    DAILY_REMINDER,
    STREAK_ALERT,
    ACHIEVEMENT,
    RECOMMENDATION,
    MILESTONE,
    WEAK_AREA_FOCUS,
    STREAK_RECOVERY,
    QUIZ_READY,
    CHAPTER_COMPLETE
}

data class NotificationContext(
    val type: NotificationType,
    val currentStreak: Int,
    val longestStreak: Int,
    val daysSinceLastActive: Int,
    val accuracy: Float,
    val versesRead: Int,
    val totalQuizzes: Int,
    val chapterProgress: Int,
    val weakAreas: List<String>,
    val userLevel: Int
)

/**
 * Classifies notification priority using ML-like scoring algorithm
 */
class NotificationPriorityClassifier(
    private val context: Context,
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao
) {

    /**
     * Classifies priority based on comprehensive user context
     */
    suspend fun classifyPriority(notificationContext: NotificationContext): NotificationPriority {
        val userStats = userStatsDao.getUserStatsOnce() ?: return NotificationPriority.LOW

        val score = withContext(Dispatchers.IO) {
            calculatePriorityScore(notificationContext, userStats)
        }

        return NotificationPriority.fromLevel(score)
    }

    /**
     * Calculates priority score (1-4) based on multiple factors
     */
    private suspend fun calculatePriorityScore(
        context: NotificationContext,
        userStats: UserStats
    ): Int {
        val weightedScore = 1.0 +
            (calculateStreakScore(context, userStats) * 0.4) +
            (calculateMomentumScore(context) * 0.3) +
            (calculateAchievementScore(context) * 0.2) +
            (calculatePersonalizationScore(context) * 0.1)

        // Clamp to valid range (1-4)
        return round(weightedScore).toInt().coerceIn(1, 4)
    }

    /**
     * Calculates streak-related priority
     */
    private fun calculateStreakScore(context: NotificationContext, userStats: UserStats): Int {
        return when {
            // Critical: Streak at risk
            context.currentStreak > 0 && context.daysSinceLastActive >= 1 -> 4
            context.currentStreak >= 7 && context.daysSinceLastActive >= 1 -> 3
            context.currentStreak >= 3 && context.daysSinceLastActive == 1 -> 2
            // High: Streak milestone
            context.currentStreak in listOf(7, 30, 100, 365) -> 3
            // Medium: Building streak
            context.currentStreak > 0 -> 2
            else -> 1
        }
    }

    /**
     * Calculates learning momentum priority
     */
    private fun calculateMomentumScore(context: NotificationContext): Int {
        return when {
            // User is active and making progress
            context.daysSinceLastActive == 0 && context.versesRead > 0 -> 2
            // User was active recently
            context.daysSinceLastActive <= 2 -> 2
            // User has been inactive
            context.daysSinceLastActive > 7 -> 1
            else -> 2
        }
    }

    /**
     * Calculates achievement-related priority
     */
    private fun calculateAchievementScore(context: NotificationContext): Int {
        return when (context.type) {
            NotificationType.ACHIEVEMENT,
            NotificationType.MILESTONE,
            NotificationType.CHAPTER_COMPLETE -> 3
            NotificationType.STREAK_ALERT,
            NotificationType.STREAK_RECOVERY -> 4
            NotificationType.WEAK_AREA_FOCUS -> 2
            NotificationType.RECOMMENDATION,
            NotificationType.QUIZ_READY -> 2
            NotificationType.DAILY_REMINDER -> 1
        }
    }

    /**
     * Calculates personalization relevance
     */
    private fun calculatePersonalizationScore(context: NotificationContext): Int {
        var score = 1

        // Score based on weak areas
        if (context.weakAreas.isNotEmpty()) {
            score += 1
        }

        // Score based on accuracy (lower accuracy = higher priority for help)
        if (context.accuracy < 60f) {
            score += 1
        }

        // Score based on user level (newer users get higher priority)
        if (context.userLevel < 5) {
            score += 1
        }

        return score.coerceIn(1, 4)
    }

    /**
     * Gets recommended action for notification based on priority
     */
    fun getRecommendedAction(priority: NotificationPriority, type: NotificationType): String {
        return when (priority) {
            NotificationPriority.CRITICAL -> when (type) {
                NotificationType.STREAK_ALERT -> "Show immediately, with vibration"
                NotificationType.ACHIEVEMENT -> "Show immediately with celebration"
                else -> "Show immediately"
            }
            NotificationPriority.HIGH -> "Show within 15 minutes"
            NotificationPriority.MEDIUM -> "Show within 1 hour"
            NotificationPriority.LOW -> "Show during next batch"
        }
    }

    /**
     * Determines if notification should be sent now based on priority and timing
     */
    suspend fun shouldSendNow(priority: NotificationPriority, type: NotificationType): Boolean {
        // Critical notifications are always sent
        if (priority == NotificationPriority.CRITICAL) return true

        // High priority: send during active hours
        if (priority == NotificationPriority.HIGH) {
            val predictor = SmartTimingPredictor(context, dailyActivityDao)
            val probability = predictor.calculateEngagementProbability()
            return probability > 0.6
        }

        // Medium/Low: batch send during optimal times
        return false
    }
}
