package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "voice_chat_messages",
    indices = [Index(value = ["timestamp"], name = "idx_voice_chat_messages_timestamp")]
)
data class VoiceChatMessage(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
