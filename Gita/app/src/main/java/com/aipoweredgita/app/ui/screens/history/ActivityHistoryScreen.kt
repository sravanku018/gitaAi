package com.aipoweredgita.app.ui.screens.history

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
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.ReadVerse
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.viewmodel.ActivityHistoryViewModel
import com.aipoweredgita.app.domain.model.ActivityHistoryEvent
import com.aipoweredgita.app.util.TimeUtils
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.charts.PerformanceTrendLineChart
import com.aipoweredgita.app.ui.charts.SpiritualPathRadarChart
import com.aipoweredgita.app.ui.LocalUiConfig
import com.aipoweredgita.app.ui.screens.history.components.AHTipsTab
import com.aipoweredgita.app.ui.screens.history.components.CalendarTab
import com.aipoweredgita.app.ui.screens.history.components.OverviewTab
import com.aipoweredgita.app.ui.screens.history.components.QuizTab

// ─── MAIN SCREEN ─────────────────────────────────────────────────────────────
@Composable
fun ActivityHistoryScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 0,
    viewModel: ActivityHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val totalCoins by viewModel.coinBalance.collectAsState()
    
    var yogaLevels by remember { mutableStateOf<List<com.aipoweredgita.app.network.YogaLevel>>(emptyList()) }
    var yogaSubStages by remember { mutableStateOf<List<com.aipoweredgita.app.network.YogaSubStage>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val res = com.aipoweredgita.app.network.CoinApi.retrofitService.getYogaStages()
            yogaLevels = res.levels
            yogaSubStages = res.sub_stages
        } catch (_: Exception) {}
    }
    
    val userStats = state.userStats
    val allActivity = state.allActivity
    val attempts = state.attempts
    val averageAccuracy = state.averageAccuracy
    val averageTime = state.averageTime
    val quiz10Stats = state.quiz10Stats
    val quiz15Stats = state.quiz15Stats
    val quiz20Stats = state.quiz20Stats
    val quiz25Stats = state.quiz25Stats
    val quiz30Stats = state.quiz30Stats
    val battleQuizStats = state.battleQuizStats
    val selectedQuizSize = state.selectedQuizSize
    val karmaCount = state.karmaYogaCount
    val bhaktiCount = state.bhaktiYogaCount
    val jnanaCount = state.jnanaYogaCount
    val dhyanaCount = state.dhyanaYogaCount
    val rajaCount = state.rajaYogaCount

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
            0 -> OverviewTab(
                userStats = userStats,
                totalCoins = totalCoins,
                yogaLevels = yogaLevels,
                yogaSubStages = yogaSubStages,
                totalQuizAttempts = attempts.size
            )
            1 -> QuizTab(
                attempts = attempts,
                averageAccuracy = averageAccuracy,
                averageTime = averageTime,
                quiz10Stats = quiz10Stats,
                quiz15Stats = quiz15Stats,
                quiz20Stats = quiz20Stats,
                quiz25Stats = quiz25Stats,
                quiz30Stats = quiz30Stats,
                battleQuizStats = battleQuizStats,
                selectedQuizSize = selectedQuizSize,
                onSelectQuizSize = { viewModel.onEvent(ActivityHistoryEvent.SelectQuizSize(it)) },
                userStats = userStats,
                karmaCount = karmaCount,
                bhaktiCount = bhaktiCount,
                jnanaCount = jnanaCount,
                dhyanaCount = dhyanaCount,
                rajaCount = rajaCount
            )
            2 -> CalendarTab()
            3 -> AHTipsTab(averageAccuracy = averageAccuracy)
        }
    }
}
