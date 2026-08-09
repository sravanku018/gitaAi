package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.domain.model.NormalModeEvent
import com.aipoweredgita.app.domain.model.NormalModeSideEffect
import com.aipoweredgita.app.domain.model.NormalModeUiState
import com.aipoweredgita.app.repository.GitaRepository
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.repository.FavoriteRepository
import com.aipoweredgita.app.repository.OfflineCacheRepository
import com.aipoweredgita.app.repository.YogaProgressionRepository
import com.aipoweredgita.app.util.TimeTracker
import com.aipoweredgita.app.util.GitaConstants
import com.aipoweredgita.app.util.ThrottledDatabaseUpdater
import com.aipoweredgita.app.repository.ModeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@HiltViewModel
class NormalModeViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val favoriteRepository: FavoriteRepository,
    private val offlineCacheRepository: OfflineCacheRepository,
    private val yogaProgressionRepository: YogaProgressionRepository,
    private val readVerseDao: com.aipoweredgita.app.database.ReadVerseDao,
    private val userStatsDao: com.aipoweredgita.app.database.UserStatsDao,
    private val dailyActivityDao: com.aipoweredgita.app.database.DailyActivityDao,
    private val application: Application
) : ViewModel() {
    private val TAG = "NormalModeViewModel"
    private val _uiState = MutableStateFlow(NormalModeUiState())
    val uiState: StateFlow<NormalModeUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<NormalModeSideEffect>()
    val sideEffect: SharedFlow<NormalModeSideEffect> = _sideEffect.asSharedFlow()

    // Backward compatibility for existing UI
    val state: StateFlow<NormalModeUiState> = uiState
    val events: SharedFlow<String> = MutableSharedFlow<String>() // Stub for backward compatibility if needed

    private val language = GitaConstants.DEFAULT_LANGUAGE
    private val gitaRepository = GitaRepository()
    private var lastRequestedChapter: Int = 1
    private var lastRequestedVerse: Int = 1

    // Chapter verse counts (18 chapters)
    private val chapterVerseCounts = GitaConstants.CHAPTER_VERSE_COUNTS

    // Throttled database updater - batches verse reads to reduce I/O
    private val throttledUpdater = ThrottledDatabaseUpdater(
        batchSize = 10,
        flushIntervalMs = 5000L
    ) { batch ->
        try {
            val today = java.time.LocalDate.now().toString()
            batch.forEach { verseRead ->
                readVerseDao.insert(
                    com.aipoweredgita.app.database.ReadVerse(
                        chapterNo = verseRead.chapter,
                        verseNo = verseRead.verse,
                        date = today
                    )
                )
            }
            val distinct = readVerseDao.distinctVersePairs()
            userStatsDao.updateDistinctVersesRead(distinct)
            val dailyDao = dailyActivityDao
            dailyDao.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
            dailyDao.addVerses(today, batch.size)
            Log.d(TAG, "Batch write completed: ${batch.size} verses")
        } catch (e: Exception) {
            Log.e(TAG, "Error in batch write: ${e.message}")
            throw e
        }
    }

    // Time tracker — uses viewModelScope to prevent coroutine leaks
    private val timeTracker = TimeTracker(scope = viewModelScope) { seconds ->
        viewModelScope.launch(Dispatchers.IO) {
            statsRepository.trackModeTime(seconds, ModeType.NORMAL)
            try {
                val today = java.time.LocalDate.now().toString()
                dailyActivityDao.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
                dailyActivityDao.addNormalSeconds(today, seconds)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to update daily normal seconds", e)
            }
        }
    }

    init {
        timeTracker.start()
        viewModelScope.launch {
            com.aipoweredgita.app.utils.NetworkUtils.networkStatusFlow(application)
                .collect { online ->
                    if (online && _uiState.value.error?.contains("Offline", ignoreCase = true) == true) {
                        loadVerse(lastRequestedChapter, lastRequestedVerse)
                    }
                }
        }
        val prefs = application.getSharedPreferences("reading_prefs", android.content.Context.MODE_PRIVATE)
        val savedChapter = prefs.getInt("last_read_chapter", 1)
        val savedVerse = prefs.getInt("last_read_verse", 1)
        val savedLang = prefs.getString("selected_language", "TE") ?: "TE"
        _uiState.update { it.copy(selectedLanguage = savedLang) }
        loadVerse(savedChapter, savedVerse)
    }

    private fun computeChapterCombinedGroups(chapter: Int) {
        // DISABLED: We want all verses to be separate, so don't compute combined groups
        // This function was causing navigation issues by marking verses as combined
        // when they should be treated as individual verses
        
        // Always set combinedGroups to empty list
        viewModelScope.launch {
            _uiState.update { it.copy(combinedGroups = emptyList()) }
        }
    }

    fun onEvent(event: NormalModeEvent) {
        when (event) {
            is NormalModeEvent.LoadVerse -> loadVerse(event.chapter, event.verse, event.retryCount, event.autoSkipCombined)
            is NormalModeEvent.NextVerse -> nextVerse()
            is NormalModeEvent.PreviousVerse -> previousVerse()
            is NormalModeEvent.GoToChapter -> goToChapter(event.chapter)
            is NormalModeEvent.ToggleFavorite -> toggleFavorite()
            is NormalModeEvent.TrackShare -> trackShare()
            is NormalModeEvent.ToggleLanguage -> {
                val newLang = event.language
                val prefs = application.getSharedPreferences("reading_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putString("selected_language", newLang).apply()
                _uiState.update { it.copy(selectedLanguage = newLang) }
                loadVerse(lastRequestedChapter, lastRequestedVerse)
            }
        }
    }

    fun loadVerse(chapter: Int, verse: Int, retryCount: Int = 0, autoSkipCombined: Boolean = false) {
        viewModelScope.launch {
            lastRequestedChapter = chapter
            lastRequestedVerse = verse

            // Save last read position for auto-resuming across app restarts
            try {
                val prefs = application.getSharedPreferences("reading_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit()
                    .putInt("last_read_chapter", chapter)
                    .putInt("last_read_verse", verse)
                    .apply()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to save last read position: ${e.message}")
            }

            _uiState.update {
                it.copy(
                    isLoading = true,
                    error = null,
                    currentChapter = chapter,
                    currentVerse = verse,
                    // Clear any previous combined group during loading to prevent stale UI
                    combinedVerseNos = emptyList()
                )
            }

            try {
                Log.d(TAG, "Loading Chapter $chapter, Verse $verse (Attempt ${retryCount + 1})")

                // Try cache first
                val verseData = offlineCacheRepository.getVerse(chapter, verse)

                if (verseData != null) {
                    Log.d(TAG, "Loaded from cache: ${verseData.verse.take(50)}")
                    
                    val engManager = com.aipoweredgita.app.util.EnglishTranslationAssetManager.getInstance(application)
                    val currentLang = _uiState.value.selectedLanguage

                    val finalVerse = if (currentLang == "EN") {
                        engManager.enrichVerseWithEnglish(verseData)
                    } else {
                        verseData
                    }
                    
                    // Check if this verse was separated from a combined group
                    val note = if (finalVerse.wasSeparated && finalVerse.originalCombinedGroup.isNotEmpty()) {
                        val verseRange = "${finalVerse.originalCombinedGroup.first()}-${finalVerse.originalCombinedGroup.last()}"
                        "ℹ️ Note: Verses $verseRange were originally combined. We separated them for convenience, but the sloka summary is the same for all verses in this group."
                    } else null
                    
                    // All verses are treated as separate - no combined verse detection
                    _uiState.update {
                        it.copy(
                            verse = finalVerse,
                            isLoading = false,
                            error = null,
                            combinedVerseNos = emptyList(),  // Always empty - all verses are separate
                            separatedVerseNote = note
                        )
                    }
                    // Don't compute combined groups - all verses are separate
                    computeChapterCombinedGroups(chapter)
                    // Check favorite status
                    checkFavoriteStatus(chapter, verse)
                    // Track verse read
                    trackVerseRead()
                } else {
                    // Fallback to API if not cached
                    val online = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(application)
                    val engManager = com.aipoweredgita.app.util.EnglishTranslationAssetManager.getInstance(application)
                    
                    if (!online) {
                        val fallbackEngText = engManager.getTranslation(chapter, verse)
                        if (!fallbackEngText.isNullOrBlank()) {
                            val offlineVerse = GitaVerse(
                                chapterNo = chapter,
                                verseNo = verse,
                                chapterName = "Chapter $chapter",
                                verse = "Chapter $chapter, Verse $verse",
                                translation = fallbackEngText,
                                purport = listOf(fallbackEngText)
                            )
                            _uiState.update {
                                it.copy(
                                    verse = offlineVerse,
                                    isLoading = false,
                                    error = null,
                                    combinedVerseNos = emptyList()
                                )
                            }
                            return@launch
                        }
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = "Offline: verse not downloaded. Use Offline mode or reconnect."
                            )
                        }
                        return@launch
                    }
                    Log.d(TAG, "Cache miss, fetching from API...")
                    val fetchedVerse = gitaRepository.getVerse(language, chapter, verse)
                    val apiVerse = engManager.enrichVerse(fetchedVerse)
                    Log.d(TAG, "Successfully loaded from API: ${apiVerse.verse.take(50)}")
                    
                    // Check if this verse was separated from a combined group
                    val note = if (apiVerse.wasSeparated && apiVerse.originalCombinedGroup.isNotEmpty()) {
                        val verseRange = "${apiVerse.originalCombinedGroup.first()}-${apiVerse.originalCombinedGroup.last()}"
                        "ℹ️ Note: Verses $verseRange were originally combined. We separated them for convenience, but the sloka summary is the same for all verses in this group."
                    } else null
                    
                    // All verses are treated as separate - no combined verse detection
                    _uiState.update {
                        it.copy(
                            verse = apiVerse,
                            isLoading = false,
                            error = null,
                            combinedVerseNos = emptyList(),  // Always empty - all verses are separate
                            separatedVerseNote = note
                        )
                    }
                    computeChapterCombinedGroups(chapter)
                    // Check favorite status
                    checkFavoriteStatus(chapter, verse)
                    // Track verse read
                    trackVerseRead()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading verse - ${e::class.simpleName}: ${e.message}", e)

                // Retry logic for transient errors (network, timeout)
                val isTransientError = e.message?.let {
                    it.contains("timeout", ignoreCase = true) ||
                    it.contains("Unable to resolve host", ignoreCase = true) ||
                    it.contains("SocketTimeoutException", ignoreCase = true)
                } ?: false

                if (isTransientError && retryCount < 2) {
                    Log.d(TAG, "Retrying... (Attempt ${retryCount + 2})")
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

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
            }
        }
    }

    fun nextVerse() {
        viewModelScope.launch {
            val current = _uiState.value
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
            val current = _uiState.value
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

    // Track the current favorite status collector job so we can cancel it on verse change
    private var favoriteJob: kotlinx.coroutines.Job? = null

    private fun checkFavoriteStatus(chapter: Int, verse: Int) {
        favoriteJob?.cancel()
        favoriteJob = viewModelScope.launch {
            favoriteRepository.isFavorite(chapter, verse).collect { isFav ->
                _uiState.update { it.copy(isFavorite = isFav) }
            }
        }
    }

    fun toggleFavorite() {
        val verse = _uiState.value.verse ?: return

        viewModelScope.launch {
            val result = if (_uiState.value.isFavorite) {
                favoriteRepository.removeFavorite(verse.chapterNo, verse.verseNo)
            } else {
                favoriteRepository.addFavorite(verse)
            }

            result.onSuccess { message ->
                _uiState.update {
                    it.copy(
                        favoriteMessage = message,
                        isFavorite = !it.isFavorite
                    )
                }
                // Clear message after delay
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(favoriteMessage = null) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        favoriteMessage = error.message ?: "Operation failed"
                    )
                }
                kotlinx.coroutines.delay(2000)
                _uiState.update { it.copy(favoriteMessage = null) }
            }
        }
    }

    private fun trackVerseRead() {
        viewModelScope.launch {
            val verse = _uiState.value.verse ?: return@launch
            // Track stats
            statsRepository.trackVerseRead()
            
            // Update yoga progression and check for level up
            val (didLevelUp, newLevel) = yogaProgressionRepository.updateProgressionAndCheckLevelUp()
            if (didLevelUp && newLevel != null) {
                // Show level-up notification
                com.aipoweredgita.app.notifications.YogaLevelUpNotificationManager.showLevelUpNotification(
                    application,
                    newLevel
                )
            }
            
            // Track distinct verses read (throttled to reduce DB writes)
            val vNo = verse.verseNo
            
            // Check for chapter completion — flush throttled writes first to get accurate count
            throttledUpdater.flush()
            kotlinx.coroutines.delay(200) // Brief pause for flush to complete
            val currentReadCount = readVerseDao.getReadVersesCountByChapter(verse.chapterNo)
            val totalVersesInChapter = chapterVerseCounts[verse.chapterNo] ?: 47
            
            if (currentReadCount >= totalVersesInChapter) {
                // Potential chapter completion.
                val prefs = application.getSharedPreferences("chapter_stats", android.content.Context.MODE_PRIVATE)
                val isCompleted = prefs.getBoolean("chapter_${verse.chapterNo}_completed", false)
                if (!isCompleted) {
                    statsRepository.trackChapterCompleted(verse.chapterNo)
                    prefs.edit().putBoolean("chapter_${verse.chapterNo}_completed", true).apply()
                    _sideEffect.emit(NormalModeSideEffect.ShowMessage("Chapter ${verse.chapterNo} Completed! \uD83E\uDE99 Sacred Coins awarded."))
                }
            }

            throttledUpdater.trackVerseRead(verse.chapterNo, verse.verseNo)
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

    private fun trackShare() {
        viewModelScope.launch {
            val verse = _uiState.value.verse
            statsRepository.trackSlokaShared(
                chapter = verse?.chapterNo,
                verse = verse?.verseNo
            )
        }
    }
}
                    

