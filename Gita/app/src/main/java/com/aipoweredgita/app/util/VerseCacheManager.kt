package com.aipoweredgita.app.util

import android.util.LruCache
import kotlinx.coroutines.sync.withLock
import com.aipoweredgita.app.data.GitaVerse

/**
 * Bounded memory cache for verses using LRU eviction.
 * Prevents unbounded memory growth from accumulated verse objects.
 *
 * Automatically evicts oldest verses when memory limit is reached.
 * Max size: 5MB (approximately 10,000 verses)
 */
class VerseCacheManager(maxSizeKb: Int = 5000) {

    // sizeOf() returns bytes, so maxSize must also be in bytes
    private val cache = object : LruCache<String, GitaVerse>(maxSizeKb * 1024) {
        override fun sizeOf(key: String, value: GitaVerse): Int {
            // Estimate verse size: verse text + translation + purport + metadata in bytes
            val verseSize = value.verse.length * 2
            val translationSize = value.translation.length * 2
            val purportSize = value.purport.sumOf { it.length * 2 }
            return verseSize + translationSize + purportSize + 200  // 200 bytes for metadata
        }
    }

    // In-flight deduplication: prevents thundering-herd duplicate fetches for same key
    private val inFlightMutexes = java.util.concurrent.ConcurrentHashMap<String, kotlinx.coroutines.sync.Mutex>()

    /**
     * Get verse from cache
     * Returns null if not cached or evicted
     */
    fun get(chapter: Int, verse: Int): GitaVerse? {
        return cache.get(makeKey(chapter, verse))
    }

    /**
     * Put verse in cache
     * Automatically evicts oldest verses if max size exceeded
     */
    fun put(chapter: Int, verse: Int, verseData: GitaVerse) {
        cache.put(makeKey(chapter, verse), verseData)
    }

    /**
     * Get from cache or fetch if missing.
     * Concurrent callers for the same (chapter, verse) share one in-flight fetch.
     */
    suspend fun getOrFetch(
        chapter: Int,
        verse: Int,
        fetch: suspend () -> GitaVerse?
    ): GitaVerse? {
        val cached = get(chapter, verse)
        if (cached != null) return cached

        val key = makeKey(chapter, verse)
        val mutex = inFlightMutexes.getOrPut(key) { kotlinx.coroutines.sync.Mutex() }
        
        return mutex.withLock {
            val doubleCheck = get(chapter, verse)
            if (doubleCheck != null) {
                doubleCheck
            } else {
                val fetched = fetch()
                if (fetched != null) put(chapter, verse, fetched)
                fetched
            }
        }
    }

    /**
     * Clear all cached verses
     * Called on ViewModel cleanup to guarantee memory release
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * Get cache statistics for debugging
     */
    fun getStats(): String {
        return "VerseCacheManager: size=${cache.size() / 1024}KB, max=${cache.maxSize() / 1024}KB"
    }

    private fun makeKey(chapter: Int, verse: Int) = "$chapter:$verse"
}
