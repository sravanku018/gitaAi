package com.aipoweredgita.app.repository

import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.network.GitaApi

/**
 * Central repository for Gita API access.
 * ViewModels should use this instead of calling Retrofit directly.
 */
class GitaRepository {
    suspend fun getVerse(language: String, chapter: Int, verse: Int): GitaVerse =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            GitaApi.retrofitService.getVerse(language, chapter, verse)
        }

    suspend fun getVerseRaw(language: String, chapter: Int, verse: Int): com.aipoweredgita.app.data.GitaVerseRaw =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            GitaApi.retrofitService.getVerseRaw(language, chapter, verse)
        }
}
