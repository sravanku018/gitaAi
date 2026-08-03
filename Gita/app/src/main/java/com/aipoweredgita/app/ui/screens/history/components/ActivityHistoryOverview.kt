package com.aipoweredgita.app.ui.screens.history.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.network.YogaLevel
import com.aipoweredgita.app.network.YogaSubStage
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.util.TimeUtils

@Composable
fun OverviewTab(
    userStats: UserStats?,
    totalCoins: Int,
    yogaLevels: List<YogaLevel>,
    yogaSubStages: List<YogaSubStage>,
    totalQuizAttempts: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        // ── Time Distribution Donut ──
        val normalTime = userStats?.normalModeTimeSeconds ?: 0L
        val quizTime = userStats?.quizModeTimeSeconds ?: 0L
        val voiceTime = userStats?.voiceStudioTimeSeconds ?: 0L
        val totalTime = normalTime + quizTime + voiceTime

        val readingColor = Color(0xFFE08A1E)   // saffron gold
        val quizColor = Color(0xFFC2410C)       // terracotta
        val chatColor = Color(0xFFF59E0B)       // amber

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Time Distribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Donut
                    Box(
                        modifier = Modifier.size(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val items = if (totalTime == 0L) {
                            listOf(Triple("Empty", 1f, Color.LightGray))
                        } else {
                            listOf(
                                Triple("Reading", normalTime.toFloat() / totalTime, readingColor),
                                Triple("Quiz", quizTime.toFloat() / totalTime, quizColor),
                                Triple("Chat", voiceTime.toFloat() / totalTime, chatColor)
                            )
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 18.dp.toPx()
                            var startAngle = -90f
                            items.forEach { (_, ratio, color) ->
                                val sweep = ratio * 360f
                                drawArc(
                                    color = color,
                                    startAngle = startAngle,
                                    sweepAngle = sweep,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweep
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = TimeUtils.formatTimeCompact(totalTime),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Legend
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        TimeDistributionLegendItem("📖 Reading", normalTime, readingColor)
                        TimeDistributionLegendItem("📝 Quiz", quizTime, quizColor)
                        TimeDistributionLegendItem("💬 Chat", voiceTime, chatColor)
                    }
                }
            }
        }

        // ── Stats Overview Cards ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                icon = "🔥",
                value = "${userStats?.currentStreak ?: 0}",
                label = "Streak",
                color = Color(0xFFE08A1E),
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                icon = "📅",
                value = "${userStats?.daysActive ?: 0}",
                label = "Days Active",
                color = Color(0xFFC2410C),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OverviewStatCard(
                icon = "📖",
                value = "${userStats?.versesRead ?: 0}",
                label = "Verses Read",
                color = Color(0xFF2D5016),
                modifier = Modifier.weight(1f)
            )
            OverviewStatCard(
                icon = "📝",
                value = "$totalQuizAttempts",
                label = "Quizzes Taken",
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
        }

        // ── Yoga Level Card ──
        val baseYogaInfo = YogaLevelManager.yogaLevelInfo(userStats)
        val baseProgress = YogaLevelManager.progressInLevel(userStats)

        val activeLevel = yogaLevels.find { totalCoins >= it.min_coins && totalCoins <= it.max_coins }
            ?: yogaLevels.lastOrNull()
        val activeSubStage = yogaSubStages.find { totalCoins >= it.min_coins && totalCoins <= it.max_coins }
            ?: yogaSubStages.filter { it.level == activeLevel?.level }.maxByOrNull { it.sub_level }

        val displayYogaName = activeLevel?.name ?: baseYogaInfo.yogaName
        val displayStep = activeSubStage?.sub_level ?: baseYogaInfo.step
        val displayEmoji = activeLevel?.let { yl ->
            when(yl.level) { 1 -> "🌿"; 2 -> "🔥"; 3 -> "🧠"; 4 -> "📘"; else -> "🌸" }
        } ?: baseYogaInfo.emoji

        val progress = activeLevel?.let { yl ->
            val range = yl.max_coins - yl.min_coins
            if (range > 0) {
                ((totalCoins - yl.min_coins).toFloat() / range).coerceIn(0f, 1f)
            } else 0f
        } ?: baseProgress

        val yogaLevel = activeLevel?.level ?: baseYogaInfo.level

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(
                            text = displayYogaName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Level $yogaLevel / 5 · Step $displayStep",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = activeLevel?.description ?: baseYogaInfo.yogaDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall),
                )
                Text(
                    text = "Progress: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Best Performance ──
        userStats?.let { stats ->
            if (stats.bestScoreOutOf > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🏆", fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Best Quiz Score",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${stats.bestScore}/${stats.bestScoreOutOf} (${stats.averageScorePercentage.toInt()}%)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Bottom spacing
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun TimeDistributionLegendItem(label: String, seconds: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(3.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = TimeUtils.formatTimeCompact(seconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OverviewStatCard(
    icon: String,
    value: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
