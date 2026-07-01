package com.aipoweredgita.app.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.YogaLevel
import com.aipoweredgita.app.network.YogaSubStage
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.ui.screens.profile.components.*
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
    initialTab: Int = 0,
    onNavigateToQuizStats: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val totalCoins by viewModel.coinBalance.collectAsState()
    
    val context = LocalContext.current
    val authPrefs = remember { com.aipoweredgita.app.utils.AuthPreferences.getInstance(context) }
    val isGuest = remember { authPrefs.isGuestUser }
    
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Stats", "Badges", "Yoga Path")

    val yogaInfo = YogaLevelManager.yogaLevelInfo(stats)
    val levelProgress = YogaLevelManager.progressInLevel(stats)

    var yogaLevels by remember { mutableStateOf<List<YogaLevel>>(emptyList()) }
    var yogaSubStages by remember { mutableStateOf<List<YogaSubStage>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val res = CoinApi.retrofitService.getYogaStages()
            yogaLevels = res.levels
            yogaSubStages = res.sub_stages
        } catch (_: Exception) {}
    }

    val activeLevel = yogaLevels.find { totalCoins >= it.min_coins && totalCoins <= it.max_coins }
        ?: yogaLevels.lastOrNull()
    val activeSubStage = yogaSubStages.find { totalCoins >= it.min_coins && totalCoins <= it.max_coins }
        ?: yogaSubStages.filter { it.level == activeLevel?.level }.maxByOrNull { it.sub_level }

    val displayYogaName = activeLevel?.name ?: yogaInfo.yogaName
    val displayStep = activeSubStage?.sub_level ?: yogaInfo.step
    val displayEmoji = activeLevel?.let { yl ->
        when(yl.level) { 1 -> "🌿"; 2 -> "🔥"; 3 -> "🧠"; 4 -> "📘"; else -> "🌸" }
    } ?: yogaInfo.emoji

    val displayProgress = activeLevel?.let { yl ->
        val range = yl.max_coins - yl.min_coins
        if (range > 0) {
            ((totalCoins - yl.min_coins).toFloat() / range).coerceIn(0f, 1f)
        } else 0f
    } ?: levelProgress

    val customYogaInfo = YogaLevelManager.YogaLevelInfo(
        level = activeLevel?.level ?: yogaInfo.level,
        step = displayStep,
        yogaName = displayYogaName,
        yogaDescription = activeLevel?.description ?: yogaInfo.yogaDescription,
        emoji = displayEmoji
    )

    val isDark = isDarkTheme
    val appBg = MaterialTheme.colorScheme.background
    val gold = if (isDark) GoldSpark else Saffron

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(appBg)) {
                ProfileHeader(
                    ProfileInfo(stats?.userName ?: "Arjuna", stats?.age ?: 0),
                    customYogaInfo,
                    displayProgress,
                    totalCoins
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = gold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = gold
                        )
                    },
                    divider = {}
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        },
        containerColor = appBg
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
            
            when (selectedTab) {
                0 -> StatsTab(
                    stats = stats,
                    uiState = uiState,
                    isGuest = isGuest,
                    onNavigateToLogin = onNavigateToLogin,
                    onNavigateToQuizStats = onNavigateToQuizStats,
                    viewModel = viewModel
                )
                1 -> BadgesTab(viewModel = viewModel)
                2 -> YogaPathTab(
                    viewModel = viewModel,
                    yogaLevels = yogaLevels,
                    yogaSubStages = yogaSubStages
                )
            }
        }
    }
}
