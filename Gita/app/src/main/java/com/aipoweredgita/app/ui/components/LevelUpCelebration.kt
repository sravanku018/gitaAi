package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

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
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    // Auto-dismiss after 3 seconds
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(3000)
        onDismiss()
    }
    
    Box(
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
