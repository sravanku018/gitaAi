package com.aipoweredgita.app.util

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.data.GitaVerse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray

data class EnglishGitaEntry(
    val chapterNo: Int,
    val verseNo: Int,
    val chapterNameEnglish: String = "",
    val translationEnglish: String = "",
    val explanationEnglish: String = "",
    val wordByWordMeaningEnglish: String = "",
    val audioLink: String = ""
)

/**
 * Singleton manager to parse and serve English Gita entries from gita_english_only.json asset.
 */
class EnglishTranslationAssetManager private constructor(context: Context) {

    @Volatile private var entries: Map<Pair<Int, Int>, EnglishGitaEntry> = emptyMap()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
    private val initDeferred = kotlinx.coroutines.CompletableDeferred<Unit>()
    private val _isLoadedState = MutableStateFlow(false)
    val isLoadedState: StateFlow<Boolean> = _isLoadedState.asStateFlow()

    init {
        scope.launch {
            try {
                val success = loadEntries(context)
                if (success) {
                    _isLoadedState.value = true
                    initDeferred.complete(Unit)
                } else {
                    initDeferred.completeExceptionally(IllegalStateException("gita_english_only.json asset is empty or invalid"))
                    _isLoadedState.value = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading gita_english_only.json asset: ${e.message}", e)
                initDeferred.completeExceptionally(e)
                _isLoadedState.value = false
            }
        }
    }

    suspend fun awaitLoaded() {
        initDeferred.await()
    }

    private fun loadEntries(context: Context): Boolean {
        val content = context.assets.open("gita_english_only.json")
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        val jsonArray = JSONArray(content)
        val tempMap = HashMap<Pair<Int, Int>, EnglishGitaEntry>()
        for (i in 0 until jsonArray.length()) {
            val obj = jsonArray.getJSONObject(i)
            val chapter = obj.optInt("chapter_no")
            val verse = obj.optInt("verse_no")
            val chapterName = obj.optString("chapter_name_english", "")
            val translation = obj.optString("translation_english", "")
            val explanation = obj.optString("explanation_english", "")
            val meaning = obj.optString("word_by_word_meaning_english", "")
            val audio = obj.optString("audio_link", "")

            if (chapter > 0 && verse > 0) {
                val entry = EnglishGitaEntry(
                    chapterNo = chapter,
                    verseNo = verse,
                    chapterNameEnglish = chapterName,
                    translationEnglish = translation,
                    explanationEnglish = explanation,
                    wordByWordMeaningEnglish = meaning,
                    audioLink = audio
                )
                tempMap[Pair(chapter, verse)] = entry
            }
        }
        if (tempMap.isNotEmpty()) {
            entries = tempMap
            Log.d(TAG, "Successfully loaded ${tempMap.size} English Gita entries from gita_english_only.json")
            return true
        }
        return false
    }

    fun getEntry(chapter: Int, verse: Int): EnglishGitaEntry? {
        return entries[Pair(chapter, verse)]
    }

    fun getTranslation(chapter: Int, verse: Int): String? {
        return entries[Pair(chapter, verse)]?.translationEnglish
    }

    fun enrichVerseWithEnglish(verse: GitaVerse): GitaVerse {
        val entry = getEntry(verse.chapterNo, verse.verseNo) ?: return verse
        return verse.copy(
            translation = entry.translationEnglish.ifBlank { verse.translation },
            synonyms = entry.wordByWordMeaningEnglish.ifBlank { verse.synonyms },
            purport = if (entry.explanationEnglish.isNotBlank()) listOf(entry.explanationEnglish) else verse.purport
        )
    }

    fun enrichVerse(verse: GitaVerse): GitaVerse = enrichVerseWithEnglish(verse)

    companion object {
        private const val TAG = "EnglishTranslationAsset"

        @Volatile
        private var instance: EnglishTranslationAssetManager? = null

        fun getInstance(context: Context): EnglishTranslationAssetManager {
            return instance ?: synchronized(this) {
                instance ?: EnglishTranslationAssetManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
