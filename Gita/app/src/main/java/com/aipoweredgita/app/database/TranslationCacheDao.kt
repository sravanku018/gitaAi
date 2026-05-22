package com.aipoweredgita.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TranslationCacheDao {
    @Query("SELECT * FROM translation_cache WHERE originalText = :originalText AND languageCode = :languageCode LIMIT 1")
    suspend fun getTranslation(originalText: String, languageCode: String): TranslationCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranslation(translation: TranslationCache)

    @Query("DELETE FROM translation_cache WHERE timestamp < :beforeTimestamp")
    suspend fun deleteOlderThan(beforeTimestamp: Long)

    @Query("DELETE FROM translation_cache")
    suspend fun clearAll()
}
