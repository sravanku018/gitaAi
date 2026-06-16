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
import com.aipoweredgita.app.utils.AuthPreferences
import com.google.gson.Gson
import java.util.concurrent.TimeUnit

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
    }

    override suspend fun doWork(): Result {
        val authPrefs = AuthPreferences.getInstance(applicationContext)
        val currentUserId = authPrefs.userId

        Log.d(TAG, "doWork() called. UserId: $currentUserId, isGuest: ${authPrefs.isGuestUser}")

        if (currentUserId.isNullOrEmpty() || authPrefs.isGuestUser) {
            Log.d(TAG, "No logged-in user or guest active, skipping background sync")
            return Result.success()
        }

        val database = GitaDatabase.getDatabase(applicationContext)
        val dao = database.pendingSyncEventDao()
        val userStatsDao = database.userStatsDao()

        val statsRepository = com.aipoweredgita.app.repository.StatsRepository(
            userStatsDao = userStatsDao,
            dailyActivityDao = database.dailyActivityDao(),
            appContext = applicationContext
        )

        val events = dao.getPendingEvents(currentUserId)

        if (events.isEmpty()) {
            Log.d(TAG, "No pending sync events found for user: $currentUserId")
            return Result.success()
        }

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

                        Log.d(TAG, "Syncing QUIZ: score=$score, total=$totalQuestions, accuracy=$accuracy, type=$quizType")
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
                                )
                            )
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Quiz sync success. New server balance: ${response.total_coins}")
                    }
                    "CHAPTER" -> {
                        Log.d(TAG, "Syncing CHAPTER completion")
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "chapter_completion"
                            )
                        )
                        userStatsDao.updateKrishnaCoins(response.total_coins)
                        Log.d(TAG, "Chapter sync success. New server balance: ${response.total_coins}")
                    }
                    "SPEND" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val question = jsonObject.get("question")?.asString ?: ""

                        Log.d(TAG, "Syncing SPEND: question=${question.take(50)}, idempotencyKey=${event.idempotencyKey}")
                        val response = CoinApi.retrofitService.spendCoins(
                            CoinSpendRequest(
                                user_id = event.userId,
                                question = question,
                                idempotency_key = event.idempotencyKey
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
                        Log.d(TAG, "Syncing CHECKIN for user: ${event.userId}")
                        val response = CoinApi.retrofitService.checkin(mapOf("user_id" to event.userId))
                        if (response.duplicate == true) {
                            Log.w(TAG, "Checkin sync: Duplicate detected on server")
                        } else {
                            Log.d(TAG, "Checkin sync success. Coins awarded: ${response.coins_awarded}")
                            if (event.userId == authPrefs.userId && response.day > 0) {
                                DailyRewardsTracker.getInstance(applicationContext).syncWithServer(response.day, response.week)
                            }
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

                        Log.d(TAG, "Syncing SHARE: chapter=$chapter, verse=$verse, slokaId=$slokaId")
                        val response = CoinApi.retrofitService.share(
                            ShareSlokaRequest(
                                user_id = event.userId,
                                sloka_id = slokaId,
                                chapter = chapter,
                                verse = verse
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
                        if (event.userId == authPrefs.userId) {
                            DailyRewardsTracker.getInstance(applicationContext).isShareSynced = true
                        }
                    }
                    "STATS_SYNC" -> {
                        Log.d(TAG, "Syncing STATS_SYNC for user: ${event.userId}")
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
                                    last_activity_date = latestStats.lastActiveDate
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
        Log.d(TAG, "Fetching latest authoritative state from server for $currentUserId")
        statsRepository.refreshUserState(currentUserId)
        
        return Result.success()
    }
}
