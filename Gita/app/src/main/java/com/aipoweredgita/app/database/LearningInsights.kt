package com.aipoweredgita.app.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_insights")
data class LearningInsights(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Insight type and content
    val insightType: String = "", // strength, weakness, pattern, prediction, milestone
    val title: String = "",
    val description: String = "",
    val detail: String = "",

    // Data basis
    val dataPoints: String = "", // comma-separated data points used
    val confidenceLevel: Float = 0f, // 0-100

    // Related entities
    val relatedYogaLevel: Int = -1, // -1 for general
    val relatedTopics: String = "", // comma-separated

    // Actionability
    val suggestedAction: String = "",
    val actionPriority: String = "", // high, medium, low
    val actionUrl: String = "", // deep link to relevant content

    // Metrics
    val impactScore: Float = 0f, // 0-100, importance of this insight
    val timelineValue: String = "", // short-term, long-term

    // Status and tracking
    val isViewed: Boolean = false,
    val isDismissed: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),
    val viewedAt: Long = 0,
    val expiresAt: Long = 0
)
