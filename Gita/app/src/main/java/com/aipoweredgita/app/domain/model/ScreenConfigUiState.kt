package com.aipoweredgita.app.domain.model

data class ScreenConfigUiState(
    val isTablet: Boolean = false,
    val isLandscape: Boolean = false,
    val gridColumns: Int = 1,
    val screenPadding: Int = 24,
    val cardHeight: Int = 120,
    val itemSpacing: Int = 16,
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseUiState
