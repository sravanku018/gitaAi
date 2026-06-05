package com.aipoweredgita.app.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Intelligent retry interceptor with exponential backoff and circuit breaker.
 * Prevents cascading failures and hammering of API during outages.
 */
class RetryInterceptor : Interceptor {
    private val TAG = "RetryInterceptor"

    // Circuit breaker state
    private val circuitBreaker = CircuitBreaker()

    // Configuration — capped delays to avoid thread-pool starvation
    private val maxRetries = 1
    private val initialDelayMs = 100L
    private val maxDelayMs = 1000L  // cap at 1s
    private val backoffMultiplier = 2.0

    // Endpoints excluded from circuit breaker (known to fail gracefully)
    private val excludedPaths = setOf("/guest/create")

    // Auth endpoints should never be blocked by circuit breaker
    private val authPaths = setOf("/auth/register", "/auth/login", "/auth/verify", "/auth/logout")

    override fun intercept(chain: Interceptor.Chain): Response {
        return executeWithRetry(chain, 0)
    }

    private fun executeWithRetry(chain: Interceptor.Chain, retryCount: Int): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        val isExcluded = excludedPaths.any { path.endsWith(it) }
        val isAuth = authPaths.any { path.endsWith(it) }

        // Check circuit breaker state (skip for excluded and auth endpoints)
        if (!isExcluded && !isAuth && circuitBreaker.isOpen()) {
            Log.w(TAG, "Circuit breaker OPEN - rejecting request to $path")
            throw IOException("Service temporarily unavailable (circuit breaker open)")
        }

        Log.d(TAG, "Executing request to $path (attempt ${retryCount + 1}/$maxRetries)")

        return try {
            val response = chain.proceed(request)

            // Success - reset circuit breaker (skip for excluded endpoints)
            if (response.isSuccessful) {
                if (!isExcluded) circuitBreaker.recordSuccess()
                Log.d(TAG, "Request successful")
                return response
            }

            // Check response status - use response.isSuccessful flag logic
            // For non-successful responses, we know server returned an error
            // 2xx codes are handled above, so here we handle 3xx, 4xx, 5xx

            // For 5xx errors (server errors) - retryable
            // For 4xx errors (client errors) - not retryable
            // We can check by attempting retry logic

            if (!response.isSuccessful) {
                val code = response.code
                // Retry only on server or rate-limit errors; do not retry on 4xx client errors
                val shouldRetry = code == 429 || (code in 500..599)
                // Don't count auth endpoint failures (401/404 are expected)
                if (!isExcluded && !isAuth) circuitBreaker.recordFailure()
                response.close()

                if (shouldRetry && retryCount < maxRetries) {
                    val delayMs = calculateBackoffDelay(retryCount)
                    Log.w(TAG, "HTTP $code, scheduling retry #${retryCount + 1} (backoff ${delayMs}ms)")
                    try { Thread.sleep(delayMs) } catch (_: InterruptedException) { }
                    // Do not block threads; tag the request with attempt info and retry immediately
                    val taggedRequest = request.newBuilder()
                        .header("X-Retry-Attempt", (retryCount + 1).toString())
                        .build()
                    return executeWithRetry(object : Interceptor.Chain by chain {
                        override fun request() = taggedRequest
                    }, retryCount + 1)
                } else {
                    val msg = if (!shouldRetry) "Non-retryable HTTP $code" else "Max retries exceeded"
                    Log.e(TAG, msg)
                    throw IOException("HTTP error $code")
                }
            }

            response
        } catch (e: Exception) {
            // Check if error is retryable
            if (isRetryableError(e) && retryCount < maxRetries && !isAuth) {
                if (!isExcluded) circuitBreaker.recordFailure()
                val delayMs = calculateBackoffDelay(retryCount)
                Log.w(TAG, "Retryable error: ${e.message}, scheduling retry #${retryCount + 1} (backoff ${delayMs}ms)")
                try { Thread.sleep(delayMs) } catch (_: InterruptedException) { }
                val taggedRequest = chain.request().newBuilder()
                    .header("X-Retry-Attempt", (retryCount + 1).toString())
                    .build()
                return executeWithRetry(object : Interceptor.Chain by chain {
                    override fun request() = taggedRequest
                }, retryCount + 1)
            } else {
                Log.e(TAG, "Non-retryable error or max retries exceeded: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Calculate exponential backoff delay with jitter
     * Formula: min(initialDelay * 2^retryCount + random jitter, maxDelay)
     */
    private fun calculateBackoffDelay(retryCount: Int): Long {
        val exponentialDelay = (initialDelayMs * Math.pow(backoffMultiplier, retryCount.toDouble())).toLong()
        val cappedDelay = exponentialDelay.coerceAtMost(maxDelayMs)
        // Add jitter: ±20% to prevent thundering herd
        // Add jitter: +/-20% to prevent thundering herd
        val jitter = (cappedDelay * 0.2 * Math.random()).toLong()
        return cappedDelay - (cappedDelay / 10) + jitter
    }

    /**
     * Determine if an error is retryable (transient)
     */
    private fun isRetryableError(error: Exception): Boolean {
        return when (error) {
            is SocketTimeoutException -> {
                Log.d(TAG, "Detected socket timeout - retryable")
                true
            }
            is IOException -> {
                val message = error.message?.lowercase() ?: ""
                val isTransient = message.contains("timeout") ||
                        message.contains("unable to resolve host") ||
                        message.contains("connection reset") ||
                        message.contains("broken pipe") ||
                        message.contains("econnrefused") ||
                        message.contains("econnreset")
                if (isTransient) {
                    Log.d(TAG, "Detected transient IO error: $message - retryable")
                } else {
                    Log.d(TAG, "Detected non-transient error: $message - not retrying")
                }
                isTransient
            }
            else -> {
                Log.d(TAG, "Unknown error type: ${error::class.simpleName} - not retrying")
                false
            }
        }
    }
}

/**
 * Circuit breaker pattern to prevent cascading failures.
 * States: CLOSED (working) -> OPEN (failing) -> HALF_OPEN (testing) -> CLOSED
 */
class CircuitBreaker {
    private val TAG = "CircuitBreaker"

    private var state = State.CLOSED
    private var failureCount = 0
    private var successCount = 0
    private var lastFailureTime = 0L

    // Configuration
    private val failureThreshold = 20  // Open after 20 consecutive failures (was 10)
    private val successThreshold = 1  // Close after 1 success in HALF_OPEN (was 2)
    private val timeoutMs = 5000L     // Try again after 5 seconds (was 10)

    fun isOpen(): Boolean {
        synchronized(this) {
            return when (state) {
                State.OPEN -> {
                    // Check if timeout has passed to transition to HALF_OPEN
                    if (System.currentTimeMillis() - lastFailureTime > timeoutMs) {
                        Log.d(TAG, "Timeout passed, transitioning OPEN -> HALF_OPEN")
                        state = State.HALF_OPEN
                        successCount = 0
                        false
                    } else {
                        true
                    }
                }
                else -> false
            }
        }
    }

    fun recordSuccess() {
        synchronized(this) {
            when (state) {
                State.CLOSED -> {
                    failureCount = 0
                }
                State.HALF_OPEN -> {
                    successCount++
                    if (successCount >= successThreshold) {
                        Log.d(TAG, "Recovery confirmed, transitioning HALF_OPEN -> CLOSED")
                        state = State.CLOSED
                        failureCount = 0
                    }
                }
                State.OPEN -> {
                    // Do nothing, still in cooldown
                }
            }
        }
    }

    fun recordFailure() {
        synchronized(this) {
            lastFailureTime = System.currentTimeMillis()

            when (state) {
                State.CLOSED -> {
                    failureCount++
                    if (failureCount >= failureThreshold) {
                        Log.w(TAG, "Failure threshold reached, transitioning CLOSED -> OPEN")
                        state = State.OPEN
                        failureCount = 0
                    }
                }
                State.HALF_OPEN -> {
                    Log.w(TAG, "Failure during recovery, transitioning HALF_OPEN -> OPEN")
                    state = State.OPEN
                    successCount = 0
                }
                State.OPEN -> {
                    // Already open, reset failure count for timeout
                    failureCount = 0
                }
            }
        }
    }

    private enum class State {
        CLOSED,      // Normal operation
        OPEN,        // Failing, reject requests
        HALF_OPEN    // Testing if service recovered
    }
}
