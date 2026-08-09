package com.aipoweredgita.app.ui.components

import com.aipoweredgita.app.database.UserStats
import kotlin.math.floor
import kotlin.math.max

object YogaLevelManager {
    data class CompositeBreakdown(
        val accuracyNorm: Float,
        val versesNorm: Float,
        val appTimeNorm: Float,
        val voiceStudioTimeNorm: Float,
        val chaptersNorm: Float,
        val streakNorm: Float,
        val decayFactor: Float,
        val weightedRaw: Float,
        val composite: Float
    )

    data class YogaLevelInfo(
        val level: Int,
        val step: Int,
        val yogaName: String,
        val yogaDescription: String,
        val emoji: String
    )

    // Composite score based on verses and quizzes (0..1 scale)
    fun compositeScore(stats: UserStats?): Float {
        if (stats == null) return 0f
        return ((stats.versesRead / 200f).coerceIn(0f, 1f) * 0.5f +
                (stats.totalQuizzesTaken / 50f).coerceIn(0f, 1f) * 0.5f).coerceIn(0f, 1f)
    }

    fun compositeBreakdown(stats: UserStats?): CompositeBreakdown {
        if (stats == null) {
            return CompositeBreakdown(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f)
        }

        // Use more reliable stats fields
        val accuracyNorm = (stats.accuracyPercentage / 100f).coerceIn(0f, 1f)
        val versesNorm = (stats.versesRead / 200f).coerceIn(0f, 1f) // Use versesRead instead of distinctVersesRead
        val appTimeNorm = (stats.totalTimeSpentSeconds / (20f * 3600f)).coerceIn(0f, 1f)
        val quizTimeNorm = (stats.quizModeTimeSeconds / (10f * 3600f)).coerceIn(0f, 1f)
        val voiceStudioTimeNorm = (stats.voiceStudioTimeSeconds / (10f * 3600f)).coerceIn(0f, 1f)
        val quizzesTakenNorm = (stats.totalQuizzesTaken / 50f).coerceIn(0f, 1f)
        val streakNorm = (stats.currentStreak.coerceAtMost(30) / 30f).coerceIn(0f, 1f)

        val weighted = (
            accuracyNorm * 0.30f +
            versesNorm * 0.15f +
            appTimeNorm * 0.15f +
            quizTimeNorm * 0.10f +
            voiceStudioTimeNorm * 0.10f +
            quizzesTakenNorm * 0.10f +
            streakNorm * 0.10f
        ).coerceIn(0f, 1f)

        val decay = inactivityDecayFactor(stats)
        val composite = (weighted * decay).coerceIn(0f, 1f)

        return CompositeBreakdown(
            accuracyNorm, versesNorm, appTimeNorm, voiceStudioTimeNorm, quizzesTakenNorm, streakNorm, decay, weighted, composite
        )
    }

    private fun inactivityDecayFactor(stats: UserStats): Float {
        val now = System.currentTimeMillis()
        val elapsedDays = ((now - stats.lastActiveTimestamp).coerceAtLeast(0L) / (1000L * 60L * 60L * 24L)).toInt()
        // No decay for first 7 days; then 5% per day down to a floor of 60%
        return if (elapsedDays <= 7) 1f else max(0.60f, 1f - 0.05f * (elapsedDays - 7))
    }

    // Map user stats to 5 Yoga Levels based on overall progress
    fun levelFor(stats: UserStats?): Int {
        if (stats == null) return 1
        val totalVerses = stats.versesRead
        val totalQuizzes = stats.totalQuizzesTaken
        val totalChapters = stats.chaptersCompleted
        val totalScore = totalVerses + totalQuizzes * 5 + totalChapters * 10
        return when {
            totalScore < 50 -> 1
            totalScore < 150 -> 2
            totalScore < 300 -> 3
            totalScore < 500 -> 4
            else -> 5
        }
    }

    // Get step within current level based on progress
    fun stepFor(stats: UserStats?): Int {
        if (stats == null) return 1
        val level = levelFor(stats)
        val totalVerses = stats.versesRead
        val totalQuizzes = stats.totalQuizzesTaken
        val totalChapters = stats.chaptersCompleted
        val totalScore = totalVerses + totalQuizzes * 5 + totalChapters * 10
        val levelBase = when (level) {
            1 -> 0; 2 -> 50; 3 -> 150; 4 -> 300; else -> 500
        }
        val levelRange = when (level) {
            1 -> 50; 2 -> 100; 3 -> 150; 4 -> 200; else -> 200
        }
        val relative = (totalScore - levelBase).coerceAtLeast(0)
        val step = (relative.toFloat() / levelRange.toFloat() * 4f).toInt().coerceIn(0, 3)
        return ((level - 1) * 4 + step + 1).coerceAtMost(19)
    }

    // Get complete Yoga level info
    fun yogaLevelInfo(stats: UserStats?): YogaLevelInfo {
        val level = levelFor(stats)
        val step = stepFor(stats)
        val (name, desc, emoji) = getYogaInfo(level)
        return YogaLevelInfo(level, step, name, desc, emoji)
    }

    private fun getYogaInfo(level: Int): Triple<String, String, String> {
        return when (level) {
            1 -> Triple("Karma Yoga", "Foundational Action", "🌿")
            2 -> Triple("Bhakti Yoga", "Path of Devotion", "🔥")
            3 -> Triple("Jnana Yoga", "Path of Knowledge", "🧠")
            4 -> Triple("Dhyana Yoga", "Path of Meditation", "🌬️")
            5 -> Triple("Raja Yoga", "Ultimate Mastery", "🌺")
            else -> Triple("Yoga Path", "Spiritual Journey", "✨")
        }
    }

    /**
     * Integer-only coin multiplier ladder (matches server yoga_levels):
     * L1 Karma 1× · L2 Bhakti 2× · L3 Jnana 2× · L4 Dhyana 3× · L5 Raja 3×
     * (No 1.5 / 2.5 — whole coins only.)
     */
    fun getCoinMultiplier(stats: UserStats?): Float {
        val coins = stats?.krishnaCoins ?: 0
        return multiplierForCoins(coins)
    }

    fun multiplierForCoins(coins: Int): Float = when {
        coins >= 6000 -> 3.0f  // Dhyana + Raja
        coins >= 1000 -> 2.0f  // Bhakti + Jnana
        else -> 1.0f           // Karma
    }

    fun multiplierForYogaLevel(level: Int): Float = when (level.coerceIn(1, 5)) {
        1 -> 1.0f
        2, 3 -> 2.0f
        4, 5 -> 3.0f
        else -> 1.0f
    }

    // Progress within current level 0..1
    fun progressInLevel(stats: UserStats?): Float {
        if (stats == null) return 0f
        val totalVerses = stats.versesRead
        val totalQuizzes = stats.totalQuizzesTaken
        val totalChapters = stats.chaptersCompleted
        val totalScore = totalVerses + totalQuizzes * 5 + totalChapters * 10
        val level = levelFor(stats)
        val levelBase = when (level) {
            1 -> 0; 2 -> 50; 3 -> 150; 4 -> 300; else -> 500
        }
        val levelRange = when (level) {
            1 -> 50; 2 -> 100; 3 -> 150; 4 -> 200; else -> 200
        }
        return ((totalScore - levelBase).toFloat() / levelRange).coerceIn(0f, 1f)
    }

    fun thresholdForLevel(level: Int): Float {
        val clamped = level.coerceIn(1, 5)
        return when (clamped) {
            1 -> 0f
            2 -> 0.2f
            3 -> 0.4f
            4 -> 0.6f
            5 -> 0.8f
            else -> 1f
        }
    }

    fun nextLevelTarget(stats: UserStats?): Float {
        val current = levelFor(stats)
        val next = (current + 1).coerceAtMost(5)
        return thresholdForLevel(next)
    }

    // Stage mapping for compatibility: map to 4 stages
    // Stage 0: Level 1 (Karma Yoga)
    // Stage 1: Level 2 (Bhakti Yoga)
    // Stage 2: Level 3 (Jnana Yoga)
    // Stage 3: Level 4-5 (Dhyana & Moksha)
    fun stageFor(stats: UserStats?): Int {
        val level = levelFor(stats)
        return when (level) {
            1 -> 0
            2 -> 1
            3 -> 2
            else -> 3
        }
    }

    private fun hasCapstone(stats: UserStats?): Boolean {
        if (stats == null) return false
        val outOf = stats.bestScoreOutOf
        if (outOf < 10) return false
        val pct = if (outOf > 0) stats.bestScore.toFloat() / outOf.toFloat() else 0f
        return pct >= 0.80f
    }
}
