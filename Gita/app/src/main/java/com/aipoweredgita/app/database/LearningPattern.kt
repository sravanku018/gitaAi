package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_patterns")
data class LearningPattern(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Learning speed metrics
    val averageTimePerQuestion: Long = 0, // in seconds
    val fastestTimePerQuestion: Long = 0,
    val slowestTimePerQuestion: Long = 0,

    // Preferred difficulty level (1-10)
    val preferredDifficulty: Int = 5,

    // Best time to study (hour of day, 0-23)
    val bestStudyHour: Int = 14, // 2 PM default

    // Learning style (0: visual, 1: reading, 2: practical, 3: mixed)
    val learningStyle: Int = 3,

    // Focus areas (encoded as comma-separated yoga levels)
    val focusAreas: String = "1,2,3,4,5",

    // Peak performance metrics
    val peakPerformanceDay: String = "Wednesday", // Day with best score
    val peakPerformanceTime: Int = 14,

    // Streaks and consistency
    val studyConsistencyScore: Float = 0f, // 0-100
    val weeklyStudyDays: Int = 0,

    // Learning efficiency (questions correct / total)
    val learningEfficiency: Float = 0f,

    // Last updated timestamp
    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
