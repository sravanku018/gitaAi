package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.TranslationCache
import com.aipoweredgita.app.database.TranslationCacheDao

class TranslationCacheRepository(private val dao: TranslationCacheDao) {
    suspend fun getTranslation(originalText: String, languageCode: String): TranslationCache? = dao.getTranslation(originalText, languageCode)
    suspend fun insertTranslation(t: TranslationCache) = dao.insertTranslation(t)
    suspend fun clearAll() = dao.clearAll()
}
