package com.aipoweredgita.app.ui

<<<<<<< HEAD
import android.widget.Toast
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
<<<<<<< HEAD
=======
import androidx.compose.foundation.isSystemInDarkTheme
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
<<<<<<< HEAD
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
<<<<<<< HEAD
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
=======
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
<<<<<<< HEAD
import com.aipoweredgita.app.ui.theme.*
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import com.aipoweredgita.app.util.TextUtils.sanitizeText
import com.aipoweredgita.app.viewmodel.NormalModeViewModel
import com.aipoweredgita.app.viewmodel.ScreenConfigViewModel

<<<<<<< HEAD
=======
// ── Brand palette ──────────────────────────────────────────────────────────
private val Saffron       = Color(0xFFE8600A)
private val SaffronLight  = Color(0xFFF0B940)
private val Gold          = Color(0xFFC8922A)
private val GoldPale      = Color(0xFFF5DFA0)
private val InkDeep       = Color(0xFF0C0800)
private val InkSoft       = Color(0xFF3D2E14)
private val Cream         = Color(0xFFFDF6E8)
private val ParchmentDark = Color(0xFFEDE0C4)
private val Forest        = Color(0xFF2D5016)
private val ForestMid     = Color(0xFF3E6B21)
private val CrimsonDeep   = Color(0xFF8B1A1A)
private val PurpleDeep    = Color(0xFF4A148C)

>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
// ── Chapter metadata ───────────────────────────────────────────────────────
private val chapterNames = mapOf(
    1 to "Arjuna's Dilemma",       2 to "Sānkhya Yoga",
    3 to "Karma Yoga",             4 to "Jñāna Yoga",
    5 to "Karma Sannyāsa Yoga",    6 to "Dhyāna Yoga",
    7 to "Jñāna Vijñāna Yoga",    8 to "Akshara Brahma Yoga",
    9 to "Rāja Vidyā Yoga",       10 to "Vibhūti Yoga",
    11 to "Vishwarūpa Darshana",  12 to "Bhakti Yoga",
    13 to "Kshetra Kshetrajña",   14 to "Gunatraya Vibhāga",
    15 to "Purushottama Yoga",    16 to "Daivāsura Sampad",
    17 to "Shraddhatraya Yoga",   18 to "Moksha Sannyāsa Yoga"
)

private val chapterVerseCounts = mapOf(
    1 to 47, 2 to 72, 3 to 43, 4 to 42, 5 to 29, 6 to 47,
    7 to 30, 8 to 28, 9 to 34, 10 to 42, 11 to 55, 12 to 20,
    13 to 34, 14 to 27, 15 to 20, 16 to 24, 17 to 28, 18 to 78
)

// ═══════════════════════════════════════════════════════════════════════════
//  SCREEN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun NormalModeScreen(
    modifier: Modifier = Modifier,
    viewModel: NormalModeViewModel = viewModel(),
    screenConfigViewModel: ScreenConfigViewModel = viewModel(),
    onReadOfflineClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showChapterDialog by remember { mutableStateOf(false) }
    var showVerseDialog   by remember { mutableStateOf(false) }
    val context = LocalContext.current

<<<<<<< HEAD
    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
                }, "Share verse via"
            )
        )
    }

<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    com.aipoweredgita.app.ui.background.AppBackground(
        pattern = com.aipoweredgita.app.ui.background.BgPattern.ORBS_MANDALA,
        intensity = 0.4f,
        isDark = isDark
=======
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Offline banner ─────────────────────────────────────────────
            AnimatedVisibility(
                visible = !isNetworkAvailable,
                enter = slideInVertically() + fadeIn(),
                exit  = slideOutVertically() + fadeOut()
            ) {
                OfflineBanner(onReadOfflineClick = onReadOfflineClick)
            }

            // ── Main content ───────────────────────────────────────────────
            when {
                state.isLoading       -> GitaLoadingScreen()
                state.error != null   -> GitaErrorScreen(
                    message = state.error ?: "",
                    onRetry = { viewModel.loadVerse(state.currentChapter, state.currentVerse) }
                )
                state.verse != null   -> {
                    val verse = state.verse!!

<<<<<<< HEAD
                    val verseAnim by animateFloatAsState(
                        targetValue = if (state.verse != null) 1f else 0f,
                        animationSpec = com.aipoweredgita.app.ui.theme.MotionTokens.springExpressive<Float>(),
                        label = "verse_enter"
                    )
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp)
<<<<<<< HEAD
                            .graphicsLayer {
                                alpha = verseAnim
                                translationY = (1f - verseAnim) * 20f
                            }
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    ) {
                        Spacer(Modifier.height(16.dp))

                        // ── 1. Cinematic chapter · verse hero ──────────────
                        ChapterVerseHeroCard(
                            chapter      = verse.chapterNo,
                            verse        = verse.verseNo,
                            combinedNos  = state.combinedVerseNos,
                            onChapterTap = { showChapterDialog = true },
                            onVerseTap   = { showVerseDialog = true }
                        )

                        Spacer(Modifier.height(20.dp))

                        // ── 2. Illuminated verse card ──────────────────────
                        IlluminatedVerseCard(text = sanitizeText(verse.verse))

                        // ── 3. Meaning card ────────────────────────────────
                        val meaning = sanitizeText(verse.meaning)
                        if (meaning.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            MeaningCard(text = meaning)
                        }

                        // ── 4. Explanation card ────────────────────────────
                        val explanation = sanitizeText(verse.explanation)
                        if (explanation.isNotBlank()) {
                            Spacer(Modifier.height(14.dp))
                            ExplanationCard(text = explanation)
                        }

                        // ── 5. Combined-verse note ─────────────────────────
                        state.separatedVerseNote?.let { note ->
                            Spacer(Modifier.height(14.dp))
                            VerseNoteCard(note = note)
                        }

                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Bottom action bar ──────────────────────────────────
                    BottomActionBar(
                        isFavorite        = state.isFavorite,
                        favoriteMessage   = state.favoriteMessage,
                        canGoPrev         = !(verse.chapterNo == 1 && verse.verseNo == 1) && !state.isLoading,
                        canGoNext         = !(verse.chapterNo == 18 && verse.verseNo == 78) && !state.isLoading,
                        onFavoriteToggle  = { viewModel.toggleFavorite() },
                        onShare           = { shareVerse(verse) },
                        onPrev            = { viewModel.previousVerse() },
                        onNext            = { viewModel.nextVerse() }
                    )
                }
            }
        }
    }

    // ── Dialogs ────────────────────────────────────────────────────────────
    if (showChapterDialog) {
        ChapterSelectionDialog(
            currentChapter    = state.currentChapter,
            onDismiss         = { showChapterDialog = false },
            onChapterSelected = { ch -> viewModel.goToChapter(ch); showChapterDialog = false }
        )
    }
    if (showVerseDialog && state.verse != null) {
        VerseSelectionDialog(
            currentChapter  = state.verse!!.chapterNo,
            currentVerse    = state.verse!!.verseNo,
            maxVerses       = chapterVerseCounts[state.verse!!.chapterNo] ?: 47,
            combinedGroups  = state.combinedGroups,
            onDismiss       = { showVerseDialog = false },
            onVerseSelected = { v ->
                viewModel.loadVerse(state.verse!!.chapterNo, v)
                showVerseDialog = false
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  1. CINEMATIC CHAPTER · VERSE HERO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ChapterVerseHeroCard(
    chapter: Int,
    verse: Int,
    combinedNos: List<Int>,
    onChapterTap: () -> Unit,
    onVerseTap: () -> Unit
) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textTertiary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val textItalicHint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    val verseDisplay = if (combinedNos.size > 1) {
        "${combinedNos.minOrNull()}–${combinedNos.maxOrNull()}"
    } else verse.toString()

    val chapterName = chapterNames[chapter] ?: ""

<<<<<<< HEAD
    val numberColor = if (isDark) GoldSpark else Saffron

    val accentColor = Saffron
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
            .shadow(4.dp, MaterialTheme.shapes.extraLarge)
=======
    // Animated gold shimmer on the number
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000), RepeatMode.Reverse),
        label = "gold_shimmer"
    )
    val numberColor = Color(
        red   = (Gold.red   + (SaffronLight.red   - Gold.red)   * shimmer).coerceIn(0f, 1f),
        green = (Gold.green + (SaffronLight.green - Gold.green) * shimmer).coerceIn(0f, 1f),
        blue  = (Gold.blue  + (SaffronLight.blue  - Gold.blue)  * shimmer).coerceIn(0f, 1f)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            .drawBehind {
                // Subtle warm glow bottom-left
                drawCircle(
                    brush = Brush.radialGradient(
<<<<<<< HEAD
                        listOf(accentColor.copy(alpha = 0.18f), Color.Transparent),
=======
                        listOf(Saffron.copy(alpha = 0.18f), Color.Transparent),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        center = Offset(0f, size.height),
                        radius = size.width * 0.7f
                    ),
                    radius = size.width * 0.7f,
                    center = Offset(0f, size.height)
                )
<<<<<<< HEAD
=======
                // Forest glow top-right
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Forest.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(size.width, 0f),
                        radius = size.width * 0.55f
                    ),
                    radius = size.width * 0.55f,
                    center = Offset(size.width, 0f)
                )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            }
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Chapter name label
            Text(
                text     = "Chapter $chapter · $chapterName",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp,
<<<<<<< HEAD
                color    = if (isDark) GoldSpark.copy(alpha = 0.7f) else Saffron
=======
                color    = Gold.copy(alpha = 0.7f)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )

            Spacer(Modifier.height(14.dp))

            // Large chapter : verse display
            Row(
                modifier            = Modifier.fillMaxWidth(),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Chapter block
                Column(
                    modifier = Modifier
<<<<<<< HEAD
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
=======
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        .clickable(onClick = onChapterTap)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "CHAPTER",
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
<<<<<<< HEAD
                        color = textTertiary,
=======
                        color = Cream.copy(alpha = 0.4f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = chapter.toString(),
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = numberColor,
                        lineHeight = 52.sp
                    )
                }

                // Ornamental separator
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == 2) 6.dp else 3.dp)
                                .clip(CircleShape)
<<<<<<< HEAD
                                .background(numberColor.copy(alpha = if (i == 2) 0.8f else 0.3f))
=======
                                .background(Gold.copy(alpha = if (i == 2) 0.8f else 0.3f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                        if (i < 4) Spacer(Modifier.height(4.dp))
                    }
                }

                // Verse block
                Column(
                    modifier = Modifier
<<<<<<< HEAD
                        .clip(MaterialTheme.shapes.large)
                        .background(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.04f))
=======
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.06f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        .clickable(onClick = onVerseTap)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "VERSE",
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
<<<<<<< HEAD
                        color = textTertiary,
=======
                        color = Cream.copy(alpha = 0.4f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text  = verseDisplay,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = numberColor,
                        lineHeight = 52.sp
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Gold ornamental rule
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
<<<<<<< HEAD
                            listOf(Color.Transparent, numberColor.copy(0.6f), Color.Transparent)
=======
                            listOf(Color.Transparent, Gold.copy(0.6f), Color.Transparent)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                    )
            )

            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text      = "ॐ",
                    fontSize  = 16.sp,
<<<<<<< HEAD
                    color     = numberColor,
=======
                    color     = Gold,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    fontStyle = FontStyle.Normal
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text      = "Tap chapter or verse to navigate",
                    fontSize  = 11.sp,
<<<<<<< HEAD
                    color     = textItalicHint,
=======
                    color     = Cream.copy(alpha = 0.35f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  2. ILLUMINATED VERSE CARD
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun IlluminatedVerseCard(text: String) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron
    val primary = Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(gold.copy(0.6f), primary.copy(0.4f), gold.copy(0.6f))),
                shape = MaterialTheme.shapes.extraLarge
            )
            .background(cardBg)
            .shadow(6.dp, MaterialTheme.shapes.extraLarge)
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.02f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.45f
                    )
                )
            }
=======
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(Gold.copy(0.6f), Saffron.copy(0.4f), Gold.copy(0.6f))),
                shape = RoundedCornerShape(20.dp)
            )
            .background(
                Brush.verticalGradient(
                    if (isSystemInDarkTheme()) {
                        listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surface)
                    } else {
                        listOf(Color(0xFFFFF9F0), Color(0xFFFDF3DC))
                    }
                )
            )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    ) {
        // Left gold accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
<<<<<<< HEAD
                    Brush.verticalGradient(listOf(gold, primary, gold))
=======
                    Brush.verticalGradient(listOf(Gold, Saffron, Gold))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
        )

        Column(modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
<<<<<<< HEAD
                        .clip(MaterialTheme.shapes.small)
                        .background(gold.copy(alpha = 0.15f))
=======
                        .clip(RoundedCornerShape(8.dp))
                        .background(Gold.copy(alpha = 0.15f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = "SHLOKA",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
<<<<<<< HEAD
                        color      = gold
=======
                        color      = InkSoft
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    )
                }
                Spacer(Modifier.width(10.dp))
                // Dotted rule
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
<<<<<<< HEAD
                                listOf(gold.copy(0.4f), Color.Transparent)
=======
                                listOf(Gold.copy(0.4f), Color.Transparent)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            )
                        )
                )
                Spacer(Modifier.width(8.dp))
<<<<<<< HEAD
                Text("✦", fontSize = 10.sp, color = gold.copy(0.6f))
=======
                Text("✦", fontSize = 10.sp, color = Gold.copy(0.6f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            }

            Spacer(Modifier.height(14.dp))

            // Verse text in serif italic
            Text(
                text       = text,
                fontSize   = 16.sp,
                fontStyle  = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                lineHeight = 28.sp,
<<<<<<< HEAD
                color      = textPrimary,
=======
                color      = MaterialTheme.colorScheme.onSurface,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                textAlign  = TextAlign.Justify
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  3. MEANING CARD — Forest green tonal
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun MeaningCard(text: String) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val greenAccent = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
            .shadow(2.dp, MaterialTheme.shapes.extraLarge)
=======
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFEAF3DE))
            .border(1.dp, ForestMid.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    ) {
        // Left stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
<<<<<<< HEAD
                .background(greenAccent)
=======
                .background(ForestMid)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Filled.Favorite,
                    contentDescription = null,
<<<<<<< HEAD
                    tint               = greenAccent,
=======
                    tint               = ForestMid,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    modifier           = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = "MEANING",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
<<<<<<< HEAD
                    color      = greenAccent
=======
                    color      = Forest
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text      = text,
                fontSize  = 14.sp,
                lineHeight = 24.sp,
<<<<<<< HEAD
                color     = textPrimary,
=======
                color     = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.onSurface else Color(0xFF173404),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                textAlign = TextAlign.Justify
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  4. EXPLANATION CARD — Deep ink with warm parchment tones
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun ExplanationCard(text: String) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
            .shadow(2.dp, MaterialTheme.shapes.extraLarge)
=======
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (isSystemInDarkTheme()) 
                    MaterialTheme.colorScheme.surface 
                else 
                    Color(0xFFFDF6EC) // Warm light parchment
            )
            .border(
                1.dp, 
                if (isSystemInDarkTheme()) Color.Transparent else Gold.copy(0.2f), 
                RoundedCornerShape(18.dp)
            )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            .padding(20.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
<<<<<<< HEAD
=======
            // Small decorative diamond row
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(1.dp))
<<<<<<< HEAD
                        .background(gold.copy(0.7f))
=======
                        .background(Gold.copy(0.7f))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
                if (it < 2) Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "COMMENTARY",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
<<<<<<< HEAD
                color      = gold
=======
                color      = if (isSystemInDarkTheme()) Gold.copy(0.8f) else Saffron
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )
        }

        Spacer(Modifier.height(4.dp))

        // Gold rule
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
<<<<<<< HEAD
                    Brush.horizontalGradient(listOf(gold.copy(0.5f), Color.Transparent))
=======
                    Brush.horizontalGradient(listOf(Gold.copy(0.5f), Color.Transparent))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text      = text,
            fontSize  = 14.sp,
            lineHeight = 24.sp,
<<<<<<< HEAD
            color     = textPrimary,
=======
            color     = if (isSystemInDarkTheme()) GoldPale.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            textAlign = TextAlign.Justify
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  5. COMBINED VERSE NOTE CARD
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun VerseNoteCard(note: String) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Saffron
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val textSecondary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.medium)
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("◆", fontSize = 12.sp, color = gold, modifier = Modifier.padding(top = 2.dp))
=======
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFAEEDA))
            .border(1.dp, Gold.copy(0.4f), RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text("◆", fontSize = 12.sp, color = Gold, modifier = Modifier.padding(top = 2.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        Spacer(Modifier.width(10.dp))
        Text(
            text      = note,
            fontSize  = 12.sp,
            lineHeight = 20.sp,
<<<<<<< HEAD
            color     = textSecondary,
=======
            color     = InkSoft,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            fontStyle = FontStyle.Italic
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  OFFLINE BANNER
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun OfflineBanner(onReadOfflineClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CrimsonDeep)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Text(
            text       = "No internet connection",
            fontSize   = 13.sp,
            fontWeight = FontWeight.Medium,
            color      = Color(0xFFFFCDD2)
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = onReadOfflineClick,
            modifier = Modifier
<<<<<<< HEAD
                .clip(MaterialTheme.shapes.small)
=======
                .clip(RoundedCornerShape(8.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                .background(Color.White.copy(0.12f))
        ) {
            Text(
                text  = "Read offline →",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  BOTTOM ACTION BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun BottomActionBar(
    isFavorite      : Boolean,
    favoriteMessage : String?,
    canGoPrev       : Boolean,
    canGoNext       : Boolean,
    onFavoriteToggle: () -> Unit,
    onShare         : () -> Unit,
    onPrev          : () -> Unit,
    onNext          : () -> Unit
) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier.border(0.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(0.dp))
=======
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .navigationBarsPadding()
        ) {
            // Favorite + Share row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Favourite button — saffron gradient when active
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
<<<<<<< HEAD
                        .clip(MaterialTheme.shapes.medium)
=======
                        .clip(RoundedCornerShape(14.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        .background(
                            if (isFavorite)
                                Brush.horizontalGradient(listOf(CrimsonDeep, Color(0xFFC62828)))
                            else
<<<<<<< HEAD
                                Brush.horizontalGradient(listOf(GoldSpark, Saffron))
=======
                                Brush.horizontalGradient(listOf(Gold, Saffron))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                        .clickable(onClick = onFavoriteToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector        = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint               = Color.White,
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text       = if (isFavorite) "Saved" else "Save",
                            color      = Color.White,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Share button — outlined
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
<<<<<<< HEAD
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
=======
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, Gold.copy(0.6f), RoundedCornerShape(14.dp))
                        .background(Color.Transparent)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        .clickable(onClick = onShare),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            imageVector        = Icons.Filled.Share,
                            contentDescription = null,
<<<<<<< HEAD
                            tint               = MaterialTheme.colorScheme.onSurface,
=======
                            tint               = InkSoft,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            modifier           = Modifier.size(18.dp)
                        )
                        Text(
                            text       = "Share",
                            color      = MaterialTheme.colorScheme.onSurface,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Fav confirmation message
<<<<<<< HEAD
            val msgColor = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            AnimatedVisibility(
                visible = !favoriteMessage.isNullOrBlank(),
                enter   = fadeIn() + slideInVertically(),
                exit    = fadeOut()
            ) {
                Text(
                    text      = favoriteMessage ?: "",
                    fontSize  = 12.sp,
<<<<<<< HEAD
                    color     = msgColor,
=======
                    color     = ForestMid,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.fillMaxWidth().padding(top = 6.dp),
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(Modifier.height(12.dp))

            // Navigation row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                NavArrowButton(
                    label     = "Previous",
                    enabled   = canGoPrev,
                    isForward = false,
                    modifier  = Modifier.weight(1f),
                    onClick   = onPrev
                )
                NavArrowButton(
                    label     = "Next",
                    enabled   = canGoNext,
                    isForward = true,
                    modifier  = Modifier.weight(1f),
                    onClick   = onNext
                )
            }
        }
    }
}

@Composable
private fun NavArrowButton(
    label    : String,
    enabled  : Boolean,
    isForward: Boolean,
    modifier : Modifier = Modifier,
    onClick  : () -> Unit
) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    val alpha = if (enabled) 1f else 0.35f
    Row(
        modifier = modifier
            .height(44.dp)
<<<<<<< HEAD
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f * alpha))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f * alpha), MaterialTheme.shapes.medium)
=======
            .clip(RoundedCornerShape(12.dp))
            .background(ParchmentDark.copy(alpha = alpha))
            .border(1.dp, Gold.copy(alpha = 0.3f * alpha), RoundedCornerShape(12.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (!isForward) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
                modifier           = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text       = label,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
<<<<<<< HEAD
            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
=======
            color      = InkSoft.copy(alpha = alpha)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        )
        if (isForward) {
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
<<<<<<< HEAD
                tint               = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
=======
                tint               = InkSoft.copy(alpha = alpha),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  LOADING / ERROR SCREENS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
private fun GitaLoadingScreen() {
    val pulse by rememberInfiniteTransition(label = "load").animateFloat(
        0.4f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("ॐ", fontSize = 52.sp, color = Gold.copy(alpha = pulse))
            Spacer(Modifier.height(16.dp))
            Text(
                "Loading verse…",
                fontSize  = 14.sp,
<<<<<<< HEAD
                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
=======
                color     = InkSoft.copy(0.5f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                fontStyle = FontStyle.Italic
            )
        }
    }
}

@Composable
private fun GitaErrorScreen(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("⚠", fontSize = 40.sp, color = Saffron)
            Spacer(Modifier.height(12.dp))
<<<<<<< HEAD
            Text(message, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
=======
            Text(message, fontSize = 14.sp, color = InkSoft, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    .background(Brush.horizontalGradient(listOf(Gold, Saffron)))
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 28.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Retry", color = Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  DIALOGS
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun ChapterSelectionDialog(
    currentChapter   : Int,
    onDismiss        : () -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
<<<<<<< HEAD
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
=======
        containerColor   = Cream,
        shape            = RoundedCornerShape(24.dp),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        title = {
            Text(
                "Select Chapter",
                fontWeight = FontWeight.Bold,
<<<<<<< HEAD
                color      = MaterialTheme.colorScheme.onSurface
=======
                color      = InkDeep
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                (1..18).forEach { ch ->
                    val isActive = ch == currentChapter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
<<<<<<< HEAD
                            .clip(MaterialTheme.shapes.small)
=======
                            .clip(RoundedCornerShape(10.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            .background(if (isActive) Gold.copy(0.12f) else Color.Transparent)
                            .clickable { onChapterSelected(ch) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "$ch",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
<<<<<<< HEAD
                            color      = if (isActive) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
=======
                            color      = if (isActive) Gold else InkSoft.copy(0.4f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            modifier   = Modifier.width(28.dp)
                        )
                        Column {
                            Text(
                                text       = "Chapter $ch",
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
<<<<<<< HEAD
                                color      = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
=======
                                color      = if (isActive) InkDeep else InkSoft
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            )
                            Text(
                                text      = chapterNames[ch] ?: "",
                                fontSize  = 12.sp,
<<<<<<< HEAD
                                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f),
=======
                                color     = InkSoft.copy(0.55f),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                                fontStyle = FontStyle.Italic
                            )
                        }
                    }
                    if (ch < 18) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .background(Gold.copy(0.12f))
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Gold, fontWeight = FontWeight.Medium)
            }
        }
    )
}

@Composable
fun VerseSelectionDialog(
    currentChapter : Int,
    currentVerse   : Int,
    maxVerses      : Int,
    combinedGroups : List<List<Int>> = emptyList(),
    onDismiss      : () -> Unit,
    onVerseSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
<<<<<<< HEAD
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
=======
        containerColor   = Cream,
        shape            = RoundedCornerShape(24.dp),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        title = {
            Text(
                "Select Verse  ·  1–$maxVerses",
                fontWeight = FontWeight.Bold,
<<<<<<< HEAD
                color      = MaterialTheme.colorScheme.onSurface
=======
                color      = InkDeep
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                var i = 1
                while (i <= maxVerses) {
                    val group    = combinedGroups.firstOrNull { it.minOrNull() == i }
                    val label    : String
                    val target   : Int
                    if (group != null && group.size > 1) {
                        val s = group.minOrNull()!!
                        val e = group.maxOrNull()!!
                        label  = "Verses $s–$e"
                        target = i
                        i      = e + 1
                    } else {
                        label  = "Verse $i"
                        target = i
                        i++
                    }
                    val isActive = target == currentVerse
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
<<<<<<< HEAD
                            .clip(MaterialTheme.shapes.small)
=======
                            .clip(RoundedCornerShape(10.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            .background(if (isActive) Gold.copy(0.12f) else Color.Transparent)
                            .clickable { onVerseSelected(target) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text       = label,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
<<<<<<< HEAD
                            color      = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
=======
                            color      = if (isActive) InkDeep else InkSoft
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                        if (isActive) {
                            Text("✦", fontSize = 12.sp, color = Gold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Gold, fontWeight = FontWeight.Medium)
            }
        }
    )
<<<<<<< HEAD
}
=======
}
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
