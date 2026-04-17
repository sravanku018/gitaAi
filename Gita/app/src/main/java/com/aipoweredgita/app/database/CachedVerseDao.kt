package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CachedVerseDao {

    @Query("SELECT * FROM cached_verses WHERE chapterNo = :chapter AND verseNo = :verse LIMIT 1")
    suspend fun getVerse(chapter: Int, verse: Int): CachedVerse?



    // Select random verse that hasn't been shown in random mode
    @Query("SELECT * FROM cached_verses c WHERE NOT EXISTS (SELECT 1 FROM random_verse_history r WHERE r.chapterNo = c.chapterNo AND r.verseNo = c.verseNo) ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomVerse(): CachedVerse?

    @Query("SELECT * FROM cached_verses WHERE chapterNo = :chapter AND verseNo = :verse LIMIT 1")
    fun getVerseSync(chapter: Int, verse: Int): CachedVerse?

    @Query("SELECT * FROM cached_verses WHERE chapterNo = :chapter ORDER BY verseNo ASC")
    fun getChapterVerses(chapter: Int): Flow<List<CachedVerse>>

    @Query("SELECT * FROM cached_verses ORDER BY chapterNo ASC, verseNo ASC")
    fun getAllVerses(): Flow<List<CachedVerse>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerse(verse: CachedVerse): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVerses(verses: List<CachedVerse>)

    @Query("SELECT COUNT(*) FROM cached_verses")
    suspend fun getCachedCount(): Int

    @Query("SELECT COUNT(*) FROM cached_verses")
    fun getCachedCountFlow(): Flow<Int>

    @Query("DELETE FROM cached_verses")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM cached_verses WHERE chapterNo = :chapter AND verseNo = :verse)")
    suspend fun isVerseCached(chapter: Int, verse: Int): Boolean

    @Query("SELECT chapterNo || ':' || verseNo FROM cached_verses")
    suspend fun getAllCachedVerseIds(): List<String>

    @Query("SELECT * FROM cached_verses WHERE verse LIKE '%' || :query || '%' OR meaning LIKE '%' || :query || '%' OR explanation LIKE '%' || :query || '%'")
    fun searchVerses(query: String): Flow<List<CachedVerse>>
}
