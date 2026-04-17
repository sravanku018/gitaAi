package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recommendation_data")
data class RecommendationData(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Recommendation details
    val recommendationType: String = "", // verse, topic, yogalevel, question
    val recommendationId: String = "", // chapter:verse, level, etc
    val recommendationTitle: String = "",

    // Priority and scoring
    val priority: Int = 5, // 1-10, higher = more important
    val confidenceScore: Float = 0f, // 0-100, ML model confidence
    val relevanceScore: Float = 0f, // 0-100, how relevant to user

    // Reasoning
    val reason: String = "", // why this was recommended
    val baseReason: String = "", // weak_area, review, progression, etc

    // Related metrics
    val userWeakness: Float = 0f, // 0-100, how weak is user in this area
    val expectedBenefit: Float = 0f, // 0-100, how much will this help
    val urgencyLevel: String = "", // high, medium, low

    // Status
    val status: String = "pending", // pending, viewed, completed, dismissed
    val isActive: Boolean = true,

    // Tracking
    val createdAt: Long = System.currentTimeMillis(),
    val viewedAt: Long = 0,
    val completedAt: Long = 0,
    val dismissedAt: Long = 0
)
