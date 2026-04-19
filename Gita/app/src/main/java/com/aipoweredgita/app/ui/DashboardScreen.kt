package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Chat

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.components.GradientActionCard
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import com.aipoweredgita.app.ui.components.LotusLevelManager
import com.aipoweredgita.app.ui.components.LotusBadge
import com.aipoweredgita.app.recommendation.RecommendationEngine
import com.aipoweredgita.app.recommendation.AdaptiveCurriculumPlanner
import com.aipoweredgita.app.recommendation.YogaAdvisor
import com.aipoweredgita.app.recommendation.predictNext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import android.content.Context
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.components.WelcomeDialog
import com.aipoweredgita.app.ui.components.MandalaBackground
import com.aipoweredgita.app.ui.components.PremiumDashboardCard
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay

@Composable
fun AnimatedItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L) // Staggered delay
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(500)) + 
                slideInVertically(
                    initialOffsetY = { 50 },
                    animationSpec = tween(500)
                )
    ) {
        content()
    }
}

@Composable
fun MandalaHeroSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                brush = Brush.horizontalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary
                    )
                )
            )
    ) {
        // Background Pattern
        MandalaBackground(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(300.dp)
                .offset(x = 100.dp),
            color = Color.White.copy(alpha = 0.15f)
        )
        
        MandalaBackground(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(150.dp)
                .offset(x = (-40).dp, y = (-40).dp),
            color = Color.White.copy(alpha = 0.1f)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "🙏 Namaste",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Continue your spiritual journey",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun DashboardScreen(
    onNavigateToNormalMode: () -> Unit,
    onNavigateToQuizMode: () -> Unit,
    onNavigateToVoiceStudio: () -> Unit = {},
    onNavigateToRecommendations: () -> Unit,
    onNavigateToRandomSloka: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
    var versesToday by remember { mutableStateOf(0) }
    var quizzesToday by remember { mutableStateOf(0) }
    var normalToday by remember { mutableStateOf(0L) }
    var quizToday by remember { mutableStateOf(0L) }
    var studioToday by remember { mutableStateOf(0L) }
    var versesListToday by remember { mutableStateOf<List<com.aipoweredgita.app.database.ReadVerse>>(emptyList()) }

    // Welcome Dialog State
    var showWelcomeDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val hasSeenWelcome = prefs.getBoolean("has_seen_welcome", false)
        if (!hasSeenWelcome) {
            showWelcomeDialog = true
        }
    }


    LaunchedEffect(Unit) {
        val today = java.time.LocalDate.now().toString()
        // Move DB work off the main thread
        try {
            val (vt, vlist) = withContext(Dispatchers.IO) {
                val vtCount = db.readVerseDao().totalReadToday(today)
                val vlistData = db.readVerseDao().getByDate(today)
                vtCount to vlistData
            }
            versesToday = vt
            versesListToday = vlist
        } catch (e: Exception) {
            android.util.Log.w("DashboardScreen", "Failed to load stats", e)
        }

        // M1-style next step predictor (reset daily)
        try {
            val lastSugDate = prefs.getString("next_suggestion_date", "")
            if (lastSugDate != today) {
                val suggestion = withContext(Dispatchers.IO) { predictNext(db) }
                prefs.edit()
                    .putString("next_step_label", suggestion.nextStep)
                    .putInt("next_level", suggestion.nextLevel)
                    .putString("next_reason", suggestion.reason)
                    .putString("next_suggestion_date", today)
                    .apply()
            }
        } catch (e: Exception) {
            android.util.Log.w("DashboardScreen", "Failed to load stats week", e)
        }
        try {
            val row = withContext(Dispatchers.IO) { db.dailyActivityDao().getByDate(today) }
            row?.let {
                normalToday = it.normalSeconds
                quizToday = it.quizSeconds
                studioToday = it.voiceStudioTimeSeconds
            }
        } catch (e: Exception) {
            android.util.Log.w("DashboardScreen", "Failed to load favorites", e)
        }
        try {
            // Get attempts count once on IO to avoid long-running collection on UI
            val count = withContext(Dispatchers.IO) {
                val flow = db.quizAttemptDao().getAttemptsByDate(today)
                val attempts = flow.first()
                attempts.size
            }
            quizzesToday = count
        } catch (e: Exception) {
            android.util.Log.w("DashboardScreen", "Failed to load attempts", e)
        }

        // Generate recommendations and curriculum at most once per day
        try {
            val lastRun = prefs.getString("last_rec_gen", "")
            if (lastRun != today) {
                // Run on IO; engines already use IO internally as well
                withContext(Dispatchers.IO) {
                    RecommendationEngine(context).generateRecommendations()
                    AdaptiveCurriculumPlanner(context).buildPlan()
                }
                prefs.edit().putString("last_rec_gen", today).apply()
            }
        } catch (e: Exception) {
            android.util.Log.w("DashboardScreen", "Failed to load last verse", e)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        item {
            val nextStepRaw = prefs.getString("next_step_label", null)
            val nextLevel = prefs.getInt("next_level", -1)
            val nextReasonRaw = prefs.getString("next_reason", null)
            // Sanitize any mojibake in persisted values
            fun clean(s: String?): String? {
                if (s == null) return null
                var t = s
                val map = mapOf(
                    "â€¢" to "•", "â€“" to "–", "â€”" to "—", "â€˜" to "‘", "â€™" to "’",
                    "â€œ" to "“", "â€" to "”", "â€¦" to "…", "Ã—" to "×", "Â" to "",
                    "ðŸ" to "", "dY" to "", "�" to ""
                )
                for ((k, v) in map) t = t?.replace(k, v)
                t = t?.replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
                return t
            }
            val nextStep = clean(nextStepRaw)
            val nextReason = clean(nextReasonRaw)
            if ((nextStep != null && nextStepRaw != null && nextStep != nextStepRaw) ||
                (nextReason != null && nextReasonRaw != null && nextReason != nextReasonRaw)) {
                prefs.edit().apply {
                    if (nextStep != null && nextStepRaw != null && nextStep != nextStepRaw) putString("next_step_label", nextStep)
                    if (nextReason != null && nextReasonRaw != null && nextReason != nextReasonRaw) putString("next_reason", nextReason)
                }.apply()
            }
            if (nextStep != null && nextLevel > 0) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Next best action", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("${nextStep ?: ""} at level $nextLevel", style = MaterialTheme.typography.bodyLarge)
                        if (!nextReason.isNullOrBlank()) Text(nextReason!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            when (nextStep) {
                                "Read" -> Button(onClick = onNavigateToNormalMode) { Text("Start Reading") }
                                "Quiz" -> Button(onClick = onNavigateToQuizMode) { Text("Start Quiz") }
                                "Studio" -> Button(onClick = onNavigateToVoiceStudio) { Text("ప్రశ్న సమాధానం") }
                            }
                        }
                    }
                }
            }
            // Welcome Card with Mandala
            AnimatedItem(index = 0) {
                MandalaHeroSection()
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Today's Progress
            AnimatedItem(index = 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Progress",
                        style = MaterialTheme.typography.headlineSmall, // Updated to use new typography
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    var showToday by remember { mutableStateOf(false) }
                    IconButton(onClick = { showToday = true }) {
                        Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Today, contentDescription = "Today")
                    }
                    if (showToday) {
                        TodaySummaryDialog(
                            onDismiss = { showToday = false },
                            verses = versesToday,
                            quizzes = quizzesToday,
                            normalTime = normalToday,
                            quizTime = quizToday,
                            studioTime = studioToday,
                            versesList = versesListToday,
                            onReadMore = onNavigateToNormalMode,
                            onTakeQuiz = onNavigateToQuizMode
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            AnimatedItem(index = 2) {
                AdaptiveStatRow(
                    listOf(
                        StatItem(
                            title = "Time",
                            value = stats?.timeSpentFormatted ?: "0m",
                            icon = "⏱",
                            color = MaterialTheme.colorScheme.primary
                        ),
                        StatItem(
                            title = "Streak",
                            value = "${stats?.currentStreak ?: 0} d",
                            icon = "🔥",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    )
                )
            }
}

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            AdaptiveStatRow(
                listOf(
                    StatItem(
                        title = "Verses",
                        value = "$versesToday",
                        icon = "📖",
                        color = MaterialTheme.colorScheme.tertiary
                    ),
                    StatItem(
                        title = "Quizzes",
                        value = "$quizzesToday",
                        icon = "🧠",
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            // Quick Actions
            AnimatedItem(index = 4) {
                Text(
                    text = "Learning Modes",
                    style = MaterialTheme.typography.headlineSmall, // Updated typography
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        item {
            // Lotus badge preview
            val level = LotusLevelManager.levelFor(stats)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Lotus Level", style = MaterialTheme.typography.titleMedium)
                LotusBadge(level = level, size = 32.dp)
            }
        }

        item {
            // Yoga focus suggestions
            val advice = YogaAdvisor.suggest(stats)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Suggested Focus", style = MaterialTheme.typography.titleMedium)
                    Text(text = "Current Level: ${advice.currentLevel}")
                    Text(text = "Next Levels: ${advice.nextFocusLevels.joinToString(", ")}")
                    advice.tips.forEach { tip -> Text(text = "• $tip", fontSize = 12.sp) }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            // Recommendations & Study Guides preview
            val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
            val recs by db.recommendationDataDao().getActiveRecommendations().collectAsState(initial = emptyList())
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Recommendations", style = MaterialTheme.typography.titleMedium)
                    recs.take(3).forEach { r ->
                        Text(text = "• ${r.recommendationTitle}", fontSize = 12.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(onClick = onNavigateToQuizMode, modifier = Modifier.weight(1f)) {
                            Text("Start Plan")
                        }
                        Button(onClick = onNavigateToRecommendations, modifier = Modifier.weight(1f)) {
                            Text("View All")
                        }
                    }
                }
            }
        }

        item {
            AnimatedItem(index = 5) {
                PremiumDashboardCard(
                    title = "Read Verses",
                    description = "Browse the sacred texts",
                    icon = { Icon(imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read", tint = Color.White) },
                    gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                    onClick = onNavigateToNormalMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
            AnimatedItem(index = 7) {
                PremiumDashboardCard(
                    title = "Take Quiz",
                    description = "Test your knowledge - Text & Voice",
                    icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.School, contentDescription = "Quiz", tint = Color.White) },
                    gradient = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary),
                    onClick = onNavigateToQuizMode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                )
            }
        }

        item {
            AnimatedItem(index = 8) {
                PremiumDashboardCard(
                    title = "ప్రశ్న సమాధానం",
                    description = "Chat & Quiz with AI Wisdom",
                    icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Mic, contentDescription = "Voice Studio", tint = Color.White) },
                    gradient = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary),
                    onClick = onNavigateToVoiceStudio,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }

        item {
        AnimatedItem(index = 8) {
            PremiumDashboardCard(
                title = "Random Sloka",
                description = "Get inspired by a random verse",
                icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Default.Shuffle, contentDescription = "Random", tint = Color.White) },
                gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
                onClick = onNavigateToRandomSloka,
                modifier = Modifier.fillMaxWidth()
            )
        }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                showWelcomeDialog = false
                prefs.edit().putBoolean("has_seen_welcome", true).apply()
            }
        )
    }
}

@Composable
fun DashboardStatCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
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
            Text(
                text = icon,
                fontSize = 32.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
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
fun QuickActionCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit),
    gradient: List<Color>,
    onClick: () -> Unit
) {
    GradientActionCard(
        title = title,
        description = description,
        icon = icon,
        gradient = gradient,
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        cornerRadius = 16.dp,
        iconSize = 40.dp,
        contentPadding = 20.dp,
        elevation = 0.dp,
        titleFontSizeSp = 18,
        descriptionFontSizeSp = 14
    )
}

// Adaptive helpers using global orientation config
data class StatItem(
    val title: String,
    val value: String,
    val icon: String,
    val color: Color,
)

@Composable
fun AdaptiveStatRow(items: List<StatItem>) {
    val uiCfg = LocalUiConfig.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (uiCfg.isLandscape) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { it ->
            DashboardStatCard(
                title = it.title,
                value = it.value,
                icon = it.icon,
                color = it.color,
                modifier = if (uiCfg.isLandscape) Modifier.width(180.dp) else Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AdaptiveQuickActionsRow(content: @Composable RowScope.() -> Unit) {
    val uiCfg = LocalUiConfig.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (uiCfg.isLandscape) Arrangement.SpaceBetween else Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun TodaySummaryDialog(
    onDismiss: () -> Unit,
    verses: Int,
    quizzes: Int,
    normalTime: Long,
    quizTime: Long,
    studioTime: Long = 0,
    versesList: List<com.aipoweredgita.app.database.ReadVerse> = emptyList(),
    onReadMore: () -> Unit = {},
    onTakeQuiz: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        title = { Text("Today’s Summary") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verses viewed: $verses")
                Text("Quizzes taken: $quizzes")
                Text("Normal Mode: ${formatTime(normalTime)}")
                Text("Quiz Mode: ${formatTime(quizTime)}")
                Text("Voice Studio: ${formatTime(studioTime)}")
                if (versesList.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Verses today:")
                    val items = versesList.take(10).joinToString { "${it.chapterNo}:${it.verseNo}" }
                    Text(items)
                    if (versesList.size > 10) Text("…and ${versesList.size - 10} more")
                }
            }
        }
    )
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
