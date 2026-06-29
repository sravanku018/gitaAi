package com.aipoweredgita.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.YogaLevel
import com.aipoweredgita.app.network.YogaSubStage
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.ui.components.*
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import com.aipoweredgita.app.domain.model.ProfileUiState
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.BadgeCategory
import kotlinx.coroutines.launch
import kotlin.math.*

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
    
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
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

@Composable
private fun StatsTab(
    stats: com.aipoweredgita.app.database.UserStats?,
    uiState: ProfileUiState,
    isGuest: Boolean,
    onNavigateToLogin: () -> Unit,
    onNavigateToQuizStats: () -> Unit,
    viewModel: ProfileViewModel
) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(stats?.userName ?: "") }
    var dob by remember { mutableStateOf(stats?.dateOfBirth ?: "") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(stats) {
        stats?.let {
            name = it.userName
            dob = it.dateOfBirth
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp)
    ) {
        if (isGuest) {
            Spacer(Modifier.height(16.dp))
            GuestLoginBanner(onLoginClick = onNavigateToLogin)
        }

        Spacer(Modifier.height(24.dp))

        CreativeCard(title = "Seeker Details", icon = Icons.Default.Person) {
            if (isEditing) {
                ProfileEditForm(
                    name = name,
                    onNameChange = { name = it },
                    dob = dob,
                    onDobChange = { dob = it },
                    onSave = {
                        scope.launch {
                            viewModel.updateProfile(name, dob)
                            isEditing = false
                        }
                    },
                    onCancel = { isEditing = false }
                )
            } else {
                ProfileDisplayInfo(
                    name = name,
                    dob = dob,
                    age = stats?.age ?: 0,
                    onEdit = { isEditing = true }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        OrnamentRule()
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sacred Journey Stats",
            style = MaterialTheme.typography.titleMedium.copy(
                color = gold,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatItem(SmallStatData("Time", stats?.timeSpentFormatted ?: "0m", Icons.Default.Timer), Modifier.weight(1f))
            SmallStatItem(SmallStatData("Verses", "${stats?.versesRead ?: 0}", Icons.AutoMirrored.Filled.MenuBook), Modifier.weight(1f))
            SmallStatItem(SmallStatData("Quizzes", "${stats?.totalQuizzesTaken ?: 0}", Icons.Default.Quiz), Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmallStatItem(SmallStatData("Streak", "${stats?.currentStreak ?: 0} days", Icons.Default.Whatshot), Modifier.weight(1f))
            SmallStatItem(SmallStatData("Badges", "${uiState.badges.size}", Icons.Default.EmojiEvents), Modifier.weight(1f))
        }

        Spacer(Modifier.height(24.dp))

        CreativeCard(
            title = "Quiz Performance",
            icon = Icons.Default.EmojiEvents,
            onClick = onNavigateToQuizStats
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Best Score: ${stats?.bestScore ?: 0}/${stats?.bestScoreOutOf ?: 0}",
                        style = MaterialTheme.typography.bodyLarge.copy(color = textPrimary.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Accuracy: ${stats?.accuracyPercentage?.toInt() ?: 0}%",
                        style = MaterialTheme.typography.bodySmall.copy(color = textPrimary.copy(alpha = 0.6f))
                    )
                }
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = gold)
            }
        }
    }
}



@Composable
private fun BadgesTab(viewModel: ProfileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val badges = uiState.badges
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProgressionHeroSection()

        if (badges.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🏅", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "No badges unlocked yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Read verses and complete quizzes to earn sacred honors!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            }
        } else {
            badges.forEach { badge ->
                BadgeItem(badge = badge, goldColor = gold)
            }
        }
    }
}

@Composable
private fun BadgeItem(badge: UserBadge, goldColor: Color) {
    val textPrimary = MaterialTheme.colorScheme.onBackground
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        cornerRadius = 20.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Badge Icon (Large Emoji inside a stylized circle)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(goldColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.5.dp, goldColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge.icon,
                    fontSize = 28.sp
                )
            }
            
            // Badge Information
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = badge.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                Spacer(Modifier.height(4.dp))
                
                Text(
                    text = badge.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textPrimary.copy(alpha = 0.7f)
                )
                
                Spacer(Modifier.height(6.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .background(goldColor.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge.category.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = goldColor
                        )
                    }
                    
                    // Unlocked Date
                    Text(
                        text = "Unlocked: ${badge.unlockedAt}",
                        style = MaterialTheme.typography.bodySmall,
                        color = textPrimary.copy(alpha = 0.5f)
                    )
                }
            }
            
            // Badge Level (if greater than 0)
            if (badge.level > 0) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Lv. ${badge.level}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = goldColor
                    )
                    Spacer(Modifier.height(2.dp))
                    Row {
                        repeat(badge.level.coerceAtMost(5)) {
                            Text("⭐", fontSize = 8.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YogaPathTab(
    viewModel: ProfileViewModel,
    yogaLevels: List<YogaLevel>,
    yogaSubStages: List<YogaSubStage>
) {
    val stats by viewModel.stats.collectAsState()
    val totalCoins by viewModel.coinBalance.collectAsState()
    val intensity = YogaLevelManager.compositeScore(stats)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            SacredFlame(modifier = Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MandalaBadge(intensity = intensity, modifier = Modifier.size(80.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        if (yogaLevels.isNotEmpty()) {
            val activeLevel = yogaLevels.find { totalCoins >= it.min_coins && totalCoins <= it.max_coins }
                ?: yogaLevels.lastOrNull()
            val currentLevelIndex = activeLevel?.let { yogaLevels.indexOf(it) } ?: 0
            val currentProgress = activeLevel?.let { yl ->
                val range = yl.max_coins - yl.min_coins
                if (range > 0) {
                    ((totalCoins - yl.min_coins).toFloat() / range * 100f).coerceIn(0f, 100f)
                } else 0f
            } ?: 0f

            VerticalProgressRoad(
                currentLevel = currentLevelIndex,
                currentProgress = currentProgress,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(Modifier.height(24.dp))
        }

        yogaLevels.forEachIndexed { index, yl ->
            val subs = yogaSubStages.filter { it.level == yl.level }.sortedBy { it.sub_level }
            val done = totalCoins > yl.max_coins
            val active = totalCoins >= yl.min_coins && totalCoins <= yl.max_coins
            val locked = !done && !active

            YogaMargStage(
                emoji = when(yl.level) { 1 -> "🌿"; 2 -> "🔥"; 3 -> "🧠"; 4 -> "📘"; else -> "🌸" },
                name = yl.name,
                range = "${yl.min_coins} – ${yl.max_coins}",
                subs = subs,
                currentCoins = totalCoins,
                done = done,
                active = active,
                locked = locked,
                isLast = index == yogaLevels.lastIndex
            )
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: ProfileInfo,
    yogaInfo: YogaLevelManager.YogaLevelInfo,
    levelProgress: Float,
    totalCoins: Int
) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(100.dp)) {
                drawArc(
                    color = gold.copy(alpha = 0.2f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )
                drawArc(
                    color = gold,
                    startAngle = -90f,
                    sweepAngle = 360f * levelProgress,
                    useCenter = false,
                    style = Stroke(width = 4.dp.toPx())
                )
            }
            Text(text = yogaInfo.emoji, fontSize = 40.sp)
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${yogaInfo.yogaName} · Step ${yogaInfo.step}",
                style = MaterialTheme.typography.labelLarge,
                color = gold,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            Text(text = "🪙 $totalCoins", style = MaterialTheme.typography.labelLarge, color = gold)
        }
    }
}

@Composable
private fun GuestLoginBanner(onLoginClick: () -> Unit) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = gold.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PersonAdd, contentDescription = null, tint = gold, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Guest Mode", fontWeight = FontWeight.Bold, color = gold)
                Text("Login to save progress", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onLoginClick, colors = ButtonDefaults.buttonColors(containerColor = gold)) {
                Text("Login", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CreativeCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).let { if (onClick != null) it.clickable { onClick() } else it },
        cornerRadius = 24.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun SmallStatItem(stat: SmallStatData, modifier: Modifier = Modifier) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    GlassCard(modifier = modifier.height(80.dp), cornerRadius = 16.dp) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(stat.icon, contentDescription = null, tint = gold.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Column {
                Text(stat.value, fontWeight = FontWeight.Bold)
                Text(stat.label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ProfileDisplayInfo(name: String, dob: String, age: Int, onEdit: () -> Unit) {
    Column {
        ProfileInfoRow("Name", name.ifEmpty { "Arjuna" })
        ProfileInfoRow("Birthday", dob.ifEmpty { "Not set" })
        Spacer(Modifier.height(12.dp))
        Button(onClick = onEdit, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Saffron)) {
            Text("Edit Profile", color = Color.White)
        }
    }
}

@Composable
private fun ProfileEditForm(
    name: String, onNameChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    onSave: () -> Unit, onCancel: () -> Unit
) {
    Column {
        OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Spiritual Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = dob, onValueChange = onDobChange, label = { Text("DOB (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(onClick = onSave, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Saffron)) {
                Text("Save", color = Color.White)
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private data class ProfileInfo(val name: String, val age: Int)
private data class SmallStatData(val label: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun ProgressionHeroSection() {
    Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(16.dp)).background(Brush.horizontalGradient(listOf(GoldSpark, Saffron)))) {
        Column(modifier = Modifier.padding(24.dp).align(Alignment.CenterStart)) {
            Text("Your Journey", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Ascending the steps of Yoga", color = Color.White.copy(alpha = 0.8f))
        }
    }
}



@Composable
private fun YogaMargStage(
    emoji: String,
    name: String,
    range: String,
    subs: List<YogaSubStage>,
    currentCoins: Int,
    done: Boolean,
    active: Boolean,
    locked: Boolean,
    isLast: Boolean
) {
    val gold = if (rememberThemeIsDark()) GoldSpark else Saffron
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (active) gold.copy(alpha = 0.08f) else Color.Transparent
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) gold else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Main Stage Header Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(if (locked) Color.Gray.copy(alpha = 0.1f) else gold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(emoji, fontSize = 20.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (locked) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (done) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Completed", tint = Color.Green)
                } else if (active) {
                    Text(
                        text = "Active",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Sub-stages list (only display if not locked, to show progress or target steps)
            if (!locked && subs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    subs.forEach { sub ->
                        val subDone = currentCoins > sub.max_coins
                        val subActive = currentCoins >= sub.min_coins && currentCoins <= sub.max_coins
                        val subLocked = !subDone && !subActive
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Sub-stage Bullet/Status
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .background(
                                        color = when {
                                            subDone -> Color.Green.copy(alpha = 0.15f)
                                            subActive -> gold.copy(alpha = 0.2f)
                                            else -> Color.Gray.copy(alpha = 0.1f)
                                        },
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when {
                                            subDone -> Color.Green
                                            subActive -> gold
                                            else -> Color.Gray.copy(alpha = 0.4f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (subDone) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.Green,
                                        modifier = Modifier.size(10.dp)
                                    )
                                } else if (subActive) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(gold, CircleShape)
                                    )
                                }
                            }
                            
                            Spacer(Modifier.width(12.dp))
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sub.sub_name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (subActive) FontWeight.Bold else FontWeight.Normal,
                                    color = when {
                                        subLocked -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        subActive -> gold
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SacredFlame(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "FlameEffects")
    
    val scaleY by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleY"
    )

    val scaleX by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scaleX"
    )

    val translationX by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "translationX"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                listOf(
                    Color.Transparent,
                    Saffron.copy(alpha = 0.05f),
                    Saffron.copy(alpha = 0.25f)
                )
            )
        ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Text(
            text = "🔥",
            fontSize = 80.sp,
            modifier = Modifier
                .padding(bottom = 20.dp)
                .graphicsLayer(
                    scaleX = scaleX,
                    scaleY = scaleY,
                    translationX = translationX
                )
        )
    }
}

@Composable
private fun MandalaBadge(intensity: Float, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "MandalaEffects")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(rotationZ = rotation)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2.3f

            drawCircle(
                color = GoldSpark.copy(alpha = glowAlpha),
                radius = baseRadius * 1.2f
            )

            drawCircle(
                color = GoldSpark.copy(alpha = 0.08f),
                radius = baseRadius
            )
            drawCircle(
                color = GoldSpark,
                radius = baseRadius,
                style = Stroke(width = 2.dp.toPx())
            )

            val petalCount = 12
            for (i in 0 until petalCount) {
                val angleRad = (i * 2 * Math.PI / petalCount)
                val petalX = center.x + cos(angleRad).toFloat() * (baseRadius * 0.85f)
                val petalY = center.y + sin(angleRad).toFloat() * (baseRadius * 0.85f)
                
                drawLine(
                    color = GoldSpark.copy(alpha = 0.4f),
                    start = center,
                    end = Offset(petalX, petalY),
                    strokeWidth = 1.dp.toPx()
                )
                drawCircle(
                    color = GoldSpark.copy(alpha = 0.8f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(petalX, petalY)
                )
            }
        }

        Text(
            text = "🕉️",
            fontSize = 28.sp
        )
    }
}
