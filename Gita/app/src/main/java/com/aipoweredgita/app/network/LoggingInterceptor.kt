package com.aipoweredgita.app.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Logging interceptor for API requests and responses.
 * Helps debug network issues and monitor API performance.
 */
class LoggingInterceptor : Interceptor {
    private val TAG = "GitaAPI"

    override fun intercept(chain: Interceptor.Chain): Response {
        val startTime = System.currentTimeMillis()

        Log.d(TAG, ">>> REQUEST")

        return try {
            val response = chain.proceed(chain.request())
            val elapsedMs = System.currentTimeMillis() - startTime

            Log.d(TAG, "<<< RESPONSE (${elapsedMs}ms)")

            response
        } catch (e: Exception) {
            val elapsedMs = System.currentTimeMillis() - startTime
            Log.e(TAG, "!!! ERROR after ${elapsedMs}ms: ${e.javaClass.simpleName}")
            throw e
        }
    }
}
