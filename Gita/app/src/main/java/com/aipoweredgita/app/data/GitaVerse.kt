package com.aipoweredgita.app.data

import com.google.gson.*
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

// ============================================================================
// CUSTOM LIST ADAPTER - Handles both single object {} and array [] responses
// ============================================================================
class GitaVerseListAdapter : JsonDeserializer<List<GitaVerse>> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): List<GitaVerse> {
        return if (json.isJsonArray) {
            // API returned an array of verses
            json.asJsonArray.map { context.deserialize<GitaVerse>(it, GitaVerse::class.java) }
        } else {
            // API returned a single verse object - wrap in list
            listOf(context.deserialize<GitaVerse>(json, GitaVerse::class.java))
        }
    }
}

// ============================================================================
// SIMPLIFIED DATA CLASS - Minimal fields for common use cases
// ============================================================================
data class SimpleGitaVerse(
    val chapter: Int,
    val verse: Int,
    val slok: String
)

// ============================================================================
// LEGACY DESERIALIZERS - For backward compatibility
// ============================================================================

// Custom deserializer for verse field (can be String or Array)
class VerseDeserializer : JsonDeserializer<String> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): String {
        return when {
            json.isJsonArray -> json.asJsonArray.joinToString("\n\n")
            json.isJsonPrimitive -> json.asString
            else -> ""
        }
    }
}

// Custom deserializer for verse_no (can be Int or Array)
class VerseNoDeserializer : JsonDeserializer<Int> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Int {
        return when {
            json.isJsonArray -> json.asJsonArray.firstOrNull()?.asInt ?: 0
            json.isJsonPrimitive -> json.asInt
            else -> 0
        }
    }
}

data class GitaVerse(
    @SerializedName("chapter_no")
    val chapterNo: Int = 0,
    @SerializedName("verse_no")
    @JsonAdapter(VerseNoDeserializer::class)
    val verseNo: Int = 0,
    @SerializedName("chapter_name")
    val chapterName: String = "",
    val language: String = "",
    @JsonAdapter(VerseDeserializer::class)
    val verse: String = "",
    val transliteration: String = "",
    val synonyms: String = "",
    @SerializedName("audio_link")
    val audioLink: String = "",
    val translation: String = "",
    val purport: List<String> = emptyList(),
    // Indicates if this verse was originally part of a combined group
    val wasSeparated: Boolean = false,
    // Original combined verse numbers (e.g., [4, 5, 6] if this was part of verses 4-5-6)
    val originalCombinedGroup: List<Int> = emptyList()
) {
    // Helper properties for backward compatibility
    val verseNoSerial: Int = 0
    val meaning: String get() = translation
    val explanation: String get() = purport.joinToString("\n\n")
}
