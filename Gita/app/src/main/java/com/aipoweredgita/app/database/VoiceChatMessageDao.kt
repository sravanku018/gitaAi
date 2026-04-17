package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VoiceChatMessageDao {
    @Query("SELECT * FROM voice_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<VoiceChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: VoiceChatMessage)

    @Query("DELETE FROM voice_chat_messages")
    suspend fun deleteAllMessages()
}
