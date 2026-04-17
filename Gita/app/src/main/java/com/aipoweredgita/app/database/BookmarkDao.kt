package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {
    @Query("SELECT * FROM bookmarks ORDER BY createdAt DESC")
    fun getAllBookmarks(): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE chapter = :chapter AND verse = :verse")
    suspend fun getBookmark(chapter: Int, verse: Int): Bookmark?

    @Query("SELECT * FROM bookmarks WHERE bookmarkType = :type")
    fun getBookmarksByType(type: BookmarkType): Flow<List<Bookmark>>

    @Query("SELECT * FROM bookmarks WHERE difficulty >= :minDifficulty ORDER BY difficulty DESC")
    fun getDifficultBookmarks(minDifficulty: Int): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark): Long

    @Update
    suspend fun updateBookmark(bookmark: Bookmark)

    @Delete
    suspend fun deleteBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("DELETE FROM bookmarks")
    suspend fun deleteAllBookmarks()

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getBookmarkCount(): Int

    @Query("UPDATE bookmarks SET lastReviewedAt = :timestamp, reviewCount = reviewCount + 1 WHERE id = :bookmarkId")
    suspend fun updateReviewTime(bookmarkId: Int, timestamp: Long)
}
