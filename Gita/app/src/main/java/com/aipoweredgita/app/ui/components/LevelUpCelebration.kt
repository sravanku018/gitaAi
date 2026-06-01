package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.*
<<<<<<< HEAD
import androidx.compose.foundation.background
=======
import androidx.compose.foundation.Canvas
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
<<<<<<< HEAD
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.ConfettiBurst
import com.aipoweredgita.app.ui.theme.GoldSpark
=======
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

@Composable
fun LevelUpCelebration(
    newLevel: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val levelName = when (newLevel) {
        1 -> "Bhakti Yoga"
        2 -> "Jnana Yoga"
        3 -> "Moksha"
        else -> "Level Up"
    }
    
    val levelColor = when (newLevel) {
        1 -> Color(0xFFE91E63) // Pink for Bhakti
        2 -> Color(0xFF2196F3) // Blue for Jnana
<<<<<<< HEAD
        3 -> GoldSpark // Gold for Moksha
=======
        3 -> Color(0xFFFFD700) // Gold for Moksha
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        else -> Color(0xFFFF9800)
    }
    
    // Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scale by infiniteTransition.animateFloat(
<<<<<<< HEAD
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
=======
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
<<<<<<< HEAD
            animation = tween(8000, easing = LinearEasing),
=======
            animation = tween(3000, easing = LinearEasing),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
<<<<<<< HEAD
    // Auto-dismiss after 4 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
=======
    // Auto-dismiss after 3 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        onDismiss()
    }
    
    Box(
<<<<<<< HEAD
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        // High fidelity Confetti Burst background
        ConfettiBurst(
            playId = 1,
            count = 100,
            onFinished = {}
        )
        
        // Level-up glass card container
        GlassCard(
            modifier = Modifier
                .width(320.dp)
                .wrapContentHeight()
                .padding(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
            cornerRadius = 28.dp,
            tint = Color.White.copy(alpha = 0.08f),
            border = Color.White.copy(alpha = 0.2f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎉 LEVEL UP! 🎉",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = levelColor,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Rotating Lotus Badge
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .graphicsLayer {
                            rotationZ = rotation
                        },
                    contentAlignment = Alignment.Center
                ) {
                    LotusBadge(
                        level = newLevel,
                        size = 90.dp,
                        animateChanges = false
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Text(
                    text = levelName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "You've unlocked a new spiritual archetype path. Continue reading and practicing!",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

=======
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Confetti background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val confettiCount = 30
            repeat(confettiCount) { i ->
                val x = (i * size.width / confettiCount) + (rotation * 2f) % size.width
                val y = ((i * 137.5f + rotation * 5f) % size.height)
                val confettiColor = when (i % 4) {
                    0 -> Color(0xFFFF6B6B)
                    1 -> Color(0xFF4ECDC4)
                    2 -> Color(0xFFFFE66D)
                    else -> Color(0xFF95E1D3)
                }
                
                drawCircle(
                    color = confettiColor,
                    radius = 8f,
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }
        
        // Level-up message
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🎉",
                fontSize = (60 * scale).sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Text(
                text = "LEVEL UP!",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = levelColor
            )
            
            Text(
                text = levelName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
