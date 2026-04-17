package com.aipoweredgita.app.services

import android.content.Context
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.StudyGuide
import com.aipoweredgita.app.database.Flashcard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SemanticContentService(private val context: Context) {
    suspend fun generateStudyGuideForChapter(chapter: Int) = withContext(Dispatchers.IO) {
        val db = GitaDatabase.getDatabase(context)
        // naive summary from cached verses; in production, use ML manager
        val title = "Chapter $chapter Study Guide"
        val summary = "Key teachings and themes synthesized from Chapter $chapter."
        val keyPoints = listOf("Duty (Dharma)", "Action (Karma)", "Devotion (Bhakti)", "Knowledge (Gyana)")
            .joinToString(", ")
        db.studyGuideDao().insert(
            StudyGuide(
                chapterNo = chapter,
                title = title,
                summary = summary,
                keyPoints = keyPoints
            )
        )
    }

    suspend fun seedFlashcardsForTopic(topic: String, chapter: Int, verse: Int) = withContext(Dispatchers.IO) {
        val db = GitaDatabase.getDatabase(context)
        val front = topic
        val back = "Meaning and application of $topic in life."
        db.flashcardDao().insert(
            Flashcard(
                topic = topic,
                frontText = front,
                backText = back,
                chapterNo = chapter,
                verseNo = verse
            )
        )
    }
}
