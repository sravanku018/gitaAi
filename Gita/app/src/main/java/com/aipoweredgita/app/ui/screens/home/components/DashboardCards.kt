package com.aipoweredgita.app.ui.screens.home.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.aipoweredgita.app.ui.theme.rememberThemeIsDark
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.LocalUiConfig

@Composable
fun Pill(
    text: String,
    color: Color = Color(0xFFFF6E00).copy(alpha = 0.25f),
    textColor: Color = Color(0xFFFFB450).copy(alpha = 0.9f)
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.large)
            .background(color)
            .border(1.dp, textColor.copy(alpha = 0.27f), MaterialTheme.shapes.large)
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DashboardModeCard(
    card: ModeCardData,
    bgContent: @Composable BoxScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
    ) {
        bgContent()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = card.emoji,
                    fontSize = 22.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(0f, 4f),
                            blurRadius = 14f
                        )
                    )
                )
            }
            Column {
                Text(
                    text = card.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.97f),
                    lineHeight = 16.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(0f, 1f),
                            blurRadius = 10f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = card.sub,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 0.2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 1f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

class DiamondShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(size.width / 2f, size.height)
            lineTo(0f, size.height / 2f)
            close()
        }
        return Outline.Generic(path)
    }
}

data class StatsData(
    val dayStreak: Int,
    val coins: Int
) {
    val dayStreakDisplay: String get() = "$dayStreak"
    val coinsDisplay: String get() = "🪙 $coins"
}

data class StatCardData(
    val emoji: String,
    val value: String,
    val label: String,
    val valueColor: Color = Color.White
)

data class RecommendationItem(
    val text: String,
    val icon: String,
    val tag: String
)

data class ModeCardData(
    val emoji: String,
    val title: String,
    val sub: String
)

@Composable
fun GlassStatRow(
    stats: StatsData
) {
    val uiCfg = LocalUiConfig.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val itemModifier = if (uiCfg.isLandscape) Modifier.width(180.dp) else Modifier.weight(1f)
        val diamondShape = remember { DiamondShape() }

        DiamondStatCard(
            modifier = itemModifier,
            diamondShape = diamondShape,
            card = StatCardData(
                emoji = "🔥",
                value = stats.dayStreakDisplay,
                label = "Day Streak",
                valueColor = Color(0xFFFFBE28)
            )
        )

        DiamondStatCard(
            modifier = itemModifier,
            diamondShape = diamondShape,
            card = StatCardData(
                emoji = "📤",
                value = stats.coinsDisplay,
                label = "Krishna Coins",
                valueColor = Color(0xFF64D8FF)
            )
        )
    }
}

@Composable
private fun DiamondStatCard(
    modifier: Modifier,
    diamondShape: DiamondShape,
    card: StatCardData
) {
    val isDark = rememberThemeIsDark()
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .shadow(6.dp, shape = diamondShape, clip = false)
            .clip(diamondShape)
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.13f), diamondShape)
            .drawBehind {
                if (isDark) {
                    val dp = Path().apply {
                        moveTo(size.width / 2f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(size.width / 2f, size.height)
                        lineTo(0f, size.height / 2f)
                        close()
                    }
                    clipPath(dp) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                                startY = 0f,
                                endY = size.height * 0.45f
                            )
                        )
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(card.emoji, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = card.value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = card.valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = card.label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.3f),
                letterSpacing = 0.4.sp
            )
        }
    }
}

@Composable
fun RecommendationRow(
    item: RecommendationItem,
    isDark: Boolean,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f), MaterialTheme.shapes.small),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, fontSize = 15.sp)
            }
            
            Text(
                text = item.text,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFFFF9628).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFFF9628).copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = item.tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA532),
                    letterSpacing = 0.4.sp
                )
            }
            
            Text(
                text = "›",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )
        }
        
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
            )
        }
    }
}
