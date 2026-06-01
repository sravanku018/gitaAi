package com.aipoweredgita.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
<<<<<<< HEAD
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
import androidx.compose.ui.draw.scale
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import androidx.compose.foundation.border
import androidx.compose.runtime.CompositionLocalProvider
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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

<<<<<<< HEAD
    // Fetch coin balance from API when userId becomes available
    LaunchedEffect(stats) {
        if (stats?.userId?.isNotEmpty() == true) {
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
=======
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                        navController.navigate(Screen.ActivityHistory.route)
=======
                        navController.navigate(Screen.QuizStats.route)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    },
                    onNavigateToOfflineDownload = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.OfflineDownload.route)
                    },
                    onNavigateToProfile = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Profile.route)
                    },
<<<<<<< HEAD
                    onNavigateToYogaLevels = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Awakening.route)
                    },
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    onNavigateToSettings = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.Settings.route)
                    },
<<<<<<< HEAD
                    onNavigateToCoinHistory = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Screen.CoinHistory.route)
                    },
                    stats = stats,
                    coinBalance = profileViewModel.coinBalance.collectAsState().value
=======
                    stats = stats
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                                tint = GoldSpark
=======
                                tint = MaterialTheme.colorScheme.onSurface
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
<<<<<<< HEAD
                        containerColor = MaterialTheme.colorScheme.surface,
=======
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                    isDarkTheme = isDarkTheme,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
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
=======
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
            selected = currentRoute == Screen.ChapterSelection.route || currentRoute?.startsWith("normal_mode") == true,
            onClick = { onNavigate(Screen.ChapterSelection.route) },
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
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    onNavigateToYogaLevels: () -> Unit,
    onNavigateToCoinHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    stats: com.aipoweredgita.app.database.UserStats?,
    coinBalance: Int = 0
=======
    onNavigateToSettings: () -> Unit,
    stats: com.aipoweredgita.app.database.UserStats?
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
<<<<<<< HEAD
            .background(MaterialTheme.colorScheme.background)
=======
            .background(MaterialTheme.colorScheme.surface)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    ) {
        // Profile Section at Top (Twitter-style)
        Column(
            modifier = Modifier
                .fillMaxWidth()
<<<<<<< HEAD
                .background(Saffron.copy(alpha = 0.05f))
                .border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
=======
                .background(MaterialTheme.colorScheme.primaryContainer)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                .padding(20.dp)
        ) {
            // Profile Avatar
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(CircleShape)
<<<<<<< HEAD
                    .background(Color(0xFFFF6400).copy(alpha = 0.15f))
                    .border(1.dp, GoldSpark.copy(alpha = 0.25f), CircleShape),
=======
                    .background(MaterialTheme.colorScheme.primary),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
=======
                fontWeight = FontWeight.Bold
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stats row
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "${stats?.totalQuizzesTaken ?: 0} Quizzes",
                    style = MaterialTheme.typography.bodySmall,
<<<<<<< HEAD
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
=======
                    color = MaterialTheme.colorScheme.onSurfaceVariant
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
                Text(
                    text = "${stats?.currentStreak ?: 0}🔥 Streak",
                    style = MaterialTheme.typography.bodySmall,
<<<<<<< HEAD
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
=======
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            }
        }

        // Navigation Items (Twitter-style list)
        Column(
            modifier = Modifier
                .weight(1f)
<<<<<<< HEAD
                .background(MaterialTheme.colorScheme.background)
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                .verticalScroll(rememberScrollState())
        ) {
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Home, contentDescription = "Home") },
                title = "Home",
<<<<<<< HEAD
                isDarkTheme = isDarkTheme,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                onClick = onNavigateToHome
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = "Read Verses") },
                title = "Read Verses",
<<<<<<< HEAD
                isDarkTheme = isDarkTheme,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                onClick = onNavigateToRead
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.School, contentDescription = "Quiz") },
                title = "Quiz",
<<<<<<< HEAD
                isDarkTheme = isDarkTheme,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                onClick = onNavigateToQuiz
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Favorite, contentDescription = "Favorites") },
                title = "Favorites",
<<<<<<< HEAD
                isDarkTheme = isDarkTheme,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                onClick = onNavigateToFavorites
            )

            TwitterMenuItem(
<<<<<<< HEAD
                icon = { Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = "Activity History") },
                title = "Activity History",
                isDarkTheme = isDarkTheme,
=======
                icon = { Icon(imageVector = Icons.Filled.Leaderboard, contentDescription = "Statistics") },
                title = "Statistics",
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                onClick = onNavigateToQuizStats
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.CloudDownload, contentDescription = "Offline Mode") },
                title = "Offline Mode",
<<<<<<< HEAD
                isDarkTheme = isDarkTheme,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                onClick = onNavigateToOfflineDownload
            )

            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Person, contentDescription = "Profile") },
                title = "Profile",
<<<<<<< HEAD
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
=======
                onClick = onNavigateToProfile
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

            // Settings section
            TwitterMenuItem(
                icon = { Icon(imageVector = Icons.Filled.Menu, contentDescription = "Appearance & Theme") },
                title = "Appearance & Theme",
<<<<<<< HEAD
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
=======
                trailing = {
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = onThemeToggle
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    isDarkTheme: Boolean,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            CompositionLocalProvider(
                LocalContentColor provides GoldSpark
            ) {
                icon()
            }
        }
=======
        Box(modifier = Modifier.size(28.dp), contentAlignment = Alignment.Center) { icon() }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

        Spacer(modifier = Modifier.width(20.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
<<<<<<< HEAD
            color = MaterialTheme.colorScheme.onSurface,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
