package com.aipoweredgita.app.ui.screens.home

import androidx.compose.foundation.background
import com.aipoweredgita.app.ui.screens.home.components.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.components.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import android.content.Context
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.components.DailyRewardsStrip
import com.aipoweredgita.app.ui.components.WelcomeDialog
import com.aipoweredgita.app.ui.components.MandalaBackground
import com.aipoweredgita.app.ui.components.PremiumDashboardCard
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.util.TimeUtils
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.aipoweredgita.app.BuildConfig

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onNavigateToNormalMode: () -> Unit,
    onNavigateToQuizMode: () -> Unit,
    onNavigateToVoiceStudio: () -> Unit = {},
    onNavigateToRecommendations: (Int) -> Unit,
    onNavigateToRandomSloka: () -> Unit = {},
    onNavigateToAwakening: () -> Unit = {},
    onNavigateToCoinHistory: () -> Unit = {},
    onNavigateToActivityHistory: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val stats by viewModel.stats.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    
    val language = remember { prefs.getString("quiz_language", "eng") ?: "eng" }

    val nextAction by viewModel.nextAction.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()

    // Welcome Dialog State
    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val lastSeenVersion = prefs.getInt("last_seen_version", 0)
        val currentVersion = BuildConfig.VERSION_CODE
        if (lastSeenVersion < currentVersion) {
            showWelcomeDialog = true
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData(context)
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            viewModel.loadDashboardData(context)
            isRefreshing = false
        }
    }

    val isDark = rememberThemeIsDark()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true },
        state = pullToRefreshState,
        modifier = modifier
            .fillMaxSize()
            .background(appBg),
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = MaterialTheme.colorScheme.surface,
                color = if (isDark) GoldSpark else Saffron
            )
        }
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
                                color = if (isDark) GoldSpark.copy(alpha = 0.8f) else Saffron,
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Bhagavad Gita",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary,
                                letterSpacing = (-0.6).sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🪙 $coinBalance",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GoldSpark,
                                    modifier = Modifier.clickable { onNavigateToCoinHistory() }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val yogaName = uiState.serverYogaLevel?.name 
                                    ?: YogaLevelManager.yogaLevelInfo(stats).yogaName
                                val step = uiState.serverYogaSubStage?.sub_level 
                                    ?: YogaLevelManager.stepFor(stats)
                                
                                Text(
                                    text = "$yogaName · Step $step",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    modifier = Modifier.clickable { onNavigateToAwakening() }
                                )

                            }
                        }
                        
                        val omBadgeColor = if (isDark) Color(0xFFFF6E00) else Saffron
                        GlassCard(
                            modifier = Modifier.size(46.dp),
                            tint = omBadgeColor.copy(alpha = 0.15f),
                            border = omBadgeColor.copy(alpha = 0.3f),
                            cornerRadius = 16.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ॐ",
                                    fontSize = 22.sp,
                                    color = if (isDark) Color.White else Saffron,
                                    style = androidx.compose.ui.text.TextStyle(
                                        shadow = Shadow(
                                            color = omBadgeColor.copy(alpha = 0.5f),
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
                        tint = if (isDark) Color(0xFFC85000).copy(alpha = 0.22f) else Saffron.copy(alpha = 0.08f),
                        border = if (isDark) Color(0xFFFF8C28).copy(alpha = 0.28f) else Saffron.copy(alpha = 0.2f),
                        cornerRadius = 32.dp,
                        elevation = 8.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .drawBehind {
                                    val brush = Brush.linearGradient(
                                        colors = if (isDark) listOf(
                                            Color(0xFFFF7800).copy(alpha = 0.18f),
                                            Color(0xFF962800).copy(alpha = 0.12f),
                                            Color.Transparent
                                        ) else listOf(
                                            Saffron.copy(alpha = 0.06f),
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
                                color = if (isDark) Color.White.copy(alpha = 0.07f) else Saffron.copy(alpha = 0.06f)
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
                                            color = if (isDark) Color.White.copy(alpha = 0.97f) else textPrimary,
                                            letterSpacing = (-0.5).sp
                                        )
                                        Text(
                                            text = "Continue your spiritual journey",
                                            fontSize = 12.sp,
                                            color = if (isDark) Color(0xFFFFDCA0).copy(alpha = 0.75f) else textSecondary,
                                        )
                                    }
                                    val rotationChevron by animateFloatAsState(
                                        targetValue = if (heroOpen) 0f else -90f,
                                        label = "chevron_rotation"
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(MaterialTheme.shapes.small)
                                            .background(if (isDark) Color.White.copy(alpha = 0.1f) else Saffron.copy(alpha = 0.1f))
                                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Saffron.copy(alpha = 0.25f), MaterialTheme.shapes.small)
                                            .graphicsLayer { rotationZ = rotationChevron },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = "⌄", fontSize = 13.sp, color = if (isDark) Color(0xFFFFC864).copy(alpha = 0.8f) else Saffron)
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
                                                .background(if (isDark) Color.White.copy(alpha = 0.1f) else Saffron.copy(alpha = 0.15f))
                                        )
                                        Spacer(modifier = Modifier.height(13.dp))
                                        Pill(text = "NEXT BEST ACTION", textColor = if (isDark) Color(0xFFFFB450).copy(alpha = 0.9f) else Saffron)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = if (nextAction.nextStep != null && nextAction.nextLevel > 0) {
                                                "${nextAction.nextStep ?: ""} at Level ${nextAction.nextLevel} · ${nextAction.nextReason ?: "Balance your modes"}"
                                            } else {
                                                "Embark on your sacred journey through the Gita. Read verses to build your daily wisdom."
                                            },
                                            fontSize = 13.sp,
                                            color = if (isDark) Color(0xFFFFEBC8).copy(alpha = 0.75f) else textSecondary,
                                            lineHeight = 18.sp
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            val actionText = when (nextAction.nextStep) {
                                                "Read" -> "Start Reading"
                                                "Quiz" -> "Start Quiz"
                                                "Studio" -> if (language == "tel") "ప్రశ్న సమాధానం" else "Voice Q&A"
                                                else -> "Begin Reading"
                                            }
                                            val actionClick = when (nextAction.nextStep) {
                                                "Read" -> onNavigateToNormalMode
                                                "Quiz" -> onNavigateToQuizMode
                                                "Studio" -> onNavigateToVoiceStudio
                                                else -> onNavigateToNormalMode
                                            }
                                            
                                            Button(
                                                onClick = actionClick,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isDark) Color.White.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primary
                                                ),
                                                shape = RoundedCornerShape(50.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                                            ) {
                                                Text(text = actionText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White.copy(alpha = 0.92f) else MaterialTheme.colorScheme.onPrimary)
                                            }
                                            
                                            Button(
                                                onClick = { onNavigateToRecommendations(1) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                                shape = RoundedCornerShape(50.dp),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFFFFC864).copy(alpha = 0.2f) else Saffron.copy(alpha = 0.4f))
                                            ) {
                                                Text(text = "View Plan", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color(0xFFFFC864).copy(alpha = 0.7f) else Saffron)
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
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        IconButton(onClick = onNavigateToActivityHistory) {
                            Icon(imageVector = Icons.Filled.Today, contentDescription = "History", tint = Color(0xFFFFB450))
                        }
                    }
                }
            }

            // Stats Cards Row — Day Streak & Share Streak
            item {
                AnimatedItem(index = 3) {
                    GlassStatRow(
                        stats = StatsData(
                            dayStreak = stats?.currentStreak ?: 0,
                            coins = coinBalance
                        )
                    )
                }
            }

            // Daily Rewards Strip
            item {
                AnimatedItem(index = 4) {
                    com.aipoweredgita.app.ui.components.DailyRewardsStrip(
                        tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(context),
                        context = context,
                        isDark = isDark,
                        coinBalance = coinBalance,
                        onEarnCoins = { amount, description ->
                            viewModel.claimDailyReward(amount, description)
                        },
                        onNavigateToShare = onNavigateToRandomSloka
                    )
                }
            }

            // Learning Modes section label
            item {
                AnimatedItem(index = 5) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Learning Modes",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    }
                }
            }

            // 2x2 Square Mode Cards
            item {
                AnimatedItem(index = 6) {
                    val gridColumns = if (LocalUiConfig.current.isLandscape) 4 else 2
                    if (gridColumns == 4) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardModeCard(
                                card = ModeCardData("🎓", "Take Quiz", "Text & Voice"),
                                bgContent = { QuizCardBg() },
                                onClick = onNavigateToQuizMode,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardModeCard(
                                card = ModeCardData("🎙", if (language == "tel") "ప్రశ్న సమాధానం" else "Voice Q&A", "AI Wisdom"),
                                bgContent = { VoiceCardBg() },
                                onClick = onNavigateToVoiceStudio,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardModeCard(
                                card = ModeCardData("✦", "Random Sloka", "Daily"),
                                bgContent = { SlokaCardBg() },
                                onClick = onNavigateToRandomSloka,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardModeCard(
                                card = ModeCardData("📖", "Read Verses", "Sacred"),
                                bgContent = { ReadCardBg() },
                                onClick = onNavigateToNormalMode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DashboardModeCard(
                                card = ModeCardData("🎓", "Take Quiz", "Text & Voice"),
                                bgContent = { QuizCardBg() },
                                onClick = onNavigateToQuizMode,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardModeCard(
                                card = ModeCardData("🎙", if (language == "tel") "ప్రశ్న సమాధానం" else "Voice Q&A", "AI Wisdom"),
                                onClick = onNavigateToVoiceStudio,
                                bgContent = { VoiceCardBg() },
                                modifier = Modifier.weight(1f)
                            )
                        }
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                DashboardModeCard(
                                    card = ModeCardData("✦", "Random Sloka", "Daily inspiration"),
                                    bgContent = { SlokaCardBg() },
                                    onClick = onNavigateToRandomSloka,
                                    modifier = Modifier.weight(1f)
                                )
                                DashboardModeCard(
                                    card = ModeCardData("📖", "Read Verses", "Sacred texts"),
                                    bgContent = { ReadCardBg() },
                                    onClick = onNavigateToNormalMode,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // Recommendations — Collapsible
            item {
                var recoOpen by remember { mutableStateOf(true) }
                val recs by viewModel.recommendations.collectAsState()
                
                AnimatedItem(index = 7) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        tint = if (isDark) Color.White.copy(alpha = 0.03f) else Saffron.copy(alpha = 0.05f),
                        cornerRadius = 32.dp,
                        elevation = 4.dp
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
                                        .clip(MaterialTheme.shapes.large)
                                        .background(Color(0xFFFF9628).copy(alpha = 0.15f))
                                        .border(1.dp, Color(0xFFFF9628).copy(alpha = 0.25f), MaterialTheme.shapes.large)
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
                                        .clip(MaterialTheme.shapes.small)
                                        .background(if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.04f))
                                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f), MaterialTheme.shapes.small)
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
                                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f))
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
                                            RecommendationItem("Continue in Quiz Mode", "🎓", "Quiz"),
                                            RecommendationItem("Review Chapter 1", "📖", "Read"),
                                            RecommendationItem("Focus on Yoga Level 1", "🧘", "Level")
                                        )
                                        fallbackRecs.forEachIndexed { i, item ->
                                            RecommendationRow(
                                                item = item,
                                                isDark = isDark,
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
                                                item = RecommendationItem(r.recommendationTitle, emoji, tag),
                                                isDark = isDark,
                                                showDivider = i < recs.size.coerceAtMost(3) - 1
                                            )
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { onNavigateToRecommendations(1) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                            shape = MaterialTheme.shapes.large,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9628).copy(alpha = 0.35f)),
                                            contentPadding = PaddingValues(vertical = 11.dp)
                                        ) {
                                            Text("View Plans", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else textPrimary)
                                        }
                                        
                                        Button(
                                            onClick = { onNavigateToRecommendations(0) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f)),
                                            shape = MaterialTheme.shapes.large,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.06f)),
                                            contentPadding = PaddingValues(vertical = 11.dp)
                                        ) {
                                            Text("View All", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = if (isDark) Color.White.copy(alpha = 0.45f) else textPrimary.copy(alpha = 0.5f))
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
                prefs.edit().putInt("last_seen_version", BuildConfig.VERSION_CODE).apply()
            }
        )
    }

}

data class TodayStats(
    val verses: Int,
    val quizzes: Int,
    val normalTime: Long,
    val quizTime: Long,
    val studioTime: Long,
    val versesList: List<com.aipoweredgita.app.database.ReadVerse>
)

