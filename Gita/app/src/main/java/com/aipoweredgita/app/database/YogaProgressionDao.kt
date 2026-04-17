package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface YogaProgressionDao {
    
    @Query("SELECT * FROM yoga_progression WHERE id = 1")
    fun getProgressionFlow(): Flow<YogaProgression?>
    
    @Query("SELECT * FROM yoga_progression WHERE id = 1")
    suspend fun getProgression(): YogaProgression?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgression(progression: YogaProgression)
    
    @Update
    suspend fun updateProgression(progression: YogaProgression)
    
    @Query("UPDATE yoga_progression SET progressionPercentage = :percentage, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updatePercentage(percentage: Float, timestamp: Long)
    
    @Query("UPDATE yoga_progression SET yogaLevel = :level, progressionPercentage = 0, lastUpdated = :timestamp WHERE id = 1")
    suspend fun levelUp(level: Int, timestamp: Long)
    
    @Query("UPDATE yoga_progression SET lastActivityDate = :date, consecutiveDays = :days, lastUpdated = :timestamp WHERE id = 1")
    suspend fun updateActivity(date: String, days: Int, timestamp: Long)
}
