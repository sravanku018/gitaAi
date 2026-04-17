package com.aipoweredgita.app.data

/**
 * Represents different learning segments in the Bhagavad Gita
 * Each segment has a weightage that determines question selection frequency
 */
enum class LearningSegment(
    val displayName: String,
    val description: String,
    val baseWeightage: Float = 1.0f
) {
    KARMA_YOGA(
        displayName = "Karma Yoga",
        description = "Action without attachment - The path of righteous deeds",
        baseWeightage = 1.0f
    ),
    BHAKTI_YOGA(
        displayName = "Bhakti Yoga",
        description = "Devotion and love - The path of heart",
        baseWeightage = 1.0f
    ),
    JNANA_YOGA(
        displayName = "Jnana Yoga",
        description = "Knowledge and wisdom - The path of understanding",
        baseWeightage = 1.0f
    ),
    DHYANA_YOGA(
        displayName = "Dhyana Yoga",
        description = "Meditation - The path of inner peace",
        baseWeightage = 1.0f
    ),
    MOKSHA_YOGA(
        displayName = "Moksha Yoga",
        description = "Liberation - The path to freedom",
        baseWeightage = 1.0f
    ),
    WARRIOR_CODE(
        displayName = "Warrior Code",
        description = "Duty and righteousness in action",
        baseWeightage = 1.0f
    ),
    DIVINE_NATURE(
        displayName = "Divine Nature",
        description = "Understanding one's true spiritual nature",
        baseWeightage = 1.0f
    ),
    SURRENDER(
        displayName = "Surrender",
        description = "Devotional surrender to the Divine",
        baseWeightage = 1.0f
    )
}

/**
 * Data class to track segment performance and weightage
 */
data class SegmentProgress(
    val segment: LearningSegment,
    var questionsAttempted: Int = 0,
    var questionsCorrect: Int = 0,
    var currentWeightage: Float = 1.0f,
    val performanceHistory: MutableList<Float> = mutableListOf()
) {
    /**
     * Calculate accuracy for this segment
     */
    val accuracy: Float
        get() = if (questionsAttempted > 0) {
            (questionsCorrect.toFloat() / questionsAttempted.toFloat()) * 100f
        } else {
            0f
        }

    /**
     * Update performance and adjust weightage
     * Lower accuracy = higher weightage for more practice
     * Higher accuracy = lower weightage
     */
    fun updatePerformance(isCorrect: Boolean) {
        questionsAttempted++

        if (isCorrect) {
            questionsCorrect++
        }

        // Add to performance history (keep last 10 scores)
        performanceHistory.add(if (isCorrect) 1f else 0f)
        if (performanceHistory.size > 10) {
            performanceHistory.removeAt(0)
        }

        // Calculate recent trend
        val recentAvg = if (performanceHistory.isNotEmpty()) {
            performanceHistory.average().toFloat()
        } else {
            0f
        }

        // Adjust weightage based on performance
        // If accuracy < 60%, increase weightage
        // If accuracy > 85%, decrease weightage
        // Range: 0.5 to 2.0
        currentWeightage = when {
            accuracy < 60f -> (1.5f + (60f - accuracy) / 100f).coerceAtLeast(0.5f).coerceAtMost(2.0f)
            accuracy > 85f -> (1.0f - (accuracy - 85f) / 200f).coerceAtLeast(0.5f).coerceAtMost(2.0f)
            else -> 1.0f
        }

        // Ensure minimum weightage even if historically weak but improving
        if (recentAvg > 0.7f && currentWeightage < 1.0f) {
            currentWeightage = 1.0f
        }
    }
}

/**
 * Container for all segment progress
 */
data class SegmentWeightageSystem(
    val segments: Map<LearningSegment, SegmentProgress> = LearningSegment.values().associateWith {
        SegmentProgress(segment = it)
    },
    val lastUpdated: Long = System.currentTimeMillis()
) {
    /**
     * Get segments sorted by current weightage (highest priority first)
     */
    fun getWeightedSegments(): List<Pair<LearningSegment, Float>> {
        return segments.map { (segment, progress) ->
            segment to progress.currentWeightage
        }.sortedByDescending { it.second }
    }

    /**
     * Update a segment's performance
     */
    fun updateSegment(segment: LearningSegment, isCorrect: Boolean) {
        segments[segment]?.updatePerformance(isCorrect)
    }

    /**
     * Get segment by name
     */
    fun getSegment(segmentName: String): LearningSegment? {
        return LearningSegment.values().find { it.name == segmentName }
    }

    /**
     * Get total questions attempted across all segments
     */
    val totalQuestionsAttempted: Int
        get() = segments.values.sumOf { it.questionsAttempted }

    /**
     * Get overall accuracy
     */
    val overallAccuracy: Float
        get() {
            val total = totalQuestionsAttempted
            if (total == 0) return 0f
            val correct = segments.values.sumOf { it.questionsCorrect }
            return (correct.toFloat() / total.toFloat()) * 100f
        }
}
