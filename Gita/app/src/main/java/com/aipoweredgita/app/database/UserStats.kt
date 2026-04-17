package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey
    val id: Int = 1, // Single row for user stats

    // User profile
    val userName: String = "",
    val dateOfBirth: String = "", // Format: "YYYY-MM-DD"

    // Quiz statistics
    val totalQuizzesTaken: Int = 0,
    val totalQuestionsAnswered: Int = 0,
    val totalCorrectAnswers: Int = 0,
    val bestScore: Int = 0,
    val bestScoreOutOf: Int = 0,

    // Reading statistics
    val versesRead: Int = 0,
    val distinctVersesRead: Int = 0,
    val chaptersCompleted: Int = 0,

    // Time tracking (in seconds)
    val totalTimeSpentSeconds: Long = 0,
    val normalModeTimeSeconds: Long = 0,
    val quizModeTimeSeconds: Long = 0,
    val voiceStudioTimeSeconds: Long = 0,
    val lastActiveTimestamp: Long = System.currentTimeMillis(),

    // Favorites
    val totalFavorites: Int = 0,

    // Streaks
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastActiveDate: String = "", // Format: "YYYY-MM-DD"

    // First use
    val firstOpenTimestamp: Long = System.currentTimeMillis()
) {
    // Computed properties
    val accuracyPercentage: Float
        get() = if (totalQuestionsAnswered > 0) {
            (totalCorrectAnswers.toFloat() / totalQuestionsAnswered) * 100
        } else 0f

    val averageScorePercentage: Float
        get() = if (bestScoreOutOf > 0) {
            (bestScore.toFloat() / bestScoreOutOf) * 100
        } else 0f

    val timeSpentFormatted: String
        get() = formatTime(totalTimeSpentSeconds)

    val normalModeTimeFormatted: String
        get() = formatTime(normalModeTimeSeconds)

    val quizModeTimeFormatted: String
        get() = formatTime(quizModeTimeSeconds)

    val voiceStudioTimeFormatted: String
        get() = formatTime(voiceStudioTimeSeconds)

    private fun formatTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m"
            else -> "${seconds}s"
        }
    }

    val daysActive: Int
        get() {
            val daysSinceFirst = (System.currentTimeMillis() - firstOpenTimestamp) / (1000 * 60 * 60 * 24)
            return daysSinceFirst.toInt() + 1
        }

    val age: Int
        get() {
            if (dateOfBirth.isEmpty()) return 0
            try {
                val parts = dateOfBirth.split("-")
                if (parts.size != 3) return 0

                val birthYear = parts[0].toInt()
                val birthMonth = parts[1].toInt()
                val birthDay = parts[2].toInt()

                val calendar = java.util.Calendar.getInstance()
                val currentYear = calendar.get(java.util.Calendar.YEAR)
                val currentMonth = calendar.get(java.util.Calendar.MONTH) + 1
                val currentDay = calendar.get(java.util.Calendar.DAY_OF_MONTH)

                var age = currentYear - birthYear
                if (currentMonth < birthMonth || (currentMonth == birthMonth && currentDay < birthDay)) {
                    age--
                }
                return age
            } catch (e: Exception) {
                return 0
            }
        }
}
