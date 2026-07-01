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

        Log.d(TAG, "doWork() called. isGuest: ${authPrefs.isGuestUser}")

        if (currentUserId.isNullOrEmpty() || authPrefs.isGuestUser) {
            Log.d(TAG, "No logged-in user or guest active, skipping background sync")
            return Result.success()
        }

        val database = GitaDatabase.getDatabase(applicationContext)
        val dao = database.pendingSyncEventDao()
        val userStatsDao = database.userStatsDao()

        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext, SyncWorkerEntryPoint::class.java
        )
        val statsRepository = entryPoint.statsRepository()

        val events = dao.getPendingEvents(currentUserId)

        if (events.isEmpty()) {
            Log.d(TAG, "No pending sync events found for user")
            return Result.success()
        }

        Log.d(TAG, "Found ${events.size} pending sync events for user")
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

                        Log.d(TAG, "Syncing QUIZ: score=$score, total=$totalQuestions, accuracy=$accuracy, type=$quizType, date=$clientDate, country=$countryCode")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "quiz_completion",
                                metadata = mapOf(
                                    "accuracy" to accuracy,
                                    "score" to score,
                                    "totalQuestions" to totalQuestions,
                                    "streakDays" to streakDays,
                                    "checkinDay" to checkinDay,
                                    "quizType" to quizType
                                ),
                                client_date = clientDate,
                                country_code = countryCode
                            )
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Quiz sync success. New server balance: ${response.total_coins}")
                    }
                    "CHAPTER" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null

                        Log.d(TAG, "Syncing CHAPTER completion: date=$clientDate, country=$countryCode")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "chapter_completion",
                                client_date = clientDate,
                                country_code = countryCode
                            )
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Chapter sync success. New server balance: ${response.total_coins}")
                    }
                    "BATTLE" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val battleCoins = jsonObject.get("battleCoins")?.asInt ?: event.coinsToAdjust
                        val score = jsonObject.get("score")?.asInt ?: 0
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null

                        Log.d(TAG, "Syncing BATTLE: battleCoins=$battleCoins, score=$score, date=$clientDate, country=$countryCode")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "battle_quiz",
                                metadata = mapOf(
                                    "battleCoins" to battleCoins,
                                    "score" to score
                                ),
                                client_date = clientDate,
                                country_code = countryCode
                            )
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Battle sync success. New server balance: ${response.total_coins}")
                    }
                    "SPEND" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val question = jsonObject.get("question")?.asString ?: ""
                        val clientDate = if (jsonObject.has("clientDate") && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        val countryCode = if (jsonObject.has("countryCode") && !jsonObject.get("countryCode").isJsonNull) jsonObject.get("countryCode").asString else null

                        Log.d(TAG, "Syncing SPEND: question=${question.take(50)}, idempotencyKey=${event.idempotencyKey}, date=$clientDate, country=$countryCode")
                        val response = CoinApi.retrofitService.spendCoins(
                            CoinSpendRequest(
                                user_id = event.userId,
                                question = question,
                                idempotency_key = event.idempotencyKey,
                                client_date = clientDate,
                                country_code = countryCode
                            )
                        )
                        
                        if (response.duplicate == true) {
                            Log.w(TAG, "Spend sync: Duplicate detected on server, updating local balance to: ${response.remaining_balance}")
                        } else {
                            Log.d(TAG, "Spend sync success. Spent: ${response.spent}, remaining: ${response.remaining_balance}")
                        }
                        userStatsDao.updateKrishnaCoins(response.remaining_balance)
                    }
                    "CHECKIN" -> {
                        Log.d(TAG, "Syncing CHECKIN")
                        val jsonObject = try { gson.fromJson(event.payload, com.google.gson.JsonObject::class.java) } catch (_: Exception) { null }
                        val clientDate = if (jsonObject?.has("clientDate") == true && !jsonObject.get("clientDate").isJsonNull) jsonObject.get("clientDate").asString else null
                        
                        val requestMap = mutableMapOf(
                            "user_id" to event.userId,
                            "idempotency_key" to event.idempotencyKey
                        )
                        if (clientDate != null) {
                            requestMap["client_date"] = clientDate
                        }
                        
                        val response = CoinApi.retrofitService.checkin(requestMap)
                        if (response.duplicate == true) {
                            Log.w(TAG, "Checkin sync: Duplicate detected on server")
                        } else {
                            Log.d(TAG, "Checkin sync success. Coins awarded: ${response.coins_awarded}")
                            if (event.userId == authPrefs.userId && response.day > 0) {
                                DailyRewardsTracker.getInstance(applicationContext).syncWithServer(response.day, response.week)
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

                        Log.d(TAG, "Syncing SHARE: chapter=$chapter, verse=$verse, slokaId=$slokaId, date=$clientDate, country=$countryCode")
                        val response = CoinApi.retrofitService.share(
                            ShareSlokaRequest(
                                user_id = event.userId,
                                sloka_id = slokaId,
                                chapter = chapter,
                                verse = verse,
                                client_date = clientDate,
                                country_code = countryCode,
                                idempotency_key = event.idempotencyKey
                            )
                        )
                        if (response.duplicate == true) {
                            Log.w(TAG, "Share sync: Duplicate detected on server")
                        } else {
                            Log.d(TAG, "Share sync success. Coins awarded: ${response.coins_awarded}")
                            if (event.userId == authPrefs.userId && response.share_day > 0) {
                                DailyRewardsTracker.getInstance(applicationContext).syncShareWithServer(response.share_day, response.share_week)
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
                        }
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
        statsRepository.refreshUserState(currentUserId)
        
        return Result.success()
    }
}
