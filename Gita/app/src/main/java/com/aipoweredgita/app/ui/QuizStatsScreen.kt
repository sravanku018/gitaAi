package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.viewmodel.QuizStatsViewModel

@Composable
fun QuizStatsScreen(
    modifier: Modifier = Modifier,
    viewModel: QuizStatsViewModel = viewModel()
) {
    val attempts by viewModel.attempts.collectAsState()
    val averageAccuracy by viewModel.averageAccuracy.collectAsState()
    val averageTime by viewModel.averageTime.collectAsState()

    val quiz10Stats by viewModel.quiz10Stats.collectAsState()
    val quiz20Stats by viewModel.quiz20Stats.collectAsState()
    val quiz30Stats by viewModel.quiz30Stats.collectAsState()
    val selectedQuizSize by viewModel.selectedQuizSize.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }

    val uiCfg = LocalUiConfig.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
    ) {
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("History") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Performance") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Tips") }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quiz Size Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedQuizSize == null,
                onClick = { viewModel.selectQuizSize(null) },
                label = { Text("All") }
            )
            if (quiz10Stats != null) {
                FilterChip(
                    selected = selectedQuizSize == 10,
                    onClick = { viewModel.selectQuizSize(10) },
                    label = { Text("10 Questions (${quiz10Stats?.totalAttempts ?: 0})") }
                )
            }
            if (quiz20Stats != null) {
                FilterChip(
                    selected = selectedQuizSize == 20,
                    onClick = { viewModel.selectQuizSize(20) },
                    label = { Text("20 Questions (${quiz20Stats?.totalAttempts ?: 0})") }
                )
            }
            if (quiz30Stats != null) {
                FilterChip(
                    selected = selectedQuizSize == 30,
                    onClick = { viewModel.selectQuizSize(30) },
                    label = { Text("30 Questions (${quiz30Stats?.totalAttempts ?: 0})") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Get filtered attempts and stats
        val currentStats = when (selectedQuizSize) {
            10 -> quiz10Stats
            20 -> quiz20Stats
            30 -> quiz30Stats
            else -> null
        }

        val displayAttempts = currentStats?.attempts ?: attempts
        val displayAvgAccuracy = currentStats?.averageAccuracy ?: averageAccuracy
        val displayAvgTime = currentStats?.averageTime ?: averageTime

        // Tab Content
        when (selectedTab) {
            0 -> HistoryTab(attempts = displayAttempts, quizSize = selectedQuizSize)
            1 -> PerformanceTab(
                attempts = displayAttempts,
                averageAccuracy = displayAvgAccuracy,
                averageTime = displayAvgTime,
                bestAttempt = currentStats?.bestAttempt
            )
            2 -> TipsTab(averageAccuracy = displayAvgAccuracy)
        }
    }
}

@Composable
fun HistoryTab(attempts: List<QuizAttempt>, quizSize: Int?) {
    if (attempts.isEmpty()) {
        val message = if (quizSize != null) {
            "No quiz attempts for $quizSize questions yet.\nTry a different quiz size!"
        } else {
            "No quiz attempts yet.\nStart a quiz to see your history!"
        }
        EmptyState(message = message)
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (quizSize != null) "Showing ${attempts.size} attempts for $quizSize questions" else "Showing all ${attempts.size} attempts",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            items(attempts, key = { it.id }) { attempt -> 
                QuizAttemptCard(attempt = attempt)
            }
        }
    }
}

@Composable
fun QuizAttemptCard(attempt: QuizAttempt) {
    val backgroundColor = when {
        attempt.accuracyPercentage >= 90 -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        attempt.accuracyPercentage >= 75 -> Color(0xFF2196F3).copy(alpha = 0.1f)
        attempt.accuracyPercentage >= 60 -> Color(0xFFFFC107).copy(alpha = 0.1f)
        else -> Color(0xFFFF5252).copy(alpha = 0.1f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${attempt.performanceEmoji} ${attempt.score}/${attempt.totalQuestions}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${attempt.accuracyPercentage.toInt()}%",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Time: ${attempt.timeSpentFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = attempt.performanceLevel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when (attempt.performanceLevel) {
                    "Excellent" -> Color(0xFF4CAF50)
                    "Good" -> Color(0xFF2196F3)
                    "Average" -> Color(0xFFFFC107)
                    else -> Color(0xFFFF5252)
                }
            )
        }
    }
}

@Composable
fun PerformanceTab(
    attempts: List<QuizAttempt>,
    averageAccuracy: Float,
    averageTime: Long,
    bestAttempt: QuizAttempt?
) {
    if (attempts.isEmpty()) {
        EmptyState(message = "No performance data available yet.\nComplete some quizzes to see your stats!")
    } else {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            // Average Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PerformanceCard(
                    title = "Avg Accuracy",
                    value = "${averageAccuracy.toInt()}%",
                    icon = "🎯",
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )

                val avgTimeFormatted = "${averageTime / 60}m ${averageTime % 60}s"
                PerformanceCard(
                    title = "Avg Time",
                    value = avgTimeFormatted,
                    icon = "⏱️",
                    color = Color(0xFFEC4899),
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PerformanceCard(
                    title = "Total Attempts",
                    value = "${attempts.size}",
                    icon = "📝",
                    color = Color(0xFF10B981),
                    modifier = Modifier.weight(1f)
                )

                PerformanceCard(
                    title = "Questions",
                    value = if (attempts.firstOrNull()?.totalQuestions != null) "${attempts.first().totalQuestions}" else "-",
                    icon = "❓",
                    color = Color(0xFFF59E0B),
                    modifier = Modifier.weight(1f)
                )
            }

            // Best Attempt
            val best = bestAttempt ?: attempts.maxByOrNull { it.accuracyPercentage }
            best?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFD700).copy(alpha = 0.1f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🏆",
                                fontSize = 32.sp
                            )
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
            }

            // Recent Trend
            Text(
                text = "Recent Attempts (Last 5)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            attempts.take(5).forEach { attempt ->
                QuizAttemptCard(attempt = attempt)
            }
        }
    }
}

@Composable
fun PerformanceCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 32.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
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
fun TipsTab(averageAccuracy: Float) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TipCard(
                icon = "📖",
                title = "Study Regularly",
                tip = "Read verses daily to improve retention. Consistency is key to understanding the Bhagavad Gita's teachings.",
                color = Color(0xFF6366F1)
            )
        }

        if (averageAccuracy < 60) {
            item {
                TipCard(
                    icon = "💡",
                    title = "Focus on Understanding",
                    tip = "Don't just memorize! Try to understand the meaning and context of each verse. Use Normal Mode to read explanations before taking quizzes.",
                    color = Color(0xFFFF5252)
                )
            }
        }

        if (averageAccuracy >= 60 && averageAccuracy < 80) {
            item {
                TipCard(
                    icon = "🎯",
                    title = "Practice More",
                    tip = "You're doing well! Keep practicing with different question types to improve your accuracy further.",
                    color = Color(0xFFFFC107)
                )
            }
        }

        if (averageAccuracy >= 80) {
            item {
                TipCard(
                    icon = "🌟",
                    title = "Excellent Work!",
                    tip = "You have a great understanding! Consider helping others learn and sharing your knowledge of the Gita.",
                    color = Color(0xFF4CAF50)
                )
            }
        }

        item {
            TipCard(
                icon = "🧘",
                title = "Reflect on Teachings",
                tip = "After each quiz, spend a moment reflecting on how the teachings apply to your life. The Gita is meant to be lived, not just learned.",
                color = Color(0xFF8B5CF6)
            )
        }

        item {
            TipCard(
                icon = "🔄",
                title = "Review Mistakes",
                tip = "Go back to verses you got wrong in quizzes. Understanding your mistakes is the fastest way to improve.",
                color = Color(0xFFEC4899)
            )
        }

        item {
            TipCard(
                icon = "⏰",
                title = "Set a Goal",
                tip = "Try to improve your accuracy by 5% each week. Small, consistent improvements lead to mastery!",
                color = Color(0xFF10B981)
            )
        }
    }
}

@Composable
fun TipCard(
    icon: String,
    title: String,
    tip: String,
    color: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = icon,
                fontSize = 40.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "📊",
                fontSize = 64.sp
            )
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
