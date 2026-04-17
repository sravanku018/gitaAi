package com.aipoweredgita.app.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.ui.components.LotusBadge
import com.aipoweredgita.app.ui.components.LotusLevelManager
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import com.aipoweredgita.app.ui.LocalUiConfig

@Composable
fun AwakeningPage(
    modifier: Modifier = Modifier
) {
    val uiCfg = LocalUiConfig.current
    val profileViewModel: ProfileViewModel = viewModel()
    val stats by profileViewModel.stats.collectAsState()
    val level = LotusLevelManager.levelFor(stats)
    val progress = LotusLevelManager.progressInLevel(stats)

    // Overall background aura
    Box(modifier = modifier.fillMaxSize()) {
        val intensity = LotusLevelManager.compositeScore(stats)
        AwakeningAuraBackground(intensity = intensity)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (uiCfg.isLandscape) 24.dp else 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val titleShadow = Shadow(
                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                offset = Offset(0f, 2f),
                blurRadius = 8f
            )
            Text(
                text = "Awakening Consciousness",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = MaterialTheme.colorScheme.primary,
                    shadow = titleShadow
                )
            )

            // Badge with local glow aura
            Box(modifier = Modifier.size(140.dp), contentAlignment = Alignment.Center) {
                BadgeAura(intensity = intensity)
                LotusBadge(level = level, size = 96.dp)
            }

            Text(text = "Level ${level + 1} / 10", style = MaterialTheme.typography.titleMedium)

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Progress to next level",
                style = MaterialTheme.typography.bodyMedium.copy(
                    shadow = Shadow(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        offset = Offset(0f, 1f),
                        blurRadius = 6f
                    )
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AwakeningAuraBackground(intensity: Float) {
    val infinite = rememberInfiniteTransition(label = "awakening-bg")
    val pulse by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg-pulse"
    )
    val warm = MaterialTheme.colorScheme.secondary
    val cool = MaterialTheme.colorScheme.primary
    val blend = lerp(warm, cool, intensity.coerceIn(0f, 1f))
    Canvas(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val w = size.width
        val h = size.height
        val minDim = kotlin.math.min(w, h)
        val center = Offset(w / 2f, h / 3f)
        // Radial gradient pulses
        drawCircle(color = blend.copy(alpha = 0.08f), radius = minDim * 0.65f * pulse, center = center)
        drawCircle(color = blend.copy(alpha = 0.06f), radius = minDim * 0.8f * pulse, center = center)
        drawCircle(color = blend.copy(alpha = 0.04f), radius = minDim * 0.95f * pulse, center = center)
    }
}

@Composable
private fun BadgeAura(intensity: Float) {
    val infinite = rememberInfiniteTransition(label = "badge-aura")
    val pulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "badge-pulse"
    )
    val rotationDeg by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 10000, easing = LinearEasing)),
        label = "badge-rot"
    )
    val warm = MaterialTheme.colorScheme.secondary
    val cool = MaterialTheme.colorScheme.primary
    val glow = lerp(warm, cool, intensity.coerceIn(0f, 1f))
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = kotlin.math.min(w, h)
        val outer = minDim * 0.48f * pulse
        // Rings
        drawCircle(color = glow.copy(alpha = 0.22f), radius = outer * 0.85f, center = Offset(cx, cy))
        drawCircle(color = glow.copy(alpha = 0.16f), radius = outer, center = Offset(cx, cy))
        // Perimeter sparkles
        val count = 6 + (intensity * 10f).toInt()
        val step = 360f / count.coerceAtLeast(1)
        val ringR = outer * 0.8f
        val r = (minDim * 0.012f) + (minDim * 0.006f * intensity)
        for (i in 0 until count) {
            val deg = step * i + rotationDeg
            val rad = Math.toRadians(deg.toDouble())
            val x = cx + ringR * kotlin.math.cos(rad).toFloat()
            val y = cy + ringR * kotlin.math.sin(rad).toFloat()
            drawCircle(color = onPrimaryColor.copy(alpha = 0.6f), radius = r * pulse, center = Offset(x, y))
        }
    }
}
