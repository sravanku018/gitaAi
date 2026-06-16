package com.aipoweredgita.app.domain.model

sealed class FavoritesSideEffect {
    data class ShowMessage(val message: String) : FavoritesSideEffect()
}
