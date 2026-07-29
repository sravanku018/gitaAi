package com.aipoweredgita.app.util

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.data.GitaVerse
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.concurrent.ConcurrentHashMap

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

    private val entries = ConcurrentHashMap<Pair<Int, Int>, EnglishGitaEntry>()
    private val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)

    init {
        scope.launch {
            loadEntries(context)
        }
    }

    private fun loadEntries(context: Context) {
        try {
            val content = context.assets.open("gita_english_only.json")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val jsonArray = JSONArray(content)
            var count = 0
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
                    entries[Pair(chapter, verse)] = entry
                    count++
                }
            }
            Log.d(TAG, "Successfully loaded $count English Gita entries from gita_english_only.json")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading gita_english_only.json asset: ${e.message}", e)
        }
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
