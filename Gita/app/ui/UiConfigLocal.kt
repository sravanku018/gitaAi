package com.aipoweredgita.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.aipoweredgita.app.domain.model.UiConfigUiState

val LocalUiConfig = staticCompositionLocalOf { UiConfigUiState() }

@Composable
fun UiConfigProvider(content: @Composable () -> Unit) {
    val conf = LocalConfiguration.current
    val cfg = remember(conf.screenWidthDp, conf.screenHeightDp) {
        val landscape = conf.screenWidthDp > conf.screenHeightDp
        val columns = if (landscape) 10 else 7
        UiConfigUiState(isLandscape = landscape, gridColumns = columns)
    }
    CompositionLocalProvider(LocalUiConfig provides cfg) {
        content()
    }
}

