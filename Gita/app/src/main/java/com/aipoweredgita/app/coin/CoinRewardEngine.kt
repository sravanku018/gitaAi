package com.aipoweredgita.app.coin

/**
 * Central coin reward algorithm for quiz completions.
 * Pure calculation — no side effects, no dependencies.
 *
 * All inputs are clamped to valid ranges internally so callers
 * can pass raw values without pre-validation.
 */
object CoinRewardEngine {

    data class Input(
        val score: Int,
        val totalQuestions: Int,
        val segmentCorrectMap: Map<String, Int> = emptyMap(),
        val currentStreakDays: Int = 0,
        val dailyCheckinDay: Int = 0,     // 1..7 from DailyRewardsTracker
        val yogaLevel: Int = 1,
        val yogaMultiplier: Float = 1f,
    ) {
        /** Validates and normalises all values, returning a clean copy. */
        fun sanitised(): Input {
            val clampedStreak = currentStreakDays.coerceAtLeast(0)
            val clampedCheckin = dailyCheckinDay.coerceIn(0, 7)
            val clampedMultiplier = yogaMultiplier.coerceAtLeast(0f)
            val safeScore = score.coerceAtLeast(0)
            val safeTotal = totalQuestions.coerceAtLeast(0)
            val safeSegments = segmentCorrectMap.mapValues { it.value.coerceAtLeast(0) }
                .filter { it.value > 0 }
            return if (clampedStreak == currentStreakDays && clampedCheckin == dailyCheckinDay
                && clampedMultiplier == yogaMultiplier && safeScore == score
                && safeTotal == totalQuestions && safeSegments == segmentCorrectMap) this
            else copy(score = safeScore, totalQuestions = safeTotal,
                segmentCorrectMap = safeSegments, currentStreakDays = clampedStreak,
                dailyCheckinDay = clampedCheckin, yogaMultiplier = clampedMultiplier)
        }
    }

    data class Result(
        val baseCoins: Int,
        val accuracyBonus: Int,
        val streakBonus: Int,
        val checkinBonus: Int,
        val totalCoins: Int,
        val segmentCoins: Map<String, Int>,
        val breakdown: String,
    )

    fun calculate(input: Input): Result {
        val safe = input.sanitised()

        val accuracy = if (safe.totalQuestions > 0) {
            (safe.score.toFloat() / safe.totalQuestions).coerceIn(0f, 1f)
        } else 0f

        val base = 4
        val accuracyBonus = (accuracy * accuracy * 6f).toInt().coerceIn(0, 6)
        val streakBonus = (safe.currentStreakDays / 5).coerceAtMost(3)
        val checkinBonus = when (safe.dailyCheckinDay) {
            7 -> 3
            5, 6 -> 2
            in 2..4 -> 1
            else -> 0
        }

        val preMultiplier = base + accuracyBonus + streakBonus + checkinBonus
        val total = (preMultiplier * safe.yogaMultiplier).toInt().coerceAtLeast(0)

        val segCoins = if (safe.segmentCorrectMap.isNotEmpty()) {
            val canonicalCorrectMap = safe.segmentCorrectMap
                .map { (segment, correct) ->
                    val key = com.aipoweredgita.app.data.LearningSegment.values().find {
                        it.name.equals(segment, ignoreCase = true) ||
                        it.displayName.equals(segment, ignoreCase = true)
                    }?.name ?: segment.uppercase().replace(" ", "_")

                    key to correct
                }
                .groupBy({ it.first }, { it.second })
                .mapValues { (_, counts) -> counts.sum() }

            val totalCorrect = canonicalCorrectMap.values.sum().coerceAtLeast(1)

            canonicalCorrectMap.mapValues { (_, correct) ->
                ((accuracyBonus + streakBonus + checkinBonus) *
                    safe.yogaMultiplier *
                    (correct.toFloat() / totalCorrect))
                    .toInt()
                    .coerceAtLeast(0)
            }.filterValues { it > 0 }
        } else emptyMap()

        val breakdown = buildString {
            append("${base}base")
            if (accuracyBonus > 0) append("+${accuracyBonus}accuracy")
            if (streakBonus > 0) append("+${streakBonus}streak")
            if (checkinBonus > 0) append("+${checkinBonus}checkin")
            if (safe.yogaMultiplier != 1f) append("×${safe.yogaMultiplier}multiplier")
            append("=${total}")
        }

        return Result(base, accuracyBonus, streakBonus, checkinBonus, total, segCoins, breakdown)
    }
}
