package com.aipoweredgita.app.quiz.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.random.Random

// Confetti particle data
data class Confetti(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    val color: Color,
    val size: Float,
    var rotation: Float,
    val rotationSpeed: Float
)

@Composable
fun CelebrationOverlay(show: Boolean) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .zIndex(2f),
            contentAlignment = Alignment.Center
        ) {
            // Confetti animation
            ConfettiAnimation(show)

            // Success message with bounce
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val scale = remember { Animatable(0f) }
                LaunchedEffect(show) {
                    if (show) {
                        scale.snapTo(0f)
                        scale.animateTo(
                            1.2f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        scale.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                        )
                    }
                }

                Text(
                    text = "🎉",
                    fontSize = 80.sp,
                    modifier = Modifier.scale(scale.value)
                )

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.scale(scale.value)
                ) {
                    Text(
                        text = "Excellent!",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConfettiAnimation(show: Boolean) {
    val confettiColors = listOf(
        Color(0xFFFF6B6B),
        Color(0xFF4ECDC4),
        Color(0xFFFFE66D),
        Color(0xFF95E1D3),
        Color(0xFFF38181),
        Color(0xFFAA96DA),
        Color(0xFFFCACA0)
    )

    val confettiList = remember { mutableStateListOf<Confetti>() }

    LaunchedEffect(show) {
        if (show) {
            confettiList.clear()
            confettiList.addAll(
                List(50) {
                    Confetti(
                        x = Random.nextFloat(),
                        y = -0.1f - Random.nextFloat() * 0.3f,
                        velocityX = (Random.nextFloat() - 0.5f) * 0.02f,
                        velocityY = Random.nextFloat() * 0.015f + 0.01f,
                        color = confettiColors.random(),
                        size = Random.nextFloat() * 15f + 10f,
                        rotation = Random.nextFloat() * 360f,
                        rotationSpeed = (Random.nextFloat() - 0.5f) * 10f
                    )
                }
            )
        }
    }

    val time = remember { Animatable(0f) }
    LaunchedEffect(show) {
        if(show) {
            time.snapTo(0f)
            time.animateTo(1f, animationSpec = infiniteRepeatable(tween(5000, easing = LinearEasing)))
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        if(show) {
            for (i in confettiList.indices) {
                val confetti = confettiList[i]
                confetti.x += confetti.velocityX
                confetti.y += confetti.velocityY
                confetti.rotation += confetti.rotationSpeed
                confetti.velocityY += 0.0005f

                if (confetti.y > 1.1f) {
                    confetti.y = -0.1f
                    confetti.x = Random.nextFloat()
                    confetti.velocityY = Random.nextFloat() * 0.015f + 0.01f
                }

                val centerX = size.width * confetti.x
                val centerY = size.height * confetti.y

                rotate(confetti.rotation, Offset(centerX, centerY)) {
                    drawRect(
                        color = confetti.color,
                        topLeft = Offset(
                            centerX - confetti.size / 2,
                            centerY - confetti.size / 2
                        ),
                        size = Size(
                            confetti.size,
                            confetti.size * 1.5f
                        )
                    )
                }
            }
        }
    }
}

// Note: WrongOverlay is defined in WrongOverlay.kt to avoid duplicates.
