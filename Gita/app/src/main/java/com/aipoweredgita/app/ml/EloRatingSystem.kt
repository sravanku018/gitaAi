package com.aipoweredgita.app.ml

/**
 * ELO Rating System for Questions and Students
 * Originally from chess, adapted for educational assessment
 *
 * Tracks:
 * - Student skill rating
 * - Question difficulty rating
 * - Real-time adjustment based on outcomes
 */
data class ELOEntity(
    val id: String,
    var rating: Double = 1500.0,  // Starting rating
    var deviation: Double = 350.0, // Uncertainty (lower = more certain)
    val type: EntityType
)

enum class EntityType {
    STUDENT,
    QUESTION
}

data class ELOOutcome(
    val winnerId: String,
    val loserId: String,
    val isDraw: Boolean = false
)

data class ELOUpdate(
    val entityId: String,
    val oldRating: Double,
    val newRating: Double,
    val deviationChange: Double,
    val kFactor: Double
)

class EloRatingSystem(
    private val kFactorBase: Double = 32.0
) {

    /**
     * Calculate expected score between two entities
     * E_A = 1 / (1 + 10^((R_B - R_A) / 400))
     */
    fun expectedScore(
        ratingA: Double,
        ratingB: Double,
        deviationA: Double,
        deviationB: Double
    ): Double {
        val expected = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0))

        // Adjust expectation based on uncertainty
        val uncertainty = (deviationA + deviationB) / 700.0
        return expected * (1.0 - uncertainty) + 0.5 * uncertainty
    }

    /**
     * Update ratings after an outcome
     * R_A' = R_A + K * (S_A - E_A)
     */
    fun updateRatings(
        entityA: ELOEntity,
        entityB: ELOEntity,
        actualScoreA: Double,  // 1 = win, 0.5 = draw, 0 = loss
        actualScoreB: Double = 1.0 - actualScoreA
    ): Pair<ELOUpdate, ELOUpdate> {
        // Calculate K factor (adaptability)
        val kFactorA = calculateKFactor(entityA)
        val kFactorB = calculateKFactor(entityB)

        // Calculate expected scores
        val expectedA = expectedScore(entityA.rating, entityB.rating, entityA.deviation, entityB.deviation)
        val expectedB = 1.0 - expectedA

        // Update ratings
        val newRatingA = entityA.rating + kFactorA * (actualScoreA - expectedA)
        val newRatingB = entityB.rating + kFactorB * (actualScoreB - expectedB)

        // Update deviations (uncertainty decreases with more data)
        val newDeviationA = updateDeviation(entityA.deviation, entityA.rating != newRatingA)
        val newDeviationB = updateDeviation(entityB.deviation, entityB.rating != newRatingB)

        // Apply updates
        val updateA = ELOUpdate(
            entityId = entityA.id,
            oldRating = entityA.rating,
            newRating = newRatingA,
            deviationChange = newDeviationA - entityA.deviation,
            kFactor = kFactorA
        )

        entityA.rating = newRatingA
        entityA.deviation = newDeviationA

        val updateB = ELOUpdate(
            entityId = entityB.id,
            oldRating = entityB.rating,
            newRating = newRatingB,
            deviationChange = newDeviationB - entityB.deviation,
            kFactor = kFactorB
        )

        entityB.rating = newRatingB
        entityB.deviation = newDeviationB

        return Pair(updateA, updateB)
    }

    /**
     * Get recommended opponent (question) for a student
     * Aim for ~70% win rate (optimal challenge zone)
     */
    fun getRecommendedQuestionRating(
        studentRating: Double,
        allQuestionRatings: List<ELOEntity>
    ): ELOEntity? {
        val targetRating = studentRating - 100  // Target slightly easier

        return allQuestionRatings
            .filter { it.type == EntityType.QUESTION }
            .minByOrNull { abs(it.rating - targetRating) }
    }

    /**
     * Predict student performance on a question
     */
    fun predictOutcome(
        student: ELOEntity,
        question: ELOEntity
    ): Double {
        return expectedScore(student.rating, question.rating, student.deviation, question.deviation)
    }

    /**
     * Bulk update from quiz results
     */
    fun updateFromQuiz(
        student: ELOEntity,
        question: ELOEntity,
        isCorrect: Boolean
    ): Pair<ELOUpdate, ELOUpdate> {
        val actualScore = if (isCorrect) 1.0 else 0.0
        return updateRatings(student, question, actualScore)
    }

    /**
     * Get student skill level description
     */
    fun getSkillLevel(rating: Double): String {
        return when {
            rating >= 2400 -> "Expert"
            rating >= 2100 -> "Advanced"
            rating >= 1800 -> "Intermediate"
            rating >= 1500 -> "Novice"
            rating >= 1200 -> "Beginner"
            else -> "Novice"
        }
    }

    /**
     * Get question difficulty level
     */
    fun getDifficultyLevel(rating: Double): String {
        return when {
            rating >= 2100 -> "Very Hard"
            rating >= 1800 -> "Hard"
            rating >= 1500 -> "Medium"
            rating >= 1200 -> "Easy"
            else -> "Very Easy"
        }
    }

    private fun calculateKFactor(entity: ELOEntity): Double {
        // Lower K factor for higher-rated entities (more stable)
        return when {
            entity.deviation < 30 -> kFactorBase * 0.5  // Very certain
            entity.deviation < 100 -> kFactorBase * 0.75  // Moderately certain
            else -> kFactorBase  // Uncertain, adjust more
        }
    }

    private fun updateDeviation(currentDeviation: Double, ratingChanged: Boolean): Double {
        // Deviation decreases with experience (more data = less uncertainty)
        val decreaseRate = if (ratingChanged) 10.0 else 5.0
        return maxOf(50.0, currentDeviation - decreaseRate)
    }

    private fun abs(value: Double): Double = kotlin.math.abs(value)
    private fun maxOf(a: Double, b: Double): Double = if (a > b) a else b
}

/**
 * Glicko Rating System - Enhanced ELO with deviation tracking
 * Better for educational contexts
 */
class GlickoRatingSystem {

    data class GlickoEntity(
        val id: String,
        var rating: Double = 1500.0,  // Rating (centered at 1500)
        var deviation: Double = 350.0, // RD (Rating Deviation)
        val type: EntityType
    )

    fun calculateExpectedScore(
        rating1: Double,
        rating2: Double,
        rd1: Double,
        rd2: Double
    ): Double {
        val g = gFunction(rd2)
        val e = 1.0 / (1.0 + Math.pow(10.0, -g * (rating1 - rating2) / 1737.8))
        return e
    }

    private fun gFunction(rd: Double): Double {
        return 1.0 / Math.sqrt(1.0 + 3.0 * rd * rd / (Math.PI * Math.PI))
    }
}
