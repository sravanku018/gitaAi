package com.aipoweredgita.app.services

import com.aipoweredgita.app.database.LearningStyle
import com.aipoweredgita.app.database.LearningStyleDao
import com.aipoweredgita.app.database.LearningStyleType
import kotlinx.coroutines.flow.Flow

class LearningStyleAnalyzer(private val dao: LearningStyleDao) {

    fun getLearningStyle(): Flow<LearningStyle> {
        return dao.getLearningStyle()
    }

    suspend fun getLearningStyleOnce(): LearningStyle? {
        return dao.getLearningStyleOnce()
    }

    suspend fun updateLearningStyle(
        visualScore: Float,
        auditoryScore: Float,
        readingScore: Float,
        kinestheticScore: Float
    ) {
        val confidence = calculateConfidence(visualScore, auditoryScore, readingScore, kinestheticScore)
        dao.updateScores(
            visual = visualScore,
            auditory = auditoryScore,
            reading = readingScore,
            kinesthetic = kinestheticScore,
            timestamp = System.currentTimeMillis(),
            confidence = confidence
        )
    }

    suspend fun analyzeFromQuizPerformance(
        mcqAccuracy: Float,
        essayQuality: Float,
        visualContentTime: Float,
        audioContentTime: Float,
        readingTime: Float,
        studioTime: Float
    ) {
        // Analyze learning style based on performance and time spent
        val visualScore = (visualContentTime * 0.4f + mcqAccuracy * 0.6f).coerceAtMost(100f)
        val auditoryScore = (audioContentTime * 0.5f + essayQuality * 0.5f).coerceAtMost(100f)
        val readingScore = (readingTime * 0.7f + essayQuality * 0.3f).coerceAtMost(100f)
        val kinestheticScore = (studioTime * 0.8f + mcqAccuracy * 0.2f).coerceAtMost(100f)

        updateLearningStyle(visualScore, auditoryScore, readingScore, kinestheticScore)
    }

    suspend fun updatePreferences(
        preferredSessionLength: Int,
        preferredStudyTime: String,
        questionTypePreference: String
    ) {
        val currentStyle = dao.getLearningStyleOnce() ?: return
        val updatedStyle = currentStyle.copy(
            preferredSessionLength = preferredSessionLength,
            preferredStudyTime = preferredStudyTime,
            questionTypePreference = questionTypePreference,
            lastUpdated = System.currentTimeMillis()
        )
        dao.updateLearningStyle(updatedStyle)
    }

    suspend fun resetToDefaults() {
        dao.deleteLearningStyle()
        val defaultStyle = LearningStyle()
        dao.insertLearningStyle(defaultStyle)
    }

    fun getDominantStyle(learningStyle: LearningStyle): LearningStyleType {
        return learningStyle.getDominantStyle()
    }

    fun getRecommendedContentType(dominantStyle: LearningStyleType): String {
        return when (dominantStyle) {
            LearningStyleType.VISUAL -> "diagrams, charts, and visual metaphors"
            LearningStyleType.AUDITORY -> "discussions and audio explanations"
            LearningStyleType.READING_WRITING -> "text-based content and note-taking"
            LearningStyleType.KINESTHETIC -> "practical exercises and real-world examples"
        }
    }

    fun getRecommendedQuestionType(dominantStyle: LearningStyleType): String {
        return when (dominantStyle) {
            LearningStyleType.VISUAL -> "MCQ with visual options"
            LearningStyleType.AUDITORY -> "Essay and discussion questions"
            LearningStyleType.READING_WRITING -> "Fill-in-the-blank and written responses"
            LearningStyleType.KINESTHETIC -> "Application and scenario-based questions"
        }
    }

    fun getOptimalSessionLength(learningStyle: LearningStyle): Int {
        return when (learningStyle.getDominantStyle()) {
            LearningStyleType.VISUAL -> learningStyle.preferredSessionLength.coerceAtMost(30)
            LearningStyleType.AUDITORY -> learningStyle.preferredSessionLength.coerceAtMost(25)
            LearningStyleType.READING_WRITING -> learningStyle.preferredSessionLength.coerceAtMost(45)
            LearningStyleType.KINESTHETIC -> learningStyle.preferredSessionLength.coerceAtMost(20)
        }
    }

    fun getBestStudyTime(learningStyle: LearningStyle): String {
        return learningStyle.preferredStudyTime
    }

    private fun calculateConfidence(
        visual: Float,
        auditory: Float,
        reading: Float,
        kinesthetic: Float
    ): Float {
        // Calculate how confident we are in this assessment
        val scores = listOf(visual, auditory, reading, kinesthetic)
        val maxScore = scores.maxOrNull() ?: 0f
        val secondMaxScore = scores.sortedDescending()[1]

        // Higher confidence when one style clearly dominates
        return when {
            maxScore - secondMaxScore > 30 -> 0.9f
            maxScore - secondMaxScore > 20 -> 0.7f
            maxScore - secondMaxScore > 10 -> 0.5f
            else -> 0.3f
        }
    }

    fun getStyleDescription(type: LearningStyleType): String {
        return when (type) {
            LearningStyleType.VISUAL -> "You learn best through images, diagrams, and visual representations"
            LearningStyleType.AUDITORY -> "You learn best through listening and discussion"
            LearningStyleType.READING_WRITING -> "You learn best through reading and writing"
            LearningStyleType.KINESTHETIC -> "You learn best through hands-on experience and practice"
        }
    }

    fun getImprovementTips(learningStyle: LearningStyle): List<String> {
        val dominantStyle = learningStyle.getDominantStyle()
        return when (dominantStyle) {
            LearningStyleType.VISUAL -> listOf(
                "Use mind maps for complex topics",
                "Highlight key verses with colors",
                "Create visual summaries"
            )
            LearningStyleType.AUDITORY -> listOf(
                "Read verses aloud",
                "Discuss meanings with others",
                "Listen to audio explanations"
            )
            LearningStyleType.READING_WRITING -> listOf(
                "Take detailed notes",
                "Rewrite key concepts",
                "Create written summaries"
            )
            LearningStyleType.KINESTHETIC -> listOf(
                "Apply concepts in daily life",
                "Use physical flashcards",
                "Practice through quizzes"
            )
        }
    }
}
