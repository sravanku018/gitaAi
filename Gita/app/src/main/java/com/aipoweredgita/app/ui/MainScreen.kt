package com.aipoweredgita.app.ui

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
import androidx.lifecycle.viewmodel.compose.viewModel
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
    val authPrefs = remember { AuthPreferences.getInstance(navController.context) }
    // User is guest if: explicitly guest OR not logged in with real credentials
    // Use a key that changes when auth state changes so the UI recomposes
    var authVersion by remember { mutableStateOf(0) }
    val isGuest = remember(authVersion) { authPrefs.isGuestUser }

    // Fetch coin balance from API when userId becomes available
    LaunchedEffect(stats) {
        if (stats?.userId?.isNotEmpty() == true) {
            profileViewModel.refreshCoinBalance()
        }
    }

    // Refresh coin balance after login/register (authVersion changes)
    LaunchedEffect(authVersion) {
        if (authVersion > 0) {
            profileViewModel.refreshCoinBalance()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface
            ) {
                DrawerContent(
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onNavigateToHome = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Home.route)
                    },
                    onNavigateToRead = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.ChapterSelection.route)
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
                        navController.navigate(Screen.ActivityHistory.route)
                    },
                    onNavigateToOfflineDownload = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.OfflineDownload.route)
                    },
                    onNavigateToProfile = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    },
                    onNavigateToYogaLevels = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Awakening.route)
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToCoinHistory = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.CoinHistory.route)
                    },
                    onNavigateToLogin = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Login.route)
                    },
                    onLogout = {
                        scope.launch {
                            drawerState.close()
                            val authManager = com.aipoweredgita.app.repository.AuthManager.getInstance(navController.context)
                            authManager.logout()
                            
                            // Reset Room DB user stats
                            val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(navController.context)
                            db.userStatsDao().updateUserId("")
                            db.userStatsDao().updateProfile("", "")
                            
                            authVersion++
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        }
                    },
                    stats = stats,
                    coinBalance = profileViewModel.coinBalance.collectAsState().value,
                    isGuest = isGuest
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
                                tint = GoldSpark
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
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
                    isDarkTheme = isDarkTheme,
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
                onThemeToggle = onThemeToggle,
                onAuthChanged = { authVersion++ }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentRoute: String?,
    isDarkTheme: Boolean,
    onNavigate: (String) -> Unit
) {
    val springSpec = spring<Float>(dampingRatio = 0.6f, stiffness = 500f)
    val springColor = spring<Color>(dampingRatio = 0.7f, stiffness = 400f)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.extraLarge),
        tonalElevation = 4.dp
    ) {
        val navItems = listOf(
            Screen.Home.route to Icons.Filled.Home to "Home",
            Screen.ChapterSelection.route to Icons.AutoMirrored.Filled.MenuBook to "Read",
            Screen.QuizSection.route to Icons.Filled.School to "Quiz",
            Screen.VoiceStudio.route to Icons.Filled.Mic to "Voice",
            Screen.Profile.route to Icons.Filled.Person to "Profile"
        )
        
        navItems.forEach { (routeAndIcon, label) ->
            val route = routeAndIcon.first
            val icon = routeAndIcon.second
            val isSelected = when (route) {
                Screen.Home.route -> currentRoute == Screen.Home.route
                Screen.ChapterSelection.route -> currentRoute == Screen.ChapterSelection.route || currentRoute?.startsWith("normal_mode") == true
                Screen.QuizSection.route -> currentRoute == Screen.QuizSection.route || currentRoute == Screen.QuizConfig.route || currentRoute == Screen.QuizMode.route
                Screen.VoiceStudio.route -> currentRoute == Screen.VoiceStudio.route
                Screen.Profile.route -> currentRoute == Screen.Profile.route
                else -> false
            }

            val iconScale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.9f,
                animationSpec = springSpec,
                label = "nav_scale_$label"
            )
            val iconColor by animateColorAsState(
                targetValue = if (isSelected) GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = springColor,
                label = "nav_color_$label"
            )
            val textColor by animateColorAsState(
                targetValue = if (isSelected) GoldSpark else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                animationSpec = springColor,
                label = "nav_text_$label"
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = { onNavigate(route) },
                icon = {
                    Box(modifier = Modifier.scale(iconScale)) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = iconColor
                        )
                    }
                },
                label = {
                    Text(
                        label,
                        color = textColor,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                )
            )
        }
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
    onNavigateToYogaLevels: () -> Unit,
    onNavigateToCoinHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToLogin: () -> Unit = {},
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
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isGuest) "Guest User" else (stats?.userName?.takeIf { it.isNotEmpty() } ?: "Gita Student"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isGuest) {
                    Surface(
                        color = GoldSpark.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldSpark.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Guest",
                            style = MaterialTheme.typography.labelSmall,
                            color = GoldSpark,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Sign In / Logout button dynamically based on auth status
            if (isGuest) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = onNavigateToLogin,
                    color = GoldSpark,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Login,
                            contentDescription = "Sign In",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Sign In",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    onClick = onLogout,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logout",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
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
                    color = GoldSpark.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, GoldSpark.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = "🪙 $coinBalance",
                        style = MaterialTheme.typography.labelSmall,
                        color = GoldSpark,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontWeight = FontWeight.Bold
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
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
                title = "Home",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToHome
            )

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
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorites") },
                title = "Favorites",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToFavorites
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = "Activity History") },
                title = "Activity History",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToQuizStats
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

            TwitterMenuItem(
                icon = { Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) { Text("🪙", fontSize = 22.sp) } },
                title = "Coin History",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToCoinHistory
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.AutoAwesome, contentDescription = "Yoga Levels") },
                title = "Yoga Levels",
                isDarkTheme = isDarkTheme,
                onClick = onNavigateToYogaLevels
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f))

            // Settings section
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Menu, contentDescription = "Appearance & Theme") },
                title = "Appearance & Theme",
                isDarkTheme = isDarkTheme,
                trailing = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GoldSpark,
                            checkedTrackColor = Saffron.copy(alpha = 0.5f),
                            uncheckedThumbColor = Color.Black.copy(alpha = 0.4f),
                            uncheckedTrackColor = Color.Black.copy(alpha = 0.1f)
                        )
                    )
                },
                onClick = onNavigateToSettings
            )
        }
    }
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

