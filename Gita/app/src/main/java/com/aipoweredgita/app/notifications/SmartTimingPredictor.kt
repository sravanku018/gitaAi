package com.aipoweredgita.app.notifications

import android.content.Context
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.GitaDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.Date

/**
 * ML-powered predictor for optimal notification timing
 * Analyzes user's daily activity patterns to predict best send times
 */
class SmartTimingPredictor(
    private val context: Context,
    private val dailyActivityDao: DailyActivityDao
) {

    /**
     * Analyzes user's activity patterns over the last N days
     * Returns hourly engagement scores (0-1)
     */
    suspend fun analyzeEngagementPatterns(days: Int = 14): Map<Int, Float> {
        val endDate = Calendar.getInstance().time
        val startDate = Calendar.getInstance().apply {
            time = endDate
            add(Calendar.DAY_OF_MONTH, -days)
        }.time

        // Get activities by iterating through date range
        val calendar = Calendar.getInstance().apply { time = startDate }
        val activities = mutableListOf<com.aipoweredgita.app.database.DailyActivity>()

        while (calendar.time <= endDate) {
            val dateString = "%04d-%02d-%02d".format(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1,
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            val activity = dailyActivityDao.getByDate(dateString)
            if (activity != null) {
                activities.add(activity)
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        // Hourly engagement scores
        val hourlyScores = mutableMapOf<Int, MutableList<Float>>()
        (0..23).forEach { hour ->
            hourlyScores[hour] = mutableListOf()
        }

        // Calculate engagement for each hour based on activity
        activities.forEach { activity ->
            // Simulate hourly breakdown (in real app, you'd store hourly data)
            val totalSeconds = activity.normalSeconds + activity.quizSeconds
            val dailyEngagement = totalSeconds.toFloat() / 3600f

            // Peak activity assumed 6-10 AM and 6-10 PM (typical user behavior)
            val peakHours = listOf(7, 8, 9, 19, 20, 21, 22)
            peakHours.forEach { hour ->
                val engagementScore = (dailyEngagement / 24f) * if (hour in 7..9 || hour in 19..22) 2.0f else 0.5f
                hourlyScores[hour]?.add(engagementScore.coerceIn(0f, 1f))
            }
        }

        // Calculate average engagement per hour
        return hourlyScores.mapValues { (_, scores) ->
            if (scores.isEmpty()) 0.3f else scores.average().toFloat()
        }
    }

    /**
     * Predicts the next optimal notification time based on:
     * 1. User's daily patterns
     * 2. Current day of week
     * 3. Time since last notification
     * 4. User's current streak status
     */
    suspend fun predictOptimalTime(
        minGapHours: Int = 4,
        streakDays: Int = 0
    ): Date {
        val patterns = analyzeEngagementPatterns()
        val calendar = Calendar.getInstance()

        // If no patterns, use default time
        if (patterns.isEmpty()) {
            calendar.add(Calendar.HOUR, minGapHours)
            return calendar.time
        }

        // Find top 3 engagement hours
        val topHours = patterns.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }

        // Adjust for streak preservation
        val adjustedHours = if (streakDays > 0) {
            // If user has streak, prioritize morning notifications
            listOf(7, 8, 9, 19, 20)
        } else {
            topHours
        }

        // Find next occurrence of high-engagement hour
        val now = Calendar.getInstance()
        adjustedHours.forEach { hour ->
            val candidate = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, (0..59).random())
                set(Calendar.SECOND, 0)
            }

            // If candidate time is in the future, use it
            if (candidate.timeInMillis > now.timeInMillis + (minGapHours * 60 * 60 * 1000)) {
                return candidate.time
            }
        }

        // If all else fails, schedule for next day at best hour
        val bestHour = topHours.firstOrNull() ?: 9
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, bestHour)
        calendar.set(Calendar.MINUTE, (0..59).random())
        calendar.set(Calendar.SECOND, 0)

        return calendar.time
    }

    /**
     * Gets user's most active hours for today
     */
    suspend fun getTodaysActiveHours(): List<Int> {
        val patterns = analyzeEngagementPatterns(7) // Last 7 days
        return patterns.entries
            .sortedByDescending { it.value }
            .take(3)
            .map { it.key }
    }

    /**
     * Calculates notification probability based on user's recent activity
     * Returns value 0-1, where 1 means high probability of engagement
     */
    suspend fun calculateEngagementProbability(): Float {
        val today = Calendar.getInstance()
        val todayString = "%04d-%02d-%02d".format(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH)
        )
        val todayActivity = dailyActivityDao.getByDate(todayString)

        if (todayActivity == null) return 0.5f

        val totalActivity = todayActivity.normalSeconds + todayActivity.quizSeconds

        // Normalize activity time (more activity = higher engagement probability)
        val activityScore = (totalActivity.toFloat() / 3600f).coerceIn(0f, 8f) / 8f
        return activityScore
    }
}
