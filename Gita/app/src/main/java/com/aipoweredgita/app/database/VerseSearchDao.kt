package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Fts4(contentEntity = CachedVerse::class)
@Entity(tableName = "verse_fts")
data class VerseFts(
    @ColumnInfo(name = "verse")
    val verse: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "chapterNo")
    val chapterNo: Int,
    @ColumnInfo(name = "verseNo")
    val verseNo: Int
)

data class SearchResult(
    val chapterNo: Int,
    val verseNo: Int,
    val verse: String,
    val translation: String
)

@Dao
interface VerseSearchDao {
    @Query("""
        SELECT chapterNo, verseNo, verse, translation
        FROM verse_fts
        WHERE verse_fts MATCH :query
        LIMIT :limit
    """)
    suspend fun search(query: String, limit: Int = 20): List<SearchResult>

    @Query("""
        SELECT chapterNo, verseNo, verse, translation
        FROM verse_fts
        WHERE verse_fts MATCH :query
        LIMIT :limit
    """)
    fun searchFlow(query: String, limit: Int = 20): Flow<List<SearchResult>>
}
