package com.aipoweredgita.app.ui.screens.study

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aipoweredgita.app.domain.model.NormalModeEvent
import com.aipoweredgita.app.ui.background.AppBackground
import com.aipoweredgita.app.ui.background.BgPattern
import com.aipoweredgita.app.ui.screens.study.components.*
import com.aipoweredgita.app.ui.theme.MotionTokens
import com.aipoweredgita.app.ui.theme.rememberThemeIsDark
import com.aipoweredgita.app.util.TextUtils.sanitizeText
import com.aipoweredgita.app.utils.SlokaNavMode
import com.aipoweredgita.app.utils.ThemePreferences
import com.aipoweredgita.app.viewmodel.NormalModeViewModel
import com.aipoweredgita.app.viewmodel.ScreenConfigViewModel

@Composable
fun NormalModeScreen(
    modifier: Modifier = Modifier,
    viewModel: NormalModeViewModel = hiltViewModel(),
    screenConfigViewModel: ScreenConfigViewModel = hiltViewModel(),
    onReadOfflineClick: () -> Unit = {},
    onNavigateToQuizBattle: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showChapterDialog by remember { mutableStateOf(false) }
    var showVerseDialog   by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val themePreferences = remember { ThemePreferences(context) }
    val slokaNavMode by themePreferences.slokaNavMode.collectAsStateWithLifecycle(
        initialValue = SlokaNavMode.BUTTONS
    )

    val voiceManager = remember(context) { com.aipoweredgita.app.utils.VoiceManager(context) }
    var isSpeaking by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.stopSpeaking()
        }
    }

    LaunchedEffect(state.currentChapter, state.currentVerse) {
        voiceManager.stopSpeaking()
        isSpeaking = false
    }

    // Prefetch chapter verses for swipe/scroll (verses only — not chapters)
    LaunchedEffect(state.currentChapter, state.selectedLanguage, slokaNavMode) {
        if (slokaNavMode == SlokaNavMode.SWIPE || slokaNavMode == SlokaNavMode.SCROLL) {
            viewModel.loadChapterVerses(state.currentChapter)
        }
    }

    fun toggleTts(verse: com.aipoweredgita.app.data.GitaVerse) {
        if (isSpeaking) {
            voiceManager.stopSpeaking()
            isSpeaking = false
        } else {
            val locale = if (state.selectedLanguage == "TE") java.util.Locale.forLanguageTag("te-IN") else java.util.Locale.US
            voiceManager.setPreferredLocale(locale)

            val fullText = buildString {
                append("Chapter ${verse.chapterNo}, Verse ${verse.verseNo}. ")
                val cleanSloka = sanitizeText(verse.verse)
                if (cleanSloka.isNotBlank()) append("$cleanSloka. ")
                val cleanMeaning = sanitizeText(verse.meaning)
                if (cleanMeaning.isNotBlank()) append("Meaning: $cleanMeaning. ")
                val cleanExplanation = sanitizeText(verse.explanation)
                if (cleanExplanation.isNotBlank()) append("Commentary: $cleanExplanation.")
            }

            isSpeaking = true
            voiceManager.speak(fullText) {
                isSpeaking = false
            }
        }
    }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is com.aipoweredgita.app.domain.model.NormalModeSideEffect.ShowMessage -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    val isNetworkAvailable by com.aipoweredgita.app.utils.NetworkUtils
        .networkStatusFlow(context)
        .collectAsStateWithLifecycle(
            initialValue = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(context)
        )


    fun shareVerse(verse: com.aipoweredgita.app.data.GitaVerse) {
        val text = buildString {
            appendLine("📖 Bhagavad Gita ${verse.chapterNo}:${verse.verseNo}")
            appendLine()
            appendLine(sanitizeText(verse.verse))
            appendLine()
            appendLine("#BhagavadGita #Gita #Wisdom")
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text); type = "text/plain"
                }, "Share sloka via"
            )
        )
        viewModel.onEvent(com.aipoweredgita.app.domain.model.NormalModeEvent.TrackShare)
    }

    val isDark = rememberThemeIsDark()
    AppBackground(
        pattern = BgPattern.ORBS_MANDALA,
        intensity = 0.4f,
        isDark = isDark
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            AnimatedVisibility(
                visible = !isNetworkAvailable,
                enter = slideInVertically() + fadeIn(),
                exit  = slideOutVertically() + fadeOut()
            ) {
                OfflineBanner(onReadOfflineClick = onReadOfflineClick)
            }

            val currentVerse = state.verse
            when {
                state.isLoading && currentVerse == null -> GitaLoadingScreen()
                state.error != null && currentVerse == null -> GitaErrorScreen(
                    message = state.error ?: "",
                    onRetry = { viewModel.onEvent(NormalModeEvent.LoadVerse(state.currentChapter, state.currentVerse)) }
                )
                currentVerse != null  -> {
                    val verse = currentVerse

                    val verseAnim by animateFloatAsState(
                        targetValue = if (state.verse != null) 1f else 0f,
                        animationSpec = MotionTokens.springExpressive<Float>(),
                        label = "verse_enter"
                    )

                    val maxInChapter = com.aipoweredgita.app.util.GitaConstants.CHAPTER_VERSE_COUNTS[verse.chapterNo] ?: 47

                    Box(modifier = Modifier.weight(1f)) {
                        when (slokaNavMode) {
                            SlokaNavMode.SWIPE -> {
                                SlokaSwipeReader(
                                    chapter = verse.chapterNo,
                                    currentVerseNo = verse.verseNo,
                                    chapterVerses = state.chapterVerses,
                                    isLoadingChapter = state.isChapterVersesLoading,
                                    selectedLanguage = state.selectedLanguage,
                                    isSpeaking = isSpeaking,
                                    separatedNote = state.separatedVerseNote,
                                    combinedNos = state.combinedVerseNos,
                                    verseAnim = verseAnim,
                                    onVerseSettled = { v ->
                                        viewModel.onEvent(NormalModeEvent.LoadVerse(verse.chapterNo, v))
                                    },
                                    onLanguageToggle = { lang ->
                                        voiceManager.stopSpeaking()
                                        isSpeaking = false
                                        viewModel.onEvent(NormalModeEvent.ToggleLanguage(lang))
                                    },
                                    onTtsToggle = { toggleTts(it) },
                                    onChapterTap = { showChapterDialog = true },
                                    onVerseTap = { showVerseDialog = true },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            SlokaNavMode.SCROLL -> {
                                SlokaScrollReader(
                                    chapter = verse.chapterNo,
                                    currentVerseNo = verse.verseNo,
                                    chapterVerses = state.chapterVerses,
                                    isLoadingChapter = state.isChapterVersesLoading,
                                    selectedLanguage = state.selectedLanguage,
                                    isSpeaking = isSpeaking,
                                    separatedNote = state.separatedVerseNote,
                                    combinedNos = state.combinedVerseNos,
                                    onVerseVisible = { v ->
                                        viewModel.onEvent(NormalModeEvent.LoadVerse(verse.chapterNo, v))
                                    },
                                    onLanguageToggle = { lang ->
                                        voiceManager.stopSpeaking()
                                        isSpeaking = false
                                        viewModel.onEvent(NormalModeEvent.ToggleLanguage(lang))
                                    },
                                    onTtsToggle = { toggleTts(it) },
                                    onChapterTap = { showChapterDialog = true },
                                    onVerseTap = { showVerseDialog = true },
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            SlokaNavMode.BUTTONS -> {
                                val verseScrollState = rememberScrollState()
                                LaunchedEffect(verse.chapterNo, verse.verseNo) {
                                    verseScrollState.scrollTo(0)
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(verseScrollState)
                                        .padding(horizontal = 18.dp)
                                        .graphicsLayer {
                                            alpha = verseAnim
                                            translationY = (1f - verseAnim) * 20f
                                        }
                                ) {
                                    Spacer(Modifier.height(16.dp))
                                    SlokaVerseBody(
                                        verse = verse,
                                        combinedNos = state.combinedVerseNos,
                                        selectedLanguage = state.selectedLanguage,
                                        isSpeaking = isSpeaking,
                                        separatedNote = state.separatedVerseNote,
                                        onLanguageToggle = { lang ->
                                            voiceManager.stopSpeaking()
                                            isSpeaking = false
                                            viewModel.onEvent(NormalModeEvent.ToggleLanguage(lang))
                                        },
                                        onTtsToggle = { toggleTts(verse) },
                                        onChapterTap = { showChapterDialog = true },
                                        onVerseTap = { showVerseDialog = true },
                                    )
                                    Spacer(Modifier.height(24.dp))
                                }
                            }
                        }
                    }

                    BottomActionBar(
                        isFavorite        = state.isFavorite,
                        favoriteMessage   = state.favoriteMessage,
                        canGoPrev         = !(verse.chapterNo == 1 && verse.verseNo == 1) && !state.isLoading,
                        canGoNext         = !(verse.chapterNo == 18 && verse.verseNo == 78) && !state.isLoading,
                        onFavoriteToggle  = { viewModel.onEvent(NormalModeEvent.ToggleFavorite) },
                        onShare           = { shareVerse(verse) },
                        onBattleQuiz      = onNavigateToQuizBattle,
                        onPrev            = {
                            if (slokaNavMode == SlokaNavMode.BUTTONS) {
                                viewModel.onEvent(NormalModeEvent.PreviousVerse)
                            } else if (verse.verseNo > 1) {
                                viewModel.onEvent(NormalModeEvent.LoadVerse(verse.chapterNo, verse.verseNo - 1))
                            }
                        },
                        onNext            = {
                            if (slokaNavMode == SlokaNavMode.BUTTONS) {
                                viewModel.onEvent(NormalModeEvent.NextVerse)
                            } else if (verse.verseNo < maxInChapter) {
                                viewModel.onEvent(NormalModeEvent.LoadVerse(verse.chapterNo, verse.verseNo + 1))
                            }
                        },
                        showNavButtons    = slokaNavMode == SlokaNavMode.BUTTONS,
                    )
                }
            }
        }
    }

    if (showChapterDialog) {
        ChapterSelectionDialog(
            currentChapter    = state.currentChapter,
            onDismiss         = { showChapterDialog = false },
            onChapterSelected = { ch -> viewModel.onEvent(NormalModeEvent.GoToChapter(ch)); showChapterDialog = false }
        )
    }
    val currentVerseObj = state.verse
    if (showVerseDialog && currentVerseObj != null) {
        VerseSelectionDialog(
            currentChapter  = currentVerseObj.chapterNo,
            currentVerse    = currentVerseObj.verseNo,
            maxVerses       = com.aipoweredgita.app.util.GitaConstants.CHAPTER_VERSE_COUNTS[currentVerseObj.chapterNo] ?: 47,
            combinedGroups  = state.combinedGroups,
            onDismiss       = { showVerseDialog = false },
            onVerseSelected = { v ->
                viewModel.onEvent(NormalModeEvent.LoadVerse(currentVerseObj.chapterNo, v))
                showVerseDialog = false
            }
        )
    }
}
