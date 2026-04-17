package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReadVerseDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(read: ReadVerse): Long

    @Query("SELECT COUNT(*) FROM read_verses")
    suspend fun totalDistinctVerses(): Int

    @Query("SELECT COUNT(DISTINCT chapterNo || '-' || verseNo) FROM read_verses")
    suspend fun distinctVersePairs(): Int

    @Query("SELECT * FROM read_verses WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getByDate(date: String): List<ReadVerse>

    @Query("SELECT COUNT(*) FROM read_verses WHERE date = :date")
    suspend fun totalReadToday(date: String): Int

    @Query("SELECT COUNT(DISTINCT verseNo) FROM read_verses WHERE chapterNo = :chapter")
    suspend fun getReadVersesCountByChapter(chapter: Int): Int
}
