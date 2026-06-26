package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.RandomVerseHistory
import com.aipoweredgita.app.database.RandomVerseHistoryDao

class RandomVerseRepository(private val dao: RandomVerseHistoryDao) {
    suspend fun insertShownVerse(h: RandomVerseHistory) = dao.insertShownVerse(h)
    suspend fun getHistoryCount(): Int = dao.getHistoryCount()
}
