package com.aipoweredgita.app.ui.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.QuizAttempt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

// ─── Donut Chart ──────────────────────────────────────────────────────────────
// Extracted from ActivityHistoryScreen.OverviewTab (lines ~174-320)
@Composable
fun TimeDonutChart(
    normalTime: Long,
    quizTime: Long,
    voiceTime: Long,
    modifier: Modifier = Modifier
) {
    val totalTime = normalTime + quizTime + voiceTime
    val readingColor = Color(0xFFE08A1E)
    val quizColor = Color(0xFFC2410C)
    val chatColor = Color(0xFFF59E0B)

    val items = if (totalTime == 0L) {
        listOf(Triple("Empty", 1f, Color.LightGray))
    } else {
        listOf(
            Triple("Reading", normalTime.toFloat() / totalTime, readingColor),
            Triple("Quiz", quizTime.toFloat() / totalTime, quizColor),
            Triple("Chat", voiceTime.toFloat() / totalTime, chatColor)
        )
    }

    Canvas(modifier = modifier) {
        val strokeWidth = 18.dp.toPx()
        var startAngle = -90f
        items.forEach { (_, ratio, color) ->
            val sweep = ratio * 360f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                size = Size(size.width - strokeWidth, size.height - strokeWidth),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweep
        }
    }
}

// ─── Tooltip ───────────────────────────────────────────────────────────────────
@Composable
fun ChartTooltip(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// ─── Performance Trend Line Chart ─────────────────────────────────────────────
@Composable
fun PerformanceTrendLineChart(
    attempts: List<QuizAttempt>,
    modifier: Modifier = Modifier
) {
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
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp),
        shape = MaterialTheme.shapes.large,
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
                val density = LocalDensity.current
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
    val density = LocalDensity.current
    val xDp = with(density) { offset.x.toDp() }
    val yDp = with(density) { offset.y.toDp() }

    Box(
        modifier = modifier
            .offset(x = xDp - 50.dp, y = yDp - 65.dp)
            .background(
                color = MaterialTheme.colorScheme.inverseSurface,
                shape = MaterialTheme.shapes.small
            )
            .border(1.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
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

// ─── Spiritual Path Radar Chart ───────────────────────────────────────────────
@Composable
fun SpiritualPathRadarChart(
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int,
    dhyanaCount: Int,
    rajaCount: Int,
    modifier: Modifier = Modifier
) {
    val saffronGold = Color(0xFFE08A1E)
    val terracotta = Color(0xFFC2410C)
    val onSurface = MaterialTheme.colorScheme.onSurface

    val targetMax = 50f
    val kp = (karmaCount / targetMax).coerceAtMost(1f)
    val bp = (bhaktiCount / targetMax).coerceAtMost(1f)
    val jp = (jnanaCount / targetMax).coerceAtMost(1f)
    val dp = (dhyanaCount / targetMax).coerceAtMost(1f)
    val rp = (rajaCount / targetMax).coerceAtMost(1f)

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

    val archetype = remember(karmaCount, bhaktiCount, jnanaCount, dhyanaCount, rajaCount) {
        val counts = listOf(karmaCount, bhaktiCount, jnanaCount, dhyanaCount, rajaCount)
        val maxCount = counts.maxOrNull() ?: 0
        when {
            maxCount == 0 -> "Aspirant"
            counts.count { it >= maxCount - 5 } >= 3 -> "Balanced Yogi"
            maxCount == karmaCount -> "Karma Yogi"
            maxCount == bhaktiCount -> "Bhakti Yogi"
            maxCount == jnanaCount -> "Jnana Yogi"
            maxCount == dhyanaCount -> "Dhyana Yogi"
            maxCount == rajaCount -> "Raja Yogi"
            else -> "Aspirant"
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
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
                    val angles = listOf(-90f, -18f, 54f, 126f, 198f)
                    val progresses = listOf(
                        (kp * animationProgress).coerceAtLeast(0.05f),
                        (bp * animationProgress).coerceAtLeast(0.05f),
                        (jp * animationProgress).coerceAtLeast(0.05f),
                        (dp * animationProgress).coerceAtLeast(0.05f),
                        (rp * animationProgress).coerceAtLeast(0.05f)
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
                    val labels = listOf("Karma", "Bhakti", "Jnana", "Dhyana", "Raja")
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
                    Text("Act", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$karmaCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Dev", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$bhaktiCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Kno", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$jnanaCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Med", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$dhyanaCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Mys", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("$rajaCount", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

