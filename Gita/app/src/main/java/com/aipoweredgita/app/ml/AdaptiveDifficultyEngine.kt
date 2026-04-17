package com.aipoweredgita.app.ml

/**
 * Adaptive difficulty engine for quizzes.
 * Adjusts question difficulty based on accuracy and response time.
 * Keeps user in the "optimal challenge zone" — not too easy, not too hard.
 */
class AdaptiveDifficultyEngine {
    
    // User state — persisted across sessions if needed
    data class UserState(
        var skillLevel: Int = 5,       // Dynamic 1-10 (starts at medium)
        var accuracy: Double = 0.0,    // Correct / Total
        var totalAnswered: Int = 0,
        var correctCount: Int = 0,
        var streak: Int = 0,           // Consecutive correct/wrong
        var averageResponseTime: Long = 0 // Milliseconds
    )

    /**
     * Update difficulty based on answer correctness and response time.
     * 
     * Advanced version: factors in speed.
     * Fast correct = +2 levels (user clearly knows this)
     * Slow correct = +1 level (user knows but hesitated)
     * Wrong = -1 level (user needs easier questions)
     */
    fun updateDifficulty(
        user: UserState,
        isCorrect: Boolean,
        responseTimeMs: Long = 0
    ): Int {
        user.totalAnswered++
        
        // Calculate response speed threshold (5 seconds = fast)
        val fast = responseTimeMs > 0 && responseTimeMs < 5000
        
        when {
            isCorrect && fast -> {
                user.skillLevel += 2  // Fast correct — push harder
                user.streak = if (user.streak > 0) user.streak + 1 else 1
            }
            isCorrect -> {
                user.skillLevel += 1  // Correct but slow — gentle push
                user.streak = if (user.streak > 0) user.streak + 1 else 1
            }
            else -> {
                user.skillLevel -= 1  // Wrong — ease up
                user.streak = if (user.streak < 0) user.streak - 1 else -1
                user.correctCount = user.correctCount.coerceAtLeast(0)
            }
        }
        
        if (isCorrect) user.correctCount++
        
        // Clamp between 1-10
        user.skillLevel = user.skillLevel.coerceIn(1, 10)
        
        // Update accuracy
        user.accuracy = if (user.totalAnswered > 0) {
            user.correctCount.toDouble() / user.totalAnswered
        } else 0.0
        
        // Track rolling average response time
        if (responseTimeMs > 0) {
            user.averageResponseTime = if (user.averageResponseTime == 0L) {
                responseTimeMs
            } else {
                (user.averageResponseTime * 0.7 + responseTimeMs * 0.3).toLong()
            }
        }
        
        return user.skillLevel
    }

    /**
     * Simple version — just correct/wrong, no timing.
     */
    fun updateDifficultySimple(user: UserState, isCorrect: Boolean): Int {
        return updateDifficulty(user, isCorrect, responseTimeMs = 0)
    }

    /**
     * Select next question from pool based on current skill level.
     * Filters to skillLevel ± 1 range, picks random.
     * Falls back to wider range if pool is empty.
     */
    fun <T> selectNextQuestion(
        questions: List<T>,
        user: UserState,
        difficultySelector: (T) -> Int
    ): T? {
        val targetDifficulty = user.skillLevel
        
        // Try exact range first (±1)
        var pool = questions.filter { q ->
            val d = difficultySelector(q)
            d in (targetDifficulty - 1)..(targetDifficulty + 1)
        }
        
        // Widen range if no questions available
        if (pool.isEmpty()) {
            pool = questions.filter { q ->
                val d = difficultySelector(q)
                d in (targetDifficulty - 2)..(targetDifficulty + 2)
            }
        }
        
        // Last resort: any question
        if (pool.isEmpty()) {
            pool = questions
        }
        
        return pool.randomOrNull()
    }

    /**
     * Generate an LLM prompt for a question at the given difficulty.
     * FIX ISSUE 3: Hard constraint prompt — enforces strict JSON output.
     */
    fun generateLLMPrompt(difficulty: Int, chapter: Int, verseNum: Int, translation: String): String {
        val difficultyLabel = when (difficulty) {
            1, 2 -> "Very Easy (basic recall)"
            3, 4 -> "Easy (simple understanding)"
            5, 6 -> "Medium (apply knowledge)"
            7, 8 -> "Hard (analyze and reason)"
            else -> "Very Hard (deep philosophical reasoning)"
        }
        
        return """
            You are generating a quiz question for the Bhagavad Gita.
            
            Verse: Chapter $chapter, Verse $verseNum
            Translation: $translation
            Difficulty: $difficulty/10 ($difficultyLabel)
            
            Rules:
            - Return STRICT JSON ONLY. No text before or after JSON.
            - The "answer" MUST be exactly one of the 4 options.
            - All 4 options must be similar in length and style.
            - No explanation outside JSON.
            
            Output format:
            {"question":"...","options":["A","B","C","D"],"correctOptionIndex":0,"explanation":"..."}
            
            Respond with ONLY the JSON object.
        """.trimIndent()
    }

    companion object {
        private const val PREF_SKILL_LEVEL = "quiz_skill_level"
        private const val PREF_ACCURACY = "quiz_accuracy"
        private const val PREF_TOTAL_ANSWERED = "quiz_total_answered"
        private const val PREF_CORRECT_COUNT = "quiz_correct_count"
        private const val PREF_STREAK = "quiz_streak"
        private const val PREF_AVG_TIME = "quiz_avg_time"

        fun saveState(state: UserState, prefs: android.content.SharedPreferences) {
            prefs.edit().apply {
                putInt(PREF_SKILL_LEVEL, state.skillLevel)
                putFloat(PREF_ACCURACY, state.accuracy.toFloat())
                putInt(PREF_TOTAL_ANSWERED, state.totalAnswered)
                putInt(PREF_CORRECT_COUNT, state.correctCount)
                putInt(PREF_STREAK, state.streak)
                putLong(PREF_AVG_TIME, state.averageResponseTime)
                apply()
            }
        }

        fun loadState(prefs: android.content.SharedPreferences): UserState {
            return UserState(
                skillLevel = prefs.getInt(PREF_SKILL_LEVEL, 5),
                accuracy = prefs.getFloat(PREF_ACCURACY, 0f).toDouble(),
                totalAnswered = prefs.getInt(PREF_TOTAL_ANSWERED, 0),
                correctCount = prefs.getInt(PREF_CORRECT_COUNT, 0),
                streak = prefs.getInt(PREF_STREAK, 0),
                averageResponseTime = prefs.getLong(PREF_AVG_TIME, 0L)
            )
        }
    }
}
