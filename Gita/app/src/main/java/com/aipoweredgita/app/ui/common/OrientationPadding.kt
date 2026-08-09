package com.aipoweredgita.app.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.domain.model.UiConfigUiState

fun Modifier.orientationPadding(
    uiCfg: UiConfigUiState,
    landscape: Dp = 24.dp,
    portrait: Dp = 16.dp
): Modifier = this.padding(if (uiCfg.isLandscape) landscape else portrait)
