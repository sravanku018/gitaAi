package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.Bookmark
import com.aipoweredgita.app.database.BookmarkDao
import com.aipoweredgita.app.database.BookmarkType
import kotlinx.coroutines.flow.Flow

class BookmarkRepository(private val dao: BookmarkDao) {
    suspend fun insertBookmark(b: Bookmark): Long = dao.insertBookmark(b)
    suspend fun deleteBookmark(b: Bookmark) = dao.deleteBookmark(b)
    suspend fun getBookmark(chapter: Int, verse: Int): Bookmark? = dao.getBookmark(chapter, verse)
    fun getBookmarksByType(type: BookmarkType): Flow<List<Bookmark>> = dao.getBookmarksByType(type)
    fun getAllBookmarks(): Flow<List<Bookmark>> = dao.getAllBookmarks()
}
