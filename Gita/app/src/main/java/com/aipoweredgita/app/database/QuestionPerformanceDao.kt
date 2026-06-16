package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuestionPerformanceDao {
    @Insert
    suspend fun insert(performance: QuestionPerformance)
    @Update
    suspend fun update(performance: QuestionPerformance)
    @Query("SELECT * FROM question_performance WHERE questionId = :questionId")
    suspend fun getPerformanceByQuestion(questionId: String): QuestionPerformance?
    @Query("SELECT * FROM question_performance WHERE topicCategory = :topic ORDER BY successRate ASC")
    fun getWeakTopics(topic: String): Flow<List<QuestionPerformance>>
    @Query("SELECT AVG(successRate) FROM question_performance")
    suspend fun getAverageSuccessRate(): Float?
    @Query("SELECT * FROM question_performance ORDER BY lastAttempted DESC LIMIT :limit")
    suspend fun getRecentlyAskedQuestions(limit: Int): List<QuestionPerformance>
}
