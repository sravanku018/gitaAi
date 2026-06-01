package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.database.CachedVerseDao
import com.aipoweredgita.app.database.ReadVerse
import com.aipoweredgita.app.database.ReadVerseDao
import kotlinx.coroutines.flow.Flow

class ReadingRepository(
    private val readVerseDao: ReadVerseDao,
    private val cachedVerseDao: CachedVerseDao
) {
    suspend fun insertReadVerse(verse: ReadVerse) = readVerseDao.insert(verse)

    suspend fun totalReadToday(date: String): Int = readVerseDao.totalReadToday(date)

    suspend fun getByDate(date: String): List<ReadVerse> = readVerseDao.getByDate(date)

    suspend fun distinctVersePairs(): Int = readVerseDao.distinctVersePairs()

    fun getKarmaYogaReadCountFlow(): Flow<Int> = readVerseDao.getKarmaYogaReadCountFlow()

    fun getBhaktiYogaReadCountFlow(): Flow<Int> = readVerseDao.getBhaktiYogaReadCountFlow()

    fun getJnanaYogaReadCountFlow(): Flow<Int> = readVerseDao.getJnanaYogaReadCountFlow()

    suspend fun getCachedVerse(chapter: Int, verse: Int): CachedVerse? =
        cachedVerseDao.getVerse(chapter, verse)

    suspend fun cacheVerse(verse: CachedVerse) = cachedVerseDao.insertVerse(verse)
}
