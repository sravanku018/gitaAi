package com.aipoweredgita.app.ui

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
fun CelebrationOverlay(
    playId: Int,
    modifier: Modifier = Modifier,
    message: String = "Great job!",
    onFinished: () -> Unit = {},
) {
    val finished = remember(playId) { mutableStateOf(false) }

    Box(
        modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
    ) {
        ConfettiBurst(
            playId = playId,
            modifier = Modifier.fillMaxSize(),
            onFinished = {
                finished.value = true
                onFinished()
            }
        )

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
    }
}

