package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_activity")
data class DailyActivity(
    @PrimaryKey
    val date: String, // yyyy-MM-dd
    val normalSeconds: Long = 0,
    val quizSeconds: Long = 0,
    val voiceStudioTimeSeconds: Long = 0,
    val versesRead: Int = 0
)

