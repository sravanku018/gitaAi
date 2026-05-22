package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.ConfettiBurst

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
        3 -> Color(0xFFFFD700) // Gold for Moksha
        else -> Color(0xFFFF9800)
    }
    
    // Animation values
    val infiniteTransition = rememberInfiniteTransition(label = "celebration")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Auto-dismiss after 4 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(4000)
        onDismiss()
    }
    
    Box(
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
