package com.aipoweredgita.app.util

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility class to help with responsive layouts based on window size
 */
object WindowSizeUtils {

    /**
     * Returns true if the device is a tablet (width >= 600dp)
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= 600
    }

    /**
     * Returns true if the device is in landscape mode
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }

    /**
     * Returns the number of columns for grid layouts based on window size
     */
    @Composable
    fun getGridColumns(windowSizeClass: WindowWidthSizeClass? = null): Int {
        val configuration = LocalConfiguration.current
        val isTablet = configuration.screenWidthDp >= 600
        val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

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
     * Returns appropriate padding based on device type
     */
    @Composable
    fun getScreenPadding(): Dp {
        val isTablet = isTablet()
        return if (isTablet) 32.dp else 24.dp
    }

    /**
     * Returns appropriate card height based on device type
     */
    @Composable
    fun getCardHeight(): Dp {
        val isTablet = isTablet()
        return if (isTablet) 140.dp else 120.dp
    }

    /**
     * Returns appropriate spacing between items
     */
    @Composable
    fun getItemSpacing(): Dp {
        val isTablet = isTablet()
        return if (isTablet) 20.dp else 16.dp
    }
}
