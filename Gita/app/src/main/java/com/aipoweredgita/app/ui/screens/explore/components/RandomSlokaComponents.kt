package com.aipoweredgita.app.ui.screens.explore.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.R
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.ui.components.GlassCard
import com.aipoweredgita.app.ui.components.PremiumDashboardCard
import com.aipoweredgita.app.ui.theme.rememberGitaColors

@Composable
fun RandomSlokaCard(
    verse: CachedVerse,
    modifier: Modifier = Modifier
) {
    val colors = rememberGitaColors()
    var meaningOpen by remember(verse.chapterNo, verse.verseNo) { mutableStateOf(false) }
    val hasMeaning = verse.meaning.isNotBlank() || verse.explanation.isNotBlank()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = modifier
    ) {
        PremiumDashboardCard(
            title = stringResource(R.string.random_sloka_chapter_verse, verse.chapterNo, verse.verseNo),
            description = verse.chapterName.ifBlank { "Sloka" },
            icon = {
                // Decorative Om — hide from TalkBack
                Text(
                    "ॐ",
                    fontSize = 32.sp,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            },
            gradient = listOf(colors.accent, colors.accentSoft),
            onClick = {},
            modifier = Modifier.fillMaxWidth()
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            tint = colors.cardTint.copy(alpha = if (colors.isDark) 0.08f else 0.55f),
            border = colors.cardBorder,
            cornerRadius = 28.dp,
            elevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = verse.verse,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 26.sp,
                        lineHeight = 36.sp,
                    ),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                    modifier = Modifier.fillMaxWidth()
                )
                HorizontalDivider(color = colors.divider)
                Text(
                    text = verse.translation,
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp),
                    textAlign = TextAlign.Start,
                    color = colors.textPrimary,
                    modifier = Modifier.fillMaxWidth()
                )

                if (hasMeaning) {
                    TextButton(
                        onClick = { meaningOpen = !meaningOpen },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            imageVector = if (meaningOpen) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = colors.accent
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (meaningOpen) R.string.random_sloka_hide_meaning
                                else R.string.random_sloka_show_meaning
                            ),
                            color = colors.accent
                        )
                    }

                    AnimatedVisibility(
                        visible = meaningOpen,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (verse.meaning.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.random_sloka_meaning),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentSoft
                                )
                                Text(
                                    text = verse.meaning,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start,
                                    color = colors.textSecondary
                                )
                            }
                            if (verse.explanation.isNotBlank()) {
                                Text(
                                    text = stringResource(R.string.random_sloka_purport),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentSoft
                                )
                                Text(
                                    text = verse.explanation,
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Start,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RandomSlokaActions(
    isSpeaking: Boolean,
    isSharing: Boolean,
    onListenClick: () -> Unit,
    onShareClick: () -> Unit,
    onShareImageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = rememberGitaColors()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onListenClick,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
        ) {
            Icon(
                if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null, // visible label carries the name
                tint = colors.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                if (isSpeaking) stringResource(R.string.random_sloka_stop)
                else stringResource(R.string.random_sloka_listen)
            )
        }

        OutlinedButton(
            onClick = onShareClick,
            enabled = !isSharing,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.random_sloka_share))
        }

        OutlinedButton(
            onClick = onShareImageClick,
            enabled = !isSharing,
            modifier = Modifier.heightIn(min = 48.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.random_sloka_share_image))
        }
    }
}
