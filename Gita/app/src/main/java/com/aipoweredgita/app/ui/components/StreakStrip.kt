package com.aipoweredgita.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.foundation.shape.CircleShape

private val springSnap = spring<Float>(dampingRatio = 0.5f, stiffness = 600f)
private val springBounce = spring<Float>(dampingRatio = 0.3f, stiffness = 400f)
private val springColor = spring<Color>(dampingRatio = 0.7f, stiffness = 500f)

@Composable
fun StreakStrip(
    days: Int = 7,
    isClaimed: (Int) -> Boolean,
    isToday: (Int) -> Boolean,
    wasJustClaimed: (Int) -> Boolean,
    onDayClick: (Int) -> Unit,
    activeColor: Color,
    completedColor: Color = Color(0xFF4CAF50),
    dimColor: Color,
    bgColor: Color,
    bdColor: Color,
    animateEntry: Boolean = false
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (d in 1..days) {
            val claimed = isClaimed(d)
            val today = isToday(d)
            val justClaimed = wasJustClaimed(d)

            val boxScale by animateFloatAsState(
                targetValue = if (justClaimed) 1.18f else 1f,
                animationSpec = springBounce, label = "box_scale_$d"
            )
            val checkAlpha by animateFloatAsState(
                targetValue = if (claimed) 1f else 0f,
                animationSpec = springSnap, label = "check_alpha_$d"
            )
            
            val currentBgColor by animateColorAsState(
                targetValue = when {
                    claimed -> completedColor.copy(alpha = 0.22f)
                    today -> activeColor.copy(alpha = 0.18f)
                    else -> bgColor
                },
                animationSpec = springColor, label = "bg_color_$d"
            )
            val currentBdColor by animateColorAsState(
                targetValue = when {
                    claimed -> completedColor.copy(alpha = 0.6f)
                    today -> activeColor
                    else -> bdColor
                },
                animationSpec = springColor, label = "border_color_$d"
            )

            val entryAlpha by animateFloatAsState(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                label = "entry_alpha_$d"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .then(
                        if (animateEntry) {
                            Modifier.graphicsLayer {
                                alpha = entryAlpha.coerceAtMost(1f)
                                translationY = (1f - entryAlpha) * 12f
                            }
                        } else {
                            Modifier.scale(boxScale)
                        }
                    )
                    .clip(CircleShape)
                    .background(currentBgColor)
                    .border(if (today) 1.8.dp else 0.8.dp, currentBdColor, CircleShape)
                    .clickable { onDayClick(d) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    if (claimed) {
                        Text(
                            text = "✓", 
                            fontSize = 14.sp, 
                            color = completedColor, 
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer { alpha = checkAlpha }
                        )
                    } else {
                        Text(
                            text = "+$d", 
                            fontSize = 12.sp, 
                            fontWeight = FontWeight.Bold, 
                            color = if (today) activeColor else dimColor
                        )
                    }
                }
            }
        }
    }
}
