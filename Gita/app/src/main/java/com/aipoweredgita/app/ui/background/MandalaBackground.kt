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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

/**
 * M3-themed rotating mandala using Material primary color palette.
 */
@Composable
fun MandalaBackground(
    alphaMultiplier: Float = 1f,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary
    val baseAlpha = (0.08f * alphaMultiplier).coerceAtMost(0.18f)

    val transition = rememberInfiniteTransition(label = "mandala_bg")
    val rotation by transition.animateFloat(0f, 360f, infiniteRepeatable(tween(60000, easing = LinearEasing)), label = "rotation")
    val auraAlpha by transition.animateFloat(0.15f, 0.35f, infiniteRepeatable(tween(4000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "aura")

    val cachedPath = remember { Path() }

    Canvas(modifier = modifier.fillMaxSize()) {
        val cx = center.x; val cy = center.y; val r = size.minDimension / 2.5f

        drawCircle(color = primary.copy(alpha = auraAlpha * alphaMultiplier * 0.2f), radius = r * 0.8f, center = center)

        rotate(rotation) {
            drawCircle(primary.copy(alpha = baseAlpha), radius = r, style = Stroke(width = 1.2f.dp.toPx()))
            drawCircle(primary.copy(alpha = baseAlpha * 0.6f), radius = r * 0.8f, style = Stroke(width = 0.8f.dp.toPx()))

            val outerPetals = 8; val outerStep = (2f * PI / outerPetals)
            for (i in 0 until outerPetals) {
                val a = i * outerStep; val px = (cx + r * cos(a)).toFloat(); val py = (cy + r * sin(a)).toFloat()
                drawCircle(primary.copy(alpha = baseAlpha * 0.5f), radius = r * 0.12f, center = Offset(px, py))
            }

            val petals = 12; val angleStep = 2f * PI / petals
            cachedPath.reset()
            for (i in 0 until petals) {
                val a = i * angleStep
                val x = (cx + cos(a) * (r * 0.8)).toFloat()
                val y = (cy + sin(a) * (r * 0.8)).toFloat()
                cachedPath.moveTo(cx, cy)
                cachedPath.quadraticTo((cx + cos(a - angleStep / 3) * (r * 0.5f)).toFloat(), (cy + sin(a - angleStep / 3) * (r * 0.5f)).toFloat(), x, y)
                cachedPath.quadraticTo((cx + cos(a + angleStep / 3) * (r * 0.5f)).toFloat(), (cy + sin(a + angleStep / 3) * (r * 0.5f)).toFloat(), cx, cy)
            }
            drawPath(cachedPath, primary.copy(alpha = baseAlpha * 0.4f), style = Stroke(width = 0.8f.dp.toPx()))

            for (i in 0 until 12) {
                val a = (i * 30f) * (PI / 180f)
                rotate(rotation * 0.3f, pivot = center) {
                    drawLine(primary.copy(alpha = baseAlpha * 0.3f),
                        Offset((cx + r * 0.3f * cos(a)).toFloat(), (cy + r * 0.3f * sin(a)).toFloat()),
                        Offset((cx + r * 0.95f * cos(a)).toFloat(), (cy + r * 0.95f * sin(a)).toFloat()), 0.5f.dp.toPx())
                }
            }
        }
        drawCircle(tertiary.copy(alpha = baseAlpha * 0.3f), radius = r * 1.1f, style = Stroke(width = 0.5f.dp.toPx()))
    }
}
