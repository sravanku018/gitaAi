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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.quiz.OrnamentRule
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

// ── Shared Sacred Gold Palette (imported from theme) ──────────────────────────
private val Border        = Color(0x14FFFFFF)   // 8 % white
private val BorderHi      = Color(0x24FFFFFF)   // 14 % white
private val TextPrimary   = TextWhite
private val TextSecondary = TextDim
private val TextMuted     = Color(0x52E8E8E8)   // 32 %

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel(),
    onNavigateToQuizStats: () -> Unit = {},
    isDarkTheme: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {},
    onNavigateToBadges: () -> Unit = {},
    onNavigateToYogaLevels: () -> Unit = {}
) {
    val stats by viewModel.stats.collectAsState()
    val yogaInfo = com.aipoweredgita.app.ui.components.LotusLevelManager.yogaLevelInfo(stats)
    val levelProgress = com.aipoweredgita.app.ui.components.LotusLevelManager.progressInLevel(stats)
    
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

    Box(modifier = modifier.fillMaxSize().background(BgDark)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // ── Header Section ───────────────────────────────────────────────
            ProfileHeader(name, stats?.age ?: 0, yogaInfo, levelProgress, onNavigateToYogaLevels)

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
                    color = GoldSpark,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                ),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallStatItem("Time", stats?.timeSpentFormatted ?: "0m", Icons.Default.Timer, Modifier.weight(1f))
                SmallStatItem("Verses", "${stats?.versesRead ?: 0}", Icons.AutoMirrored.Filled.MenuBook, Modifier.weight(1f))
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmallStatItem("Streak", "${stats?.currentStreak ?: 0} 🔥", Icons.Default.Whatshot, Modifier.weight(1f))
                SmallStatItem("Badges", "${stats?.totalFavorites ?: 0}", Icons.Default.EmojiEvents, Modifier.weight(1f))
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
                            style = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Accuracy: ${stats?.accuracyPercentage?.toInt() ?: 0}%",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                        )
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = GoldSpark)
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Appearance ───────────────────────────────────────────────────
            CreativeCard(title = "Appearance", icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Dark Mode", style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary))
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldSpark,
                            checkedTrackColor = GoldSpark.copy(alpha = 0.3f),
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = Surface2
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    age: Int,
    yogaInfo: com.aipoweredgita.app.ui.components.LotusLevelManager.YogaLevelInfo,
    levelProgress: Float,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(
                Brush.verticalGradient(listOf(GoldSpark.copy(alpha = 0.15f), BgDark))
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
                        color = GoldSpark.copy(alpha = 0.2f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 4.dp.toPx())
                    )
                    drawArc(
                        color = GoldSpark,
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
                        .background(Surface1, CircleShape)
                        .border(1.dp, BorderHi, CircleShape),
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
                text = name.ifEmpty { "Arjuna" },
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            )

            Surface(
                color = GoldSpark.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text(
                    text = "${yogaInfo.yogaName} — Step ${yogaInfo.step}",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = GoldSpark,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            if (age > 0) {
                Text(
                    text = "$age Year Old Seeker",
                    modifier = Modifier.padding(top = 4.dp),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextMuted,
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = Surface1,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, BorderHi)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = GoldSpark, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = TextSecondary,
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

@Composable
private fun SmallStatItem(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.height(85.dp),
        shape = RoundedCornerShape(16.dp),
        color = Surface1,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Border)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(icon, contentDescription = null, tint = GoldSpark.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleMedium.copy(color = TextPrimary, fontWeight = FontWeight.Bold))
                Text(label, style = MaterialTheme.typography.labelSmall.copy(color = TextMuted, fontSize = 9.sp))
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
            colors = ButtonDefaults.buttonColors(containerColor = GoldSpark),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Edit Profile", color = BgDark, fontWeight = FontWeight.Bold)
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
        OutlinedTextField(
            value = name, onValueChange = onNameChange,
            label = { Text("Spiritual Name", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldSpark,
                unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = dob, onValueChange = onDobChange,
            label = { Text("DOB (YYYY-MM-DD)", color = TextMuted) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GoldSpark,
                unfocusedBorderColor = Border,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                border = androidx.compose.foundation.BorderStroke(1.dp, GoldSpark),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldSpark)
            ) { Text("Cancel") }
            Button(
                onClick = onSave,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = GoldSpark)
            ) { Text("Save", color = BgDark, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary, fontWeight = FontWeight.SemiBold))
    }
}
