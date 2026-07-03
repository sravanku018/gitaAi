package com.aipoweredgita.app.navigation
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.aipoweredgita.app.ui.screens.study.NormalModeScreen
import androidx.navigation.compose.composable
import com.aipoweredgita.app.ui.screens.home.DashboardScreen
import com.aipoweredgita.app.ui.screens.voice.VoiceStudioScreen

import com.aipoweredgita.app.ui.screens.study.FavoritesScreen
import com.aipoweredgita.app.ui.OfflineDownloadScreen
import com.aipoweredgita.app.ui.screens.profile.ProfileScreen

import com.aipoweredgita.app.ui.screens.history.ActivityHistoryScreen
import com.aipoweredgita.app.ui.screens.explore.RandomSlokaScreen
import com.aipoweredgita.app.ui.screens.settings.SettingsScreen
import com.aipoweredgita.app.ui.login.LoginScreen
import com.aipoweredgita.app.ui.ProtectedQuizConfigScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.viewmodel.OfflineDownloadViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object NormalMode : Screen("normal_mode?chapter={chapter}&verse={verse}")
    object ChapterSelection : Screen("chapter_selection")
    object QuizConfig : Screen("quiz_config")
    object QuizMode : Screen("quiz_mode")
    object QuizSection : Screen("quiz_section")
    object VoiceStudio : Screen("voice_studio")
    object Favorites : Screen("favorites")
    object OfflineDownload : Screen("offline_download")
    object Profile : Screen("profile")
    object ActivityHistory : Screen("activity_history")
    object Settings : Screen("settings")
    object CoinHistory : Screen("coin_history")
    object Recommendations : Screen("recommendations?tab={tab}")
    object Login : Screen("login")
    object GitaSearch : Screen("gita_search")
    object Notes : Screen("notes")
    object Meditation : Screen("meditation")
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
    onAuthChanged: () -> Unit = {},
    sharedProfileViewModel: com.aipoweredgita.app.viewmodel.ProfileViewModel? = null
) {
    val context = LocalContext.current
    val authPrefs = remember { com.aipoweredgita.app.utils.AuthPreferences.getInstance(context) }
    val startDest = if (authPrefs.onboardingCompleted) Screen.Home.route else Screen.Onboarding.route

    NavHost(
        navController = navController,
        startDestination = startDest,
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
        composable(Screen.Onboarding.route) {
            com.aipoweredgita.app.ui.OnboardingScreen(
                onFinished = {
                    authPrefs.onboardingCompleted = true
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) {
            val vm = sharedProfileViewModel ?: hiltViewModel()
            DashboardScreen(
                onNavigateToNormalMode = { navController.navigate(Screen.ChapterSelection.route) },
                onNavigateToQuizMode = { navController.navigate(Screen.QuizSection.route) },
                onNavigateToVoiceStudio = { navController.navigate(Screen.VoiceStudio.route) },
                onNavigateToRecommendations = { tab -> navController.navigate("recommendations?tab=$tab") },
                onNavigateToRandomSloka = { navController.navigate("random_sloka") },
                onNavigateToAwakening = { navController.navigate("profile?tab=2") },
                onNavigateToCoinHistory = { navController.navigate(Screen.CoinHistory.route) },
                onNavigateToActivityHistory = { navController.navigate(Screen.ActivityHistory.route) },
                viewModel = vm
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
            NormalModeScreen(
                viewModel = vm,
                onReadOfflineClick = { navController.navigate(Screen.OfflineDownload.route) },
                onNavigateToQuizBattle = { navController.navigate(Screen.QuizBattle.route) }
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
            com.aipoweredgita.app.ui.screens.quiz.QuizScreen(
                onExitQuiz = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                viewModel = quizViewModel
            )
        }

        composable(Screen.QuizSection.route) {
            com.aipoweredgita.app.ui.screens.quiz.QuizSectionScreen(
                onExit = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.VoiceStudio.route) {
            VoiceStudioScreen(
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
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
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

        composable(
            route = "profile?tab={tab}",
            arguments = listOf(
                androidx.navigation.navArgument("tab") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
            val profileViewModel = sharedProfileViewModel ?: hiltViewModel()
            ProfileScreen(
                initialTab = tab,
                onNavigateToQuizStats = {
                    navController.navigate(Screen.ActivityHistory.route)
                },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
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
            LoginScreen(
                onLoginSuccess = { userId ->
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

        composable(Screen.GitaSearch.route) {
            com.aipoweredgita.app.ui.screens.explore.GitaSearchScreen(
                onBack = { navController.popBackStack() },
                onVerseClick = { chapter, verse -> navController.navigate("normal_mode?chapter=$chapter&verse=$verse") }
            )
        }

        composable(Screen.ActivityHistory.route) {
            ActivityHistoryScreen()
        }

        composable(Screen.CoinHistory.route) {
            com.aipoweredgita.app.ui.screens.coinhistory.CoinHistoryScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "recommendations?tab={tab}",
            arguments = listOf(
                androidx.navigation.navArgument("tab") { type = androidx.navigation.NavType.IntType; defaultValue = 0 }
            )
        ) { backStackEntry ->
            val tab = backStackEntry.arguments?.getInt("tab") ?: 0
            com.aipoweredgita.app.ui.screens.explore.RecommendationsScreen(
                initialTab = tab,
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
        ) {
            com.aipoweredgita.app.ui.screens.study.FlashcardsScreen(onBack = { navController.popBackStack() })
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
                onComplete = { minutes ->
                    kotlinx.coroutines.MainScope().launch {
                        val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                            navController.context.applicationContext,
                            com.aipoweredgita.app.services.SyncWorkerEntryPoint::class.java
                        )
                        val coins = minutes * 2
                        entryPoint.statsRepository().claimDailyReward(coins, "Meditation reward")
                    }
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.QuizBattle.route) {
            com.aipoweredgita.app.ui.screens.quiz.QuizBattleScreen(
                onBack = { navController.popBackStack() },
                onGameOver = { score: Int, maxCombo: Int, questionsAnswered: Int, battleCoins: Int ->
                    if (battleCoins > 0) {
                        kotlinx.coroutines.MainScope().launch {
                            val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                                navController.context.applicationContext,
                                com.aipoweredgita.app.services.SyncWorkerEntryPoint::class.java
                            )
                            entryPoint.statsRepository().trackBattleCompletion(battleCoins, score, questionsAnswered)
                        }
                    }
                }
            )
        }
    }
}
