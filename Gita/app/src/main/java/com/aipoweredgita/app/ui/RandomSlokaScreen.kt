package com.aipoweredgita.app.ui

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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import com.aipoweredgita.app.network.GitaApi
import com.aipoweredgita.app.util.GitaConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.widget.Toast

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomSlokaScreen(
    onBack: () -> Unit = {},
    initialChapter: Int = 0,
    initialVerse: Int = 0
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val database = remember { GitaDatabase.getDatabase(context) }
    var currentVerse by remember { mutableStateOf<CachedVerse?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }

    val voiceManager = remember { VoiceManager(context) }
    DisposableEffect(Unit) {
        onDispose { voiceManager.destroy() }
    }

    fun generateNewSloka() {
        scope.launch {
            isLoading = true
            try {
                // Determine total number of cached verses to know if we can fetch one
                val count = database.cachedVerseDao().getCachedCount()
                if (count > 0) {
                    val newVerse = database.cachedVerseDao().getRandomVerse()
                    if (newVerse != null) {
                        currentVerse = newVerse
                        // Record that we showed this verse
                        database.randomVerseHistoryDao().insertShownVerse(
                            RandomVerseHistory(
                                chapterNo = newVerse.chapterNo,
                                verseNo = newVerse.verseNo
                            )
                        )
                    } else {
                        // If all verses shown, maybe clear history or reset?
                        // For now, simpler to just clear history and try again to loop?
                        // Or just show nothing? Let's clear history and retry once.
                        database.randomVerseHistoryDao().clearHistory()
                        val retryVerse = database.cachedVerseDao().getRandomVerse()
                        if (retryVerse != null) {
                             currentVerse = retryVerse
                             database.randomVerseHistoryDao().insertShownVerse(
                                RandomVerseHistory(
                                    chapterNo = retryVerse.chapterNo,
                                    verseNo = retryVerse.verseNo
                                )
                            )
                        }
                    }
                } else {
                    // Fallback: Fetch from API if database is empty
                    val chapter = (1..GitaConstants.MAX_CHAPTERS).random()
                    val maxVerses = GitaConstants.CHAPTER_VERSE_COUNTS[chapter] ?: 20
                    val verseNo = (1..maxVerses).random()

                    try {
                        val apiVerse = withContext(Dispatchers.IO) {
                            GitaApi.retrofitService.getVerse(
                                GitaConstants.DEFAULT_LANGUAGE,
                                chapter,
                                verseNo
                            )
                        }

                        // Convert to CachedVerse
                        val cachedVerse = CachedVerse.fromGitaVerse(apiVerse)
                        
                        // Save to DB so we have it for next time
                        database.cachedVerseDao().insertVerse(cachedVerse)
                        
                        // Update state
                        currentVerse = cachedVerse
                        
                        // Record history
                        database.randomVerseHistoryDao().insertShownVerse(
                            RandomVerseHistory(
                                chapterNo = cachedVerse.chapterNo,
                                verseNo = cachedVerse.verseNo
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to fetch verse. Check internet.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (initialChapter > 0 && initialVerse > 0) {
            // Load specific verse
            scope.launch {
                isLoading = true
                try {
                     // Try cache first
                     var cached = database.cachedVerseDao().getVerse(initialChapter, initialVerse)
                     if (cached == null) {
                         // Fetch from API
                         withContext(Dispatchers.IO) {
                             try {
                                 val apiVerse = GitaApi.retrofitService.getVerse(
                                     GitaConstants.DEFAULT_LANGUAGE,
                                     initialChapter,
                                     initialVerse
                                 )
                                 val newCached = CachedVerse.fromGitaVerse(apiVerse)
                                 database.cachedVerseDao().insertVerse(newCached)
                                 cached = newCached
                             } catch (e: Exception) {
                                e.printStackTrace() 
                             }
                         }
                     }
                     
                     if (cached != null) {
                         currentVerse = cached
                         // Record history? Maybe not if just viewing specific? 
                         // Let's record it to avoid it showing up randomly soon
                          database.randomVerseHistoryDao().insertShownVerse(
                            RandomVerseHistory(
                                chapterNo = cached.chapterNo,
                                verseNo = cached.verseNo
                            )
                        )
                     } else {
                         // Fallback if specific load failed
                         generateNewSloka()
                     }
                } finally {
                    isLoading = false
                }
            }
        } else {
             generateNewSloka()
        }
    }

    fun shareSloka(verse: CachedVerse) {
        val shareText = """
            Bhagavad Gita - Chapter ${verse.chapterNo}, Verse ${verse.verseNo}
            
            ${verse.verse}
        """.trimIndent()

        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    Scaffold(
        topBar = {
             CenterAlignedTopAppBar(
                 title = { Text("Random Sloka", style = MaterialTheme.typography.titleLarge) },
                 navigationIcon = {
                     IconButton(onClick = onBack) {
                         Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                     }
                 }
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

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
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
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        HorizontalDivider()
                                        Text(
                                            text = verse.translation,
                                            style = MaterialTheme.typography.bodyLarge,
                                            textAlign = TextAlign.Center
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
                                                 val isSupported = voiceManager.setLanguage(Locale("te", "IN"))
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
                                         }
                                     ) {
                                         Icon(
                                             if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow, 
                                             contentDescription = "Listen"
                                         )
                                         Spacer(Modifier.width(8.dp))
                                         Text(if (isSpeaking) "Stop Audio" else "Listen in Telugu")
                                     }

                                     Spacer(Modifier.width(12.dp))

                                     OutlinedButton(
                                         onClick = { shareSloka(verse) }
                                     ) {
                                         Icon(Icons.Default.Share, contentDescription = null)
                                         Spacer(Modifier.width(8.dp))
                                         Text("Share")
                                     }
                                }
                                    }
                                }
                            }
                        }

                } else {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
