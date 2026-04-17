package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val chapter: Int,
    val verse: Int,
    val verseText: String,
    val bookmarkType: BookmarkType,
    val difficulty: Int = 5, // 1-10 scale
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val reviewCount: Int = 0
)

enum class BookmarkType {
    DIFFICULT_QUESTION,
    IMPORTANT_VERSE,
    PERSONAL_REFLECTION,
    REVISION_NEEDED,
    FAVORITE_TOPIC
}
