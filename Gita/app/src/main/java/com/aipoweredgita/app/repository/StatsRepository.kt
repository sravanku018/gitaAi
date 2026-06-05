package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.coin.CoinRewardEngine
import com.aipoweredgita.app.coin.CoinTransactionLogger
import com.aipoweredgita.app.coin.DailyRewardsTracker
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.PendingSyncEvent
import com.aipoweredgita.app.database.PendingSyncEventDao
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.CoinAwardRequest
import com.aipoweredgita.app.network.CoinSpendRequest
import com.aipoweredgita.app.network.CreateUserRequest
import com.aipoweredgita.app.network.ShareSlokaRequest
import com.aipoweredgita.app.services.SyncWorker
import com.aipoweredgita.app.utils.AuthPreferences
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsRepository(
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val appContext: Context,
    private val pendingSyncEventDao: PendingSyncEventDao = GitaDatabase.getDatabase(appContext).pendingSyncEventDao()
) {
    companion object {
        @Volatile
        private var lastSyncedUserId: String? = null
        private val syncLock = Any()
    }

    /** Observable network state for UI feedback. */
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Idle)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val authPrefs by lazy { AuthPreferences.getInstance(appContext) }

    private val _coinBalance = MutableStateFlow(authPrefs.localCoins)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val prefChangeListener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == "local_coins") {
            _coinBalance.value = prefs.getInt("local_coins", 0)
        }
    }

    init {
        appContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefChangeListener)
    }

    /** Ensures the user exists on the server before any coin API call. */
    private suspend fun ensureUserSynced() {
        val uid = userId() ?: return
        synchronized(syncLock) {
            if (lastSyncedUserId == uid) return
        }

        if (authPrefs.isGuestUser) {
            synchronized(syncLock) { lastSyncedUserId = uid }
            return
        }

        try {
            val balance = CoinApi.retrofitService.getBalance(uid)
            if (balance.krishna_coins >= 0) {
                synchronized(syncLock) { lastSyncedUserId = uid }
                return
            }
        } catch (_: Exception) { }

        syncUserWithCloud()
        synchronized(syncLock) { lastSyncedUserId = uid }
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

        val accuracy = if (totalQuestions > 0) (score.toFloat() / totalQuestions).coerceIn(0f, 1f) else 0f
        val currentStreak = stats?.currentStreak ?: 0
        val checkinDay = DailyRewardsTracker.getInstance(appContext).getCurrentCheckinDay()

        // Use CoinRewardEngine for calculation
        val result = CoinRewardEngine.calculate(
            CoinRewardEngine.Input(
                score = score,
                totalQuestions = totalQuestions,
                segmentCorrectMap = segmentCorrectMap,
                currentStreakDays = currentStreak,
                dailyCheckinDay = checkinDay
            )
        )

        val isGuest = authPrefs.isGuestUser

        val coins = if (isGuest) {
            if (result.totalCoins > 0) {
                authPrefs.addLocalCoins(result.totalCoins)
                CoinTransactionLogger.log(appContext, result.totalCoins, "${result.breakdown} (guest)")
            }
            result.totalCoins
        } else {
            userId()?.let { uid ->
                try {
                    val response = CoinApi.retrofitService.awardCoins(
                        CoinAwardRequest(
                            user_id = uid,
                            source = "quiz_completion",
                            metadata = mapOf(
                                "accuracy" to accuracy,
                                "score" to score,
                                "totalQuestions" to totalQuestions,
                                "streakDays" to currentStreak,
                                "checkinDay" to checkinDay
                            )
                        )
                    )
                    authPrefs.localCoins = response.total_coins
                    CoinTransactionLogger.log(appContext, response.awarded, result.breakdown)
                    response.awarded
                } catch (e: Exception) {
                    Log.e("StatsRepository", "Failed to award coins: ${e.message}")
                    val fallback = result.totalCoins
                    authPrefs.addLocalCoins(fallback)
                    CoinTransactionLogger.log(appContext, fallback, "${result.breakdown} (offline)")

                    try {
                        val payloadMap = mapOf(
                            "score" to score,
                            "totalQuestions" to totalQuestions,
                            "accuracy" to accuracy,
                            "streakDays" to currentStreak,
                            "checkinDay" to checkinDay
                        )
                        val payloadString = Gson().toJson(payloadMap)
                        pendingSyncEventDao.insert(
                            PendingSyncEvent(
                                userId = uid,
                                eventType = "QUIZ",
                                payload = payloadString,
                                coinsToAdjust = fallback
                            )
                        )
                        SyncWorker.schedule(appContext)
                    } catch (dbEx: Exception) {
                        Log.e("StatsRepository", "Failed to queue quiz sync event: ${dbEx.message}")
                    }

                    fallback
                }
            } ?: 0
        }

        updateStreak()
        return coins
    }

    suspend fun trackVerseRead() {
        userStatsDao.incrementVersesRead()
        updateStreak()
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

    suspend fun trackSlokaShared(chapter: Int? = null, verse: Int? = null): Int {
        updateStreak()

        val isGuest = authPrefs.isGuestUser
        val tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(appContext)
        val dailyState = tracker.getShareState()
        
        // Prevent duplicate share processing if already shared today
        if (dailyState.todayClaimed) {
            return 0
        }

        // Claim locally first to mark as claimed today
        val fallbackCoins = tracker.claimShare()
        if (fallbackCoins <= 0) return 0

        val coinsAwarded = if (isGuest) {
            authPrefs.addLocalCoins(fallbackCoins)
            CoinTransactionLogger.log(appContext, fallbackCoins, "Daily sloka share")
            fallbackCoins
        } else {
            ensureUserSynced()
            userId()?.let { uid ->
                try {
                    val slokaId = if (chapter != null && verse != null) "ch${chapter}v${verse}" else null
                    val response = CoinApi.retrofitService.share(ShareSlokaRequest(uid, slokaId, chapter = chapter, verse = verse))
                    tracker.isShareSynced = true
                    val totalAwarded = response.coins_awarded + response.weekly_bonus
                    if (totalAwarded > 0) {
                        authPrefs.addLocalCoins(totalAwarded)
                        CoinTransactionLogger.log(appContext, totalAwarded, "Daily sloka share")
                        totalAwarded
                    } else {
                        authPrefs.addLocalCoins(fallbackCoins)
                        CoinTransactionLogger.log(appContext, fallbackCoins, "Daily sloka share")
                        fallbackCoins
                    }
                } catch (e: Exception) {
                    Log.e("StatsRepository", "Failed to track sloka share: ${e.message}")
                    authPrefs.addLocalCoins(fallbackCoins)
                    CoinTransactionLogger.log(appContext, fallbackCoins, "Daily sloka share (offline)")
                    
                    try {
                        val hasPending = pendingSyncEventDao.getPendingEvents(uid).any { it.eventType == "SHARE" }
                        if (!hasPending) {
                            val slokaId = if (chapter != null && verse != null) "ch${chapter}v${verse}" else null
                            val payloadMap = mapOf(
                                "chapter" to chapter,
                                "verse" to verse,
                                "slokaId" to slokaId
                            )
                            val payloadString = Gson().toJson(payloadMap)
                            pendingSyncEventDao.insert(
                                PendingSyncEvent(
                                    userId = uid,
                                    eventType = "SHARE",
                                    payload = payloadString,
                                    coinsToAdjust = fallbackCoins
                                )
                            )
                            SyncWorker.schedule(appContext)
                        }
                    } catch (dbEx: Exception) {
                        Log.e("StatsRepository", "Failed to queue share sync event: ${dbEx.message}")
                    }

                    fallbackCoins
                }
            } ?: fallbackCoins
        }
        return coinsAwarded
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
        userStatsDao.incrementChaptersCompleted()
        val isGuest = authPrefs.isGuestUser

        if (isGuest) {
            authPrefs.addLocalCoins(15)
            CoinTransactionLogger.log(appContext, 15, "Chapter $chapterNo Completion (guest)")
        } else {
            ensureUserSynced()
            userId()?.let { uid ->
                try {
                    val response = CoinApi.retrofitService.awardCoins(CoinAwardRequest(uid, "chapter_completion"))
                    authPrefs.localCoins = response.total_coins
                    CoinTransactionLogger.log(appContext, response.awarded, "Chapter $chapterNo Completion")
                } catch (e: Exception) {
                    Log.e("StatsRepository", "Failed to award chapter coins: ${e.message}")
                    authPrefs.addLocalCoins(15)
                    CoinTransactionLogger.log(appContext, 15, "Chapter $chapterNo Completion (offline)")

                    try {
                        val payloadMap = mapOf("chapterNo" to chapterNo)
                        val payloadString = Gson().toJson(payloadMap)
                        pendingSyncEventDao.insert(
                            PendingSyncEvent(
                                userId = uid,
                                eventType = "CHAPTER",
                                payload = payloadString,
                                coinsToAdjust = 15
                            )
                        )
                        SyncWorker.schedule(appContext)
                    } catch (dbEx: Exception) {
                        Log.e("StatsRepository", "Failed to queue chapter sync event: ${dbEx.message}")
                    }
                }
            }
        }
        updateStreak()
    }

    suspend fun syncUserWithCloud() {
        val stats = userStatsDao.getUserStatsOnce() ?: return
        val uid = stats.userId
        if (uid.isEmpty()) return

        try {
            if (authPrefs.isGuestUser) {
                try {
                    val response = CoinApi.retrofitService.createGuest()
                    val guestId = response.guest_id
                    if (guestId.isNotEmpty()) {
                        authPrefs.saveGuestState(guestId)
                    }
                    Log.d("StatsRepository", "Guest synced: $guestId with ${response.coins} coins")
                } catch (guestError: Exception) {
                    Log.w("StatsRepository", "Guest creation failed (will sync on login): ${guestError.message}")
                }
            } else {
                CoinApi.retrofitService.createUser(CreateUserRequest(uid, stats.userName.ifEmpty { "Gita Seeker" }, ""))
                Log.d("StatsRepository", "User $uid successfully synced with cloud.")
            }
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync user with cloud: ${e.message}")
        }
    }

    /** Sync a locally-recorded check-in to the cloud (safe to call even if already synced). */
    suspend fun syncCheckinToCloud(coinsToAdjust: Int = 0) {
        ensureUserSynced()
        val uid = userId() ?: return
        try {
            CoinApi.retrofitService.checkin(mapOf("user_id" to uid))
            DailyRewardsTracker.getInstance(appContext).isCheckinSynced = true
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync checkin to cloud: ${e.message}")
            try {
                val hasPending = pendingSyncEventDao.getPendingEvents(uid).any { it.eventType == "CHECKIN" }
                if (!hasPending) {
                    val finalCoins = if (coinsToAdjust > 0) coinsToAdjust else DailyRewardsTracker.getInstance(appContext).getCurrentCheckinDay()
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = uid,
                            eventType = "CHECKIN",
                            payload = "{}",
                            coinsToAdjust = finalCoins
                        )
                    )
                    SyncWorker.schedule(appContext)
                }
            } catch (dbEx: Exception) {
                Log.e("StatsRepository", "Failed to queue checkin sync event: ${dbEx.message}")
            }
        }
    }

    /** Sync a locally-recorded share to the cloud (safe to call even if already synced). */
    suspend fun syncShareToCloud(coinsToAdjust: Int = 0) {
        ensureUserSynced()
        val uid = userId() ?: return
        try {
            CoinApi.retrofitService.share(ShareSlokaRequest(uid, "local_sync"))
            DailyRewardsTracker.getInstance(appContext).isShareSynced = true
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync share to cloud: ${e.message}")
            try {
                val hasPending = pendingSyncEventDao.getPendingEvents(uid).any { it.eventType == "SHARE" }
                if (!hasPending) {
                    val finalCoins = if (coinsToAdjust > 0) coinsToAdjust else DailyRewardsTracker.getInstance(appContext).getShareState().reward
                    val payloadMap = mapOf("slokaId" to "local_sync")
                    val payloadString = Gson().toJson(payloadMap)
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = uid,
                            eventType = "SHARE",
                            payload = payloadString,
                            coinsToAdjust = finalCoins
                        )
                    )
                    SyncWorker.schedule(appContext)
                }
            } catch (dbEx: Exception) {
                Log.e("StatsRepository", "Failed to queue share sync event: ${dbEx.message}")
            }
        }
    }

    suspend fun claimDailyReward(coins: Int, description: String) {
        authPrefs.addLocalCoins(coins)
        CoinTransactionLogger.log(appContext, coins, description)

        if (!authPrefs.isGuestUser) {
            syncCheckinToCloud(coins)
        }
    }

    suspend fun claimShareReward(coins: Int, description: String) {
        authPrefs.addLocalCoins(coins)
        CoinTransactionLogger.log(appContext, coins, description)

        if (!authPrefs.isGuestUser) {
            syncShareToCloud(coins)
        }
    }

    suspend fun getBalance(): Int {
        if (authPrefs.isGuestUser) {
            // Award 50 coin welcome bonus to new guests (once only)
            if (!authPrefs.guestWelcomeAwarded) {
                authPrefs.localCoins = 50
                authPrefs.guestWelcomeAwarded = true
                CoinTransactionLogger.log(appContext, 50, "Welcome bonus (guest)")
            }
            return authPrefs.localCoins
        }

        val uid = userId() ?: return authPrefs.localCoins
        _networkState.value = NetworkState.Loading("balance")
        return try {
            val serverCoins = CoinApi.retrofitService.getBalance(uid).krishna_coins
            val pendingEvents = pendingSyncEventDao.getPendingEvents(uid)
            val pendingCoinsAdjustment = pendingEvents.sumOf { it.coinsToAdjust }
            val adjustedBalance = serverCoins + pendingCoinsAdjustment
            authPrefs.localCoins = adjustedBalance
            _networkState.value = NetworkState.Success
            adjustedBalance
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to get balance: ${e.message}")
            _networkState.value = NetworkState.Error("balance", e.message ?: "Network error")
            authPrefs.localCoins
        }
    }

    suspend fun spendCoins(question: String): Boolean {
        val isGuest = authPrefs.isGuestUser

        if (isGuest) {
            if (authPrefs.localCoins < 10) return false
            authPrefs.addLocalCoins(-10)
            CoinTransactionLogger.log(appContext, -10, "Asked question: $question")
            return true
        }

        ensureUserSynced()
        val uid = userId() ?: return false
        _networkState.value = NetworkState.Loading("spend")
        try {
            val response = CoinApi.retrofitService.spendCoins(CoinSpendRequest(uid, question))
            authPrefs.localCoins = response.remaining_balance
            _networkState.value = NetworkState.Success
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to spend coins: ${e.message}")
            _networkState.value = NetworkState.Error("spend", e.message ?: "Network error")
            if (authPrefs.localCoins < 10) return false
            authPrefs.addLocalCoins(-10)
            CoinTransactionLogger.log(appContext, -10, "Asked question: $question (offline)")

            try {
                val payloadMap = mapOf("question" to question)
                val payloadString = Gson().toJson(payloadMap)
                pendingSyncEventDao.insert(
                    PendingSyncEvent(
                        userId = uid,
                        eventType = "SPEND",
                        payload = payloadString,
                        coinsToAdjust = -10
                    )
                )
                SyncWorker.schedule(appContext)
            } catch (dbEx: Exception) {
                Log.e("StatsRepository", "Failed to queue spend sync event: ${dbEx.message}")
            }
        }
        return true
    }
}