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
<<<<<<< HEAD
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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

<<<<<<< HEAD
    val userStats by viewModel.userStats.collectAsState()
    val karmaCount by viewModel.karmaYogaCount.collectAsState()
    val bhaktiCount by viewModel.bhaktiYogaCount.collectAsState()
    val jnanaCount by viewModel.jnanaYogaCount.collectAsState()

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                bestAttempt = currentStats?.bestAttempt,
                userStats = userStats,
                karmaCount = karmaCount,
                bhaktiCount = bhaktiCount,
                jnanaCount = jnanaCount
=======
                bestAttempt = currentStats?.bestAttempt
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
        attempt.accuracyPercentage >= 90 -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        attempt.accuracyPercentage >= 75 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        attempt.accuracyPercentage >= 60 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    }

    val statusColor = when {
        attempt.accuracyPercentage >= 90 -> MaterialTheme.colorScheme.primary
        attempt.accuracyPercentage >= 75 -> MaterialTheme.colorScheme.secondary
        attempt.accuracyPercentage >= 60 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
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
                color = statusColor
            )
        }
    }
}

@Composable
fun PerformanceTab(
    attempts: List<QuizAttempt>,
    averageAccuracy: Float,
    averageTime: Long,
<<<<<<< HEAD
    bestAttempt: QuizAttempt?,
    userStats: com.aipoweredgita.app.database.UserStats?,
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int
=======
    bestAttempt: QuizAttempt?
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                val avgTimeFormatted = "${averageTime / 60}m ${averageTime % 60}s"
                PerformanceCard(
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
                PerformanceCard(
                    title = "Total Attempts",
                    value = "${attempts.size}",
                    icon = "📝",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )

                PerformanceCard(
                    title = "Questions",
                    value = if (attempts.firstOrNull()?.totalQuestions != null) "${attempts.first().totalQuestions}" else "-",
                    icon = "❓",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

<<<<<<< HEAD
            // Charts Section
            PerformanceTrendLineChart(attempts = attempts)
            
            userStats?.let {
                ActivityDistributionDonutChart(
                    normalTime = it.normalModeTimeSeconds,
                    quizTime = it.quizModeTimeSeconds,
                    voiceTime = it.voiceStudioTimeSeconds
                )
            }

            SpiritualPathRadarChart(
                karmaCount = karmaCount,
                bhaktiCount = bhaktiCount,
                jnanaCount = jnanaCount
            )

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            // Best Attempt
            val best = bestAttempt ?: attempts.maxByOrNull { it.accuracyPercentage }
            best?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
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
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (averageAccuracy < 60) {
            item {
                TipCard(
                    icon = "💡",
                    title = "Focus on Understanding",
                    tip = "Don't just memorize! Try to understand the meaning and context of each verse. Use Normal Mode to read explanations before taking quizzes.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        if (averageAccuracy >= 60 && averageAccuracy < 80) {
            item {
                TipCard(
                    icon = "🎯",
                    title = "Practice More",
                    tip = "You're doing well! Keep practicing with different question types to improve your accuracy further.",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        if (averageAccuracy >= 80) {
            item {
                TipCard(
                    icon = "🌟",
                    title = "Excellent Work!",
                    tip = "You have a great understanding! Consider helping others learn and sharing your knowledge of the Gita.",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        item {
            TipCard(
                icon = "🧘",
                title = "Reflect on Teachings",
                tip = "After each quiz, spend a moment reflecting on how the teachings apply to your life. The Gita is meant to be lived, not just learned.",
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        item {
            TipCard(
                icon = "🔄",
                title = "Review Mistakes",
                tip = "Go back to verses you got wrong in quizzes. Understanding your mistakes is the fastest way to improve.",
                color = MaterialTheme.colorScheme.secondary
            )
        }

        item {
            TipCard(
                icon = "⏰",
                title = "Set a Goal",
                tip = "Try to improve your accuracy by 5% each week. Small, consistent improvements lead to mastery!",
                color = MaterialTheme.colorScheme.tertiary
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
<<<<<<< HEAD

@Composable
fun PerformanceTrendLineChart(attempts: List<QuizAttempt>) {
    val last10Attempts = remember(attempts) {
        attempts.take(10).reversed()
    }
    
    if (last10Attempts.isEmpty()) return

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }
    var tooltipOffset by remember { mutableStateOf(Offset.Zero) }

    val saffronGold = Color(0xFFE08A1E)
    val terracotta = Color(0xFFC2410C)
    val onSurface = MaterialTheme.colorScheme.onSurface

    // Caching Path objects to eliminate allocations inside DrawScope
    val cachedLinePath = remember { Path() }
    val cachedFillPath = remember { Path() }

    // Chart entry animation
    var animationTriggered by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "line_chart_anim"
    )
    LaunchedEffect(last10Attempts) {
        animationTriggered = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Performance Trend (Last 10 Quizzes)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                val density = androidx.compose.ui.platform.LocalDensity.current
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(last10Attempts, animationProgress) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val height = size.height
                                val paddingLeft = with(density) { 40.dp.toPx() }
                                val paddingRight = with(density) { 20.dp.toPx() }
                                val paddingTop = with(density) { 20.dp.toPx() }
                                val paddingBottom = with(density) { 40.dp.toPx() }
                                val chartWidth = width - paddingLeft - paddingRight
                                val chartHeight = height - paddingTop - paddingBottom

                                val pointsCount = last10Attempts.size
                                val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth

                                var closestIndex = -1
                                var minDistance = Float.MAX_VALUE

                                for (i in last10Attempts.indices) {
                                    val attempt = last10Attempts[i]
                                    val x = paddingLeft + i * stepX
                                    val y = paddingTop + chartHeight - (attempt.accuracyPercentage / 100f) * chartHeight * animationProgress
                                    val distance = (offset.x - x) * (offset.x - x) + (offset.y - y) * (offset.y - y)
                                    val touchThreshold = with(density) { 40.dp.toPx() }
                                    if (distance < minDistance && distance < touchThreshold * touchThreshold) {
                                        minDistance = distance
                                        closestIndex = i
                                        tooltipOffset = Offset(x, y)
                                    }
                                }
                                selectedPointIndex = if (closestIndex != -1) closestIndex else null
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 40.dp.toPx()
                    val paddingRight = 20.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingBottom = 40.dp.toPx()

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // Draw Horizontal Gridlines & Labels
                    val gridLines = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
                    val paint = android.graphics.Paint().apply {
                        color = onSurface.copy(alpha = 0.6f).toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }

                    gridLines.forEach { percentage ->
                        val y = paddingTop + chartHeight - percentage * chartHeight
                        drawLine(
                            color = onSurface.copy(alpha = 0.1f),
                            start = Offset(paddingLeft, y),
                            end = Offset(width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(percentage * 100).toInt()}%",
                            paddingLeft - 8.dp.toPx(),
                            y + 4.dp.toPx(),
                            paint
                        )
                    }

                    if (last10Attempts.isEmpty()) return@Canvas

                    val pointsCount = last10Attempts.size
                    val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth

                    cachedLinePath.reset()
                    cachedFillPath.reset()

                    for (i in last10Attempts.indices) {
                        val attempt = last10Attempts[i]
                        val x = paddingLeft + i * stepX
                        val y = paddingTop + chartHeight - (attempt.accuracyPercentage / 100f) * chartHeight * animationProgress

                        if (i == 0) {
                            cachedLinePath.moveTo(x, y)
                            cachedFillPath.moveTo(x, paddingTop + chartHeight)
                            cachedFillPath.lineTo(x, y)
                        } else {
                            val prevAttempt = last10Attempts[i - 1]
                            val prevX = paddingLeft + (i - 1) * stepX
                            val prevY = paddingTop + chartHeight - (prevAttempt.accuracyPercentage / 100f) * chartHeight * animationProgress
                            
                            // Cubic Bezier curve control points
                            val cp1x = prevX + (x - prevX) / 2f
                            val cp1y = prevY
                            val cp2x = prevX + (x - prevX) / 2f
                            val cp2y = y
                            
                            cachedLinePath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                            cachedFillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                        }

                        if (i == last10Attempts.lastIndex) {
                            cachedFillPath.lineTo(x, paddingTop + chartHeight)
                            cachedFillPath.close()
                        }
                    }

                    // Draw Gradient Fill under line
                    drawPath(
                        path = cachedFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(saffronGold.copy(alpha = 0.3f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )

                    // Draw Line
                    drawPath(
                        path = cachedLinePath,
                        color = saffronGold,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw Points
                    for (i in last10Attempts.indices) {
                        val attempt = last10Attempts[i]
                        val x = paddingLeft + i * stepX
                        val y = paddingTop + chartHeight - (attempt.accuracyPercentage / 100f) * chartHeight * animationProgress

                        // Accent border
                        drawCircle(
                            color = terracotta,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    // Draw X axis labels
                    val labelPaint = android.graphics.Paint().apply {
                        color = onSurface.copy(alpha = 0.6f).toArgb()
                        textSize = 8.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    for (i in last10Attempts.indices) {
                        val x = paddingLeft + i * stepX
                        val y = paddingTop + chartHeight + 16.dp.toPx()
                        val text = "Q${i + 1}"
                        drawContext.canvas.nativeCanvas.drawText(
                            text,
                            x,
                            y,
                            labelPaint
                        )
                    }
                }

                // Tooltip Overlay
                selectedPointIndex?.let { index ->
                    val attempt = last10Attempts[index]
                    Tooltip(
                        attempt = attempt,
                        offset = tooltipOffset,
                        modifier = Modifier.align(Alignment.TopStart)
                    )
                }
            }
        }
    }
}

@Composable
fun Tooltip(
    attempt: QuizAttempt,
    offset: Offset,
    modifier: Modifier = Modifier
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val xDp = with(density) { offset.x.toDp() }
    val yDp = with(density) { offset.y.toDp() }

    Box(
        modifier = modifier
            .offset(x = xDp - 50.dp, y = yDp - 65.dp)
            .background(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = RoundedCornerShape(8.dp)
            )
            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${attempt.accuracyPercentage.toInt()}% Acc",
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = attempt.dateFormatted,
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun ActivityDistributionDonutChart(
    normalTime: Long,
    quizTime: Long,
    voiceTime: Long
) {
    val totalTime = normalTime + quizTime + voiceTime
    val saffronGold = Color(0xFFE08A1E)
    val terracotta = Color(0xFFC2410C)
    val amberYellow = Color(0xFFF59E0B)
    val onSurface = MaterialTheme.colorScheme.onSurface

    val items = remember(normalTime, quizTime, voiceTime) {
        if (totalTime == 0L) {
            listOf(
                Triple("Empty", 1f, Color.LightGray)
            )
        } else {
            listOf(
                Triple("Normal Mode", normalTime.toFloat() / totalTime, saffronGold),
                Triple("Quiz Mode", quizTime.toFloat() / totalTime, terracotta),
                Triple("Voice Studio", voiceTime.toFloat() / totalTime, amberYellow)
            )
        }
    }

    var animationTriggered by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "donut_chart_anim"
    )
    LaunchedEffect(normalTime, quizTime, voiceTime) {
        animationTriggered = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Study Time Distribution",
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
                // Donut Chart Canvas
                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 16.dp.toPx()
                        
                        // Background track circle
                        drawCircle(
                            color = onSurface.copy(alpha = 0.05f),
                            radius = (size.minDimension - strokeWidth) / 2f,
                            style = Stroke(width = strokeWidth)
                        )

                        var startAngle = -90f
                        items.forEach { (_, sweepRatio, color) ->
                            val sweepAngle = sweepRatio * 360f * animationProgress
                            drawArc(
                                color = color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                            startAngle += sweepRatio * 360f // Continue startAngle based on full ratio
                        }
                    }
                    
                    // Center Text
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Time",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val totalHours = totalTime / 3600
                        val totalMinutes = (totalTime % 3600) / 60
                        val displayStr = when {
                            totalHours > 0 -> "${totalHours}h ${totalMinutes}m"
                            totalMinutes > 0 -> "${totalMinutes}m"
                            else -> "${totalTime}s"
                        }
                        Text(
                            text = displayStr,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Legend
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items.forEach { (label, ratio, color) ->
                        if (label != "Empty") {
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
                                    val percent = (ratio * 100).toInt()
                                    Text(
                                        text = "$percent%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpiritualPathRadarChart(
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int
) {
    val saffronGold = Color(0xFFE08A1E)
    val terracotta = Color(0xFFC2410C)
    val onSurface = MaterialTheme.colorScheme.onSurface

    val targetMax = 50f
    val karmaProgress = (karmaCount.toFloat() / targetMax).coerceAtMost(1.0f)
    val bhaktiProgress = (bhaktiCount.toFloat() / targetMax).coerceAtMost(1.0f)
    val jnanaProgress = (jnanaCount.toFloat() / targetMax).coerceAtMost(1.0f)

    // Caching Path objects to eliminate allocations inside DrawScope
    val ringPath = remember { Path() }
    val progressPath = remember { Path() }

    // Chart entry animation
    var animationTriggered by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "radar_chart_anim"
    )
    LaunchedEffect(karmaCount, bhaktiCount, jnanaCount) {
        animationTriggered = true
    }

    val archetype = remember(karmaCount, bhaktiCount, jnanaCount) {
        val k = karmaCount
        val b = bhaktiCount
        val j = jnanaCount

        when {
            k == 0 && b == 0 && j == 0 -> "Aspirant (Beginner's Mind)"
            kotlin.math.abs(k - b) <= 5 && kotlin.math.abs(b - j) <= 5 && kotlin.math.abs(k - j) <= 5 -> "Raja Yogi (The Harmonious Path)"
            k > b && k > j -> "Karma Yogi (The Active Path)"
            b > k && b > j -> "Bhakti Yogi (The Devotional Path)"
            j > k && j > b -> "Jnana Yogi (The Philosophical Path)"
            else -> "Aspirant (Beginner's Mind)"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spiritual Archetype Progression",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = archetype,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = terracotta,
                modifier = Modifier.align(Alignment.Start)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Radar Spider Canvas
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .padding(8.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width / 2) * 0.8f

                    val angles = listOf(-90f, 30f, 150f)
                    val labels = listOf("Karma Yoga", "Bhakti Yoga", "Jnana Yoga")

                    // Draw Concentric Rings
                    val steps = listOf(0.25f, 0.5f, 0.75f, 1.0f)
                    steps.forEach { step ->
                        ringPath.reset()
                        for (i in angles.indices) {
                            val angleRad = Math.toRadians(angles[i].toDouble())
                            val x = center.x + radius * step * cos(angleRad).toFloat()
                            val y = center.y + radius * step * sin(angleRad).toFloat()
                            if (i == 0) {
                                ringPath.moveTo(x, y)
                            } else {
                                ringPath.lineTo(x, y)
                            }
                        }
                        ringPath.close()
                        drawPath(
                            path = ringPath,
                            color = onSurface.copy(alpha = 0.05f),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }

                    // Draw Axes lines
                    angles.forEach { angle ->
                        val angleRad = Math.toRadians(angle.toDouble())
                        val endX = center.x + radius * cos(angleRad).toFloat()
                        val endY = center.y + radius * sin(angleRad).toFloat()
                        drawLine(
                            color = onSurface.copy(alpha = 0.1f),
                            start = center,
                            end = Offset(endX, endY),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Draw Progress Area (Animated)
                    progressPath.reset()
                    val progresses = listOf(
                        (karmaProgress * animationProgress).coerceAtLeast(0.05f),
                        (bhaktiProgress * animationProgress).coerceAtLeast(0.05f),
                        (jnanaProgress * animationProgress).coerceAtLeast(0.05f)
                    )
                    for (i in angles.indices) {
                        val angleRad = Math.toRadians(angles[i].toDouble())
                        val p = progresses[i]
                        val x = center.x + radius * p * cos(angleRad).toFloat()
                        val y = center.y + radius * p * sin(angleRad).toFloat()
                        if (i == 0) {
                            progressPath.moveTo(x, y)
                        } else {
                            progressPath.lineTo(x, y)
                        }
                    }
                    progressPath.close()

                    // Fill progress area
                    drawPath(
                        path = progressPath,
                        brush = Brush.radialGradient(
                            colors = listOf(saffronGold.copy(alpha = 0.4f), terracotta.copy(alpha = 0.2f)),
                            center = center,
                            radius = radius
                        )
                    )

                    // Stroke progress area
                    drawPath(
                        path = progressPath,
                        color = terracotta,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    // Draw corners points
                    for (i in angles.indices) {
                        val angleRad = Math.toRadians(angles[i].toDouble())
                        val p = progresses[i]
                        val x = center.x + radius * p * cos(angleRad).toFloat()
                        val y = center.y + radius * p * sin(angleRad).toFloat()
                        drawCircle(
                            color = saffronGold,
                            radius = 4.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }

                    // Draw labels
                    val labelPaint = android.graphics.Paint().apply {
                        color = onSurface.toArgb()
                        textSize = 9.sp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }

                    for (i in angles.indices) {
                        val angleRad = Math.toRadians(angles[i].toDouble())
                        val x = center.x + (radius + 15.dp.toPx()) * cos(angleRad).toFloat()
                        val y = center.y + (radius + 15.dp.toPx()) * sin(angleRad).toFloat()

                        val label = labels[i]
                        val textWidth = labelPaint.measureText(label)
                        
                        val alignX = x - textWidth / 2f
                        val alignY = when {
                            sin(angleRad) > 0.1 -> y + 4.dp.toPx()
                            sin(angleRad) < -0.1 -> y - 4.dp.toPx()
                            else -> y
                        }

                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            alignX,
                            alignY,
                            labelPaint
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details/Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Action", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$karmaCount verses", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Devotion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$bhaktiCount verses", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Knowledge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$jnanaCount verses", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
