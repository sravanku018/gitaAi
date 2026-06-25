package com.aipoweredgita.app.domain.model

sealed class NormalModeEvent {
    data class LoadVerse(val chapter: Int, val verse: Int, val retryCount: Int = 0, val autoSkipCombined: Boolean = false) : NormalModeEvent()
    object NextVerse : NormalModeEvent()
    object PreviousVerse : NormalModeEvent()
    data class GoToChapter(val chapter: Int) : NormalModeEvent()
    object ToggleFavorite : NormalModeEvent()
    object TrackShare : NormalModeEvent()
}
