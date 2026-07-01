package com.aipoweredgita.app.data

import com.google.gson.annotations.SerializedName

/**
 * Raw API response model that preserves array structures
 * Used for offline caching to handle grouped verses properly
 */
data class GitaVerseRaw(
    @SerializedName("chapter_no")
    val chapterNo: Int = 0,
    @SerializedName("verse_no")
    val verseNo: Any? = null, // Can be Int or Array<Int>
    @SerializedName("chapter_name")
    val chapterName: String = "",
    val language: String = "",
    val verse: Any? = null, // Can be String or Array<String>
    val transliteration: String = "",
    val synonyms: String = "",
    @SerializedName("audio_link")
    val audioLink: String = "",
    val translation: String = "",
    val purport: Any? = null // Can be String or Array<String>
) {
    /**
     * Converts raw API response to list of individual verses
     * Handles both single verses and grouped verses
     * ALWAYS separates combined verses into individual entries
     */
    fun toIndividualVerses(): List<GitaVerse> {
        // Extract verse texts
        val verseTexts: List<String> = when (verse) {
            is List<*> -> (verse as List<*>).mapNotNull { it as? String }
            is String -> listOf(verse as String)
            else -> emptyList()
        }

        // Extract purport texts
        val purportTexts: List<String> = when (purport) {
            is List<*> -> (purport as List<*>).mapNotNull { it as? String }
            is String -> listOf(purport as String)
            else -> emptyList()
        }

        // Helper to parse various verse_no shapes (Int, Double, String ranges "4-6" or comma lists, or List<*>)
        fun parseVerseNumbers(raw: Any?, expectedCount: Int): List<Int> {
            fun parseOne(any: Any?): Int? = when (any) {
                is Int -> any
                is Double -> any.toInt()
                is Number -> any.toInt()
                is String -> any.trim().toIntOrNull()
                else -> null
            }
            if (raw is List<*>) {
                val list = raw.flatMap { element ->
                    when (element) {
                        is Int -> listOf(element)
                        is Double -> listOf(element.toInt())
                        is Number -> listOf(element.toInt())
                        is String -> {
                            val s = element.trim()
                            when {
                                // Range like "4-6" -> expand to [4, 5, 6]
                                Regex("^\\d+\\s*[-–—]\\s*\\d+$").matches(s) -> {
                                    val parts = s.split(Regex("[-–—]"), limit = 2)
                                    val start = parts[0].trim().toIntOrNull()
                                    val end = parts[1].trim().toIntOrNull()
                                    if (start != null && end != null && end >= start) (start..end).toList() else emptyList()
                                }
                                // Comma list like "4,5,6" -> [4, 5, 6]
                                s.contains(",") -> s.split(',').mapNotNull { it.trim().toIntOrNull() }
                                else -> listOfNotNull(s.toIntOrNull())
                            }
                        }
                        else -> emptyList()
                    }
                }
                if (list.isNotEmpty()) return list
            }
            when (raw) {
                is Int -> {
                    // If we have multiple verse texts but single verse number, expand as range
                    return if (expectedCount > 1) (raw..(raw + expectedCount - 1)).toList() else listOf(raw)
                }
                is Double -> {
                    val start = raw.toInt()
                    return if (expectedCount > 1) (start..(start + expectedCount - 1)).toList() else listOf(start)
                }
                is String -> {
                    val s = raw.trim()
                    // Range like "4-6"
                    if (Regex("^\\d+\\s*[-–—]\\s*\\d+$").matches(s)) {
                        val parts = s.split(Regex("[-–—]"), limit = 2)
                        val start = parts[0].trim().toIntOrNull()
                        val end = parts[1].trim().toIntOrNull()
                        if (start != null && end != null && end >= start) return (start..end).toList()
                    }
                    // Comma list like "4,5,6"
                    if (s.contains(",")) {
                        val list = s.split(',').mapNotNull { it.trim().toIntOrNull() }
                        if (list.isNotEmpty()) return list
                    }
                    // Single number
                    val single = s.toIntOrNull()
                    if (single != null) {
                        return if (expectedCount > 1) (single..(single + expectedCount - 1)).toList() else listOf(single)
                    }
                }
            }
            // Fallback: unknown, return empty and let repository layer adjust if needed
            return emptyList()
        }

        val expectedCount = if (verseTexts.isNotEmpty()) verseTexts.size else 1
        val verseNumbers: List<Int> = parseVerseNumbers(verseNo, expectedCount).let { nums ->
            when {
                nums.isEmpty() && verseTexts.size > 1 -> List(verseTexts.size) { 0 }
                nums.isEmpty() -> listOf(0)
                nums.size >= verseTexts.size && verseTexts.isNotEmpty() -> nums.take(verseTexts.size)
                nums.size < verseTexts.size && nums.isNotEmpty() -> {
                    // If we have only a start, extend contiguously
                    val start = nums.first()
                    (start..(start + verseTexts.size - 1)).toList()
                }
                else -> nums
            }
        }

        // Determine if these verses were originally combined
        val wasCombined = verseNumbers.size > 1

        // Create individual GitaVerse objects - one per verse text
        val count = if (verseTexts.isNotEmpty()) verseTexts.size else maxOf(verseNumbers.size, 1)
        val result = mutableListOf<GitaVerse>()
        for (i in 0 until count) {
            result.add(
                GitaVerse(
                    chapterNo = chapterNo,
                    verseNo = verseNumbers.getOrElse(i) { verseNumbers.lastOrNull() ?: 0 },
                    chapterName = chapterName,
                    language = language,
                    verse = verseTexts.getOrElse(i) { verseTexts.lastOrNull() ?: "" },
                    transliteration = transliteration,
                    synonyms = synonyms,
                    audioLink = audioLink,
                    translation = translation,
                    purport = listOf(purportTexts.getOrElse(i) { purportTexts.lastOrNull() ?: "" }),
                    wasSeparated = wasCombined,
                    originalCombinedGroup = if (wasCombined) verseNumbers else emptyList()
                )
            )
        }

        return result
    }
}
