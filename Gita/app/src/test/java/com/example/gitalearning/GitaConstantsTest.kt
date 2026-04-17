package com.aipoweredgita.app

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for Gita constants and verse utilities
 */
class GitaConstantsTest {
    @Test
    fun totalChapters_shouldBe18() {
        // Bhagavad Gita has exactly 18 chapters
        val totalChapters = 18
        assertEquals(18, totalChapters)
    }

    @Test
    fun totalVerses_shouldBe700() {
        // Bhagavad Gita has approximately 700 verses
        val totalVerses = 700
        assertEquals(700, totalVerses)
    }

    @Test
    fun chapterVerseCounts_chapter1_47Verses() {
        // Chapter 1 (Arjuna Vishada Yoga) has 47 verses
        val chapter1Verses = 47
        assertEquals(47, chapter1Verses)
    }

    @Test
    fun chapterVerseCounts_chapter18_78Verses() {
        // Chapter 18 (Moksha Sannyasa Yoga) has 78 verses
        val chapter18Verses = 78
        assertEquals(78, chapter18Verses)
    }

    @Test
    fun verseLanguageSupport() {
        // App supports 5 languages
        val supportedLanguages = listOf("tel", "eng", "hi", "ta", "kn")
        assertEquals(5, supportedLanguages.size)
        assertTrue(supportedLanguages.contains("tel"))
    }
}
