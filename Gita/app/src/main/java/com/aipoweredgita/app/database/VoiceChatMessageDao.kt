package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VoiceChatMessageDao {
    @Query("SELECT * FROM voice_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<VoiceChatMessage>

    @Query("SELECT * FROM (SELECT * FROM voice_chat_messages ORDER BY timestamp DESC LIMIT :limit) ORDER BY timestamp ASC")
    suspend fun getRecentMessages(limit: Int): List<VoiceChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: VoiceChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<VoiceChatMessage>)

    @Query("DELETE FROM voice_chat_messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM voice_chat_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)
}
