package com.aipoweredgita.app.network

import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.GitaVerseListAdapter
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
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
