package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.CoinAwardRequest
import com.aipoweredgita.app.network.CoinSpendRequest
import com.aipoweredgita.app.network.CreateUserRequest
import com.aipoweredgita.app.network.ShareSlokaRequest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsRepository(
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val appContext: Context? = null
) {
    companion object {
        /** Shared across all StatsRepository instances to prevent duplicate cloud user creation. */
        private var userSynced = false
    }

    /** Ensures the user exists on the server before any coin API call. */
    private suspend fun ensureUserSynced() {
        if (userSynced) return
        val uid = userId()
        if (uid != null) {
            // Check if user already exists on server to avoid duplicate welcome bonus
            try {
                val balance = CoinApi.retrofitService.getBalance(uid)
                if (balance.krishna_coins >= 0) {
                    userSynced = true // Already exists, skip creation
                    return
                }
            } catch (_: Exception) { }
            // User doesn't exist yet — create them
            syncUserWithCloud()
            userSynced = true
        }
    }

    private suspend fun userId(): String? = userStatsDao.getUserStatsOnce()?.userId?.takeIf { it.isNotEmpty() }

    suspend fun trackQuizCompletion(
        score: Int,
        totalQuestions: Int,
        segmentCorrectMap: Map<String, Int> = emptyMap()
    ): Int {
        ensureUserSynced()
        userStatsDao.incrementQuizzesTaken()
        userStatsDao.addQuestionsAnswered(totalQuestions)
        userStatsDao.addCorrectAnswers(score)

        val stats = userStatsDao.getUserStatsOnce()
        stats?.let {
            val currentBestPercentage = if (it.bestScoreOutOf > 0)
                (it.bestScore.toFloat() / it.bestScoreOutOf) * 100
            else 0f
            val newPercentage = if (totalQuestions > 0) (score.toFloat() / totalQuestions) * 100 else 0f
            if (newPercentage > currentBestPercentage)
                userStatsDao.updateBestScore(score, totalQuestions)
        }

        val coins = userId()?.let { uid ->
            try {
                val response = CoinApi.retrofitService.awardCoins(CoinAwardRequest(uid, "quiz_completion"))
                response.awarded
            } catch (e: Exception) {
                android.util.Log.e("StatsRepository", "Failed to award coins: ${e.message}")
                0
            }
        } ?: 0

        updateStreak()
        return coins
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

        userStatsDao.updateLastActive(System.currentTimeMillis(), today)

        when {
            lastActiveDate.isEmpty() -> {
                userStatsDao.updateCurrentStreak(1)
                userStatsDao.updateLongestStreak(1)
                userStatsDao.updateDaysActive(1)
            }
            lastActiveDate == today -> {}
            isYesterday(lastActiveDate) -> {
                val newStreak = currentStats.currentStreak + 1
                userStatsDao.updateCurrentStreak(newStreak)
                if (newStreak > currentStats.longestStreak) {
                    userStatsDao.updateLongestStreak(newStreak)
                }
                userStatsDao.updateDaysActive(currentStats.daysActive + 1)
            }
            else -> {
                userStatsDao.updateCurrentStreak(1)
                userStatsDao.updateDaysActive(currentStats.daysActive + 1)
            }
        }
    }

    suspend fun trackSlokaShared(chapter: Int? = null, verse: Int? = null) {
        ensureUserSynced()
        val uid = userId()
        if (uid != null) {
            try {
                val slokaId = if (chapter != null && verse != null) "ch${chapter}v${verse}" else null
                CoinApi.retrofitService.share(ShareSlokaRequest(uid, slokaId, chapter = chapter, verse = verse))
            } catch (e: Exception) {
                android.util.Log.e("StatsRepository", "Failed to track sloka share: ${e.message}")
            }
        }
        updateStreak()
    }

    suspend fun trackCheckinClaimed() {
        ensureUserSynced()
        val uid = userId()
        if (uid != null) {
            try { CoinApi.retrofitService.checkin(mapOf("user_id" to uid)) } catch (e: Exception) {
                android.util.Log.e("StatsRepository", "Failed to track checkin: ${e.message}")
            }
        }
        val today = LocalDate.now().toString()
        dailyActivityDao?.let { dao ->
            dao.insertIfAbsent(DailyActivity(date = today))
        }
        updateStreak()
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

    suspend fun trackChapterCompleted(chapterNo: Int) {
        ensureUserSynced()
        userStatsDao.incrementChaptersCompleted()
        userId()?.let { uid ->
            try { CoinApi.retrofitService.awardCoins(CoinAwardRequest(uid, "chapter_completion")) }
            catch (e: Exception) { android.util.Log.e("StatsRepository", "Failed to award chapter coins: ${e.message}") }
        }
        updateStreak()
    }

    suspend fun syncUserWithCloud() {
        val stats = userStatsDao.getUserStatsOnce() ?: return
        val uid = stats.userId
        if (uid.isEmpty()) return

        try {
            CoinApi.retrofitService.createUser(CreateUserRequest(uid, stats.userName.ifEmpty { "Gita Seeker" }, ""))
            Log.d("StatsRepository", "User $uid successfully synced with cloud.")
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync user with cloud: ${e.message}")
        }
    }

    suspend fun updateFavoritesCount(count: Int) {
        userStatsDao.updateFavoritesCount(count)
    }

    /** Sync a locally-recorded check-in to the cloud (safe to call even if already synced). */
    suspend fun syncCheckinToCloud() {
        ensureUserSynced()
        val uid = userId() ?: return
        try { CoinApi.retrofitService.checkin(mapOf("user_id" to uid)) }
        catch (e: Exception) { android.util.Log.e("StatsRepository", "Failed to sync checkin to cloud: ${e.message}") }
    }

    /** Sync a locally-recorded share to the cloud (safe to call even if already synced). */
    suspend fun syncShareToCloud() {
        ensureUserSynced()
        val uid = userId() ?: return
        try { CoinApi.retrofitService.share(ShareSlokaRequest(uid, "local_sync")) }
        catch (e: Exception) { android.util.Log.e("StatsRepository", "Failed to sync share to cloud: ${e.message}") }
    }

    suspend fun getBalance(): Int {
        val uid = userId() ?: return 0
        return try {
            CoinApi.retrofitService.getBalance(uid).krishna_coins
        } catch (e: Exception) { android.util.Log.e("StatsRepository", "Failed to get balance: ${e.message}"); 0 }
    }

    suspend fun spendCoins(question: String) {
        ensureUserSynced()
        val uid = userId() ?: return
        try {
            CoinApi.retrofitService.spendCoins(CoinSpendRequest(uid, question))
        } catch (e: Exception) { android.util.Log.e("StatsRepository", "Failed to spend coins: ${e.message}") }
    }
}
