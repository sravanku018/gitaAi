package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun LotusBadge(
    level: Int, // 1..5 (5 Yoga levels)
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    animateChanges: Boolean = true
) {
    val clamped = level.coerceIn(1, 5)
    val t = (clamped - 1) / 4f // Map 1..5 to 0..1

    val petals = 6 + ((t * 6f).toInt()) // 6..12
    val opennessTarget = 0.3f + 0.7f * t // 0.3..1.0
    val innerRadiusFactor = 0.25f + 0.25f * t // 0.25..0.5

    val animatedOpenness by animateFloatAsState(
        targetValue = opennessTarget,
        animationSpec = if (animateChanges) tween(300) else tween(0),
        label = "lotus-openness"
    )

    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val onPrimary = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val minDim = kotlin.math.min(w, h)
        val cx = w / 2f
        val cy = h / 2f
        val outerR = minDim * 0.45f
        val innerR = outerR * innerRadiusFactor
        val petalHalfW = outerR * 0.18f

        // Background subtle circle
        drawCircle(
            color = colorPrimary.copy(alpha = 0.12f),
            radius = outerR + minDim * 0.05f,
            center = Offset(cx, cy)
        )

        val brush = Brush.radialGradient(
            colors = listOf(colorPrimary, colorSecondary.copy(alpha = 0.9f)),
            center = Offset(cx, cy),
            radius = outerR
        )

        fun petalPath(): Path {
            val p = Path()
            // Build a leaf/teardrop shape pointing up (local coords, origin at center)
            p.moveTo(cx, cy - innerR)
            p.cubicTo(
                cx + petalHalfW, cy - innerR,
                cx + petalHalfW, cy - outerR * animatedOpenness,
                cx, cy - outerR
            )
            p.cubicTo(
                cx - petalHalfW, cy - outerR * animatedOpenness,
                cx - petalHalfW, cy - innerR,
                cx, cy - innerR
            )
            p.close()
            return p
        }

        val step = 360f / petals
        repeat(petals) { i ->
            rotate(degrees = step * i, pivot = Offset(cx, cy)) {
                drawPath(path = petalPath(), brush = brush)
            }
        }

        // Inner core
        drawCircle(
            color = onPrimary.copy(alpha = 0.9f),
            radius = innerR * 0.6f,
            center = Offset(cx, cy)
        )
    }
}
