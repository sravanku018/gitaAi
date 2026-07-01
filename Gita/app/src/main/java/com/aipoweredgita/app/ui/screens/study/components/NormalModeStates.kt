package com.aipoweredgita.app.ui.screens.study.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.Gold
import com.aipoweredgita.app.ui.theme.Saffron

@Composable
fun GitaLoadingScreen() {
    val pulse by rememberInfiniteTransition(label = "load").animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ॐ", fontSize = 52.sp, color = Gold.copy(alpha = pulse))
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading verse…",
                fontSize  = 14.sp,
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
fun GitaErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠", fontSize = 40.sp, color = Saffron)
            Spacer(Modifier.height(12.dp))
            Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(Brush.horizontalGradient(listOf(Gold, Saffron)))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}
