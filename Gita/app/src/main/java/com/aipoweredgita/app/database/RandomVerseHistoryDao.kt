package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RandomVerseHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShownVerse(history: RandomVerseHistory)

    @Query("SELECT COUNT(*) FROM random_verse_history")
    suspend fun getHistoryCount(): Int

    @Query("DELETE FROM random_verse_history")
    suspend fun clearHistory()
}
