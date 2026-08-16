package com.aipoweredgita.app.ui.screens.explore

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.MandalaBackground
import com.aipoweredgita.app.ui.screens.explore.components.RandomSlokaActions
import com.aipoweredgita.app.ui.screens.explore.components.RandomSlokaCard
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.viewmodel.RandomSlokaViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomSlokaScreen(
    onBack: () -> Unit,
    initialChapter: Int = 0,
    initialVerse: Int = 0,
    viewModel: RandomSlokaViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentVerse = state.currentVerse

    var isSpeaking by remember { mutableStateOf(false) }

    val voiceManager = remember { VoiceManager(context) }

    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val gold = if (isDark) GoldSpark else Saffron

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopSpeaking()
            voiceManager.destroy()
        }
    }

    LaunchedEffect(initialChapter, initialVerse) {
        viewModel.loadVerse(initialChapter, initialVerse)
    }

    fun shareSloka(verse: CachedVerse) {
        val shareText = "Bhagavad Gita Chapter ${verse.chapterNo}, Verse ${verse.verseNo}:\n\n${verse.verse}\n\nTranslation:\n${verse.translation}\n\nShared via AI Powered Gita App"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Bhagavad Gita Verse")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        try {
            context.startActivity(Intent.createChooser(intent, "Share Sloka"))
            viewModel.trackSlokaShared(verse.chapterNo, verse.verseNo) { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                 CenterAlignedTopAppBar(
                     title = { Text("Random Sloka", style = MaterialTheme.typography.titleLarge, color = textPrimary) },
                     navigationIcon = {
                         IconButton(onClick = onBack) {
                             Icon(
                                 imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                 contentDescription = "Back",
                                 tint = gold
                             )
                         }
                     },
                     actions = {
                          IconButton(onClick = {
                              viewModel.generateNewSloka()
                          }) {
                             Icon(
                                 imageVector = Icons.Default.Refresh,
                                 contentDescription = "Refresh",
                                 tint = gold
                             )
                         }
                     },
                     colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                         containerColor = Color.Transparent,
                         titleContentColor = textPrimary,
                         navigationIconContentColor = textPrimary
                     )
                 )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Background
                MandalaBackground(
                    modifier = Modifier.align(Alignment.Center).size(300.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (currentVerse != null) {
                        // Make the content scrollable
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 16.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            AnimatedContent(
                                targetState = currentVerse,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "verse_transition"
                            ) { verse ->
                                if (verse != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(24.dp)
                                    ) {
                                        RandomSlokaCard(verse = verse, isDark = isDark)
                                        
                                        RandomSlokaActions(
                                            isSpeaking = isSpeaking,
                                            onListenClick = {
                                                 if (isSpeaking) {
                                                     voiceManager.stopSpeaking()
                                                     isSpeaking = false
                                                 } else {
                                                     // Try to set Telugu locale, fallback happens internally if unsupported
                                                     val isSupported = voiceManager.setLanguage(Locale.forLanguageTag("te-IN"))
                                                     if (isSupported) {
                                                         isSpeaking = true
                                                         val textToRead = "${verse.verse}. ${verse.translation}"
                                                         voiceManager.speak(textToRead, flush = true) {
                                                             isSpeaking = false
                                                         }
                                                     } else {
                                                         Toast.makeText(context, "Telugu Voice Data not installed! Please install it in Android Settings -> Text-to-Speech.", Toast.LENGTH_LONG).show()
                                                     }
                                                 }
                                            },
                                            onShareClick = { shareSloka(verse) },
                                            goldColor = gold
                                        )
                                    }
                                }
                            }
                        }

                    } else {
                        CircularProgressIndicator(color = gold)
                    }
                }
            }
        }
    }
}
