package com.aipoweredgita.app.network

import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.GitaVerseListAdapter
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
<<<<<<< HEAD
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
=======
import retrofit2.http.GET
import retrofit2.http.Path
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
        if (com.aipoweredgita.app.util.FeatureFlags.ENABLE_VERBOSE_NETWORK_LOGS) {
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
}
<<<<<<< HEAD

// ── Coin API (reuses shared okHttpClient + gson) ─────────────────────────────────

private const val COIN_API_BASE_URL = "https://prime-gorilla-49.sravanku018.deno.net/"

private val coinRetrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create(gson))
    .baseUrl(COIN_API_BASE_URL)
    .client(okHttpClient)
    .build()

// ── Coin API Request Models ──────────────────────────────────────────────────────

data class CreateUserRequest(val user_id: String, val name: String = "", val email: String = "")

data class CoinAwardRequest(
    val user_id: String,
    val source: String,
    val metadata: Map<String, Any>? = null
)

data class CoinSpendRequest(
    val user_id: String,
    val question: String
)

data class ShareSlokaRequest(
    val user_id: String,
    val sloka_id: String? = null,
    val chapter: Int? = null,
    val verse: Int? = null
)

// ── Coin API Response Models ─────────────────────────────────────────────────────

data class CoinBalanceResponse(
    val krishna_coins: Int = 0,
    val days_active: Int = 0,
    val yoga_name: String = "",
    val multiplier: Int = 1,
    val is_max: Int = 0
)

data class CoinAwardResponse(
    val awarded: Int = 0,
    val total_coins: Int = 0,
    val levelled_up: Boolean = false,
    val new_level: Int = 1
)

data class CoinSpendResponse(
    val spent: Int = 0,
    val label: String = "",
    val remaining_balance: Int = 0
)

data class CreateUserResponse(
    val success: Boolean = false,
    val coins: Int = 0
)

data class CheckinResponse(
    val day: Int = 0,
    val week: Int = 0,
    val coins_awarded: Int = 0,
    val weekly_bonus: Int = 0
)

data class ShareResponse(
    val share_day: Int = 0,
    val share_week: Int = 0,
    val coins_awarded: Int = 0,
    val weekly_bonus: Int = 0
)

data class CoinHistoryEntry(
    val amount: Int = 0,
    val type: String = "EARN",
    val source: String = "",
    val description: String = "",
    val created_at: String = ""
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
    val multiplier: Int = 1,
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

interface CoinApiService {
    @GET("coins/balance")
    suspend fun getBalance(@Query("user_id") userId: String): CoinBalanceResponse

    @POST("coins/award")
    suspend fun awardCoins(@Body request: CoinAwardRequest): CoinAwardResponse

    @POST("coins/spend")
    suspend fun spendCoins(@Body request: CoinSpendRequest): CoinSpendResponse

    @GET("coins/history")
    suspend fun getHistory(@Query("user_id") userId: String): List<CoinHistoryEntry>

    @GET("coins/leaderboard")
    suspend fun getLeaderboard(): List<LeaderboardEntry>

    @POST("checkin")
    suspend fun checkin(@Body request: Map<String, String>): CheckinResponse

    @POST("share")
    suspend fun share(@Body request: ShareSlokaRequest): ShareResponse

    @POST("users/create")
    suspend fun createUser(@Body request: CreateUserRequest): CreateUserResponse

    @GET("yoga/stages")
    suspend fun getYogaStages(): YogaStagesResponse
}

object CoinApi {
    val retrofitService: CoinApiService by lazy {
        coinRetrofit.create(CoinApiService::class.java)
    }
}
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
