package com.aipoweredgita.app.ui.screens.home.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.theme.rememberGitaColors
import com.aipoweredgita.app.viewmodel.ProfileViewModel

@Composable
fun DashboardHeroCard(
    nextAction: ProfileViewModel.NextActionData,
    language: String,
    onPrimaryAction: () -> Unit,
    onViewPlan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberGitaColors()
    var heroOpen by remember { mutableStateOf(true) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = colors.heroTint,
        border = colors.heroBorder,
        cornerRadius = 32.dp,
        elevation = if (colors.isDark) UiDefaults.ElevationEmphasis else UiDefaults.ElevationDefault,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                colors.heroGradientStart,
                                colors.heroGradientEnd,
                                Color.Transparent,
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(size.width, size.height),
                        )
                    )
                }
        ) {
            com.aipoweredgita.app.ui.components.MandalaBackground(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(140.dp)
                    .offset(x = 10.dp, y = (-10).dp),
                color = colors.accent.copy(alpha = 0.07f),
            )

            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { heroOpen = !heroOpen }
                        .padding(horizontal = UiDefaults.CardPadding, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "🙏",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Namaste",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colors.heroTitle,
                        )
                        Text(
                            text = "Continue your spiritual journey",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.heroSubtitle,
                        )
                    }
                    val rotationChevron by animateFloatAsState(
                        targetValue = if (heroOpen) 0f else -90f,
                        label = "chevron_rotation",
                    )
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(MaterialTheme.shapes.small)
                            .background(colors.subtleBg)
                            .border(1.dp, colors.subtleBorder, MaterialTheme.shapes.small)
                            .graphicsLayer { rotationZ = rotationChevron },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = if (heroOpen) "Collapse" else "Expand",
                            tint = colors.buttonOutlineText,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }

                AnimatedVisibility(
                    visible = heroOpen,
                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(300)),
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(300)),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = UiDefaults.CardPadding,
                                end = UiDefaults.CardPadding,
                                bottom = 18.dp,
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colors.divider)
                        )
                        Spacer(modifier = Modifier.height(13.dp))
                        Pill(
                            text = "NEXT BEST ACTION",
                            color = colors.pillBg,
                            textColor = colors.pillText,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (nextAction.nextStep != null && nextAction.nextLevel > 0) {
                                "${nextAction.nextStep ?: ""} at Level ${nextAction.nextLevel} · ${nextAction.nextReason ?: "Balance your modes"}"
                            } else {
                                "Embark on your sacred journey through the Gita. Read verses to build your daily wisdom."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.heroSubtitle,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val actionText = when (nextAction.nextStep) {
                                "Read" -> "Start Reading"
                                "Quiz" -> "Start Quiz"
                                "Studio" -> if (language == "tel") "ప్రశ్న సమాధానం" else "Voice Q&A"
                                else -> "Begin Reading"
                            }

                            Button(
                                onClick = onPrimaryAction,
                                colors = ButtonDefaults.buttonColors(containerColor = colors.buttonPrimary),
                                shape = RoundedCornerShape(50.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (colors.isDark) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                                ),
                            ) {
                                Text(
                                    text = actionText,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }

                            Button(
                                onClick = onViewPlan,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                shape = RoundedCornerShape(50.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.buttonOutline),
                            ) {
                                Text(
                                    text = "View Plan",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.buttonOutlineText,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
