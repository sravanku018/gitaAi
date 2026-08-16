package com.aipoweredgita.app.services

import android.content.Context
import android.util.Log
import androidx.work.*
import com.aipoweredgita.app.coin.DailyRewardsTracker
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.PendingSyncEvent
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.CoinAwardRequest
import com.aipoweredgita.app.network.CoinSpendRequest
import com.aipoweredgita.app.network.ShareSlokaRequest
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.utils.AuthPreferences
import com.google.gson.Gson
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncWorkerEntryPoint {
    fun statsRepository(): StatsRepository
}

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "SyncWorker"
        private const val WORK_NAME = "offline_coin_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
            Log.d(TAG, "Offline coin synchronization scheduled")
        }

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val periodicRequest = androidx.work.PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "${WORK_NAME}_periodic",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
            Log.d(TAG, "Periodic offline coin synchronization scheduled")
        }
    }

    override suspend fun doWork(): Result {
        val authPrefs = AuthPreferences.getInstance(applicationContext)
        val currentUserId = authPrefs.userId

        Log.d(TAG, "=== SyncWorker doWork START ===")
        Log.d(TAG, "isGuest: ${authPrefs.isGuestUser}, userId: $currentUserId, guestId: ${authPrefs.guestId}")

        if (currentUserId.isNullOrEmpty()) {
            Log.d(TAG, "No logged-in user or guest active, skipping background sync")
            return Result.success()
        }

        // Guest users now sync to server (coin_transactions filter removed on server)

        val database = GitaDatabase.getDatabase(applicationContext)
        val dao = database.pendingSyncEventDao()
        val userStatsDao = database.userStatsDao()

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, SyncWorkerEntryPoint::class.java
        )
        val statsRepository = entryPoint.statsRepository()

        val events = dao.getPendingEvents(currentUserId)

        if (events.isEmpty()) {
            Log.d(TAG, "No pending sync events found for user: $currentUserId")
            return Result.success()
        }

        val authToken = authPrefs.token
        if (authToken.isNullOrEmpty()) {
            Log.e(TAG, "No auth token — cannot sync protected coin endpoints; will retry later")
            return Result.retry()
        }
        val bearer = "Bearer $authToken"

        Log.d(TAG, "Found ${events.size} pending sync events for user: $currentUserId")
        val gson = Gson()

        for (event in events) {
            try {
                Log.d(TAG, "Processing event ID: ${event.id}, type: ${event.eventType}, idempotencyKey: ${event.idempotencyKey}")
                when (event.eventType) {
                    "QUIZ" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val score = jsonObject.get("score")?.asInt ?: 0
                        val totalQuestions = jsonObject.get("totalQuestions")?.asInt ?: 0
                        val accuracy = jsonObject.get("accuracy")?.asFloat ?: 0f
                        val streakDays = jsonObject.get("streakDays")?.asInt ?: 0
                        val checkinDay = jsonObject.get("checkinDay")?.asInt ?: 0
                        val quizType = jsonObject.get("quizType")?.asString ?: "general"
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null
                        val attemptId = if (jsonObject.has("attemptId") && !jsonObject.get("attemptId").isJsonNull) jsonObject.get("attemptId").asString else null
                        val language = if (jsonObject.has("language") && !jsonObject.get("language").isJsonNull) jsonObject.get("language").asString else "en"

                        val userTz = java.util.TimeZone.getDefault().id
                        Log.d(TAG, "Syncing QUIZ: score=$score, total=$totalQuestions, accuracy=$accuracy, type=$quizType, date=$clientDate, country=$countryCode, tz=$userTz, attemptId=$attemptId, language=$language")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "quiz_completion",
                                metadata = mapOf<String, Any>(
                                    "accuracy" to accuracy,
                                    "score" to score,
                                    "totalQuestions" to totalQuestions,
                                    "streakDays" to streakDays,
                                    "checkinDay" to checkinDay,
                                    "quizType" to quizType,
                                    "language" to language
                                ).let {
                                    if (attemptId != null) it + ("attemptId" to attemptId) else it
                                },
                                client_date = clientDate,
                                country_code = countryCode,
                                timezone = userTz,
                                idempotency_key = event.idempotencyKey
                            ),
                            bearer
                        )
                        
                        try {
                            val timeSpentSeconds = jsonObject.get("timeSpentSeconds")?.asLong ?: 0L
                            com.aipoweredgita.app.network.CoinApi.retrofitService.recordQuizAttempt(
                                com.aipoweredgita.app.network.QuizAttemptRequest(
                                    user_id = event.userId,
                                    score = score,
                                    total_questions = totalQuestions,
                                    quiz_type = quizType,
                                    time_spent_seconds = timeSpentSeconds,
                                    coins_earned = event.coinsToAdjust,
                                    client_date = clientDate,
                                    country_code = countryCode,
                                    attempt_id = attemptId,
                                    language = language
                                ),
                                bearer
                            )
                            Log.d(TAG, "Quiz attempt recorded to server successfully")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to record quiz attempt to server: ${e.message}")
                        }

                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Quiz sync success. awarded=${response.awarded} total=${response.total_coins}")
                    }
                    "CHAPTER" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null
                        val userTz = java.util.TimeZone.getDefault().id

                        Log.d(TAG, "Syncing CHAPTER completion: date=$clientDate, country=$countryCode, tz=$userTz")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "chapter_completion",
                                client_date = clientDate,
                                country_code = countryCode,
                                timezone = userTz,
                                idempotency_key = event.idempotencyKey
                            ),
                            bearer
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Chapter sync success. New server balance: ${response.total_coins}")
                    }
                    "BATTLE" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val battleCoins = jsonObject.get("battleCoins")?.asInt ?: event.coinsToAdjust
                        val score = jsonObject.get("score")?.asInt ?: 0
                        val questionsAnswered = jsonObject.get("questionsAnswered")?.asInt ?: 0
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null
                        val attemptId = if (jsonObject.has("attemptId") && !jsonObject.get("attemptId").isJsonNull) jsonObject.get("attemptId").asString else null
                        val language = if (jsonObject.has("language") && !jsonObject.get("language").isJsonNull) jsonObject.get("language").asString else "en"
                        val userTz = java.util.TimeZone.getDefault().id

                        Log.d(TAG, "Syncing BATTLE: battleCoins=$battleCoins, score=$score, qa=$questionsAnswered, date=$clientDate, country=$countryCode, tz=$userTz, attemptId=$attemptId, language=$language")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "battle_quiz",
                                metadata = mapOf<String, Any>(
                                    "battleCoins" to battleCoins,
                                    "score" to score,
                                    "questionsAnswered" to questionsAnswered,
                                    "timeSpentSeconds" to 60,
                                    "language" to language
                                ).let {
                                    if (attemptId != null) it + ("attemptId" to attemptId) else it
                                },
                                client_date = clientDate,
                                country_code = countryCode,
                                timezone = userTz,
                                idempotency_key = event.idempotencyKey
                            ),
                            bearer
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Battle sync success. awarded=${response.awarded} total=${response.total_coins}")
                    }
                    "SPEND" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val question = jsonObject.get("question")?.asString ?: ""
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null
                        val userTz = java.util.TimeZone.getDefault().id

                        Log.d(TAG, "Syncing SPEND: question=${question.take(50)}, idempotencyKey=${event.idempotencyKey}, date=$clientDate, country=$countryCode, tz=$userTz")
                        val response = CoinApi.retrofitService.spendCoins(
                            CoinSpendRequest(
                                user_id = event.userId,
                                question = question,
                                idempotency_key = event.idempotencyKey,
                                client_date = clientDate,
                                country_code = countryCode,
                                timezone = userTz
                            ),
                            bearer
                        )
                        
                        if (response.duplicate == true) {
                            Log.w(TAG, "Spend sync: Duplicate detected on server, updating local balance to: ${response.remaining_balance}")
                        } else {
                            Log.d(TAG, "Spend sync success. Spent: ${response.spent}, remaining: ${response.remaining_balance}")
                        }
                        userStatsDao.updateKrishnaCoins(response.remaining_balance)
                    }
                    "MEDITATION" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val minutes = jsonObject.get("minutes")?.asInt ?: 5
                        Log.d(TAG, "Syncing MEDITATION: $minutes minutes")
                        val response = CoinApi.retrofitService.logMeditation(
                            com.aipoweredgita.app.network.MeditationLogRequest(
                                minutes = minutes,
                                country_code = java.util.Locale.getDefault().country,
                                timezone = java.util.TimeZone.getDefault().id,
                                idempotency_key = event.idempotencyKey
                            ),
                            bearer
                        )
                        Log.d(TAG, "Meditation sync success. Coins earned: ${response.coins_earned}")
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                    }
                    "CHECKIN" -> {
                        Log.d(TAG, "Syncing CHECKIN")
                        val jsonObject = try { gson.fromJson(event.payload, com.google.gson.JsonObject::class.java) } catch (_: Exception) { null }
                        val clientDate = if (jsonObject?.has("clientDate") == true && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val userTz = java.util.TimeZone.getDefault().id
                        
                        val requestMap = mutableMapOf<String, String>(
                            "user_id" to event.userId,
                            "timezone" to userTz
                        )
                        event.idempotencyKey?.let { requestMap["idempotency_key"] = it }
                        if (clientDate != null) {
                            requestMap["client_date"] = clientDate
                        }
                        
                        val response = CoinApi.retrofitService.checkin(requestMap, bearer)
                        if (response.duplicate == true) {
                            Log.w(TAG, "Checkin sync: Duplicate detected on server")
                        } else {
                            Log.d(TAG, "Checkin sync success. Coins awarded: ${response.coins_awarded}")
                            if (event.userId == authPrefs.userId && response.day > 0) {
                                val syncDate = clientDate?.take(10) ?: DailyRewardsTracker.getInstance(applicationContext).nowLocal()
                                DailyRewardsTracker.getInstance(applicationContext).syncWithServer(response.day, response.week, syncDate)
                            }
                        }
                        if (response.total_coins >= 0) {
                            userStatsDao.updateKrishnaCoins(response.total_coins)
                        }
                        if (event.userId == authPrefs.userId) {
                            DailyRewardsTracker.getInstance(applicationContext).isCheckinSynced = true
                        }
                    }
                    "SHARE" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val chapter = if (jsonObject.has("chapter") && !jsonObject.get("chapter").isJsonNull) jsonObject.get("chapter").asInt else null
                        val verse = if (jsonObject.has("verse") && !jsonObject.get("verse").isJsonNull) jsonObject.get("verse").asInt else null
                        val slokaId = if (jsonObject.has("slokaId") && !jsonObject.get("slokaId").isJsonNull) jsonObject.get("slokaId").asString else null
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null
                        val userTz = java.util.TimeZone.getDefault().id

                        Log.d(TAG, "Syncing SHARE: chapter=$chapter, verse=$verse, slokaId=$slokaId, date=$clientDate, country=$countryCode, tz=$userTz")
                        val response = CoinApi.retrofitService.share(
                            ShareSlokaRequest(
                                user_id = event.userId,
                                sloka_id = slokaId,
                                chapter = chapter,
                                verse = verse,
                                client_date = clientDate,
                                country_code = countryCode,
                                timezone = userTz,
                                idempotency_key = event.idempotencyKey
                            ),
                            bearer
                        )
                        if (response.duplicate == true) {
                            Log.w(TAG, "Share sync: Duplicate detected on server")
                        } else {
                            Log.d(TAG, "Share sync success. Coins awarded: ${response.coins_awarded}")
                            if (event.userId == authPrefs.userId && response.share_day > 0) {
                                val syncDate = clientDate?.take(10) ?: DailyRewardsTracker.getInstance(applicationContext).nowLocal()
                                DailyRewardsTracker.getInstance(applicationContext).syncShareWithServer(response.share_day, response.share_week, syncDate)
                            }
                        }
                        if (response.total_coins >= 0) {
                            userStatsDao.updateKrishnaCoins(response.total_coins)
                        }
                        if (event.userId == authPrefs.userId) {
                            DailyRewardsTracker.getInstance(applicationContext).isShareSynced = true
                        }
                    }
                    "STATS_SYNC" -> {
                        Log.d(TAG, "Syncing STATS_SYNC")
                        val token = authPrefs.token
                        val latestStats = userStatsDao.getUserStatsOnce()
                        if (!token.isNullOrEmpty() && latestStats != null) {
                            val response = CoinApi.retrofitService.syncUserStats(
                                token = "Bearer $token",
                                request = com.aipoweredgita.app.network.UserStatsSyncRequest(
                                    user_id = event.userId,
                                    current_streak = latestStats.currentStreak,
                                    longest_streak = latestStats.longestStreak,
                                    total_quizzes_taken = latestStats.totalQuizzesTaken,
                                    total_questions_answered = latestStats.totalQuestionsAnswered,
                                    verses_read = latestStats.versesRead,
                                    chapters_completed = latestStats.chaptersCompleted,
                                    last_activity_date = latestStats.lastActiveDate,
                                    country_code = java.util.Locale.getDefault().country
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
                                    daysActive = userStatsDao.getUserStatsOnce()?.daysActive ?: 1,
                                    lastActiveDate = response.stats.last_activity_date
                                )
                                Log.d(TAG, "Stats sync success. Server streak: ${response.stats.current_streak}")
                            }
                        } else {
                            Log.w(TAG, "No token available for stats sync, skipping")
                            return Result.retry()
                        }
                    }
                    "ADD_NOTE" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val chapter = jsonObject.get("chapter")?.asInt ?: 0
                        val verse = jsonObject.get("verse")?.asInt ?: 0
                        val text = jsonObject.get("note")?.asString ?: ""
                        
                        Log.d(TAG, "Syncing ADD_NOTE: chapter=$chapter, verse=$verse")
                        CoinApi.retrofitService.syncNotes(
                            com.aipoweredgita.app.network.NotesSyncRequest(event.userId, listOf(com.aipoweredgita.app.network.NoteSyncItem(chapter, verse, text))),
                            bearer
                        )
                    }
                    "DELETE_NOTE" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val chapter = jsonObject.get("chapter")?.asInt ?: 0
                        val verse = jsonObject.get("verse")?.asInt ?: 0
                        
                        Log.d(TAG, "Syncing DELETE_NOTE: chapter=$chapter, verse=$verse")
                        CoinApi.retrofitService.deleteNote(
                            com.aipoweredgita.app.network.NoteDeleteRequest(event.userId, chapter, verse),
                            bearer
                        )
                    }
                    else -> {
                        Log.w(TAG, "Unknown event type: ${event.eventType}, deleting event")
                    }
                }
                // Successful processing: delete event
                dao.delete(event)
                Log.d(TAG, "Successfully processed and deleted event ID: ${event.id}")
            } catch (e: java.io.IOException) {
                Log.e(TAG, "Network failure syncing event ID: ${event.id}: ${e.message}")
                return Result.retry()
            } catch (e: retrofit2.HttpException) {
                Log.e(TAG, "HTTP server error syncing event ID: ${event.id}: ${e.code()} ${e.message()}")
                val errorBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
                Log.e(TAG, "Error body: $errorBody")
                
                if (e.code() in 400..499) {
                    // For SPEND events, don't delete on 400 - might be duplicate idempotency
                    if (event.eventType == "SPEND") {
                        if (errorBody?.contains("duplicate") == true) {
                            dao.delete(event)
                            Log.w(TAG, "Deleted duplicate spend event ID: ${event.id}")
                        } else {
                            Log.w(TAG, "Keeping spend event ID: ${event.id} for retry")
                        }
                    } else {
                        // Non-SPEND events: delete on client error
                        dao.delete(event)
                        Log.w(TAG, "Deleted invalid event ID: ${event.id} due to Client Error ${e.code()}")
                    }
                } else {
                    // Server error (500+): retry
                    return Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error syncing event ID: ${event.id}: ${e.message}", e)
                // Don't delete on unexpected errors - might be transient
                return Result.retry()
            }
        }

        Log.d(TAG, "All events processed successfully")
        
        // TOP-TO-BOTTOM SYNC: Fetch authoritative state from server after all offline events are processed
        Log.d(TAG, "Fetching latest authoritative state from server")
        statsRepository.refreshUserState(currentUserId, force = true)
        
        return Result.success()
    }
}
