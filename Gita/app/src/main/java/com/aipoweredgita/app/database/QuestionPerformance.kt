package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "question_performance")
data class QuestionPerformance(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Question identification
    val questionId: String = "",
    val topicCategory: String = "", // yoga level or specific topic
    val questionType: String = "", // MCQ, essay, comparison, etc

    // Performance metrics
    val timesAttempted: Int = 0,
    val timesCorrect: Int = 0,
    val successRate: Float = 0f, // 0-100

    // Difficulty assessment
    val perceivedDifficulty: Int = 5, // 1-10
    val averageTimeSpent: Long = 0, // seconds

    // User feedback
    val userFeedback: String = "", // good, hard, easy, confusing
    val commonMistakes: String = "", // comma-separated mistakes

    // Learning impact
    val learningValue: Float = 0f, // 0-100, how much this question helps learning
    val recommendationScore: Float = 0f, // likelihood of showing again

    val lastAttempted: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)
