package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "favorite_verses",
    indices = [Index(value = ["chapterNo", "verseNo"], unique = true)] // Prevents duplicates
)
data class FavoriteVerse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterNo: Int,
    val verseNo: Int,
    val chapterName: String,
    val verse: String,
    val translation: String,
    val explanation: String,
    val savedAt: Long = System.currentTimeMillis()
)
