package com.aipoweredgita.app.ui.components

import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.utils.DeviceTier
import com.aipoweredgita.app.utils.DeviceTierDetector
import kotlin.math.cos
import kotlin.math.sin

private val SineEasing = Easing { fraction ->
    (1f - cos(fraction * Math.PI.toFloat())) / 2f
}

/** Continuous Canvas animations are expensive — skip on low tiers. */
@Composable
fun rememberAllowAmbientAnimation(): Boolean {
    val context = LocalContext.current
    val tier = remember(context) { DeviceTierDetector.detect(context) }
    return tier != DeviceTier.LOW && tier != DeviceTier.LOW_MID
}

@Composable
fun AmbientOrbs(modifier: Modifier = Modifier) {
    // Skip continuous animation on low-end devices (battery / jank).
    val animate = rememberAllowAmbientAnimation()

    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    if (!animate) {
        AmbientOrbsCanvas(
            modifier = modifier,
            primary = primary,
            secondary = secondary,
            tertiary = tertiary,
            drift1X = 0f,
            drift1Y = 0f,
            drift2X = 0f,
            drift2Y = 0f,
        )
        return
    }

    val transition = rememberInfiniteTransition(label = "ambient_orbs")
    val drift1X by transition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift1X"
    )
    val drift1Y by transition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift1Y"
    )
    val drift2X by transition.animateFloat(
        initialValue = 30f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift2X"
    )
    val drift2Y by transition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "drift2Y"
    )

    AmbientOrbsCanvas(
        modifier = modifier,
        primary = primary,
        secondary = secondary,
        tertiary = tertiary,
        drift1X = drift1X,
        drift1Y = drift1Y,
        drift2X = drift2X,
        drift2Y = drift2Y,
    )
}

@Composable
private fun AmbientOrbsCanvas(
    modifier: Modifier,
    primary: Color,
    secondary: Color,
    tertiary: Color,
    drift1X: Float,
    drift1Y: Float,
    drift2X: Float,
    drift2Y: Float,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.22f), Color.Transparent),
                center = Offset(size.width * 0.25f + drift1X.dp.toPx(), -20.dp.toPx() + drift1Y.dp.toPx()),
                radius = 180.dp.toPx()
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width + drift2X.dp.toPx(), 220.dp.toPx() + drift2Y.dp.toPx()),
                radius = 160.dp.toPx()
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(tertiary.copy(alpha = 0.13f), Color.Transparent),
                center = Offset(drift2X.dp.toPx(), size.height - 180.dp.toPx() + drift1Y.dp.toPx()),
                radius = 140.dp.toPx()
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondary.copy(alpha = 0.10f), Color.Transparent),
                center = Offset(size.width - 40.dp.toPx() + drift1X.dp.toPx(), size.height - 300.dp.toPx() + drift2Y.dp.toPx()),
                radius = 120.dp.toPx()
            )
        )
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 32.dp,
    tint: Color = Color.White.copy(alpha = 0.055f),
    border: Color = Color.White.copy(alpha = 0.13f),
    elevation: Dp = 0.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = com.aipoweredgita.app.ui.theme.rememberThemeIsDark()
    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.5f) else Color(0x35546E7A)

    val shadowModifier = if (elevation > 0.dp) {
        Modifier.shadow(
            elevation = elevation,
            shape = RoundedCornerShape(cornerRadius),
            clip = false,
            ambientColor = shadowColor,
            spotColor = shadowColor
        )
    } else Modifier

    Box(
        modifier = modifier
            .then(shadowModifier)
            .clip(RoundedCornerShape(cornerRadius))
            .background(tint)
            .border(1.dp, border, RoundedCornerShape(cornerRadius))
            .drawBehind {
                if (isDark) {
                    val r = cornerRadius.toPx()
                    // Top glossy reflection shine (dark mode only to avoid two-tone look in light mode)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        ),
                        cornerRadius = CornerRadius(r, r)
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun QuizCardBg(modifier: Modifier = Modifier) {
    val animate = rememberAllowAmbientAnimation()
    if (!animate) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(QuizGreenStart, QuizGreenEnd)))
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "quiz_bg")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val floatAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = SineEasing), RepeatMode.Reverse),
        label = "float"
    )

    val cachedHexagonPath = remember { Path() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(QuizGreenStart, QuizGreenEnd)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width - 15.dp.toPx(), 45.dp.toPx())
            val baseSize = 80.dp.toPx()

            // Draw rotating sacred geometry (6 hexagons scaled and rotated)
            for (i in 0 until 6) {
                rotate(rotation + i * 15f, pivot = center) {
                    val scale = 1f - i * 0.14f
                    val size = baseSize * scale
                    cachedHexagonPath.reset()
                    for (j in 0..5) {
                        val angleRad = (j * 60 - 90) * Math.PI / 180f
                        val x = center.x + size * cos(angleRad).toFloat()
                        val y = center.y + size * sin(angleRad).toFloat()
                        if (j == 0) cachedHexagonPath.moveTo(x, y) else cachedHexagonPath.lineTo(x, y)
                    }
                    cachedHexagonPath.close()
                    drawPath(
                        path = cachedHexagonPath,
                        color = Color.White.copy(alpha = 0.18f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }
            }

            // Draw circles
            listOf(12.dp, 28.dp, 44.dp).forEach { r ->
                drawCircle(
                    color = Color.White.copy(alpha = 0.12f),
                    radius = r.toPx(),
                    center = center,
                    style = Stroke(width = 0.8.dp.toPx())
                )
            }

            // Floating particles
            for (i in 0 until 5) {
                val offsetPhase = i * 0.25f * Math.PI.toFloat()
                val px = size.width * (0.15f + i * 0.12f) + sin(floatAnim * 2f * Math.PI.toFloat() + offsetPhase) * 6.dp.toPx()
                val py = size.height * (0.2f + i * 0.15f) + cos(floatAnim * 2f * Math.PI.toFloat() + offsetPhase) * 8.dp.toPx()
                val radius = (3 + i).dp.toPx()
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    center = Offset(px, py)
                )
            }
        }
    }
}

@Composable
fun VoiceCardBg(modifier: Modifier = Modifier) {
    val animate = rememberAllowAmbientAnimation()
    if (!animate) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(VoiceRedStart, VoiceRedEnd)))
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "voice_bg")
    val pulseProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseProgress"
    )
    val micPulse by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = SineEasing), RepeatMode.Reverse),
        label = "micPulse"
    )
    val waveAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "waveAnim"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(VoiceRedStart, VoiceRedEnd)))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val micCenter = Offset(size.width - 32.dp.toPx(), 48.dp.toPx())
            val baseRadius = 24.dp.toPx()

            // Drawing expanding ripple rings
            for (i in 0 until 3) {
                val ringProgress = (pulseProgress + i * 0.33f) % 1f
                val scale = 0.3f + 1.8f * ringProgress
                val radius = baseRadius * scale
                val alpha = (1f - ringProgress) * 0.4f
                drawCircle(
                    color = Color(0xFFFFB450).copy(alpha = alpha),
                    radius = radius,
                    center = micCenter,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Drawing mic glow center
            drawCircle(
                color = Color(0xFFFF9628).copy(alpha = 0.12f + 0.15f * micPulse),
                radius = 18.dp.toPx(),
                center = micCenter
            )
            drawCircle(
                color = Color(0xFFFFB450).copy(alpha = 0.25f),
                radius = 18.dp.toPx(),
                center = micCenter,
                style = Stroke(width = 1.dp.toPx())
            )

            // Draw mic icon text using native canvas to keep it light
            val paint = android.graphics.Paint().apply {
                color = Color.White.toArgb()
                textSize = 18.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawContext.canvas.nativeCanvas.drawText(
                "🎙",
                micCenter.x,
                micCenter.y + 6.dp.toPx(),
                paint
            )

            // Waveform at the bottom
            val barCount = 18
            val gap = 2.5.dp.toPx()
            val totalWidth = size.width - 28.dp.toPx()
            val barWidth = (totalWidth - (barCount - 1) * gap) / barCount
            val bottomY = size.height - 14.dp.toPx()
            val maxBarHeight = 35.dp.toPx()

            for (i in 0 until barCount) {
                val phase = i * 0.5f
                val heightRatio = sin(waveAnim * 2f * Math.PI.toFloat() + phase) * 0.35f + 0.65f
                val barHeight = heightRatio * maxBarHeight

                val left = 14.dp.toPx() + i * (barWidth + gap)
                val top = bottomY - barHeight

                val colorVal = 180 + (heightRatio * 75).toInt()
                val barColor = Color(255, colorVal, 80, 140)

                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun SlokaCardBg(modifier: Modifier = Modifier) {
    val animate = rememberAllowAmbientAnimation()
    if (!animate) {
        SlokaCardBgCanvas(modifier = modifier, rotation = 0f, twinkleAnim = 0.6f)
        return
    }
    val transition = rememberInfiniteTransition(label = "sloka_bg")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(28000, easing = LinearEasing), RepeatMode.Restart),
        label = "rotation"
    )
    val twinkleAnim by transition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = SineEasing), RepeatMode.Reverse),
        label = "twinkle"
    )
    SlokaCardBgCanvas(modifier = modifier, rotation = rotation, twinkleAnim = twinkleAnim)
}

@Composable
private fun SlokaCardBgCanvas(
    modifier: Modifier,
    rotation: Float,
    twinkleAnim: Float,
) {
    val isDark = com.aipoweredgita.app.ui.theme.rememberThemeIsDark()
    val primaryColor = MaterialTheme.colorScheme.primary
    val bgBrush = if (isDark) {
        Brush.linearGradient(listOf(SlokaGoldStart, SlokaGoldEnd))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFFFDF5), Color(0xFFFFF5E0)))
    }
    val geomColor = if (isDark) Color(0xFFFFD080) else primaryColor.copy(alpha = 0.6f)
    val accentLineColor = if (isDark) Color(0xFFFFB830) else primaryColor.copy(alpha = 0.7f)
    val starColor = if (isDark) Color(255, 220, 120) else Color(210, 150, 40)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)

            val outerRadius = 70.dp.toPx()
            val outerRotation = rotation
            for (i in 0 until 12) {
                rotate(outerRotation + i * 30f, pivot = center) {
                    drawOval(
                        color = geomColor.copy(alpha = if (isDark) 0.20f else 0.15f),
                        topLeft = Offset(center.x - 7.dp.toPx(), center.y - outerRadius + 22.dp.toPx()),
                        size = Size(14.dp.toPx(), 52.dp.toPx()),
                        style = Stroke(width = 0.9.dp.toPx())
                    )
                }
            }

            listOf(18.dp, 32.dp, 48.dp, 62.dp).forEach { r ->
                drawCircle(
                    color = geomColor.copy(alpha = if (isDark) 0.15f else 0.12f),
                    radius = r.toPx(),
                    center = center,
                    style = Stroke(width = 0.7.dp.toPx())
                )
            }

            val innerRotation = -rotation * 0.6f
            for (i in 0 until 8) {
                rotate(innerRotation + i * 45f, pivot = center) {
                    drawLine(
                        color = accentLineColor.copy(alpha = if (isDark) 0.32f else 0.25f),
                        start = Offset(center.x, center.y - 8.dp.toPx()),
                        end = Offset(center.x, center.y - 35.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            drawCircle(
                color = accentLineColor.copy(alpha = if (isDark) 0.32f else 0.25f),
                radius = 8.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            for (i in 0 until 8) {
                val phase = i * 0.4f
                val opacity = (sin(twinkleAnim * 2f * Math.PI.toFloat() + phase) * 0.4f + 0.6f).coerceIn(0.2f, 1f)
                val starX = size.width * (0.1f + (i * 0.13f) % 0.8f)
                val starY = size.height * (0.1f + (i * 0.17f) % 0.8f)
                drawCircle(
                    color = starColor.copy(alpha = opacity * (if (isDark) 1f else 0.7f)),
                    radius = 1.5.dp.toPx(),
                    center = Offset(starX, starY)
                )
            }
        }
    }
}

@Composable
fun ReadCardBg(modifier: Modifier = Modifier) {
    val animate = rememberAllowAmbientAnimation()
    if (!animate) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(ReadBlueStart, ReadBlueEnd)))
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "read_bg")
    val scrollOffset by transition.animateFloat(
        initialValue = 0f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing), RepeatMode.Restart),
        label = "scrollOffset"
    )
    val bobbingAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200, easing = SineEasing), RepeatMode.Reverse),
        label = "bobbing"
    )
    val sweepAnim by transition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep"
    )

    val texts = listOf(
        "ॐ तत् सत्", "कर्म योग", "ज्ञान मार्ग", "भक्ति योग",
        "धर्म क्षेत्र", "अर्जुन उवाच", "श्री कृष्ण", "ॐ तत् सत्",
        "कर्म योग", "ज्ञान मार्ग"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(ReadBlueStart, ReadBlueEnd)))
            .drawBehind {
                // Diagonal light sweep
                val sweepX = sweepAnim * size.width
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color(0xFF64A0FF).copy(alpha = 0.12f), Color.Transparent),
                        start = Offset(sweepX - 50.dp.toPx(), 0f),
                        end = Offset(sweepX + 50.dp.toPx(), size.height)
                    ),
                    topLeft = Offset(0f, 0f),
                    size = size
                )
            }
    ) {
        // Sanskrit flowing text lines
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .graphicsLayer {
                    translationY = -scrollOffset.dp.toPx()
                },
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            texts.forEachIndexed { i, t ->
                val opacity = 0.15f + (i % 3) * 0.1f
                Text(
                    text = t,
                    fontSize = (9 + (i % 3) * 2).sp,
                    color = Color.White.copy(alpha = opacity),
                    letterSpacing = (2 + (i % 2)).sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }

        // Floating book emoji
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
                .graphicsLayer {
                    translationY = (bobbingAnim * -8.dp.toPx())
                    rotationZ = (bobbingAnim * 6f) - 3f
                }
        ) {
            Text(
                text = "📖",
                fontSize = 32.sp,
                modifier = Modifier.graphicsLayer { alpha = 0.25f }
            )
        }
    }
}
