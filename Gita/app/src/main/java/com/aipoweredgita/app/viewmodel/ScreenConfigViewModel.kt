package com.aipoweredgita.app.viewmodel

import android.content.Context
import android.content.res.Configuration
import androidx.lifecycle.ViewModel
import com.aipoweredgita.app.domain.model.ScreenConfigEvent
import com.aipoweredgita.app.domain.model.ScreenConfigUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class ScreenConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScreenConfigUiState())
    val uiState: StateFlow<ScreenConfigUiState> = _uiState.asStateFlow()

    // Legacy support
    val screenConfig: StateFlow<ScreenConfigUiState> = uiState

    init {
        updateScreenConfig(context.resources.configuration)
    }

    fun onEvent(event: ScreenConfigEvent) {
        when (event) {
            is ScreenConfigEvent.UpdateScreenConfig -> updateScreenConfig(event.configuration ?: context.resources.configuration)
            is ScreenConfigEvent.OnConfigurationChanged -> updateScreenConfig(event.newConfig)
        }
    }

    private fun updateScreenConfig(configuration: Configuration) {
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp

        val isTablet = screenWidthDp >= 600
        val isLandscape = screenWidthDp > screenHeightDp

        val gridColumns = calculateGridColumns(isTablet, isLandscape)
        val screenPadding = if (isTablet) 32 else 24
        val cardHeight = if (isTablet) 140 else 120
        val itemSpacing = if (isTablet) 20 else 16

        _uiState.update {
            it.copy(
                isTablet = isTablet,
                isLandscape = isLandscape,
                gridColumns = gridColumns,
                screenPadding = screenPadding,
                cardHeight = cardHeight,
                itemSpacing = itemSpacing
            )
        }
    }

    private fun calculateGridColumns(isTablet: Boolean, isLandscape: Boolean): Int {
        return when {
            isTablet && isLandscape -> 3
            isTablet && !isLandscape -> 2
            else -> 1
        }
    }

    // Keep legacy methods working by redirecting to events
    fun updateScreenConfig() {
        onEvent(ScreenConfigEvent.UpdateScreenConfig())
    }

    fun onConfigurationChanged(newConfig: Configuration) {
        onEvent(ScreenConfigEvent.OnConfigurationChanged(newConfig))
    }
}
