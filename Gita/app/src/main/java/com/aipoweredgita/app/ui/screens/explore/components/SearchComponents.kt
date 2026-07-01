package com.aipoweredgita.app.ui.screens.explore.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.database.SearchResult
import com.aipoweredgita.app.ui.theme.GoldSpark

@Composable
fun SearchResultCard(
    result: SearchResult,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Chapter ${result.chapterNo}, Verse ${result.verseNo}",
                style = MaterialTheme.typography.labelMedium,
                color = GoldSpark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.verse,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = result.translation,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 3,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
