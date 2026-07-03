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
    val dailyState = remember(coinBalance) { tracker.getDailyState() }
    val weeklyState = remember(coinBalance) { tracker.getWeeklyState() }
    var claimedDay by remember(coinBalance) { mutableStateOf(dailyState.todayClaimed) }
    var claimedCount by remember(coinBalance) { mutableIntStateOf(if (dailyState.todayClaimed) dailyState.day else dailyState.day - 1) }
    var weekCompleted by remember(coinBalance) { mutableStateOf(dailyState.day >= 7 && dailyState.todayClaimed) }
    var dayBonusMessage by remember { mutableStateOf<String?>(null) }
    var claimedDayIndex by remember { mutableIntStateOf(-1) }

    // Reset claimed highlight after animation completes
    LaunchedEffect(claimedDayIndex) {
        if (claimedDayIndex > 0) { delay(350); claimedDayIndex = -1 }
    }

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
            isToday = { d -> d == claimedCount + 1 && !claimedDay },
            wasJustClaimed = { d -> claimedDayIndex == d },
            onDayClick = { d ->
                claimedDayIndex = d
                val coins = tracker.claimDaily(); claimedDay = true; claimedCount++
                if (coins > 0) {
                    if (d == 7) {
                        weekCompleted = true
                        val total = coins + weeklyState.reward
                        val desc = buildString {
                            append("Day 7 check-in")
                            append(" + Week ${weeklyState.week} bonus")
                            append(" = $total coins")
                        }
                        onEarnCoins(total, desc)
                        dayBonusMessage = "Week ${weeklyState.week} done! +$total bonus"
                    } else {
                        onEarnCoins(coins, "Day $d check-in")
                        dayBonusMessage = "+$coins coins"
                    }
                } else { dayBonusMessage = "Protection used" }
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
        val shareState = remember(coinBalance) { tracker.getShareState() }
        var claimedShare by remember(coinBalance) { mutableStateOf(shareState.todayClaimed) }
        HorizontalDivider(color = bd)
        Text("Daily Share Rewards", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tc)
        StreakStrip(
            days = 7,
            isClaimed = { d -> d < shareState.day || (d == shareState.day && claimedShare) },
            isToday = { d -> d == shareState.day && !claimedShare },
            wasJustClaimed = { false },
            onDayClick = { onNavigateToShare() },
            activeColor = Color(0xFFFF9800),
            dimColor = dim,
            bgColor = bg,
            bdColor = bd,
            animateEntry = true
        )
        Text("Share a sloka from Random Sloka", fontSize = 10.sp, color = dim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
