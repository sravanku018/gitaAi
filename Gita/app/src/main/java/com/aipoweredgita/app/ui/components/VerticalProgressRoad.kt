package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.GoldSpark

data class YogaLevel(
    val level: Int,
    val name: String,
    val emoji: String,
    val color: Color
)

@Composable
fun VerticalProgressRoad(
    currentLevel: Int,
    currentProgress: Float, // 0-100
    modifier: Modifier = Modifier
) {
    val levels = listOf(
        YogaLevel(0, "Karma Yoga", "🌿", Color(0xFFFF9800)),
        YogaLevel(1, "Bhakti Yoga", "🔥", Color(0xFFE91E63)),
        YogaLevel(2, "Jnana Yoga", "🧠", Color(0xFF2196F3)),
        YogaLevel(3, "Dhyana Yoga", "🌬️", Color(0xFF9C27B0)),
        YogaLevel(4, "Moksha", "🌺", GoldSpark)
    )
    
    // Animated progress fraction (from 0 to 4 levels)
    val targetProgressFraction = (currentLevel + (currentProgress / 100f)).coerceIn(0f, 4f)
    val animatedProgressFraction by animateFloatAsState(
        targetValue = targetProgressFraction,
        animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
        label = "progress_animation"
    )

    val dashPathEffect = remember { PathEffect.dashPathEffect(floatArrayOf(20f, 20f)) }
    
    // Pulsing animation for current level
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Spiritual Journey",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Box enclosing road and milestones
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(500.dp),
                contentAlignment = Alignment.Center
            ) {
                // Draw the road path
                Canvas(
                    modifier = Modifier
                        .width(80.dp)
                        .fillMaxHeight()
                ) {
                    val roadWidth = 60.dp.toPx()
                    val centerX = size.width / 2
                    val padding = 28.dp.toPx()
                    val roadHeightRange = size.height - 2 * padding
                    
                    // Background road (gray)
                    drawRect(
                        color = Color(0xFFBDBDBD),
                        topLeft = Offset(centerX - roadWidth / 2, 0f),
                        size = androidx.compose.ui.geometry.Size(roadWidth, size.height)
                    )
                    
                    // Progress road (gradient)
                    val progressHeight = (animatedProgressFraction / 4f) * roadHeightRange
                    val activeColor = levels.getOrNull(currentLevel)?.color ?: GoldSpark
                    val gradient = Brush.verticalGradient(
                        colors = listOf(
                            activeColor.copy(alpha = 0.7f),
                            activeColor
                        ),
                        startY = size.height - padding - progressHeight,
                        endY = size.height - padding
                    )
                    
                    drawRect(
                        brush = gradient,
                        topLeft = Offset(centerX - roadWidth / 2, size.height - padding - progressHeight),
                        size = androidx.compose.ui.geometry.Size(roadWidth, progressHeight)
                    )
                    
                    // Draw sub-stage ticks/steps (20 per level segment, 100 total stages)
                    val subStageSpacing = roadHeightRange / 4f / 20f
                    val currentGlobalIndex = currentLevel * 20 + (currentProgress / 100f * 20f).toInt()
                    
                    for (l in 0..3) {
                        for (s in 0..19) {
                            val subStageGlobalIndex = l * 20 + s
                            val isSubCompleted = subStageGlobalIndex < currentGlobalIndex
                            val isSubActive = subStageGlobalIndex == currentGlobalIndex
                            
                            val y = size.height - padding - subStageGlobalIndex * subStageSpacing
                            
                            val tickColor = when {
                                isSubCompleted -> (levels.getOrNull(l)?.color ?: GoldSpark).copy(alpha = 0.8f)
                                isSubActive -> levels.getOrNull(l)?.color ?: GoldSpark
                                else -> Color.White.copy(alpha = 0.4f)
                            }
                            
                            val tickLength = 6.dp.toPx()
                            val tickThickness = 1.5.dp.toPx()
                            
                            // Left tick
                            drawRect(
                                color = tickColor,
                                topLeft = Offset(centerX - roadWidth / 2 + 4.dp.toPx(), y - tickThickness / 2),
                                size = androidx.compose.ui.geometry.Size(tickLength, tickThickness)
                            )
                            
                            // Right tick
                            drawRect(
                                color = tickColor,
                                topLeft = Offset(centerX + roadWidth / 2 - 4.dp.toPx() - tickLength, y - tickThickness / 2),
                                size = androidx.compose.ui.geometry.Size(tickLength, tickThickness)
                            )
                        }
                    }
                    
                    // Road center line (dashed)
                    drawLine(
                        color = Color.White,
                        start = Offset(centerX, 0f),
                        end = Offset(centerX, size.height),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = dashPathEffect
                    )
                }
                
                // Level milestones
                Column(
                    modifier = Modifier.fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    levels.reversed().forEachIndexed { index, level ->
                        val isCompleted = level.level < currentLevel
                        val isCurrent = level.level == currentLevel
                        val isLocked = level.level > currentLevel
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Level info (left side)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.End
                            ) {
                                if (isCurrent || isCompleted) {
                                    Text(
                                        text = level.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrent) level.color else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Milestone circle
                            Box(
                                modifier = Modifier
                                    .size(if (isCurrent) 64.dp * pulseScale else 56.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLocked) Color.Gray.copy(alpha = 0.3f)
                                        else if (isCurrent) level.color
                                        else level.color.copy(alpha = 0.7f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = level.emoji,
                                    fontSize = if (isCurrent) 32.sp else 28.sp
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            // Progress percentage (right side)
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.Start
                            ) {
                                if (isCurrent) {
                                    Text(
                                        text = "${currentProgress.toInt()}%",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = level.color
                                    )
                                } else if (isCompleted) {
                                    Text(
                                        text = "✓ Complete",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF4CAF50)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
