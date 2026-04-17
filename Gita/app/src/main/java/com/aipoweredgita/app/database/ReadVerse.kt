package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "read_verses",
    indices = [
        Index(value = ["chapterNo", "verseNo", "date"], unique = true),
        Index(value = ["chapterNo", "verseNo"], unique = false)
    ]
)
data class ReadVerse(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapterNo: Int,
    val verseNo: Int,
    val date: String, // yyyy-MM-dd
    val timestamp: Long = System.currentTimeMillis()
)

