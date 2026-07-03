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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
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
    val profileViewModel: ProfileViewModel = hiltViewModel()
    val stats by profileViewModel.stats.collectAsState()
    val authPrefs = remember { AuthPreferences.getInstance(navController.context) }
    // User is guest if: explicitly guest OR not logged in with real credentials
    // Use a key that changes when auth state changes so the UI recomposes
    var authVersion by remember { mutableStateOf(0) }
    val isGuest = remember(authVersion) { authPrefs.isGuestUser }
    var showMoreSheet by remember { mutableStateOf(false) }

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
                        navController.navigate("profile?tab=0")
                    },
                    onNavigateToYogaLevels = {
                        scope.launch { drawerState.close() }
                        navController.navigate("profile?tab=2")
                    },
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
                    onNavigateToNotes = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Notes.route)
                    },
                    onNavigateToMeditation = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Meditation.route)
                    },
                    onNavigateToStudyPlan = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Recommendations.route)
                    },
                    onNavigateToQuizBattle = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.QuizBattle.route)
                    },
                    onNavigateToCoinHistory = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.CoinHistory.route)
                    },
                    onNavigateToLogin = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Login.route)
                    },

                    onNavigateToFlashcards = {
                        scope.launch { drawerState.close() }
                        navController.navigate("flashcards?topic=")
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
                    actions = {
                        IconButton(onClick = { navController.navigate(Screen.GitaSearch.route) }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "Search",
                                tint = GoldSpark
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
                Box(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp)) {
                    BottomNavigationBar(
                        currentRoute = currentRoute,
                        isDarkTheme = isDarkTheme,
                        onNavigate = { route ->
                            if (route == "more") {
                                showMoreSheet = true
                            } else if (currentRoute != route) {
                                navController.navigate(route) {
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
            }
        ) { innerPadding ->
            if (showMoreSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showMoreSheet = false },
                    containerColor = MaterialTheme.colorScheme.surface,
                    dragHandle = { BottomSheetDefaults.DragHandle(color = GoldSpark.copy(alpha = 0.5f)) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
                    ) {
                        Text(
                            text = "More Options",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        val moreItems = listOf(
                            Triple(Icons.Filled.EditNote, "My Notes", Screen.Notes.route),
                            Triple(Icons.Filled.SelfImprovement, "Meditation", Screen.Meditation.route),
                            Triple(Icons.Filled.SportsMma, "Quiz Battle", Screen.QuizBattle.route),
                            Triple(Icons.Filled.CalendarMonth, "Study Plans", Screen.Recommendations.route),
                            Triple(Icons.Filled.Favorite, "Favorites", Screen.Favorites.route),
                            Triple(Icons.Filled.Person, "Profile", Screen.Profile.route)
                        )
                        
                        moreItems.forEach { (icon, title, route) ->
                            ListItem(
                                headlineContent = { Text(title) },
                                leadingContent = { Icon(icon, contentDescription = null, tint = GoldSpark) },
                                modifier = Modifier.clickable {
                                    showMoreSheet = false
                                    navController.navigate(route)
                                }
                            )
                        }
                    }
                }
            }
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onAuthChanged = { authVersion++ },
                sharedProfileViewModel = profileViewModel
            )
        }
    }
}

