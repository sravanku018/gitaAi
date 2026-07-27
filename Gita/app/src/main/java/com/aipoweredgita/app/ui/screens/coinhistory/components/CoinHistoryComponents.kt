package com.aipoweredgita.app.ui.screens.coinhistory.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.network.CoinHistoryEntry
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.Date

private fun parseDateRobust(dateStr: String?): Date? {
    if (dateStr.isNullOrEmpty()) return null
    
    // Check if it's a numeric Unix timestamp
    val asLong = dateStr.toLongOrNull()
    if (asLong != null) {
        // If it's less than 30000000000 (year 2920), it's likely seconds. Otherwise milliseconds.
        return if (asLong < 30000000000L) Date(asLong * 1000) else Date(asLong)
    }

    var normalized = dateStr
    
    // Fix 6-digit microseconds before a timezone (e.g. .267992+05:30 -> .267+05:30)
    val microRegex = Regex("\\.(\\d{6})([+-]\\d{2}:?\\d{2}|Z)")
    normalized = microRegex.replace(normalized) { matchResult ->
        ".${matchResult.groupValues[1].take(3)}${matchResult.groupValues[2]}"
    }

    val formats = listOf(
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (formatStr in formats) {
        try {
            val sdf = SimpleDateFormat(formatStr, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = sdf.parse(normalized)
            if (parsed != null) return parsed
        } catch (e: Exception) {
            // Ignore
        }
    }
    return null
}

@Composable
fun CoinBalanceCard(
    coinBalance: Int,
    totalEarned: Int,
    totalSpent: Int,
    displayNet: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFDEA0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        "KRISHNA COINS",
                        fontSize = 11.sp, letterSpacing = 1.sp,
                        color = Color(0xFF8B5E00),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "$coinBalance",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2B1C00),
                        letterSpacing = (-1).sp
                    )
                    Text(
                        "Total balance",
                        fontSize = 13.sp,
                        color = Color(0xFF8B5E00)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(Color(0xFF8B5E00)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🪙", fontSize = 26.sp)
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color(0xFF8B5E00).copy(alpha = 0.3f))

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(0xFFFFDEA0))
                    .border(0.5.dp, Color(0xFF8B5E00).copy(alpha = 0.2f), MaterialTheme.shapes.medium)
            ) {
                listOf(
                    Triple("Earned", "+$totalEarned", Color(0xFF1B6B36)),
                    Triple("Spent", "-$totalSpent", Color(0xFFBA1A1A)),
                    Triple("Net", if (displayNet >= 0) "+$displayNet" else "$displayNet", Color(0xFF8B5E00))
                ).forEachIndexed { i, (label, value, color) ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
                        Text(label, fontSize = 10.sp, color = Color(0xFF8B5E00), letterSpacing = 0.5.sp)
                    }
                    if (i < 2) {
                        Box(
                            modifier = Modifier
                                .width(0.5.dp)
                                .height(36.dp)
                                .align(Alignment.CenterVertically)
                                .background(Color(0xFF8B5E00).copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CoinFilterChips(
    activeFilter: String,
    onFilterChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("all" to "✦ All activity", "earned" to "↑ Earned", "spent" to "↓ Spent").forEach { (key, label) ->
            val isActive = activeFilter == key
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        if (isActive) Color(0xFFFAE0BA)
                        else Color.Transparent
                    )
                    .border(
                        1.dp,
                        if (isActive) Color.Transparent
                        else Color(0xFF4D4333),
                        MaterialTheme.shapes.large
                    )
                    .clickable { onFilterChanged(key) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isActive) Color(0xFF271904) else Color(0xFFD0C3A4)
                )
            }
        }
    }
}

@Composable
fun CoinEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 60.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🪙", fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text("No transactions", color = Color(0xFF5A4828), fontSize = 14.sp)
        }
    }
}

@Composable
fun CoinTransactionItem(
    entry: CoinHistoryEntry,
    index: Int
) {
    val isEarn = entry.amount > 0 || (entry.amount == 0 && entry.type == "EARN")
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(300, delayMillis = (minOf(index, 25) * 40)),
        label = "tx_fade"
    )

    val dateStr = try {
        val localFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val parsed = parseDateRobust(entry.created_at)
        if (parsed != null) localFmt.format(parsed) else ""
    } catch (_: Exception) { "" }

    val isVoiceSource = entry.source == "voice_chat" || entry.source == "voice" || entry.description.lowercase().contains("question") || entry.description.lowercase().contains("asked")
    val isCheckinSource = entry.source == "checkin_day" || entry.source == "checkin" ||
        entry.source == "daily_checkin" || entry.source.contains("checkin")
    val isShareSource = entry.source == "share_sloka" || entry.source == "share" ||
        entry.source == "daily_share" || entry.source.contains("share")

    val label = when {
        entry.source == "signup" && entry.description.contains("Guest", ignoreCase = true) -> "Guest welcome bonus"
        entry.source == "signup" -> "Welcome bonus"
        entry.source == "quiz_completion" -> "Quiz completed"
        entry.source == "battle_quiz" -> "Battle Quiz"
        entry.source == "chapter_completion" -> "Chapter completed"
        isCheckinSource -> "Daily check-in"
        isShareSource -> "Daily share"
        isVoiceSource -> if (isEarn) "Voice chat" else "Voice chat question"
        entry.source == "level_up_bonus" || entry.source == "level_up" -> "Level up bonus"
        else -> if (isEarn) "Earned coins" else "Spent coins"
    }
    
    val icon = when {
        entry.source == "signup" -> "🎉"
        entry.source == "quiz_completion" -> "📚"
        entry.source == "battle_quiz" -> "⚔️"
        entry.source == "chapter_completion" -> "📚"
        isCheckinSource -> "☀️"
        isShareSource -> "📖"
        isVoiceSource -> "🎙"
        entry.source == "level_up_bonus" || entry.source == "level_up" -> "⬆"
        else -> if (isEarn) "✦" else "◈"
    }
        
    val supporting = when {
        isVoiceSource && !isEarn -> entry.description.replace(Regex("(?i)asked question:\\s*"), "").take(35)
        entry.source == "signup" -> "Welcome bonus"
        entry.source == "quiz_completion" -> "Quiz completed"
        entry.source == "battle_quiz" -> "Battle fought"
        entry.source == "chapter_completion" -> "Chapter done"
        isCheckinSource -> "Daily reward"
        isShareSource -> "Sloka shared"
        isVoiceSource -> "Voice chat"
        entry.source == "level_up_bonus" || entry.source == "level_up" -> "Bonus"
        else -> ""
    }

    val iconBg = if (isEarn) Color(0xFFD1EBC7) else Color(0xFFFFDAD6)
    val iconColor = if (isEarn) Color(0xFF0B2009) else Color(0xFFBA1A1A)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .graphicsLayer { this.alpha = alpha },
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { }
                .padding(vertical = 8.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 15.sp, color = Color(0xFFEDE1CF), fontWeight = FontWeight.Medium)
                if (supporting.isNotEmpty()) {
                    Text(supporting, fontSize = 13.sp, color = Color(0xFFD0C3A4))
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (entry.amount > 0) "+${entry.amount}" else "${entry.amount}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEarn) Color(0xFF1B6B36) else Color(0xFFBA1A1A)
                )
                Text(dateStr, fontSize = 10.sp, color = Color(0xFF9A8D71))
            }
        }
    }
}
