package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.repository.ModeType
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

class StatsRepository(private val userStatsDao: UserStatsDao) {

    suspend fun trackQuizCompletion(score: Int, totalQuestions: Int) {
        // Update quiz statistics
        userStatsDao.incrementQuizzesTaken()
        userStatsDao.addQuestionsAnswered(totalQuestions)
        userStatsDao.addCorrectAnswers(score)

        // Update best score if this is better
        val currentStats = userStatsDao.getUserStatsOnce()
        currentStats?.let {
            val currentBestPercentage = if (it.bestScoreOutOf > 0)
                (it.bestScore.toFloat() / it.bestScoreOutOf) * 100
            else 0f

            val newPercentage = if (totalQuestions > 0) {
                (score.toFloat() / totalQuestions) * 100
            } else {
                0f
            }

            if (newPercentage > currentBestPercentage) {
                userStatsDao.updateBestScore(score, totalQuestions)
            }
        }

        updateStreak()
    }

    suspend fun trackVerseRead() {
        userStatsDao.incrementVersesRead()
        updateStreak()
    }

    suspend fun trackTimeSpent(seconds: Long) {
        userStatsDao.addTimeSpent(seconds)
    }

    suspend fun trackModeTime(seconds: Long, mode: ModeType) {
        when (mode) {
            ModeType.NORMAL -> userStatsDao.addNormalModeTime(seconds)
            ModeType.QUIZ -> userStatsDao.addQuizModeTime(seconds)
            ModeType.VOICE -> userStatsDao.addVoiceStudioTime(seconds)
        }
        updateStreak()
    }

    private suspend fun updateStreak() {
        val currentStats = userStatsDao.getUserStatsOnce() ?: return

        val today = LocalDate.now().toString()
        val lastActiveDate = currentStats.lastActiveDate

        // Update last active
        userStatsDao.updateLastActive(System.currentTimeMillis(), today)

        // Calculate streak
        when {
            lastActiveDate.isEmpty() -> {
                // First time
                userStatsDao.updateCurrentStreak(1)
                userStatsDao.updateLongestStreak(1)
            }
            lastActiveDate == today -> {
                // Already active today, no change
            }
            isYesterday(lastActiveDate) -> {
                // Consecutive day
                val newStreak = currentStats.currentStreak + 1
                userStatsDao.updateCurrentStreak(newStreak)
                if (newStreak > currentStats.longestStreak) {
                    userStatsDao.updateLongestStreak(newStreak)
                }
            }
            else -> {
                // Streak broken
                userStatsDao.updateCurrentStreak(1)
            }
        }
    }

    private fun isYesterday(dateString: String): Boolean {
        return try {
            val inputDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            val yesterday = LocalDate.now().minusDays(1)
            inputDate == yesterday
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateFavoritesCount(count: Int) {
        userStatsDao.updateFavoritesCount(count)
    }
}
