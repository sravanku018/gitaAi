package com.aipoweredgita.app.ui.components

import com.aipoweredgita.app.database.UserStats
import kotlin.math.floor
import kotlin.math.max

object LotusLevelManager {
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

    // Enhanced composite with additional signals and inactivity decay
    fun compositeScore(stats: UserStats?): Float {
        return compositeBreakdown(stats).composite
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

    // Map user stats to 5 Yoga Levels with 19 steps progression
    // Level 1 (Karma Yoga): Steps 1-3
    // Level 2 (Bhakti Yoga): Steps 4-7
    // Level 3 (Jnana Yoga): Steps 8-11
    // Level 4 (Dhyana Yoga): Steps 12-15
    // Level 5 (Moksha/Sanyasa): Steps 16-19
    fun levelFor(stats: UserStats?): Int {
        if (stats == null) return 1
        val composite = compositeScore(stats)
        // Map 0..1 composite to 1..5 levels
        return when {
            composite < 0.2f -> 1
            composite < 0.4f -> 2
            composite < 0.6f -> 3
            composite < 0.8f -> 4
            else -> 5
        }
    }

    // Get step within current level (1-3, 4-7, 8-11, 12-15, 16-19)
    fun stepFor(stats: UserStats?): Int {
        if (stats == null) return 1
        val composite = compositeScore(stats)
        return when {
            composite < 0.2f -> ((composite / 0.2f) * 3f).toInt() + 1
            composite < 0.4f -> ((((composite - 0.2f) / 0.2f) * 4f).toInt() + 4).coerceIn(4, 7)
            composite < 0.6f -> ((((composite - 0.4f) / 0.2f) * 4f).toInt() + 8).coerceIn(8, 11)
            composite < 0.8f -> ((((composite - 0.6f) / 0.2f) * 4f).toInt() + 12).coerceIn(12, 15)
            else -> ((((composite - 0.8f) / 0.2f) * 4f).toInt() + 16).coerceIn(16, 19)
        }
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
            1 -> Triple("Karma Yoga", "Action without Attachment", "🌿")
            2 -> Triple("Bhakti Yoga", "Devotion and Love for God", "🔥")
            3 -> Triple("Jnana Yoga", "Knowledge and Wisdom", "🧠")
            4 -> Triple("Dhyana Yoga", "Meditation and Mind Control", "🌬️")
            5 -> Triple("Moksha/Sanyasa", "Liberation and Freedom", "🌺")
            else -> Triple("Yoga Path", "Spiritual Journey", "✨")
        }
    }

    // Progress within current level 0..1
    fun progressInLevel(stats: UserStats?): Float {
        if (stats == null) return 0f
        val composite = compositeScore(stats)
        return when {
            composite < 0.2f -> composite / 0.2f
            composite < 0.4f -> (composite - 0.2f) / 0.2f
            composite < 0.6f -> (composite - 0.4f) / 0.2f
            composite < 0.8f -> (composite - 0.6f) / 0.2f
            else -> (composite - 0.8f) / 0.2f
        }.coerceIn(0f, 1f)
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
