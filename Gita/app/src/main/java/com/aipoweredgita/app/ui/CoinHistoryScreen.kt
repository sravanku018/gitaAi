package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.CoinHistoryEntry
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import com.aipoweredgita.app.coin.CoinTransactionLogger
import com.aipoweredgita.app.utils.AuthPreferences
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinHistoryScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    var allHistory by remember { mutableStateOf<List<CoinHistoryEntry>>(emptyList()) }
    var activeFilter by remember { mutableStateOf("all") }
    var isGuest by remember { mutableStateOf(false) }

    // Fetch history and balance from API when userId becomes available
    LaunchedEffect(stats) {
        val uid = stats?.userId
        if (uid != null && uid.isNotEmpty()) {
            val authPrefs = AuthPreferences.getInstance(context)
            isGuest = authPrefs.isGuestUser

            if (isGuest) {
                // For guests: show local transactions from CoinTransactionLogger
                val localTx = CoinTransactionLogger.getHistory(context).map { tx ->
                    CoinHistoryEntry(
                        amount = tx.amount,
                        type = tx.type.name,
                        source = tx.description.lowercase().let { desc ->
                            when {
                                desc.contains("welcome") -> "signup"
                                desc.contains("quiz") -> "quiz_completion"
                                desc.contains("check") -> "checkin_day"
                                desc.contains("share") -> "share_sloka"
                                desc.contains("voice") -> "voice_chat"
                                desc.contains("chapter") -> "chapter_completion"
                                else -> "other"
                            }
                        },
                        description = tx.description,
                        created_at = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(java.util.Date(tx.timestamp))
                    )
                }
                allHistory = localTx
            } else {
                try { allHistory = CoinApi.retrofitService.getHistory(uid) } catch (e: Exception) { android.util.Log.e("CoinHistory", "Failed to load history: ${e.message}") }
            }
            try { viewModel.refreshCoinBalance() } catch (e: Exception) { android.util.Log.e("CoinHistory", "Failed to load balance: ${e.message}") }
        }
    }

    val filtered = when (activeFilter) {
        "earned" -> allHistory.filter { it.amount > 0 }
        "spent" -> allHistory.filter { it.amount < 0 }
        else -> allHistory
    }
    // Deduplicate signup (welcome bonus) to avoid server duplicates from re-syncs
    var seenSignup = false
    val totalEarned = allHistory.filter { it.amount > 0 }
        .filter { if (it.source == "signup") { if (seenSignup) false else { seenSignup = true; true } } else true }
        .sumOf { it.amount }
    val totalSpent = allHistory.filter { it.amount < 0 }.sumOf { -it.amount }
    val todayLabel = SimpleDateFormat("MMMM d", Locale.getDefault()).format(Date())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141000))
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Coin History", fontWeight = FontWeight.Medium) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Text("←", fontSize = 22.sp, color = Color(0xFFEDE1CF))
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Text("⋮", fontSize = 20.sp, color = Color(0xFFEDE1CF))
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Balance Card
            item {
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
                                Triple("Net", "+${totalEarned - totalSpent}", Color(0xFF8B5E00))
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

            // Filter Chips
            item {
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
                                .clickable { activeFilter = key }
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

            // Section header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today · $todayLabel",
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFF9A8D71)
                    )
                    Text(
                        "See all",
                        fontSize = 13.sp,
                        color = Color(0xFF8B5E00),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { }
                    )
                }
            }

            if (filtered.isEmpty()) {
                item {
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
            }

            // Transaction items
            items(filtered.take(100)) { entry ->
                val isEarn = entry.type == "EARN"
                val index = filtered.indexOf(entry)
                val alpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(350, delayMillis = 60 + index * 60),
                    label = "tx_fade"
                )

                val dateStr = try {
                    val parts = entry.created_at.split(" ")
                    if (parts.size >= 2) parts[1].substringBeforeLast(":").take(5) else ""
                } catch (_: Exception) { "" }
                val label = when (entry.source) {
                    "signup" -> "Welcome bonus"
                    "quiz_completion" -> "Quiz completed"
                    "chapter_completion" -> "Chapter completed"
                    "checkin_day" -> "Daily check-in"
                    "share_sloka" -> "Verse shared"
                    "voice_chat" -> if (isEarn) "Voice chat" else "Voice chat question"
                    "level_up_bonus" -> "Level up bonus"
                    else -> entry.description.ifBlank { if (isEarn) "Earned" else "Spent" }
                }
                val icon = when (entry.source) {
                    "signup" -> "🎉"
                    "quiz_completion" -> "📚"
                    "chapter_completion" -> "📚"
                    "checkin_day" -> "☀️"
                    "share_sloka" -> "📖"
                    "voice_chat" -> "🎙"
                    "level_up_bonus" -> "⬆"
                    else -> if (isEarn) "✦" else "◈"
                }
                val supporting = if (entry.source == "voice_chat" && !isEarn) {
                    entry.description.take(40)
                } else when (entry.source) {
                    "signup" -> "Welcome"
                    "quiz_completion" -> "Completed"
                    "chapter_completion" -> "Chapter done"
                    "checkin_day" -> "Daily reward"
                    "share_sloka" -> "Shared"
                    "voice_chat" -> "Voice chat"
                    "level_up_bonus" -> "Bonus"
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
                        // Icon
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(MaterialTheme.shapes.large)
                                .background(iconBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(icon, fontSize = 20.sp)
                        }

                        // Text
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 15.sp, color = Color(0xFFEDE1CF), fontWeight = FontWeight.Medium)
                            if (supporting.isNotEmpty()) {
                                Text(supporting, fontSize = 13.sp, color = Color(0xFFD0C3A4))
                            }
                        }

                        // Amount + time
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                if (isEarn) "+${entry.amount}" else "${entry.amount}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEarn) Color(0xFF1B6B36) else Color(0xFFBA1A1A)
                            )
                            Text(dateStr, fontSize = 10.sp, color = Color(0xFF9A8D71))
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
