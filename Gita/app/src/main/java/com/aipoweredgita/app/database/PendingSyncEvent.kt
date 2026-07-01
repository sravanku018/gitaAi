package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync_events")
data class PendingSyncEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String,
    val eventType: String, // "QUIZ", "CHAPTER", "SPEND", "CHECKIN", "SHARE", "STATS_SYNC"
    val payload: String, // JSON payload representing parameters
    val coinsToAdjust: Int, // Local balance adjustment (+10, +15, -10, etc.)
    val timestamp: Long = System.currentTimeMillis(),
    val idempotencyKey: String? = null // Unique key to prevent duplicate processing
)
