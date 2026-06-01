package com.aipoweredgita.app.repository

import androidx.room.withTransaction
import com.aipoweredgita.app.database.*

class QuizRepositoryImpl(
    private val database: GitaDatabase,
    private val quizAttemptDao: QuizAttemptDao,
    private val questionPerformanceDao: QuestionPerformanceDao,
    private val translationCacheDao: TranslationCacheDao,
    private val statsRepository: StatsRepository,
    private val yogaProgressionRepository: YogaProgressionRepository
) : QuizRepository {

    override suspend fun saveQuizAttemptWithStats(
        attempt: QuizAttempt,
        score: Int,
        totalQuestions: Int,
        segmentCorrectMap: Map<String, Int>
    ): Triple<Boolean, Int?, Int> {
        return database.withTransaction {
            val coinsEarned = statsRepository.trackQuizCompletion(score, totalQuestions, segmentCorrectMap)
            quizAttemptDao.insertAttempt(attempt.copy(coinsEarned = coinsEarned))
            val (didLevelUp, newLevel) = yogaProgressionRepository.updateFromQuiz(score, totalQuestions)
            Triple(didLevelUp, newLevel, coinsEarned)
        }
    }

    override suspend fun getCachedTranslation(originalText: String, languageCode: String): TranslationCache? {
        return translationCacheDao.getTranslation(originalText, languageCode)
    }

    override suspend fun saveTranslationToCache(translation: TranslationCache) {
        translationCacheDao.insertTranslation(translation)
    }

    override suspend fun updateQuestionPerformance(
        qId: String,
        theme: String,
        typeName: String,
        isCorrect: Boolean,
        difficultyLevel: Int
    ) {
        database.withTransaction {
            val existing = questionPerformanceDao.getPerformanceByQuestion(qId)
            val attempts = (existing?.timesAttempted ?: 0) + 1
            val corrects = (existing?.timesCorrect ?: 0) + if (isCorrect) 1 else 0
            val successRate = if (attempts > 0) (corrects.toFloat() * 100f / attempts) else 0f
            val perf = (existing ?: QuestionPerformance()).copy(
                questionId = qId,
                topicCategory = theme,
                questionType = typeName,
                timesAttempted = attempts,
                timesCorrect = corrects,
                successRate = successRate,
                perceivedDifficulty = difficultyLevel,
                lastAttempted = System.currentTimeMillis()
            )
            if (existing == null) {
                questionPerformanceDao.insert(perf)
            } else {
                questionPerformanceDao.update(perf)
            }
        }
    }

    override suspend fun getRecentlyAskedQuestions(limit: Int): List<QuestionPerformance> {
        return questionPerformanceDao.getRecentlyAskedQuestions(limit)
    }
}
