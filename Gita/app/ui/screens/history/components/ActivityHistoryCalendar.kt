package com.aipoweredgita.app.ui.screens.history.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.ReadVerse
import com.aipoweredgita.app.util.TimeUtils
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarTab() {
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
        val rows = try { db.dailyActivityDao().getByDates(dates) } catch (_: Exception) { emptyList() }
        val map = rows.associateBy { it.date }
        activityByDate = dates.associateWith { d -> map[d] }
        allZero = rows.all { row ->
            ((row.normalSeconds ?: 0L) + (row.quizSeconds ?: 0L) + (row.voiceStudioTimeSeconds ?: 0L)) == 0L
        }
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
                                .background(color = bg, shape = MaterialTheme.shapes.extraSmall)
                                .then(
                                    if (borderColor != null) Modifier.border(1.dp, borderColor, MaterialTheme.shapes.extraSmall) else Modifier
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
                    Text("📖 Reading: ${TimeUtils.formatTimeCompact(normal)}")
                    Text("📝 Quiz: ${TimeUtils.formatTimeCompact(quiz)}")
                    Text("💬 Chat: ${TimeUtils.formatTimeCompact(studio)}")
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

// ─── Utility Functions ───────────────────────────────────────────────────────
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
