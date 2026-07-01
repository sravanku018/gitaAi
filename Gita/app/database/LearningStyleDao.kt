package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningStyleDao {
    @Query("SELECT * FROM learning_style WHERE id = 1")
    fun getLearningStyle(): Flow<LearningStyle>

    @Query("SELECT * FROM learning_style WHERE id = 1")
    suspend fun getLearningStyleOnce(): LearningStyle?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLearningStyle(style: LearningStyle)

    @Update
    suspend fun updateLearningStyle(style: LearningStyle)

    @Query("DELETE FROM learning_style")
    suspend fun deleteLearningStyle()

    @Query("UPDATE learning_style SET visualScore = :visual, auditoryScore = :auditory, readingScore = :reading, kinestheticScore = :kinesthetic, lastUpdated = :timestamp, confidence = :confidence WHERE id = 1")
    suspend fun updateScores(
        visual: Float,
        auditory: Float,
        reading: Float,
        kinesthetic: Float,
        timestamp: Long,
        confidence: Float
    )
}
