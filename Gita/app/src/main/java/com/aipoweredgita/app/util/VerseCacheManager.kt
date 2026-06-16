package com.aipoweredgita.app.util

import android.util.LruCache
import com.aipoweredgita.app.data.GitaVerse

/**
 * Bounded memory cache for verses using LRU eviction.
 * Prevents unbounded memory growth from accumulated verse objects.
 *
 * Automatically evicts oldest verses when memory limit is reached.
 * Max size: 5MB (approximately 10,000 verses)
 */
class VerseCacheManager(maxSizeKb: Int = 5000) {

    private val cache = object : LruCache<String, GitaVerse>(maxSizeKb) {
        override fun sizeOf(key: String, value: GitaVerse): Int {
            // Estimate verse size: verse text + translation + purport + metadata
            val verseSize = value.verse.length
            val translationSize = value.translation.length
            val purportSize = value.purport.sumOf { it.length }
            return verseSize + translationSize + purportSize + 200  // 200 bytes for metadata
        }
    }

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
     * Get from cache or fetch if missing
     */
    suspend fun getOrFetch(
        chapter: Int,
        verse: Int,
        fetch: suspend () -> GitaVerse?
    ): GitaVerse? {
        val cached = get(chapter, verse)
        if (cached != null) return cached

        val fetched = fetch()
        if (fetched != null) {
            put(chapter, verse, fetched)
        }
        return fetched
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
        return "VerseCacheManager: size=${cache.size()}KB, max=${cache.maxSize()}KB"
    }

    private fun makeKey(chapter: Int, verse: Int) = "$chapter:$verse"
}
