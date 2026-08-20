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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AutoAwesome
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.theme.rememberGitaColors

@Composable
fun DashboardRecommendationsCard(
    recommendations: List<RecommendationData>,
    onViewPlans: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = rememberGitaColors()
    var recoOpen by remember { mutableStateOf(true) }

    GlassCard(
        modifier = modifier.fillMaxWidth(),
        tint = colors.cardTint,
        cornerRadius = 32.dp,
        elevation = UiDefaults.ElevationDefault,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { recoOpen = !recoOpen }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = colors.chipText,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "RECOMMENDATIONS",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.chipText,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f),
                )

                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(colors.chipBg)
                        .border(1.dp, colors.chipBorder, MaterialTheme.shapes.large)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                ) {
                    Text(
                        text = "${recommendations.size} items",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = colors.chipText,
                    )
                }

                val rotationChevron by animateFloatAsState(
                    targetValue = if (recoOpen) 0f else -90f,
                    label = "reco_chevron_rotation",
                )
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(colors.subtleBg.copy(alpha = if (colors.isDark) 0.7f else 0.5f))
                        .border(1.dp, colors.subtleBorder.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        .graphicsLayer { rotationZ = rotationChevron },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (recoOpen) "Collapse" else "Expand",
                        tint = colors.buttonOutlineText,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            if (recoOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .padding(horizontal = 16.dp)
                        .background(colors.divider.copy(alpha = 0.6f)),
                )
            }

            AnimatedVisibility(
                visible = recoOpen,
                enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(300)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp),
                ) {
                    val rows = if (recommendations.isEmpty()) {
                        listOf(
                            RecommendationItem("Continue in Quiz Mode", "🎓", "Quiz"),
                            RecommendationItem("Review Chapter 1", "📖", "Read"),
                            RecommendationItem("Focus on Yoga Level 1", "🧘", "Level"),
                        )
                    } else {
                        recommendations.take(3).map { r ->
                            val (emoji, tag) = when (r.recommendationType) {
                                "verse" -> "📖" to "Read"
                                "topic" -> "🎓" to "Quiz"
                                "yogalevel" -> "🧘" to "Level"
                                "question" -> "🎙" to "Voice"
                                else -> "✦" to "Gita"
                            }
                            RecommendationItem(r.recommendationTitle, emoji, tag)
                        }
                    }

                    rows.forEachIndexed { i, item ->
                        RecommendationRow(
                            item = item,
                            accent = colors.accent,
                            accentSoft = colors.chipText,
                            isDark = colors.isDark,
                            showDivider = i < rows.size - 1,
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onViewPlans,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.chipBorder),
                            contentPadding = PaddingValues(vertical = 11.dp),
                        ) {
                            Text(
                                "View Plans",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (colors.isDark) Color.White else colors.textPrimary,
                            )
                        }

                        Button(
                            onClick = onViewAll,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (colors.isDark) {
                                    Color.White.copy(alpha = 0.06f)
                                } else {
                                    Color.Black.copy(alpha = 0.04f)
                                }
                            ),
                            shape = MaterialTheme.shapes.large,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (colors.isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f),
                            ),
                            contentPadding = PaddingValues(vertical = 11.dp),
                        ) {
                            Text(
                                "View All",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = colors.textDim,
                            )
                        }
                    }
                }
            }
        }
    }
}
