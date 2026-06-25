package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.QuizAttemptDao
import com.aipoweredgita.app.database.QuizSizeStats
import kotlinx.coroutines.flow.Flow

class QuizStatsRepository(private val quizAttemptDao: QuizAttemptDao) {

    fun getAllAttempts(): Flow<List<QuizAttempt>> = quizAttemptDao.getAllAttempts()

    fun getAttemptsByQuizSize(size: Int): Flow<List<QuizAttempt>> = quizAttemptDao.getAttemptsByQuizSize(size)

    fun getAttemptsByDate(date: String): Flow<List<QuizAttempt>> = quizAttemptDao.getAttemptsByDate(date)

    suspend fun getTotalAttemptsByQuizSize(size: Int): Int = quizAttemptDao.getTotalAttemptsByQuizSize(size)

    suspend fun getAverageAccuracyByQuizSize(size: Int): Float? = quizAttemptDao.getAverageAccuracyByQuizSize(size)

    suspend fun getAverageTimeByQuizSize(size: Int): Long? = quizAttemptDao.getAverageTimeByQuizSize(size)

    suspend fun getBestAttemptByQuizSize(size: Int): QuizAttempt? = quizAttemptDao.getBestAttemptByQuizSize(size)

    suspend fun getStatsByQuizSize(size: Int): QuizSizeStats? = quizAttemptDao.getStatsByQuizSize(size)

    suspend fun getAverageAccuracy(): Float? = quizAttemptDao.getAverageAccuracy()

    suspend fun getAverageTime(): Long? = quizAttemptDao.getAverageTime()

}
