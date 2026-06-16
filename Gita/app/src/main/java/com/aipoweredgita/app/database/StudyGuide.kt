package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_guides")
data class StudyGuide(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapterNo: Int = 1,
    val title: String = "",
    val summary: String = "",
    val keyPoints: String = "", // comma-separated
    val createdAt: Long = System.currentTimeMillis()
)

