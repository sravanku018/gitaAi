package com.aipoweredgita.app.navigation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aipoweredgita.app.ui.DashboardScreen

import com.aipoweredgita.app.ui.FavoritesScreen
import com.aipoweredgita.app.ui.OfflineDownloadScreen
import com.aipoweredgita.app.ui.ProfileScreen
import com.aipoweredgita.app.ui.QuizStatsScreen
import com.aipoweredgita.app.ui.WidgetSettingsScreen
import com.aipoweredgita.app.ui.BadgesScreen
import com.aipoweredgita.app.ui.RandomSlokaScreen
import com.aipoweredgita.app.ui.AwakeningPage
import com.aipoweredgita.app.ui.DailyActivityScreen
import com.aipoweredgita.app.ui.SettingsScreen
import com.aipoweredgita.app.ui.ProtectedQuizConfigScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.viewmodel.OfflineDownloadViewModel

sealed class Screen(val route: String) {
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
    object QuizStats : Screen("quiz_stats")
    object WidgetSettings : Screen("widget_settings")
    object Settings : Screen("settings")
    object Badges : Screen("badges")
    object Awakening : Screen("awakening")
    object DailyActivity : Screen("daily_activity")
    object Recommendations : Screen("recommendations")

    object Flashcards : Screen("flashcards?topic={topic}")
    object RandomSloka : Screen("random_sloka?chapter={chapter}&verse={verse}")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    onThemeToggle: (Boolean) -> Unit = {}
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            DashboardScreen(
                onNavigateToNormalMode = { navController.navigate(Screen.ChapterSelection.route) },
                onNavigateToQuizMode = { navController.navigate(Screen.QuizSection.route) },
                onNavigateToVoiceStudio = { navController.navigate(Screen.VoiceStudio.route) },
                onNavigateToRecommendations = { navController.navigate(Screen.Recommendations.route) },
                onNavigateToRandomSloka = { navController.navigate("random_sloka") }
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
            val vm: com.aipoweredgita.app.viewmodel.NormalModeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
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
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.QuizConfig.route)
            }
            val quizViewModel: QuizViewModel = viewModel(
                viewModelStoreOwner = parentEntry
            )
            ProtectedQuizConfigScreen(
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
            val quizConfigEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.QuizConfig.route)
            }
            val quizViewModel: QuizViewModel = viewModel(
                viewModelStoreOwner = quizConfigEntry
            )
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

        composable(Screen.VoiceStudio.route) {
            com.aipoweredgita.app.ui.VoiceStudioScreen(
                onExit = { navController.popBackStack() }
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onVerseClick = { chapter, verse ->
                    // Navigate to normal mode with specific verse
                    navController.navigate(Screen.NormalMode.route)
                }
            )
        }

        composable(Screen.OfflineDownload.route) {
            val offlineViewModel: OfflineDownloadViewModel = viewModel()
            OfflineDownloadScreen(viewModel = offlineViewModel)
        }

        composable(Screen.Profile.route) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(Screen.Home.route)
            }
            val profileViewModel: com.aipoweredgita.app.viewmodel.ProfileViewModel = viewModel(
                viewModelStoreOwner = parentEntry
            )
            ProfileScreen(
                onNavigateToQuizStats = {
                    navController.navigate(Screen.QuizStats.route)
                },
                isDarkTheme = isDarkTheme,
                onThemeToggle = onThemeToggle,
                // Add badge navigation callback
                onNavigateToBadges = { navController.navigate(Screen.Badges.route) },
                viewModel = profileViewModel
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                themePreferences = com.aipoweredgita.app.utils.ThemePreferences(LocalContext.current),
                isDarkTheme = isDarkTheme,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Badges.route) {
            BadgesScreen()
        }

        composable(Screen.Awakening.route) {
            AwakeningPage()
        }

        composable(Screen.DailyActivity.route) {
            DailyActivityScreen(
                onNavigateToProgression = { navController.navigate(Screen.Badges.route) }
            )
        }

        composable(Screen.QuizStats.route) {
            QuizStatsScreen()
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
    }
}
