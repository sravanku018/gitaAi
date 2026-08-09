package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.database.FavoriteVerse

data class FavoritesUiState(
    val favorites: List<FavoriteVerse> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val favoriteCount: Int = 0
) : BaseUiState
