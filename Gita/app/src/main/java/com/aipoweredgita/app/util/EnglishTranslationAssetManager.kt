package com.aipoweredgita.app.util

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.data.GitaVerse
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton manager to parse and serve English translations from gita_translations_english_only.csv asset.
 * Loaded after Telugu / primary translation as an offline translation fallback and secondary language layer.
 */
class EnglishTranslationAssetManager private constructor(context: Context) {

    private val translations = ConcurrentHashMap<Pair<Int, Int>, String>()

    init {
        loadTranslations(context)
    }

    private fun loadTranslations(context: Context) {
        try {
            val content = context.assets.open("gita_translations_english_only.csv")
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            val lines = content.split("\n")
            var count = 0
            for ((index, line) in lines.withIndex()) {
                if (index == 0 || line.isBlank()) continue
                val fields = parseCsvLine(line)
                if (fields.size >= 3) {
                    val chapter = fields[0].trim().toIntOrNull()
                    val verse = fields[1].trim().toIntOrNull()
                    var text = fields[2].trim()
                    if (text.startsWith("\"") && text.endsWith("\"")) {
                        text = text.substring(1, text.length - 1).trim()
                    }
                    if (chapter != null && verse != null && text.isNotBlank()) {
                        translations[Pair(chapter, verse)] = text
                        count++
                    }
                }
            }
            Log.d(TAG, "Successfully loaded $count English verse translations from gita_translations_english_only.csv")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading gita_translations_english_only.csv asset: ${e.message}", e)
        }
    }

    fun getTranslation(chapter: Int, verse: Int): String? {
        return translations[Pair(chapter, verse)]
    }

    fun enrichVerse(verse: GitaVerse): GitaVerse {
        val engText = getTranslation(verse.chapterNo, verse.verseNo) ?: return verse
        if (verse.translation.isBlank() || verse.translation == verse.verse) {
            return verse.copy(translation = engText)
        }
        return verse
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        fields.add(sb.toString())
        return fields
    }

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
