package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.database.CachedVerse

/**
 * UI State for Verse/Reading Screen
 * Single source of truth for all reading-related UI state
 */
data class VerseUiState(
    val currentVerse: CachedVerse? = null,
    val verses: List<CachedVerse> = emptyList(),
    val currentChapter: Int = 0,
    val currentVerseNumber: Int = 0,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isBookmarked: Boolean = false,
    val translationLanguage: String = "english"
) : BaseUiState

/**
 * Events that can occur on the Verse screen
 */
sealed class VerseEvent {
    data class LoadVerse(val chapter: Int, val verse: Int) : VerseEvent()
    data object NextVerse : VerseEvent()
    data object PreviousVerse : VerseEvent()
    data class GoToChapter(val chapter: Int) : VerseEvent()
    data class ToggleBookmark(val chapter: Int, val verse: Int) : VerseEvent()
    data class ChangeTranslation(val language: String) : VerseEvent()
}

/**
 * One-time side effects for the Verse screen
 */
sealed class VerseSideEffect {
    data class ShowToast(val message: String) : VerseSideEffect()
    data class ShowError(val message: String) : VerseSideEffect()
    data object BookmarkAdded : VerseSideEffect()
    data object BookmarkRemoved : VerseSideEffect()
}
