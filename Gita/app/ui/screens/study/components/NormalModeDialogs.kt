package com.aipoweredgita.app.ui.screens.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.Gold

@Composable
fun ChapterSelectionDialog(
    currentChapter   : Int,
    onDismiss        : () -> Unit,
    onChapterSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                "Select Chapter",
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
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
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isActive) Gold.copy(0.12f) else Color.Transparent)
                            .clickable { onChapterSelected(ch) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "$ch",
                            fontWeight = FontWeight.Bold,
                            fontSize   = 15.sp,
                            color      = if (isActive) Gold else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                            modifier   = Modifier.width(28.dp)
                        )
                        Column {
                            Text(
                                text       = "Chapter $ch",
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color      = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text      = normalModeChapterNames[ch] ?: "",
                                fontSize  = 12.sp,
                                color     = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.55f),
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
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                "Select Verse  ·  1–$maxVerses",
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface
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
                            .clip(MaterialTheme.shapes.small)
                            .background(if (isActive) Gold.copy(0.12f) else Color.Transparent)
                            .clickable { onVerseSelected(target) }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text       = label,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
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
}
