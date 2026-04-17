package com.aipoweredgita.app.services

import com.aipoweredgita.app.data.LearningSegment
import com.aipoweredgita.app.data.SegmentWeightageSystem
import com.aipoweredgita.app.ml.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.random.Random

/**
 * Unified Adaptive Difficulty Service
 *
 * Combines multiple ML algorithms:
 * 1. Item Response Theory (IRT) - Gold standard for ability estimation
 * 2. ELO Rating - Real-time skill tracking
 * 3. Multi-Armed Bandit - Optimal exploration
 * 4. Segment Weightage - Weakness-based focus
 *
 * Provides seamless difficulty adjustment for optimal learning
 */
data class AdaptiveDifficultyState(
    val studentAbility: Double = 0.0,  // IRT theta (-4 to +4)
    val studentElo: Double = 1500.0,   // ELO rating (0-3000)
    val currentStreak: Int = 0,
    val confidence: Double = 0.5,  // How confident we are in ability estimate
    val recommendedDifficulty: Int = 5,  // 1-10 scale for UI
    val optimalSuccessRate: Double = 0.7  // Target 70% success
)

data class DifficultyAdjustment(
    val newDifficulty: Int,
    val reason: String,
    val confidence: Double,
    val recommendedSegments: List<LearningSegment>,
    val algorithms: List<String>  // Which algorithms contributed
)

class AdaptiveDifficultyService {
    private val _state = MutableStateFlow(AdaptiveDifficultyState())
    val state: Flow<AdaptiveDifficultyState> = _state.asStateFlow()

    // ML Engines
    private val irtEngine = ItemResponseTheoryEngine()
    private val simplifiedIRT = SimplifiedIRT()
    private val eloSystem = EloRatingSystem()
    private val banditEngine = QuestionBanditEngine()
    private val contextualBandit = ContextualBandit()

    // Integration
    private var segmentSystem: SegmentWeightageSystem? = null

    /**
     * Update student performance and adjust difficulty
     */
    fun updatePerformance(
        isCorrect: Boolean,
        questionId: String,
        questionDifficulty: Double,
        segment: LearningSegment,
        responseTimeMs: Long
    ): DifficultyAdjustment {
        val currentState = _state.value

        // Update all algorithms
        val updatedState = updateAllAlgorithms(
            currentState,
            isCorrect,
            questionId,
            questionDifficulty,
            responseTimeMs
        )

        _state.value = updatedState

        // Generate adjustment recommendation
        return calculateDifficultyAdjustment(updatedState, segment, isCorrect)
    }

    /**
     * Get next recommended question difficulty
     */
    fun getNextDifficulty(
        preferredSegments: List<LearningSegment> = emptyList(),
        preferredTypes: List<String> = emptyList()
    ): Int {
        val currentState = _state.value

        // Use IRT to find optimal difficulty
        val studentAbility = ELOEntity(
            id = "student",
            rating = currentState.studentElo,
            type = EntityType.STUDENT
        )

        val irtItem = ItemParameters(
            difficulty = 0.0,
            discrimination = 1.0
        )

        val recommendedDifficulty = irtEngine.getRecommendedDifficulty(
            StudentAbility("student", currentState.studentAbility),
            targetSuccessRate = currentState.optimalSuccessRate
        )

        // Map IRT theta to 1-10 difficulty scale
        return mapThetaToDifficultyScale(recommendedDifficulty)
    }

    /**
     * Get recommended question for student
     */
    fun getRecommendedQuestion(
        allQuestions: List<QuestionMetadata>,
        preferredSegments: List<LearningSegment>
    ): QuestionMetadata? {
        val currentState = _state.value

        // Create bandit arms for available questions
        allQuestions.forEach { q ->
            val difficulty = mapDifficultyToTheta(q.difficulty)
            banditEngine.addQuestion(
                id = q.id,
                difficulty = difficulty,
                segment = q.segment.name,
                questionType = q.type
            )
        }

        // Select using Thompson Sampling
        val selectedArm = banditEngine.selectNextQuestion(
            preferredSegments = preferredSegments.map { it.name }
        ) ?: return null

        return allQuestions.find { it.id == selectedArm.id }
    }

    /**
     * Set segment system for integration
     */
    fun setSegmentSystem(segmentSystem: SegmentWeightageSystem) {
        this.segmentSystem = segmentSystem
    }

    /**
     * Analyze student learning pattern
     */
    fun analyzeLearningPattern(): LearningPatternAnalysis {
        val currentState = _state.value

        val analysis = LearningPatternAnalysis(
            isImproving = currentState.studentAbility > 0,
            confidenceLevel = currentState.confidence,
            recommendedFocus = when {
                currentState.studentElo < 1200 -> "Foundation Building"
                currentState.studentElo < 1500 -> "Skill Development"
                currentState.studentElo < 1800 -> "Challenge Mode"
                else -> "Advanced Mastery"
            },
            optimalSessionLength = calculateOptimalSessionLength(currentState),
            predictedImprovement = predictImprovementRate(currentState)
        )

        return analysis
    }

    private fun updateAllAlgorithms(
        state: AdaptiveDifficultyState,
        isCorrect: Boolean,
        questionId: String,
        questionDifficulty: Double,
        responseTimeMs: Long
    ): AdaptiveDifficultyState {
        var newAbility = state.studentAbility
        var newElo = state.studentElo
        var newStreak = if (isCorrect) state.currentStreak + 1 else 0

        // Update using simplified IRT (faster)
        newAbility = simplifiedIRT.updateAbilitySimple(
            currentAbility = state.studentAbility,
            isCorrect = isCorrect,
            expectedDifficulty = questionDifficulty
        )

        // Update ELO rating
        val studentEntity = ELOEntity("student", state.studentElo, type = EntityType.STUDENT)
        val questionEntity = ELOEntity(questionId, mapDifficultyToElo(questionDifficulty), type = EntityType.QUESTION)

        val (studentUpdate, _) = eloSystem.updateFromQuiz(studentEntity, questionEntity, isCorrect)
        newElo = studentUpdate.newRating

        // Update bandit
        banditEngine.update(questionId, isCorrect)

        // Calculate confidence (inverse of standard error)
        val confidence = calculateConfidence(state.studentAbility, newAbility, state.confidence)

        // Calculate optimal success rate based on performance
        val optimalSuccessRate = when {
            newStreak >= 5 && isCorrect -> 0.6  // Streak = make slightly harder
            newStreak >= 10 && isCorrect -> 0.5  // Long streak = much harder
            !isCorrect && newStreak == 0 -> 0.8  // Fail = make easier
            else -> 0.7  // Default 70%
        }

        return state.copy(
            studentAbility = newAbility,
            studentElo = newElo,
            currentStreak = newStreak,
            confidence = confidence,
            optimalSuccessRate = optimalSuccessRate
        )
    }

    private fun calculateDifficultyAdjustment(
        state: AdaptiveDifficultyState,
        segment: LearningSegment,
        wasCorrect: Boolean
    ): DifficultyAdjustment {
        val currentDifficulty = mapThetaToDifficultyScale(state.studentAbility)
        val recommendedDifficulty = getNextDifficulty()

        val adjustment = if (wasCorrect) {
            // Student is doing well, increase difficulty
            (currentDifficulty + 1).coerceAtMost(10)
        } else {
            // Student struggling, decrease difficulty
            (currentDifficulty - 1).coerceAtLeast(1)
        }

        val reason = if (wasCorrect) {
            "Correct answers indicate readiness for harder questions"
        } else {
            "Incorrect answers suggest reviewing fundamentals"
        }

        val recommendedSegments = segmentSystem?.let { system ->
            system.getWeightedSegments()
                .sortedByDescending { it.second }
                .take(3)
                .map { it.first }
        } ?: emptyList()

        val algorithms = mutableListOf("IRT", "ELO", "Bandit")

        return DifficultyAdjustment(
            newDifficulty = adjustment,
            reason = reason,
            confidence = state.confidence,
            recommendedSegments = recommendedSegments,
            algorithms = algorithms
        )
    }

    private fun mapThetaToDifficultyScale(theta: Double): Int {
        // Map IRT theta (-4 to +4) to 1-10 difficulty scale
        val normalized = (theta + 4.0) / 8.0  // 0 to 1
        return (normalized * 9 + 1).toInt().coerceIn(1, 10)
    }

    private fun mapDifficultyToTheta(difficulty: Double): Double {
        // Map 1-10 difficulty to IRT theta (-4 to +4)
        val normalized = (difficulty - 1.0) / 9.0  // 0 to 1
        return normalized * 8.0 - 4.0
    }

    private fun mapDifficultyToElo(difficulty: Double): Double {
        // Map 1-10 difficulty to ELO (1200-2400)
        return 1200 + (difficulty - 1) * 120.0
    }

    private fun calculateConfidence(oldAbility: Double, newAbility: Double, currentConfidence: Double): Double {
        val change = abs(newAbility - oldAbility)
        // Less change = higher confidence
        val confidenceDelta = (1.0 - change / 2.0) * 0.1
        return (currentConfidence + confidenceDelta).coerceIn(0.0, 1.0)
    }

    private fun calculateOptimalSessionLength(state: AdaptiveDifficultyState): Int {
        // Longer sessions for higher ability
        return when {
            state.studentElo > 2000 -> 45
            state.studentElo > 1700 -> 30
            state.studentElo > 1400 -> 20
            else -> 15
        }
    }

    private fun predictImprovementRate(state: AdaptiveDifficultyState): String {
        return when {
            state.confidence > 0.8 && state.currentStreak > 5 -> "Fast (High confidence & streak)"
            state.confidence > 0.6 -> "Moderate (Good confidence)"
            state.confidence > 0.4 -> "Slow (Building confidence)"
            else -> "Variable (Low confidence)"
        }
    }
}

data class QuestionMetadata(
    val id: String,
    val difficulty: Double,  // 1-10
    val segment: LearningSegment,
    val type: String  // MCQ, Essay, etc.
)

data class LearningPatternAnalysis(
    val isImproving: Boolean,
    val confidenceLevel: Double,
    val recommendedFocus: String,
    val optimalSessionLength: Int,
    val predictedImprovement: String
)

/**
 * Real-time difficulty adjustment based on performance
 */
class DynamicDifficultyAdjuster {

    private val difficultyHistory = mutableListOf<PerformancePoint>()

    data class PerformancePoint(
        val difficulty: Int,
        val success: Boolean,
        val timestamp: Long
    )

    /**
     * Adjust difficulty based on recent performance
     * Uses moving average for smooth transitions
     */
    fun adjustDifficulty(
        currentDifficulty: Int,
        lastNResults: List<Boolean>
    ): Int {
        if (lastNResults.isEmpty()) return currentDifficulty

        val successRate = lastNResults.count { it }.toDouble() / lastNResults.size

        // Adjust based on success rate
        return when {
            successRate >= 0.85 && lastNResults.size >= 3 -> (currentDifficulty + 1).coerceAtMost(10)
            successRate <= 0.50 && lastNResults.size >= 3 -> (currentDifficulty - 1).coerceAtLeast(1)
            else -> currentDifficulty
        }
    }

    /**
     * Get optimal difficulty for target success rate
     */
    fun getOptimalDifficulty(
        recentPerformance: List<PerformancePoint>,
        targetSuccessRate: Double = 0.7
    ): Int {
        if (recentPerformance.size < 5) return 5  // Default

        val avgDifficulty = recentPerformance.map { it.difficulty }.average()
        val successRate = recentPerformance.count { it.success }.toDouble() / recentPerformance.size

        // Adjust based on deviation from target
        val adjustment = (successRate - targetSuccessRate) * 3
        return (avgDifficulty + adjustment).toInt().coerceIn(1, 10)
    }
}
