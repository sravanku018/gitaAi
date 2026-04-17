package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_style")
data class LearningStyle(
    @PrimaryKey
    val id: Int = 1, // Single record for user
    val visualScore: Float = 0f, // 0-100
    val auditoryScore: Float = 0f, // 0-100
    val readingScore: Float = 0f, // 0-100
    val kinestheticScore: Float = 0f, // 0-100
    val preferredSessionLength: Int = 15, // minutes
    val preferredStudyTime: String = "morning", // morning, afternoon, evening
    val questionTypePreference: String = "mixed", // mcq, essay, comparison, application, mixed
    val lastUpdated: Long = System.currentTimeMillis(),
    val confidence: Float = 0f // How confident we are in this assessment
) {
    fun getDominantStyle(): LearningStyleType {
        val scores = listOf(visualScore, auditoryScore, readingScore, kinestheticScore)
        val maxIndex = scores.indexOf(scores.max())
        return when (maxIndex) {
            0 -> LearningStyleType.VISUAL
            1 -> LearningStyleType.AUDITORY
            2 -> LearningStyleType.READING_WRITING
            else -> LearningStyleType.KINESTHETIC
        }
    }
}

enum class LearningStyleType {
    VISUAL,
    AUDITORY,
    READING_WRITING,
    KINESTHETIC
}
