package com.aipoweredgita.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.os.Build
import android.content.Context
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import com.aipoweredgita.app.navigation.NavGraph
import com.aipoweredgita.app.navigation.Screen
import com.aipoweredgita.app.ui.SplashScreen
import com.aipoweredgita.app.ui.ExitScreen
import com.aipoweredgita.app.ui.MainScreen
import com.aipoweredgita.app.ui.ModelDownloadScreen
import com.aipoweredgita.app.ui.UiConfigProvider
import com.aipoweredgita.app.ui.theme.GitaLearningTheme
import com.aipoweredgita.app.utils.ThemePreferences
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.ml.ModelDownloadManager
import com.aipoweredgita.app.ml.ModelStateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.aipoweredgita.app.notifications.DailyReflectionWorker
import com.aipoweredgita.app.services.GemmaDownloadWorker
import com.aipoweredgita.app.services.QwenDownloadWorker
import com.aipoweredgita.app.services.QuestionIngestionWorker

class MainActivity : ComponentActivity() {
    
    // Notification permission launcher (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            android.util.Log.d("MainActivity", "Notification permission granted")
        } else {
            android.util.Log.d("MainActivity", "Notification permission denied")
        }
    }
    
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Create notification channels
        createNotificationChannels()
        
        // Request notification permission for Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Initialize database and schedule workers on background thread (non-blocking)
        // This prevents ANR during app startup
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Initialize database and ensure UserStats record exists
                val database = GitaDatabase.getDatabase(applicationContext)
                database.userStatsDao().initializeStatsIfNeeded()
                
                // Check and apply yoga progression decay for inactivity
                val yogaProgressionRepository = com.aipoweredgita.app.repository.YogaProgressionRepository(database.yogaProgressionDao())
                val (didLevelDecrease, oldLevel, newLevel) = yogaProgressionRepository.checkAndApplyDecay()
                
                // Show notification if level decreased
                if (didLevelDecrease && oldLevel != null && newLevel != null) {
                    val lastDate = try {
                        val progression = yogaProgressionRepository.getProgression()
                        java.time.LocalDate.parse(progression.lastActivityDate)
                    } catch (e: Exception) {
                        java.time.LocalDate.now()
                    }
                    val daysInactive = java.time.temporal.ChronoUnit.DAYS.between(lastDate, java.time.LocalDate.now()).toInt()
                    
                    com.aipoweredgita.app.notifications.YogaLevelUpNotificationManager.showLevelDecreaseNotification(
                        applicationContext,
                        oldLevel,
                        newLevel,
                        daysInactive
                    )
                }

                // Schedule daily verse notification worker (once; it survives app restarts)
                // Switch back to main thread for WorkManager
                CoroutineScope(Dispatchers.Main).launch {
                    scheduleDailyVerseWorker()
                    
                    // Automatically schedule model downloads and question ingestion on first run
                    GemmaDownloadWorker.scheduleBackgroundDownload(applicationContext)
                    QwenDownloadWorker.scheduleImmediateDownload(applicationContext)
                    QuestionIngestionWorker.schedule(applicationContext)
                }
            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "Error initializing database: ${e.message}", e)
            }
        }

        val themePreferences = ThemePreferences(applicationContext)

        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var showExitDialog by remember { mutableStateOf(false) }
            val isDarkTheme by themePreferences.isDarkTheme.collectAsStateWithLifecycle(initialValue = false)
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                // Initialize modelsReady based on actual download state to avoid flicker
                try {
                    val manager = ModelDownloadManager(applicationContext)
                    val ready = manager.areAllModelsDownloaded()
                    ModelStateManager.setModelsReady(ready)
                } catch (_: Exception) {
                    ModelStateManager.setModelsReady(false)
                }
            }

            val themePreferencesState = ThemePreferences(applicationContext)
            val accent by themePreferencesState.accent.collectAsStateWithLifecycle(initialValue = "Saffron")
            val dynamicColor by themePreferencesState.isDynamicColor.collectAsStateWithLifecycle(initialValue = true)
            GitaLearningTheme(darkTheme = isDarkTheme, dynamicColor = dynamicColor, accentName = accent) {
                UiConfigProvider {
                when {
                    showSplash -> {
                        SplashScreen(onSplashFinished = { showSplash = false })
                    }
                    showExitDialog -> {
                        ExitScreen(
                            onConfirmExit = { finish() },
                            onCancelExit = { showExitDialog = false }
                        )
                    }
                    else -> {
                        val launchRoute = intent.getStringExtra("NAVIGATE_TO")
                        val launchChapter = intent.getIntExtra("CHAPTER", 0)
                        val launchVerse = intent.getIntExtra("VERSE", 0)
                        MainContent(
                            onRequestExit = { showExitDialog = true },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { scope.launch { themePreferences.setDarkTheme(it) } },
                            launchRoute = launchRoute,
                            launchChapter = launchChapter,
                            launchVerse = launchVerse
                        )
                    }
                }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainContent(
        onRequestExit: () -> Unit,
        isDarkTheme: Boolean,
        onThemeToggle: (Boolean) -> Unit,

        launchRoute: String? = null,
        launchChapter: Int = 0,
        launchVerse: Int = 0
    ) {
        val navController = rememberNavController()
        
        // Handle deep linking
        LaunchedEffect(launchRoute) {
            if (launchRoute == "random_sloka") {
                if (launchChapter > 0 && launchVerse > 0) {
                    navController.navigate("random_sloka?chapter=$launchChapter&verse=$launchVerse")
                } else {
                    navController.navigate(Screen.RandomSloka.route)
                }
            }
        }

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Handle back button on home screen
        BackHandler(enabled = currentRoute == Screen.Home.route) {
            onRequestExit()
        }

        MainScreen(
            navController = navController,
            isDarkTheme = isDarkTheme,
            onThemeToggle = onThemeToggle
        )
    }
}

private fun ComponentActivity.scheduleDailyVerseWorker() {
    val work = PeriodicWorkRequestBuilder<
        com.aipoweredgita.app.notifications.DailyVerseWorker
    >(24, TimeUnit.HOURS)
        .build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "daily_verse",
        ExistingPeriodicWorkPolicy.KEEP,
        work
    )

    // Also schedule daily reflection prompt
    val reflection = PeriodicWorkRequestBuilder<DailyReflectionWorker>(24, TimeUnit.HOURS).build()
    WorkManager.getInstance(this).enqueueUniquePeriodicWork(
        "daily_reflection_work",
        ExistingPeriodicWorkPolicy.KEEP,
        reflection
    )
}

private fun ComponentActivity.createNotificationChannels() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Model download channel
        val modelChannel = NotificationChannel(
            "model_download_channel",
            "Model Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for ML model downloads"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(modelChannel)

        // Qwen download channel
        val qwenChannel = NotificationChannel(
            "qwen_download_channel",
            "Qwen Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for Qwen AI model downloads"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(qwenChannel)

        // Question ingestion channel
        val ingestionChannel = NotificationChannel(
            "question_ingestion_channel",
            "Content Updates",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notifications for quiz content updates"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(ingestionChannel)
        
        android.util.Log.d("MainActivity", "Notification channels created")
    }
}
