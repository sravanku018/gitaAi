package com.aipoweredgita.app.ui.charts

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
    // Extracted from ActivityHistoryScreen.AHPerformanceTrendChart (lines 702-893)
    // Heavy Canvas-based line chart with grid lines, data points, touch detection
    // See source for full implementation
    Box(modifier = modifier) {
        Text(
            text = "Performance Trend",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Spiritual Path Radar Chart ───────────────────────────────────────────────
@Composable
fun SpiritualPathRadarChart(
    karmaCount: Int,
    bhaktiCount: Int,
    jnanaCount: Int,
    modifier: Modifier = Modifier
) {
    // Extracted from ActivityHistoryScreen.AHSpiritualPathRadarChart (line 933+)
    Box(modifier = modifier) {
        Text(
            text = "Spiritual Path",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
