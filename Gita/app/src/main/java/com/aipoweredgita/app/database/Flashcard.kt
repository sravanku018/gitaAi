package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val topic: String = "", // dharma, karma, etc
    val frontText: String = "",
    val backText: String = "",
    val chapterNo: Int = 0,
    val verseNo: Int = 0,
    val timesShown: Int = 0,
    val timesCorrect: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

