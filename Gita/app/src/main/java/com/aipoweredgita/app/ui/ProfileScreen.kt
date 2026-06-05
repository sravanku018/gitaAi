package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

// ── Shared Sacred Gold Palette (imported from theme) ──────────────────────────
private val Border        = Color(0x14FFFFFF)   // 8 % white
private val BorderHi      = Color(0x24FFFFFF)   // 14 % white

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToQuizStats: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {},
    onNavigateToBadges: () -> Unit = {},
    onNavigateToYogaLevels: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val yogaInfo = com.aipoweredgita.app.ui.components.YogaLevelManager.yogaLevelInfo(stats)
    val levelProgress = com.aipoweredgita.app.ui.components.YogaLevelManager.progressInLevel(stats)
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val authPrefs = remember { com.aipoweredgita.app.utils.AuthPreferences.getInstance(context) }
    val isGuest = remember { authPrefs.isGuest }
    
    var name by remember { mutableStateOf(stats?.userName ?: "") }
    var dob by remember { mutableStateOf(stats?.dateOfBirth ?: "") }
    var isEditing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(stats) {
        stats?.let {
            name = it.userName
            dob = it.dateOfBirth
        }
    }

    val isDark = rememberThemeIsDark()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val gold = if (isDark) GoldSpark else Saffron

    Box(modifier = modifier.fillMaxSize().background(appBg)) {
        if (isDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── Header Section ───────────────────────────────────────────────
            ProfileHeader(ProfileInfo(name, stats?.age ?: 0), yogaInfo, levelProgress, onNavigateToYogaLevels)

            // ── Guest Login Banner ──────────────────────────────────────────
            if (isGuest) {
                Spacer(Modifier.height(16.dp))
                GuestLoginBanner(onLoginClick = onNavigateToLogin)
            }

            Spacer(Modifier.height(24.dp))

            // ── Profile Info Card ────────────────────────────────────────────
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

            // ── Dashboard / Stats ────────────────────────────────────────────
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
                SmallStatItem(SmallStatData("Badges", "${stats?.totalFavorites ?: 0}", Icons.Default.EmojiEvents), Modifier.weight(1f))
            }

            Spacer(Modifier.height(24.dp))

            // ── Quiz Card ────────────────────────────────────────────────────
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

            Spacer(Modifier.height(16.dp))

            // ── Appearance ───────────────────────────────────────────────────
            CreativeCard(title = "Appearance", icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode) {
                Text(
                    text = "Theme mode is controlled from Settings, where System, Light, and Dark are all available.",
                    style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary.copy(alpha = 0.9f))
                )
            }
        }
    }
}

@Composable
private fun GuestLoginBanner(onLoginClick: () -> Unit) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = gold.copy(alpha = 0.1f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(
                colors = listOf(gold.copy(alpha = 0.3f), gold.copy(alpha = 0.1f))
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = null,
                tint = gold,
                modifier = Modifier.size(32.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Guest Mode",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = gold
                    )
                )
                Text(
                    text = "Create an account to save your progress and sync across devices",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                )
            }
            
            Button(
                onClick = onLoginClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = gold,
                    contentColor = Color(0xFF1A0F00)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Login",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

private data class ProfileInfo(
    val name: String,
    val age: Int
)

@Composable
private fun ProfileHeader(
    profile: ProfileInfo,
    yogaInfo: com.aipoweredgita.app.ui.components.YogaLevelManager.YogaLevelInfo,
    levelProgress: Float,
    onClick: () -> Unit
) {
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFFF9628).copy(alpha = if (isDark) 0.15f else 0.25f),
                        Color.Transparent
                    )
                )
            )
            .clickable(onClick = onClick, indication = null, interactionSource = remember { MutableInteractionSource() }),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Avatar with Progress Ring
            Box(contentAlignment = Alignment.Center) {
                // Progress Arc
                androidx.compose.foundation.Canvas(modifier = Modifier.size(110.dp)) {
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

                // Core Avatar
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = yogaInfo.emoji,
                        fontSize = 44.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = profile.name.ifEmpty { "Arjuna" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = textPrimary.copy(alpha = 0.95f),
                    fontWeight = FontWeight.Bold
                )
            )

            Surface(
                color = gold.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "${yogaInfo.yogaName} � Step ${yogaInfo.step}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = gold,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            if (profile.age > 0) {
                Text(
                    text = "${profile.age} Year Old Seeker",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = textSecondary,
                        fontStyle = FontStyle.Italic
                    )
                )
            }
        }
    }
}

@Composable
private fun CreativeCard(
    title: String,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = rememberThemeIsDark()
    val textLabel = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    val clickModifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .then(clickModifier),
        cornerRadius = 32.dp,
        elevation = 4.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = gold, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = textLabel,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    )
                )
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

private data class SmallStatData(
    val label: String,
    val value: String,
    val icon: ImageVector
)

@Composable
private fun SmallStatItem(stat: SmallStatData, modifier: Modifier = Modifier) {
    val uiCfg = LocalUiConfig.current
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

    GlassCard(
        modifier = modifier.height(if (uiCfg.isLandscape) 75.dp else 90.dp),
        cornerRadius = 24.dp,
        elevation = 2.dp,
        tint = cardBg,
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(stat.icon, contentDescription = null, tint = gold.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Column {
                Text(stat.value, style = MaterialTheme.typography.titleMedium.copy(color = textPrimary.copy(alpha = 0.9f), fontWeight = FontWeight.Bold))
                Text(stat.label, style = MaterialTheme.typography.labelSmall.copy(color = textSecondary, fontSize = 9.sp))
            }
        }
    }
}

@Composable
private fun ProfileDisplayInfo(name: String, dob: String, age: Int, onEdit: () -> Unit) {
    Column {
        ProfileInfoRow("Name", name.ifEmpty { "Not set" })
        ProfileInfoRow("Birthday", dob.ifEmpty { "Not set" })
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
            shape = MaterialTheme.shapes.small
        ) {
            Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ProfileEditForm(
    name: String, onNameChange: (String) -> Unit,
    dob: String, onDobChange: (String) -> Unit,
    onSave: () -> Unit, onCancel: () -> Unit
) {
    val isDark = rememberThemeIsDark()
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron

    Column {
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Spiritual Name", color = textSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = gold,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedLabelColor = gold,
                unfocusedLabelColor = textSecondary
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dob, onValueChange = onDobChange,
            label = { Text("DOB (YYYY-MM-DD)", color = textSecondary) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = gold,
                unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.15f),
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedLabelColor = gold,
                unfocusedLabelColor = textSecondary
            )
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, gold),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = gold)
            ) { Text("Cancel") }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Saffron)
            ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = textSecondary))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = textPrimary.copy(alpha = 0.9f), fontWeight = FontWeight.SemiBold))
    }
}

