package com.aipoweredgita.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.MotionTokens

// ─── M3 Expressive Split Button ─────────────────────────────────────────────
// A primary action button with a secondary trigger (dropdown arrow).
// Both parts share the same background but are separated by a divider.

@Composable
fun M3SplitButton(
    label: String,
    onClick: () -> Unit,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),  // M3X expressive uses 16dp
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    height: Dp = 48.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 500f),
        label = "split_scale"
    )
    val bg by animateColorAsState(
        targetValue = if (enabled) containerColor else containerColor.copy(alpha = 0.38f),
        animationSpec = spring<Color>(dampingRatio = 1f, stiffness = 400f),
        label = "split_bg"
    )

    Row(
        modifier = modifier
            .scale(scale)
            .height(height)
            .clip(shape)
            .background(bg)
            .border(0.5.dp, contentColor.copy(alpha = 0.12f), shape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Main action
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
            )
        }

        // Vertical divider
        Box(
            modifier = Modifier
                .width(0.5.dp)
                .height(24.dp)
                .background(contentColor.copy(alpha = 0.15f))
        )

        // Secondary trigger
        Box(
            modifier = Modifier
                .width(44.dp)
                .fillMaxHeight()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onSecondaryClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "▾",
                fontSize = 16.sp,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.38f)
            )
        }
    }
}

// ─── M3 Expressive Button Group ─────────────────────────────────────────────
// A horizontal row of connected buttons with M3X shape.

@Composable
fun M3ButtonGroup(
    items: List<M3ButtonGroupItem>,
    selectedIndex: Int = -1,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    Row(modifier = modifier.clip(shape).border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, shape)) {
        items.forEachIndexed { i, item ->
            val isSelected = i == selectedIndex
            val isFirst = i == 0
            val isLast = i == items.size - 1
            val itemShape = when {
                items.size == 1 -> shape
                isFirst -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                isLast -> RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                else -> RoundedCornerShape(0.dp)
            }

            Box(
                modifier = Modifier
                    .weight(item.weight)
                    .height(48.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                        else Color.Transparent
                    )
                    .then(
                        if (!isLast) Modifier.border(
                            0.5.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(0.dp)
                        ) else Modifier
                    )
                    .clickable { onItemClick(i) }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (item.icon != null) {
                        Text(item.icon, fontSize = 18.sp)
                    }
                    Text(
                        item.label,
                        fontSize = if (item.icon != null) 10.sp else 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

data class M3ButtonGroupItem(
    val label: String,
    val icon: String? = null,
    val weight: Float = 1f
)
