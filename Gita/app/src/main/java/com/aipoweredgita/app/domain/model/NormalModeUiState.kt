package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.data.GitaVerse

data class NormalModeUiState(
    val verse: GitaVerse? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val currentChapter: Int = 1,
    val currentVerse: Int = 1,
    val isFavorite: Boolean = false,
    val favoriteMessage: String? = null,
    val combinedVerseNos: List<Int> = emptyList(),
    val combinedGroups: List<List<Int>> = emptyList(),
    val separatedVerseNote: String? = null,
    val selectedLanguage: String = "TE",
    /** Cached verses for the current chapter (swipe/scroll modes). */
    val chapterVerses: List<GitaVerse> = emptyList(),
    val isChapterVersesLoading: Boolean = false,
)
