package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DailyActivityDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(row: DailyActivity)

    @Query("UPDATE daily_activity SET normalSeconds = normalSeconds + :seconds WHERE date = :date")
    suspend fun addNormalSeconds(date: String, seconds: Long)

    @Query("UPDATE daily_activity SET quizSeconds = quizSeconds + :seconds WHERE date = :date")
    suspend fun addQuizSeconds(date: String, seconds: Long)

    @Query("UPDATE daily_activity SET voiceStudioTimeSeconds = voiceStudioTimeSeconds + :seconds WHERE date = :date")
    suspend fun addVoiceStudioSeconds(date: String, seconds: Long)

    @Query("UPDATE daily_activity SET versesRead = versesRead + :count WHERE date = :date")
    suspend fun addVerses(date: String, count: Int)

    @Query("SELECT * FROM daily_activity WHERE date = :date LIMIT 1")
    suspend fun getByDate(date: String): DailyActivity?
}

