package com.aipoweredgita.app

import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit tests
 * These tests verify core business logic without needing Android context
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun quizScoreCalculation() {
        // Test score calculation: correct answers only
        val correctAnswers = 8
        val totalQuestions = 10
        val score = (correctAnswers * 100) / totalQuestions
        assertEquals(80, score)
    }

    @Test
    fun difficultyAdjustment_increase() {
        val engine = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine()
        val user = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine.UserState(skillLevel = 5)
        
        // Fast correct should increase difficulty by 2
        val newLevel = engine.updateDifficulty(user, isCorrect = true, responseTimeMs = 3000)
        assertEquals(7, newLevel)
        assertEquals(1, user.correctCount)
        assertEquals(1, user.totalAnswered)
        assertEquals(1, user.streak)
    }

    @Test
    fun difficultyAdjustment_decrease() {
        val engine = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine()
        val user = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine.UserState(skillLevel = 5)
        
        // Incorrect answer should decrease difficulty by 1
        val newLevel = engine.updateDifficulty(user, isCorrect = false, responseTimeMs = 4000)
        assertEquals(4, newLevel)
        assertEquals(0, user.correctCount)
        assertEquals(1, user.totalAnswered)
        assertEquals(-1, user.streak)
    }

    @Test
    fun difficultyBounds_maximum() {
        val engine = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine()
        val user = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine.UserState(skillLevel = 10)
        
        // Correct answer should clamp skill level to maximum of 10
        val newLevel = engine.updateDifficulty(user, isCorrect = true, responseTimeMs = 2000)
        assertEquals(10, newLevel)
    }

    @Test
    fun difficultyBounds_minimum() {
        val engine = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine()
        val user = com.aipoweredgita.app.ml.AdaptiveDifficultyEngine.UserState(skillLevel = 1)
        
        // Incorrect answer should clamp skill level to minimum of 1
        val newLevel = engine.updateDifficulty(user, isCorrect = false, responseTimeMs = 2000)
        assertEquals(1, newLevel)
    }

    @Test
    fun timeSpentCalculation() {
        // Calculate time spent (in seconds)
        val startTime = 1000L
        val endTime = 61000L
        val timeSpent = (endTime - startTime) / 1000
        assertEquals(60, timeSpent)
    }

    @Test
    fun percentageCalculation() {
        // Calculate success percentage
        val correct = 7
        val total = 10
        val percentage = (correct.toFloat() * 100 / total).toInt()
        assertEquals(70, percentage)
    }
}
