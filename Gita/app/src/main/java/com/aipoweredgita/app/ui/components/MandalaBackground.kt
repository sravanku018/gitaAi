package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun MandalaBackground(
    modifier: Modifier = Modifier,
    color: Color = Color.White.copy(alpha = 0.1f)
) {
    val animate = rememberAllowAmbientAnimation()
    if (!animate) {
        MandalaCanvas(modifier = modifier, color = color, rotation = 0f)
        return
    }
    val infiniteTransition = rememberInfiniteTransition(label = "mandala_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing)
        ),
        label = "rotation"
    )
    MandalaCanvas(modifier = modifier, color = color, rotation = rotation)
}

@Composable
private fun MandalaCanvas(
    modifier: Modifier,
    color: Color,
    rotation: Float,
) {
    val cachedPath = remember { Path() }

    Canvas(modifier = modifier) {
        val center = center
        val radius = size.minDimension / 2.5f

        rotate(rotation) {
            drawCircle(
                color = color,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )

            drawCircle(
                color = color.copy(alpha = color.alpha * 0.7f),
                radius = radius * 0.8f,
                style = Stroke(width = 1.dp.toPx())
            )

            val petals = 12
            val angleStep = (2 * PI / petals).toFloat()

            cachedPath.reset()
            for (i in 0 until petals) {
                val angle = i * angleStep
                val x = center.x + cos(angle) * (radius * 0.8f)
                val y = center.y + sin(angle) * (radius * 0.8f)

                cachedPath.moveTo(center.x, center.y)
                val controlAngle1 = angle - angleStep / 3
                val cp1x = center.x + cos(controlAngle1) * (radius * 0.5f)
                val cp1y = center.y + sin(controlAngle1) * (radius * 0.5f)

                cachedPath.quadraticBezierTo(cp1x, cp1y, x.toFloat(), y.toFloat())

                val controlAngle2 = angle + angleStep / 3
                val cp2x = center.x + cos(controlAngle2) * (radius * 0.5f)
                val cp2y = center.y + sin(controlAngle2) * (radius * 0.5f)

                cachedPath.quadraticBezierTo(cp2x, cp2y, center.x, center.y)
            }
            drawPath(
                cachedPath,
                color.copy(alpha = color.alpha * 0.5f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
