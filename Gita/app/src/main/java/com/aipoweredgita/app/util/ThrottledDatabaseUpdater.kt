package com.aipoweredgita.app.util

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Throttled batch database writer to reduce I/O pressure.
 * Instead of writing to database on every verse read, batches multiple reads
 * and writes them together.
 *
 * Benefits:
 * - Reduces database writes by 50-90%
 * - Prevents ANR from database locks
 * - Improves app responsiveness
 * - Maintains data consistency
 *
 * Usage:
 * throttledUpdater.trackVerseRead(chapter, verse)  // Called frequently
 */
class ThrottledDatabaseUpdater(
    private val batchSize: Int = 10,
    private val flushIntervalMs: Long = 5000L,  // 5 seconds
    private val onBatchWrite: suspend (List<VerseRead>) -> Unit
) {
    private val TAG = "ThrottledUpdater"
    private val batch = mutableListOf<VerseRead>()
    private val retryCount = java.util.concurrent.ConcurrentHashMap<String, Int>()
    private var flushJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val maxRetries = 3
    private val writerJobs = java.util.concurrent.ConcurrentHashMap.newKeySet<Job>()

    @Volatile private var isCleaningUp = false

    /**
     * Track a verse read - batched and throttled
     */
    fun trackVerseRead(chapter: Int, verse: Int) {
        if (isCleaningUp) return
        synchronized(batch) {
            if (isCleaningUp) return
            batch.add(VerseRead(chapter, verse))
            Log.d(TAG, "Queued: Ch$chapter:V$verse (batch size: ${batch.size}/$batchSize)")

            // Auto-flush if batch size exceeded
            if (batch.size >= batchSize) {
                Log.d(TAG, "Batch size limit reached, flushing immediately")
                flush()
            } else {
                // Schedule flush if not already scheduled
                if (flushJob?.isCompleted != false) {
                    scheduleFlush()
                }
            }
        }
    }

    /**
     * Force immediate flush of pending writes
     */
    fun flush() {
        if (isCleaningUp) return
        synchronized(batch) {
            if (isCleaningUp || batch.isEmpty()) {
                Log.d(TAG, "No pending writes to flush")
                return
            }

            val toBatch = batch.toList()
            batch.clear()
            flushJob?.cancel()
            flushJob = null

            Log.d(TAG, "Flushing ${toBatch.size} verse reads to database")
            val job = scope.launch {
                try {
                    onBatchWrite(toBatch)
                    // Clear retry counts on success
                    toBatch.forEach { retryCount.remove("${it.chapter}:${it.verse}") }
                    Log.d(TAG, "Successfully flushed ${toBatch.size} reads")
                } catch (e: Exception) {
                    Log.e(TAG, "Error flushing batch: ${e.message}")
                    // Re-queue on failure with retry limit
                    val eligible = toBatch.filter { verse ->
                        val key = "${verse.chapter}:${verse.verse}"
                        val count = retryCount.getOrDefault(key, 0)
                        if (count < maxRetries) {
                            retryCount[key] = count + 1
                            true
                        } else {
                            Log.e(TAG, "Dropping ${key} after $maxRetries retries")
                            retryCount.remove(key)
                            false
                        }
                    }
                    if (eligible.isNotEmpty() && !isCleaningUp) {
                        synchronized(batch) {
                            batch.addAll(0, eligible)
                            scheduleFlush() // Schedule retry flush!
                        }
                    }
                }
            }
            writerJobs.add(job)
            job.invokeOnCompletion { writerJobs.remove(job) }
        }
    }

    /**
     * Schedule a delayed flush if one isn't already scheduled
     */
    private fun scheduleFlush() {
        if (isCleaningUp || (flushJob != null && !flushJob!!.isCompleted)) {
            return  // Already scheduled or cleaning up
        }

        flushJob = scope.launch {
            delay(flushIntervalMs)
            flush()
        }
        Log.d(TAG, "Flush scheduled in ${flushIntervalMs}ms")
    }

    /**
     * Cleanup - flush all pending writes and await completion off the main thread.
     * Prevents new work and drains the queue after in-flight writer jobs finish.
     */
    suspend fun cleanup() = kotlinx.coroutines.withContext(Dispatchers.IO) {
        Log.d(TAG, "Cleaning up - flushing pending writes")
        isCleaningUp = true

        val initialBatch = synchronized(batch) {
            val copied = batch.toList()
            batch.clear()
            flushJob?.cancel()
            flushJob = null
            copied
        }
        if (initialBatch.isNotEmpty()) {
            try {
                onBatchWrite(initialBatch)
            } catch (e: Exception) {
                Log.e(TAG, "Error during cleanup flush: ${e.message}")
            }
        }

        // Await active in-flight writer jobs
        val inFlight = writerJobs.toList()
        inFlight.forEach { job ->
            try { job.join() } catch (_: Exception) {}
        }

        // Final drain of any items re-queued by failed writer jobs
        val leftoverBatch = synchronized(batch) {
            val copied = batch.toList()
            batch.clear()
            copied
        }
        if (leftoverBatch.isNotEmpty()) {
            try {
                onBatchWrite(leftoverBatch)
            } catch (e: Exception) {
                Log.e(TAG, "Error during final cleanup drain: ${e.message}")
            }
        }

        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }

    /**
     * Non-blocking cleanup launcher for callers without a coroutine scope.
     */
    fun cleanupAsync() {
        scope.launch { cleanup() }
    }

    data class VerseRead(
        val chapter: Int,
        val verse: Int
    )
}

/**
 * Query-level caching for frequently accessed database records.
 * Caches recent reads to avoid redundant database queries.
 */
class DatabaseQueryCache(private val maxSize: Int = 1000) {
    private val TAG = "QueryCache"
    private val cache = object : android.util.LruCache<String, CachedRecord>(maxSize) {
        override fun sizeOf(key: String, value: CachedRecord): Int = 1  // Count by entries
    }

    fun get(key: String): Any? {
        val record = cache.get(key)
        return if (record != null) {
            if (!record.isExpired()) {
                Log.d(TAG, "Cache hit: $key")
                record.value
            } else {
                Log.d(TAG, "Cache expired: $key, evicting")
                cache.remove(key)
                null
            }
        } else {
            Log.d(TAG, "Cache miss: $key")
            null
        }
    }

    fun put(key: String, value: Any, ttlMs: Long = 60000) {
        cache.put(key, CachedRecord(value, System.currentTimeMillis() + ttlMs))
        Log.d(TAG, "Cached: $key (TTL: ${ttlMs}ms)")
    }

    fun invalidate(key: String) {
        cache.remove(key)
        Log.d(TAG, "Cache invalidated: $key")
    }

    fun clear() {
        cache.evictAll()
        Log.d(TAG, "Cache cleared")
    }

    private data class CachedRecord(
        val value: Any,
        val expiresAt: Long
    ) {
        fun isExpired() = System.currentTimeMillis() > expiresAt
    }
}
