package com.aipoweredgita.app.ui.screens.explore

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipoweredgita.app.R
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.ui.ProgressShareCard
import com.aipoweredgita.app.ui.components.AmbientOrbs
import com.aipoweredgita.app.ui.components.SlokaCardBg
import com.aipoweredgita.app.ui.screens.explore.components.RandomSlokaActions
import com.aipoweredgita.app.ui.screens.explore.components.RandomSlokaCard
import com.aipoweredgita.app.ui.theme.rememberGitaColors
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.viewmodel.RandomSlokaViewModel
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val colors = rememberGitaColors()
    val scope = rememberCoroutineScope()

    var isSpeaking by remember { mutableStateOf(false) }
    var isSharing by remember { mutableStateOf(false) }
    var pendingShareVerse by remember { mutableStateOf<CachedVerse?>(null) }

    val voiceManager = remember { VoiceManager(context) }

    val shareLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val verse = pendingShareVerse
        pendingShareVerse = null
        isSharing = false
        if (verse != null) {
            viewModel.onShareCompleted(verse.chapterNo, verse.verseNo) { key ->
                val msg = when {
                    key.startsWith("shared_ok_coins:") -> {
                        val coins = key.removePrefix("shared_ok_coins:").toIntOrNull() ?: 0
                        context.getString(R.string.random_sloka_shared_ok_coins, coins)
                    }
                    else -> context.getString(R.string.random_sloka_shared_ok)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopSpeaking()
            voiceManager.destroy()
        }
    }

    // Stop TTS when the verse changes mid-playback
    LaunchedEffect(currentVerse?.chapterNo, currentVerse?.verseNo) {
        voiceManager.stopSpeaking()
        isSpeaking = false
    }

    LaunchedEffect(initialChapter, initialVerse) {
        viewModel.loadVerse(initialChapter, initialVerse)
    }

    fun launchPlainShare(verse: CachedVerse) {
        if (isSharing) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Bhagavad Gita Verse")
            putExtra(Intent.EXTRA_TEXT, viewModel.sharePlainText(verse))
        }
        pendingShareVerse = verse
        isSharing = true
        try {
            shareLauncher.launch(
                Intent.createChooser(intent, context.getString(R.string.random_sloka_share_chooser))
            )
        } catch (e: Exception) {
            pendingShareVerse = null
            isSharing = false
            Toast.makeText(
                context,
                context.getString(R.string.random_sloka_share_failed) + ": ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun launchImageShare(verse: CachedVerse) {
        if (isSharing) return
        isSharing = true
        pendingShareVerse = verse
        scope.launch {
            try {
                val chooser = withContext(Dispatchers.IO) {
                    val bitmap = ProgressShareCard.generateVerseShareBitmap(
                        verseText = verse.verse,
                        translation = verse.translation,
                        chapter = verse.chapterNo,
                        verseNo = verse.verseNo,
                    )
                    try {
                        ProgressShareCard.buildShareImageIntent(
                            context,
                            bitmap,
                            context.getString(R.string.random_sloka_share_chooser)
                        )
                    } finally {
                        bitmap.recycle()
                    }
                }
                if (chooser != null) {
                    shareLauncher.launch(chooser)
                } else {
                    pendingShareVerse = null
                    isSharing = false
                    Toast.makeText(context, R.string.random_sloka_share_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                pendingShareVerse = null
                isSharing = false
                Toast.makeText(
                    context,
                    context.getString(R.string.random_sloka_share_failed) + ": ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun toggleListen(verse: CachedVerse) {
        if (isSpeaking) {
            voiceManager.stopSpeaking()
            isSpeaking = false
            return
        }
        val supported = voiceManager.setLanguage(Locale.forLanguageTag("te-IN"))
        if (!supported) {
            Toast.makeText(context, R.string.random_sloka_telugu_tts_missing, Toast.LENGTH_LONG).show()
            return
        }
        // Speak Telugu translation/meaning only — never Devanagari through te-IN
        isSpeaking = true
        voiceManager.speak(viewModel.ttsTextFor(verse), flush = true) {
            isSpeaking = false
        }
    }

    val pullState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        // Continuity with dashboard Random Sloka tile
        SlokaCardBg(modifier = Modifier.fillMaxSize())
        AmbientOrbs(modifier = Modifier.fillMaxSize())

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            stringResource(R.string.random_sloka_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = colors.textPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                                tint = colors.accent
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.generateNewSloka(force = false) },
                            enabled = !state.isLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.random_sloka_refresh),
                                tint = if (state.isLoading) colors.textDim else colors.accent
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = colors.textPrimary,
                        navigationIconContentColor = colors.textPrimary
                    )
                )
            }
        ) { paddingValues ->
            PullToRefreshBox(
                isRefreshing = state.isLoading && currentVerse != null,
                onRefresh = { viewModel.generateNewSloka(force = false) },
                state = pullState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    state.isLoading && currentVerse == null -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = colors.accent)
                            Spacer(Modifier.height(12.dp))
                            Text(
                                stringResource(R.string.random_sloka_loading),
                                color = colors.textSecondary
                            )
                        }
                    }
                    currentVerse == null -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(R.string.random_sloka_error),
                                color = colors.textPrimary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = { viewModel.retry() }) {
                                Text(stringResource(R.string.generic_retry))
                            }
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .verticalScroll(rememberScrollState()),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            if (!state.errorMessage.isNullOrBlank()) {
                                Text(
                                    text = stringResource(R.string.random_sloka_error),
                                    color = colors.textSecondary,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Button(onClick = {
                                    viewModel.clearError()
                                    viewModel.retry()
                                }) {
                                    Text(stringResource(R.string.generic_retry))
                                }
                            }
                            AnimatedContent(
                                targetState = currentVerse,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "verse_transition"
                            ) { verse ->
                                if (verse != null) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(20.dp)
                                    ) {
                                        RandomSlokaCard(verse = verse)
                                        RandomSlokaActions(
                                            isSpeaking = isSpeaking,
                                            isSharing = isSharing,
                                            onListenClick = { toggleListen(verse) },
                                            onShareClick = { launchPlainShare(verse) },
                                            onShareImageClick = { launchImageShare(verse) },
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
