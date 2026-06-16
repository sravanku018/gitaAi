package com.aipoweredgita.app.domain.model

data class UiConfigUiState(
    val isLandscape: Boolean = false,
    val gridColumns: Int = 7,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseUiState
