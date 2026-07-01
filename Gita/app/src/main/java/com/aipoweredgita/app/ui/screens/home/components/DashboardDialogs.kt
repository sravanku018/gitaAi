package com.aipoweredgita.app.ui.screens.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.ui.screens.home.TodayStats
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.util.TimeUtils

@Composable
fun TodaySummaryDialog(
    onDismiss: () -> Unit,
    stats: TodayStats,
    onReadMore: () -> Unit = {},
    onTakeQuiz: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = GoldSpark)) { Text("Close") } },
        title = { Text("Today’s Summary", color = GoldSpark) },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Verses viewed: ${stats.verses}", color = MaterialTheme.colorScheme.onSurface)
                Text("Quizzes taken: ${stats.quizzes}", color = MaterialTheme.colorScheme.onSurface)
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
                Text("Normal Mode: ${TimeUtils.formatTime(stats.normalTime)}", color = MaterialTheme.colorScheme.onSurface)
                Text("Quiz Mode: ${TimeUtils.formatTime(stats.quizTime)}", color = MaterialTheme.colorScheme.onSurface)
                Text("Voice Studio: ${TimeUtils.formatTime(stats.studioTime)}", color = MaterialTheme.colorScheme.onSurface)

                if (stats.versesList.isNotEmpty()) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Text("Verses today:", color = GoldSpark)
                    val items = stats.versesList.take(10).joinToString { "${it.chapterNo}:${it.verseNo}" }
                    Text(items, color = MaterialTheme.colorScheme.onSurface)
                    if (stats.versesList.size > 10) Text("…and ${stats.versesList.size - 10} more", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
