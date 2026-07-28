package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizAttemptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: QuizAttempt): Long

    @Query("DELETE FROM quiz_attempts WHERE quizType = 'battle_quiz' AND score > totalQuestions")
    suspend fun deleteGlitchedBattleQuizzes()

    @Query("SELECT * FROM quiz_attempts WHERE quizType != 'battle_quiz' ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_attempts WHERE quizType != 'battle_quiz' ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentAttempts(limit: Int): Flow<List<QuizAttempt>>

    @Query("SELECT * FROM quiz_attempts WHERE date = :date AND quizType != 'battle_quiz' ORDER BY timestamp DESC")
    fun getAttemptsByDate(date: String): Flow<List<QuizAttempt>>

    @Query("SELECT AVG(score * 100.0 / totalQuestions) FROM quiz_attempts WHERE totalQuestions > 0 AND quizType != 'battle_quiz'")
    suspend fun getAverageAccuracy(): Float?

    @Query("SELECT AVG(timeSpentSeconds) FROM quiz_attempts WHERE quizType != 'battle_quiz'")
    suspend fun getAverageTime(): Long?

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE quizType != 'battle_quiz'")
    suspend fun getTotalAttempts(): Int

    @Query("""
        SELECT COUNT(*) FROM quiz_attempts
        WHERE score = :score
          AND totalQuestions = :totalQuestions
          AND quizType = :quizType
          AND ABS(timestamp - :timestamp) < :windowMs
    """)
    suspend fun countSimilarAttempts(
        score: Int,
        totalQuestions: Int,
        quizType: String,
        timestamp: Long,
        windowMs: Long = 5 * 60 * 1000L
    ): Int

    @Query("SELECT * FROM quiz_attempts WHERE totalQuestions > 0 AND quizType != 'battle_quiz' ORDER BY (score * 100.0 / totalQuestions) DESC LIMIT 1")
    suspend fun getBestAttempt(): QuizAttempt?

    @Query("DELETE FROM quiz_attempts")
    suspend fun deleteAll()

    @Query("SELECT * FROM quiz_attempts")
    suspend fun getAllAttemptsDirect(): List<QuizAttempt>

    @Delete
    suspend fun deleteAttempt(attempt: QuizAttempt)

    @Query("SELECT * FROM quiz_attempts WHERE quizType != 'battle_quiz' ORDER BY timestamp DESC LIMIT 10")
    suspend fun getLast10Attempts(): List<QuizAttempt>

    // Group by quiz size queries (excluding battle_quiz)
    @Query("SELECT * FROM quiz_attempts WHERE totalQuestions = :quizSize AND quizType != 'battle_quiz' ORDER BY timestamp DESC")
    fun getAttemptsByQuizSize(quizSize: Int): Flow<List<QuizAttempt>>

    @Query("SELECT AVG(score * 100.0 / totalQuestions) FROM quiz_attempts WHERE totalQuestions = :quizSize AND totalQuestions > 0 AND quizType != 'battle_quiz'")
    suspend fun getAverageAccuracyByQuizSize(quizSize: Int): Float?

    @Query("SELECT AVG(timeSpentSeconds) FROM quiz_attempts WHERE totalQuestions = :quizSize AND quizType != 'battle_quiz'")
    suspend fun getAverageTimeByQuizSize(quizSize: Int): Long?

    @Query("SELECT COUNT(*) FROM quiz_attempts WHERE totalQuestions = :quizSize AND quizType != 'battle_quiz'")
    suspend fun getTotalAttemptsByQuizSize(quizSize: Int): Int

    @Query("SELECT * FROM quiz_attempts WHERE totalQuestions = :quizSize AND totalQuestions > 0 AND quizType != 'battle_quiz' ORDER BY (score * 100.0 / totalQuestions) DESC LIMIT 1")
    suspend fun getBestAttemptByQuizSize(quizSize: Int): QuizAttempt?

    @Query("SELECT * FROM quiz_attempts WHERE quizType = :quizType ORDER BY timestamp DESC")
    fun getAttemptsByType(quizType: String): Flow<List<QuizAttempt>>

    @Query("""
        SELECT
            COUNT(*) as totalAttempts,
            AVG(score * 100.0 / totalQuestions) as averageAccuracy,
            AVG(timeSpentSeconds) as averageTime
        FROM quiz_attempts
        WHERE quizType = :quizType AND totalQuestions > 0
    """)
    suspend fun getStatsByType(quizType: String): QuizSizeStats?

    @Query("SELECT * FROM quiz_attempts WHERE quizType = :quizType AND totalQuestions > 0 ORDER BY (score * 100.0 / totalQuestions) DESC LIMIT 1")
    suspend fun getBestAttemptByType(quizType: String): QuizAttempt?

    @Query("""
        SELECT
            COUNT(*) as totalAttempts,
            AVG(score * 100.0 / totalQuestions) as averageAccuracy,
            AVG(timeSpentSeconds) as averageTime
        FROM quiz_attempts
        WHERE totalQuestions = :quizSize AND totalQuestions > 0 AND quizType != 'battle_quiz'
    """)
    suspend fun getStatsByQuizSize(quizSize: Int): QuizSizeStats?
}

data class QuizSizeStats(
    val totalAttempts: Int,
    val averageAccuracy: Float?,
    val averageTime: Long?
)
