package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.R
import kotlin.random.Random
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import com.aipoweredgita.app.ui.theme.*

@Composable
fun LoadingScreen(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    // List of all loader drawables
    val loaderImages = remember {
        listOf(
            R.drawable.loader_krishna_arjuna,
            R.drawable.loader_conch,
            R.drawable.loader_chakra,
            R.drawable.loader_lotus,
            R.drawable.loader_sacred_book,
            R.drawable.loader_om_symbol
        )
    }

    // Randomly select one loader image (stable across recompositions)
    val selectedLoader = remember { loaderImages[Random.nextInt(loaderImages.size)] }

    // Rotation animation for the loader
    val infiniteTransition = rememberInfiniteTransition(label = "loader_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Rotating loader image
            Image(
                painter = painterResource(id = selectedLoader),
                contentDescription = "Loading animation",
                modifier = Modifier
                    .size(150.dp)
                    .rotate(rotation)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading message
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                fontSize = 18.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Additional progress indicator
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun LoadingScreenPreview() {
    LoadingScreen(message = "Seeking Divine Wisdom...")
}
