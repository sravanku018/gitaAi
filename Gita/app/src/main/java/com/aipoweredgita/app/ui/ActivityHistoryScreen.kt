package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.ReadVerse
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ui.components.LotusLevelManager
import com.aipoweredgita.app.viewmodel.ActivityHistoryViewModel
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ─── MAIN SCREEN ─────────────────────────────────────────────────────────────
@Composable
fun ActivityHistoryScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    viewModel: ActivityHistoryViewModel = viewModel()
) {
    val userStats by viewModel.userStats.collectAsState()
    val allActivity by viewModel.allActivity.collectAsState()
    val attempts by viewModel.attempts.collectAsState()
    val averageAccuracy by viewModel.averageAccuracy.collectAsState()
    val averageTime by viewModel.averageTime.collectAsState()
    val quiz10Stats by viewModel.quiz10Stats.collectAsState()
    val quiz20Stats by viewModel.quiz20Stats.collectAsState()
    val quiz30Stats by viewModel.quiz30Stats.collectAsState()
    val selectedQuizSize by viewModel.selectedQuizSize.collectAsState()
    val karmaCount by viewModel.karmaYogaCount.collectAsState()
    val bhaktiCount by viewModel.bhaktiYogaCount.collectAsState()
    val jnanaCount by viewModel.jnanaYogaCount.collectAsState()

    var selectedTab by remember { mutableIntStateOf(initialTab) }
    val uiCfg = LocalUiConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
    ) {
        // Header
        Text(
            text = "Activity History",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Your complete learning journey",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 0.dp
        ) {
            listOf("Overview", "Quiz", "Calendar", "Tips").forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tab Content
        when (selectedTab) {
            0 -> OverviewTab(userStats = userStats)
            1 -> QuizTab(
                attempts = attempts,
                averageAccuracy = averageAccuracy,
                averageTime = averageTime,
                quiz10Stats = quiz10Stats,
                quiz20Stats = quiz20Stats,
                quiz30Stats = quiz30Stats,
                selectedQuizSize = selectedQuizSize,
                onSelectQuizSize = { viewModel.selectQuizSize(it) },
                userStats = userStats,
                karmaCount = karmaCount,
                bhaktiCount = bhaktiCount,
                jnanaCount = jnanaCount
            )
            2 -> CalendarTab()
            3 -> AHTipsTab(averageAccuracy = averageAccuracy)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 1: OVERVIEW
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun OverviewTab(userStats: UserStats?) {
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
            shape = RoundedCornerShape(16.dp),
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
                                text = formatTimeCompact(totalTime),
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
                value = "${userStats?.totalQuizzesTaken ?: 0}",
                label = "Quizzes Taken",
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
        }

        // ── Yoga Level Card ──
        val yogaInfo = LotusLevelManager.yogaLevelInfo(userStats)
        val yogaLevel = LotusLevelManager.levelFor(userStats)
        val progress = LotusLevelManager.progressInLevel(userStats)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = yogaInfo.emoji, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = yogaInfo.yogaName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Level $yogaLevel / 5",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = yogaInfo.yogaDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
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
private fun TimeDistributionLegendItem(label: String, seconds: Long, color: Color) {
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
                text = formatTimeCompact(seconds),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun OverviewStatCard(
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

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 2: QUIZ
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun QuizTab(
    attempts: List<QuizAttempt>,
    averageAccuracy: Float,
    averageTime: Long,
    quiz10Stats: com.aipoweredgita.app.viewmodel.QuizSizeStatsData?,
    quiz20Stats: com.aipoweredgita.app.viewmodel.QuizSizeStatsData?,
    quiz30Stats: com.aipoweredgita.app.viewmodel.QuizSizeStatsData?,
    selectedQuizSize: Int?,
    onSelectQuizSize: (Int?) -> Unit,
    userStats: UserStats?,
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int
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
        if (quiz20Stats != null) {
            FilterChip(
                selected = selectedQuizSize == 20,
                onClick = { onSelectQuizSize(20) },
                label = { Text("20Q (${quiz20Stats.totalAttempts})") }
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
        20 -> quiz20Stats
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
                    title = "Total Attempts",
                    value = "${displayAttempts.size}",
                    icon = "📝",
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f)
                )
                AHPerformanceCard(
                    title = "Accuracy",
                    value = "${userStats?.accuracyPercentage?.toInt() ?: 0}%",
                    icon = "🎓",
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }

            // Performance Trend Chart
            AHPerformanceTrendChart(attempts = displayAttempts)

            // Spiritual Path Radar
            AHSpiritualPathRadarChart(
                karmaCount = karmaCount,
                bhaktiCount = bhaktiCount,
                jnanaCount = jnanaCount
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
private fun AHPerformanceCard(
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
private fun AHQuizAttemptCard(attempt: QuizAttempt) {
    val statusColor = when {
        attempt.accuracyPercentage >= 90 -> MaterialTheme.colorScheme.primary
        attempt.accuracyPercentage >= 75 -> MaterialTheme.colorScheme.secondary
        attempt.accuracyPercentage >= 60 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val bgColor = statusColor.copy(alpha = 0.1f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.dateFormatted,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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

// ── Performance Trend Line Chart ──
@Composable
private fun AHPerformanceTrendChart(attempts: List<QuizAttempt>) {
    val last10 = remember(attempts) { attempts.take(10).reversed() }
    if (last10.isEmpty()) return

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
        label = "ah_line_chart_anim"
    )
    LaunchedEffect(last10) {
        animationTriggered = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Performance Trend (Last 10)",
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
                        .pointerInput(last10, animationProgress) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val height = size.height
                                val paddingLeft = with(density) { 40.dp.toPx() }
                                val paddingRight = with(density) { 20.dp.toPx() }
                                val paddingTop = with(density) { 20.dp.toPx() }
                                val paddingBottom = with(density) { 40.dp.toPx() }
                                val chartWidth = width - paddingLeft - paddingRight
                                val chartHeight = height - paddingTop - paddingBottom

                                val pointsCount = last10.size
                                val stepX = if (pointsCount > 1) chartWidth / (pointsCount - 1) else chartWidth

                                var closestIndex = -1
                                var minDistance = Float.MAX_VALUE

                                for (i in last10.indices) {
                                    val attempt = last10[i]
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
                    val paddingLeft = 40.dp.toPx()
                    val paddingRight = 20.dp.toPx()
                    val paddingTop = 20.dp.toPx()
                    val paddingBottom = 40.dp.toPx()
                    val chartWidth = size.width - paddingLeft - paddingRight
                    val chartHeight = size.height - paddingTop - paddingBottom

                    // Grid lines
                    val paint = android.graphics.Paint().apply {
                        color = onSurface.copy(alpha = 0.6f).toArgb()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                    listOf(0f, 0.25f, 0.5f, 0.75f, 1f).forEach { pct ->
                        val y = paddingTop + chartHeight - pct * chartHeight
                        drawLine(
                            color = onSurface.copy(alpha = 0.1f),
                            start = Offset(paddingLeft, y),
                            end = Offset(size.width - paddingRight, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(pct * 100).toInt()}%",
                            paddingLeft - 8.dp.toPx(),
                            y + 4.dp.toPx(),
                            paint
                        )
                    }

                    val count = last10.size
                    val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth

                    cachedLinePath.reset()
                    cachedFillPath.reset()

                    for (i in last10.indices) {
                        val x = paddingLeft + i * stepX
                        val y = paddingTop + chartHeight - (last10[i].accuracyPercentage / 100f) * chartHeight * animationProgress
                        if (i == 0) {
                            cachedLinePath.moveTo(x, y)
                            cachedFillPath.moveTo(x, paddingTop + chartHeight)
                            cachedFillPath.lineTo(x, y)
                        } else {
                            val prevX = paddingLeft + (i - 1) * stepX
                            val prevY = paddingTop + chartHeight - (last10[i - 1].accuracyPercentage / 100f) * chartHeight * animationProgress

                            val cp1x = prevX + (x - prevX) / 2f
                            val cp1y = prevY
                            val cp2x = prevX + (x - prevX) / 2f
                            val cp2y = y

                            cachedLinePath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                            cachedFillPath.cubicTo(cp1x, cp1y, cp2x, cp2y, x, y)
                        }
                        if (i == last10.lastIndex) {
                            cachedFillPath.lineTo(x, paddingTop + chartHeight)
                            cachedFillPath.close()
                        }
                    }

                    drawPath(
                        path = cachedFillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(saffronGold.copy(alpha = 0.3f), Color.Transparent),
                            startY = paddingTop,
                            endY = paddingTop + chartHeight
                        )
                    )
                    drawPath(
                        path = cachedLinePath,
                        color = saffronGold,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    for (i in last10.indices) {
                        val x = paddingLeft + i * stepX
                        val y = paddingTop + chartHeight - (last10[i].accuracyPercentage / 100f) * chartHeight * animationProgress
                        drawCircle(color = terracotta, radius = 5.dp.toPx(), center = Offset(x, y))
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
                    }

                    val labelPaint = android.graphics.Paint().apply {
                        color = onSurface.copy(alpha = 0.6f).toArgb()
                        textSize = 8.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                    for (i in last10.indices) {
                        drawContext.canvas.nativeCanvas.drawText(
                            "Q${i + 1}",
                            paddingLeft + i * stepX,
                            paddingTop + chartHeight + 16.dp.toPx(),
                            labelPaint
                        )
                    }
                }

                selectedPointIndex?.let { index ->
                    val attempt = last10[index]
                    AHTooltip(
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
private fun AHTooltip(
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

// ── Spiritual Path Radar Chart ──
@Composable
private fun AHSpiritualPathRadarChart(
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int
) {
    val saffronGold = Color(0xFFE08A1E)
    val terracotta = Color(0xFFC2410C)
    val onSurface = MaterialTheme.colorScheme.onSurface

    val targetMax = 50f
    val kp = (karmaCount / targetMax).coerceAtMost(1f)
    val bp = (bhaktiCount / targetMax).coerceAtMost(1f)
    val jp = (jnanaCount / targetMax).coerceAtMost(1f)

    // Caching Path objects to eliminate allocations inside DrawScope
    val ringPath = remember { Path() }
    val progressPath = remember { Path() }

    // Chart entry animation
    var animationTriggered by remember { mutableStateOf(false) }
    val animationProgress by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "ah_radar_chart_anim"
    )
    LaunchedEffect(karmaCount, bhaktiCount, jnanaCount) {
        animationTriggered = true
    }

    val archetype = remember(karmaCount, bhaktiCount, jnanaCount) {
        when {
            karmaCount == 0 && bhaktiCount == 0 && jnanaCount == 0 -> "Aspirant"
            kotlin.math.abs(karmaCount - bhaktiCount) <= 5 && kotlin.math.abs(bhaktiCount - jnanaCount) <= 5 -> "Raja Yogi"
            karmaCount > bhaktiCount && karmaCount > jnanaCount -> "Karma Yogi"
            bhaktiCount > karmaCount && bhaktiCount > jnanaCount -> "Bhakti Yogi"
            jnanaCount > karmaCount && jnanaCount > bhaktiCount -> "Jnana Yogi"
            else -> "Aspirant"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Spiritual Archetype",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.Start)
            )
            Text(
                text = archetype,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = terracotta,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.size(200.dp).padding(8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = (size.width / 2) * 0.8f
                    val angles = listOf(-90f, 30f, 150f)
                    val progresses = listOf(
                        (kp * animationProgress).coerceAtLeast(0.05f),
                        (bp * animationProgress).coerceAtLeast(0.05f),
                        (jp * animationProgress).coerceAtLeast(0.05f)
                    )

                    // Concentric rings
                    listOf(0.25f, 0.5f, 0.75f, 1f).forEach { step ->
                        ringPath.reset()
                        for (i in angles.indices) {
                            val rad = Math.toRadians(angles[i].toDouble())
                            val x = center.x + radius * step * cos(rad).toFloat()
                            val y = center.y + radius * step * sin(rad).toFloat()
                            if (i == 0) ringPath.moveTo(x, y) else ringPath.lineTo(x, y)
                        }
                        ringPath.close()
                        drawPath(ringPath, onSurface.copy(alpha = 0.05f), style = Stroke(1.dp.toPx()))
                    }

                    // Axes
                    angles.forEach { angle ->
                        val rad = Math.toRadians(angle.toDouble())
                        drawLine(
                            onSurface.copy(alpha = 0.1f),
                            center,
                            Offset(center.x + radius * cos(rad).toFloat(), center.y + radius * sin(rad).toFloat()),
                            1.dp.toPx()
                        )
                    }

                    // Progress area
                    progressPath.reset()
                    for (i in angles.indices) {
                        val rad = Math.toRadians(angles[i].toDouble())
                        val x = center.x + radius * progresses[i] * cos(rad).toFloat()
                        val y = center.y + radius * progresses[i] * sin(rad).toFloat()
                        if (i == 0) progressPath.moveTo(x, y) else progressPath.lineTo(x, y)
                    }
                    progressPath.close()
                    drawPath(progressPath, Brush.radialGradient(
                        listOf(saffronGold.copy(alpha = 0.4f), terracotta.copy(alpha = 0.2f)),
                        center, radius
                    ))
                    drawPath(progressPath, terracotta, style = Stroke(2.dp.toPx()))

                    // Points
                    for (i in angles.indices) {
                        val rad = Math.toRadians(angles[i].toDouble())
                        val x = center.x + radius * progresses[i] * cos(rad).toFloat()
                        val y = center.y + radius * progresses[i] * sin(rad).toFloat()
                        drawCircle(saffronGold, 4.dp.toPx(), Offset(x, y))
                    }

                    // Labels
                    val lp = android.graphics.Paint().apply {
                        color = onSurface.toArgb()
                        textSize = 9.sp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val labels = listOf("Karma", "Bhakti", "Jnana")
                    for (i in angles.indices) {
                        val rad = Math.toRadians(angles[i].toDouble())
                        val x = center.x + (radius + 15.dp.toPx()) * cos(rad).toFloat()
                        val y = center.y + (radius + 15.dp.toPx()) * sin(rad).toFloat()
                        val tw = lp.measureText(labels[i])
                        drawContext.canvas.nativeCanvas.drawText(labels[i], x - tw / 2f, y + 4.dp.toPx(), lp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Action", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$karmaCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Devotion", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$bhaktiCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Knowledge", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$jnanaCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 3: CALENDAR
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarTab() {
    val context = LocalContext.current
    val db = remember { GitaDatabase.getDatabase(context) }

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    val dates = remember(currentMonth) { generateDatesForMonth(currentMonth) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var dailyRow by remember { mutableStateOf<DailyActivity?>(null) }
    var versesList by remember { mutableStateOf<List<ReadVerse>>(emptyList()) }
    var activityByDate by remember { mutableStateOf<Map<String, DailyActivity?>>(emptyMap()) }
    var allZero by remember { mutableStateOf(true) }
    var selectedMode by remember { mutableStateOf("All") }

    LaunchedEffect(dates) {
        val map = mutableMapOf<String, DailyActivity?>()
        var anyActivity = false
        for (d in dates) {
            val row = try { db.dailyActivityDao().getByDate(d) } catch (_: Exception) { null }
            map[d] = row
            val total = (row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L)
            if (total > 0L) anyActivity = true
        }
        activityByDate = map
        allZero = !anyActivity
    }

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            dailyRow = try { db.dailyActivityDao().getByDate(date) } catch (_: Exception) { null }
            versesList = try { db.readVerseDao().getByDate(date) } catch (_: Exception) { emptyList() }
        }
    }

    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Mode filter
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("All", "Reading", "Quiz", "Chat").forEach { mode ->
                FilterChip(
                    selected = selectedMode == mode,
                    onClick = { selectedMode = mode },
                    label = { Text(mode) }
                )
            }
        }

        if (allZero) {
            Text(
                text = "No activity tracked this month yet.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Heat map card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Month picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { currentMonth = currentMonth.minusMonths(1) }) {
                        Text("<", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = currentMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { currentMonth = currentMonth.plusMonths(1) }) {
                        Text(">", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Weekday labels
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                        Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Calendar grid
                val maxForMode = remember(selectedMode, activityByDate) {
                    var local = 1L
                    activityByDate.values.forEach { row ->
                        val v = when (selectedMode) {
                            "Reading" -> (row?.normalSeconds ?: 0L)
                            "Quiz" -> (row?.quizSeconds ?: 0L)
                            "Chat" -> (row?.voiceStudioTimeSeconds ?: 0L)
                            else -> ((row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L))
                        }
                        if (v > local) local = v
                    }
                    local.coerceAtLeast(1L)
                }

                val monthStartDay = currentMonth.atDay(1).dayOfWeek.value
                val emptySlots = monthStartDay - 1
                val totalSlots = dates.size + emptySlots
                val rows = (totalSlots + 6) / 7
                val cellSize = 40.dp
                val spacing = 4.dp
                val gridHeight = (cellSize * rows) + (spacing * (rows - 1))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    userScrollEnabled = false,
                    modifier = Modifier.height(gridHeight + 20.dp)
                ) {
                    items(emptySlots) {
                        Box(modifier = Modifier.size(cellSize))
                    }
                    items(dates.size) { index ->
                        val date = dates[index]
                        val row = activityByDate[date]
                        val total = when (selectedMode) {
                            "Reading" -> (row?.normalSeconds ?: 0L)
                            "Quiz" -> (row?.quizSeconds ?: 0L)
                            "Chat" -> (row?.voiceStudioTimeSeconds ?: 0L)
                            else -> (row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L)
                        }
                        val intensity = (total.toFloat() / maxForMode.toFloat()).coerceIn(0f, 1f)
                        val base = MaterialTheme.colorScheme.primary
                        val bg = if (total > 0) base.copy(alpha = 0.20f + 0.80f * intensity) else MaterialTheme.colorScheme.surface
                        val borderColor = if (total > 0) null else MaterialTheme.colorScheme.outlineVariant

                        Box(
                            modifier = Modifier
                                .size(cellSize)
                                .background(color = bg, shape = RoundedCornerShape(4.dp))
                                .then(
                                    if (borderColor != null) Modifier.border(1.dp, borderColor, RoundedCornerShape(4.dp)) else Modifier
                                )
                                .clickable { selectedDate = date },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = formatDay(date),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (total > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Legend
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val base = MaterialTheme.colorScheme.primary
                    listOf(0.30f, 0.45f, 0.60f, 0.80f).forEach { a ->
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(base.copy(alpha = a), MaterialTheme.shapes.small)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        )
                    }
                    Text("More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Day detail dialog
    selectedDate?.let { date ->
        AlertDialog(
            onDismissRequest = { selectedDate = null },
            confirmButton = { TextButton(onClick = { selectedDate = null }) { Text("Close") } },
            title = { Text("Activity on ${formatDisplayDate(date)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val normal = dailyRow?.normalSeconds ?: 0L
                    val quiz = dailyRow?.quizSeconds ?: 0L
                    val studio = dailyRow?.voiceStudioTimeSeconds ?: 0L
                    Text("📖 Reading: ${formatTimeCompact(normal)}")
                    Text("📝 Quiz: ${formatTimeCompact(quiz)}")
                    Text("💬 Chat: ${formatTimeCompact(studio)}")
                    HorizontalDivider()
                    Text("Verses viewed: ${versesList.size}", fontWeight = FontWeight.Bold)
                    if (versesList.isNotEmpty()) {
                        val items = versesList.take(12).joinToString(", ") { "${it.chapterNo}:${it.verseNo}" }
                        Text(text = items, style = MaterialTheme.typography.bodyMedium)
                        if (versesList.size > 12) {
                            Text(
                                text = "… and ${versesList.size - 12} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 4: TIPS
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AHTipsTab(averageAccuracy: Float) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            AHTipCard("📖", "Study Regularly", "Read verses daily to improve retention. Consistency is key to understanding the Bhagavad Gita's teachings.", MaterialTheme.colorScheme.primary)
        }
        if (averageAccuracy < 60) {
            item {
                AHTipCard("💡", "Focus on Understanding", "Don't just memorize! Try to understand the meaning and context of each verse. Use Normal Mode to read explanations before taking quizzes.", MaterialTheme.colorScheme.error)
            }
        }
        if (averageAccuracy >= 60 && averageAccuracy < 80) {
            item {
                AHTipCard("🎯", "Practice More", "You're doing well! Keep practicing with different question types to improve your accuracy further.", MaterialTheme.colorScheme.secondary)
            }
        }
        if (averageAccuracy >= 80) {
            item {
                AHTipCard("🌟", "Excellent Work!", "You have a great understanding! Consider helping others learn and sharing your knowledge of the Gita.", MaterialTheme.colorScheme.primary)
            }
        }
        item {
            AHTipCard("🧘", "Reflect on Teachings", "After each quiz, spend a moment reflecting on how the teachings apply to your life.", MaterialTheme.colorScheme.tertiary)
        }
        item {
            AHTipCard("🔄", "Review Mistakes", "Go back to verses you got wrong in quizzes. Understanding your mistakes is the fastest way to improve.", MaterialTheme.colorScheme.secondary)
        }
        item {
            AHTipCard("⏰", "Set a Goal", "Try to improve your accuracy by 5% each week. Small, consistent improvements lead to mastery!", MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
private fun AHTipCard(icon: String, title: String, tip: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Text(text = icon, fontSize = 36.sp, modifier = Modifier.padding(end = 14.dp))
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = tip, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
private fun AHEmptyState(message: String) {
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

// ─── Utility Functions ───────────────────────────────────────────────────────
private fun formatTimeCompact(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

private fun formatDisplayDate(dateStr: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateStr) ?: return dateStr
        outputFormat.format(date)
    } catch (_: Exception) { dateStr }
}

private fun formatDay(dateStr: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("d", Locale.getDefault())
        val date = inputFormat.parse(dateStr) ?: return dateStr
        outputFormat.format(date)
    } catch (_: Exception) { dateStr }
}

private fun generateDatesForMonth(month: YearMonth): List<String> {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    return (1..month.lengthOfMonth()).map { day -> month.atDay(day).format(formatter) }
}
