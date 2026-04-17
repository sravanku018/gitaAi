package com.aipoweredgita.app.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.NormalModeViewModel

/**
 * Unified verse reading screen that reuses the existing NormalModeScreen UI
 * and NormalModeViewModel logic, for both online and offline reading.
 */
@Composable
fun VerseScreen(
    viewModel: NormalModeViewModel = viewModel(),
    onReadOfflineClick: () -> Unit = {}
) {
    NormalModeScreen(
        viewModel = viewModel,
        onReadOfflineClick = onReadOfflineClick
    )
}

