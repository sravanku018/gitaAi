package com.aipoweredgita.app.ui.screens.study.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.util.TextUtils.sanitizeText
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Shared verse body used by buttons / swipe / scroll modes.
 */
@Composable
fun SlokaVerseBody(
    verse: GitaVerse,
    combinedNos: List<Int> = emptyList(),
    selectedLanguage: String,
    isSpeaking: Boolean,
    separatedNote: String?,
    onLanguageToggle: (String) -> Unit,
    onTtsToggle: () -> Unit,
    onChapterTap: () -> Unit,
    onVerseTap: () -> Unit,
    modifier: Modifier = Modifier,
    showHero: Boolean = true,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (showHero) {
            ChapterVerseHeroCard(
                chapter = verse.chapterNo,
                verse = verse.verseNo,
                combinedNos = combinedNos,
                selectedLanguage = selectedLanguage,
                isSpeaking = isSpeaking,
                onLanguageToggle = onLanguageToggle,
                onTtsToggle = onTtsToggle,
                onChapterTap = onChapterTap,
                onVerseTap = onVerseTap,
            )
            Spacer(Modifier.height(20.dp))
        } else {
            Text(
                text = "Sloka ${verse.verseNo}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 10.dp),
            )
        }

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

        if (!separatedNote.isNullOrBlank()) {
            Spacer(Modifier.height(14.dp))
            VerseNoteCard(note = separatedNote)
        }
    }
}

/**
 * Horizontal swipe between slokas in the **current chapter only**.
 */
@Composable
fun SlokaSwipeReader(
    chapter: Int,
    currentVerseNo: Int,
    chapterVerses: List<GitaVerse>,
    isLoadingChapter: Boolean,
    selectedLanguage: String,
    isSpeaking: Boolean,
    separatedNote: String?,
    combinedNos: List<Int>,
    verseAnim: Float,
    onVerseSettled: (Int) -> Unit,
    onLanguageToggle: (String) -> Unit,
    onTtsToggle: (GitaVerse) -> Unit,
    onChapterTap: () -> Unit,
    onVerseTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val pageCount = chapterVerses.size.coerceAtLeast(1)
    val initialPage = (currentVerseNo - 1).coerceIn(0, pageCount - 1)
    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount },
    )

    // Keep pager in sync when chapter / jump-to-verse changes
    LaunchedEffect(chapter, currentVerseNo, pageCount) {
        val target = (currentVerseNo - 1).coerceIn(0, pageCount - 1)
        if (pagerState.currentPage != target) {
            pagerState.scrollToPage(target)
        }
    }

    LaunchedEffect(pagerState, chapter) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val verseNo = page + 1
                if (verseNo != currentVerseNo) onVerseSettled(verseNo)
            }
    }

    when {
        isLoadingChapter && chapterVerses.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        chapterVerses.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No slokas loaded for this chapter", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> {
            HorizontalPager(
                state = pagerState,
                modifier = modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
            ) { page ->
                val verse = chapterVerses.getOrNull(page) ?: return@HorizontalPager
                val scroll = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scroll)
                        .padding(horizontal = 18.dp)
                        .graphicsLayer {
                            alpha = verseAnim
                            translationY = (1f - verseAnim) * 20f
                        },
                ) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Swipe for next sloka · Chapter $chapter",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(8.dp))
                    SlokaVerseBody(
                        verse = verse,
                        combinedNos = if (verse.verseNo == currentVerseNo) combinedNos else emptyList(),
                        selectedLanguage = selectedLanguage,
                        isSpeaking = isSpeaking && verse.verseNo == currentVerseNo,
                        separatedNote = if (verse.verseNo == currentVerseNo) separatedNote else null,
                        onLanguageToggle = onLanguageToggle,
                        onTtsToggle = { onTtsToggle(verse) },
                        onChapterTap = onChapterTap,
                        onVerseTap = onVerseTap,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * Continuous vertical scroll through slokas in the **current chapter only**.
 */
@Composable
fun SlokaScrollReader(
    chapter: Int,
    currentVerseNo: Int,
    chapterVerses: List<GitaVerse>,
    isLoadingChapter: Boolean,
    selectedLanguage: String,
    isSpeaking: Boolean,
    separatedNote: String?,
    combinedNos: List<Int>,
    onVerseVisible: (Int) -> Unit,
    onLanguageToggle: (String) -> Unit,
    onTtsToggle: (GitaVerse) -> Unit,
    onChapterTap: () -> Unit,
    onVerseTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (currentVerseNo - 1).coerceAtLeast(0),
    )

    LaunchedEffect(chapter, currentVerseNo, chapterVerses.size) {
        val target = (currentVerseNo - 1).coerceIn(0, (chapterVerses.size - 1).coerceAtLeast(0))
        if (chapterVerses.isNotEmpty() && listState.firstVisibleItemIndex != target) {
            listState.scrollToItem(target)
        }
    }

    LaunchedEffect(listState, chapter) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val verseNo = index + 1
                if (verseNo != currentVerseNo) onVerseVisible(verseNo)
            }
    }

    when {
        isLoadingChapter && chapterVerses.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        chapterVerses.isEmpty() -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No slokas loaded for this chapter", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                item {
                    Text(
                        text = "Scroll through slokas · Chapter $chapter",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    // Chapter picker stays on the first hero only
                    chapterVerses.firstOrNull()?.let { first ->
                        Spacer(Modifier.height(8.dp))
                        ChapterVerseHeroCard(
                            chapter = chapter,
                            verse = currentVerseNo,
                            combinedNos = combinedNos,
                            selectedLanguage = selectedLanguage,
                            isSpeaking = isSpeaking,
                            onLanguageToggle = onLanguageToggle,
                            onTtsToggle = {
                                chapterVerses.find { it.verseNo == currentVerseNo }?.let(onTtsToggle)
                            },
                            onChapterTap = onChapterTap,
                            onVerseTap = onVerseTap,
                        )
                    }
                }
                itemsIndexed(chapterVerses, key = { _, v -> "${v.chapterNo}:${v.verseNo}" }) { _, verse ->
                    SlokaVerseBody(
                        verse = verse,
                        combinedNos = if (verse.verseNo == currentVerseNo) combinedNos else emptyList(),
                        selectedLanguage = selectedLanguage,
                        isSpeaking = isSpeaking && verse.verseNo == currentVerseNo,
                        separatedNote = if (verse.verseNo == currentVerseNo) separatedNote else null,
                        onLanguageToggle = onLanguageToggle,
                        onTtsToggle = { onTtsToggle(verse) },
                        onChapterTap = onChapterTap,
                        onVerseTap = onVerseTap,
                        showHero = false,
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
        }
    }
}
