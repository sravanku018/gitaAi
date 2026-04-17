package com.aipoweredgita.app.data

/**
 * Data class for calendar heatmap visualization
 * Shows user activity over time (like GitHub contributions)
 */
data class HeatmapDay(
    val date: String, // YYYY-MM-DD format
    val activityCount: Int, // Number of activities (quizzes, verses read, etc.)
    val activityType: ActivityType,
    val intensity: Int // 0-4 (0=none, 1=low, 2=medium, 3=high, 4=very high)
)

enum class ActivityType {
    QUIZ_ATTEMPT,
    VERSE_READ,
    NOTE_TAKEN,
    REVIEW_SESSION,
    FAVORITE_ADDED
}

data class HeatmapData(
    val year: Int,
    val data: List<HeatmapDay>
) {
    /**
     * Get activity count for a specific date
     */
    fun getActivityForDate(date: String): Int {
        return data.find { it.date == date }?.activityCount ?: 0
    }

    /**
     * Get current streak of active days
     */
    fun getCurrentStreak(): Int {
        var streak = 0
        val sortedData = data.sortedByDescending { it.date }

        for (day in sortedData) {
            if (day.activityCount > 0) {
                streak++
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Get longest streak in this period
     */
    fun getLongestStreak(): Int {
        var longestStreak = 0
        var currentStreak = 0
        val sortedData = data.sortedBy { it.date }

        for (day in sortedData) {
            if (day.activityCount > 0) {
                currentStreak++
                longestStreak = maxOf(longestStreak, currentStreak)
            } else {
                currentStreak = 0
            }
        }

        return longestStreak
    }

    /**
     * Get total active days
     */
    val activeDays: Int
        get() = data.count { it.activityCount > 0 }

    /**
     * Get average activity per day
     */
    val averageActivity: Float
        get() = if (data.isNotEmpty()) {
            data.sumOf { it.activityCount }.toFloat() / data.size
        } else {
            0f
        }
}
