package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizAttemptDao {

    @Insert
    suspend fun insertAttempt(attempt: QuizAttempt): Long

    @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAttempts(limit: Int): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_attempts WHERE date = :date ORDER BY timestamp DESC")
    fun getAttemptsByDate(date: String): Flow<List<QuizAttempt>>

    @Query("SELECT AVG(score * 100.0 / totalQuestions) FROM quiz_attempts")
    suspend fun getAverageAccuracy(): Float?

    @Query("SELECT AVG(timeSpentSeconds) FROM quiz_attempts")
    suspend fun getAverageTime(): Long?

    @Query("SELECT COUNT(*) FROM quiz_attempts")
    suspend fun getTotalAttempts(): Int

    @Query("SELECT * FROM quiz_attempts ORDER BY (score * 100.0 / totalQuestions) DESC LIMIT 1")
    suspend fun getBestAttempt(): QuizAttempt?

    @Query("DELETE FROM quiz_attempts")
    suspend fun deleteAll()

    @Query("SELECT * FROM quiz_attempts ORDER BY timestamp DESC LIMIT 10")
    suspend fun getLast10Attempts(): List<QuizAttempt>

    // Group by quiz size queries
    @Query("SELECT * FROM quiz_attempts WHERE totalQuestions = :quizSize ORDER BY timestamp DESC")
    fun getAttemptsByQuizSize(quizSize: Int): Flow<List<QuizAttempt>>

    @Query("SELECT AVG(score * 100.0 / totalQuestions) FROM quiz_attempts WHERE totalQuestions = :quizSize")
    suspend fun getAverageAccuracyByQuizSize(quizSize: Int): Float?

    @Query("SELECT AVG(timeSpentSeconds) FROM quiz_attempts WHERE totalQuestions = :quizSize")
    suspend fun getAverageTimeByQuizSize(quizSize: Int): Long?

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE totalQuestions = :quizSize")
    suspend fun getTotalAttemptsByQuizSize(quizSize: Int): Int

    @Query("SELECT * FROM quiz_attempts WHERE totalQuestions = :quizSize ORDER BY (score * 100.0 / totalQuestions) DESC LIMIT 1")
    suspend fun getBestAttemptByQuizSize(quizSize: Int): QuizAttempt?
}
