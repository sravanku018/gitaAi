package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.aipoweredgita.app.R
import com.aipoweredgita.app.navigation.Screen
import com.aipoweredgita.app.navigation.NavGraph
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavHostController,
    isDarkTheme: Boolean,
    onThemeToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val profileViewModel: ProfileViewModel = viewModel()
    val stats by profileViewModel.stats.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerContent(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onNavigateToHome = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Home.route)
                    },
                    onNavigateToRead = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.NormalMode.route)
                    },
                    onNavigateToQuiz = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.QuizSection.route)
                    },
                    onNavigateToFavorites = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Favorites.route)
                    },
                    onNavigateToQuizStats = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.QuizStats.route)
                    },
                    onNavigateToOfflineDownload = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.OfflineDownload.route)
                    },
                    onNavigateToProfile = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    stats = stats
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🕉️",
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Bhagavad Gita",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Filled.Menu,
                                contentDescription = "Menu",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                        navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                        actionIconContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    modifier = Modifier.height(64.dp)
                )
            },
            bottomBar = {
                BottomNavigationBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        // Only navigate if not already on that route
                        if (currentRoute != route) {
                            navController.navigate(route) {
                                // Pop up to the start destination
                                popUpTo(Screen.Home.route) {
                                    saveState = false
                                }
                                launchSingleTop = true
                                restoreState = false
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceVariant
    ) {
        NavigationBarItem(
            selected = currentRoute == Screen.Home.route,
            onClick = { onNavigate(Screen.Home.route) },
            icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.NormalMode.route,
            onClick = { onNavigate(Screen.NormalMode.route) },
            icon = { Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read") },
            label = { Text("Read") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.QuizSection.route || currentRoute == Screen.QuizConfig.route || currentRoute == Screen.QuizMode.route,
            onClick = { onNavigate(Screen.QuizSection.route) },
            icon = { Icon(imageVector = Icons.Filled.School, contentDescription = "Quiz") },
            label = { Text("Quiz") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.VoiceStudio.route,
            onClick = { onNavigate(Screen.VoiceStudio.route) },
            icon = { Icon(imageVector = Icons.Filled.Mic, contentDescription = "Voice") },
            label = { Text("Voice") }
        )
        NavigationBarItem(
            selected = currentRoute == Screen.Profile.route,
            onClick = { onNavigate(Screen.Profile.route) },
            icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile") },
            label = { Text("Profile") }
        )
    }
}

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
    onNavigateToSettings: () -> Unit,
    stats: com.aipoweredgita.app.database.UserStats?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Profile Section at Top (Twitter-style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(20.dp)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🕉️",
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Name
            Text(
                text = "Gita Student",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${stats?.totalQuizzesTaken ?: 0} Quizzes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${stats?.currentStreak ?: 0}🔥 Streak",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Navigation Items (Twitter-style list)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
                title = "Home",
                onClick = onNavigateToHome
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read Verses") },
                title = "Read Verses",
                onClick = onNavigateToRead
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.School, contentDescription = "Quiz") },
                title = "Quiz",
                onClick = onNavigateToQuiz
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorites") },
                title = "Favorites",
                onClick = onNavigateToFavorites
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = "Statistics") },
                title = "Statistics",
                onClick = onNavigateToQuizStats
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = "Offline Mode") },
                title = "Offline Mode",
                onClick = onNavigateToOfflineDownload
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile") },
                title = "Profile",
                onClick = onNavigateToProfile
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Settings section
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Menu, contentDescription = "Appearance & Theme") },
                title = "Appearance & Theme",
                trailing = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeToggle
                    )
                },
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
fun QuickStatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TwitterMenuItem(
    icon: @Composable (() -> Unit),
    title: String,
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
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun DrawerMenuItem(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
