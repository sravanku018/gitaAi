package com.aipoweredgita.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.platform.LocalContext
import com.aipoweredgita.app.GitaApp
import kotlinx.coroutines.isActive

private data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
    val size: Float,
    val shape: Int, // 0 = rect, 1 = circle, 2 = triangle
    var rotation: Float,
    val rotationVelocity: Float,
    var alpha: Float = 1.0f
)

@Composable
fun ConfettiBurst(
    playId: Int,
    modifier: Modifier = Modifier,
    count: Int = -1, // -1 means use device profile
    onFinished: () -> Unit = {},
) {
    val context = LocalContext.current
    val app = context.applicationContext as? GitaApp
    val multiplier = app?.deviceProfile?.particleMultiplier ?: 1.0f
    val finalCount = if (count == -1) (80 * multiplier).toInt() else count

    val particles = remember(playId) { mutableStateListOf<ConfettiParticle>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Pre-allocated base paths to avoid allocation during draw
    val baseTrianglePath = remember {
        Path().apply {
            moveTo(0f, -0.5f)
            lineTo(0.5f, 0.5f)
            lineTo(-0.5f, 0.5f)
            close()
        }
    }

    LaunchedEffect(playId) {
        particles.clear()
        val colors = listOf(
            Color(0xFFFF5252), Color(0xFF448AFF), Color(0xFFFFC107),
            Color(0xFF4CAF50), Color(0xFFE040FB), Color(0xFFFF9800), Color(0xFF00BCD4)
        )
        repeat(finalCount) {
            val angle = (it.toFloat() / finalCount) * (Math.PI * 2).toFloat()
            val speed = 150f + (it % 7) * 45f
            val vx = (kotlin.math.cos(angle) * speed) * (0.8f + (it % 3) * 0.2f)
            val vy = (kotlin.math.sin(angle) * speed) - 300f - (it % 4) * 50f
            val sizeVal = 8f + (it % 5) * 3f
            val shapeVal = it % 3 // 0 = rect, 1 = circle, 2 = triangle
            val rot = (it % 360).toFloat()
            val rotV = -180f + (it % 9) * 40f
            particles.add(
                ConfettiParticle(
                    x = 0f, y = 0f,
                    vx = vx, vy = vy,
                    color = colors[it % colors.size],
                    size = sizeVal,
                    shape = shapeVal,
                    rotation = rot,
                    rotationVelocity = rotV
                )
            )
        }
    }

    LaunchedEffect(playId) {
        var last = 0L
        var elapsed = 0f
        while (isActive && particles.isNotEmpty() && elapsed < 3.0f) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = (now - last) / 1_000_000_000f
                last = now
                elapsed += dt

                val g = 900f
                particles.forEach { p ->
                    p.vy += g * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                    p.rotation += p.rotationVelocity * dt
                    p.alpha = (1.0f - (elapsed / 3.0f)).coerceIn(0f, 1f)
                }
                if (canvasSize.height > 0) {
                    particles.removeAll { it.y > canvasSize.height + 40f }
                }
            }
        }
        onFinished()
    }

    Canvas(modifier.fillMaxSize().onSizeChanged { canvasSize = it }) {
        drawParticles(particles, baseTrianglePath)
    }
}

private fun DrawScope.drawParticles(
    particles: List<ConfettiParticle>,
    baseTrianglePath: Path
) {
    val cx = size.width / 2f
    val cy = size.height / 3f
    particles.forEach { p ->
        val colorWithAlpha = p.color.copy(alpha = p.alpha)
        when (p.shape) {
            0 -> { // Rectangle
                rotate(degrees = p.rotation, pivot = Offset(cx + p.x, cy + p.y)) {
                    drawRect(
                        color = colorWithAlpha,
                        topLeft = Offset(cx + p.x - p.size / 2f, cy + p.y - p.size / 2f),
                        size = Size(p.size, p.size * 1.5f)
                    )
                }
            }
            1 -> { // Circle
                drawCircle(
                    color = colorWithAlpha,
                    radius = p.size / 2f,
                    center = Offset(cx + p.x, cy + p.y)
                )
            }
            2 -> { // Triangle
                withTransform({
                    translate(cx + p.x, cy + p.y)
                    rotate(p.rotation, pivot = Offset.Zero)
                    scale(p.size, p.size, pivot = Offset.Zero)
                }) {
                    drawPath(path = baseTrianglePath, color = colorWithAlpha)
                }
            }
        }
    }
}
