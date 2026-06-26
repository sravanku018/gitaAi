package com.aipoweredgita.app.domain.usecase

import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ml.AIBadgeSystem
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.UserLevel
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use case to generate AI badges and user level
 * Encapsulates the business logic for badge generation
 */
@Singleton
class GenerateBadgesUseCase @Inject constructor() {
    data class BadgeResult(
        val badges: List<UserBadge>,
        val level: UserLevel
    )

    suspend operator fun invoke(stats: UserStats): Result<BadgeResult> {
        return try {
            val badgeSystem = AIBadgeSystem()
            val badges = badgeSystem.generateBadges(
                versesRead = stats.versesRead,
                quizzesTaken = stats.totalQuizzesTaken,
                score = stats.totalCorrectAnswers,
                totalQuestions = stats.totalQuestionsAnswered.coerceAtLeast(1),
                timeSpent = stats.totalTimeSpentSeconds,
                currentStreak = stats.currentStreak,
                favoriteCount = stats.totalFavorites
            )

            val level = badgeSystem.calculateLevel(
                versesRead = stats.versesRead,
                quizzesTaken = stats.totalQuizzesTaken,
                score = stats.totalCorrectAnswers,
                totalQuestions = stats.totalQuestionsAnswered.coerceAtLeast(1),
                timeSpent = stats.totalTimeSpentSeconds,
                currentStreak = stats.currentStreak,
                favoriteCount = stats.totalFavorites
            )

            Result.success(BadgeResult(badges, level))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
