package com.aipoweredgita.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.isActive

private data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Color,
)

@Composable
fun ConfettiBurst(
    playId: Int,
    modifier: Modifier = Modifier,
    count: Int = 80,
    onFinished: () -> Unit = {},
) {
    val particles = remember(playId) { mutableStateListOf<Particle>() }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Prepare particles on each play
    LaunchedEffect(playId) {
        particles.clear()
        val colors = listOf(
            Color(0xFFFF5252), Color(0xFF448AFF), Color(0xFFFFC107),
            Color(0xFF4CAF50), Color(0xFFE040FB)
        )
        repeat(count) {
            val angle = (it.toFloat() / count) * (Math.PI * 2).toFloat()
            val speed = 200f + (it % 5) * 40f
            val vx = (kotlin.math.cos(angle) * speed)
            val vy = (kotlin.math.sin(angle) * speed) - 200f
            particles.add(
                Particle(
                    x = 0f, y = 0f,
                    vx = vx, vy = vy,
                    color = colors[it % colors.size]
                )
            )
        }
    }

    // Frame-synced animation loop
    LaunchedEffect(playId) {
        var last = 0L
        var elapsed = 0f
        // Run until particles fall out of bounds or max duration reached
        while (isActive && particles.isNotEmpty() && elapsed < 3.0f) {
            withFrameNanos { now ->
                if (last == 0L) last = now
                val dt = (now - last) / 1_000_000_000f
                last = now
                elapsed += dt

                // Basic physics
                val g = 900f
                particles.forEach { p ->
                    p.vy += g * dt
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                }
                // Cull off-screen particles if we know the size
                if (canvasSize.height > 0) {
                    particles.removeAll { it.y > canvasSize.height + 40f }
                }
            }
        }
        onFinished()
    }

    Canvas(modifier.fillMaxSize().onSizeChanged { canvasSize = it }) {
        drawParticles(particles)
    }
}

private fun DrawScope.drawParticles(particles: List<Particle>) {
    val cx = size.width / 2f
    val cy = size.height / 3f
    particles.forEach { p ->
        drawRect(
            color = p.color,
            topLeft = Offset(cx + p.x, cy + p.y),
            size = Size(6f, 12f)
        )
    }
}
