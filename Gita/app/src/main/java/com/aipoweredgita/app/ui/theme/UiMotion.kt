package com.aipoweredgita.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing

object UiMotion {
    // Durations (ms)
    const val Short = 150
    const val Medium = 300
    const val Long = 500

    // Easing
    val StandardEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // FastOutSlowIn-like
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    // Scales
    const val PressedScale = 0.98f
}

