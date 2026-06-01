package com.aipoweredgita.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
<<<<<<< HEAD
import androidx.compose.foundation.shape.RoundedCornerShape
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aipoweredgita.app.R
import com.aipoweredgita.app.database.FavoriteVerse
import com.aipoweredgita.app.viewmodel.FavoritesViewModel
import com.aipoweredgita.app.ui.LocalUiConfig
<<<<<<< HEAD
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.aipoweredgita.app.ui.theme.*
import androidx.compose.foundation.background
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier,
    viewModel: FavoritesViewModel = viewModel(),
    onVerseClick: (Int, Int) -> Unit = { _, _ -> }
) {
    val state by viewModel.state.collectAsState()
    var showClearDialog by remember { mutableStateOf(false) }
    val uiCfg = LocalUiConfig.current

<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val appBg = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val textTertiary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
    val textItalicHint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(appBg)
    ) {
        if (isDark) {
            com.aipoweredgita.app.ui.components.AmbientOrbs(modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Favorite Verses",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                if (state.favorites.isNotEmpty()) {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text("Clear All", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Favorite count
            Text(
                text = "${state.favoriteCount} verse${if (state.favoriteCount != 1) "s" else ""} saved",
                style = MaterialTheme.typography.bodyMedium,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(16.dp))
=======
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(if (uiCfg.isLandscape) 24.dp else 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Favorite Verses",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            if (state.favorites.isNotEmpty()) {
                TextButton(onClick = { showClearDialog = true }) {
                    Text("Clear All", color = MaterialTheme.colorScheme.error)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Favorite count
        Text(
            text = "${state.favoriteCount} verse${if (state.favoriteCount != 1) "s" else ""} saved",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

        // Message display
        state.message?.let { message ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = message,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.favorites.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Outlined.FavoriteBorder,
                            contentDescription = "No favorites icon",
                            modifier = Modifier.size(64.dp),
<<<<<<< HEAD
                            tint = textTertiary
=======
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Favorite Verses Yet",
                            style = MaterialTheme.typography.titleLarge,
<<<<<<< HEAD
                            color = textSecondary
=======
                            color = MaterialTheme.colorScheme.onSurfaceVariant
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Add verses to favorites to see them here",
                            style = MaterialTheme.typography.bodyMedium,
<<<<<<< HEAD
                            color = textItalicHint,
=======
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.favorites,
                        key = { fav: com.aipoweredgita.app.database.FavoriteVerse -> fav.id }
                    ) { favorite ->
                        FavoriteVerseCard(
                            favorite = favorite,
                            onDelete = { viewModel.deleteFavorite(favorite.chapterNo, favorite.verseNo) },
                            onClick = { onVerseClick(favorite.chapterNo, favorite.verseNo) }
                        )
                    }
                }
            }
        }
    }
<<<<<<< HEAD
    }
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

    // Clear all confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear All Favorites?") },
            text = { Text("This will remove all ${state.favoriteCount} favorite verses. This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllFavorites()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteVerseCard(
    favorite: FavoriteVerse,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
<<<<<<< HEAD
    val isDark = rememberThemeIsDark()
    val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val textPrimary = MaterialTheme.colorScheme.onSurface
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val gold = if (isDark) GoldSpark else Saffron
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    var showDeleteDialog by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(
<<<<<<< HEAD
            containerColor = cardBg
        ),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder)
=======
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with chapter and verse info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Chapter ${favorite.chapterNo} : Verse ${favorite.verseNo}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
<<<<<<< HEAD
                        color = gold
=======
                        color = MaterialTheme.colorScheme.primary
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    )
                    if (favorite.chapterName.isNotEmpty()) {
                        Text(
                            text = favorite.chapterName,
                            style = MaterialTheme.typography.bodySmall,
<<<<<<< HEAD
                            color = textSecondary
=======
                            color = MaterialTheme.colorScheme.onSurfaceVariant
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        )
                    }
                }

                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Filled.Favorite,
                        contentDescription = "Remove from favorites",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Verse text (first 2 lines preview)
            Text(
                text = favorite.verse.take(150) + if (favorite.verse.length > 150) "..." else "",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
<<<<<<< HEAD
                color = textPrimary,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                textAlign = TextAlign.Justify
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
<<<<<<< HEAD
                HorizontalDivider(color = cardBorder)
=======
                HorizontalDivider()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                Spacer(modifier = Modifier.height(12.dp))

                // Translation/Meaning
                if (favorite.translation.isNotEmpty()) {
                    Text(
                        text = "Meaning",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
<<<<<<< HEAD
                        color = gold
=======
                        color = MaterialTheme.colorScheme.primary
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = favorite.translation,
                        style = MaterialTheme.typography.bodyMedium,
<<<<<<< HEAD
                        color = textPrimary,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        textAlign = TextAlign.Justify
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Explanation
                if (favorite.explanation.isNotEmpty()) {
                    Text(
                        text = "Explanation",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
<<<<<<< HEAD
                        color = gold
=======
                        color = MaterialTheme.colorScheme.primary
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = favorite.explanation,
                        style = MaterialTheme.typography.bodyMedium,
<<<<<<< HEAD
                        color = textPrimary,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        textAlign = TextAlign.Justify
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Go to verse button
                OutlinedButton(
                    onClick = onClick,
<<<<<<< HEAD
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = gold),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gold.copy(alpha = 0.5f))
=======
                    modifier = Modifier.fillMaxWidth()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                ) {
                    Text("View Full Verse")
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Remove from Favorites?") },
            text = { Text("Remove Chapter ${favorite.chapterNo}, Verse ${favorite.verseNo} from favorites?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
