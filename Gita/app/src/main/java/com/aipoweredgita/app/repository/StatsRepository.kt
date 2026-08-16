package com.aipoweredgita.app.repository

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.coin.CoinRewardEngine
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.coin.CoinTransactionLogger
import com.aipoweredgita.app.coin.VoiceCoinPricing
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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.room.withTransaction
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
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

    /** Balance network TTL — skip resume/MainScreen spam within window. */
    @Volatile private var lastBalanceFetchMs: Long = 0L
    @Volatile private var lastBalanceUid: String? = null

    companion object {
        private const val BALANCE_TTL_MS = 10 * 60 * 1000L // 10 minutes
    }

    val coinBalance: StateFlow<Int> = userStatsDao.getUserStats()
        .map { it?.krishnaCoins ?: 0 }
        .stateIn(coroutineScope, SharingStarted.Eagerly, 0)

    /** Call after earn/spend/login so next pull is not TTL-skipped. */
    fun invalidateBalanceCache() {
        lastBalanceFetchMs = 0L
        lastBalanceUid = null
    }

    fun cancel() {
        coroutineScope.cancel()
    }

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
            daysActive = maxOf(current.daysActive, days_active),
            currentStreak = maxOf(current.currentStreak, current_streak),
            longestStreak = maxOf(current.longestStreak, longest_streak),
            totalQuizzesTaken = maxOf(current.totalQuizzesTaken, total_quizzes_taken),
            totalQuestionsAnswered = maxOf(current.totalQuestionsAnswered, total_questions_answered),
            totalCorrectAnswers = maxOf(current.totalCorrectAnswers, total_correct_answers),
            bestScore = maxOf(current.bestScore, best_score),
            bestScoreOutOf = maxOf(current.bestScoreOutOf, best_score_out_of),
            versesRead = maxOf(current.versesRead, verses_read),
            chaptersCompleted = maxOf(current.chaptersCompleted, chapters_completed),
            lastActiveDate = if (!last_activity_date.isNullOrEmpty()) last_activity_date else current.lastActiveDate,
            serverUpdatedAt = updated_at ?: current.serverUpdatedAt
        )
    }

    /**
     * Unified sync function that acts as the single source of truth for pulling state from the server.
     * Uses a staleness guard (serverUpdatedAt) to prevent slow background syncs from clobbering fresh optimistic updates.
     * @param force when false, skips network if a successful pull ran within [BALANCE_TTL_MS] for same user.
     */
    suspend fun refreshUserState(uid: String, force: Boolean = false) {
        val token = authPrefs.token ?: return
        val now = System.currentTimeMillis()
        if (!force &&
            uid == lastBalanceUid &&
            lastBalanceFetchMs > 0L &&
            now - lastBalanceFetchMs < BALANCE_TTL_MS
        ) {
            Log.d("Sync", "refreshUserState skipped (TTL ${now - lastBalanceFetchMs}ms)")
            return
        }
        try {
            val balance = CoinApi.retrofitService.getBalance(uid, "Bearer $token")
            var currentStats = userStatsDao.getUserStatsOnce() ?: com.aipoweredgita.app.database.UserStats(id = 1, userId = uid)

            // If user changed or user_stats has different userId, update userId and reset staleness guard
            if (currentStats.userId != uid) {
                userStatsDao.updateUserId(uid)
                currentStats = currentStats.copy(userId = uid, serverUpdatedAt = "")
            }

            // Server timestamp guard against stale overwrites of coin stats.
            // NEVER skip when force=true (login/relogin): we just wiped RewardState and must
            // re-apply check-in / share strip from the server or the UI shows "unclicked".
            val serverUpdated = balance.updated_at
            if (!force &&
                serverUpdated != null &&
                serverUpdated.isNotEmpty() &&
                currentStats.serverUpdatedAt.isNotEmpty()
            ) {
                if (serverUpdated < currentStats.serverUpdatedAt) {
                    Log.w("Sync", "Skipping stale server data: ${serverUpdated} < ${currentStats.serverUpdatedAt}")
                    return
                }
            }

            // Map and save entity
            val updatedStats = balance.updateEntity(currentStats, uid)
            userStatsDao.insertStats(updatedStats) // Upsert

            // Sync daily UI trackers from server (day 0 = never checked in → day 1 clickable)
            val tracker = DailyRewardsTracker.getInstance(appContext)
            val lastCheckin = balance.last_checkin
            val lastShare = balance.last_share
            Log.d(
                "Sync",
                "checkin sync force=$force day=${balance.checkin_day} week=${balance.checkin_week} " +
                    "last_checkin=$lastCheckin last_share=$lastShare"
            )
            tracker.syncWithServer(
                balance.checkin_day,
                balance.checkin_week,
                lastCheckin,
                force = force
            )
            tracker.syncShareWithServer(
                balance.share_day,
                balance.share_week,
                lastShare,
                force = force
            )

            lastBalanceFetchMs = System.currentTimeMillis()
            lastBalanceUid = uid
            _networkState.value = NetworkState.Success
        } catch (e: Exception) {
            Log.e("Sync", "Failed refreshUserState: ${e.message}")
            _networkState.value = NetworkState.Error("sync", e.message ?: "Sync failed")
        }
    }

    /** Ensures the user exists on the server before any coin API call. */
    private suspend fun ensureUserSynced() {
        val uid = resolvedUserId() ?: return
        if (authPrefs.isGuestUser) {
            return
        }
        refreshUserState(uid, force = true)
        syncUserWithCloud()
    }

    /** Local-DB cached userId — kept for offline/legacy lookups. Prefer resolvedUserId() for gating logic. */
    private suspend fun userId(): String? = userStatsDao.getUserStatsOnce()?.userId?.takeIf { it.isNotEmpty() }

    /**
     * The real signed-in identity. Prefers the auth session (available immediately at login,
     * no DB round-trip needed) over the local `user_stats.userId` cache, which can be stale
     * right after login/guest-claim and must never gate whether a reward is awarded or logged.
     */
    private fun resolvedUserId(): String? = authPrefs.userId?.takeIf { it.isNotEmpty() }

    suspend fun trackQuizCompletion(
        score: Int,
        totalQuestions: Int,
        segmentCorrectMap: Map<String, Int> = emptyMap(),
        quizType: String = "general",
        timeSpentSeconds: Long = 0,
        attemptId: String? = null,
        language: String = "en"
    ): Pair<Int, String> {
        // Read local stats BEFORE server sync so the coin multiplier is
        // based on what the user actually sees in the UI (not inflated server data).
        val preStats = userStatsDao.getUserStatsOnce()
        val multiplier = YogaLevelManager.getCoinMultiplier(preStats)

        ensureUserSynced()
        userStatsDao.incrementQuizzesTaken()
        userStatsDao.addQuestionsAnswered(totalQuestions)
        userStatsDao.addCorrectAnswers(score)

        // Track quiz time in daily activity
        val today = DailyRewardsTracker.getInstance(appContext).nowLocal()
        dailyActivityDao?.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
        dailyActivityDao?.addQuizSeconds(today, if (timeSpentSeconds > 0) timeSpentSeconds else 60)

        // Track quiz mode time in user stats for overview time distribution
        if (timeSpentSeconds > 0) {
            userStatsDao.addQuizModeTime(timeSpentSeconds)
        }

        // Update streak BEFORE coin calculation so reward reflects current streak
        updateStreak()

        // Re-read stats to get the updated streak (but keep pre-sync multiplier)
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
        val checkinDay = try {
            DailyRewardsTracker.getInstance(appContext).getCurrentCheckinDay()
        } catch (e: Throwable) {
            1
        }

        // Same rules as server: base 5 + accuracy, cap 15, then × yoga (1/2/2/3/3), round
        val result = CoinRewardEngine.calculate(
            CoinRewardEngine.Input(
                score = score,
                totalQuestions = totalQuestions,
                segmentCorrectMap = segmentCorrectMap,
                currentStreakDays = currentStreak,
                dailyCheckinDay = checkinDay,
                yogaMultiplier = multiplier
            )
        )

        Log.d("CoinsCalc", "trackQuizCompletion: score=$score, totalQ=$totalQuestions, streak=$currentStreak, checkin=$checkinDay, multiplier=$multiplier, calculated=${result.totalCoins}, breakdown=${result.breakdown}")

        val isGuest = authPrefs.isGuestUser

        val coins = if (isGuest) {
            // Guest: local balance + local history + queue for server sync
            if (result.totalCoins > 0) {
                userStatsDao.addKrishnaCoins(result.totalCoins)
                val guestUid = resolvedUserId() ?: userId() ?: authPrefs.userId
                CoinTransactionLogger.log(
                    appContext,
                    result.totalCoins,
                    result.breakdown,
                    source = "quiz_completion",
                    userId = guestUid
                )
                // Queue sync event so SyncWorker sends to server
                if (guestUid != null) {
                    try {
                        val payloadMap = mapOf(
                            "score" to score,
                            "totalQuestions" to totalQuestions,
                            "accuracy" to accuracy,
                            "streakDays" to currentStreak,
                            "checkinDay" to checkinDay,
                            "quizType" to quizType,
                            "timeSpentSeconds" to timeSpentSeconds,
                            "clientDate" to java.time.OffsetDateTime.now().toString(),
                            "countryCode" to java.util.Locale.getDefault().country,
                            "attemptId" to attemptId,
                            "language" to language
                        )
                        val payloadString = Gson().toJson(payloadMap)
                        val safeAttemptId = attemptId ?: java.util.UUID.randomUUID().toString()
                        pendingSyncEventDao.insert(
                            PendingSyncEvent(
                                userId = guestUid,
                                eventType = "QUIZ",
                                payload = payloadString,
                                coinsToAdjust = result.totalCoins,
                                idempotencyKey = "quiz_${guestUid}_$safeAttemptId"
                            )
                        )
                        SyncWorker.schedule(appContext)
                    } catch (dbEx: Exception) {
                        Log.e("StatsRepository", "Failed to queue guest quiz sync: ${dbEx.message}")
                    }
                }
            }
            result.totalCoins
        } else {
            // Optimistic amount matches server formula; SyncWorker still sets total_coins from server
            val fallback = result.totalCoins
            userStatsDao.addKrishnaCoins(fallback)

            // Resolve the real uid for the sync queue only — prefer the auth
            // session over the local cached row, which may not be populated yet.
            val uid = resolvedUserId() ?: userId()
            if (uid != null) {
                try {
                    val safeAttemptId = attemptId ?: java.util.UUID.randomUUID().toString()
                    val payloadMap = mapOf(
                        "score" to score,
                        "totalQuestions" to totalQuestions,
                        "accuracy" to accuracy,
                        "streakDays" to currentStreak,
                        "checkinDay" to checkinDay,
                        "quizType" to quizType,
                        "timeSpentSeconds" to timeSpentSeconds,
                        "clientDate" to java.time.OffsetDateTime.now().toString(),
                        "countryCode" to java.util.Locale.getDefault().country,
                        "attemptId" to safeAttemptId,
                        "language" to language
                    )
                    val payloadString = Gson().toJson(payloadMap)
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = uid,
                            eventType = "QUIZ",
                            payload = payloadString,
                            coinsToAdjust = fallback,
                            idempotencyKey = "quiz_${uid}_$safeAttemptId"
                        )
                    )
                    SyncWorker.schedule(appContext)
                } catch (dbEx: Exception) {
                    Log.e("StatsRepository", "Failed to queue quiz sync event: ${dbEx.message}")
                }
            } else {
                Log.w("StatsRepository", "trackQuizCompletion: no uid available, coin awarded locally but server sync not queued")
            }
            fallback
        }

        return Pair(coins, result.breakdown)
    }

    suspend fun trackBattleCompletion(battleCoins: Int, score: Int, questionsAnswered: Int, language: String = "en") {
        ensureUserSynced()
        userStatsDao.incrementQuizzesTaken()
        userStatsDao.addQuestionsAnswered(questionsAnswered)
        userStatsDao.addCorrectAnswers(score)
        
        userStatsDao.addQuizModeTime(60)

        val preStats = userStatsDao.getUserStatsOnce()
        val yogaMult = YogaLevelManager.getCoinMultiplier(preStats)
        // Same as server: fib(correct) × yoga (ignore client battleCoins for final amount)
        val serverMatchedCoins = CoinRewardEngine.battleTotal(score, yogaMult)

        preStats?.let {
            val currentBestPercentage = if (it.bestScoreOutOf > 0)
                (it.bestScore.toFloat() / it.bestScoreOutOf) * 100
            else 0f
            val newPercentage = if (questionsAnswered > 0) (score.toFloat() / questionsAnswered) * 100 else 0f
            if (newPercentage > currentBestPercentage)
                userStatsDao.updateBestScore(score, questionsAnswered)
        }

        val db = GitaDatabase.getDatabase(appContext)
        val battleAttempt = com.aipoweredgita.app.database.QuizAttempt(
            score = score,
            totalQuestions = questionsAnswered,
            coinsEarned = serverMatchedCoins,
            quizType = "battle_quiz",
            timeSpentSeconds = 60,
            language = language
        )
        db.quizAttemptDao().insertAttempt(battleAttempt)

        val today = LocalDate.now().toString()
        dailyActivityDao?.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
        dailyActivityDao?.addQuizSeconds(today, 60)

        updateStreak()

        val isGuest = authPrefs.isGuestUser

        if (isGuest) {
            if (serverMatchedCoins > 0) {
                userStatsDao.addKrishnaCoins(serverMatchedCoins)
                val guestUid = resolvedUserId() ?: userId() ?: authPrefs.userId
                CoinTransactionLogger.log(
                    appContext,
                    serverMatchedCoins,
                    "BQ",
                    source = "battle_quiz",
                    userId = guestUid
                )
                // Queue sync event for guest battle
                if (guestUid != null) {
                    try {
                        val fibBase = CoinRewardEngine.battleFibCoins(score)
                        val db2 = com.aipoweredgita.app.database.GitaDatabase.getDatabase(appContext)
                        db2.pendingSyncEventDao().insert(
                            com.aipoweredgita.app.database.PendingSyncEvent(
                                userId = guestUid,
                                eventType = "BATTLE",
                                payload = """{"battleCoins":$fibBase,"score":$score,"questionsAnswered":$questionsAnswered,"clientDate":"${java.time.OffsetDateTime.now()}","countryCode":"${java.util.Locale.getDefault().country}","attemptId":"${battleAttempt.attemptId}","language":"${battleAttempt.language}"}""",
                                coinsToAdjust = serverMatchedCoins,
                                idempotencyKey = "battle_${guestUid}_${battleAttempt.attemptId}"
                            )
                        )
                        SyncWorker.schedule(appContext)
                    } catch (dbEx: Exception) {
                        Log.e("StatsRepository", "Failed to queue guest battle sync: ${dbEx.message}")
                    }
                }
            }
        } else {
            if (serverMatchedCoins > 0 || score > 0) {
                if (serverMatchedCoins > 0) {
                    userStatsDao.addKrishnaCoins(serverMatchedCoins)
                }
                val uid = resolvedUserId() ?: userId()
                if (uid != null) {
                    try {
                        val fibBase = CoinRewardEngine.battleFibCoins(score)
                        val db2 = com.aipoweredgita.app.database.GitaDatabase.getDatabase(appContext)
                        db2.pendingSyncEventDao().insert(
                            com.aipoweredgita.app.database.PendingSyncEvent(
                                userId = uid,
                                eventType = "BATTLE",
                                payload = """{"battleCoins":$fibBase,"score":$score,"questionsAnswered":$questionsAnswered,"clientDate":"${java.time.OffsetDateTime.now()}","countryCode":"${java.util.Locale.getDefault().country}","attemptId":"${battleAttempt.attemptId}","language":"${battleAttempt.language}"}""",
                                coinsToAdjust = serverMatchedCoins,
                                idempotencyKey = "battle_${uid}_${battleAttempt.attemptId}"
                            )
                        )
                        SyncWorker.schedule(appContext)
                    } catch (dbEx: Exception) {
                        Log.e("StatsRepository", "Failed to queue battle sync event: ${dbEx.message}")
                    }
                } else {
                    Log.w("StatsRepository", "trackBattleCompletion: no uid available, coin awarded locally but server sync not queued")
                }
            }
        }
    }

    suspend fun trackVerseRead() {
        userStatsDao.incrementVersesRead()
        val today = DailyRewardsTracker.getInstance(appContext).nowLocal()
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
        val today = DailyRewardsTracker.getInstance(appContext).nowLocal()
        dailyActivityDao?.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
        when (mode) {
            ModeType.NORMAL -> dailyActivityDao?.addNormalSeconds(today, seconds)
            ModeType.QUIZ -> dailyActivityDao?.addQuizSeconds(today, seconds)
            ModeType.VOICE -> dailyActivityDao?.addVoiceStudioSeconds(today, seconds)
        }

        updateStreak()
    }

    private fun parseLocalDate(rawDateString: String): LocalDate? {
        if (rawDateString.isBlank()) return null
        return try {
            if (rawDateString.contains("T")) {
                Instant.parse(rawDateString).atZone(ZoneId.systemDefault()).toLocalDate()
            } else {
                LocalDate.parse(rawDateString.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
            }
        } catch (e: Exception) {
            try {
                LocalDate.parse(rawDateString.take(10), DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (ex: Exception) {
                null
            }
        }
    }

    private suspend fun runInTransaction(block: suspend () -> Unit) {
        try {
            val db = GitaDatabase.getDatabase(appContext)
            db.withTransaction { block() }
        } catch (e: Throwable) {
            block()
        }
    }

    suspend fun checkPassiveStreakReset() {
        runInTransaction {
            val currentStats = userStatsDao.getUserStatsOnce() ?: return@runInTransaction
            val rawLastActiveDate = currentStats.lastActiveDate
            val lastActiveLocalDate = parseLocalDate(rawLastActiveDate) ?: return@runInTransaction

            val today = LocalDate.now(ZoneId.systemDefault())

            // Allow 48-hour grace window (today.minusDays(2)) so late night / timezone shifts don't wipe streaks
            if (lastActiveLocalDate < today.minusDays(2)) {
                if (currentStats.currentStreak > 0) {
                    userStatsDao.updateCurrentStreak(0)
                }
            }
        }
    }

    private suspend fun updateStreak() {
        runInTransaction {
            val currentStats = userStatsDao.getUserStatsOnce() ?: return@runInTransaction
            val today = LocalDate.now(ZoneId.systemDefault())
            val todayStr = today.toString()
            val rawLastActiveDate = currentStats.lastActiveDate
            val lastActiveLocalDate = parseLocalDate(rawLastActiveDate)

            when {
                lastActiveLocalDate == null -> {
                    val streakVal = maxOf(1, currentStats.currentStreak)
                    userStatsDao.updateCurrentStreak(streakVal)
                    userStatsDao.updateLongestStreak(maxOf(streakVal, currentStats.longestStreak))
                    userStatsDao.updateDaysActive(maxOf(1, currentStats.daysActive))
                }
                lastActiveLocalDate == today -> {
                    if (currentStats.currentStreak == 0) {
                        userStatsDao.updateCurrentStreak(1)
                        userStatsDao.updateLongestStreak(maxOf(1, currentStats.longestStreak))
                    }
                }
                lastActiveLocalDate == today.minusDays(1) || lastActiveLocalDate == today.minusDays(2) -> {
                    // Continued streak (yesterday or 2-day timezone grace window)
                    val baseStreak = if (currentStats.currentStreak == 0) 1 else currentStats.currentStreak
                    val newStreak = baseStreak + 1
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

            userStatsDao.updateLastActive(System.currentTimeMillis(), todayStr)
        }

        val currentStats = userStatsDao.getUserStatsOnce() ?: return
        try {
            val payloadString = "{}"
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

        if (!isGuest) {
            ensureUserSynced()
        }

        val tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(appContext)
        val dailyState = tracker.getShareState()
        
        // Prevent duplicate share processing if already shared today
        if (dailyState.todayClaimed) {
            return 0
        }

        // Claim locally first to mark as claimed today
        var fallbackCoins = tracker.claimShare()
        if (fallbackCoins <= 0) return 0

        var isWeeklyBonus = false
        if (dailyState.day == 7) {
            val weeklyReward = tracker.getShareWeeklyState().reward
            tracker.claimShareDay7BonusIfEligible()
            fallbackCoins += weeklyReward
            isWeeklyBonus = true
        }

        val coinsAwarded = if (isGuest) {
            userStatsDao.addKrishnaCoins(fallbackCoins)
            val guestUid = resolvedUserId() ?: authPrefs.userId
            if (isWeeklyBonus) {
                CoinTransactionLogger.log(appContext, fallbackCoins - 10, "Daily sloka share (guest)", source = "share_daily", userId = guestUid)
                CoinTransactionLogger.log(appContext, 10, "7-day share bonus (guest)", source = "share_day7_bonus", userId = guestUid)
            } else {
                CoinTransactionLogger.log(appContext, fallbackCoins, "Daily sloka share (guest)", source = "share_daily", userId = guestUid)
            }
            // Queue sync event for guest share
            if (guestUid != null) {
                try {
                    val slokaId = if (chapter != null && verse != null) "ch${chapter}v${verse}" else null
                    val payloadMap = mapOf(
                        "chapter" to chapter,
                        "verse" to verse,
                        "slokaId" to slokaId,
                        "clientDate" to tracker.nowLocal(),
                        "countryCode" to java.util.Locale.getDefault().country,
                        "isWeeklyBonus" to isWeeklyBonus
                    )
                    val payloadString = Gson().toJson(payloadMap)
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = guestUid,
                            eventType = "SHARE",
                            payload = payloadString,
                            coinsToAdjust = fallbackCoins,
                            // Date-based key, matching server's own fallback convention
                            // (share_${user_id}_${today}) — lets the server's idempotency
                            // check actually dedupe retried/duplicate sync events instead
                            // of relying solely on the last_share date race-prone check.
                            idempotencyKey = "share_${guestUid}_${tracker.nowLocal()}"
                        )
                    )
                    SyncWorker.schedule(appContext)
                } catch (dbEx: Exception) {
                    Log.e("StatsRepository", "Failed to queue guest share sync: ${dbEx.message}")
                }
            }
            fallbackCoins
        } else {
            // Always award + log for a signed-in user. Don't gate on the local
            // DB's cached userId — it can be stale right after login.
            userStatsDao.addKrishnaCoins(fallbackCoins)
            if (isWeeklyBonus) {
                CoinTransactionLogger.log(appContext, fallbackCoins - 10, "Daily sloka share", source = "share_daily")
                CoinTransactionLogger.log(appContext, 10, "7-day share bonus", source = "share_day7_bonus")
            } else {
                CoinTransactionLogger.log(appContext, fallbackCoins, "Daily sloka share", source = "share_daily")
            }
            tracker.isShareSynced = true

            // Resolve the real uid for the sync queue only — prefer the auth
            // session over the local cached row, which may not be populated yet.
            val uid = resolvedUserId() ?: userId()
            if (uid != null) {
                try {
                    val slokaId = if (chapter != null && verse != null) "ch${chapter}v${verse}" else null
                    val payloadMap = mapOf(
                        "chapter" to chapter,
                        "verse" to verse,
                        "slokaId" to slokaId,
                        "clientDate" to tracker.nowLocal(),
                        "countryCode" to java.util.Locale.getDefault().country,
                        "isWeeklyBonus" to isWeeklyBonus
                    )
                    val payloadString = Gson().toJson(payloadMap)
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = uid,
                            eventType = "SHARE",
                            payload = payloadString,
                            coinsToAdjust = fallbackCoins,
                            // Date-based key, matching server's own fallback convention
                            // (share_${user_id}_${today}) — lets the server's idempotency
                            // check actually dedupe retried/duplicate sync events instead
                            // of relying solely on the last_share date race-prone check.
                            idempotencyKey = "share_${uid}_${tracker.nowLocal()}"
                        )
                    )
                    SyncWorker.schedule(appContext)
                } catch (dbEx: Exception) {
                    Log.e("StatsRepository", "Failed to queue share sync event: ${dbEx.message}")
                }
            } else {
                Log.w("StatsRepository", "trackSlokaShared: no uid available, coin awarded locally but server sync not queued")
            }
            fallbackCoins
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
        val yogaMult = YogaLevelManager.getCoinMultiplier(userStatsDao.getUserStatsOnce())
        val chapterCoins = CoinRewardEngine.chapterTotal(yogaMult)

        if (isGuest) {
            userStatsDao.addKrishnaCoins(chapterCoins)
            val guestUid = resolvedUserId() ?: userId() ?: authPrefs.userId
            CoinTransactionLogger.log(
                appContext,
                chapterCoins,
                "Chapter $chapterNo Completion",
                source = "chapter_completion",
                userId = guestUid
            )
            // Queue sync event for guest chapter completion
            if (guestUid != null) {
                try {
                    val payloadMap = mapOf(
                        "chapterNo" to chapterNo,
                        "clientDate" to java.time.OffsetDateTime.now().toString(),
                        "countryCode" to java.util.Locale.getDefault().country
                    )
                    val payloadString = Gson().toJson(payloadMap)
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = guestUid,
                            eventType = "CHAPTER",
                            payload = payloadString,
                            coinsToAdjust = chapterCoins,
                            idempotencyKey = "chapter_${guestUid}_${chapterNo}_${java.time.LocalDate.now()}"
                        )
                    )
                    SyncWorker.schedule(appContext)
                } catch (dbEx: Exception) {
                    Log.e("StatsRepository", "Failed to queue guest chapter sync: ${dbEx.message}")
                }
            }
        } else {
            ensureUserSynced()

            // Same as server: 15 × yoga
            userStatsDao.addKrishnaCoins(chapterCoins)

            val uid = resolvedUserId() ?: userId()
            if (uid != null) {
                try {
                    val payloadMap = mapOf(
                        "chapterNo" to chapterNo,
                        "clientDate" to java.time.OffsetDateTime.now().toString(),
                        "countryCode" to java.util.Locale.getDefault().country
                    )
                    val payloadString = Gson().toJson(payloadMap)
                    pendingSyncEventDao.insert(
                        PendingSyncEvent(
                            userId = uid,
                            eventType = "CHAPTER",
                            payload = payloadString,
                            coinsToAdjust = chapterCoins,
                            idempotencyKey = "chapter_${uid}_${chapterNo}_${java.time.LocalDate.now()}"
                        )
                    )
                    SyncWorker.schedule(appContext)
                } catch (dbEx: Exception) {
                    Log.e("StatsRepository", "Failed to queue chapter sync event: ${dbEx.message}")
                }
            } else {
                Log.w("StatsRepository", "trackChapterCompleted: no uid available, coin awarded locally but server sync not queued")
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
                // Do NOT replace local guest_* id with a server guest id — that orphans
                // SharedPreferences history under tx_<oldId> and empties Coin History UI.
                Log.d("StatsRepository", "Guest local-only; skip createGuest id swap (uid=$uid)")
            } else {
                val response = CoinApi.retrofitService.createUser(CreateUserRequest(uid, stats.userName.ifEmpty { "Gita Seeker" }, ""))
                if (response.token != null && authPrefs.token == null) {
                    authPrefs.saveLoginState(userId = uid, loginMethod = "device", token = response.token)
                    Log.d("StatsRepository", "Upgraded old user with new backend session token")
                }
                Log.d("StatsRepository", "User successfully synced with cloud.")
            }
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to sync user with cloud: ${e.message}")
        }
    }

    /** Sync a locally-recorded check-in to the cloud (safe to call even if already synced). */
    suspend fun syncCheckinToCloud(coinsToAdjust: Int = 0) {
        ensureUserSynced()
        val uid = resolvedUserId() ?: userId() ?: return
        val token = authPrefs.token
        if (token.isNullOrEmpty()) {
            Log.w("StatsRepository", "Checkin sync skipped — no auth token; queueing")
            queueCheckinSync(uid, coinsToAdjust)
            return
        }
        try {
            val localDate = DailyRewardsTracker.getInstance(appContext).nowLocal()
            val response = CoinApi.retrofitService.checkin(
                mapOf(
                    "user_id" to uid,
                    "client_date" to localDate,
                    "timezone" to java.util.TimeZone.getDefault().id
                ),
                "Bearer $token"
            )
            if (response.day > 0) {
                DailyRewardsTracker.getInstance(appContext).syncWithServer(response.day, response.week, localDate)
            }
            refreshUserState(uid, force = true)
            DailyRewardsTracker.getInstance(appContext).isCheckinSynced = true
        } catch (e: retrofit2.HttpException) {
            // 401/403 means the server never recorded this check-in — do NOT mark synced.
            if (e.code() == 401 || e.code() == 403) {
                Log.e("StatsRepository", "Checkin sync unauthorized (HTTP ${e.code()}) — will retry")
                queueCheckinSync(uid, coinsToAdjust)
            } else {
                Log.d("StatsRepository", "Checkin sync: HTTP ${e.code()} - marking as synced")
                DailyRewardsTracker.getInstance(appContext).isCheckinSynced = true
            }
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
                        // Date-based, same reasoning as the share sync events — matches
                        // server's own checkin_${user_id}_${today} fallback convention.
                        idempotencyKey = "checkin_${uid}_${DailyRewardsTracker.getInstance(appContext).nowLocal()}"
                    )
                )
                if (finalCoins != 0) userStatsDao.addKrishnaCoins(finalCoins)
                SyncWorker.schedule(appContext)
            }
        } catch (dbEx: Exception) {
            Log.e("StatsRepository", "Failed to queue checkin sync: ${dbEx.message}")
        }
    }

    /** Sync a locally-recorded share to the cloud (safe to call even if already synced). */
    suspend fun syncShareToCloud(coinsToAdjust: Int = 0) {
        ensureUserSynced()
        val uid = resolvedUserId() ?: userId() ?: return
        val token = authPrefs.token
        if (token.isNullOrEmpty()) {
            Log.w("StatsRepository", "Share sync skipped — no auth token; queueing")
            queueShareSync(uid, coinsToAdjust)
            return
        }
        try {
            val localDate = DailyRewardsTracker.getInstance(appContext).nowLocal()
            val response = CoinApi.retrofitService.share(
                ShareSlokaRequest(uid, "local_sync", client_date = localDate),
                "Bearer $token"
            )
            if (response.share_day > 0) {
                DailyRewardsTracker.getInstance(appContext).syncShareWithServer(response.share_day, response.share_week, localDate)
            }
            refreshUserState(uid, force = true)
            DailyRewardsTracker.getInstance(appContext).isShareSynced = true
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401 || e.code() == 403) {
                Log.e("StatsRepository", "Share sync unauthorized (HTTP ${e.code()}) — will retry")
                queueShareSync(uid, coinsToAdjust)
            } else {
                Log.d("StatsRepository", "Share sync: HTTP ${e.code()} - marking as synced")
                DailyRewardsTracker.getInstance(appContext).isShareSynced = true
            }
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
                        // Date-based, same reasoning as the direct-claim sync events above.
                        idempotencyKey = "share_${uid}_${DailyRewardsTracker.getInstance(appContext).nowLocal()}"
                    )
                )
                if (finalCoins != 0) userStatsDao.addKrishnaCoins(finalCoins)
                SyncWorker.schedule(appContext)
            }
        } catch (dbEx: Exception) {
            Log.e("StatsRepository", "Failed to queue share sync: ${dbEx.message}")
        }
    }

    suspend fun claimDailyReward(coins: Int, description: String) {
        userStatsDao.addKrishnaCoins(coins)
        val guestUid = if (authPrefs.isGuestUser) resolvedUserId() ?: userId() ?: authPrefs.userId else null
        val uid = guestUid ?: resolvedUserId() ?: userId()
        CoinTransactionLogger.log(appContext, coins, description, source = "checkin_daily", userId = uid)
        // Both guest and signed-in queue for sync
        queueCheckinSync(uid ?: return, coins)
    }

    /** Log a completed meditation session to the server. */
    suspend fun logMeditationSession(minutes: Int) {
        val uid = resolvedUserId() ?: userId() ?: return
        val token = authPrefs.token
        // Update local balance immediately (server formula: floor(min/5)*10, capped at 40)
        val localCoins = minOf(40, (minutes / 5) * 10)
        if (localCoins > 0) userStatsDao.addKrishnaCoins(localCoins)
        CoinTransactionLogger.log(
            appContext, localCoins,
            "Meditation $minutes mins",
            source = "meditation",
            userId = uid
        )
        // Queue for server sync
        try {
            val payloadStr = com.google.gson.Gson().toJson(mapOf("minutes" to minutes))
            pendingSyncEventDao.insert(
                PendingSyncEvent(
                    userId = uid,
                    eventType = "MEDITATION",
                    payload = payloadStr,
                    coinsToAdjust = localCoins,
                    idempotencyKey = "meditation_${uid}_${minutes}_${java.time.LocalDate.now()}"
                )
            )
            SyncWorker.schedule(appContext)
        } catch (e: Exception) {
            android.util.Log.e("StatsRepository", "Failed to queue meditation sync: ${e.message}")
        }
    }

    suspend fun claimShareReward(coins: Int, description: String) {
        userStatsDao.addKrishnaCoins(coins)
        val guestUid = if (authPrefs.isGuestUser) resolvedUserId() ?: userId() ?: authPrefs.userId else null
        val uid = guestUid ?: resolvedUserId() ?: userId()
        CoinTransactionLogger.log(appContext, coins, description, source = "share_daily", userId = uid)
        // Both guest and signed-in queue for sync
        queueShareSync(uid ?: return, coins)
    }

    suspend fun getBalance(force: Boolean = false): Int {
        if (authPrefs.isGuestUser) {
            // Welcome bonus once — balance + local guest history line
            if (!authPrefs.guestWelcomeAwarded) {
                userStatsDao.updateKrishnaCoins(50)
                authPrefs.guestWelcomeAwarded = true
                val guestUid = resolvedUserId() ?: userId() ?: authPrefs.userId
                if (!guestUid.isNullOrEmpty()) {
                    CoinTransactionLogger.log(
                        appContext,
                        50,
                        "Welcome bonus (guest)",
                        source = "signup",
                        userId = guestUid
                    )
                }
            } else {
                // Flag set but log empty (re-entry / race) — keep history usable
                val guestUid = resolvedUserId() ?: authPrefs.userId
                if (!guestUid.isNullOrEmpty() &&
                    CoinTransactionLogger.getHistory(appContext, guestUid).isEmpty()
                ) {
                    val bal = coinBalance.value.coerceAtLeast(50)
                    CoinTransactionLogger.log(
                        appContext,
                        50.coerceAtMost(bal),
                        "Welcome bonus (guest)",
                        source = "signup",
                        userId = guestUid
                    )
                }
            }
            return coinBalance.value
        }

        val uid = resolvedUserId() ?: userId() ?: return coinBalance.value
        val token = authPrefs.token ?: run {
            Log.w("StatsRepository", "No auth token — returning local balance")
            return coinBalance.value
        }

        val now = System.currentTimeMillis()
        if (!force &&
            uid == lastBalanceUid &&
            lastBalanceFetchMs > 0L &&
            now - lastBalanceFetchMs < BALANCE_TTL_MS
        ) {
            Log.d("StatsRepository", "getBalance skipped (TTL ${now - lastBalanceFetchMs}ms)")
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

            // Sync daily UI trackers (include day 0 so old accounts still get a claimable day 1)
            val tracker = DailyRewardsTracker.getInstance(appContext)
            tracker.syncWithServer(
                balanceResponse.checkin_day,
                balanceResponse.checkin_week,
                balanceResponse.last_checkin,
                force = force
            )
            tracker.syncShareWithServer(
                balanceResponse.share_day,
                balanceResponse.share_week,
                balanceResponse.last_share,
                force = force
            )

            lastBalanceFetchMs = System.currentTimeMillis()
            lastBalanceUid = uid
            _networkState.value = NetworkState.Success
            adjustedBalance
        } catch (e: Exception) {
            Log.e("StatsRepository", "Failed to get balance: ${e.message}")
            _networkState.value = NetworkState.Error("balance", e.message ?: "Network error")
            coinBalance.value
        }
    }

    suspend fun syncStatsToServer() {
        val uid = resolvedUserId() ?: userId() ?: return
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
                    krishna_coins = localStats.krishnaCoins,
                    yoga_level = YogaLevelManager.levelFor(localStats),
                    last_activity_date = localStats.lastActiveDate
                )
            )
            if (response.success && response.stats != null) {
                userStatsDao.syncRemoteStats(
                    currentStreak = response.stats.current_streak.coerceAtLeast(0),
                    longestStreak = response.stats.longest_streak.coerceAtLeast(0),
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
        val idempotencyKey = "spend_${resolvedUserId() ?: userId() ?: "guest"}_${question.hashCode()}"

        // Guest and signed-in: both queue for server sync
        val guestUid = if (isGuest) {
            authPrefs.userId?.ifEmpty { authPrefs.guestId } ?: authPrefs.guestId
        } else null
        val uid = guestUid ?: (resolvedUserId() ?: userId() ?: return false)

        // Dynamic pricing: Short 4 / Medium 6 / Long 10
        val cost = VoiceCoinPricing.costFor(question)
        
        if (coinBalance.value < cost) {
            Log.w("StatsRepository", "Insufficient coins: ${coinBalance.value} < $cost")
            return false
        }

        userStatsDao.addKrishnaCoins(-cost)
        CoinTransactionLogger.log(appContext, -cost, "Voice chat", source = "voice_chat", userId = uid)
        Log.d("StatsRepository", "Spend -$cost coins (${question.length} chars)")

        // Queue for server sync (works for both guest and signed-in)
        try {
            val payloadMap = mapOf(
                "question" to question,
                "clientDate" to java.time.OffsetDateTime.now().toString(),
                "countryCode" to java.util.Locale.getDefault().country
            )
            val payloadString = Gson().toJson(payloadMap)
            val event = PendingSyncEvent(
                userId         = uid,
                eventType      = "SPEND",
                payload        = payloadString,
                coinsToAdjust  = -cost,
                idempotencyKey = idempotencyKey
            )
            pendingSyncEventDao.insert(event)
            SyncWorker.schedule(appContext)
        } catch (dbEx: Exception) {
            Log.e("StatsRepository", "Failed to queue spend sync event: ${dbEx.message}", dbEx)
        }
        
        return true
    }
}
