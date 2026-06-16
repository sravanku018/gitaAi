package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.repository.CoinReconciliationManager
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinHistoryScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val coinBalance by viewModel.coinBalance.collectAsState()
    var allHistory by remember { mutableStateOf<List<CoinHistoryEntry>>(emptyList()) }
    var activeFilter by remember { mutableStateOf("all") }
    var isGuest by remember { mutableStateOf(false) }

    // Refresh trigger for manual refresh and lifecycle-based refresh
    var refreshTrigger by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Auto-refresh when screen resumes (e.g., navigating back after earning coins)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
                viewModel.refreshCoinBalance()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initial load when screen first appears
    LaunchedEffect(Unit) {
        viewModel.refreshCoinBalance()
        kotlinx.coroutines.delay(300)
        refreshTrigger++
    }

    // Function to run reconciliation — disabled, auto-reconcile corrupts balance
    suspend fun runReconciliationIfNeeded() {
        // Auto-reconcile disabled: Groq AI was deleting transactions and
        // adjusting balances incorrectly on every screen refresh
    }

    // Build local history from CoinTransactionLogger — used as fallback when no JWT token
    // (users created via users/create have no token, so server API returns 401/403)
    fun buildLocalHistory(ctx: android.content.Context): List<CoinHistoryEntry> {
        // Format local timestamps as UTC strings so they sort correctly alongside server entries.
        // The server stores times in UTC; using local time here would create ordering mismatches
        // for users in timezones ahead of UTC (e.g. IST entries would appear 5.5 h in the future).
        val utcFmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        return CoinTransactionLogger.getHistory(ctx).map { tx ->
            CoinHistoryEntry(
                amount = tx.amount,
                type = tx.type.name,
                source = tx.description.lowercase().let { desc ->
                    when {
                        desc.contains("welcome") -> "signup"
                        desc.contains("quiz") -> "quiz_completion"
                        desc.contains("check") || desc.contains("checkin") -> "checkin_day"
                        desc.contains("share") -> "share_sloka"
                        desc.contains("voice") -> "voice_chat"
                        desc.contains("chapter") -> "chapter_completion"
                        desc.contains("level") -> "level_up_bonus"
                        else -> "other"
                    }
                },
                description = tx.description,
                created_at = utcFmt.format(java.util.Date(tx.timestamp))
            )
        }
    }

    // Fetch history from API when userId becomes available, balance changes, or refresh is triggered
    LaunchedEffect(stats, refreshTrigger, coinBalance) {
        val authPrefs = AuthPreferences.getInstance(context)
        isGuest = authPrefs.isGuestUser

        // Always prefer authPrefs.userId (set synchronously on login) over stats?.userId
        // (Room DB Flow can lag behind after login, causing 403 if wrong userId is sent)
        val effectiveUid = authPrefs.userId?.takeIf { it.isNotEmpty() }
            ?: stats?.userId?.takeIf { it.isNotEmpty() }

        android.util.Log.d("CoinHistory", "LaunchedEffect triggered: stats=${stats?.userId}, refreshTrigger=$refreshTrigger, isGuest=$isGuest, effectiveUid=$effectiveUid, hasToken=${authPrefs.token != null}")

        if (effectiveUid != null && effectiveUid.isNotEmpty()) {
            // Run reconciliation on refresh (1 time only)
            if (refreshTrigger > 0) {
                runReconciliationIfNeeded()
            }

            // Small delay to ensure server has processed the transaction
            if (refreshTrigger > 0 || coinBalance > 0) {
                kotlinx.coroutines.delay(500)
            }

            if (isGuest) {
                // For guests: show local transactions from CoinTransactionLogger
                allHistory = buildLocalHistory(context)
                android.util.Log.d("CoinHistory", "Guest: Loaded ${allHistory.size} local transactions")
            } else {
                val token = authPrefs.token
                var serverLoaded = false
                if (token != null) {
                    // Has JWT token (auth/register users) — try server first
                    try {
                        val serverHistory = CoinApi.retrofitService.getHistory(
                            effectiveUid, "Bearer $token", limit = 500
                        )
                        // Server is source of truth — use server data directly
                        // Filter out guest bonus entries for non-guest users
                        val filteredServerHistory = if (!isGuest) {
                            serverHistory.filter { entry ->
                                !(entry.source == "signup" && entry.description.contains("Guest", ignoreCase = true))
                            }
                        } else {
                            serverHistory
                        }
                        // Merge server + local: only add local entries not already on server
                        val localHistory = buildLocalHistory(context)
                        // Build unique keys using source + amount + date to prevent day entries from being dropped
                        val serverKeys = filteredServerHistory.map { entry ->
                            val datePart = entry.created_at?.split(" ")?.get(0) ?: ""
                            "${entry.source}_${entry.amount}_${datePart}"
                        }.toSet()
                        val extraLocalEntries = localHistory.filter { local ->
                            val localDate = local.created_at?.split(" ")?.get(0) ?: ""
                            val key = "${local.source}_${local.amount}_${localDate}"
                            !serverKeys.contains(key)
                        }
                        allHistory = filteredServerHistory + extraLocalEntries
                        serverLoaded = true
                        // Sync server data to local (preserves it for offline use)
                        if (serverHistory.isNotEmpty()) {
                            CoinTransactionLogger.syncFromServer(context, serverHistory)
                        }
                        android.util.Log.d("CoinHistory", "Server: ${serverHistory.size} txns + ${extraLocalEntries.size} older local txns = ${allHistory.size} total for $effectiveUid")
                    } catch (e: Exception) {
                        android.util.Log.e("CoinHistory", "Server history failed (${e.message}), falling back to local")
                    }
                } else {
                    android.util.Log.w("CoinHistory", "No JWT token (users/create user) — using local history")
                }
                // Fallback: no token OR server failed → show local history
                if (!serverLoaded) {
                    val localOnly = buildLocalHistory(context)
                    // Filter out guest bonus entries for non-guest users
                    allHistory = if (!isGuest) {
                        localOnly.filter { entry ->
                            !(entry.source == "signup" && entry.description.contains("Guest", ignoreCase = true))
                        }
                    } else {
                        localOnly
                    }
                    android.util.Log.d("CoinHistory", "Local fallback: Loaded ${allHistory.size} transactions")
                }
            }
        } else {
            android.util.Log.w("CoinHistory", "No userId available, stats=${stats?.userId}, authUserId=${authPrefs.userId}")
        }
    }



    val filtered = when (activeFilter) {
        "earned" -> allHistory.filter { it.amount > 0 }
        "spent" -> allHistory.filter { it.amount < 0 }
        else -> allHistory
    }

    val totalSpent = allHistory.filter { it.amount < 0 }.sumOf { -it.amount }
    val displayNet = coinBalance // Use actual balance from server
    val totalEarned = displayNet + totalSpent // Derive earned so earned - spent = net always matches

    // Group history by date
    val groupedHistory = remember(filtered) {
        val utcParse = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFmt = java.text.SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        
        val todayStr = dateFmt.format(Date())
        val yesterdayDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val yesterdayStr = dateFmt.format(yesterdayDate)

        filtered.groupBy { entry ->
            try {
                val parsed = utcParse.parse(entry.created_at)
                if (parsed != null) {
                    val dateKey = dateFmt.format(parsed)
                    when (dateKey) {
                        todayStr -> "Today"
                        yesterdayStr -> "Yesterday"
                        else -> displayDateFmt.format(parsed)
                    }
                } else "Unknown Date"
            } catch (e: Exception) {
                "Unknown Date"
            }
        }
    }

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
                IconButton(onClick = { refreshTrigger++ }) {
                    Text("↻", fontSize = 20.sp, color = Color(0xFFEDE1CF))
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
            } else {
                groupedHistory.forEach { (dateLabel, transactions) ->
                    // Section header for each date group
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                dateLabel.uppercase(Locale.getDefault()),
                                fontSize = 11.sp,
                                letterSpacing = 1.5.sp,
                                color = Color(0xFF9A8D71),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Transaction items for this date group
                    itemsIndexed(transactions) { index, entry ->
                val isEarn = entry.type == "EARN"
                val alpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(300, delayMillis = (minOf(index, 25) * 40)),
                    label = "tx_fade"
                )

                // created_at is stored in UTC ("yyyy-MM-dd HH:mm:ss").
                // Parse as UTC, then format to device local time for display.
                val dateStr = try {
                    val utcParse = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val localFmt = java.text.SimpleDateFormat("HH:mm", Locale.getDefault())
                    // localFmt uses device default timezone automatically
                    val parsed = utcParse.parse(entry.created_at)
                    if (parsed != null) localFmt.format(parsed) else ""
                } catch (_: Exception) { "" }
                val label = when {
                    entry.source == "signup" && entry.description.contains("Guest", ignoreCase = true) -> "Guest welcome bonus"
                    entry.source == "signup" -> "Welcome bonus"
                    entry.source == "quiz_completion" -> "Quiz completed"
                    entry.source == "chapter_completion" -> "Chapter completed"
                    // Accept multiple backend source variants for check-in
                    entry.source == "checkin_day" || entry.source == "checkin" ||
                    entry.source == "daily_checkin" || entry.source.contains("checkin") -> "Daily check-in"
                    // Accept multiple backend source variants for share
                    entry.source == "share_sloka" || entry.source == "share" ||
                    entry.source == "daily_share" || entry.source.contains("share") -> "Verse shared"
                    entry.source == "voice_chat" || entry.source == "voice" ->
                        if (isEarn) "Voice chat" else "Voice chat question"
                    entry.source == "level_up_bonus" || entry.source == "level_up" -> "Level up bonus"
                    else -> entry.description.ifBlank { if (isEarn) "Earned" else "Spent" }
                }
                val icon = when {
                    entry.source == "signup" -> "🎉"
                    entry.source == "quiz_completion" -> "📚"
                    entry.source == "chapter_completion" -> "📚"
                    entry.source == "checkin_day" || entry.source == "checkin" ||
                    entry.source == "daily_checkin" || entry.source.contains("checkin") -> "☀️"
                    entry.source == "share_sloka" || entry.source == "share" ||
                    entry.source == "daily_share" || entry.source.contains("share") -> "📖"
                    entry.source == "voice_chat" || entry.source == "voice" -> "🎙"
                    entry.source == "level_up_bonus" || entry.source == "level_up" -> "⬆"
                    else -> if (isEarn) "✦" else "◈"
                }
                val isVoiceSource = entry.source == "voice_chat" || entry.source == "voice"
                val isCheckinSource = entry.source == "checkin_day" || entry.source == "checkin" ||
                    entry.source == "daily_checkin" || entry.source.contains("checkin")
                val isShareSource = entry.source == "share_sloka" || entry.source == "share" ||
                    entry.source == "daily_share" || entry.source.contains("share")
                val supporting = if (isVoiceSource && !isEarn) {
                    entry.description.take(40)
                } else when {
                    entry.source == "signup" -> "Welcome"
                    entry.source == "quiz_completion" -> "Completed"
                    entry.source == "chapter_completion" -> "Chapter done"
                    isCheckinSource -> "Daily reward"
                    isShareSource -> "Shared"
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
        } // closes groupedHistory.forEach
    } // closes else block

    item { Spacer(Modifier.height(24.dp)) }
} // closes LazyColumn
} // closes Column
} // closes CoinHistoryScreen
