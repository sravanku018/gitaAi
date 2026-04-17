package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_question_bank")
data class QuizQuestionBank(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Question identification
    val questionHash: String = "", // unique hash for deduplication
    val questionType: String = "", // MCQ, essay, comparison, application, true_false
    val difficulty: Int = 5, // 1-10

    // Content
    val question: String = "",
    val chapter: Int = 0,
    val verse: Int = 0,
    val yogaLevel: Int = 0, // 0-4

    // Options for MCQ
    val optionA: String = "",
    val optionB: String = "",
    val optionC: String = "",
    val optionD: String = "",

    // Correct answer
    val correctAnswer: String = "", // A, B, C, D, or full text for essay
    val explanation: String = "",

    // Keywords and topics
    val keywords: String = "", // comma-separated
    val topics: String = "", // comma-separated topic tags

    // Generation metadata
    val generatedBy: String = "", // AI model name
    val generationMethod: String = "", // generated, manual, verified
    val modelVersion: String = "",

    // Quality metrics
    val qualityScore: Float = 0f, // 0-100
    val relevanceScore: Float = 0f, // 0-100
    val isVerified: Boolean = false,
    val isApproved: Boolean = true,

    // Usage tracking
    val usageCount: Int = 0,
    val usersAttempted: Int = 0,
    val averageSuccessRate: Float = 0f,
    val lastUsed: Long = 0,
    val lastAskedAt: Long = 0,  // Cooldown: prevents recent re-asks

    // Quality & feedback
    val userRating: Float = 0f,  // Average user rating (0-5)
    val ratingCount: Int = 0,    // How many users rated this question

    // Status
    val isActive: Boolean = true,
    val isArchived: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
