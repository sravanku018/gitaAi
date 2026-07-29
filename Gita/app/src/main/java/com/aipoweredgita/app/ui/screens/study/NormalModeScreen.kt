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
                state.isLoading       -> GitaLoadingScreen()
                state.error != null   -> GitaErrorScreen(
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
                    val verseScrollState = rememberScrollState()
                    LaunchedEffect(verse.chapterNo, verse.verseNo) {
                        verseScrollState.scrollTo(0)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(verseScrollState)
                            .padding(horizontal = 18.dp)
                            .graphicsLayer {
                                alpha = verseAnim
                                translationY = (1f - verseAnim) * 20f
                            }
                    ) {
                        Spacer(Modifier.height(16.dp))

                        ChapterVerseHeroCard(
                            chapter          = verse.chapterNo,
                            verse            = verse.verseNo,
                            combinedNos      = state.combinedVerseNos,
                            selectedLanguage = state.selectedLanguage,
                            onLanguageToggle = { lang -> viewModel.onEvent(NormalModeEvent.ToggleLanguage(lang)) },
                            onChapterTap     = { showChapterDialog = true },
                            onVerseTap       = { showVerseDialog = true }
                        )

                        Spacer(Modifier.height(20.dp))

                        IlluminatedVerseCard(text = sanitizeText(verse.verse))

                        val meaning = sanitizeText(verse.meaning)
                        if (meaning.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            MeaningCard(text = meaning)
                        }

                        val explanation = sanitizeText(verse.explanation)
                        if (explanation.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            ExplanationCard(text = explanation)
                        }

                        state.separatedVerseNote?.let { note ->
                            Spacer(Modifier.height(14.dp))
                            VerseNoteCard(note = note)
                        }

                        Spacer(Modifier.height(24.dp))
                    }

                    BottomActionBar(
                        isFavorite        = state.isFavorite,
                        favoriteMessage   = state.favoriteMessage,
                        canGoPrev         = !(verse.chapterNo == 1 && verse.verseNo == 1) && !state.isLoading,
                        canGoNext         = !(verse.chapterNo == 18 && verse.verseNo == 78) && !state.isLoading,
                        onFavoriteToggle  = { viewModel.onEvent(NormalModeEvent.ToggleFavorite) },
                        onShare           = { shareVerse(verse) },
                        onBattleQuiz      = onNavigateToQuizBattle,
                        onPrev            = { viewModel.onEvent(NormalModeEvent.PreviousVerse) },
                        onNext            = { viewModel.onEvent(NormalModeEvent.NextVerse) }
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
