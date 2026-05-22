package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
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
import com.aipoweredgita.app.ui.components.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
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
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.quiz.OrnamentRule
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
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
                        Saffron,
                        Saffron.copy(alpha = 0.7f)
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

private fun clean(s: String?): String? {
    if (s == null) return null
    var t = s
    val map = mapOf(
        "â€¢" to "•", "â€“" to "–", "â€”" to "—", "â€˜" to "‘", "â€™" to "’",
        "â€œ" to "“", "â€ " to "”", "â€¦" to "…", "Ã—" to "×", "Â" to "",
        "ðŸ" to "", "dY" to "", "" to ""
    )
    for ((k, v) in map) t = t?.replace(k, v)
    t = t?.replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
    return t
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
    val language = remember { prefs.getString("quiz_language", "eng") ?: "eng" }
    val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
    var versesToday by remember { mutableStateOf(0) }
    var quizzesToday by remember { mutableStateOf(0) }
    var normalToday by remember { mutableStateOf(0L) }
    var quizToday by remember { mutableStateOf(0L) }
    var studioToday by remember { mutableStateOf(0L) }
    var versesListToday by remember { mutableStateOf<List<com.aipoweredgita.app.database.ReadVerse>>(emptyList()) }

    // Read initial cached values synchronously to avoid visual flicker during load
    val initialNextStepRaw = remember { prefs.getString("next_step_label", null) }
    val initialNextLevel = remember { prefs.getInt("next_level", -1) }
    val initialNextReasonRaw = remember { prefs.getString("next_reason", null) }

    var nextStep by remember { mutableStateOf<String?>(clean(initialNextStepRaw)) }
    var nextLevel by remember { mutableIntStateOf(initialNextLevel) }
    var nextReason by remember { mutableStateOf<String?>(clean(initialNextReasonRaw)) }

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
                val cleanedStep = clean(suggestion.nextStep)
                val cleanedReason = clean(suggestion.reason)
                withContext(Dispatchers.IO) {
                    prefs.edit()
                        .putString("next_step_label", cleanedStep)
                        .putInt("next_level", suggestion.nextLevel)
                        .putString("next_reason", cleanedReason)
                        .putString("next_suggestion_date", today)
                        .apply()
                }
                nextStep = cleanedStep
                nextLevel = suggestion.nextLevel
                nextReason = cleanedReason
            } else {
                val rawStep = prefs.getString("next_step_label", null)
                val rawReason = prefs.getString("next_reason", null)
                val cleanedStep = clean(rawStep)
                val cleanedReason = clean(rawReason)
                if (cleanedStep != rawStep || cleanedReason != rawReason) {
                    withContext(Dispatchers.IO) {
                        prefs.edit().apply {
                            if (cleanedStep != rawStep) putString("next_step_label", cleanedStep)
                            if (cleanedReason != rawReason) putString("next_reason", cleanedReason)
                        }.apply()
                    }
                }
                nextStep = cleanedStep
                nextLevel = prefs.getInt("next_level", -1)
                nextReason = cleanedReason
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF080400))
    ) {
        AmbientOrbs(modifier = Modifier.fillMaxSize())

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            item {
                AnimatedItem(index = 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                            val greeting = when {
                                hour < 12 -> "Good morning"
                                hour < 17 -> "Good afternoon"
                                else -> "Good evening"
                            }
                            Text(
                                text = greeting,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFAA3C).copy(alpha = 0.75f),
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bhagavad Gita",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.95f),
                                letterSpacing = (-0.6).sp
                            )
                        }
                        
                        GlassCard(
                            modifier = Modifier.size(46.dp),
                            tint = Color(0xFFFF6E00).copy(alpha = 0.15f),
                            border = Color(0xFFFF8228).copy(alpha = 0.3f),
                            cornerRadius = 16.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ॐ",
                                    fontSize = 22.sp,
                                    color = Color.White,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(
                                            color = Color(0xFFFF7800).copy(alpha = 0.5f),
                                            offset = Offset(0f, 0f),
                                            blurRadius = 8f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Hero — Collapsible Namaste & Next Best Action
            item {
                var heroOpen by remember { mutableStateOf(true) }
                AnimatedItem(index = 1) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        tint = Color(0xFFC85000).copy(alpha = 0.22f),
                        border = Color(0xFFFF8C28).copy(alpha = 0.28f),
                        cornerRadius = 28.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFF7800).copy(alpha = 0.18f),
                                            Color(0xFF962800).copy(alpha = 0.12f),
                                            Color.Transparent
                                        ),
                                        start = Offset(0f, 0f),
                                        end = Offset(size.width, size.height)
                                    )
                                    drawRect(brush)
                                }
                        ) {
                            MandalaBackground(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(140.dp)
                                    .offset(x = 10.dp, y = (-10).dp),
                                color = Color.White.copy(alpha = 0.07f)
                            )

                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { heroOpen = !heroOpen }
                                        .padding(horizontal = 20.dp, vertical = 18.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(text = "🙏", fontSize = 30.sp)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Namaste",
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White.copy(alpha = 0.97f),
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "Continue your spiritual journey",
                                            fontSize = 12.sp,
                                            color = Color(0xFFFFDCA0).copy(alpha = 0.75f)
                                        )
                                    }
                                    val rotationChevron by animateFloatAsState(
                                        targetValue = if (heroOpen) 0f else -90f,
                                        label = "chevron_rotation"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(Color.White.copy(alpha = 0.1f))
                                            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(9.dp))
                                            .graphicsLayer { rotationZ = rotationChevron },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "⌄", fontSize = 13.sp, color = Color(0xFFFFC864).copy(alpha = 0.8f))
                                    }
                                }

                                AnimatedVisibility(
                                    visible = heroOpen,
                                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(300)),
                                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(300))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 20.dp, end = 20.dp, bottom = 18.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(Color.White.copy(alpha = 0.1f))
                                        )
                                        Spacer(modifier = Modifier.height(13.dp))
                                        Pill(text = "NEXT BEST ACTION")
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (nextStep != null && nextLevel > 0) {
                                                "${nextStep ?: ""} at Level $nextLevel · ${nextReason ?: "Balance your modes"}"
                                            } else {
                                                "Read at Level 1 · Keep consistent"
                                            },
                                            fontSize = 13.sp,
                                            color = Color(0xFFFFEBC8).copy(alpha = 0.75f),
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val actionText = when (nextStep) {
                                                "Read" -> "Start Reading"
                                                "Quiz" -> "Start Quiz"
                                                "Studio" -> if (language == "tel") "ప్రశ్న సమాధానం" else "Voice Q&A"
                                                else -> "Explore"
                                            }
                                            val actionClick = when (nextStep) {
                                                "Read" -> onNavigateToNormalMode
                                                "Quiz" -> onNavigateToQuizMode
                                                "Studio" -> onNavigateToVoiceStudio
                                                else -> onNavigateToNormalMode
                                            }
                                            
                                            Button(
                                                onClick = actionClick,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.15f)),
                                                shape = RoundedCornerShape(50.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                                            ) {
                                                Text(text = actionText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.92f))
                                            }
                                            
                                            Button(
                                                onClick = onNavigateToRecommendations,
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                                shape = RoundedCornerShape(50.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFFC864).copy(alpha = 0.2f))
                                            ) {
                                                Text(text = "View Plan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFFFFC864).copy(alpha = 0.7f))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stats Label Row
            item {
                AnimatedItem(index = 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Stats",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.88f),
                            letterSpacing = (-0.3).sp
                        )
                        var showToday by remember { mutableStateOf(false) }
                        IconButton(onClick = { showToday = true }) {
                            Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Today, contentDescription = "Today", tint = Color(0xFFFFB450))
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

            // Stats Cards Row
            item {
                AnimatedItem(index = 3) {
                    GlassStatRow(
                        timeValue = stats?.timeSpentFormatted ?: "0m",
                        streakValue = "${stats?.currentStreak ?: 0}d"
                    )
                }
            }

            // Learning Modes section label & Lotus Level badge
            item {
                AnimatedItem(index = 4) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Learning Modes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.88f),
                            letterSpacing = (-0.3).sp
                        )
                        val level = LotusLevelManager.levelFor(stats)
                        Pill(
                            text = "Lotus · Lv $level",
                            color = Color.White.copy(alpha = 0.06f),
                            textColor = Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
            }

            // 2x2 Square Mode Cards
            item {
                AnimatedItem(index = 5) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardModeCard(
                                emoji = "🎓",
                                title = "Take Quiz",
                                sub = "Text & Voice · 10m",
                                bgContent = { QuizCardBg() },
                                onClick = onNavigateToQuizMode,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardModeCard(
                                emoji = "🎙",
                                title = if (language == "tel") "ప్రశ్న సమాధానం" else "Voice Q&A",
                                sub = "AI Wisdom · Live",
                                bgContent = { VoiceCardBg() },
                                onClick = onNavigateToVoiceStudio,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            DashboardModeCard(
                                emoji = "✦",
                                title = "Random Sloka",
                                sub = "Daily inspiration",
                                bgContent = { SlokaCardBg() },
                                onClick = onNavigateToRandomSloka,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardModeCard(
                                emoji = "📖",
                                title = "Read Verses",
                                sub = "Sacred texts",
                                bgContent = { ReadCardBg() },
                                onClick = onNavigateToNormalMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Recommendations — Collapsible
            item {
                var recoOpen by remember { mutableStateOf(true) }
                val recs by db.recommendationDataDao().getActiveRecommendations().collectAsState(initial = emptyList())
                
                AnimatedItem(index = 6) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        tint = Color.White.copy(alpha = 0.03f),
                        cornerRadius = 24.dp
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { recoOpen = !recoOpen }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "✦ RECOMMENDATIONS",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFA532),
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(Color(0xFFFF9628).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFFF9628).copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${recs.size} items",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFA532),
                                        letterSpacing = 0.3.sp
                                    )
                                }
                                
                                val rotationChevron by animateFloatAsState(
                                    targetValue = if (recoOpen) 0f else -90f,
                                    label = "reco_chevron_rotation"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(26.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.07f))
                                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                        .graphicsLayer { rotationZ = rotationChevron },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "⌄", fontSize = 12.sp, color = Color(0xFFFFC864).copy(alpha = 0.7f))
                                }
                            }
                            
                            if (recoOpen) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .padding(horizontal = 16.dp)
                                        .background(Color.White.copy(alpha = 0.06f))
                                )
                            }
                            
                            AnimatedVisibility(
                                visible = recoOpen,
                                enter = expandVertically(animationSpec = tween(400)) + fadeIn(animationSpec = tween(300)),
                                exit = shrinkVertically(animationSpec = tween(400)) + fadeOut(animationSpec = tween(300))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 4.dp)
                                ) {
                                    if (recs.isEmpty()) {
                                        val fallbackRecs = listOf(
                                            Triple("Continue in Quiz Mode", "🎓", "Quiz"),
                                            Triple("Review Chapter 1", "📖", "Read"),
                                            Triple("Focus on Yoga Level 1", "🧘", "Level")
                                        )
                                        fallbackRecs.forEachIndexed { i, item ->
                                            RecommendationRow(
                                                text = item.first,
                                                icon = item.second,
                                                tag = item.third,
                                                showDivider = i < fallbackRecs.size - 1
                                            )
                                        }
                                    } else {
                                        recs.take(3).forEachIndexed { i, r ->
                                            val (emoji, tag) = when (r.recommendationType) {
                                                "verse" -> "📖" to "Read"
                                                "topic" -> "🎓" to "Quiz"
                                                "yogalevel" -> "🧘" to "Level"
                                                "question" -> "🎙" to "Voice"
                                                else -> "✦" to "Gita"
                                            }
                                            RecommendationRow(
                                                text = r.recommendationTitle,
                                                icon = emoji,
                                                tag = tag,
                                                showDivider = i < recs.size.coerceAtMost(3) - 1
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = onNavigateToQuizMode,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9628).copy(alpha = 0.35f)),
                                            contentPadding = PaddingValues(vertical = 11.dp)
                                        ) {
                                            Text("Start Plan", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        
                                        Button(
                                            onClick = onNavigateToRecommendations,
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.06f)),
                                            shape = RoundedCornerShape(16.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                                            contentPadding = PaddingValues(vertical = 11.dp)
                                        ) {
                                            Text("View All", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.45f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
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
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
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
        confirmButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = GoldSpark)) { Text("Close") } },
        title = { Text("Today’s Summary", color = GoldSpark) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verses viewed: $verses", color = MaterialTheme.colorScheme.onSurface)
                Text("Quizzes taken: $quizzes", color = MaterialTheme.colorScheme.onSurface)
                Text("Normal Mode: ${formatTime(normalTime)}", color = MaterialTheme.colorScheme.onSurface)
                Text("Quiz Mode: ${formatTime(quizTime)}", color = MaterialTheme.colorScheme.onSurface)
                Text("Voice Studio: ${formatTime(studioTime)}", color = MaterialTheme.colorScheme.onSurface)
                if (versesList.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("Verses today:", color = GoldSpark)
                    val items = versesList.take(10).joinToString { "${it.chapterNo}:${it.verseNo}" }
                    Text(items, color = MaterialTheme.colorScheme.onSurface)
                    if (versesList.size > 10) Text("…and ${versesList.size - 10} more", color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@Composable
fun Pill(
    text: String,
    color: Color = Color(0xFFFF6E00).copy(alpha = 0.25f),
    textColor: Color = Color(0xFFFFB450).copy(alpha = 0.9f)
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(color)
            .border(1.dp, textColor.copy(alpha = 0.27f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun DashboardModeCard(
    emoji: String,
    title: String,
    sub: String,
    bgContent: @Composable BoxScope.() -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = tween(100),
        label = "scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .border(1.dp, Color.White.copy(alpha = 0.13f), RoundedCornerShape(22.dp))
    ) {
        bgContent()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.05f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.55f)
                        )
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent)
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(15.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 22.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.3f),
                            offset = Offset(0f, 4f),
                            blurRadius = 14f
                        )
                    )
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.97f),
                    lineHeight = 16.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.6f),
                            offset = Offset(0f, 1f),
                            blurRadius = 10f
                        )
                    )
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = sub,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 0.2.sp,
                    style = androidx.compose.ui.text.TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.5f),
                            offset = Offset(0f, 1f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}

@Composable
fun GlassStatRow(
    timeValue: String,
    streakValue: String
) {
    val uiCfg = LocalUiConfig.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val itemModifier = if (uiCfg.isLandscape) Modifier.width(180.dp) else Modifier.weight(1f)
        
        GlassCard(
            modifier = itemModifier,
            tint = Color.White.copy(alpha = 0.04f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text("⏱", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = timeValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF7828)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Time Today",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 0.4.sp
                )
            }
        }

        GlassCard(
            modifier = itemModifier,
            tint = Color.White.copy(alpha = 0.04f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 16.dp)
            ) {
                Text("🔥", fontSize = 22.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = streakValue,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFFBE28)
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Day Streak",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 0.4.sp
                )
            }
        }
    }
}

@Composable
fun RecommendationRow(
    text: String,
    icon: String,
    tag: String,
    showDivider: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = icon, fontSize = 15.sp)
            }
            
            Text(
                text = text,
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.65f),
                modifier = Modifier.weight(1f)
            )
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF9628).copy(alpha = 0.1f))
                    .border(1.dp, Color(0xFFFF9628).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tag,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFA532),
                    letterSpacing = 0.4.sp
                )
            }
            
            Text(
                text = "›",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.18f)
            )
        }
        
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.05f))
            )
        }
    }
}
