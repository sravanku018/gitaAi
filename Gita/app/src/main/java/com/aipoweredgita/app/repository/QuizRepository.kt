package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.TranslationCache
import com.aipoweredgita.app.database.QuestionPerformance

data class SaveQuizResult(
    val didLevelUp: Boolean,
    val newLevel: Int?,
    val coinsEarned: Int,
    val breakdown: String
)

interface QuizRepository {
    suspend fun saveQuizAttemptWithStats(
        attempt: QuizAttempt,
        score: Int,
        totalQuestions: Int,
        segmentCorrectMap: Map<String, Int> = emptyMap(),
        quizType: String = "general"
    ): SaveQuizResult

    suspend fun getCachedTranslation(originalText: String, languageCode: String): TranslationCache?

    suspend fun saveTranslationToCache(translation: TranslationCache)

    suspend fun updateQuestionPerformance(
        qId: String,
        theme: String,
        typeName: String,
        isCorrect: Boolean,
        difficultyLevel: Int
    )

    suspend fun getRecentlyAskedQuestions(limit: Int): List<QuestionPerformance>
}
