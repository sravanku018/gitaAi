package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferences(
    @PrimaryKey
    val userId: Int = 1, // Single user for now

    // Question preferences
    val preferredQuestionTypes: String = "MCQ,Essay,Comparison", // comma-separated
    val preferredDifficultyMin: Int = 3,
    val preferredDifficultyMax: Int = 8,

    // Content preferences
    val preferredYogaLevel: Int = 0, // 0-4, or 0 for all
    val preferredChapters: String = "", // comma-separated chapters to focus on

    // Study mode preferences
    val preferredStudyMode: String = "quiz", // quiz, studio, reading, normal
    val questionsPerSession: Int = 10,
    val languagePreference: String = "English",

    // AI features preferences
    val enableAIRecommendations: Boolean = true,
    val enableDifficultyAdaptation: Boolean = true,
    val enablePersonalizedPaths: Boolean = true,
    val enableSpacedRepetition: Boolean = true,

    // Notification preferences
    val dailyReminderTime: String = "09:00", // HH:mm format
    val enableDailyReminders: Boolean = false,
    val reminderFrequency: String = "daily", // daily, weekly, none

    // Display preferences
    val darkModeEnabled: Boolean = false,
    val fontSize: Int = 16,
    val showHints: Boolean = true,

    // Content filtering
    val hideCompletedTopics: Boolean = false,
    val showOnlyWeakAreas: Boolean = false,

    val lastUpdated: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
