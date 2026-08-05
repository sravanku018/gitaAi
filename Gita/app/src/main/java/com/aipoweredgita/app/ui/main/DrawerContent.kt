package com.aipoweredgita.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsMma
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.aipoweredgita.app.ui.theme.GitaLearningTheme
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aipoweredgita.app.R
import com.aipoweredgita.app.navigation.Screen
import com.aipoweredgita.app.navigation.NavGraph
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.utils.AuthPreferences
import androidx.compose.foundation.border
import androidx.compose.runtime.CompositionLocalProvider

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
    onLogout: () -> Unit = {},
    stats: com.aipoweredgita.app.database.UserStats?,
    coinBalance: Int = 0,
    isGuest: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Profile Section at Top (Twitter-style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Saffron.copy(alpha = 0.05f))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
                .padding(20.dp)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF6400).copy(alpha = 0.15f))
                    .border(1.dp, GoldSpark.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🕉️",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name + Guest badge row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isGuest) "Guest User" else (stats?.userName?.takeIf { it.isNotEmpty() } ?: "Gita Student"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (isGuest) {
                    Surface(
                        color = (if (isDarkTheme) GoldSpark else Saffron).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, (if (isDarkTheme) GoldSpark else Saffron).copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Guest",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isDarkTheme) GoldSpark else Saffron,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }



            Spacer(modifier = Modifier.height(4.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${stats?.totalQuizzesTaken ?: 0} Quizzes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "${stats?.currentStreak ?: 0}🔥 Streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Surface(
                    color = GoldSpark.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldSpark.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "🪙 $coinBalance",
                        style = MaterialTheme.typography.titleSmall,
                        color = GoldSpark,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Navigation Items (Twitter-style list)
        Column(
            modifier = Modifier
                .weight(1f)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
        ) {
            DrawerSectionHeader("LEARN")
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read Verses") },
                title = "Read Verses",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToRead
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.School, contentDescription = "Quiz") },
                title = "Quiz",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToQuiz
            )
            TwitterMenuItem(
                icon = { Icon(Icons.Filled.SportsMma, "Battle") },
                title = "Quiz Battle",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToQuizBattle
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Style, contentDescription = "Flashcards") },
                title = "Flashcards",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToFlashcards,
                trailing = {
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Soon",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )

            DrawerSectionHeader("REFLECT")
            TwitterMenuItem(
                icon = { Icon(Icons.Filled.SelfImprovement, "Meditation") },
                title = "Meditation",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToMeditation
            )
            TwitterMenuItem(
                icon = { Icon(Icons.Filled.EditNote, "Notes") },
                title = "My Notes",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToNotes
            )
            TwitterMenuItem(
                icon = { Icon(Icons.Filled.CalendarMonth, "Plans") },
                title = "Study Plans",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToStudyPlan
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorites") },
                title = "Favorites",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToFavorites
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Lightbulb, contentDescription = "Seeker Ideas & Feedback") },
                title = "Seeker Ideas & Feedback",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToFeedback
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Feedback, contentDescription = "Support & Complaints") },
                title = "Support & Complaints",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToFeedback
            )

            DrawerSectionHeader("PROGRESS")
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = "Activity History") },
                title = "Activity History",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToQuizStats
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = "Yoga Levels") },
                title = "Yoga Levels",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToYogaLevels
            )
            TwitterMenuItem(
                icon = { Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Text("🪙", fontSize = 22.sp) } },
                title = "Coin History",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToCoinHistory
            )


            DrawerSectionHeader("APP")
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings") },
                title = "Settings",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToSettings
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = "Offline Mode") },
                title = "Offline Mode",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToOfflineDownload
            )
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile") },
                title = "Profile",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToProfile
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

            // Appearance Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    contentDescription = null,
                    tint = if (isDarkTheme) GoldSpark else Saffron,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(20.dp))
                Text(
                    text = "Dark Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = onThemeToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GoldSpark,
                        checkedTrackColor = Saffron.copy(alpha = 0.5f)
                    )
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

            // Logout / Sign In Button at Bottom
            if (isGuest) {
                Surface(
                    onClick = onNavigateToLogin,
                    color = if (isDarkTheme) GoldSpark else Saffron,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Login,
                            contentDescription = "Sign In",
                            tint = if (isDarkTheme) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkTheme) Color.Black else Color.White
                        )
                    }
                }
            } else {
                Surface(
                    onClick = onLogout,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun DrawerSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = GoldSpark.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun TwitterMenuItem(
    icon: @Composable (() -> Unit),
    title: String,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(
                LocalContentColor provides GoldSpark
            ) {
                icon()
            }
        }

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            trailing()
        }
    }
}

@Preview(showBackground = true, name = "Drawer Content - Light Mode")
@Composable
fun DrawerContentPreviewLight() {
    GitaLearningTheme(darkTheme = false) {
        DrawerContent(
            isDarkTheme = false,
            onThemeToggle = {},
            onNavigateToHome = {},
            onNavigateToRead = {},
            onNavigateToQuiz = {},
            onNavigateToFavorites = {},
            onNavigateToQuizStats = {},
            onNavigateToOfflineDownload = {},
            onNavigateToProfile = {},
            onNavigateToYogaLevels = {},
            onNavigateToCoinHistory = {},
            onNavigateToSettings = {},
            stats = com.aipoweredgita.app.database.UserStats(
                userId = "123",
                userName = "Arjuna Kumar",
                totalQuizzesTaken = 14,
                currentStreak = 5,
                krishnaCoins = 120
            ),
            coinBalance = 120,
            isGuest = false
        )
    }
}

@Preview(showBackground = true, name = "Drawer Content - Dark Mode", backgroundColor = 0xFF0F0F0F)
@Composable
fun DrawerContentPreviewDark() {
    GitaLearningTheme(darkTheme = true) {
        DrawerContent(
            isDarkTheme = true,
            onThemeToggle = {},
            onNavigateToHome = {},
            onNavigateToRead = {},
            onNavigateToQuiz = {},
            onNavigateToFavorites = {},
            onNavigateToQuizStats = {},
            onNavigateToOfflineDownload = {},
            onNavigateToProfile = {},
            onNavigateToYogaLevels = {},
            onNavigateToCoinHistory = {},
            onNavigateToSettings = {},
            stats = com.aipoweredgita.app.database.UserStats(
                userId = "123",
                userName = "Arjuna Kumar",
                totalQuizzesTaken = 14,
                currentStreak = 5,
                krishnaCoins = 120
            ),
            coinBalance = 120,
            isGuest = false
        )
    }
}


