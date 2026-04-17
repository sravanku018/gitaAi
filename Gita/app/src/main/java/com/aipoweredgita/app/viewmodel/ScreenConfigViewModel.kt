package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ScreenConfig(
    val isTablet: Boolean = false,
    val isLandscape: Boolean = false,
    val gridColumns: Int = 1,
    val screenPadding: Int = 24,
    val cardHeight: Int = 120,
    val itemSpacing: Int = 16
)

class ScreenConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val _screenConfig = MutableStateFlow(ScreenConfig())
    val screenConfig: StateFlow<ScreenConfig> = _screenConfig.asStateFlow()

    init {
        updateScreenConfig()
    }

    /**
     * Update screen configuration based on current device configuration
     * This should be called with the current Configuration from Compose
     */
    fun updateScreenConfig(configuration: Configuration = getApplication<Application>().resources.configuration) {
        val screenWidthDp = configuration.screenWidthDp
        val screenHeightDp = configuration.screenHeightDp

        val isTablet = screenWidthDp >= 600
        val isLandscape = screenWidthDp > screenHeightDp

        val gridColumns = calculateGridColumns(isTablet, isLandscape)
        val screenPadding = if (isTablet) 32 else 24
        val cardHeight = if (isTablet) 140 else 120
        val itemSpacing = if (isTablet) 20 else 16

        _screenConfig.value = ScreenConfig(
            isTablet = isTablet,
            isLandscape = isLandscape,
            gridColumns = gridColumns,
            screenPadding = screenPadding,
            cardHeight = cardHeight,
            itemSpacing = itemSpacing
        )
    }

    /**
     * Calculate number of grid columns based on device type and orientation
     */
    private fun calculateGridColumns(isTablet: Boolean, isLandscape: Boolean): Int {
        return when {
            // Tablet in landscape: 3 columns
            isTablet && isLandscape -> 3
            // Tablet in portrait: 2 columns
            isTablet && !isLandscape -> 2
            // Phone: 1 column
            else -> 1
        }
    }

    /**
     * Handle configuration changes (called when orientation changes)
     */
    fun onConfigurationChanged(newConfig: Configuration) {
        updateScreenConfig()
    }
}
