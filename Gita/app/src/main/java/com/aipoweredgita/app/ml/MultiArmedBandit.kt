package com.aipoweredgita.app.ml

import kotlin.math.sqrt
import kotlin.math.ln
import kotlin.math.cos
import kotlin.math.PI

/**
 * Multi-Armed Bandit Algorithm for Question Selection
 *
 * Balances:
 * - Exploitation: Choose questions we know the student can handle
 * - Exploration: Try new questions to learn more about student ability
 *
 * Implements Thompson Sampling - Bayesian approach with excellent performance
 */
data class QuestionArm(
    val id: String,
    var attempts: Int = 0,
    var successes: Int = 0,
    val difficulty: Double = 0.0,  // -3 (easy) to +3 (hard)
    val segment: String,  // Learning segment
    val questionType: String  // MCQ, Essay, etc.
) {
    /**
     * Calculate success rate (current estimate)
     */
    val successRate: Double
        get() = if (attempts > 0) successes.toDouble() / attempts else 0.5

    /**
     * Get Thompson Sample for this arm
     * Uses Beta distribution with Bayesian updating
     */
    fun getThompsonSample(): Double {
        // Bayesian approach: Beta(Beta prior + successes, Beta prior + failures)
        val alpha = 1.0 + successes  // Beta prior + successes
        val beta = 1.0 + attempts - successes  // Beta prior + failures

        // For Thompson Sampling, we sample from Beta(alpha, beta)
        // Simple approximation using normal distribution
        val mean = alpha / (alpha + beta)
        val variance = (alpha * beta) / ((alpha + beta) * (alpha + beta) * (alpha + beta + 1))
        val stdDev = sqrt(variance)

        // Box-Muller approximation for normal sampling
        val u1 = Math.random()
        val u2 = Math.random()
        val z0 = sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)

        // Sample from normal and map to [0, 1]
        return (mean + stdDev * z0).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate UCB (Upper Confidence Bound) score
     * Alternative to Thompson Sampling
     */
    fun getUCBScore(explorationConstant: Double = 2.0): Double {
        if (attempts == 0) return Double.MAX_VALUE

        val averageReward = successRate
        val confidence = explorationConstant * sqrt(ln(10.0) / attempts)

        return averageReward + confidence
    }

    /**
     * Update arm with outcome
     */
    fun update(isCorrect: Boolean) {
        attempts++
        if (isCorrect) successes++
    }
}

/**
 * Question Selection using Thompson Sampling
 * Adapts to student performance in real-time
 */
class QuestionBanditEngine(
    private val arms: MutableMap<String, QuestionArm> = mutableMapOf()
) {

    /**
     * Add a new question arm
     */
    fun addQuestion(
        id: String,
        difficulty: Double,
        segment: String,
        questionType: String
    ) {
        arms[id] = QuestionArm(
            id = id,
            difficulty = difficulty,
            segment = segment,
            questionType = questionType
        )
    }

    /**
     * Select next question using Thompson Sampling
     * Returns arm with highest sample value
     */
    fun selectNextQuestion(
        preferredSegments: List<String> = emptyList(),
        preferredTypes: List<String> = emptyList()
    ): QuestionArm? {
        if (arms.isEmpty()) return null

        // Filter by preferences if specified
        val candidates = arms.values.filter { arm ->
            (preferredSegments.isEmpty() || preferredSegments.contains(arm.segment)) &&
                    (preferredTypes.isEmpty() || preferredTypes.contains(arm.questionType))
        }

        return candidates.maxByOrNull { it.getThompsonSample() }
    }

    /**
     * Select question using UCB (Upper Confidence Bound)
     * Alternative to Thompson Sampling
     */
    fun selectNextQuestionUCB(
        preferredSegments: List<String> = emptyList(),
        preferredTypes: List<String> = emptyList()
    ): QuestionArm? {
        if (arms.isEmpty()) return null

        val candidates = arms.values.filter { arm ->
            (preferredSegments.isEmpty() || preferredSegments.contains(arm.segment)) &&
                    (preferredTypes.isEmpty() || preferredTypes.contains(arm.questionType))
        }

        return candidates.maxByOrNull { it.getUCBScore() }
    }

    /**
     * Update question with student response
     */
    fun update(questionId: String, isCorrect: Boolean) {
        arms[questionId]?.update(isCorrect)
    }

    /**
     * Get arms sorted by uncertainty (best for exploration)
     */
    fun getMostUncertainQuestions(count: Int = 5): List<QuestionArm> {
        return arms.values
            .sortedBy { it.attempts }
            .take(count)
    }

    /**
     * Get easy questions (success rate > 80%)
     */
    fun getEasyQuestions(count: Int = 5): List<QuestionArm> {
        return arms.values
            .filter { it.successRate > 0.8 && it.attempts >= 5 }
            .sortedByDescending { it.successRate }
            .take(count)
    }

    /**
     * Get challenging questions (success rate 40-60%)
     */
    fun getChallengingQuestions(count: Int = 5): List<QuestionArm> {
        return arms.values
            .filter { it.successRate in 0.4..0.6 && it.attempts >= 3 }
            .sortedBy { abs(it.successRate - 0.5) }
            .take(count)
    }

    /**
     * Get questions for specific segment
     */
    fun getQuestionsForSegment(segment: String): List<QuestionArm> {
        return arms.values.filter { it.segment == segment }
    }

    /**
     * Get question statistics
     */
    fun getStatistics(): QuestionStatistics {
        val totalAttempts = arms.values.sumOf { it.attempts }
        val totalSuccesses = arms.values.sumOf { it.successes }
        val averageSuccessRate = if (totalAttempts > 0) {
            totalSuccesses.toDouble() / totalAttempts
        } else 0.0

        return QuestionStatistics(
            totalQuestions = arms.size,
            totalAttempts = totalAttempts,
            totalSuccesses = totalSuccesses,
            averageSuccessRate = averageSuccessRate,
            mostAttempted = arms.values.maxByOrNull { it.attempts },
            leastAttempted = arms.values.minByOrNull { it.attempts }
        )
    }

    /**
     * Reset all statistics (for new student)
     */
    fun reset() {
        arms.values.forEach { arm ->
            arm.attempts = 0
            arm.successes = 0
        }
    }

    private fun abs(value: Double): Double = kotlin.math.abs(value)

    data class QuestionStatistics(
        val totalQuestions: Int,
        val totalAttempts: Int,
        val totalSuccesses: Int,
        val averageSuccessRate: Double,
        val mostAttempted: QuestionArm?,
        val leastAttempted: QuestionArm?
    )
}

/**
 * Contextual Bandit - Considers context (time, topic, difficulty) in selection
 */
class ContextualBandit {
    data class Context(
        val timeOfDay: Int,  // 0-23
        val dayOfWeek: Int,  // 0-6
        val previousPerformance: Double,  // 0-1
        val sessionLength: Int,  // minutes
        val studentMood: String?  // optional
    )

    data class ContextualArm(
        val id: String,
        val baseDifficulty: Double,
        val contextWeights: Map<String, Double> = emptyMap()  // How context affects this question
    )

    /**
     * Select question based on context
     * More sophisticated than simple bandit
     */
    fun selectWithContext(
        arms: List<ContextualArm>,
        context: Context
    ): ContextualArm {
        // Simple contextual selection (can be enhanced with ML)
        return arms.maxByOrNull { arm ->
            calculateContextualScore(arm, context)
        } ?: arms.first()
    }

    private fun calculateContextualScore(arm: ContextualArm, context: Context): Double {
        var score = arm.baseDifficulty

        // Adjust for time of day
        val hourWeight = arm.contextWeights["hour"] ?: 0.1
        score += when (context.timeOfDay) {
            in 6..10 -> hourWeight * 0.5  // Morning = better
            in 14..17 -> hourWeight * 0.2  // Afternoon = okay
            in 19..22 -> -hourWeight * 0.3  // Evening = harder
            else -> -hourWeight * 0.5  // Night = hardest
        }

        // Adjust for previous performance
        val perfWeight = arm.contextWeights["performance"] ?: 0.3
        score += (context.previousPerformance - 0.5) * perfWeight

        // Adjust for session length
        val lengthWeight = arm.contextWeights["sessionLength"] ?: 0.1
        score += when {
            context.sessionLength < 15 -> -lengthWeight  // Short session = easier
            context.sessionLength > 45 -> lengthWeight * 0.5  // Long session = harder
            else -> 0.0
        }

        return score
    }
}
