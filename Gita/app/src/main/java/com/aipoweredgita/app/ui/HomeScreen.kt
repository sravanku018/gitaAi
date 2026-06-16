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
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
            gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
            onClick = onNavigateToNormalMode
        ),
        ModeItem(
            title = "Daily Activity",
            description = "See where you spent time by date",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Timeline, contentDescription = "Activity") },
            gradient = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary),
            onClick = onNavigateToDailyActivity
        ),
        ModeItem(
            title = "Quiz Mode",
            description = "Test your knowledge with random questions",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.School, contentDescription = "Quiz") },
            gradient = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary),
            onClick = onNavigateToQuizMode
        ),
        ModeItem(
            title = "Favorites",
            description = "View and manage your saved verses",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Favorite, contentDescription = "Favorites") },
            gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
            onClick = onNavigateToFavorites
        ),
        ModeItem(
            title = "Offline Mode",
            description = "Download all verses for offline access (~3-4 MB)",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.CloudDownload, contentDescription = "Offline") },
            gradient = listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary),
            onClick = onNavigateToOfflineDownload
        ),
        ModeItem(
            title = "My Profile",
            description = "View your stats, achievements, and progress",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Person, contentDescription = "Profile") },
            gradient = listOf(MaterialTheme.colorScheme.tertiary, MaterialTheme.colorScheme.primary),
            onClick = onNavigateToProfile
        ),
        ModeItem(
            title = "Random Sloka",
            description = "Get inspired by a random verse",
            icon = { Icon(imageVector = androidx.compose.material.icons.Icons.Filled.Shuffle, contentDescription = "Random") },
            gradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary),
            onClick = onNavigateToRandomSloka
        )
    )

    val isDark = isSystemInDarkTheme()
    val appBg = MaterialTheme.colorScheme.background
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) Color(0xFFFFD050) else Color(0xFFE8600A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

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
                    color = gold
                )

                Text(
                    text = "Choose Your Learning Mode",
                    style = MaterialTheme.typography.titleMedium,
                    color = textSecondary,
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "scale"
    )

    val isDark = isSystemInDarkTheme()
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) Color(0xFFFFD050) else Color(0xFFE8600A)
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(screenConfig.cardHeight.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    try {
                        onClick()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            ),
        cornerRadius = 32.dp,
        elevation = 6.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Glowing orange-tinted icon box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFFFF7800).copy(alpha = if (isDark) 0.12f else 0.08f))
                    .border(1.dp, Color(0xFFFF9628).copy(alpha = if (isDark) 0.25f else 0.15f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalContentColor provides (if (isDark) Color(0xFFFFB450) else Color(0xFFE8600A))
                ) {
                    icon()
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = gold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    fontSize = 13.sp,
                    color = textSecondary,
                    lineHeight = 17.sp
                )
            }

            Icon(
                imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = gold.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
