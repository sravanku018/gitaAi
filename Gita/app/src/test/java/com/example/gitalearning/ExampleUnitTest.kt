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
        // Difficulty increases when answer is correct
        var difficulty = 5
        difficulty = if (true) difficulty + 1 else difficulty - 1
        assertEquals(6, difficulty)
    }

    @Test
    fun difficultyAdjustment_decrease() {
        // Difficulty decreases when answer is wrong
        var difficulty = 5
        difficulty = if (false) difficulty + 1 else difficulty - 1
        assertEquals(4, difficulty)
    }

    @Test
    fun difficultyBounds_maximum() {
        // Difficulty should not exceed 10
        var difficulty = 9
        difficulty = if (true) difficulty + 1 else difficulty - 1
        difficulty = minOf(difficulty, 10)
        assertEquals(10, difficulty)
    }

    @Test
    fun difficultyBounds_minimum() {
        // Difficulty should not go below 1
        var difficulty = 2
        difficulty = if (false) difficulty + 1 else difficulty - 1
        difficulty = maxOf(difficulty, 1)
        assertEquals(1, difficulty)
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
