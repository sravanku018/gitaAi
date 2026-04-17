package com.aipoweredgita.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.theme.UiMotion

@Composable
fun GradientActionCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit),
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = UiDefaults.CornerRadius,
    iconSize: Dp = 40.dp,
    contentPadding: Dp = UiDefaults.CardPadding,
    elevation: Dp = UiDefaults.ElevationNone,
    titleFontSizeSp: Int = 18,
    descriptionFontSizeSp: Int = 14
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) UiMotion.PressedScale else 1f,
        animationSpec = tween(UiMotion.Short, easing = UiMotion.StandardEasing),
        label = "card-press-scale"
    )

    Card(
        modifier = modifier
            .scale(scale)
            .clickable(onClick = onClick, interactionSource = interactionSource, indication = null),
        shape = RoundedCornerShape(cornerRadius),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = Brush.horizontalGradient(gradient))
                .padding(contentPadding)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(iconSize), contentAlignment = Alignment.Center) {
                    icon()
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = titleFontSizeSp.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = description,
                        fontSize = descriptionFontSizeSp.sp,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}
