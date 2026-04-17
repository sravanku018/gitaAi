package com.aipoweredgita.app.ml

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Item Response Theory (IRT) - Gold Standard for Adaptive Testing
 *
 * Models the probability of answering a question correctly based on:
 * - Student ability (theta)
 * - Item difficulty (b)
 * - Item discrimination (a)
 * - Guessing parameter (c)
 */
data class ItemParameters(
    val difficulty: Double,      // b parameter: -3 (easy) to +3 (hard)
    val discrimination: Double,  // a parameter: 0-2 (how well it discriminates)
    val guessing: Double = 0.2   // c parameter: 0-0.3 (chance of guessing)
)

data class StudentAbility(
    val studentId: String,
    var theta: Double = 0.0,     // Ability estimate: -4 to +4
    val standardError: Double = 1.0,
    val questionHistory: MutableList<QuestionResponse> = mutableListOf()
)

data class QuestionResponse(
    val questionId: String,
    val isCorrect: Boolean,
    val thetaBefore: Double,
    val thetaAfter: Double,
    val timestamp: Long
)

class ItemResponseTheoryEngine {

    companion object {
        private const val MAX_ITERATIONS = 10
        private const val CONVERGENCE_THRESHOLD = 0.001
    }

    /**
     * Calculate probability of correct response using 3-Parameter Logistic Model
     * P(θ) = c + (1 - c) / (1 + exp(-1.7 * a * (θ - b)))
     */
    fun probabilityCorrect(
        ability: Double,
        item: ItemParameters
    ): Double {
        val a = item.discrimination
        val b = item.difficulty
        val c = item.guessing

        val exponent = -1.7 * a * (ability - b)
        val probability = c + (1 - c) / (1 + exp(exponent))

        return probability.coerceIn(0.0, 1.0)
    }

    /**
     * Update student ability using Maximum Likelihood Estimation
     * Implements Newton-Raphson method for parameter estimation
     */
    fun updateAbility(
        student: StudentAbility,
        responses: List<Pair<ItemParameters, Boolean>>
    ): StudentAbility {
        var theta = student.theta
        var iterations = 0

        while (iterations < MAX_ITERATIONS) {
            val (firstDerivative, secondDerivative) = calculateDerivatives(theta, responses)

            if (abs(firstDerivative) < CONVERGENCE_THRESHOLD) break

            val deltaTheta = firstDerivative / secondDerivative
            theta -= deltaTheta

            // Constrain theta to reasonable range
            theta = theta.coerceIn(-4.0, 4.0)

            iterations++
        }

        // Calculate standard error (Fisher Information)
        val fisherInfo = calculateFisherInformation(theta, responses)
        val standardError = if (fisherInfo > 0) 1.0 / sqrt(fisherInfo) else 1.0

        return student.copy(
            theta = theta,
            standardError = standardError
        )
    }

    /**
     * Estimate item difficulty from student responses
     * When student gets 50% correct, that's the difficulty level
     */
    fun estimateItemDifficulty(
        studentAbility: Double,
        correctCount: Int,
        totalCount: Int
    ): Double {
        if (totalCount == 0) return 0.0

        val successRate = correctCount.toDouble() / totalCount

        // Use logistic function to estimate difficulty
        // If successRate = 0.5, difficulty = studentAbility
        return when {
            successRate <= 0.01 -> studentAbility - 2.5
            successRate >= 0.99 -> studentAbility + 2.5
            else -> {
                val p = successRate.coerceIn(0.01, 0.99)
                studentAbility + (ln((1 - p) / p) / 1.7)
            }
        }
    }

    /**
     * Get recommended question difficulty for a student
     * Aim for 70% success rate (optimal learning zone)
     */
    fun getRecommendedDifficulty(
        student: StudentAbility,
        targetSuccessRate: Double = 0.7
    ): Double {
        val targetP = targetSuccessRate.coerceIn(0.1, 0.9)

        // Solve for difficulty: P(θ) = 0.7
        // Assume discrimination = 1.0 (average), guessing = 0.2
        val a = 1.0
        val c = 0.2

        return student.theta + (ln((1 - c) / (targetP - c)) / (1.7 * a))
    }

    private fun calculateDerivatives(
        theta: Double,
        responses: List<Pair<ItemParameters, Boolean>>
    ): Pair<Double, Double> {
        var firstDerivative = 0.0
        var secondDerivative = 0.0

        for ((item, isCorrect) in responses) {
            val a = item.discrimination
            val c = item.guessing

            val p = probabilityCorrect(theta, item)
            val q = 1.0 - p

            // First derivative: d log L / dθ
            val dL = if (isCorrect) (a * q) else (-a * p)
            firstDerivative += dL

            // Second derivative: d² log L / dθ²
            val d2L = -a * a * p * q
            secondDerivative += d2L
        }

        return Pair(firstDerivative, secondDerivative)
    }

    private fun calculateFisherInformation(
        theta: Double,
        responses: List<Pair<ItemParameters, Boolean>>
    ): Double {
        var fisherInfo = 0.0

        for ((item, _) in responses) {
            val p = probabilityCorrect(theta, item)
            val q = 1.0 - p
            val a = item.discrimination

            fisherInfo += a * a * p * q
        }

        return fisherInfo
    }

    private fun abs(value: Double): Double = kotlin.math.abs(value)
}

/**
 * SimplifiedIRT for faster computation
 * Less accurate but much faster for real-time use
 */
class SimplifiedIRT {

    /**
     * Update ability using simple proportional control
     * Faster but less accurate than full IRT
     */
    fun updateAbilitySimple(
        currentAbility: Double,
        isCorrect: Boolean,
        expectedDifficulty: Double
    ): Double {
        val targetP = 0.7 // 70% success rate
        val actualP = if (isCorrect) 1.0 else 0.0
        val error = actualP - targetP

        // Learning rate decreases as error gets smaller
        val learningRate = 0.3

        // Adjust ability towards the difficulty where we'd expect 70% success
        val targetAbility = expectedDifficulty

        // Update ability with learning rate and error signal
        val deltaAbility = learningRate * error * 0.5
        val newAbility = currentAbility + (targetAbility - currentAbility) * 0.1 + deltaAbility

        return newAbility.coerceIn(-4.0, 4.0)
    }
}
