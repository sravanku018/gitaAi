package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Entity(tableName = "quiz_attempts")
data class QuizAttempt(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val score: Int,
    val totalQuestions: Int,
    val timeSpentSeconds: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val date: String = LocalDate.now().toString()
) {
    val accuracyPercentage: Float
        get() = if (totalQuestions > 0) {
            (score.toFloat() / totalQuestions) * 100
        } else 0f

    val timeSpentFormatted: String
        get() {
            val minutes = timeSpentSeconds / 60
            val seconds = timeSpentSeconds % 60
            return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
        }

    val dateFormatted: String
        get() {
            // Use current time if timestamp is invalid
            val validTimestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()

            return try {
                val formatter = DateTimeFormatter.ofPattern("MM/dd/yy HH:mm")
                val zoned = Instant.ofEpochMilli(validTimestamp).atZone(ZoneId.systemDefault()).toLocalDateTime()
                formatter.format(zoned)
            } catch (e: Exception) {
                date
            }
        }

    val performanceLevel: String
        get() = when {
            accuracyPercentage >= 90 -> "Excellent"
            accuracyPercentage >= 75 -> "Good"
            accuracyPercentage >= 60 -> "Average"
            else -> "Needs Improvement"
        }

    val performanceEmoji: String
        get() = when {
            accuracyPercentage >= 90 -> "🌟"
            accuracyPercentage >= 75 -> "😊"
            accuracyPercentage >= 60 -> "🙂"
            else -> "📚"
        }
}
