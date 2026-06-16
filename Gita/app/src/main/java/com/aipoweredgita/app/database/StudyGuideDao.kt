package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyGuideDao {
    @Insert
    suspend fun insert(guide: StudyGuide)

    @Query("SELECT * FROM study_guides WHERE chapterNo = :chapter ORDER BY createdAt DESC")
    fun getByChapter(chapter: Int): Flow<List<StudyGuide>>
}

