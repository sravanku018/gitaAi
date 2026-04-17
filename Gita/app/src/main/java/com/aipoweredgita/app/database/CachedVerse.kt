package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.aipoweredgita.app.data.GitaVerse

@Entity(
    tableName = "cached_verses",
    indices = [Index(value = ["chapterNo", "verseNo"], unique = true)]
)
data class CachedVerse(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chapterNo: Int,
    val verseNo: Int,
    val chapterName: String,
    val verse: String,
    val translation: String,
    val meaning: String,
    val explanation: String,
    val timestamp: Long = System.currentTimeMillis(),
    // New fields for separated verse tracking
    val wasSeparated: Boolean = false,
    val originalCombinedGroup: String = ""  // Stored as comma-separated list (e.g., "4,5,6")
) {
    fun toGitaVerse(): GitaVerse {
        return GitaVerse(
            chapterNo = chapterNo,
            verseNo = verseNo,
            chapterName = chapterName,
            verse = verse,
            translation = translation,
            purport = explanation.split("\n\n").filter { it.isNotBlank() },
            wasSeparated = wasSeparated,
            originalCombinedGroup = if (originalCombinedGroup.isNotBlank()) {
                originalCombinedGroup.split(",").mapNotNull { it.trim().toIntOrNull() }
            } else emptyList()
        )
    }

    companion object {
        fun fromGitaVerse(gitaVerse: GitaVerse): CachedVerse {
            return CachedVerse(
                chapterNo = gitaVerse.chapterNo,
                verseNo = gitaVerse.verseNo,
                chapterName = gitaVerse.chapterName,
                verse = gitaVerse.verse,
                translation = gitaVerse.translation,
                meaning = gitaVerse.translation,
                explanation = gitaVerse.purport.joinToString("\n\n"),
                wasSeparated = gitaVerse.wasSeparated,
                originalCombinedGroup = gitaVerse.originalCombinedGroup.joinToString(",")
            )
        }
    }
}
