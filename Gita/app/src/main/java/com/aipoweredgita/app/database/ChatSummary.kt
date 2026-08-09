package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_summaries")
data class ChatSummary(
    @PrimaryKey
    val sessionId: String,
    val summary: String,
    val lastUpdated: Long
)
