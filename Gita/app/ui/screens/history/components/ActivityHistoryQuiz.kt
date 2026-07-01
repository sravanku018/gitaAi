package com.aipoweredgita.app.ui.screens.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ui.charts.PerformanceTrendLineChart
import com.aipoweredgita.app.ui.charts.SpiritualPathRadarChart
import com.aipoweredgita.app.viewmodel.QuizSizeStatsData

@Composable
fun QuizTab(
    attempts: List<QuizAttempt>,
    averageAccuracy: Float,
    averageTime: Long,
    quiz10Stats: QuizSizeStatsData?,
    quiz15Stats: QuizSizeStatsData?,
    quiz20Stats: QuizSizeStatsData?,
    quiz25Stats: QuizSizeStatsData?,
    quiz30Stats: QuizSizeStatsData?,
    selectedQuizSize: Int?,
    onSelectQuizSize: (Int?) -> Unit,
    userStats: UserStats?,
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int,
    dhyanaCount: Int,
    rajaCount: Int
) {
    // Quiz Size Filter Chips
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedQuizSize == null,
            onClick = { onSelectQuizSize(null) },
            label = { Text("All") }
        )
        if (quiz10Stats != null) {
            FilterChip(
                selected = selectedQuizSize == 10,
                onClick = { onSelectQuizSize(10) },
                label = { Text("10Q (${quiz10Stats.totalAttempts})") }
            )
        }
        if (quiz15Stats != null) {
            FilterChip(
                selected = selectedQuizSize == 15,
                onClick = { onSelectQuizSize(15) },
                label = { Text("15Q (${quiz15Stats.totalAttempts})") }
            )
        }
        if (quiz20Stats != null) {
            FilterChip(
                selected = selectedQuizSize == 20,
                onClick = { onSelectQuizSize(20) },
                label = { Text("20Q (${quiz20Stats.totalAttempts})") }
            )
        }
        if (quiz25Stats != null) {
            FilterChip(
                selected = selectedQuizSize == 25,
                onClick = { onSelectQuizSize(25) },
                label = { Text("25Q (${quiz25Stats.totalAttempts})") }
            )
        }
        if (quiz30Stats != null) {
            FilterChip(
                selected = selectedQuizSize == 30,
                onClick = { onSelectQuizSize(30) },
                label = { Text("30Q (${quiz30Stats.totalAttempts})") }
            )
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    val currentStats = when (selectedQuizSize) {
        10 -> quiz10Stats
        15 -> quiz15Stats
        20 -> quiz20Stats
        25 -> quiz25Stats
        30 -> quiz30Stats
        else -> null
    }
    val displayAttempts = currentStats?.attempts ?: attempts
    val displayAvgAccuracy = currentStats?.averageAccuracy ?: averageAccuracy
    val displayAvgTime = currentStats?.averageTime ?: averageTime

    if (displayAttempts.isEmpty()) {
        AHEmptyState(message = "No quiz attempts yet.\nStart a quiz to see your performance!")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Quick stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AHPerformanceCard(
                    title = "Avg Accuracy",
                    value = "${displayAvgAccuracy.toInt()}%",
                    icon = "🎯",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                val avgTimeFormatted = "${displayAvgTime / 60}m ${displayAvgTime % 60}s"
                AHPerformanceCard(
                    title = "Avg Time",
                    value = avgTimeFormatted,
                    icon = "⏱️",
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AHPerformanceCard(
                    title = "Attempts",
                    value = "${displayAttempts.size}",
                    icon = "📝",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                AHPerformanceCard(
                    title = "Accuracy",
                    value = "${userStats?.accuracyPercentage?.toInt() ?: 0}%",
                    icon = "🎯",
                    color = Color(0xFF8B5CF6),
                    modifier = Modifier.weight(1f)
                )
            }

            // Performance Trend Chart
            PerformanceTrendLineChart(attempts = displayAttempts)

            // Spiritual Path Radar
            SpiritualPathRadarChart(
                karmaCount = karmaCount,
                bhaktiCount = bhaktiCount,
                jnanaCount = jnanaCount,
                dhyanaCount = dhyanaCount,
                rajaCount = rajaCount
            )

            // Best attempt
            val best = currentStats?.bestAttempt ?: displayAttempts.maxByOrNull { it.accuracyPercentage }
            best?.let {
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
                                text = "Best Performance",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${best.score}/${best.totalQuestions} (${best.accuracyPercentage.toInt()}%)",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = best.dateFormatted,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Recent attempts
            Text(
                text = "Recent Attempts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            displayAttempts.take(5).forEach { attempt ->
                AHQuizAttemptCard(attempt = attempt)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AHPerformanceCard(
    title: String,
    value: String,
    icon: String,
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
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 28.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AHQuizAttemptCard(attempt: QuizAttempt) {
    val statusColor = when {
        attempt.accuracyPercentage >= 90 -> MaterialTheme.colorScheme.primary
        attempt.accuracyPercentage >= 75 -> MaterialTheme.colorScheme.secondary
        attempt.accuracyPercentage >= 60 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val bgColor = statusColor.copy(alpha = 0.08f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top row: emoji + score + accuracy + performance level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = attempt.performanceEmoji,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "${attempt.score}/${attempt.totalQuestions}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${attempt.accuracyPercentage.toInt()}% accuracy",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Circular accuracy badge
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(statusColor.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${attempt.accuracyPercentage.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Bottom row: time spent + finish time + performance level
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Time spent icon + text
                    Text("⏱", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = attempt.timeSpentFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    // Finish time
                    Text("🕐", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = attempt.dateFormatted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Performance level badge
                Surface(
                    color = statusColor.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = attempt.performanceLevel,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AHEmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = "📊", fontSize = 64.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
