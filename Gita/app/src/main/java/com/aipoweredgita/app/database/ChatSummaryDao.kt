package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ChatSummaryDao {
    @Query("SELECT * FROM chat_summaries WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSummary(sessionId: String): ChatSummary?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSummary(summary: ChatSummary)

    @Query("DELETE FROM chat_summaries WHERE sessionId = :sessionId")
    suspend fun deleteSummary(sessionId: String)
}
