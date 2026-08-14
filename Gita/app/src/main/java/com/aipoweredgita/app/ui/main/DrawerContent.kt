package com.aipoweredgita.app.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.utils.AuthPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerContent(
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRead: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToQuizStats: () -> Unit,
    onNavigateToOfflineDownload: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToYogaLevels: () -> Unit,
    onNavigateToCoinHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToNotes: () -> Unit = {},
    onNavigateToMeditation: () -> Unit = {},
    onNavigateToFeedback: () -> Unit = {},
    onNavigateToStudyPlan: () -> Unit = {},
    onNavigateToQuizBattle: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {},
    onNavigateToFlashcards: () -> Unit = {},
    onNavigateToVoiceStudio: () -> Unit = {},
    onLogout: () -> Unit = {},
    stats: com.aipoweredgita.app.database.UserStats?,
    coinBalance: Int = 0,
    isGuest: Boolean = false
) {
    // Dynamic Theme Colors
    val bgColor = if (isDarkTheme) Color(0xFF000000) else Color(0xFFFFFFFF)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val subtextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
    val dividerColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val avatarBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Twitter/X Style Profile Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp)
        ) {
            // Profile Avatar Circle
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(avatarBg)
                    .border(1.5.dp, Color(0xFFF59E0B), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🕉️",
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Display Name
            val context = LocalContext.current
            val authPrefs = remember { AuthPreferences.getInstance(context) }
            val displayName = run {
                val uid = authPrefs.userId
                android.util.Log.d("DrawerContent", "authPrefs.userId=$uid")
                if (uid != null && uid.startsWith("guest_")) {
                    // Guest user — show unique ID
                    val shortId = uid.removePrefix("guest_").take(6).uppercase()
                    android.util.Log.d("DrawerContent", "Guest uid=$uid, shortId=$shortId")
                    "Guest $shortId"
                } else {
                    // Regular signed-in user
                    stats?.userName?.takeIf { it.isNotEmpty() } ?: "Gita Seeker"
                }
            }
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = textColor,
                fontSize = 20.sp
            )

            // Handle / Username - show @guest_uniqueid for guests
            val handleText = run {
                val uid = authPrefs.userId
                if (uid != null && uid.startsWith("guest_")) {
                    val shortId = uid.removePrefix("guest_").take(6).uppercase()
                    "@guest_$shortId"
                } else {
                    "@${(stats?.userName ?: "seeker").lowercase().replace(" ", "")}"
                }
            }
            Text(
                text = handleText,
                style = MaterialTheme.typography.bodyMedium,
                color = subtextColor,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Twitter-style Following / Followers / Stats Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stats?.totalQuizzesTaken ?: 0} ",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Quizzes",
                        color = subtextColor,
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${stats?.currentStreak ?: 0}🔥 ",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Streak",
                        color = subtextColor,
                        fontSize = 14.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$coinBalance ",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Coins",
                        color = subtextColor,
                        fontSize = 14.sp
                    )
                }
            }
        }

        HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

        // Scrollable Menu List (Feedback & Complaints kept down at bottom)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TwitterMenuItem(
                icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Profile") },
                title = "Profile",
                textColor = textColor,
                onClick = onNavigateToProfile
            )

            TwitterMenuItem(
                icon = { Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read Verses") },
                title = "Read Verses",
                textColor = textColor,
                onClick = onNavigateToRead
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.School, contentDescription = "Quiz") },
                title = "Quiz",
                textColor = textColor,
                onClick = onNavigateToQuiz
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.Mic, contentDescription = "Voice Chat") },
                title = "Voice Chat",
                textColor = textColor,
                onClick = onNavigateToVoiceStudio
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.SelfImprovement, contentDescription = "Meditation") },
                title = "Meditation",
                textColor = textColor,
                onClick = onNavigateToMeditation
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.EditNote, contentDescription = "My Notes") },
                title = "My Notes",
                textColor = textColor,
                onClick = onNavigateToNotes
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.FavoriteBorder, contentDescription = "Favorites") },
                title = "Favorites",
                textColor = textColor,
                onClick = onNavigateToFavorites
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.Leaderboard, contentDescription = "Activity History") },
                title = "Activity History",
                textColor = textColor,
                onClick = onNavigateToQuizStats
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Yoga Levels") },
                title = "Yoga Levels",
                textColor = textColor,
                onClick = onNavigateToYogaLevels
            )

            TwitterMenuItem(
                icon = { Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { Text("🪙", fontSize = 18.sp) } },
                title = "Coin History",
                textColor = textColor,
                onClick = onNavigateToCoinHistory
            )

            HorizontalDivider(color = dividerColor, thickness = 0.8.dp, modifier = Modifier.padding(vertical = 8.dp))

            // Bottom Settings & Help Section
            TwitterMenuItem(
                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings and privacy") },
                title = "Settings and privacy",
                textColor = textColor,
                onClick = onNavigateToSettings
            )

            TwitterMenuItem(
                icon = { Icon(Icons.Default.HelpOutline, contentDescription = "Help Centre") },
                title = "Help Centre",
                textColor = textColor,
                onClick = onNavigateToFeedback
            )
        }

        HorizontalDivider(color = dividerColor, thickness = 0.8.dp)

        // Bottom Footer Bar (Dark mode moon button & Logout)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Dark Mode Moon Icon (Twitter-style bottom-left toggle)
            IconButton(
                onClick = { onThemeToggle(!isDarkTheme) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    contentDescription = "Theme Toggle",
                    tint = textColor
                )
            }

            // Sign In / Logout Button
            if (isGuest) {
                TextButton(onClick = onNavigateToLogin) {
                    Text("Sign In", color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            } else {
                IconButton(onClick = onLogout) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", tint = Color(0xFFEF4444))
                }
            }
        }
    }
}

@Composable
private fun TwitterMenuItem(
    icon: @Composable (() -> Unit),
    title: String,
    textColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(LocalContentColor provides textColor) {
                icon()
            }
        }

        Spacer(modifier = Modifier.width(22.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
