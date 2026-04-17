package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "random_verse_history")
data class RandomVerseHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapterNo: Int,
    val verseNo: Int,
    val timestamp: Long = System.currentTimeMillis()
)
