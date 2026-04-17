package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningPatternDao {

    @Insert
    suspend fun insert(pattern: LearningPattern)

    @Update
    suspend fun update(pattern: LearningPattern)

    @Delete
    suspend fun delete(pattern: LearningPattern)

    @Query("SELECT * FROM learning_patterns WHERE id = :id")
    suspend fun getPatternById(id: Int): LearningPattern?

    @Query("SELECT * FROM learning_patterns ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getLatestPattern(): LearningPattern?

    @Query("SELECT * FROM learning_patterns")
    fun getAllPatterns(): Flow<List<LearningPattern>>

    @Query("DELETE FROM learning_patterns")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM learning_patterns")
    fun getPatternCount(): Flow<Int>

    @Query("SELECT preferredDifficulty FROM learning_patterns ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getPreferredDifficulty(): Int?

    @Query("UPDATE learning_patterns SET lastUpdated = :timestamp WHERE id = :id")
    suspend fun updateLastModified(id: Int, timestamp: Long)
}
