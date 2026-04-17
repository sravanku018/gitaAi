package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "voice_chat_messages")
data class VoiceChatMessage(
    @PrimaryKey
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long
)
