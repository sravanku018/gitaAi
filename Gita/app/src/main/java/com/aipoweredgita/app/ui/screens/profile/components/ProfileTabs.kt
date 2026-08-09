package com.aipoweredgita.app.ui.screens.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.domain.model.ProfileUiState
import com.aipoweredgita.app.network.YogaLevel
import com.aipoweredgita.app.network.YogaSubStage
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.ui.components.VerticalProgressRoad
import com.aipoweredgita.app.ui.components.YogaLevelManager
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.ui.theme.rememberThemeIsDark
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun StatsTab(
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
fun BadgesTab(viewModel: ProfileViewModel) {
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
fun YogaPathTab(
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
