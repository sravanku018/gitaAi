package com.aipoweredgita.app.coin

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Coin reward rules — must match server `deno-backend-hono.ts` award path.
 *
 * Quiz: base 5 + accuracy tier 1–6, cap 15, then × yoga (1/2/2/3/3), round, cap 10_000.
 * Battle: Fibonacci(correct) then × yoga, round, cap 10_000.
 * Chapter: 15 × yoga, round, cap 10_000.
 *
 * Pure calculation — no side effects.
 */
object CoinRewardEngine {

    const val QUIZ_BASE = 5
    const val QUIZ_MAX_BEFORE_YOGA = 15
    const val CHAPTER_BASE = 15
    const val COIN_HARD_CAP = 10_000

    data class Input(
        val score: Int,
        val totalQuestions: Int,
        val segmentCorrectMap: Map<String, Int> = emptyMap(),
        val currentStreakDays: Int = 0,
        val dailyCheckinDay: Int = 0,
        val yogaLevel: Int = 1,
        val yogaMultiplier: Float = 1f,
    ) {
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
            else copy(
                score = safeScore,
                totalQuestions = safeTotal,
                segmentCorrectMap = safeSegments,
                currentStreakDays = clampedStreak,
                dailyCheckinDay = clampedCheckin,
                yogaMultiplier = clampedMultiplier
            )
        }
    }

    data class Result(
        val baseCoins: Int,
        val accuracyBonus: Int,
        val streakBonus: Int,
        val checkinBonus: Int,
        /** Final coins after yoga × (same as server award). */
        val totalCoins: Int,
        val segmentCoins: Map<String, Int>,
        val breakdown: String,
        val yogaMultiplier: Float = 1f,
        val coinsBeforeYoga: Int = 0,
    )

    /** Same accuracy tiers as server /coins/award quiz_completion. */
    fun accuracyBonus(accuracy: Float): Int {
        val a = accuracy.coerceIn(0f, 1f)
        return when {
            a >= 0.9f -> 6
            a >= 0.8f -> 5
            a >= 0.7f -> 4
            a >= 0.6f -> 3
            a >= 0.5f -> 2
            else -> 1
        }
    }

    /**
     * Battle Fibonacci — same as server battleFibCoins and UI BattleState.battleCoins.
     */
    fun battleFibCoins(correctAnswers: Int): Int {
        val n = correctAnswers.coerceAtLeast(0)
        if (n <= 0) return 0
        var a = 1
        var b = 1
        for (i in 3..n) {
            val temp = a + b
            a = b
            b = temp
        }
        return if (n == 1) a else b
    }

    /** Integer yoga × then round (server Math.round). */
    fun applyYogaMultiplier(baseCoins: Int, yogaMultiplier: Float): Int {
        if (baseCoins <= 0) return 0
        val mult = yogaMultiplier.coerceAtLeast(0f)
        return max(0, (baseCoins * mult).roundToInt()).coerceAtMost(COIN_HARD_CAP)
    }

    fun calculate(input: Input): Result {
        val safe = input.sanitised()

        val accuracy = if (safe.totalQuestions > 0) {
            (safe.score.toFloat() / safe.totalQuestions).coerceIn(0f, 1f)
        } else 0f

        val base = QUIZ_BASE
        val accuracyBonus = accuracyBonus(accuracy)
        val beforeYoga = (base + accuracyBonus).coerceAtMost(QUIZ_MAX_BEFORE_YOGA)
        val total = applyYogaMultiplier(beforeYoga, safe.yogaMultiplier)

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
                (accuracyBonus * (correct.toFloat() / totalCorrect))
                    .toInt()
                    .coerceAtLeast(0)
            }.filterValues { it > 0 }
        } else emptyMap()

        val breakdown = if (safe.yogaMultiplier > 1f) {
            "${base}base + ${accuracyBonus}acc = ${beforeYoga} ×${safe.yogaMultiplier} = $total"
        } else {
            "${base}base + ${accuracyBonus}acc = $total"
        }

        return Result(
            baseCoins = base,
            accuracyBonus = accuracyBonus,
            streakBonus = 0,
            checkinBonus = 0,
            totalCoins = total,
            segmentCoins = segCoins,
            breakdown = breakdown,
            yogaMultiplier = safe.yogaMultiplier,
            coinsBeforeYoga = beforeYoga,
        )
    }

    fun battleTotal(correctAnswers: Int, yogaMultiplier: Float): Int {
        val fib = battleFibCoins(correctAnswers)
        return applyYogaMultiplier(fib, yogaMultiplier)
    }

    fun chapterTotal(yogaMultiplier: Float): Int =
        applyYogaMultiplier(CHAPTER_BASE, yogaMultiplier)
}
