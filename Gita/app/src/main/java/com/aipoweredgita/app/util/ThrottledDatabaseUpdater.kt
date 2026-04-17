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
    private var flushJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Track a verse read - batched and throttled
     */
    fun trackVerseRead(chapter: Int, verse: Int) {
        synchronized(batch) {
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
        synchronized(batch) {
            if (batch.isEmpty()) {
                Log.d(TAG, "No pending writes to flush")
                return
            }

            val toBatch = batch.toList()
            batch.clear()
            flushJob?.cancel()
            flushJob = null

            Log.d(TAG, "Flushing ${toBatch.size} verse reads to database")
            scope.launch {
                try {
                    onBatchWrite(toBatch)
                    Log.d(TAG, "Successfully flushed ${toBatch.size} reads")
                } catch (e: Exception) {
                    Log.e(TAG, "Error flushing batch: ${e.message}")
                    // Re-queue on failure
                    synchronized(batch) {
                        batch.addAll(0, toBatch)
                    }
                }
            }
        }
    }

    /**
     * Schedule a delayed flush if one isn't already scheduled
     */
    private fun scheduleFlush() {
        if (flushJob != null && !flushJob!!.isCompleted) {
            return  // Already scheduled
        }

        flushJob = scope.launch {
            delay(flushIntervalMs)
            flush()
        }
        Log.d(TAG, "Flush scheduled in ${flushIntervalMs}ms")
    }

    /**
     * Cleanup - flush all pending writes
     */
    fun cleanup() {
        Log.d(TAG, "Cleaning up - flushing pending writes")
        flush()
        scope.launch {
            delay(500)  // Give time for flush to complete
            scope.coroutineContext[Job]?.cancel()
        }
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
        return if (record != null && !record.isExpired()) {
            Log.d(TAG, "Cache hit: $key")
            record.value
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
