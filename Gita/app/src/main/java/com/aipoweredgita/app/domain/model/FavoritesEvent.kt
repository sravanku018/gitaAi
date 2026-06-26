package com.aipoweredgita.app.domain.model

sealed class FavoritesEvent {
    data class DeleteFavorite(val chapter: Int, val verse: Int) : FavoritesEvent()
    data object ClearAllFavorites : FavoritesEvent()
}
