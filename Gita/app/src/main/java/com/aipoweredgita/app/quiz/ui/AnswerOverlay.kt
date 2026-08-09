package com.aipoweredgita.app.quiz.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random
import com.aipoweredgita.app.ui.ConfettiBurst

@Composable
fun AnswerOverlay(
    show: Boolean,
    isCorrect: Boolean,
) {
    val emoji = if (isCorrect) "🎉" else "💭"
    val message = if (isCorrect) "Excellent!" else "Keep Learning!"
    val bgColor = if (isCorrect) Color(0xFF4CAF50).copy(alpha = 0.15f) else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
    val cardColor = if (isCorrect) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    val textColor = if (isCorrect) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer

    var playId by remember { mutableStateOf(0) }
    LaunchedEffect(show, isCorrect) {
        if (show && isCorrect) {
            playId++
        }
    }

    AnimatedVisibility(
        visible = show,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isCorrect) {
                ConfettiBurst(playId = playId)
            }

            // Shake offset for wrong answers
            val offsetX = if (!isCorrect) {
                val shakeOffset = remember { Animatable(0f) }
                LaunchedEffect(show) {
                    if (show) {
                        shakeOffset.animateTo(
                            0f,
                            animationSpec = keyframes {
                                durationMillis = 400
                                0f at 0
                                -20f at 50
                                20f at 150
                                -15f at 250
                                15f at 350
                                0f at 400
                            }
                        )
                    }
                }
                shakeOffset
            } else null

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = if (offsetX != null) Modifier.offset(x = offsetX.value.dp) else Modifier
            ) {
                val scale = remember { Animatable(0f) }
                LaunchedEffect(show) {
                    if (show) {
                        scale.snapTo(0f)
                        scale.animateTo(
                            if (isCorrect) 1.2f else 1f,
                            animationSpec = spring(
                                dampingRatio = if (isCorrect) Spring.DampingRatioMediumBouncy else Spring.DampingRatioMediumBouncy,
                                stiffness = if (isCorrect) Spring.StiffnessLow else Spring.StiffnessMedium
                            )
                        )
                        if (isCorrect) {
                            scale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy))
                        }
                    }
                }

                Text(
                    text = emoji,
                    fontSize = 80.sp,
                    modifier = Modifier.scale(scale.value)
                )

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.scale(scale.value)
                ) {
                    Text(
                        text = message,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
        }
    }
}


