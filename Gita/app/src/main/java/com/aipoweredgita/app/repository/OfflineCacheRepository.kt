package com.aipoweredgita.app.repository

import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.database.CachedVerseDao
import com.aipoweredgita.app.network.GitaApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class OfflineCacheRepository(private val cachedVerseDao: CachedVerseDao) {

    // Chapter verse counts (standard Bhagavad Gita)
    private val chapterVerseCounts = com.aipoweredgita.app.util.GitaConstants.CHAPTER_VERSE_COUNTS

    private val language = com.aipoweredgita.app.util.GitaConstants.DEFAULT_LANGUAGE
    private val totalVerses = com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES // Traditional Bhagavad Gita verse count

    suspend fun getVerse(chapter: Int, verse: Int): GitaVerse? {
        // Try cache first
        val cached = cachedVerseDao.getVerse(chapter, verse)
        if (cached != null) {
            return cached.toGitaVerse()
        }

        // If not cached, fetch from API using raw endpoint to handle combined verses
        return try {
            val rawVerseData = GitaApi.retrofitService.getVerseRaw(language, chapter, verse)
            
            // Convert to individual verses (separates combined verses)
            val individualVerses = rawVerseData.toIndividualVerses()
            
            // If API returned verse text as an array but verse_no isn't an array, fix verse numbers
            val correctedVerses = if (rawVerseData.verse is List<*> && rawVerseData.verseNo !is List<*>) {
                individualVerses.mapIndexed { idx, v -> v.copy(verseNo = verse + idx) }
            } else individualVerses
            
            // Save all individual verses to cache using batch insert
            val cachedVerses = correctedVerses.map { CachedVerse.fromGitaVerse(it) }
            try {
                cachedVerseDao.insertVerses(cachedVerses)
            } catch (e: Exception) {
                // Fallback to individual inserts if batch fails
                for (individualVerse in correctedVerses) {
                    try {
                        cachedVerseDao.insertVerse(CachedVerse.fromGitaVerse(individualVerse))
                    } catch (e: Exception) {
                        // Ignore duplicates
                        if (e.message?.contains("UNIQUE constraint") != true) {
                            throw e
                        }
                    }
                }
            }
            
            // Return the specific verse that was requested
            correctedVerses.find { it.verseNo == verse } ?: correctedVerses.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getCachedCount(): Flow<Int> = cachedVerseDao.getCachedCountFlow()

    suspend fun getCachedCountSync(): Int = cachedVerseDao.getCachedCount()

    suspend fun isFullyCached(): Boolean = cachedVerseDao.getCachedCount() >= totalVerses

    fun downloadAllVerses(): Flow<DownloadProgress> = flow {
        var newlyDownloaded = 0
        var failed = 0
        val failedVerses = mutableListOf<Pair<Int, Int>>() // Store as chapter, verse pairs

        // Emit initial progress
        val initialCached = cachedVerseDao.getCachedCount()
        emit(DownloadProgress(
            initialCached,
            totalVerses,
            (initialCached * 100) / totalVerses,
            DownloadStatus.DOWNLOADING,
            if (initialCached > 0) "Resuming... $initialCached verses already cached" else "Starting download..."
        ))

        // Load all cached verse IDs at once for fast lookup
        val cachedIds = cachedVerseDao.getAllCachedVerseIds().toMutableSet()

        // First pass: download all verses using chapter/verse (reliable)
        for (chapter in 1..18) {
            val maxVerses = chapterVerseCounts[chapter] ?: 0
            for (verse in 1..maxVerses) {
                try {
                    // Fast check using in-memory Set instead of database query
                    val verseId = "$chapter:$verse"
                    if (!cachedIds.contains(verseId)) {
                        // Try to download new verse using raw endpoint
                        try {
                            val rawVerseData = GitaApi.retrofitService.getVerseRaw(language, chapter, verse)

                            // Convert to individual verses (handles grouped verses)
                            val individualVerses = rawVerseData.toIndividualVerses()
                            // If API returned verse text as an array but verse_no isn't an array, fix verse numbers as a contiguous range
                            val correctedVerses = if (rawVerseData.verse is List<*> && rawVerseData.verseNo !is List<*>) {
                                individualVerses.mapIndexed { idx, v -> v.copy(verseNo = verse + idx) }
                            } else individualVerses
                            // If all verses from this fetch are already cached (e.g., resuming), skip emitting progress
                            val allCached = correctedVerses.all { cachedIds.contains("${it.chapterNo}:${it.verseNo}") }
                            if (allCached) {
                                continue
                            }

                            // Collect verses for batch insert
                            val newCachedVerses = mutableListOf<CachedVerse>()
                            for (individualVerse in correctedVerses) {
                                newCachedVerses.add(CachedVerse.fromGitaVerse(individualVerse))
                                cachedIds.add("${individualVerse.chapterNo}:${individualVerse.verseNo}")
                            }
                            
                            // Batch insert all verses at once
                            try {
                                cachedVerseDao.insertVerses(newCachedVerses)
                                newlyDownloaded += newCachedVerses.size
                            } catch (e: Exception) {
                                // Fallback to individual inserts if batch fails
                                for (individualVerse in correctedVerses) {
                                    try {
                                        cachedVerseDao.insertVerse(CachedVerse.fromGitaVerse(individualVerse))
                                        newlyDownloaded++
                                    } catch (e: Exception) {
                                        // Ignore duplicates
                                        if (e.message?.contains("UNIQUE constraint") != true) {
                                            throw e
                                        }
                                    }
                                }
                            }

                            val currentCached = initialCached + newlyDownloaded
                            val progress = (currentCached * 100) / totalVerses
                            emit(DownloadProgress(
                                currentCached,
                                totalVerses,
                                progress,
                                DownloadStatus.DOWNLOADING,
                                "Ch.$chapter:$verse • $currentCached/$totalVerses"
                            ))

                            // Small delay to prevent overwhelming the server
                            kotlinx.coroutines.delay(50)
                        } catch (e: Exception) {
                            // Any error - mark for retry (don't skip)
                            failed++
                            failedVerses.add(Pair(chapter, verse))
                        }
                    }
                    // Silently skip already-cached verses (no UI lag)
                } catch (e: Exception) {
                    // Unexpected error during cache check
                    failed++
                    failedVerses.add(Pair(chapter, verse))
                }
            }
        }

        // Second pass: retry failed verses (up to 3 attempts with longer delays)
        if (failedVerses.isNotEmpty()) {
            val currentCached = initialCached + newlyDownloaded
            emit(DownloadProgress(
                currentCached,
                totalVerses,
                (currentCached * 100) / totalVerses,
                DownloadStatus.DOWNLOADING,
                "Retrying ${failedVerses.size} failed verses..."
            ))

            val stillFailed = mutableListOf<Pair<Int, Int>>()
            for ((chapter, verse) in failedVerses) {
                var retryAttempts = 0
                var success = false

                while (retryAttempts < 3 && !success) {
                    try {
                        // Check if it got cached somehow
                        if (!cachedVerseDao.isVerseCached(chapter, verse)) {
                            // Longer delay for retries (1s, 2s, 3s)
                            kotlinx.coroutines.delay(1000L * (retryAttempts + 1))

                            val rawVerseData = GitaApi.retrofitService.getVerseRaw(language, chapter, verse)
                            val individualVerses = rawVerseData.toIndividualVerses()
                            val correctedVersesRetry = if (rawVerseData.verse is List<*> && rawVerseData.verseNo !is List<*>) {
                                individualVerses.mapIndexed { idx, v -> v.copy(verseNo = verse + idx) }
                            } else individualVerses
                            // Skip if all verses in this group are already cached
                            val allCachedRetry = correctedVersesRetry.all { cachedVerseDao.isVerseCached(chapter, it.verseNo) }
                            if (allCachedRetry) {
                                success = true
                                failed--
                                continue
                            }

                            // Save each individual verse and update in-memory cache set
                            for (individualVerse in correctedVersesRetry) {
                                try {
                                    cachedVerseDao.insertVerse(CachedVerse.fromGitaVerse(individualVerse))
                                    newlyDownloaded++
                                    failed--
                                    cachedIds.add("${individualVerse.chapterNo}:${individualVerse.verseNo}")
                                } catch (e: Exception) {
                                    // Ignore duplicates
                                    if (e.message?.contains("UNIQUE constraint") != true) {
                                        throw e
                                    }
                                }
                            }

                            success = true

                            val updatedCached = initialCached + newlyDownloaded
                            emit(DownloadProgress(
                                updatedCached,
                                totalVerses,
                                (updatedCached * 100) / totalVerses,
                                DownloadStatus.DOWNLOADING,
                                "Retry: Ch.$chapter:$verse ✓"
                            ))
                        } else {
                            success = true // Already cached
                            failed--
                        }
                    } catch (e: Exception) {
                        retryAttempts++
                        if (retryAttempts >= 3) {
                            // Still failed after 3 attempts
                            stillFailed.add(Pair(chapter, verse))
                        } else {
                            // Will retry again
                            kotlinx.coroutines.delay(500L)
                        }
                    }
                }
            }

            // Update failed list to only those still failed after retries
            failedVerses.clear()
            failedVerses.addAll(stillFailed)
        }

        // Get final count from database to ensure accuracy
        val finalCachedCount = cachedVerseDao.getCachedCount()
        val actualProgress = (finalCachedCount * 100) / totalVerses
        val finalStatus = when {
            failed == 0 && finalCachedCount >= totalVerses -> DownloadStatus.COMPLETED
            failed > 0 -> DownloadStatus.COMPLETED_WITH_ERRORS
            else -> DownloadStatus.COMPLETED_WITH_ERRORS
        }

        val finalMessage = buildString {
            append("Downloaded: $newlyDownloaded")
            append("\nTotal cached: $finalCachedCount/$totalVerses")
            if (failed > 0) {
                append("\nFailed: $failed verses")
                if (failedVerses.size <= 10) {
                    append("\nMissing: ${failedVerses.joinToString(", ") { "${it.first}:${it.second}" }}")
                }
            }
            if (finalCachedCount < totalVerses) {
                append("\n⚠ ${totalVerses - finalCachedCount} verses not available")
            } else {
                append("\n✓ All verses downloaded!")
            }
        }
        emit(DownloadProgress(finalCachedCount, totalVerses, actualProgress, finalStatus, finalMessage))
    }

    suspend fun clearCache() {
        cachedVerseDao.deleteAll()
    }

    fun searchCachedVerses(query: String): Flow<List<CachedVerse>> {
        return cachedVerseDao.searchVerses(query)
    }

    // Get list of missing verses for debugging/verification
    suspend fun getMissingVerses(): List<Pair<Int, Int>> {
        // Build a fast lookup set of cached IDs to avoid per-verse DB checks
        val cachedIds = cachedVerseDao.getAllCachedVerseIds().toSet()
        val missing = mutableListOf<Pair<Int, Int>>()
        for (chapter in 1..18) {
            val maxVerses = chapterVerseCounts[chapter] ?: 0
            for (verse in 1..maxVerses) {
                val id = "$chapter:$verse"
                if (!cachedIds.contains(id)) {
                    missing.add(Pair(chapter, verse))
                }
            }
        }
        return missing
    }
}

data class DownloadProgress(
    val current: Int,
    val total: Int,
    val percentage: Int,
    val status: DownloadStatus,
    val message: String? = null
)

enum class DownloadStatus {
    IDLE,
    DOWNLOADING,
    COMPLETED,
    COMPLETED_WITH_ERRORS,
    ERROR,
    CANCELLED
}
