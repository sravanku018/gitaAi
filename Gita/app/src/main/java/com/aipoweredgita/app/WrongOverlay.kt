package com.aipoweredgita.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign

@Composable
fun WrongOverlay(
    playId: Int,
    modifier: Modifier = Modifier,
    message: String = "Try again",
    onFinished: () -> Unit = {},
) {
    val finished = remember(playId) { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
    ) {
        // Could reuse a different particle effect or a shake; keep simple for now
        Text(
            text = message,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }

    LaunchedEffect(playId) {
        finished.value = false
        // invoke finished after a short display if desired
        onFinished()
    }
}

