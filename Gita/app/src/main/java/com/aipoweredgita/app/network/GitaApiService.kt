package com.aipoweredgita.app.network

import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.GitaVerseListAdapter
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val BASE_URL = com.aipoweredgita.app.util.GitaConstants.API_BASE_URL

private val gson = GsonBuilder()
    .setStrictness(Strictness.LENIENT)
    .registerTypeAdapter(
        object : TypeToken<List<GitaVerse>>() {}.type,
        GitaVerseListAdapter()
    )
    .create()

/**
 * OkHttp client with intelligent retry strategy, timeouts, and logging.
 * - Connection timeout: 15 seconds (detect dead connections)
 * - Read timeout: 20 seconds (API response time limit)
 * - Write timeout: 10 seconds (upload time limit)
 * - Retry interceptor: Exponential backoff with circuit breaker
 */
private val okHttpClient = OkHttpClient.Builder()
    .connectTimeout(com.aipoweredgita.app.util.GitaConstants.NETWORK_CONNECT_TIMEOUT_SEC, TimeUnit.SECONDS)
    .readTimeout(com.aipoweredgita.app.util.GitaConstants.NETWORK_READ_TIMEOUT_SEC, TimeUnit.SECONDS)
    .writeTimeout(com.aipoweredgita.app.util.GitaConstants.NETWORK_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)
    .addInterceptor(RetryInterceptor())
    .apply {
        if (com.aipoweredgita.app.BuildConfig.DEBUG) {
            addInterceptor(LoggingInterceptor())
        }
    }
    .build()

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gson))
    .baseUrl(BASE_URL)
    .client(okHttpClient)
    .build()

interface GitaApiService {
    @GET("{language}/verse/{chapter_no}/{verse_no}")
    suspend fun getVerse(
        @Path("language") language: String,
        @Path("chapter_no") chapterNo: Int,
        @Path("verse_no") verseNo: Int
    ): GitaVerse

    @GET("{language}/verse/{chapter_no}/{verse_no}")
    suspend fun getVerseRaw(
        @Path("language") language: String,
        @Path("chapter_no") chapterNo: Int,
        @Path("verse_no") verseNo: Int
    ): com.aipoweredgita.app.data.GitaVerseRaw
}

object GitaApi {
    val retrofitService: GitaApiService by lazy {
        retrofit.create(GitaApiService::class.java)
    }

    /** Shared OkHttpClient with retry and circuit-breaker — reuse in VoiceChat etc. */
    val sharedOkHttpClient: OkHttpClient by lazy { okHttpClient }
}

// ── Coin API (reuses shared okHttpClient + gson) ─────────────────────────────────

private val coinRetrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gson))
    .baseUrl(com.aipoweredgita.app.util.GitaConstants.COIN_API_BASE_URL)
    .client(okHttpClient)
    .build()

// ── Coin API Request Models ──────────────────────────────────────────────────────

data class CreateUserRequest(val user_id: String, val name: String = "", val email: String = "")

data class CoinAwardRequest(
    val user_id: String,
    val source: String,
    val metadata: Map<String, Any>? = null,
    val client_date: String? = null,
    val country_code: String? = null,
    val timezone: String? = null
)

data class CoinSpendRequest(
    val user_id: String,
    val question: String,
    val idempotency_key: String? = null,
    val client_date: String? = null,
    val country_code: String? = null,
    val timezone: String? = null
)

data class ShareSlokaRequest(
    val user_id: String,
    val sloka_id: String? = null,
    val chapter: Int? = null,
    val verse: Int? = null,
    val idempotency_key: String? = null,
    val client_date: String? = null,
    val country_code: String? = null,
    val timezone: String? = null
)

data class MeditationLogRequest(
    val minutes: Int,
    val country_code: String? = null,
    val timezone: String? = null
)

data class MeditationLogResponse(
    val success: Boolean = false,
    val user_id: String = "",
    val minutes: Int = 0,
    val coins_earned: Int = 0,
    val total_coins: Int = 0
)

data class ClaimGuestRequest(
    val guest_id: String,
    val real_user_id: String,
    val name: String = "",
    val email: String = ""
)

// ── Auth Request Models ──────────────────────────────────────────────────────

data class AuthRegisterRequest(
    val user_id: String,
    val password: String,
    val name: String = "",
    val email: String = ""
)

data class AuthLoginRequest(
    val user_id: String,
    val password: String
)

// ── Coin API Response Models ─────────────────────────────────────────────────────

data class CoinBalanceResponse(
    val krishna_coins: Int = 0,
    val days_active: Int = 0,
    val current_streak: Int = 0,
    val longest_streak: Int = 0,
    val total_quizzes_taken: Int = 0,
    val total_questions_answered: Int = 0,
    val total_correct_answers: Int = 0,
    val best_score: Int = 0,
    val best_score_out_of: Int = 0,
    val verses_read: Int = 0,
    val chapters_completed: Int = 0,
    val last_activity_date: String? = null,
    val updated_at: String? = null,
    val yoga_name: String? = null,
    val multiplier: Double = 1.0,
    val is_max: Int = 0,
    val checkin_day: Int = 0,
    val checkin_week: Int = 0,
    val share_day: Int = 0,
    val share_week: Int = 0,
    val last_checkin: String? = null,
    val last_share: String? = null
)

data class UserStatsSyncRequest(
    val user_id: String,
    val current_streak: Int = 0,
    val longest_streak: Int = 0,
    val total_quizzes_taken: Int = 0,
    val total_questions_answered: Int = 0,
    val total_correct_answers: Int = 0,
    val verses_read: Int = 0,
    val chapters_completed: Int = 0,
    val last_activity_date: String = "",
    val country_code: String? = null
)

data class UserStatsSyncDto(
    val current_streak: Int = 0,
    val longest_streak: Int = 0,
    val total_quizzes_taken: Int = 0,
    val total_questions_answered: Int = 0,
    val total_correct_answers: Int = 0,
    val verses_read: Int = 0,
    val chapters_completed: Int = 0,
    val last_activity_date: String = "",
    val updated_at: String = ""
)

data class UserStatsSyncResponse(
    val success: Boolean = false,
    val stats: UserStatsSyncDto? = null
)

data class ServerNote(
    val id: Int = 0,
    val chapter_no: Int = 0,
    val verse_no: Int = 0,
    val note: String = "",
    val created_at: String = "",
    val updated_at: String = ""
)

data class NotesSyncRequest(
    val user_id: String,
    val notes: List<NoteSyncItem>
)

data class NoteSyncItem(
    val chapterNo: Int,
    val verseNo: Int,
    val note: String
)

data class NotesSyncResponse(
    val success: Boolean = false,
    val synced: Int = 0
)

data class NoteDeleteRequest(
    val user_id: String,
    val chapter_no: Int,
    val verse_no: Int
)

data class CoinAwardResponse(
    val awarded: Int = 0,
    val total_coins: Int = 0,
    val levelled_up: Boolean = false,
    val new_level: Int = 1
)

data class QuizAttemptRequest(
    val user_id: String,
    val score: Int,
    val total_questions: Int,
    val quiz_type: String = "general",
    val time_spent_seconds: Long = 0,
    val coins_earned: Int = 0,
    val client_date: String? = null,
    val country_code: String? = null,
    val attempt_id: String? = null,
    val language: String = "en"
)

data class QuizAttemptDto(
    val id: Int = 0,
    val score: Int = 0,
    val total_questions: Int = 15,
    val quiz_type: String = "general",
    val time_spent_seconds: Long = 0,
    val avg_time_per_question: Int = 0,
    val coins_earned: Int = 0,
    val accuracy: Int = 0,
    val created_at: String = "",
    val attempt_id: String? = null,
    val language: String = "en"
)

data class ActivityDayDto(
    val date: String = "",
    val checkins: Int = 0,
    val quizzes: Int = 0,
    val shares: Int = 0,
    val voice_chats: Int = 0,
    val total_events: Int = 0
)

data class CoinSpendResponse(
    val spent: Int = 0,
    val label: String = "",
    val remaining_balance: Int = 0,
    val duplicate: Boolean = false
)

data class CreateUserResponse(
    val success: Boolean = false,
    val coins: Int = 0,
    val token: String? = null
)

data class CreateGuestResponse(
    val guest_id: String = "",
    val coins: Int = 50
)

data class ClaimGuestResponse(
    val success: Boolean = false,
    val user_id: String = "",
    val sync_bonus: Int = 150,
    val error: String? = null
)

// ── Auth Response Models ─────────────────────────────────────────────────────

data class AuthResponse(
    val success: Boolean = false,
    val user_id: String = "",
    val token: String = "",
    val coins: Int = 0,
    val yoga_level: Int = 1,
    val error: String? = null
)

data class CheckinResponse(
    val day: Int = 0,
    val week: Int = 0,
    val coins_awarded: Int = 0,
    val weekly_bonus: Int = 0,
    val total_coins: Int = -1,  // -1 = not provided by older backends
    val duplicate: Boolean = false
)

data class ShareResponse(
    val share_day: Int = 0,
    val share_week: Int = 0,
    val coins_awarded: Int = 0,
    val weekly_bonus: Int = 0,
    val total_coins: Int = -1,  // -1 = not provided by older backends
    val duplicate: Boolean = false
)

data class CoinHistoryEntry(
    val id: Int = 0,
    val amount: Int = 0,
    val type: String = "EARN",
    val source: String = "",
    val description: String = "",
    val idempotency_key: String? = null,
    @com.google.gson.annotations.SerializedName(value = "created_at", alternate = ["createdAt"])
    val created_at: String = ""
) {
    val isSpend: Boolean get() = type.equals("SPEND", ignoreCase = true) || amount < 0
    val isEarn: Boolean get() = !isSpend
    val signedAmount: Int get() = if (isSpend) -kotlin.math.abs(amount) else kotlin.math.abs(amount)
}

data class VoiceCostResponse(
    val cost: Int = 2,
    val label: String = "Short",
    val length: Int = 0
)

data class LeaderboardEntry(
    val name: String = "",
    val krishna_coins: Int = 0,
    val yoga_level: String = "",
    val rank: Int = 0
)

data class YogaLevel(
    val level: Int = 0,
    val name: String = "",
    val min_coins: Int = 0,
    val max_coins: Int = 0,
    val multiplier: Double = 1.0,
    val description: String = ""
)

data class YogaSubStage(
    val id: Int = 0,
    val level: Int = 0,
    val sub_level: Int = 0,
    val sub_name: String = "",
    val min_coins: Int = 0,
    val max_coins: Int = 0
)

data class YogaStagesResponse(
    val levels: List<YogaLevel> = emptyList(),
    val sub_stages: List<YogaSubStage> = emptyList()
)

// Reconciliation data classes
data class ReconcileRequest(
    val user_id: String,
    val local_balance: Int,
    val pending_events: List<PendingEvent>
)

data class PendingEvent(
    val idempotency_key: String,
    val coinsToAdjust: Int
)

data class ReconcileResponse(
    val server_balance: Int,
    val expected_balance: Int,
    val needs_correction: Boolean,
    val correction_delta: Int,
    val pending_adjustment: Int,
    val unprocessed_events: List<UnprocessedEvent>
)

data class AutoReconcileRequest(
    val user_id: String
)

data class AutoReconcileResponse(
    val current_balance: Int,
    val corrected_balance: Int,
    val needs_correction: Boolean,
    val delta_applied: Int,
    val rows_deleted: Int,
    val anomalies_detected: Int,
    val anomalies: List<AnomalyItem>,
    val corrections_applied: List<CorrectionItem>,
    val groq_analysis: GroqAnalysis?,
    val engine: String,
    val corrected: Boolean
)

data class AnomalyItem(
    val type: String,
    val severity: String,
    val description: String
)

data class CorrectionItem(
    val reason: String
)

data class GroqAnalysis(
    val analysis: String?,
    val anomaly_score: Int?,
    val confidence: Double?
)

data class UnprocessedEvent(
    val idempotency_key: String,
    val amount: Int
)

interface CoinApiService {
    @GET("coins/balance")
    suspend fun getBalance(
        @Query("user_id") userId: String,
        @Header("Authorization") token: String? = null
    ): CoinBalanceResponse

    @POST("coins/award")
    suspend fun awardCoins(
        @Body request: CoinAwardRequest,
        @Header("Authorization") token: String? = null
    ): CoinAwardResponse

    @POST("coins/spend")
    suspend fun spendCoins(
        @Body request: CoinSpendRequest,
        @Header("Authorization") token: String? = null
    ): CoinSpendResponse

    @POST("coins/reconcile")
    suspend fun reconcileBalance(
        @Body request: ReconcileRequest,
        @Header("Authorization") token: String? = null
    ): ReconcileResponse

    @POST("coins/auto-reconcile")
    suspend fun autoReconcile(
        @Body request: AutoReconcileRequest,
        @Header("Authorization") token: String? = null
    ): AutoReconcileResponse

    @GET("coins/voice-cost")
    suspend fun getVoiceCost(@Query("question") question: String): VoiceCostResponse

    @GET("coins/history")
    suspend fun getHistory(
        @Query("user_id") userId: String,
        @Header("Authorization") token: String? = null,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): List<CoinHistoryEntry>

    @GET("coins/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @POST("checkin")
    suspend fun checkin(
        @Body request: Map<String, String>,
        @Header("Authorization") token: String? = null
    ): CheckinResponse

    @POST("share")
    suspend fun share(
        @Body request: ShareSlokaRequest,
        @Header("Authorization") token: String? = null
    ): ShareResponse

    @POST("quiz/attempt")
    suspend fun recordQuizAttempt(
        @Body request: QuizAttemptRequest,
        @Header("Authorization") token: String? = null
    ): Map<String, Any>

    @POST("meditation/log")
    suspend fun logMeditation(
        @Body request: MeditationLogRequest,
        @Header("Authorization") token: String? = null
    ): MeditationLogResponse

    @GET("quiz/history")
    suspend fun getQuizHistory(
        @Query("user_id") userId: String,
        @Header("Authorization") token: String? = null,
        @Query("limit") limit: Int = 50
    ): List<QuizAttemptDto>

    @GET("activity/history")
    suspend fun getActivityHistory(
        @Query("user_id") userId: String,
        @Header("Authorization") token: String? = null
    ): List<ActivityDayDto>

    @POST("users/create")
    suspend fun createUser(@Body request: CreateUserRequest): CreateUserResponse

    @POST("guest/create")
    suspend fun createGuest(): CreateGuestResponse

    @POST("guest/claim")
    suspend fun claimGuest(
        @Body request: ClaimGuestRequest,
        @Header("Authorization") token: String? = null
    ): ClaimGuestResponse

    // ── Auth Endpoints ───────────────────────────────────────────────────

    @POST("auth/register")
    suspend fun register(@Body request: AuthRegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body request: AuthLoginRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Header("Authorization") token: String): AuthResponse

    @POST("auth/delete")
    suspend fun deleteAccount(@Header("Authorization") token: String): AuthResponse

    @GET("yoga/stages")
    suspend fun getYogaStages(): YogaStagesResponse

    @POST("users/stats/sync")
    suspend fun syncUserStats(
        @Body request: UserStatsSyncRequest,
        @Header("Authorization") token: String? = null
    ): UserStatsSyncResponse

    @GET("notes")
    suspend fun getNotes(
        @Query("user_id") userId: String,
        @Header("Authorization") token: String? = null
    ): List<ServerNote>

    @POST("notes/sync")
    suspend fun syncNotes(
        @Body request: NotesSyncRequest,
        @Header("Authorization") token: String? = null
    ): NotesSyncResponse

    @POST("notes/delete")
    suspend fun deleteNote(
        @Body request: NoteDeleteRequest,
        @Header("Authorization") token: String? = null
    ): Map<String, Any>
}

object CoinApi {
    val retrofitService: CoinApiService by lazy {
        coinRetrofit.create(CoinApiService::class.java)
    }
}
