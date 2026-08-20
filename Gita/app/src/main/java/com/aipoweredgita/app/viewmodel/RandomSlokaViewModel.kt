package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.database.CachedVerseDao
import com.aipoweredgita.app.database.RandomVerseHistory
import com.aipoweredgita.app.database.RandomVerseHistoryDao
import com.aipoweredgita.app.network.GitaApi
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.util.GitaConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class RandomSlokaViewModel @Inject constructor(
    private val cachedVerseDao: CachedVerseDao,
    private val randomVerseHistoryDao: RandomVerseHistoryDao,
    private val statsRepository: StatsRepository,
) : ViewModel() {

    data class RandomSlokaUiState(
        val currentVerse: CachedVerse? = null,
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    )

    private val _uiState = MutableStateFlow(RandomSlokaUiState(isLoading = true))
    val uiState: StateFlow<RandomSlokaUiState> = _uiState.asStateFlow()

    fun loadVerse(initialChapter: Int, initialVerse: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (initialChapter > 0 && initialVerse > 0) {
                val verse = withContext(Dispatchers.IO) {
                    cachedVerseDao.getVerse(initialChapter, initialVerse)
                }
                if (verse != null) {
                    _uiState.update {
                        it.copy(currentVerse = verse, isLoading = false, errorMessage = null)
                    }
                } else {
                    try {
                        val apiVerse = withContext(Dispatchers.IO) {
                            GitaApi.retrofitService.getVerse(
                                GitaConstants.DEFAULT_LANGUAGE,
                                initialChapter,
                                initialVerse
                            )
                        }
                        val cached = CachedVerse(
                            chapterNo = apiVerse.chapterNo,
                            verseNo = apiVerse.verseNo,
                            chapterName = apiVerse.chapterName,
                            verse = apiVerse.verse,
                            translation = apiVerse.translation,
                            meaning = apiVerse.meaning,
                            explanation = apiVerse.explanation
                        )
                        withContext(Dispatchers.IO) { cachedVerseDao.insertVerse(cached) }
                        _uiState.update {
                            it.copy(currentVerse = cached, isLoading = false, errorMessage = null)
                        }
                    } catch (e: Exception) {
                        generateNewSloka(force = true)
                    }
                }
            } else {
                generateNewSloka(force = true)
            }
        }
    }

    /**
     * @param force when false, ignores taps while already loading (refresh debounce).
     */
    fun generateNewSloka(force: Boolean = false) {
        if (!force && _uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            withContext(Dispatchers.IO) {
                var verseData = cachedVerseDao.getRandomVerse()
                if (verseData == null && cachedVerseDao.getCachedCount() > 0) {
                    randomVerseHistoryDao.clearHistory()
                    verseData = cachedVerseDao.getRandomVerse()
                }
                if (verseData != null) {
                    randomVerseHistoryDao.insertShownVerse(
                        RandomVerseHistory(
                            chapterNo = verseData.chapterNo,
                            verseNo = verseData.verseNo
                        )
                    )
                    _uiState.update {
                        it.copy(currentVerse = verseData, isLoading = false, errorMessage = null)
                    }
                } else {
                    try {
                        val fallback = GitaApi.retrofitService.getVerse(
                            GitaConstants.DEFAULT_LANGUAGE, 2, 47
                        )
                        val cached = CachedVerse(
                            chapterNo = fallback.chapterNo,
                            verseNo = fallback.verseNo,
                            chapterName = fallback.chapterName,
                            verse = fallback.verse,
                            translation = fallback.translation,
                            meaning = fallback.meaning,
                            explanation = fallback.explanation
                        )
                        cachedVerseDao.insertVerse(cached)
                        _uiState.update {
                            it.copy(currentVerse = cached, isLoading = false, errorMessage = null)
                        }
                    } catch (e: Exception) {
                        // Prefer showing Retry over an English hard-coded stub in a Telugu app.
                        // If we already have a previous verse on screen, keep it and surface the error.
                        val previous = _uiState.value.currentVerse
                        if (previous != null) {
                            _uiState.update {
                                it.copy(isLoading = false, errorMessage = e.message ?: "load_failed")
                            }
                        } else {
                            // Telugu offline seed so first launch offline still teaches Karma Yoga
                            val fallbackCached = CachedVerse(
                                chapterNo = 2,
                                verseNo = 47,
                                chapterName = "సాంఖ్య యోగం",
                                verse = "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन।\nमा कर्मफलहेतुर्भूर्मा ते सङ्गोऽस्त्वकर्मणि॥",
                                translation = "నీకు నిర్ణీత కర్తవ్యాలు నిర్వర్తించే అధికారం ఉంది, కానీ వాటి ఫలితాలపై నీకు హక్కు లేదు. ఫలాపేక్షతో చర్యలు చేయకు; అలాగే నిష్క్రియత్వానికి అంటుకోకు.",
                                meaning = "కర్మయోగం — ఫలాసక్తి లేకుండా కర్తవ్యం నిర్వర్తించు.",
                                explanation = "శ్రీకృష్ణుడు అర్జునునికి చెప్పిన కర్మయోగ సారం: కర్తవ్యం నిర్వర్తించే హక్కు ఉంది, కానీ ఫలితాలపై హక్కు లేదు. ఫలాసక్తి లేకుండా, సోమరితనం లేకుండా చర్య చేయాలి."
                            )
                            _uiState.update {
                                it.copy(
                                    currentVerse = fallbackCached,
                                    isLoading = false,
                                    errorMessage = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun retry() {
        generateNewSloka(force = true)
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /** Text suitable for Telugu TTS (translation only — not Devanagari). */
    fun ttsTextFor(verse: CachedVerse): String {
        val translation = verse.translation.trim()
        val meaning = verse.meaning.trim()
        return buildString {
            append("భగవద్గీత అధ్యాయం ${verse.chapterNo}, శ్లోకం ${verse.verseNo}. ")
            if (translation.isNotBlank()) append(translation)
            if (meaning.isNotBlank()) {
                append(". అర్థం: ")
                append(meaning)
            }
        }
    }

    fun sharePlainText(verse: CachedVerse): String =
        buildString {
            appendLine("Bhagavad Gita Chapter ${verse.chapterNo}, Verse ${verse.verseNo}:")
            appendLine()
            appendLine(verse.verse)
            appendLine()
            appendLine("Translation:")
            appendLine(verse.translation)
            if (verse.meaning.isNotBlank()) {
                appendLine()
                appendLine("Meaning:")
                appendLine(verse.meaning)
            }
            appendLine()
            append("Shared via AI Powered Gita App")
        }

    /**
     * Award coins only after the share sheet returns successfully (caller must gate on RESULT_OK).
     */
    fun onShareCompleted(chapter: Int, verse: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val awarded = statsRepository.trackSlokaShared(chapter = chapter, verse = verse)
                if (awarded > 0) {
                    onComplete("shared_ok_coins:$awarded")
                } else {
                    onComplete("shared_ok")
                }
            } catch (_: Exception) {
                onComplete("shared_ok")
            }
        }
    }

    /** @deprecated Prefer [onShareCompleted] after RESULT_OK. */
    fun trackSlokaShared(chapter: Int, verse: Int, onComplete: (String) -> Unit) {
        onShareCompleted(chapter, verse, onComplete)
    }
}
