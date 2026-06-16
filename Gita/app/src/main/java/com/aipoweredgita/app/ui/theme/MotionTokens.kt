package com.aipoweredgita.app.ui.theme

import androidx.compose.animation.core.*

object MotionTokens {

    fun <T> springControl(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium
    )
    fun <T> springExpressive(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow
    )
    fun <T> springSnappy(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessHigh
    )
    fun <T> springSmooth(): SpringSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
    )
    fun <T> springEnter(): SpringSpec<T> = spring(
        dampingRatio = 0.6f, stiffness = 250f
    )

    val emphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)
    val emphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    const val durationShort = 200
    const val durationMedium = 400
    const val durationLong = 600

    fun staggerDelay(index: Int, baseMs: Int = 60, perItemMs: Int = 50): Int =
        baseMs + index * perItemMs
}
