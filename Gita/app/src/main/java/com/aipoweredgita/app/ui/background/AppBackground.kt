package com.aipoweredgita.app.ui.background

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

enum class BgPattern {
    NONE,
    AMBIENT_ORBS,
    MANDALA,
    PARTICLES,
    ORBS_MANDALA,    // Both orbs + mandala layered
    ALL              // Orbs + mandala + particles
}

@Composable
fun AppBackground(
    pattern: BgPattern = BgPattern.AMBIENT_ORBS,
    intensity: Float = 1f,
    isDark: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Background layer
        when (pattern) {
            BgPattern.AMBIENT_ORBS -> AmbientOrbsBackground(intensity = intensity)
            BgPattern.MANDALA -> MandalaBackground(alphaMultiplier = intensity)
            BgPattern.PARTICLES -> ParticleField(intensity = intensity)
            BgPattern.ORBS_MANDALA -> {
                AmbientOrbsBackground(intensity = intensity * 0.7f)
                MandalaBackground(alphaMultiplier = intensity * 0.5f)
            }
            BgPattern.ALL -> {
                AmbientOrbsBackground(intensity = intensity * 0.6f)
                MandalaBackground(alphaMultiplier = intensity * 0.3f)
                ParticleField(intensity = intensity * 0.5f)
            }
            BgPattern.NONE -> {}
        }

        // Foreground content
        content()
    }
}
