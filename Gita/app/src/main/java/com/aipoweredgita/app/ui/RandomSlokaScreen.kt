package com.aipoweredgita.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RandomVerseHistory
import com.aipoweredgita.app.ui.components.MandalaBackground
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.PremiumDashboardCard
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import kotlinx.coroutines.launch
import java.util.Locale
import com.aipoweredgita.app.utils.VoiceManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import com.aipoweredgita.app.ui.theme.*
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomSlokaScreen(
    onBack: () -> Unit,
    initialChapter: Int = 0,
    initialVerse: Int = 0,
    viewModel: com.aipoweredgita.app.viewmodel.RandomSlokaViewModel = hiltViewModel()
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
                                 imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
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
                                    PremiumDashboardCard(
                                        title = "Chapter ${verse.chapterNo}",
                                        description = "Verse ${verse.verseNo}",
                                        icon = { Text("ॐ", fontSize = 32.sp) }, // Om symbol
                                        gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
                                        onClick = {}, // No action on card click itself
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(MaterialTheme.shapes.extraLarge)
                                            .background(cardBg)
                                            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
                                            .shadow(6.dp, MaterialTheme.shapes.extraLarge)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Text(
                                                text = verse.verse,
                                                style = MaterialTheme.typography.headlineSmall,
                                                textAlign = TextAlign.Center,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) GoldSpark else Saffron
                                            )
                                            HorizontalDivider(color = cardBorder)
                                            Text(
                                                text = verse.translation,
                                                style = MaterialTheme.typography.bodyLarge,
                                                textAlign = TextAlign.Center,
                                                color = textPrimary
                                            )
                                        }
                                    }
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                         OutlinedButton(
                                             onClick = {
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
                                             colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                             border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                                         ) {
                                             Icon(
                                                 if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow, 
                                                 contentDescription = "Listen",
                                                 tint = gold
                                             )
                                             Spacer(Modifier.width(8.dp))
                                             Text(if (isSpeaking) "Stop Audio" else "Listen in Telugu")
                                         }

                                         Spacer(Modifier.width(12.dp))

                                         OutlinedButton(
                                             onClick = { shareSloka(verse) },
                                             colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                                             border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.5f))
                                         ) {
                                             Icon(Icons.Default.Share, contentDescription = null, tint = gold)
                                             Spacer(Modifier.width(8.dp))
                                             Text("Share")
                                         }
                                    }
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
