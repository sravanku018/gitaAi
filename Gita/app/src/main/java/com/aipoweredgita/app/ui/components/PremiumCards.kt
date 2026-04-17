package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.theme.UiMotion

@Composable
fun PremiumDashboardCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit),
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = tween(100),
        label = "press_scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .height(110.dp),
        shape = RoundedCornerShape(cornerRadius),
        color = Color.Transparent, // We'll handle background manually
        shadowElevation = if (pressed) 2.dp else 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            gradient[0].copy(alpha = 0.9f),
                            gradient[1].copy(alpha = 0.85f)
                        )
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // Custom scale animation replaces ripple if desired, or keep ripple
                    onClick = onClick
                )
        ) {
            // Glassmorphism shine
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon container with slight bloom
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    icon()
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
