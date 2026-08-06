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

/**
 * Convert JSON metadata stored in coin_transactions.description (from /coins/award)
 * into a short human-readable label for the history UI.
 */
private fun humanizeJsonDescription(raw: String, source: String): String {
    return try {
        val obj = org.json.JSONObject(raw)
        when {
            source.contains("quiz", ignoreCase = true) || obj.has("score") -> {
                val score = obj.optInt("score", -1)
                val total = obj.optInt("totalQuestions", obj.optInt("questionsAnswered", -1))
                val type = obj.optString("quizType", "").ifBlank { "quiz" }
                when {
                    score >= 0 && total > 0 -> "Quiz ($type): $score/$total"
                    score >= 0 -> "Quiz: $score correct"
                    else -> "Quiz completed"
                }
            }
            source.contains("battle", ignoreCase = true) || obj.has("battleCoins") -> {
                val coins = obj.optInt("battleCoins", -1)
                val score = obj.optInt("score", -1)
                if (coins >= 0 && score >= 0) "Battle quiz: $score correct (+$coins)"
                else if (score >= 0) "Battle quiz: $score correct"
                else "Battle quiz"
            }
            else -> source.replace('_', ' ').replaceFirstChar { it.uppercase() }.ifBlank { "Earned coins" }
        }
    } catch (_: Exception) {
        source.replace('_', ' ').ifBlank { "Activity" }
    }
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
    val isEarn = entry.isEarn
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

    val src = entry.source.lowercase()
    val rawDesc = entry.description.trim()
    // Prefer server/admin description. JSON blobs (quiz metadata) → human summary.
    val cleanDesc = when {
        rawDesc.isEmpty() -> ""
        rawDesc.startsWith("{") -> humanizeJsonDescription(rawDesc, entry.source)
        else -> rawDesc
    }

    val isVoiceSource = src == "voice_chat" || src == "voice" ||
        rawDesc.lowercase().contains("question") || rawDesc.lowercase().contains("asked")
    val isCheckinSource = src == "checkin_day" || src == "checkin" || src == "daily_checkin" ||
        src.contains("checkin") || src.contains("daily login") || src.contains("daily_login")
    val isShareSource = src == "share_sloka" || src == "share" || src == "daily_share" ||
        (src.contains("share") && !src.contains("chapter"))
    val isMeditationSource = src == "meditation" || rawDesc.lowercase().contains("meditation")

    // Fallback labels by source — only used when description is empty
    val fallbackLabel = when {
        src == "signup" && cleanDesc.contains("Guest", ignoreCase = true) -> "Guest welcome bonus"
        src == "signup" || src.contains("welcome") -> "Welcome bonus"
        src == "quiz_completion" || src == "quiz" -> "Quiz completed"
        src == "battle_quiz" || src.contains("battle") -> "Battle Quiz"
        src == "chapter_completion" || src.contains("chapter") -> "Chapter completed"
        isCheckinSource -> "Daily check-in"
        isShareSource -> "Daily share"
        isMeditationSource -> "Meditation practice"
        isVoiceSource -> if (isEarn) "Voice chat" else "Voice chat question"
        src == "level_up_bonus" || src.contains("level") -> "Level up bonus"
        src == "admin_adjustment" || src == "admin_quick_edit" -> "Admin adjustment"
        else -> if (isEarn) "Earned coins" else "Spent coins"
    }

    // Admin/dashboard edits write description in Turso — always show it in the app.
    val label = if (cleanDesc.isNotBlank()) cleanDesc else fallbackLabel
    
    val icon = when {
        src == "signup" || src.contains("welcome") -> "🎉"
        src == "quiz_completion" || src == "quiz" -> "📚"
        src == "battle_quiz" || src.contains("battle") -> "⚔️"
        src == "chapter_completion" || src.contains("chapter") -> "📚"
        isCheckinSource -> "☀️"
        isShareSource -> "📖"
        isMeditationSource -> "🧘"
        isVoiceSource -> "🎙"
        src == "level_up_bonus" || src.contains("level") -> "⬆"
        src.startsWith("admin") -> "🛠"
        else -> if (isEarn) "✦" else "◈"
    }
        
    val supporting = when {
        // Custom description already shown as title — show source category under it
        cleanDesc.isNotBlank() && cleanDesc != fallbackLabel -> fallbackLabel
        isVoiceSource && !isEarn && cleanDesc.isNotBlank() ->
            cleanDesc.replace(Regex("(?i)asked question:\\s*"), "").take(35)
        cleanDesc.isBlank() -> when {
            src == "signup" -> "Welcome bonus"
            src == "quiz_completion" || src == "quiz" -> "Quiz reward"
            src == "battle_quiz" -> "Battle reward"
            src == "chapter_completion" -> "Chapter done"
            isCheckinSource -> "Daily reward"
            isShareSource -> "Sloka shared"
            isVoiceSource -> "Voice chat"
            src == "level_up_bonus" -> "Bonus"
            else -> entry.source.takeIf { it.isNotBlank() } ?: ""
        }
        else -> entry.source.takeIf { it.isNotBlank() && it != cleanDesc } ?: ""
    }

    val isDark = com.aipoweredgita.app.ui.theme.rememberThemeIsDark()
    val textColor = if (isDark) Color(0xFFEDE1CF) else MaterialTheme.colorScheme.onSurface
    val subTextColor = if (isDark) Color(0xFFD0C3A4) else MaterialTheme.colorScheme.onSurfaceVariant
    val iconBg = if (isEarn) (if (isDark) Color(0x334CAF50) else Color(0xFFE8F5E9)) else (if (isDark) Color(0x33FF5252) else Color(0xFFFFEBEE))
    val amountColor = if (isEarn) (if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)) else (if (isDark) Color(0xFFFF8A80) else Color(0xFFC62828))

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
                Text(label, fontSize = 15.sp, color = textColor, fontWeight = FontWeight.Bold)
                if (supporting.isNotEmpty()) {
                    Text(supporting, fontSize = 13.sp, color = subTextColor)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (isEarn) "+${kotlin.math.abs(entry.signedAmount)}" else "-${kotlin.math.abs(entry.signedAmount)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(dateStr, fontSize = 11.sp, color = subTextColor)
            }
        }
    }
}
