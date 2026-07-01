package com.aipoweredgita.app

import com.aipoweredgita.app.ui.TeluguTransliterator
import com.aipoweredgita.app.database.VerseNote
import com.aipoweredgita.app.database.StudyPlan
import com.aipoweredgita.app.database.StudyPlanProgress
import com.aipoweredgita.app.database.StudyPlanTemplates
import org.junit.Assert.*
import org.junit.Test

class FeatureTests {

    // ── Telugu Transliteration ──────────────────────────────────────

    @Test
    fun `transliterate basic vowels`() {
        assertEquals("అ", TeluguTransliterator.transliterate("a"))
        assertEquals("ఆ", TeluguTransliterator.transliterate("aa"))
        assertEquals("ఇ", TeluguTransliterator.transliterate("i"))
        assertEquals("ఈ", TeluguTransliterator.transliterate("ee"))
    }

    @Test
    fun `transliterate consonants`() {
        assertEquals("క", TeluguTransliterator.transliterate("ka"))
        assertEquals("ర", TeluguTransliterator.transliterate("ra"))
        assertEquals("మ", TeluguTransliterator.transliterate("ma"))
    }

    @Test
    fun `transliterate empty string`() {
        assertEquals("", TeluguTransliterator.transliterate(""))
    }

    @Test
    fun `transliterate spaces preserved`() {
        assertEquals("అ ఇ", TeluguTransliterator.transliterate("a i"))
    }

    @Test
    fun `transliterate digits`() {
        assertEquals("౧౨౩", TeluguTransliterator.transliterate("123"))
    }

    // ── VerseNote Data Class ────────────────────────────────────────

    @Test
    fun `VerseNote default values`() {
        val note = VerseNote(chapterNo = 2, verseNo = 47, note = "Test note")
        assertEquals(0, note.id)
        assertEquals(2, note.chapterNo)
        assertEquals(47, note.verseNo)
        assertEquals("Test note", note.note)
        assertTrue(note.createdAt > 0)
        assertTrue(note.updatedAt > 0)
    }

    // ── StudyPlan Data Class ────────────────────────────────────────

    @Test
    fun `StudyPlan default values`() {
        val plan = StudyPlan(
            title = "Test Plan",
            description = "Test desc",
            durationDays = 14,
            planType = "karma_yoga",
            chapters = "2,3,4,18"
        )
        assertEquals(0, plan.id)
        assertEquals(1, plan.currentDay)
        assertTrue(plan.isActive)
        assertNull(plan.completedAt)
    }

    // ── StudyPlanTemplates ──────────────────────────────────────────

    @Test
    fun `karmaYoga14Day returns 14 items`() {
        val progress = StudyPlanTemplates.karmaYoga14Day()
        assertEquals(14, progress.size)
        assertEquals(1, progress.first().day)
        assertEquals(14, progress.last().day)
    }

    @Test
    fun `karmaYoga14Day uses correct chapters`() {
        val progress = StudyPlanTemplates.karmaYoga14Day()
        val chapters = progress.map { p -> p.chapterNo }
        assertTrue(chapters.contains(2))
        assertTrue(chapters.contains(3))
        assertTrue(chapters.contains(4))
        assertTrue(chapters.contains(18))
    }

    @Test
    fun `quizChallenge7Day returns 7 items`() {
        val progress = StudyPlanTemplates.quizChallenge7Day()
        assertEquals(7, progress.size)
    }

    @Test
    fun `fullGita18Day returns 18 items`() {
        val progress = StudyPlanTemplates.fullGita18Day()
        assertEquals(18, progress.size)
        assertEquals(1, progress.first().chapterNo)
        assertEquals(18, progress.last().chapterNo)
    }

    // ── StudyPlanProgress Data Class ────────────────────────────────

    @Test
    fun `StudyPlanProgress default values`() {
        val progress = StudyPlanProgress(
            planId = 1,
            day = 1,
            chapterNo = 2,
            verseStart = 1,
            verseEnd = 10
        )
        assertEquals(0, progress.id)
        assertFalse(progress.isCompleted)
        assertNull(progress.completedAt)
    }
}
