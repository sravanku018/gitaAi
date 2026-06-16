package com.aipoweredgita.app.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Floating M3 tonal orbs drifting across the screen.
 * Uses Material theme primary/secondary/tertiary colors.
 */
@Composable
fun AmbientOrbsBackground(
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val alpha = (0.18f * intensity).coerceAtMost(0.35f)
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    val transition = rememberInfiniteTransition(label = "ambient_orbs")
    val drift1X by transition.animateFloat(-30f, 30f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "drift1X")
    val drift1Y by transition.animateFloat(-30f, 30f, infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse), label = "drift1Y")
    val drift2X by transition.animateFloat(30f, -30f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "drift2X")
    val drift2Y by transition.animateFloat(-30f, 30f, infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse), label = "drift2Y")

    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(listOf(primary.copy(alpha = alpha), Color.Transparent)),
            center = Offset(drift1X.dp.toPx(), -20.dp.toPx() + drift1Y.dp.toPx()),
            radius = 180.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(secondary.copy(alpha = alpha * 0.8f), Color.Transparent)),
            center = Offset(size.width + drift2X.dp.toPx(), 220.dp.toPx() + drift2Y.dp.toPx()),
            radius = 160.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(tertiary.copy(alpha = alpha * 0.6f), Color.Transparent)),
            center = Offset(drift2X.dp.toPx(), size.height - 180.dp.toPx() + drift1Y.dp.toPx()),
            radius = 140.dp.toPx()
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(primary.copy(alpha = alpha * 0.45f), Color.Transparent)),
            center = Offset(size.width - 40.dp.toPx() + drift1X.dp.toPx(), size.height - 300.dp.toPx() + drift2Y.dp.toPx()),
            radius = 120.dp.toPx()
        )
    }
}
