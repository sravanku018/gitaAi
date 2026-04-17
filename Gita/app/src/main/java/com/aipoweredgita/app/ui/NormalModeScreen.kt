package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import com.aipoweredgita.app.util.TextUtils.sanitizeText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.viewmodel.NormalModeViewModel
import com.aipoweredgita.app.viewmodel.ScreenConfigViewModel
import com.aipoweredgita.app.utils.NetworkUtils

// Use central sanitizeText from TextUtils
@Composable
fun NormalModeScreen(
    modifier: Modifier = Modifier,
    viewModel: NormalModeViewModel = viewModel(),
    screenConfigViewModel: ScreenConfigViewModel = viewModel(),
    onReadOfflineClick: () -> Unit = {}
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showChapterDialog by remember { mutableStateOf(false) }
    var showVerseDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isNetworkAvailable by com.aipoweredgita.app.utils.NetworkUtils.networkStatusFlow(context).collectAsStateWithLifecycle(initialValue = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(context))
    
    // Share function
    fun shareVerse(verse: com.aipoweredgita.app.data.GitaVerse) {
        val shareText = buildString {
            appendLine("📖 Bhagavad Gita ${verse.chapterNo}:${verse.verseNo}")
            appendLine()
            appendLine(sanitizeText(verse.verse))
            appendLine()
            appendLine("#BhagavadGita #Gita #Wisdom")
        }
        
        val sendIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        
        val shareIntent = android.content.Intent.createChooser(sendIntent, "Share verse via")
        context.startActivity(shareIntent)
    }

    // Reactive network status is collected above; remove legacy polling

    val chapterVerseCounts = mapOf(
        1 to 47, 2 to 72, 3 to 43, 4 to 42, 5 to 29, 6 to 47,
        7 to 30, 8 to 28, 9 to 34, 10 to 42, 11 to 55, 12 to 20,
        13 to 34, 14 to 27, 15 to 20, 16 to 24, 17 to 28, 18 to 78
    )

    val screenConfig by screenConfigViewModel.screenConfig.collectAsState()
    val uiCfg = LocalUiConfig.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
    ) {
        // Network status indicator
        if (!isNetworkAvailable) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = com.aipoweredgita.app.R.string.error_no_internet),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onReadOfflineClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(stringResource(id = com.aipoweredgita.app.R.string.cta_read_offline))
            }
        }
        when {
            state.isLoading -> {
                LoadingScreen(message = stringResource(id = com.aipoweredgita.app.R.string.loading_verse))
            }

            state.error != null -> {
                ErrorScreen(
                    message = state.error ?: "",
                    onRetry = { viewModel.loadVerse(state.currentChapter, state.currentVerse) }
                )
            }

            state.verse != null -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Chapter and Verse Selectors
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Chapter Selector
                            TextButton(onClick = { showChapterDialog = true }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Chapter",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = "${state.verse!!.chapterNo}",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Text(
                                text = ":",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )

                            // Verse Selector (supports combined verses like 4,5,6)
                            TextButton(onClick = { showVerseDialog = true }) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "Verse",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    val combined = state.combinedVerseNos
                                    val displayNos = if (combined.size > 1) {
                                        val start = combined.minOrNull()
                                        val end = combined.maxOrNull()
                                        if (start != null && end != null) "${start}–${end}" else combined.joinToString(", ")
                                    } else "${state.verse!!.verseNo}"
                                    Text(
                                        text = displayNos,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Verse Text
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Verse",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = sanitizeText(state.verse!!.verse),
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Justify
                            )
                        }
                    }

                    // Meaning (shown only if available)
                    val meaningText = sanitizeText(state.verse!!.meaning)
                    if (meaningText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Meaning",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = meaningText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }

                    // Explanation (shown only if available)
                    val explanationText = sanitizeText(state.verse!!.explanation)
                    if (explanationText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Explanation",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = explanationText,
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }
                    
                    // Separated Verse Note (shown if this verse was originally combined)
                    state.separatedVerseNote?.let { note ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Justify
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Favorite Button
                Button(
                    onClick = { viewModel.toggleFavorite() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isFavorite)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (state.isFavorite)
                            androidx.compose.material.icons.Icons.Filled.Favorite
                        else
                            androidx.compose.material.icons.Icons.Outlined.FavoriteBorder,
                        contentDescription = if (state.isFavorite) "Remove from favorites" else "Add to favorites"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (state.isFavorite) "Remove from Favorites" else "Add to Favorites")
                }

                // Show favorite message
                state.favoriteMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                // Share Button
                OutlinedButton(
                    onClick = { state.verse?.let { shareVerse(it) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Share,
                        contentDescription = "Share verse"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Share Verse")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { viewModel.previousVerse() },
                        enabled = !state.isLoading && !(state.verse!!.chapterNo == 1 && state.verse!!.verseNo == 1),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Previous"
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = { viewModel.nextVerse() },
                        enabled = !state.isLoading && !(state.verse!!.chapterNo == 18 && state.verse!!.verseNo == 78),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Next")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next"
                        )
                    }
                }
            }
        }
    }

    if (showChapterDialog) {
        ChapterSelectionDialog(
            currentChapter = state.currentChapter,
            onDismiss = { showChapterDialog = false },
            onChapterSelected = { chapter ->
                viewModel.goToChapter(chapter)
                showChapterDialog = false
            }
        )
    }

    if (showVerseDialog && state.verse != null) {
        VerseSelectionDialog(
            currentChapter = state.verse?.chapterNo ?: state.currentChapter,
            currentVerse = state.verse?.verseNo ?: state.currentVerse,
            maxVerses = chapterVerseCounts[state.verse?.chapterNo ?: state.currentChapter] ?: 47,
            combinedVerseNos = state.combinedVerseNos,
            combinedGroups = state.combinedGroups,
            onDismiss = { showVerseDialog = false },
            onVerseSelected = { verse ->
                viewModel.loadVerse(state.verse?.chapterNo ?: state.currentChapter, verse)
                showVerseDialog = false
            }
        )
    }
}

@Composable
fun ChapterSelectionDialog(
    currentChapter: Int,
    onDismiss: () -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = com.aipoweredgita.app.R.string.select_chapter_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                (1..18).forEach { chapter ->
                    TextButton(
                        onClick = { onChapterSelected(chapter) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Chapter $chapter",
                            fontWeight = if (chapter == currentChapter) FontWeight.Bold else FontWeight.Normal,
                            color = if (chapter == currentChapter)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.aipoweredgita.app.R.string.generic_cancel))
            }
        }
    )
}

@Composable
fun VerseSelectionDialog(
    currentChapter: Int,
    currentVerse: Int,
    maxVerses: Int,
    combinedVerseNos: List<Int> = emptyList(),
    combinedGroups: List<List<Int>> = emptyList(),
    onDismiss: () -> Unit,
    onVerseSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = com.aipoweredgita.app.R.string.select_verse_title, maxVerses)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Build picker list from combined groups so user selects groups where applicable
                val groups = combinedGroups
                var i = 1
                while (i <= maxVerses) {
                    val group = groups.firstOrNull { it.minOrNull() == i }
                    val label: String
                    val targetVerse: Int
                    if (group != null && group.size > 1) {
                        val start = group.minOrNull()
                        val end = group.maxOrNull()
                        val isContiguous = group.sorted().zipWithNext().all { (a, b) -> b == a + 1 }
                        label = when {
                            start != null && end != null && isContiguous && group.size > 1 ->
                                stringResource(id = com.aipoweredgita.app.R.string.verses_range, start, end)
                            else ->
                                stringResource(id = com.aipoweredgita.app.R.string.verses_list, group.joinToString(", "))
                        }
                        targetVerse = i
                        i = (group.maxOrNull() ?: i) + 1
                    } else {
                        label = "Verse $i"
                        targetVerse = i
                        i++
                    }

                    TextButton(
                        onClick = { onVerseSelected(targetVerse) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = label,
                            fontWeight = if (targetVerse == currentVerse) FontWeight.Bold else FontWeight.Normal,
                            color = if (targetVerse == currentVerse)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = com.aipoweredgita.app.R.string.generic_cancel))
            }
        }
    )
}

