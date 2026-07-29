package com.aipoweredgita.app.ui.screens.study.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron
import com.aipoweredgita.app.ui.theme.rememberThemeIsDark

@Composable
fun ChapterVerseHeroCard(
    chapter: Int,
    verse: Int,
    combinedNos: List<Int>,
    selectedLanguage: String = "TE",
    onLanguageToggle: (String) -> Unit = {},
    onChapterTap: () -> Unit,
    onVerseTap: () -> Unit
) {
    val isDark = rememberThemeIsDark()
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val cardBorder = if (isDark) Color(0xFF333333) else Color(0xFFFFE0B2)
    val textTertiary = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    val innerBoxBg = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFFFF8F2)
    val innerBoxBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFFFE0B2).copy(alpha = 0.6f)

    val verseDisplay = if (combinedNos.size > 1) {
        "${combinedNos.minOrNull()}–${combinedNos.maxOrNull()}"
    } else verse.toString()

    val chapterName = normalModeChapterNames[chapter] ?: ""
    val numberColor = if (isDark) GoldSpark else Color(0xFFE65100)
    val accentColor = Saffron
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
            .shadow(if (isDark) 4.dp else 0.dp, MaterialTheme.shapes.extraLarge)
            .drawBehind {
                if (isDark) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(accentColor.copy(alpha = 0.18f), Color.Transparent),
                            center = Offset(0f, size.height),
                            radius = size.width * 0.7f
                        ),
                        radius = size.width * 0.7f,
                        center = Offset(0f, size.height)
                    )
                }
            }
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text     = "Chapter $chapter · $chapterName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color    = if (isDark) GoldSpark.copy(alpha = 0.8f) else Color(0xFFD84315),
                    modifier = Modifier.weight(1f)
                )

                // Language Toggle Pill: [ TEL | ENG ]
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFFFF3E0))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFFFB74D), CircleShape)
                        .padding(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isTe = selectedLanguage == "TE"
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isTe) (if (isDark) GoldSpark else Color(0xFFD84315)) else Color.Transparent)
                            .clickable { onLanguageToggle("TE") }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TEL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTe) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White.copy(0.7f) else Color(0xFF5D4037))
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (!isTe) (if (isDark) GoldSpark else Color(0xFFD84315)) else Color.Transparent)
                            .clickable { onLanguageToggle("EN") }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ENG",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isTe) (if (isDark) Color.Black else Color.White) else (if (isDark) Color.White.copy(0.7f) else Color(0xFF5D4037))
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier            = Modifier.fillMaxWidth(),
                verticalAlignment   = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(innerBoxBg)
                        .border(1.dp, innerBoxBorder, MaterialTheme.shapes.large)
                        .clickable(onClick = onChapterTap)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "CHAPTER",
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = textTertiary,
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

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    repeat(5) { i ->
                        Box(
                            modifier = Modifier
                                .size(if (i == 2) 6.dp else 3.dp)
                                .clip(CircleShape)
                                .background(numberColor.copy(alpha = if (i == 2) 0.8f else 0.3f))
                        )
                        if (i < 4) Spacer(Modifier.height(4.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.large)
                        .background(innerBoxBg)
                        .border(1.dp, innerBoxBorder, MaterialTheme.shapes.large)
                        .clickable(onClick = onVerseTap)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text  = "VERSE",
                        fontSize = 9.sp,
                        letterSpacing = 2.sp,
                        color = textTertiary,
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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(innerBoxBg)
                    .clickable(onClick = onChapterTap)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🕉", fontSize = 12.sp, color = numberColor)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text      = "Tap chapter or verse to navigate",
                        fontSize  = 11.sp,
                        color     = textTertiary,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
        }
    }
}

@Composable
fun IlluminatedVerseCard(text: String) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Color(0xFFD84315)
    val primary = if (isDark) Saffron else Color(0xFFE65100)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .border(
                width = 1.dp,
                color = if (isDark) Color(0xFF333333) else Color(0xFFE5E0D8),
                shape = MaterialTheme.shapes.extraLarge
            )
            .background(cardBg)
            .shadow(if (isDark) 4.dp else 0.dp, MaterialTheme.shapes.extraLarge)
            .drawBehind {
                if (isDark) {
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.06f), Color.Transparent),
                            startY = 0f,
                            endY = size.height * 0.45f
                        )
                    )
                }
            }
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    Brush.verticalGradient(listOf(gold, primary, gold))
                )
        )

        Column(modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 18.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .background(gold.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text       = "SHLOKA",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color      = gold
                    )
                }
                Spacer(Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(gold.copy(0.4f), Color.Transparent)
                            )
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text("✦", fontSize = 10.sp, color = gold.copy(0.6f))
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text       = text,
                fontSize   = 16.sp,
                fontStyle  = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                lineHeight = 28.sp,
                color      = textPrimary,
                textAlign  = TextAlign.Justify
            )
        }
    }
}

@Composable
fun MeaningCard(text: String) {
    val isDark = rememberThemeIsDark()
    val greenAccent = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val cardBorder = if (isDark) Color(0xFF333333) else Color(0xFFE5E0D8)
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
            .shadow(if (isDark) 4.dp else 0.dp, MaterialTheme.shapes.extraLarge)
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .background(greenAccent)
        )

        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector        = Icons.Filled.Favorite,
                    contentDescription = null,
                    tint               = greenAccent,
                    modifier           = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = "MEANING",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color      = greenAccent
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text      = text,
                fontSize  = 14.sp,
                lineHeight = 24.sp,
                color     = textPrimary,
                textAlign = TextAlign.Justify
            )
        }
    }
}

@Composable
fun ExplanationCard(text: String) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Color(0xFFD84315)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val cardBorder = if (isDark) Color(0xFF333333) else Color(0xFFE5E0D8)
    val textPrimary = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(cardBg)
            .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
            .shadow(if (isDark) 4.dp else 0.dp, MaterialTheme.shapes.extraLarge)
            .padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(RoundedCornerShape(1.dp))
                        .background(gold.copy(0.7f))
                )
                if (it < 2) Spacer(Modifier.width(4.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text       = "COMMENTARY",
                fontSize   = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color      = gold
            )
        }

        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(listOf(gold.copy(0.5f), Color.Transparent))
                )
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text      = text,
            fontSize  = 14.sp,
            lineHeight = 24.sp,
            color     = textPrimary,
            textAlign = TextAlign.Justify
        )
    }
}

@Composable
fun VerseNoteCard(note: String) {
    val isDark = rememberThemeIsDark()
    val gold = if (isDark) GoldSpark else Color(0xFFD84315)
    val cardBg = if (isDark) Color(0xFF1E1E1E) else Color.White
    val cardBorder = if (isDark) Color(0xFF333333) else Color(0xFFE5E0D8)
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
        Spacer(Modifier.width(10.dp))
        Text(
            text      = note,
            fontSize  = 12.sp,
            lineHeight = 20.sp,
            color     = textSecondary,
            fontStyle = FontStyle.Italic
        )
    }
}
