package com.aipoweredgita.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 18 chapters of the Gita, each with a unique typographic treatment
 * using Devanagari numerals (१-१८) and Sanskrit titles.
 */
data class ChapterInfo(
    val number: Int,
    val title: String,
    val sanskritTitle: String,
    val devanagariNumeral: String,
    val summary: String,
    val verseCount: Int,
    val fontSizeMultiplier: Float = 1f  // 1.0-1.8 for visual variety
)

private val devanagariNumerals = listOf(
    "१", "२", "३", "४", "५", "६", "७", "८", "९", "१०",
    "११", "१२", "१३", "१४", "१५", "१६", "१७", "१८"
)

private val bgColors = listOf(
    Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460), Color(0xFF533483),
    Color(0xFF2D1B69), Color(0xFF1B1464), Color(0xFF0B0B2B), Color(0xFF1C1C3A),
    Color(0xFF2A2A4A), Color(0xFF151530), Color(0xFF1E1E3F), Color(0xFF252550),
    Color(0xFF181835), Color(0xFF202045), Color(0xFF2E2E55), Color(0xFF12122A),
    Color(0xFF222248), Color(0xFF282852)
)

private val accentColors = listOf(
    Color(0xFFFFD700), Color(0xFFFF6B35), Color(0xFF00B4D8), Color(0xFFE040FB),
    Color(0xFF4CAF50), Color(0xFFFF9800), Color(0xFFE91E63), Color(0xFF00BCD4),
    Color(0xFFFF5722), Color(0xFF9C27B0), Color(0xFF8BC34A), Color(0xFFFF4081),
    Color(0xFF03A9F4), Color(0xFFFFEB3B), Color(0xFF7C4DFF), Color(0xFFFF5252),
    Color(0xFF69F0AE), Color(0xFFFFAB40)
)

fun createChapterInfo(number: Int, title: String, sanskritTitle: String, summary: String, verseCount: Int): ChapterInfo =
    ChapterInfo(number, title, sanskritTitle, devanagariNumerals.getOrElse(number - 1) { "$number" }, summary, verseCount,
        fontSizeMultiplier = 1f + (number - 1) * 0.05f)

@Composable
fun ChapterCard(
    chapter: ChapterInfo,
    readCount: Int = 0,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = bgColors.getOrElse(chapter.number - 1) { bgColors.last() }
    val accent = accentColors.getOrElse(chapter.number - 1) { accentColors.last() }
    val devNumSize = (72 + (chapter.number * 2)).sp
    // Cycle between serif, sans-serif and monospace for variety
    val fontFamily = when (chapter.number % 3) {
        0 -> FontFamily.Serif
        1 -> FontFamily.Default
        else -> FontFamily.Monospace
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Devanagari watermark number
            Text(
                text = chapter.devanagariNumeral,
                fontSize = devNumSize,
                color = Color.White.copy(alpha = 0.06f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 15.dp, y = (-10).dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top row: chapter number + accent bar
                Column {
                    Text(
                        text = "Chapter ${chapter.number}",
                        fontSize = 11.sp,
                        color = accent.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp
                    )
                    Spacer(Modifier.height(4.dp))

                    // Sanskrit title as main visual
                    Text(
                        text = chapter.sanskritTitle,
                        fontSize = (20 * chapter.fontSizeMultiplier).sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.5.sp
                    )

                    // English title
                    Text(
                        text = chapter.title,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontFamily = fontFamily,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Bottom row: verses + progress
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Thin accent line
                    Box(
                        modifier = Modifier
                            .width((36 + chapter.number * 2).dp)
                            .height(2.dp)
                            .background(accent, RoundedCornerShape(1.dp))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${chapter.verseCount} verses",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        if (readCount > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${(readCount.toFloat() / chapter.verseCount * 100).toInt()}%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "Read →",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                            }
                        } else {
                            Text(
                                text = "Read →",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}
