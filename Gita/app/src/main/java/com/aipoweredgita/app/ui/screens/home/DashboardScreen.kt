package com.aipoweredgita.app.ui.screens.home

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.BuildConfig
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.DailyRewardsStrip
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.components.QuizCardBg
import com.aipoweredgita.app.ui.components.ReadCardBg
import com.aipoweredgita.app.ui.components.SlokaCardBg
import com.aipoweredgita.app.ui.components.VoiceCardBg
import com.aipoweredgita.app.ui.components.WelcomeDialog
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.ui.screens.home.components.DashboardHeroCard
import com.aipoweredgita.app.ui.screens.home.components.DashboardModeCard
import com.aipoweredgita.app.ui.screens.home.components.DashboardRecommendationsCard
import com.aipoweredgita.app.ui.screens.home.components.GlassStatRow
import com.aipoweredgita.app.ui.screens.home.components.ModeCardData
import com.aipoweredgita.app.ui.screens.home.components.StatsData
import com.aipoweredgita.app.ui.theme.UiDefaults
import com.aipoweredgita.app.ui.theme.rememberGitaColors
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.delay

@Composable
fun AnimatedItem(
    index: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 50L)
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
    val recommendations by viewModel.recommendations.collectAsState()

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

    val colors = rememberGitaColors()
    val appBg = MaterialTheme.colorScheme.background

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
                color = colors.accent
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
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = colors.accentSoft.copy(alpha = 0.85f),
                                letterSpacing = 0.4.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Śrīmad Bhagavad Gītā",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                letterSpacing = (-0.5).sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "🪙 $coinBalance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentSoft,
                                    modifier = Modifier.clickable { onNavigateToCoinHistory() }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val yogaName = uiState.serverYogaLevel?.name
                                    ?: YogaLevelManager.yogaLevelInfo(stats).yogaName
                                val step = uiState.serverYogaSubStage?.sub_level
                                    ?: YogaLevelManager.stepFor(stats)

                                Text(
                                    text = "$yogaName · Step $step",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    modifier = Modifier.clickable { onNavigateToAwakening() }
                                )
                            }
                        }

                        GlassCard(
                            modifier = Modifier.size(46.dp),
                            tint = colors.accent.copy(alpha = 0.15f),
                            border = colors.accent.copy(alpha = 0.3f),
                            cornerRadius = UiDefaults.CornerRadius
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ॐ",
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        color = if (colors.isDark) Color.White else colors.accent,
                                        shadow = Shadow(
                                            color = colors.accent.copy(alpha = 0.5f),
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

            item {
                AnimatedItem(index = 1) {
                    val primaryAction = when (nextAction.nextStep) {
                        "Read" -> onNavigateToNormalMode
                        "Quiz" -> onNavigateToQuizMode
                        "Studio" -> onNavigateToVoiceStudio
                        else -> onNavigateToNormalMode
                    }
                    DashboardHeroCard(
                        nextAction = nextAction,
                        language = language,
                        onPrimaryAction = primaryAction,
                        onViewPlan = { onNavigateToRecommendations(1) },
                    )
                }
            }

            item {
                AnimatedItem(index = 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today's Stats",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        IconButton(onClick = onNavigateToActivityHistory) {
                            Icon(
                                imageVector = Icons.Filled.Today,
                                contentDescription = "History",
                                tint = colors.accentSoft
                            )
                        }
                    }
                }
            }

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

            item {
                AnimatedItem(index = 4) {
                    DailyRewardsStrip(
                        tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(context),
                        context = context,
                        coinBalance = coinBalance,
                        onEarnCoins = { amount, description ->
                            viewModel.claimDailyReward(amount, description)
                        },
                        onNavigateToShare = onNavigateToRandomSloka,
                        onMessage = { msg ->
                            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            letterSpacing = (-0.3).sp
                        )
                    }
                }
            }

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

            item {
                AnimatedItem(index = 7) {
                    DashboardRecommendationsCard(
                        recommendations = recommendations,
                        onViewPlans = { onNavigateToRecommendations(1) },
                        onViewAll = { onNavigateToRecommendations(0) },
                    )
                }
            }
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                showWelcomeDialog = false
                prefs.edit().putInt("last_seen_version", BuildConfig.VERSION_CODE).apply()
            },
            onNavigateToBattleQuiz = onNavigateToQuizMode
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
