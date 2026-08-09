package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "yoga_progression")
data class YogaProgression(
    @PrimaryKey
    val id: Int = 1, // Single row for user progression
    val yogaLevel: Int = 0, // 0=Karma, 1=Bhakti, 2=Jnana, 3=Moksha
    val progressionPercentage: Float = 0f, // 0.0 to 100.0
    val lastActivityDate: String = "", // yyyy-MM-dd format
    val consecutiveDays: Int = 0,
    val quizAccuracy: Float = 0f, // Recent quiz performance (0-100)
    val readingConsistency: Float = 0f, // Reading consistency score (0-100)
    val totalQuizzesTaken: Int = 0,
    val totalVersesRead: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)
