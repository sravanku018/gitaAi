package com.aipoweredgita.app.ui.background

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlin.math.sin

private data class Particle(
    val x: Float,
    val y: Float,
    val radius: Float,
    val phase: Float
)

@Composable
fun ParticleField(
    intensity: Float = 1f,
    modifier: Modifier = Modifier
) {
    val count = (24 * intensity).toInt().coerceIn(12, 48)
    val particles = remember {
        List(count) {
            Particle(
                x = (0..1000).random() / 1000f,
                y = (0..1000).random() / 1000f,
                radius = 1.5f + (0..30).random() * 0.1f,
                phase = (0..628).random() / 100f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val tick by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.2831855f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
        label = "tick"
    )

    val baseAlpha = (0.35f * intensity).coerceAtMost(0.6f)
    val particleColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val a = (sin((tick + p.phase).toDouble()).toFloat() * baseAlpha * 0.5f + baseAlpha * 0.5f).coerceIn(0f, 1f)
            drawCircle(color = particleColor.copy(alpha = a), radius = p.radius, center = Offset(size.width * p.x, size.height * p.y))
        }
    }
}
