package com.aipoweredgita.app.ui

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.ReadVerse
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

import com.aipoweredgita.app.viewmodel.ProfileViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.ui.components.LotusLevelManager
import com.aipoweredgita.app.ui.components.LotusBadge
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToProgression: () -> Unit = {}
) {
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

    // Calendar (official Material DatePicker)
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis(),
        yearRange = 1900..2100
    )

    // Profile Stats
    val stats by viewModel.stats.collectAsState()
    val yogaLevel = LotusLevelManager.levelFor(stats)
    val yogaStep = LotusLevelManager.stepFor(stats)
    val yogaInfo = LotusLevelManager.yogaLevelInfo(stats)
    val breakdown = LotusLevelManager.compositeBreakdown(stats)

    LaunchedEffect(dates) {
        val map = mutableMapOf<String, DailyActivity?>()
        var anyActivity = false
        for (d in dates) {
            val row = try { db.dailyActivityDao().getByDate(d) } catch (e: Exception) {
                android.util.Log.w("DailyActivityScreen", "Failed to load activity for $d", e)
                null
            }
            map[d] = row
            val total = (row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L)
            if (total > 0L) anyActivity = true
        }
        activityByDate = map
        allZero = !anyActivity
    }

    LaunchedEffect(selectedDate) {
        selectedDate?.let { date ->
            dailyRow = try { db.dailyActivityDao().getByDate(date) } catch (e: Exception) {
                android.util.Log.w("DailyActivityScreen", "Failed to load daily row for $date", e)
                null
            }
            versesList = try { db.readVerseDao().getByDate(date) } catch (e: Exception) {
                android.util.Log.w("DailyActivityScreen", "Failed to load verses for $date", e)
                emptyList()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(text = "Activity", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(text = "Track your daily spiritual practice", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToProgression),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🕉️",
                            fontSize = 40.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stats?.userName?.ifEmpty { "Gita Student" } ?: "Gita Student",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = yogaInfo.emoji,
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Active for ${stats?.daysActive ?: 0} days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                }
            }
        }

        // Level breakdown card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onNavigateToProgression),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Your Yoga Progression", style = MaterialTheme.typography.titleMedium)
                Text(text = "${yogaInfo.emoji} ${yogaInfo.yogaName} - Level $yogaLevel / 5", fontWeight = FontWeight.Bold)
                Text(text = yogaInfo.yogaDescription, style = MaterialTheme.typography.bodySmall)
                Text(text = "Step: $yogaStep / 19", style = MaterialTheme.typography.bodyMedium)
                
                // Simplified breakdown for activity screen
                LinearProgressIndicator(
                    progress = { LotusLevelManager.progressInLevel(stats) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                )
                Text(text = "Progress to next level: ${((LotusLevelManager.progressInLevel(stats)) * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }
        }

        if (allZero) {
            Text(text = "No activity tracked in the last 35 days.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

    // ViewModel-driven UI config
    val uiCfg = LocalUiConfig.current

    // Mode filter
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf("All", "Normal", "Quiz", "Studio").forEach { mode ->
                FilterChip(selected = selectedMode == mode, onClick = { selectedMode = mode }, label = { Text(mode) })
            }
        }

        // Heat map (per-mode scaling)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Month Picker Header
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

                WeekdayLabels()
                val maxForMode = remember(selectedMode, activityByDate) {
                    var local = 1L
                    activityByDate.values.forEach { row ->
                        val v = when (selectedMode) {
                            "Normal" -> (row?.normalSeconds ?: 0L)
                            "Quiz" -> (row?.quizSeconds ?: 0L)
                            "Studio" -> (row?.voiceStudioTimeSeconds ?: 0L)
                            else -> ((row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L))
                        }
                        if (v > local) local = v
                    }
                    local.coerceAtLeast(1L)
                }
                
                SquareDateCalendar(
                    dates = dates,
                    data = activityByDate,
                    maxSeconds = maxForMode,
                    onSelect = { selectedDate = it },
                    mode = selectedMode,
                    monthStartDay = currentMonth.atDay(1).dayOfWeek.value // 1=Mon, 7=Sun
                )
                HeatMapLegend()
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val date: Date = Date(millis)
                        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                        selectedDate = formatter.format(date)
                    }
                    showDatePicker = false
                }) { Text("Done") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                    Text("Normal Mode: ${formatTime(normal)}")
                    Text("Quiz Mode: ${formatTime(quiz)}")
                    Text("Voice Studio: ${formatTime(studio)}")
                    HorizontalDivider()
                    Text("Verses viewed: ${versesList.size}", fontWeight = FontWeight.Bold)
                    if (versesList.isNotEmpty()) {
                        val items = versesList.take(12).joinToString(", ") { "${it.chapterNo}:${it.verseNo}" }
                        Text(
                            text = items,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
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

// Backward-compatible name
@Composable
fun DailyActivityScreen(
    modifier: Modifier = Modifier,
    onNavigateToProgression: () -> Unit = {}
) = ActivityScreen(modifier, onNavigateToProgression = onNavigateToProgression)

@Composable
private fun WeekdayLabels() {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        labels.forEach { d ->
            Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                Text(text = d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HeatMapLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Less", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val base = MaterialTheme.colorScheme.primary
        val colors = listOf(0.30f, 0.45f, 0.60f, 0.80f).map { a -> base.copy(alpha = a) }
        colors.forEach { c ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(color = c, shape = MaterialTheme.shapes.small)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), MaterialTheme.shapes.small)
            )
        }
        Text(text = "More", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HeatMapGrid(
    dates: List<String>,
    data: Map<String, DailyActivity?>,
    maxSeconds: Long,
    onSelect: (String) -> Unit,
    mode: String = "All",
    modifier: Modifier = Modifier
) {
    val uiCfg = LocalUiConfig.current
    val columns = uiCfg.gridColumns
    val cellSize = if (uiCfg.isLandscape) 36.dp else 44.dp
    val spacing = 8.dp
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        modifier = modifier
    ) {
        items(dates.size) { index ->
            val date = dates[index]
            val row = data[date]
            val total = when (mode) {
                "Normal" -> (row?.normalSeconds ?: 0L)
                "Quiz" -> (row?.quizSeconds ?: 0L)
                "Studio" -> (row?.voiceStudioTimeSeconds ?: 0L)
                else -> (row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L)
            }
            val intensity = (total.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
            val base = MaterialTheme.colorScheme.primary
            val bg = base.copy(alpha = 0.30f + 0.50f * intensity)
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .background(color = bg, shape = androidx.compose.foundation.shape.CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), androidx.compose.foundation.shape.CircleShape)
                    .clickable { onSelect(date) },
                contentAlignment = Alignment.Center
            ) {
                Text(text = formatDay(date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}

@Composable
private fun SquareDateCalendar(
    dates: List<String>,
    data: Map<String, DailyActivity?>,
    maxSeconds: Long,
    onSelect: (String) -> Unit,
    mode: String = "All",
    monthStartDay: Int, // 1=Mon, ..., 7=Sun
    modifier: Modifier = Modifier
) {
    val daysInWeek = 7
    // Calculate empty slots at start. Mon(1) -> 0 empty. Sun(7) -> 6 empty.
    val emptySlots = monthStartDay - 1
    
    val totalSlots = dates.size + emptySlots
    val uiCfg = LocalUiConfig.current
    val spacing = 4.dp
    
    // Auto-calculate height based on rows
    val rows = (totalSlots + daysInWeek - 1) / daysInWeek
    val cellSize = 40.dp // Fixed size for squares
    val gridHeight = (cellSize * rows) + (spacing * (rows - 1))

    LazyVerticalGrid(
        columns = GridCells.Fixed(daysInWeek),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        userScrollEnabled = false,
        modifier = modifier.height(gridHeight + 20.dp) // Little buffer
    ) {
        // Empty slots for alignment
        items(emptySlots) {
             Box(modifier = Modifier.size(cellSize))
        }

        items(dates.size) { index ->
            val date = dates[index]
            val row = data[date]
            val total = when (mode) {
                "Normal" -> (row?.normalSeconds ?: 0L)
                "Quiz" -> (row?.quizSeconds ?: 0L)
                "Studio" -> (row?.voiceStudioTimeSeconds ?: 0L)
                else -> (row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L)
            }
            val intensity = (total.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
            val base = MaterialTheme.colorScheme.primary
            // if 0 activity, use surface variant. if activity, use refined alpha
            val bg = if (total > 0) base.copy(alpha = 0.20f + 0.80f * intensity) else MaterialTheme.colorScheme.surface
            
            val border = if (total > 0) null else MaterialTheme.colorScheme.outlineVariant
            
            Box(
                modifier = Modifier
                    .size(cellSize)
                    .background(color = bg, shape = RoundedCornerShape(4.dp))
                    .then(
                         if (border != null) Modifier.border(1.dp, border, RoundedCornerShape(4.dp)) else Modifier
                    )
                    .clickable { onSelect(date) },
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
}

private fun generateDates(days: Int): List<String> {
    val calendar = Calendar.getInstance()
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return (0 until days).map { offset ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_MONTH, -offset)
        }
        formatter.format(cal.time)
    }
}

private fun generateDatesFromOffset(dayOffset: Int, days: Int): List<String> {
    val calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_MONTH, -dayOffset)
    }
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return (0 until days).map { offset ->
        val cal = Calendar.getInstance().apply {
            timeInMillis = calendar.timeInMillis
            add(Calendar.DAY_OF_MONTH, -offset)
        }
        formatter.format(cal.time)
    }
}

private fun generateDatesForMonth(month: YearMonth): List<String> {
    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val daysInMonth = month.lengthOfMonth()
    val list = mutableListOf<String>()
    for (day in 1..daysInMonth) {
        list.add(month.atDay(day).format(formatter))
    }
    return list
}

private fun formatTime(seconds: Long): String {
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

private fun formatShortDate(dateStr: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = java.text.SimpleDateFormat("dd/MM", Locale.getDefault())
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
