package com.aipoweredgita.app.viewmodel

import android.app.Application
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
    private val application: Application
) : ViewModel() {

    data class RandomSlokaUiState(
        val currentVerse: CachedVerse? = null,
        val isLoading: Boolean = false
    )

    private val _uiState = MutableStateFlow(RandomSlokaUiState())
    val uiState: StateFlow<RandomSlokaUiState> = _uiState.asStateFlow()

    fun loadVerse(initialChapter: Int, initialVerse: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            if (initialChapter > 0 && initialVerse > 0) {
                val verse = withContext(Dispatchers.IO) {
                    cachedVerseDao.getVerse(initialChapter, initialVerse)
                }
                if (verse != null) {
                    _uiState.update { it.copy(currentVerse = verse, isLoading = false) }
                } else {
                    try {
                        val apiVerse = withContext(Dispatchers.IO) {
                            GitaApi.retrofitService.getVerse(GitaConstants.DEFAULT_LANGUAGE, initialChapter, initialVerse)
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
                        withContext(Dispatchers.IO) {
                            cachedVerseDao.insertVerse(cached)
                        }
                        _uiState.update { it.copy(currentVerse = cached, isLoading = false) }
                    } catch (e: Exception) {
                        generateNewSloka()
                    }
                }
            } else {
                generateNewSloka()
            }
        }
    }

    fun generateNewSloka() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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
                    _uiState.update { it.copy(currentVerse = verseData, isLoading = false) }
                } else {
                    try {
                        val fallback = GitaApi.retrofitService.getVerse(GitaConstants.DEFAULT_LANGUAGE, 2, 47)
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
                        _uiState.update { it.copy(currentVerse = cached, isLoading = false) }
                    } catch (e: Exception) {
                        val fallbackCached = CachedVerse(
                            chapterNo = 2,
                            verseNo = 47,
                            chapterName = "Bhagavad Gita",
                            verse = "कर्मण्येवाधिकारस्ते मा फलेषु कदाचन।\nमा कर्मफलहेतुर्भूर्मा ते सङ्गोऽस्त्वकर्मणि॥",
                            translation = "You have a right to perform your prescribed duties, but you are not entitled to the fruits of your actions.",
                            meaning = "Karma yoga",
                            explanation = "This is the core verse of Karma Yoga. Sri Krishna explains that a seeker has a right to perform prescribed duties, but has no claim on the results of those actions. One should perform their duty without attachment to the outcome, and without developing an attitude of laziness or inaction."
                        )
                        _uiState.update { it.copy(currentVerse = fallbackCached, isLoading = false) }
                    }
                }
            }
        }
    }

    fun trackSlokaShared(chapter: Int, verse: Int, onComplete: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val awarded = statsRepository.trackSlokaShared(chapter = chapter, verse = verse)
                if (awarded > 0) {
                    onComplete("Shared successfully! Earned $awarded coins 🪙")
                } else {
                    onComplete("Shared successfully!")
                }
            } catch (e: Exception) {
                onComplete("Shared successfully!")
            }
        }
    }
}
