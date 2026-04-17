package com.aipoweredgita.app.services

import com.aipoweredgita.app.database.Bookmark
import com.aipoweredgita.app.database.BookmarkDao
import com.aipoweredgita.app.database.BookmarkType
import kotlinx.coroutines.flow.Flow

class BookmarkManager(private val bookmarkDao: BookmarkDao) {

    fun getAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.getAllBookmarks()
    }

    fun getBookmarksByType(type: BookmarkType): Flow<List<Bookmark>> {
        return bookmarkDao.getBookmarksByType(type)
    }

    fun getDifficultBookmarks(minDifficulty: Int = 7): Flow<List<Bookmark>> {
        return bookmarkDao.getDifficultBookmarks(minDifficulty)
    }

    suspend fun addBookmark(
        chapter: Int,
        verse: Int,
        verseText: String,
        type: BookmarkType,
        difficulty: Int = 5,
        notes: String? = null
    ): Long {
        val bookmark = Bookmark(
            chapter = chapter,
            verse = verse,
            verseText = verseText,
            bookmarkType = type,
            difficulty = difficulty,
            notes = notes
        )
        return bookmarkDao.insertBookmark(bookmark)
    }

    suspend fun updateBookmark(bookmark: Bookmark) {
        bookmarkDao.updateBookmark(bookmark)
    }

    suspend fun deleteBookmark(bookmark: Bookmark) {
        bookmarkDao.deleteBookmark(bookmark)
    }

    suspend fun deleteBookmarkById(id: Int) {
        bookmarkDao.deleteBookmarkById(id)
    }

    suspend fun clearAllBookmarks() {
        bookmarkDao.deleteAllBookmarks()
    }

    suspend fun updateReviewTime(bookmarkId: Int) {
        bookmarkDao.updateReviewTime(bookmarkId, System.currentTimeMillis())
    }

    suspend fun getBookmarkCount(): Int {
        return bookmarkDao.getBookmarkCount()
    }

    suspend fun getBookmark(chapter: Int, verse: Int): Bookmark? {
        return bookmarkDao.getBookmark(chapter, verse)
    }

    fun isBookmarked(chapter: Int, verse: Int): Flow<Boolean> {
        // Helper function to check if a verse is bookmarked
        return object : Flow<Boolean> {
            override suspend fun collect(collector: kotlinx.coroutines.flow.FlowCollector<Boolean>) {
                val bookmark = bookmarkDao.getBookmark(chapter, verse)
                collector.emit(bookmark != null)
            }
        }
    }
}
