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
import com.aipoweredgita.app.database.UserStats
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StatsRepository(
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao? = null,
    private val appContext: Context,
    private val pendingSyncEventDao: PendingSyncEventDao = GitaDatabase.getDatabase(appContext).pendingSyncEventDao()
) {

    /** Observable network state for UI feedback. */
    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Idle)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val authPrefs by lazy { AuthPreferences.getInstance(appContext) }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val coinBalance: StateFlow<Int> = userStatsDao.getUserStats()
        .map { it?.krishnaCoins ?: 0 }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 0)

    /** Expose UserStats flow for ProfileViewModel (replaces direct GitaDatabase access). */
    fun getUserStatsFlow(): kotlinx.coroutines.flow.Flow<UserStats?> = userStatsDao.getUserStats()

    /** Initialize stats row if not yet created. */
    suspend fun initializeStatsIfNeeded() = userStatsDao.initializeStatsIfNeeded()

    /**
     * The single source of truth for mapping a server Balance response onto the local UserStats entity,
     * preserving any local-only fields (like totalTimeSpentSeconds, userName, etc.)
     */
    private fun com.aipoweredgita.app.network.CoinBalanceResponse.updateEntity(
        current: com.aipoweredgita.app.database.UserStats,
        userId: String
    ): com.aipoweredgita.app.database.UserStats {
        return current.copy(
            userId = userId,
            krishnaCoins = krishna_coins,
            daysActive = days_active,
            currentStreak = current_streak,
            longestStreak = longest_streak,
            totalQuizzesTaken = total_quizzes_taken,
            totalQuestionsAnswered = total_questions_answered,
            totalCorrectAnswers = total_correct_answers,
            bestScore = best_score,
            bestScoreOutOf = best_score_out_of,
            versesRead = verses_read,
            chaptersCompleted = chapters_completed,
            lastActiveDate = last_activity_date ?: current.lastActiveDate,
            serverUpdatedAt = updated_at ?: current.serverUpdatedAt
        )
    }

    /**
     * Unified sync function that acts as the single source of truth for pulling state from the server.
     * Uses a staleness guard (serverUpdatedAt) to prevent slow background syncs from clobbering fresh optimistic updates.
     */
    suspend fun refreshUserState(uid: String) {
        val token = authPrefs.token ?: return
        try {
            val balance = CoinApi.retrofitService.getBalance(uid, "Bearer $token")
            val currentStats = userStatsDao.getUserStatsOnce() ?: com.aipoweredgita.app.database.UserStats(id = 1, userId = uid)

            // Server timestamp guard against stale overwrites
            val serverUpdated = balance.updated_at
            if (serverUpdated != null && serverUpdated.isNotEmpty() && currentStats.serverUpdatedAt.isNotEmpty()) {
                if (serverUpdated < currentStats.serverUpdatedAt) {
                    Log.w("Sync", "Skipping stale server data: ${serverUpdated} < ${currentStats.serverUpdatedAt}")
                    return
                }
            }

            // Map and save entity
            val updatedStats = balance.updateEntity(currentStats, uid)
            userStatsDao.insertStats(updatedStats) // Upsert

            // Sync daily UI trackers — skip on fresh install so streak resets
            val tracker = DailyRewardsTracker.getInstance(appContext)
            if (!tracker.isFreshInstall()) {
                if (balance.checkin_day > 0) {
                    tracker.syncWithServer(balance.checkin_day, balance.checkin_week, balance.last_checkin)
                }
                if (balance.share_day > 0) {
                    tracker.syncShareWithServer(balance.share_day, balance.share_week, balance.last_share)
                }
            }
            
            _networkState.value = NetworkState.Success
        } catch (e: Exception) {
            Log.e("Sync", "Failed refreshUserState: ${e.message}")
            _networkState.value = NetworkState.Error("sync", e.message ?: "Sync failed")
        }
    }

    /** Ensures the user exists on the server before any coin API call. */
    private suspend fun ensureUserSynced() {
        val uid = userId() ?: return

        if (authPrefs.isGuestUser) {
            return
        }

        refreshUserState(uid)
        syncUserWithCloud()
    }

    private suspend fun userId(): String? = userStatsDao.getUserStatsOnce()?.userId?.takeIf { it.isNotEmpty() }

    suspend fun trackQuizCompletion(
        score: Int,
        totalQuestions: Int,
        segmentCorrectMap: Map<String, Int> = emptyMap(),
        quizType: String = "general",
        timeSpentSeconds: Long = 0
    ): Int {
        ensureUserSynced()
        userStatsDao.incrementQuizzesTaken()
        userStatsDao.addQuestionsAnswered(totalQuestions)
        userStatsDao.addCorrectAnswers(score)

        // Track quiz time in daily activity
        val today = LocalDate.now().toString()
        dailyActivityDao?.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
        dailyActivityDao?.addQuizSeconds(today, if (timeSpentSeconds > 0) timeSpentSeconds else 60)

        // Track quiz mode time in user stats for overview time distribution
        if (timeSpentSeconds > 0) {
            userStatsDao.addQuizModeTime(timeSpentSeconds)
        }

        // Update streak BEFORE coin calculation so reward reflects current streak
        updateStreak()

        // Re-read stats to get the updated streak
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
                userStatsDao.addKrishnaCoins(result.totalCoins)
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
                                "checkinDay" to checkinDay,
                                "quizType" to quizType
                            )
                        )
                    )
                    userStatsDao.updateKrishnaCoins(response.total_coins)
                    CoinTransactionLogger.log(appContext, response.awarded, result.breakdown)
                    // Record quiz attempt on server
                    try {
                        CoinApi.retrofitService.recordQuizAttempt(
                            com.aipoweredgita.app.network.QuizAttemptRequest(
                                user_id = uid,
                                score = score,
                                total_questions = totalQuestions,
                                quiz_type = quizType,
                                time_spent_seconds = timeSpentSeconds,
                                coins_earned = response.awarded
                            )
                        )
                    } catch (_: Exception) {}
                    response.awarded
                } catch (e: Exception) {
                    Log.e("StatsRepository", "Failed to award coins: ${e.message}")
                    val fallback = result.totalCoins
                    userStatsDao.addKrishnaCoins(fallback)
                    CoinTransactionLogger.log(appContext, fallback, "${result.breakdown} (offline)")

                    try {
                        val payloadMap = mapOf(
                            "score" to score,
                            "totalQuestions" to totalQuestions,
                            "accuracy" to accuracy,
                            "streakDays" to currentStreak,
                            "checkinDay" to checkinDay,
                            "quizType" to quizType
                        )
                        val payloadString = Gson().toJson(payloadMap)
                        pendingSyncEventDao.insert(
                            PendingSyncEvent(
                                userId = uid,
                                eventType = "QUIZ",
                                payload = payloadString,
                                coinsToAdjust = fallback,
                                idempotencyKey = "quiz_${uid}_${System.currentTimeMillis()}"
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

        return coins
    }

    suspend fun trackVerseRead() {
        userStatsDao.incrementVersesRead()
        val today = LocalDate.now().toString()
        dailyActivityDao?.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
        dailyActivityDao?.addVerses(today, 1)
        updateStreak()
    }

    suspend fun trackModeTime(seconds: Long, mode: ModeType) {
        when (mode) {
            ModeType.NORMAL -> userStatsDao.addNormalModeTime(seconds)
            ModeType.QUIZ -> userStatsDao.addQuizModeTime(seconds)
            ModeType.VOICE -> userStatsDao.addVoiceStudioTime(seconds)
        }

        // Also update daily_activity table for calendar heat map
        val today = LocalDate.now().toString()
        dailyActivityDao?.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
        when (mode) {
            ModeType.NORMAL -> dailyActivityDao?.addNormalSeconds(today, seconds)
            ModeType.QUIZ -> dailyActivityDao?.addQuizSeconds(today, seconds)
            ModeType.VOICE -> dailyActivityDao?.addVoiceStudioSeconds(today, seconds)
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

        // Continually back up monotonic stats (quizzes taken, verses read) to the server
        // We use a PendingSyncEvent instead of a direct coroutine to ensure this runs 
        // AFTER the current SQLite transaction completely commits.
        try {
            val payloadString = "{}" // SyncWorker now reads fresh stats directly from DB
            pendingSyncEventDao.insert(
                com.aipoweredgita.app.database.PendingSyncEvent(
                    userId = currentStats.userId,
                    eventType = "STATS_SYNC",
                    payload = payloadString,
                    coinsToAdjust = 0,
                    idempotencyKey = "stats_sync_${currentStats.userId}_${System.currentTimeMillis()}"
                )
            )
            com.aipoweredgita.app.services.SyncWorker.schedule(appContext)
        } catch (dbEx: Exception) {
            android.util.Log.e("StatsRepository", "Failed to queue stats sync event: ${dbEx.message}")
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
            userStatsDao.addKrishnaCoins(fallbackCoins)
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
                        userStatsDao.addKrishnaCoins(totalAwarded)
                        CoinTransactionLogger.log(appContext, totalAwarded, "Daily sloka share")
                        totalAwarded
                    } else {
                        userStatsDao.addKrishnaCoins(fallbackCoins)
                        CoinTransactionLogger.log(appContext, fallbackCoins, "Daily sloka share")
                        fallbackCoins
                    }
                } catch (e: Exception) {
                    Log.e("StatsRepository", "Failed to track sloka share: ${e.message}")
                    userStatsDao.addKrishnaCoins(fallbackCoins)
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
                                coinsToAdjust = fallbackCoins,
                                idempotencyKey = "share_${uid}_${System.currentTimeMillis()}"
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
            userStatsDao.addKrishnaCoins(15)
            CoinTransactionLogger.log(appContext, 15, "Chapter $chapterNo Completion (guest)")
        } else {
            ensureUserSynced()
            userId()?.let { uid ->
                try {
                    val response = CoinApi.retrofitService.awardCoins(CoinAwardRequest(uid, "chapter_completion"))
                    userStatsDao.updateKrishnaCoins(response.total_coins)
                    CoinTransactionLogger.log(appContext, response.awarded, "Chapter $chapterNo Completion")
                } catch (e: Exception) {
                    Log.e("StatsRepository", "Failed to award chapter coins: ${e.message}")
                    userStatsDao.addKrishnaCoins(15)
                    CoinTransactionLogger.log(appContext, 15, "Chapter $chapterNo Completion (offline)")

                    try {
                        val payloadMap = mapOf("chapterNo" to chapterNo)
                        val payloadString = Gson().toJson(payloadMap)
                        pendingSyncEventDao.insert(
                            PendingSyncEvent(
                                userId = uid,
                                eventType = "CHAPTER",
                                payload = payloadString,
                                coinsToAdjust = 15,
                                idempotencyKey = "chapter_${chapterNo}_${uid}_${System.currentTimeMillis()}"
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
                val response = CoinApi.retrofitService.createUser(CreateUserRequest(uid, stats.userName.ifEmpty { "Gita Seeker" }, ""))
                if (response.token != null && authPrefs.token == null) {
                    authPrefs.saveLoginState(userId = uid, loginMethod = "device", token = response.token)
                    Log.d("StatsRepository", "Upgraded old user $uid with new backend session token")
                }
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
            val localDate = DailyRewardsTracker.getInstance(appContext).nowLocal()
            val response = CoinApi.retrofitService.checkin(mapOf(
                "user_id" to uid,
                "client_date" to localDate
            ))
            if (response.duplicate == true) {
                Log.d("StatsRepository", "Checkin already synced (duplicate)")
            } else {
                Log.d("StatsRepository", "Checkin synced. Coins awarded: ${response.coins_awarded}")
            }
            refreshUserState(uid)
            DailyRewardsTracker.getInstance(appContext).isCheckinSynced = true
        } catch (e: retrofit2.HttpException) {
            // 400 = "Already checked in today" - that's OK, mark as synced
            Log.d("StatsRepository", "Checkin sync: HTTP ${e.code()} - marking as synced")
            DailyRewardsTracker.getInstance(appContext).isCheckinSynced = true
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync checkin: ${e.message}")
            queueCheckinSync(uid, coinsToAdjust)
        }
    }

    private suspend fun queueCheckinSync(uid: String, coinsToAdjust: Int) {
        try {
            val hasPending = pendingSyncEventDao.getPendingEvents(uid).any { it.eventType == "CHECKIN" }
            if (!hasPending) {
                val finalCoins = if (coinsToAdjust > 0) coinsToAdjust else DailyRewardsTracker.getInstance(appContext).getCurrentCheckinDay()
                pendingSyncEventDao.insert(
                    PendingSyncEvent(
                        userId = uid,
                        eventType = "CHECKIN",
                        payload = "{}",
                        coinsToAdjust = finalCoins,
                        idempotencyKey = "checkin_${uid}_${System.currentTimeMillis()}"
                    )
                )
                SyncWorker.schedule(appContext)
            }
        } catch (dbEx: Exception) {
            Log.e("StatsRepository", "Failed to queue checkin sync: ${dbEx.message}")
        }
    }

    /** Sync a locally-recorded share to the cloud (safe to call even if already synced). */
    suspend fun syncShareToCloud(coinsToAdjust: Int = 0) {
        ensureUserSynced()
        val uid = userId() ?: return
        try {
            val localDate = DailyRewardsTracker.getInstance(appContext).nowLocal()
            val response = CoinApi.retrofitService.share(ShareSlokaRequest(uid, "local_sync", client_date = localDate))
            if (response.duplicate == true) {
                Log.d("StatsRepository", "Share already synced (duplicate)")
            } else {
                Log.d("StatsRepository", "Share synced. Coins awarded: ${response.coins_awarded}")
            }
            refreshUserState(uid)
            DailyRewardsTracker.getInstance(appContext).isShareSynced = true
        } catch (e: retrofit2.HttpException) {
            // 400 = "Already shared today" - that's OK, mark as synced
            Log.d("StatsRepository", "Share sync: HTTP ${e.code()} - marking as synced")
            DailyRewardsTracker.getInstance(appContext).isShareSynced = true
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync share: ${e.message}")
            queueShareSync(uid, coinsToAdjust)
        }
    }

    private suspend fun queueShareSync(uid: String, coinsToAdjust: Int) {
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
                        coinsToAdjust = finalCoins,
                        idempotencyKey = "share_sync_${uid}_${System.currentTimeMillis()}"
                    )
                )
                SyncWorker.schedule(appContext)
            }
        } catch (dbEx: Exception) {
            Log.e("StatsRepository", "Failed to queue share sync: ${dbEx.message}")
        }
    }

    suspend fun claimDailyReward(coins: Int, description: String) {
        userStatsDao.addKrishnaCoins(coins)
        CoinTransactionLogger.log(appContext, coins, description)

        if (!authPrefs.isGuestUser) {
            syncCheckinToCloud(coins)
        }
    }

    suspend fun claimShareReward(coins: Int, description: String) {
        userStatsDao.addKrishnaCoins(coins)
        CoinTransactionLogger.log(appContext, coins, description)

        if (!authPrefs.isGuestUser) {
            syncShareToCloud(coins)
        }
    }

    suspend fun getBalance(): Int {
        if (authPrefs.isGuestUser) {
            // Award 50 coin welcome bonus to new guests (once only)
            if (!authPrefs.guestWelcomeAwarded) {
                userStatsDao.updateKrishnaCoins(50)
                authPrefs.guestWelcomeAwarded = true
                CoinTransactionLogger.log(appContext, 50, "Welcome bonus (guest)")
            }
            return coinBalance.value
        }

        val uid = userId() ?: return coinBalance.value
        val token = authPrefs.token ?: run {
            Log.w("StatsRepository", "No auth token — returning local balance")
            return coinBalance.value
        }

        _networkState.value = NetworkState.Loading("balance")
        return try {
            val balanceResponse = CoinApi.retrofitService.getBalance(
                userId = uid,
                token  = "Bearer $token"
            )
            val serverCoins = balanceResponse.krishna_coins
            val pendingEvents = pendingSyncEventDao.getPendingEvents(uid)
            val pendingCoinsAdjustment = pendingEvents.sumOf { it.coinsToAdjust }
            val adjustedBalance = serverCoins + pendingCoinsAdjustment
            userStatsDao.updateKrishnaCoins(adjustedBalance)
            
            val currentStats = userStatsDao.getUserStatsOnce() ?: com.aipoweredgita.app.database.UserStats(id = 1, userId = uid)
            
            // Server timestamp guard against stale overwrites
            val serverUpdated = balanceResponse.updated_at
            if (serverUpdated != null && serverUpdated.isNotEmpty() && currentStats.serverUpdatedAt.isNotEmpty()) {
                if (serverUpdated < currentStats.serverUpdatedAt) {
                    Log.w("Sync", "Skipping stale server data in getBalance: ${serverUpdated} < ${currentStats.serverUpdatedAt}")
                    _networkState.value = NetworkState.Success
                    return adjustedBalance
                }
            }

            val updatedStats = balanceResponse.updateEntity(currentStats, uid).copy(krishnaCoins = adjustedBalance)
            userStatsDao.insertStats(updatedStats)

            // Sync daily UI trackers from server (server is source of truth)
            if (balanceResponse.checkin_day > 0) {
                DailyRewardsTracker.getInstance(appContext).syncWithServer(balanceResponse.checkin_day, balanceResponse.checkin_week, balanceResponse.last_checkin)
            }
            if (balanceResponse.share_day > 0) {
                DailyRewardsTracker.getInstance(appContext).syncShareWithServer(balanceResponse.share_day, balanceResponse.share_week, balanceResponse.last_share)
            }
            
            _networkState.value = NetworkState.Success
            adjustedBalance
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to get balance: ${e.message}")
            _networkState.value = NetworkState.Error("balance", e.message ?: "Network error")
            coinBalance.value
        }
    }

    suspend fun syncStatsToServer() {
        if (authPrefs.isGuestUser) return
        val uid = userId() ?: return
        val token = authPrefs.token ?: return
        
        val localStats = userStatsDao.getUserStatsOnce() ?: return
        try {
            val response = CoinApi.retrofitService.syncUserStats(
                token = "Bearer $token",
                request = com.aipoweredgita.app.network.UserStatsSyncRequest(
                    user_id = uid,
                    current_streak = localStats.currentStreak,
                    longest_streak = localStats.longestStreak,
                    total_quizzes_taken = localStats.totalQuizzesTaken,
                    total_questions_answered = localStats.totalQuestionsAnswered,
                    total_correct_answers = localStats.totalCorrectAnswers,
                    verses_read = localStats.versesRead,
                    chapters_completed = localStats.chaptersCompleted,
                    last_activity_date = localStats.lastActiveDate
                )
            )
            if (response.success && response.stats != null) {
                userStatsDao.syncRemoteStats(
                    currentStreak = response.stats.current_streak,
                    longestStreak = response.stats.longest_streak,
                    totalQuizzesTaken = response.stats.total_quizzes_taken,
                    totalQuestionsAnswered = response.stats.total_questions_answered,
                    versesRead = response.stats.verses_read,
                    chaptersCompleted = response.stats.chapters_completed,
                    daysActive = localStats.daysActive,
                    lastActiveDate = response.stats.last_activity_date.ifEmpty { localStats.lastActiveDate }
                )
            }
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync stats to server: ${e.message}")
            // Queue for retry via PendingSyncEvent
            try {
                val payloadMap = mapOf(
                    "current_streak" to localStats.currentStreak,
                    "longest_streak" to localStats.longestStreak,
                    "total_quizzes_taken" to localStats.totalQuizzesTaken,
                    "total_questions_answered" to localStats.totalQuestionsAnswered,
                    "verses_read" to localStats.versesRead,
                    "chapters_completed" to localStats.chaptersCompleted,
                    "last_activity_date" to localStats.lastActiveDate
                )
                val payloadString = Gson().toJson(payloadMap)
                pendingSyncEventDao.insert(
                    PendingSyncEvent(
                        userId = uid,
                        eventType = "STATS_SYNC",
                        payload = payloadString,
                        coinsToAdjust = 0,
                        idempotencyKey = "stats_sync_${uid}_${System.currentTimeMillis()}"
                    )
                )
                SyncWorker.schedule(appContext)
            } catch (dbEx: Exception) {
                Log.e("StatsRepository", "Failed to queue stats sync event: ${dbEx.message}")
            }
        }
    }

    suspend fun spendCoins(question: String): Boolean {
        val isGuest = authPrefs.isGuestUser
        // Use question hash as idempotency key to prevent duplicate spends
        val idempotencyKey = "spend_${userId() ?: "guest"}_${question.hashCode()}"

        if (isGuest) {
            // Dynamic pricing based on question length (matches backend voice_chat_rules)
            val cost = when {
                question.length <= 50 -> 2   // Short
                question.length <= 150 -> 3  // Medium
                else -> 5                    // Long
            }
            
            if (coinBalance.value < cost) {
                Log.w("StatsRepository", "Insufficient coins: ${coinBalance.value} < $cost")
                return false
            }
            userStatsDao.addKrishnaCoins(-cost)
            CoinTransactionLogger.log(appContext, -cost, "Asked question: $question")
            Log.d("StatsRepository", "Guest spend: -$cost coins (${question.length} chars)")
            return true
        }

        ensureUserSynced()
        val uid = userId() ?: return false
        _networkState.value = NetworkState.Loading("spend")
        
        Log.d("StatsRepository", "Attempting to spend coins for user: $uid, question: ${question.take(50)}...")
        
        try {
            val response = CoinApi.retrofitService.spendCoins(
                CoinSpendRequest(uid, question, idempotencyKey)
            )
            
            if (response.duplicate == true) {
                Log.w("StatsRepository", "Duplicate spend detected, server remaining: ${response.remaining_balance}")
                userStatsDao.updateKrishnaCoins(response.remaining_balance)
                _networkState.value = NetworkState.Success
                return true
            }
            
            Log.d("StatsRepository", "Spend successful: ${response.spent} coins, remaining: ${response.remaining_balance}")
            userStatsDao.updateKrishnaCoins(response.remaining_balance)
            _networkState.value = NetworkState.Success
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to spend coins: ${e.message}")
            _networkState.value = NetworkState.Error("spend", e.message ?: "Network error")

            // FIX: use same pricing as backend voice_chat_rules (not hardcoded -10)
            val offlineCost = when {
                question.length <= 50  -> 2
                question.length <= 150 -> 3
                else                   -> 5
            }

            if (coinBalance.value < offlineCost) {
                Log.w("StatsRepository", "Insufficient coins: ${coinBalance.value} < $offlineCost")
                return false
            }

            userStatsDao.addKrishnaCoins(-offlineCost)
            CoinTransactionLogger.log(appContext, -offlineCost, "Asked question: $question (offline)")
            Log.d("StatsRepository", "Deducted $offlineCost coins locally (offline), queuing for sync")

            try {
                val payloadMap   = mapOf("question" to question)
                val payloadString = Gson().toJson(payloadMap)
                val event = PendingSyncEvent(
                    userId           = uid,
                    eventType        = "SPEND",
                    payload          = payloadString,
                    coinsToAdjust    = -offlineCost,   // FIX: was -10
                    idempotencyKey   = idempotencyKey
                )
                pendingSyncEventDao.insert(event)
                SyncWorker.schedule(appContext)
            } catch (dbEx: Exception) {
                Log.e("StatsRepository", "Failed to queue spend sync event: ${dbEx.message}", dbEx)
            }
        }
        return true
    }
}