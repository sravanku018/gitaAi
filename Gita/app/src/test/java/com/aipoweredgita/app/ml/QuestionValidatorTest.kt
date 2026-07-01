package com.aipoweredgita.app.ml

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestionValidatorTest {

    @Test
    fun testExtractJsonFromResponse_rawJson() {
        val raw = """{"question": "test"}"""
        val extracted = QuestionValidator.extractJsonFromResponse(raw)
        assertEquals(raw, extracted)
    }

    @Test
    fun testExtractJsonFromResponse_markdownBlock() {
        val json = "{\n    \"question\": \"test\"\n}"
        val raw = "Here is your generated question:\n```json\n$json\n```\nHope you like it!"
        
        val extracted = QuestionValidator.extractJsonFromResponse(raw)
        assertEquals(json, extracted)
    }

    @Test
    fun testExtractJsonFromResponse_embeddedJson() {
        val json = """{"question": "test"}"""
        val raw = "Sure! Here it is: $json Let me know if you need more."
        
        val extracted = QuestionValidator.extractJsonFromResponse(raw)
        assertEquals(json, extracted)
    }

    @Test
    fun testValidateLLMOutput_validJson() {
        val json = """
            {
                "question": "What is Karma?",
                "options": ["Action", "Inaction", "Knowledge", "Devotion"],
                "correctOptionIndex": 0,
                "explanation": "Karma means action.",
                "theme": "Philosophy"
            }
        """.trimIndent()

        val result = QuestionValidator.validateLLMOutput(json, chapter = 3, verseNum = 1, difficulty = 5)
        
        assertTrue(result.isValid)
        assertEquals("What is Karma?", result.question?.question)
        assertEquals("Action", result.question?.correctAnswer)
    }

    @Test
    fun testValidateLLMOutput_invalidStructure() {
        // Missing options
        val json = """
            {
                "question": "What is Karma?",
                "options": [],
                "correctOptionIndex": 0
            }
        """.trimIndent()

        val result = QuestionValidator.validateLLMOutput(json, chapter = 3, verseNum = 1, difficulty = 5)
        
        assertFalse(result.isValid)
        assertEquals("Options list is empty", result.error)
    }
    
    @Test
    fun testValidateLLMOutput_duplicateSemanticCheck() {
        val json = """
            {
                "question": "What exactly is Karma?",
                "options": ["Action", "Inaction", "Knowledge", "Devotion"],
                "correctOptionIndex": 0
            }
        """.trimIndent()

        // Existing questions contain a semantically identical question (differing only by "exactly" and punctuation)
        // Wait, "what exactly is karma" -> "what exactly karma" (words > 3). "exactly", "karma", "what"
        // "what is karma" -> "karma", "what"
        // Let's use a closer paraphrase that normalizes to the same string.
        // normalize: lowercase, remove punctuation, filter > 3 chars, sorted, join
        // "What is Karma?" -> ["karma", "what"]
        // "Karma, what is it?" -> ["karma", "what"]
        
        val existing = listOf("Karma, what is it?")
        
        val newJson = """
            {
                "question": "What is Karma?",
                "options": ["Action", "Inaction", "Knowledge", "Devotion"],
                "correctOptionIndex": 0
            }
        """.trimIndent()

        val result = QuestionValidator.validateLLMOutput(newJson, chapter = 3, verseNum = 1, difficulty = 5, existingQuestions = existing)
        
        assertFalse("Should be flagged as a duplicate", result.isValid)
        assertEquals("Duplicate question (semantic match)", result.error)
    }
}
