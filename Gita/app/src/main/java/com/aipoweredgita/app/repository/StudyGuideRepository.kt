package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.StudyGuide
import com.aipoweredgita.app.database.StudyGuideDao
import kotlinx.coroutines.flow.Flow

class StudyGuideRepository(private val dao: StudyGuideDao) {
    suspend fun insert(guide: StudyGuide) = dao.insert(guide)
    fun getByChapter(chapter: Int): Flow<List<StudyGuide>> = dao.getByChapter(chapter)
}
