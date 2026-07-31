package com.aipoweredgita.app.ui.screens.explore.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.ui.components.PremiumDashboardCard
import com.aipoweredgita.app.ui.theme.GoldSpark
import com.aipoweredgita.app.ui.theme.Saffron

@Composable
fun RandomSlokaCard(
    verse: CachedVerse,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = modifier
    ) {
        PremiumDashboardCard(
            title = "Chapter ${verse.chapterNo}",
            description = "Verse ${verse.verseNo}",
            icon = { Text("ॐ", fontSize = 32.sp) }, // Om symbol
            gradient = listOf(Color(0xFFF59E0B), Color(0xFFD97706)),
            onClick = {}, // No action on card click itself
            modifier = Modifier.fillMaxWidth()
        )

        val cardBg = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        val cardBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        val textPrimary = MaterialTheme.colorScheme.onBackground
        val shadowColor = if (isDark) Color.Black else Color(0x35546E7A)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.extraLarge)
                .background(cardBg)
                .border(1.dp, cardBorder, MaterialTheme.shapes.extraLarge)
                .shadow(
                    elevation = 6.dp,
                    shape = MaterialTheme.shapes.extraLarge,
                    ambientColor = shadowColor,
                    spotColor = shadowColor
                )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = verse.verse,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) GoldSpark else Saffron
                )
                HorizontalDivider(color = cardBorder)
                Text(
                    text = verse.translation,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = textPrimary
                )
            }
        }
    }
}

@Composable
fun RandomSlokaActions(
    isSpeaking: Boolean,
    onListenClick: () -> Unit,
    onShareClick: () -> Unit,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedButton(
            onClick = onListenClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = goldColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, goldColor.copy(alpha = 0.5f))
        ) {
            Icon(
                if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = "Listen",
                tint = goldColor
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isSpeaking) "Stop Audio" else "Listen in Telugu")
        }

        Spacer(Modifier.width(12.dp))

        OutlinedButton(
            onClick = onShareClick,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = goldColor),
            border = androidx.compose.foundation.BorderStroke(1.dp, goldColor.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = goldColor)
            Spacer(Modifier.width(8.dp))
            Text("Share")
        }
    }
}
