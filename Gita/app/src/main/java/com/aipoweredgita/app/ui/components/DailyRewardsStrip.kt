package com.aipoweredgita.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.coin.DailyRewardsTracker
import com.aipoweredgita.app.ui.theme.GoldSpark

private val springSnap = spring<Float>(dampingRatio = 0.5f, stiffness = 600f)
private val springBounce = spring<Float>(dampingRatio = 0.3f, stiffness = 400f)
private val springColor = spring<Color>(dampingRatio = 0.7f, stiffness = 500f)

@Composable
fun DailyRewardsStrip(
    tracker: DailyRewardsTracker,
    context: android.content.Context,
    isDark: Boolean,
    coinBalance: Int,
    onEarnCoins: (amount: Int, description: String) -> Unit = { _, _ -> },
    onNavigateToShare: () -> Unit = {}
) {
    // Read every recomposition so re-login gets the new user id (not a frozen remember)
    val authUserId = com.aipoweredgita.app.utils.AuthPreferences.getInstance(context).userId
    // Re-read when user id changes (re-login) — remember{} alone freezes pre-sync empty state
    var dailyState by remember(authUserId) { mutableStateOf(tracker.getDailyState()) }
    var weeklyState by remember(authUserId) { mutableStateOf(tracker.getWeeklyState()) }
    var claimedDay by remember(authUserId) { mutableStateOf(dailyState.todayClaimed) }
    // Days 1..claimedCount claimed; next slot is claimable when !claimedDay
    var claimedCount by remember(authUserId) {
        mutableIntStateOf(
            if (dailyState.todayClaimed) dailyState.day.coerceIn(1, 7)
            else (dailyState.day - 1).coerceAtLeast(0)
        )
    }
    var weekCompleted by remember(authUserId) {
        mutableStateOf(dailyState.day >= 7 && dailyState.todayClaimed)
    }
    var dayBonusMessage by remember { mutableStateOf<String?>(null) }
    var claimedDayIndex by remember { mutableIntStateOf(-1) }

    fun reloadStripFromTracker() {
        val ds = tracker.getDailyState()
        val ws = tracker.getWeeklyState()
        dailyState = ds
        weeklyState = ws
        claimedDay = ds.todayClaimed
        claimedCount = if (ds.todayClaimed) ds.day.coerceIn(1, 7) else (ds.day - 1).coerceAtLeast(0)
        weekCompleted = ds.day >= 7 && ds.todayClaimed
    }

    // Re-read when balance/user changes OR when tracker.revision bumps after server syncWithServer
    LaunchedEffect(coinBalance, authUserId) {
        var lastRev = -1
        // Poll briefly so async login force-sync lands on the strip
        repeat(20) {
            val rev = tracker.revision
            if (rev != lastRev) {
                lastRev = rev
                reloadStripFromTracker()
            }
            delay(250)
        }
        reloadStripFromTracker()
    }

    // Reset claimed highlight after animation completes
    LaunchedEffect(claimedDayIndex) {
        if (claimedDayIndex > 0) { delay(350); claimedDayIndex = -1 }
    }

    val todaySlot = (claimedCount + 1).coerceIn(1, 7)

    val tc = if (isDark) Color.White else Color.Black
    val dim = if (isDark) Color.White.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.4f)
    val bg = if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)
    val bd = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

        // ─── Daily Check-in ───────────────────────────────────────────
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Daily Check-in Rewards", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tc)
            if (dailyState.hasProtection) {
                Box(Modifier.clip(MaterialTheme.shapes.small).background(Color(0xFFFF9800).copy(alpha = 0.15f)).border(0.5.dp, Color(0xFFFF9800).copy(alpha = 0.3f), MaterialTheme.shapes.small).padding(horizontal = 8.dp, vertical = 2.dp)) {
                    Text("Protected", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9800))
                }
            }
        }

        StreakStrip(
            days = 7,
            isClaimed = { d -> d <= claimedCount },
            isToday = { d -> !claimedDay && d == todaySlot },
            wasJustClaimed = { d -> claimedDayIndex == d },
            onDayClick = { d ->
                if (!com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(context)) {
                    android.widget.Toast.makeText(context, "Internet connection required to claim daily streak!", android.widget.Toast.LENGTH_SHORT).show()
                    return@StreakStrip
                }
                val isAlreadyClaimed = d <= claimedCount
                val isTodaySlot = !claimedDay && d == todaySlot
                val isFutureSlot = d > todaySlot
                when {
                    isAlreadyClaimed -> android.widget.Toast.makeText(context, "Day $d already claimed ✓", android.widget.Toast.LENGTH_SHORT).show()
                    isFutureSlot -> android.widget.Toast.makeText(context, "Complete previous days first!", android.widget.Toast.LENGTH_SHORT).show()
                    claimedDay -> android.widget.Toast.makeText(context, "Already claimed today — come back tomorrow!", android.widget.Toast.LENGTH_SHORT).show()
                    isTodaySlot -> {
                        val coins = tracker.claimDaily()
                        claimedDayIndex = d
                        claimedDay = true
                        if (coins > 0) {
                            claimedCount = d.coerceIn(1, 7)
                            if (d == 7) {
                                weekCompleted = true
                                val total = coins + weeklyState.reward
                                val desc = "Day 7 check-in + Week ${weeklyState.week} bonus = $total coins"
                                onEarnCoins(total, desc)
                                dayBonusMessage = "Week ${weeklyState.week} done! +$total bonus"
                            } else {
                                onEarnCoins(coins, "Day $d check-in")
                                dayBonusMessage = "+$coins coins"
                            }
                        } else {
                            // Still mark slot claimed so strip refreshes even if 0-coin protection
                            claimedCount = d.coerceIn(1, 7)
                            val desc = "Day $d check-in (Streak Protected)"
                            onEarnCoins(0, desc)
                            dayBonusMessage = "Streak Protected ✓"
                        }
                    }
                }
            },
            activeColor = GoldSpark,
            dimColor = dim,
            bgColor = bg,
            bdColor = bd
        )

        // Progress bar with spring animation
        val animProgress by animateFloatAsState(
            targetValue = (claimedCount / 7f).coerceIn(0f, 1f),
            animationSpec = springSnap, label = "progress"
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(bg)) {
                Box(Modifier.fillMaxWidth(animProgress).fillMaxHeight().clip(RoundedCornerShape(2.dp))
                    .background(if (claimedCount >= 7) Color(0xFF4CAF50) else GoldSpark))
            }
            Text("$claimedCount/7", fontSize = 10.sp, color = dim)
        }

        dayBonusMessage?.let { Text(it, fontSize = 12.sp, color = GoldSpark, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) }

        // ─── Weekly Bonus — auto-claimed with day 7 ────────────────────
        val weekBg by animateColorAsState(
            targetValue = if (weekCompleted) Color(0xFF4CAF50).copy(alpha = 0.08f) else bg,
            animationSpec = springColor, label = "week_bg"
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(weekBg)
                .border(0.5.dp, if (weekCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f) else bd, MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Week ${weeklyState.week} Bonus", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tc)
                Text(
                    when { weekCompleted -> "Collected +${weeklyState.reward} coins"; else -> "Complete 7-day check-in" },
                    fontSize = 11.sp, color = if (weekCompleted) Color(0xFF4CAF50) else dim)
            }
            val wkScale by animateFloatAsState(
                targetValue = if (weekCompleted) 1.2f else 1f,
                animationSpec = springBounce, label = "wk_scale"
            )
            Box(Modifier.size(32.dp).scale(wkScale).clip(CircleShape)
                .background(if (weekCompleted) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f))
                .border(1.dp, if (weekCompleted) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(if (weekCompleted) "✓" else "🪙", fontSize = 13.sp)
            }
        }

        // ─── Share Rewards ──────────────────────────────────────────────
        var shareState by remember(authUserId) { mutableStateOf(tracker.getShareState()) }
        var claimedShare by remember(authUserId) { mutableStateOf(shareState.todayClaimed) }
        // Keyed on tracker.revision (bumped by claimShare()/syncShareWithServer()) instead
        // of coinBalance/dailyState — those don't reliably change from a share claim made
        // on a different screen (Random Sloka), so this strip could keep showing
        // "unclaimed" after navigating back until something unrelated happened to bump
        // coinBalance. revision changes unconditionally on every local claim, so this
        // always catches it, the same pattern the check-in poll above already relies on.
        LaunchedEffect(authUserId) {
            var lastRev = -1
            repeat(20) {
                val rev = tracker.revision
                if (rev != lastRev) {
                    lastRev = rev
                    shareState = tracker.getShareState()
                    claimedShare = shareState.todayClaimed
                }
                delay(250)
            }
            shareState = tracker.getShareState()
            claimedShare = shareState.todayClaimed
        }
        val shareToday = shareState.day.coerceIn(1, 7)
        HorizontalDivider(color = bd)
        Text("Daily Share Rewards", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tc)
        StreakStrip(
            days = 7,
            isClaimed = { d -> d < shareToday || (d == shareToday && claimedShare) },
            isToday = { d -> d == shareToday && !claimedShare },
            wasJustClaimed = { false },
            onDayClick = {
                if (!com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(context)) {
                    android.widget.Toast.makeText(context, "Internet connection required to claim daily streak!", android.widget.Toast.LENGTH_SHORT).show()
                } else if (claimedShare) {
                    android.widget.Toast.makeText(context, "Already shared today — come back tomorrow!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    onNavigateToShare()
                }
            },
            activeColor = Color(0xFFFF9800),
            dimColor = dim,
            bgColor = bg,
            bdColor = bd,
            animateEntry = true
        )
        Text("Share a sloka from Random Sloka", fontSize = 10.sp, color = dim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Preview(showBackground = true, name = "Daily Rewards - Light Mode")
@Composable
fun DailyRewardsStripPreviewLight() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tracker = remember { com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(ctx) }
    com.aipoweredgita.app.ui.theme.GitaLearningTheme(darkTheme = false) {
        DailyRewardsStrip(
            tracker = tracker,
            context = ctx,
            isDark = false,
            coinBalance = 50,
            onEarnCoins = { _, _ -> },
            onNavigateToShare = {}
        )
    }
}

@Preview(showBackground = true, name = "Daily Rewards - Dark Mode", backgroundColor = 0xFF0F0F0F)
@Composable
fun DailyRewardsStripPreviewDark() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val tracker = remember { com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(ctx) }
    com.aipoweredgita.app.ui.theme.GitaLearningTheme(darkTheme = true) {
        DailyRewardsStrip(
            tracker = tracker,
            context = ctx,
            isDark = true,
            coinBalance = 50,
            onEarnCoins = { _, _ -> },
            onNavigateToShare = {}
        )
    }
}
