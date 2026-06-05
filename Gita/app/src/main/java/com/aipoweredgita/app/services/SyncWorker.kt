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

        if (currentUserId.isNullOrEmpty() || authPrefs.isGuestUser) {
            Log.d(TAG, "No logged-in user or guest active, skipping background sync")
            return Result.success()
        }

        val database = GitaDatabase.getDatabase(applicationContext)
        val dao = database.pendingSyncEventDao()
        val events = dao.getPendingEvents(currentUserId)

        if (events.isEmpty()) {
            Log.d(TAG, "No pending sync events found for user: $currentUserId")
            return Result.success()
        }

        Log.d(TAG, "Found ${events.size} pending sync events for user: $currentUserId. Processing...")
        val gson = Gson()

        for (event in events) {
            try {
                Log.d(TAG, "Processing event ID: ${event.id}, type: ${event.eventType}")
                when (event.eventType) {
                    "QUIZ" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val score = jsonObject.get("score")?.asInt ?: 0
                        val totalQuestions = jsonObject.get("totalQuestions")?.asInt ?: 0
                        val accuracy = jsonObject.get("accuracy")?.asFloat ?: 0f
                        val streakDays = jsonObject.get("streakDays")?.asInt ?: 0
                        val checkinDay = jsonObject.get("checkinDay")?.asInt ?: 0

                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "quiz_completion",
                                metadata = mapOf(
                                    "accuracy" to accuracy,
                                    "score" to score,
                                    "totalQuestions" to totalQuestions,
                                    "streakDays" to streakDays,
                                    "checkinDay" to checkinDay
                                )
                            )
                        )
                        authPrefs.localCoins = response.total_coins
                        Log.d(TAG, "Quiz sync success. New server balance: ${response.total_coins}")
                    }
                    "CHAPTER" -> {
                        val response = CoinApi.retrofitService.awardCoins(
                            CoinAwardRequest(
                                user_id = event.userId,
                                source = "chapter_completion"
                            )
                        )
                        authPrefs.localCoins = response.total_coins
                        Log.d(TAG, "Chapter sync success. New server balance: ${response.total_coins}")
                    }
                    "SPEND" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val question = jsonObject.get("question")?.asString ?: ""

                        val response = CoinApi.retrofitService.spendCoins(
                            CoinSpendRequest(
                                user_id = event.userId,
                                question = question
                            )
                        )
                        authPrefs.localCoins = response.remaining_balance
                        Log.d(TAG, "Spend sync success. Remaining balance: ${response.remaining_balance}")
                    }
                    "CHECKIN" -> {
                        val response = CoinApi.retrofitService.checkin(mapOf("user_id" to event.userId))
                        if (event.userId == authPrefs.userId) {
                            DailyRewardsTracker.getInstance(applicationContext).isCheckinSynced = true
                        }
                        Log.d(TAG, "Checkin sync success. Coins awarded: ${response.coins_awarded}")
                    }
                    "SHARE" -> {
                        val jsonObject = gson.fromJson(event.payload, com.google.gson.JsonObject::class.java)
                        val chapter = if (jsonObject.has("chapter") && !jsonObject.get("chapter").isJsonNull) jsonObject.get("chapter").asInt else null
                        val verse = if (jsonObject.has("verse") && !jsonObject.get("verse").isJsonNull) jsonObject.get("verse").asInt else null
                        val slokaId = if (jsonObject.has("slokaId") && !jsonObject.get("slokaId").isJsonNull) jsonObject.get("slokaId").asString else null

                        val response = CoinApi.retrofitService.share(
                            ShareSlokaRequest(
                                user_id = event.userId,
                                sloka_id = slokaId,
                                chapter = chapter,
                                verse = verse
                            )
                        )
                        if (event.userId == authPrefs.userId) {
                            DailyRewardsTracker.getInstance(applicationContext).isShareSynced = true
                        }
                        Log.d(TAG, "Share sync success. Coins awarded: ${response.coins_awarded}")
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
                Log.e(TAG, "HTTP server error syncing event ID: ${event.id}: ${e.message()}")
                if (e.code() in 400..499) {
                    // Client error (e.g. 400/404): delete event to avoid blocking queue
                    dao.delete(event)
                    Log.w(TAG, "Deleted invalid event ID: ${event.id} due to Client Error ${e.code()}")
                } else {
                    return Result.retry()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error syncing event ID: ${event.id}: ${e.message}", e)
                // Delete event to avoid blocking queue forever if it's a corrupted payload/state
                dao.delete(event)
            }
        }

        return Result.success()
    }
}
