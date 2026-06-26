package com.aipoweredgita.app.ui

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.viewmodel.NormalModeViewModel

/**
 * Unified verse reading screen that reuses the existing NormalModeScreen UI
 * and NormalModeViewModel logic, for both online and offline reading.
 */
@Composable
fun VerseScreen(
    viewModel: NormalModeViewModel = hiltViewModel(),
    onReadOfflineClick: () -> Unit = {}
) {
    NormalModeScreen(
        viewModel = viewModel,
        onReadOfflineClick = onReadOfflineClick
    )
}

