package com.aipoweredgita.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.luminance

@Composable
fun rememberThemeIsDark(): Boolean {
    val background = MaterialTheme.colorScheme.background
    return remember(background) { background.luminance() < 0.5f }
}
