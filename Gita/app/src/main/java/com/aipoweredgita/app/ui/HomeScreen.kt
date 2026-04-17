package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.components.GradientActionCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Chat
import com.aipoweredgita.app.viewmodel.ScreenConfigViewModel
import com.aipoweredgita.app.ui.components.YogaProgressionBar
import com.aipoweredgita.app.ui.components.WelcomeDialog
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.YogaProgression
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

data class ModeItem(
    val title: String,
    val description: String,
    val icon: @Composable (() -> Unit),
    val gradient: List<Color>,
    val onClick: () -> Unit
)

@Composable
fun HomeScreen(
    onNavigateToNormalMode: () -> Unit,
    onNavigateToQuizMode: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToOfflineDownload: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToWidgetSettings: () -> Unit = {},
    onNavigateToDailyActivity: () -> Unit = {},
    onNavigateToRandomSloka: () -> Unit = {},
    screenConfigViewModel: ScreenConfigViewModel = viewModel()
) {
    val screenConfig by screenConfigViewModel.screenConfig.collectAsState()
    val uiCfg = LocalUiConfig.current
    val isTablet = screenConfig.isTablet
    val isLandscape = uiCfg.isLandscape
    val columns = screenConfig.gridColumns
    val padding = (if (uiCfg.isLandscape) screenConfig.screenPadding + 8 else screenConfig.screenPadding).dp
    
    // Get yoga progression
    val context = LocalContext.current
    val database = remember { GitaDatabase.getDatabase(context) }
    val progression by database.yogaProgressionDao().getProgressionFlow().collectAsState(initial = null)

    // Initialize yoga progression if not exists
    LaunchedEffect(Unit) {
        try {
            val dao = database.yogaProgressionDao()
            val existing = dao.getProgression()
            if (existing == null) {
                dao.insertProgression(com.aipoweredgita.app.database.YogaProgression())
            }
        } catch (e: Exception) {
            android.util.Log.e("HomeScreen", "Error initializing progression: ${e.message}")
        }
    }


    // Define all mode items
    val modeItems = listOf(
        ModeItem(
            title = "Normal Mode",
            description = "Read and explore verses from the Bhagavad Gita",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read") },
            gradient = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)),
            onClick = onNavigateToNormalMode
        ),
        ModeItem(
            title = "Daily Activity",
            description = "See where you spent time by date",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Timeline, contentDescription = "Activity") },
            gradient = listOf(Color(0xFF3B82F6), Color(0xFF60A5FA)),
            onClick = onNavigateToDailyActivity
        ),
        ModeItem(
            title = "Quiz Mode",
            description = "Test your knowledge with random questions",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.School, contentDescription = "Quiz") },
            gradient = listOf(Color(0xFF10B981), Color(0xFF059669)),
            onClick = onNavigateToQuizMode
        ),
        ModeItem(
            title = "Favorites",
            description = "View and manage your saved verses",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Favorite, contentDescription = "Favorites") },
            gradient = listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
            onClick = onNavigateToFavorites
        ),
        ModeItem(
            title = "Offline Mode",
            description = "Download all verses for offline access (~3-4 MB)",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.CloudDownload, contentDescription = "Offline") },
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFF6366F1)),
            onClick = onNavigateToOfflineDownload
        ),
        ModeItem(
            title = "My Profile",
            description = "View your stats, achievements, and progress",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Person, contentDescription = "Profile") },
            gradient = listOf(Color(0xFF06B6D4), Color(0xFF10B981)),
            onClick = onNavigateToProfile
        ),

        ModeItem(
            title = "Random Sloka",
            description = "Get inspired by a random verse",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Shuffle, contentDescription = "Random") },
            gradient = listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
            onClick = onNavigateToRandomSloka
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Column(
            modifier = Modifier.padding(vertical = if (isTablet && isLandscape) 16.dp else 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bhagavad Gita",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Choose Your Learning Mode",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        
        // Yoga Progression Bar - always show with default if null
        YogaProgressionBar(
            progression = progression ?: com.aipoweredgita.app.database.YogaProgression(),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Adaptive grid layout based on device and orientation
        if (isTablet && columns > 1) {
            // Tablet: Use LazyVerticalGrid for better performance
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                horizontalArrangement = Arrangement.spacedBy(screenConfig.itemSpacing.dp),
                verticalArrangement = Arrangement.spacedBy(screenConfig.itemSpacing.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(modeItems) { item ->
                    ModeCard(
                        title = item.title,
                        description = item.description,
                        icon = item.icon,
                        gradient = item.gradient,
                        onClick = item.onClick
                    )
                }
            }
        } else {
            // Phone: Use scrollable Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(screenConfig.itemSpacing.dp)
            ) {
                modeItems.forEach { item ->
                    ModeCard(
                        title = item.title,
                        description = item.description,
                        icon = item.icon,
                        gradient = item.gradient,
                        onClick = item.onClick
                    )
                }
            }
        }
    }
}

@Composable
fun ModeCard(
    title: String,
    description: String,
    icon: @Composable (() -> Unit),
    gradient: List<Color>,
    onClick: () -> Unit,
    screenConfigViewModel: ScreenConfigViewModel = viewModel()
) {
    val screenConfig by screenConfigViewModel.screenConfig.collectAsState()

    GradientActionCard(
        title = title,
        description = description,
        icon = icon,
        gradient = gradient,
        onClick = {
            try {
                onClick()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .height(screenConfig.cardHeight.dp),
        cornerRadius = 16.dp,
        iconSize = 48.dp,
        contentPadding = 20.dp,
        elevation = 4.dp,
        titleFontSizeSp = 20,
        descriptionFontSizeSp = 14
    )
}
