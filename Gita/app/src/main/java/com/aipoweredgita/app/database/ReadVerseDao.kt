package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

    @Query("SELECT COUNT(DISTINCT chapterNo || '-' || verseNo) FROM read_verses WHERE chapterNo BETWEEN 1 AND 6")
    fun getKarmaYogaReadCountFlow(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT chapterNo || '-' || verseNo) FROM read_verses WHERE chapterNo BETWEEN 7 AND 12")
    fun getBhaktiYogaReadCountFlow(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT chapterNo || '-' || verseNo) FROM read_verses WHERE chapterNo BETWEEN 13 AND 18")
    fun getJnanaYogaReadCountFlow(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT chapterNo || '-' || verseNo) FROM read_verses WHERE chapterNo = 6")
    fun getDhyanaYogaReadCountFlow(): Flow<Int>

    @Query("SELECT COUNT(DISTINCT chapterNo || '-' || verseNo) FROM read_verses WHERE chapterNo = 9")
    fun getRajaYogaReadCountFlow(): Flow<Int>
}
