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
import kotlinx.coroutines.launch
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
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
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Stats", "Badges", "Yoga Path")

    val yogaInfo = YogaLevelManager.yogaLevelInfo(stats)
    val levelProgress = YogaLevelManager.progressInLevel(stats)

    val isDark = isDarkTheme
    val appBg = MaterialTheme.colorScheme.background
    val gold = if (isDark) GoldSpark else Saffron

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(appBg)) {
                ProfileHeader(
                    ProfileInfo(stats?.userName ?: "Arjuna", stats?.age ?: 0),
                    yogaInfo,
                    levelProgress,
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
            if (isDark) {
                AmbientOrbs(modifier = Modifier.fillMaxSize())
            }
            
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
                2 -> YogaPathTab(viewModel = viewModel)
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ProgressionHeroSection()
        
        val context = LocalContext.current
        val database = remember { com.aipoweredgita.app.database.GitaDatabase.getDatabase(context) }
        val progression by database.yogaProgressionDao().getProgressionFlow().collectAsState(initial = null)

        val currentLevelIndex = progression?.yogaLevel ?: 0
        val getStatus = { levelIndex: Int ->
             if (levelIndex - 1 < currentLevelIndex) TimelineStatus.COMPLETED
             else if (levelIndex - 1 == currentLevelIndex) TimelineStatus.CURRENT
             else TimelineStatus.LOCKED
        }

        // 1. Karma Yoga
        val karmaStart = Color(0xFF10B981)
        val karmaEnd = Color(0xFF059669)
        val status1 = getStatus(1)
        TimelineLevelItem(1, "Karma Yoga", "Path of Action", "🌿", karmaStart, karmaEnd, status1, isFirst = true)
        TimelineStepItem(1, "Swadharma", "Do your duty honestly", "①", karmaEnd, status1)
        TimelineStepItem(2, "Nishkama Karma", "Action without desire", "②", karmaEnd, status1)
        TimelineStepItem(3, "Ishwara Arpanam", "Offer work to God", "③", karmaEnd, status1)

        // 2. Bhakti Yoga
        val bhaktiStart = Color(0xFFEC4899)
        val bhaktiEnd = Color(0xFFF43F5E)
        val status2 = getStatus(2)
        TimelineLevelItem(2, "Bhakti Yoga", "Path of Devotion", "🔥", bhaktiStart, bhaktiEnd, status2)
        TimelineStepItem(4, "Bhakti Drida", "Firm Devotion", "④", bhaktiEnd, status2)
        TimelineStepItem(5, "Surrender", "Saranagati", "⑤", bhaktiEnd, status2)
        TimelineStepItem(6, "Prem Bhakti", "Divine Love", "⑥", bhaktiEnd, status2)

        // 3. Jnana Yoga
        val jnanaStart = Color(0xFF8B5CF6)
        val jnanaEnd = Color(0xFF6366F1)
        val status3 = getStatus(3)
        TimelineLevelItem(3, "Jnana Yoga", "Path of Knowledge", "🧠", jnanaStart, jnanaEnd, status3)
        TimelineStepItem(8, "Self-Inquiry", "Who am I?", "⑧", jnanaEnd, status3)
        TimelineStepItem(9, "Discrimination", "Real vs Unreal", "⑨", jnanaEnd, status3)
    }
}

@Composable
private fun YogaPathTab(viewModel: ProfileViewModel) {
    val stats by viewModel.stats.collectAsState()
    val totalCoins by viewModel.coinBalance.collectAsState()
    val intensity = YogaLevelManager.compositeScore(stats)

    var yogaLevels by remember { mutableStateOf<List<YogaLevel>>(emptyList()) }
    var yogaSubStages by remember { mutableStateOf<List<YogaSubStage>>(emptyList()) }

    LaunchedEffect(Unit) {
        try {
            val res = CoinApi.retrofitService.getYogaStages()
            yogaLevels = res.levels
            yogaSubStages = res.sub_stages
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Awakening Consciousness",
            style = MaterialTheme.typography.headlineSmall.copy(color = GoldSpark, fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )
        
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            SacredFlame(modifier = Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MandalaBadge(intensity = intensity, modifier = Modifier.size(80.dp))
                Text("🪙 $totalCoins", color = GoldSpark, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(24.dp))

        yogaLevels.forEachIndexed { index, yl ->
            val subs = yogaSubStages.filter { it.level == yl.level }.sortedBy { it.sub_level }
            val done = totalCoins >= yl.max_coins
            val active = totalCoins in yl.min_coins until yl.max_coins
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
private fun TimelineLevelItem(level: Int, name: String, description: String, emoji: String, colorStart: Color, colorEnd: Color, status: TimelineStatus, isFirst: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 24.sp, modifier = Modifier.padding(end = 16.dp))
        Column {
            Text("$level. $name", fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TimelineStepItem(step: Int, name: String, description: String, icon: String, color: Color, status: TimelineStatus) {
    Row(modifier = Modifier.fillMaxWidth().padding(start = 40.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
            Text(step.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall)
        }
    }
}

enum class TimelineStatus { LOCKED, CURRENT, COMPLETED }

@Composable
private fun YogaMargStage(emoji: String, name: String, range: String, subs: List<YogaSubStage>, currentCoins: Int, done: Boolean, active: Boolean, locked: Boolean, isLast: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = if(active) Saffron.copy(alpha = 0.1f) else Color.Transparent), border = BorderStroke(1.dp, if(active) Saffron else Color.Gray.copy(alpha = 0.2f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold)
                Text(range, style = MaterialTheme.typography.labelSmall)
            }
            if(done) Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
        }
    }
}

@Composable
private fun SacredFlame(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(Brush.verticalGradient(listOf(Color.Transparent, Saffron.copy(alpha = 0.3f)))), contentAlignment = Alignment.BottomCenter) {
        Text("🔥", fontSize = 80.sp, modifier = Modifier.padding(bottom = 20.dp))
    }
}

@Composable
private fun MandalaBadge(intensity: Float, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = GoldSpark.copy(alpha = 0.2f), radius = size.minDimension / 2)
            drawCircle(color = GoldSpark, radius = size.minDimension / 2, style = Stroke(2.dp.toPx()))
        }
        Text("🕉️", fontSize = 24.sp)
    }
}
