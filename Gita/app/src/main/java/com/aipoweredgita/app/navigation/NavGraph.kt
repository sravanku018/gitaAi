package com.aipoweredgita.app.navigation
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aipoweredgita.app.ui.DashboardScreen

import com.aipoweredgita.app.ui.FavoritesScreen
import com.aipoweredgita.app.ui.OfflineDownloadScreen
import com.aipoweredgita.app.ui.ProfileScreen

import com.aipoweredgita.app.ui.ActivityHistoryScreen
import com.aipoweredgita.app.ui.WidgetSettingsScreen
import com.aipoweredgita.app.ui.BadgesScreen
import com.aipoweredgita.app.ui.RandomSlokaScreen
import com.aipoweredgita.app.ui.AwakeningPage
import com.aipoweredgita.app.ui.SettingsScreen
import com.aipoweredgita.app.ui.LoginScreen
import com.aipoweredgita.app.ui.ProtectedQuizConfigScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.viewmodel.OfflineDownloadViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object NormalMode : Screen("normal_mode?chapter={chapter}&verse={verse}")
    object ChapterSelection : Screen("chapter_selection")
    object QuizConfig : Screen("quiz_config")
    object QuizMode : Screen("quiz_mode")
    object QuizSection : Screen("quiz_section")
    object KrishnaTalks : Screen("krishna_talks")
    object VoiceStudio : Screen("voice_studio")
    object Favorites : Screen("favorites")
    object OfflineDownload : Screen("offline_download")
    object Profile : Screen("profile")
    object QuizStats : Screen("quiz_stats")
    object ActivityHistory : Screen("activity_history")
    object WidgetSettings : Screen("widget_settings")
    object Settings : Screen("settings")
    object Badges : Screen("badges")
    object CoinHistory : Screen("coin_history")
    object Awakening : Screen("awakening")
    object DailyActivity : Screen("daily_activity")
    object Recommendations : Screen("recommendations")
    object Login : Screen("login")
    object Notes : Screen("notes")
    object Meditation : Screen("meditation")
    object StudyPlan : Screen("study_plan")
    object QuizBattle : Screen("quiz_battle")

    object Flashcards : Screen("flashcards?topic={topic}")
    object RandomSloka : Screen("random_sloka?chapter={chapter}&verse={verse}")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {},
    onAuthChanged: () -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(300)
            )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.Start, tween(300)
            )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(300)
            )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutOfContainer(
                AnimatedContentTransitionScope.SlideDirection.End, tween(300)
            )
        }
    ) {
        composable(Screen.Home.route) {
            DashboardScreen(
                onNavigateToNormalMode = { navController.navigate(Screen.ChapterSelection.route) },
                onNavigateToQuizMode = { navController.navigate(Screen.QuizSection.route) },
                onNavigateToVoiceStudio = { navController.navigate(Screen.VoiceStudio.route) },
                onNavigateToRecommendations = { navController.navigate(Screen.Recommendations.route) },
                onNavigateToRandomSloka = { navController.navigate("random_sloka") },
                onNavigateToAwakening = { navController.navigate(Screen.Awakening.route) },
                onNavigateToCoinHistory = { navController.navigate(Screen.CoinHistory.route) }
            )
        }

        composable(
            route = "normal_mode?chapter={chapter}&verse={verse}",
            arguments = listOf(
                androidx.navigation.navArgument("chapter") { type = androidx.navigation.NavType.IntType; defaultValue = 0 },
                androidx.navigation.navArgument("verse") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val chapter = backStackEntry.arguments?.getInt("chapter") ?: 0
            val verse = backStackEntry.arguments?.getInt("verse") ?: 0
            val vm: com.aipoweredgita.app.viewmodel.NormalModeViewModel = hiltViewModel()
            androidx.compose.runtime.LaunchedEffect(chapter, verse) {
                if (chapter > 0 && verse > 0) vm.loadVerse(chapter, verse) else if (chapter > 0) vm.goToChapter(chapter)
            }
            com.aipoweredgita.app.ui.VerseScreen(
                viewModel = vm,
                onReadOfflineClick = { navController.navigate(Screen.OfflineDownload.route) }
            )
        }

        composable(Screen.ChapterSelection.route) {
            com.aipoweredgita.app.ui.ChapterSelectionScreen(
                onChapterSelected = { chapter ->
                    navController.navigate("normal_mode?chapter=${chapter}&verse=1")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.QuizConfig.route) { backStackEntry ->
            val quizViewModel: QuizViewModel = hiltViewModel()
            val quizState by quizViewModel.quizState.collectAsState()
            ProtectedQuizConfigScreen(
                language = quizState.language,
                onStartQuiz = { questionCount, language ->
                    quizViewModel.setQuizLimit(questionCount)
                    quizViewModel.setQuizLanguage(language)
                    navController.navigate(Screen.QuizMode.route)
                },
                onBackClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) {
                            inclusive = false
                        }
                    }
                }
            )
        }

        composable(Screen.QuizMode.route) { backStackEntry ->
            val quizViewModel: QuizViewModel = hiltViewModel()
            com.aipoweredgita.app.ui.QuizScreen(
                onExitQuiz = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                viewModel = quizViewModel
            )
        }

        composable(Screen.QuizSection.route) {
            com.aipoweredgita.app.ui.QuizSectionScreen(
                onExit = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.KrishnaTalks.route) {
            com.aipoweredgita.app.ui.VoiceStudioScreen(
                onExit = { navController.popBackStack() },
                onNavigateToQuiz = {
                    navController.navigate(Screen.QuizSection.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateToRead = {
                    navController.navigate(Screen.ChapterSelection.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.VoiceStudio.route) {
            com.aipoweredgita.app.ui.VoiceStudioScreen(
                onExit = { navController.popBackStack() },
                onNavigateToQuiz = {
                    navController.navigate(Screen.QuizSection.route) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onNavigateToRead = {
                    navController.navigate(Screen.ChapterSelection.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onVerseClick = { chapter, verse ->
                    navController.navigate("normal_mode?chapter=${chapter}&verse=${verse}")
                }
            )
        }

        composable(Screen.OfflineDownload.route) {
            val offlineViewModel: OfflineDownloadViewModel = hiltViewModel()
            OfflineDownloadScreen(viewModel = offlineViewModel)
        }

        composable(Screen.Profile.route) { backStackEntry ->
            val profileViewModel: com.aipoweredgita.app.viewmodel.ProfileViewModel = hiltViewModel()
            ProfileScreen(
                onNavigateToQuizStats = {
                    navController.navigate(Screen.QuizStats.route)
                },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                onNavigateToBadges = { navController.navigate(Screen.Badges.route) },
                onNavigateToYogaLevels = { navController.navigate(Screen.Awakening.route) },
                onNavigateToLogin = { navController.navigate(Screen.Login.route) },
                viewModel = profileViewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                themePreferences = com.aipoweredgita.app.utils.ThemePreferences(LocalContext.current),
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() },
                onAccountDeleted = {
                    onAuthChanged()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Login.route) {
            val context = LocalContext.current
            val coroutineScope = rememberCoroutineScope()
            LoginScreen(
                onLoginSuccess = { userId ->
                    coroutineScope.launch {
                        val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                        val dao = db.userStatsDao()
                        dao.insertIfEmpty(com.aipoweredgita.app.database.UserStats(userId = userId))
                        val repo = com.aipoweredgita.app.repository.StatsRepository(
                            userStatsDao = dao,
                            dailyActivityDao = db.dailyActivityDao(),
                            appContext = context
                        )
                        repo.refreshUserState(userId)
                    }
                    onAuthChanged()
                    navController.popBackStack()
                },
                onGuestLogin = {
                    onAuthChanged()
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Badges.route) {
            BadgesScreen()
        }

        composable(Screen.CoinHistory.route) {
            com.aipoweredgita.app.ui.CoinHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Awakening.route) {
            AwakeningPage()
        }

        composable(Screen.DailyActivity.route) {
            ActivityHistoryScreen(initialTab = 2)
        }

        composable(Screen.QuizStats.route) {
            ActivityHistoryScreen(initialTab = 1)
        }

        composable(Screen.ActivityHistory.route) {
            ActivityHistoryScreen()
        }

        composable(Screen.WidgetSettings.route) {
            WidgetSettingsScreen()
        }

        composable(Screen.Recommendations.route) {
            com.aipoweredgita.app.ui.RecommendationsScreen(
                onOpenChapter = { chapter -> navController.navigate("normal_mode?chapter=${chapter}&verse=1") },
                onStartTopicQuiz = { navController.navigate(Screen.QuizConfig.route) },
                onOpenFlashcards = { topic -> navController.navigate("flashcards?topic=${topic ?: ""}") },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.Flashcards.route,
            arguments = listOf(
                androidx.navigation.navArgument("topic") { type = androidx.navigation.NavType.StringType; defaultValue = "" }
            )
        ) { backStackEntry ->
            val topic = backStackEntry.arguments?.getString("topic") ?: ""
            com.aipoweredgita.app.ui.FlashcardsScreen(topic = topic, onBack = { navController.popBackStack() })
        }

        composable(
            route = "random_sloka?chapter={chapter}&verse={verse}",
            arguments = listOf(
                androidx.navigation.navArgument("chapter") { type = androidx.navigation.NavType.IntType; defaultValue = 0 },
                androidx.navigation.navArgument("verse") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val initChapter = backStackEntry.arguments?.getInt("chapter") ?: 0
            val initVerse = backStackEntry.arguments?.getInt("verse") ?: 0
            RandomSlokaScreen(
                onBack = { navController.popBackStack() },
                initialChapter = initChapter,
                initialVerse = initVerse
            )
        }

        composable(Screen.Notes.route) {
            com.aipoweredgita.app.ui.NotesScreen(
                onBack = { navController.popBackStack() },
                onVerseClick = { chapter, verse -> navController.navigate("normal_mode?chapter=$chapter&verse=$verse") }
            )
        }

        composable(Screen.Meditation.route) {
            com.aipoweredgita.app.ui.MeditationTimerScreen(
                onBack = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.StudyPlan.route) {
            com.aipoweredgita.app.ui.StudyPlanScreen(
                onBack = { navController.popBackStack() },
                onStartReading = { chapter, verse -> navController.navigate("normal_mode?chapter=$chapter&verse=$verse") }
            )
        }

        composable(Screen.QuizBattle.route) {
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            com.aipoweredgita.app.ui.QuizBattleScreen(
                onBack = { navController.popBackStack() },
                onGameOver = { score, maxCombo, questionsAnswered, battleCoins ->
                    if (battleCoins > 0) {
                        scope.launch {
                            try {
                                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                                val repo = com.aipoweredgita.app.repository.StatsRepository(
                                    userStatsDao = db.userStatsDao(),
                                    dailyActivityDao = db.dailyActivityDao(),
                                    appContext = context
                                )
                                repo.trackBattleCompletion(battleCoins, score, questionsAnswered)
                            } catch (_: Exception) {}
                        }
                    }
                    navController.popBackStack()
                }
            )
        }
    }
}
