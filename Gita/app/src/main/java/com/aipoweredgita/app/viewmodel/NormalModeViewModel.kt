package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.repository.GitaRepository
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.repository.FavoriteRepository
import com.aipoweredgita.app.util.TimeTracker
import com.aipoweredgita.app.util.GitaConstants
import com.aipoweredgita.app.util.ThrottledDatabaseUpdater
import com.aipoweredgita.app.repository.ModeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.Stable

@Stable
data class NormalModeState(
    val verse: GitaVerse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChapter: Int = 1,
    val currentVerse: Int = 1,
    val isFavorite: Boolean = false,
    val favoriteMessage: String? = null,
    // If API returns grouped verses (arrays), keep full set here
    val combinedVerseNos: List<Int> = emptyList(),
    // Chapter-level combined groups (each list contains the verses in a group)
    val combinedGroups: List<List<Int>> = emptyList(),
    // Note to show when viewing separated verses
    val separatedVerseNote: String? = null
)

class NormalModeViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "NormalModeViewModel"
    private val _state = MutableStateFlow(NormalModeState())
    val state: StateFlow<NormalModeState> = _state.asStateFlow()

    private val favoriteRepository: FavoriteRepository
    private val offlineCacheRepository: com.aipoweredgita.app.repository.OfflineCacheRepository
    private lateinit var statsRepository: StatsRepository
    private lateinit var database: GitaDatabase
    private lateinit var readVerseDao: com.aipoweredgita.app.database.ReadVerseDao
    private lateinit var yogaProgressionRepository: com.aipoweredgita.app.repository.YogaProgressionRepository
    private val language = GitaConstants.DEFAULT_LANGUAGE
    private val gitaRepository = GitaRepository()
    private var lastRequestedChapter: Int = 1
    private var lastRequestedVerse: Int = 1

    // Throttled database updater - batches verse reads to reduce I/O
    private lateinit var throttledUpdater: ThrottledDatabaseUpdater

    // Time tracker
    private val timeTracker = TimeTracker { seconds ->
        viewModelScope.launch(Dispatchers.IO) {
            statsRepository.trackModeTime(seconds, ModeType.NORMAL)
            try {
                val today = java.time.LocalDate.now().toString()
                val dailyDao = database.dailyActivityDao()
                dailyDao.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
                dailyDao.addNormalSeconds(today, seconds)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update daily normal seconds", e)
            }
        }
    }

    init {
        database = GitaDatabase.getDatabase(application)
        favoriteRepository = FavoriteRepository(database.favoriteVerseDao())
        offlineCacheRepository = com.aipoweredgita.app.repository.OfflineCacheRepository(database.cachedVerseDao())
        statsRepository = StatsRepository(database.userStatsDao())
        readVerseDao = database.readVerseDao()
        yogaProgressionRepository = com.aipoweredgita.app.repository.YogaProgressionRepository(database.yogaProgressionDao())

        // Initialize throttled updater for batched database writes
        throttledUpdater = ThrottledDatabaseUpdater(
            batchSize = 10,
            flushIntervalMs = 5000L
        ) { batch ->
            // Batch write callback
            try {
                val today = java.time.LocalDate.now().toString()

                // Insert all verse reads in batch
                batch.forEach { verseRead ->
                    readVerseDao.insert(
                        com.aipoweredgita.app.database.ReadVerse(
                            chapterNo = verseRead.chapter,
                            verseNo = verseRead.verse,
                            date = today
                        )
                    )
                }

                // Update distinct verses count
                val distinct = readVerseDao.distinctVersePairs()
                database.userStatsDao().updateDistinctVersesRead(distinct)

                // Record in daily activity
                val dailyDao = database.dailyActivityDao()
                dailyDao.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
                dailyDao.addVerses(today, batch.size)

                Log.d(TAG, "Batch write completed: ${batch.size} verses")
            } catch (e: Exception) {
                Log.e(TAG, "Error in batch write: ${e.message}")
                throw e
            }
        }

        // Start time tracking
        timeTracker.start()

        // Auto-retry on reconnect if previous load failed due to offline
        viewModelScope.launch {
            com.aipoweredgita.app.utils.NetworkUtils.networkStatusFlow(getApplication())
                .collect { online ->
                    if (online && _state.value.error?.contains("Offline", ignoreCase = true) == true) {
                        loadVerse(lastRequestedChapter, lastRequestedVerse)
                    }
                }
        }
    }

    // Chapter verse counts (18 chapters)
    private val chapterVerseCounts = GitaConstants.CHAPTER_VERSE_COUNTS

    init {
        loadVerse(1, 1)
    }

    private fun computeChapterCombinedGroups(chapter: Int) {
        // DISABLED: We want all verses to be separate, so don't compute combined groups
        // This function was causing navigation issues by marking verses as combined
        // when they should be treated as individual verses
        
        // Always set combinedGroups to empty list
        viewModelScope.launch {
            _state.value = _state.value.copy(combinedGroups = emptyList())
        }
    }

    fun loadVerse(chapter: Int, verse: Int, retryCount: Int = 0, autoSkipCombined: Boolean = false) {
        viewModelScope.launch {
            lastRequestedChapter = chapter
            lastRequestedVerse = verse
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                currentChapter = chapter,
                currentVerse = verse,
                // Clear any previous combined group during loading to prevent stale UI
                combinedVerseNos = emptyList()
            )

            try {
                println("GitaApp: Loading Chapter $chapter, Verse $verse (Attempt ${retryCount + 1})")

                // Try cache first
                val verseData = offlineCacheRepository.getVerse(chapter, verse)

                if (verseData != null) {
                    println("GitaApp: Loaded from cache: ${verseData.verse.take(50)}")
                    
                    // Check if this verse was separated from a combined group
                    val note = if (verseData.wasSeparated && verseData.originalCombinedGroup.isNotEmpty()) {
                        val verseRange = "${verseData.originalCombinedGroup.first()}-${verseData.originalCombinedGroup.last()}"
                        "ℹ️ Note: Verses $verseRange were originally combined. We separated them for convenience, but the sloka summary is the same for all verses in this group."
                    } else null
                    
                    // All verses are treated as separate - no combined verse detection
                    _state.value = _state.value.copy(
                        verse = verseData,
                        isLoading = false,
                        error = null,
                        combinedVerseNos = emptyList(),  // Always empty - all verses are separate
                        separatedVerseNote = note
                    )
                    // Don't compute combined groups - all verses are separate
                    computeChapterCombinedGroups(chapter)
                    // Check favorite status
                    checkFavoriteStatus(chapter, verse)
                    // Track verse read
                    trackVerseRead()
                } else {
                    // Fallback to API if not cached
                    val online = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(getApplication())
                    if (!online) {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Offline: verse not downloaded. Use Offline mode or reconnect."
                        )
                        return@launch
                    }
                    println("GitaApp: Cache miss, fetching from API...")
                    val apiVerse = gitaRepository.getVerse(language, chapter, verse)
                    println("GitaApp: Successfully loaded from API: ${apiVerse.verse.take(50)}")
                    
                    // Check if this verse was separated from a combined group
                    val note = if (apiVerse.wasSeparated && apiVerse.originalCombinedGroup.isNotEmpty()) {
                        val verseRange = "${apiVerse.originalCombinedGroup.first()}-${apiVerse.originalCombinedGroup.last()}"
                        "ℹ️ Note: Verses $verseRange were originally combined. We separated them for convenience, but the sloka summary is the same for all verses in this group."
                    } else null
                    
                    // All verses are treated as separate - no combined verse detection
                    _state.value = _state.value.copy(
                        verse = apiVerse,
                        isLoading = false,
                        error = null,
                        combinedVerseNos = emptyList(),  // Always empty - all verses are separate
                        separatedVerseNote = note
                    )
                    computeChapterCombinedGroups(chapter)
                    // Check favorite status
                    checkFavoriteStatus(chapter, verse)
                    // Track verse read
                    trackVerseRead()
                }
            } catch (e: Exception) {
                println("GitaApp: Error loading verse - ${e::class.simpleName}: ${e.message}")
                e.printStackTrace()

                // Retry logic for transient errors (network, timeout)
                val isTransientError = e.message?.let {
                    it.contains("timeout", ignoreCase = true) ||
                    it.contains("Unable to resolve host", ignoreCase = true) ||
                    it.contains("SocketTimeoutException", ignoreCase = true)
                } ?: false

                if (isTransientError && retryCount < 2) {
                    println("GitaApp: Retrying... (Attempt ${retryCount + 2})")
                    kotlinx.coroutines.delay(1000) // Wait 1 second before retry
                    loadVerse(chapter, verse, retryCount + 1)
                    return@launch
                }

                val errorMsg = when {
                    // Network errors
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.contains("UnknownHostException", ignoreCase = true) == true ||
                    e.message?.contains("No address associated", ignoreCase = true) == true ->
                        "❌ No Internet Connection\n\nPlease check:\n• WiFi or mobile data is enabled\n• Internet is working"

                    // Timeout errors
                    e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("SocketTimeoutException", ignoreCase = true) == true ->
                        "⏱️ Connection Timeout\n\nThe server took too long to respond.\nPlease try again."

                    // API errors
                    e.message?.contains("404") == true ||
                    e.message?.contains("HTTP 404") == true ->
                        "❌ Verse Not Found\n\nChapter $chapter, Verse $verse may not exist.\nTry a different verse."

                    e.message?.contains("500") == true ||
                    e.message?.contains("HTTP 5") == true ->
                        "⚠️ Server Error\n\nThe API server is having issues.\nPlease try again later."

                    // JSON parsing errors
                    e.message?.contains("JsonSyntaxException", ignoreCase = true) == true ||
                    e.message?.contains("MalformedJsonException", ignoreCase = true) == true ->
                        "⚠️ Data Format Error\n\nReceived invalid data from server.\nPlease try again or try a different verse."

                    // Empty or null response
                    e.message?.contains("Expected", ignoreCase = true) == true &&
                    e.message?.contains("but was", ignoreCase = true) == true ->
                        "⚠️ Incomplete Data\n\nReceived incomplete verse data.\nPlease try another verse."

                    // Generic error with helpful info
                    else -> {
                        val errorType = e::class.simpleName ?: "Unknown"
                        "❌ Failed to Load Verse\n\nError: $errorType\n\nPossible solutions:\n• Check internet connection\n• Try again in a moment\n• Try a different verse\n\nDetails: ${e.message?.take(100) ?: "No details"}"
                    }
                }

                _state.value = _state.value.copy(
                    isLoading = false,
                    error = errorMsg
                )
            }
        }
    }

    fun nextVerse() {
        viewModelScope.launch {
            val current = _state.value
            val currentVerse = current.verse ?: return@launch
            val currentChapter = currentVerse.chapterNo
            val maxVersesInChapter = chapterVerseCounts[currentChapter] ?: 47
            
            // Simple increment - always go to next verse number
            val nextCandidate = currentVerse.verseNo + 1

            if (nextCandidate <= maxVersesInChapter) {
                loadVerse(currentChapter, nextCandidate)
            } else if (currentChapter < GitaConstants.MAX_CHAPTERS) {
                loadVerse(currentChapter + 1, 1)
            }
        }
    }


    fun previousVerse() {
        viewModelScope.launch {
            val current = _state.value
            val currentVerse = current.verse ?: return@launch
            val currentChapter = currentVerse.chapterNo
            val currentNo = currentVerse.verseNo

            // Simple decrement - always go to previous verse number
            val prevCandidate = currentNo - 1

            if (prevCandidate >= 1) {
                loadVerse(currentChapter, prevCandidate)
            } else if (currentChapter > 1) {
                val prevChapter = currentChapter - 1
                val lastVerseInPrevChapter = chapterVerseCounts[prevChapter] ?: 47
                loadVerse(prevChapter, lastVerseInPrevChapter)
            }
        }
    }


    fun goToChapter(chapter: Int) {
        if (chapter in 1..GitaConstants.MAX_CHAPTERS) {
            loadVerse(chapter, 1)
        }
    }

    private fun checkFavoriteStatus(chapter: Int, verse: Int) {
        viewModelScope.launch {
            favoriteRepository.isFavorite(chapter, verse).collect { isFav ->
                _state.value = _state.value.copy(isFavorite = isFav)
            }
        }
    }

    fun toggleFavorite() {
        val verse = _state.value.verse ?: return

        viewModelScope.launch {
            val result = if (_state.value.isFavorite) {
                favoriteRepository.removeFavorite(verse.chapterNo, verse.verseNo)
            } else {
                favoriteRepository.addFavorite(verse)
            }

            result.onSuccess { message ->
                _state.value = _state.value.copy(
                    favoriteMessage = message,
                    isFavorite = !_state.value.isFavorite
                )
                // Clear message after delay
                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(favoriteMessage = null)
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    favoriteMessage = error.message ?: "Operation failed"
                )
                kotlinx.coroutines.delay(2000)
                _state.value = _state.value.copy(favoriteMessage = null)
            }
        }
    }

    private fun trackVerseRead() {
        viewModelScope.launch {
            val verse = _state.value.verse ?: return@launch
            // Track stats
            statsRepository.trackVerseRead()
            
            // Update yoga progression and check for level up
            val (didLevelUp, newLevel) = yogaProgressionRepository.updateProgressionAndCheckLevelUp()
            if (didLevelUp && newLevel != null) {
                // Show level-up notification
                com.aipoweredgita.app.notifications.YogaLevelUpNotificationManager.showLevelUpNotification(
                    getApplication(),
                    newLevel
                )
            }
            
            // Track distinct verses read (throttled to reduce DB writes)
            val vNo = verse.verseNo
            if (verse.chapterNo in 1..18 && vNo >= 1) {
                throttledUpdater.trackVerseRead(verse.chapterNo, vNo)
            } else {
                throttledUpdater.trackVerseRead(verse.chapterNo, verse.verseNo)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Stop time tracking when ViewModel is cleared
        timeTracker.stop()
        // Flush any pending verse reads to database
        throttledUpdater.flush()
        Log.d(TAG, "NormalModeViewModel cleared - throttled updater flushed")
    }
}
                    

