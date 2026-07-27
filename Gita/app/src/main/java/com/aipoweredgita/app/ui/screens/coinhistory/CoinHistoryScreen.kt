package com.aipoweredgita.app.ui.screens.coinhistory

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aipoweredgita.app.ui.screens.coinhistory.components.*
import com.aipoweredgita.app.viewmodel.ProfileViewModel
import java.text.SimpleDateFormat
import java.util.*

fun parseDateRobust(dateStr: String?): Date? {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoinHistoryScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val coinBalance by viewModel.coinBalance.collectAsState()
    val allHistory by viewModel.coinHistory.collectAsState()
    var activeFilter by remember { mutableStateOf("all") }

    var refreshTrigger by remember { mutableIntStateOf(0) }

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

    LaunchedEffect(Unit) {
        viewModel.refreshCoinBalance()
        viewModel.loadCoinHistory()
        kotlinx.coroutines.delay(300)
        refreshTrigger++
    }

    LaunchedEffect(refreshTrigger, coinBalance) {
        viewModel.loadCoinHistory()
    }

    val filtered = when (activeFilter) {
        "earned" -> allHistory.filter { it.isEarn }
        "spent" -> allHistory.filter { it.isSpend }
        else -> allHistory
    }

    val totalSpent = allHistory.filter { it.isSpend }.sumOf { kotlin.math.abs(it.signedAmount) }
    val displayNet = coinBalance
    val totalEarned = displayNet + totalSpent

    val groupedHistory = remember(filtered) {
        val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayDateFmt = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
        
        val todayStr = dateFmt.format(Date())
        val yesterdayDate = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.time
        val yesterdayStr = dateFmt.format(yesterdayDate)

        filtered.groupBy { entry ->
            try {
                val parsed = parseDateRobust(entry.created_at)
                if (parsed != null) {
                    val dateKey = dateFmt.format(parsed)
                    when (dateKey) {
                        todayStr -> "Today"
                        yesterdayStr -> "Yesterday"
                        else -> displayDateFmt.format(parsed)
                    }
                } else "Recent Activity"
            } catch (e: Exception) {
                "Recent Activity"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF141000))
    ) {
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
            item {
                CoinBalanceCard(
                    coinBalance = coinBalance,
                    totalEarned = totalEarned,
                    totalSpent = totalSpent,
                    displayNet = displayNet
                )
            }

            item {
                CoinFilterChips(
                    activeFilter = activeFilter,
                    onFilterChanged = { activeFilter = it }
                )
            }

            if (filtered.isEmpty()) {
                item { CoinEmptyState() }
            } else {
                groupedHistory.forEach { (dateLabel, transactions) ->
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

                    itemsIndexed(transactions, key = { index, entry -> "${entry.source}_${entry.amount}_${entry.created_at}_$index" }) { index, entry ->
                        CoinTransactionItem(entry = entry, index = index)
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
