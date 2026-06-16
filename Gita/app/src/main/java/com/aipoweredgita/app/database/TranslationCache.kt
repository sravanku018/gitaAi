package com.aipoweredgita.app.database

import androidx.room.Entity

@Entity(
    tableName = "translation_cache",
    primaryKeys = ["originalText", "languageCode"]
)
data class TranslationCache(
    val originalText: String,
    val languageCode: String,
    val translatedText: String,
    val timestamp: Long = System.currentTimeMillis()
)
