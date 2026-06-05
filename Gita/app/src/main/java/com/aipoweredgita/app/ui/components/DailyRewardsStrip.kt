package com.aipoweredgita.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
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
    onEarnCoins: (amount: Int, description: String) -> Unit = { _, _ -> }
) {
    val dailyState = remember(coinBalance) { tracker.getDailyState() }
    val weeklyState = remember(coinBalance) { tracker.getWeeklyState() }
    var claimedDay by remember(coinBalance) { mutableStateOf(dailyState.todayClaimed) }
    var claimedWeek by remember(coinBalance) { mutableStateOf(weeklyState.claimed) }
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

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (d in 1..7) {
                val isClaimed = d <= claimedCount
                val isToday = d == claimedCount + 1 && !claimedDay
                val wasJustClaimed = claimedDayIndex == d

                val boxScale by animateFloatAsState(
                    targetValue = if (wasJustClaimed) 1.15f else if (isClaimed) 1f else 1f,
                    animationSpec = springBounce, label = "box_scale_$d"
                )
                val checkAlpha by animateFloatAsState(
                    targetValue = if (isClaimed) 1f else 0f,
                    animationSpec = springSnap, label = "check_alpha_$d"
                )
                val bgColor by animateColorAsState(
                    targetValue = when { isClaimed -> Color(0xFF4CAF50).copy(alpha = 0.2f); isToday -> GoldSpark.copy(alpha = 0.12f); else -> bg },
                    animationSpec = springColor, label = "bg_color_$d"
                )
                val borderColor by animateColorAsState(
                    targetValue = when { isClaimed -> Color(0xFF4CAF50).copy(alpha = 0.5f); isToday -> GoldSpark; else -> bd },
                    animationSpec = springColor, label = "border_color_$d"
                )

                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .scale(boxScale)
                        .clip(MaterialTheme.shapes.small)
                        .background(bgColor)
                        .border(if (isToday) 1.5.dp else 0.5.dp, borderColor, MaterialTheme.shapes.small)
                        .clickable(enabled = isToday) {
                            claimedDayIndex = d
                            val coins = tracker.claimDaily(); claimedDay = true; claimedCount++
                            if (coins > 0) {
                                if (d == 7) {
                                    weekCompleted = true
                                    val bonus = tracker.claimDay7BonusIfEligible()
                                    val wk = tracker.claimWeekly()
                                    if (wk > 0) claimedWeek = true
                                    val total = coins + bonus + wk
                                    val desc = buildString {
                                        append("Day 7 check-in")
                                        if (bonus > 0) append(" + 7-day bonus")
                                        if (wk > 0) append(" + Week ${weeklyState.week} bonus")
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
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isClaimed) {
                            Text("✓", fontSize = 15.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold,
                                modifier = Modifier.graphicsLayer { alpha = checkAlpha })
                        } else {
                            Text("+$d", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isToday) GoldSpark else dim)
                        }
                    }
                }
            }
        }

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
            targetValue = if (claimedWeek) Color(0xFF4CAF50).copy(alpha = 0.08f) else bg,
            animationSpec = springColor, label = "week_bg"
        )
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(weekBg)
                .border(0.5.dp, if (claimedWeek) Color(0xFF4CAF50).copy(alpha = 0.2f) else bd, MaterialTheme.shapes.small)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Week ${weeklyState.week} Bonus", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = tc)
                Text(
                    when { claimedWeek -> "Collected +${weeklyState.reward} coins"; !weekCompleted -> "Complete 7-day check-in"; else -> "Awarded on day 7" },
                    fontSize = 11.sp, color = if (claimedWeek) Color(0xFF4CAF50) else dim)
            }
            val wkScale by animateFloatAsState(
                targetValue = if (claimedWeek) 1.2f else 1f,
                animationSpec = springBounce, label = "wk_scale"
            )
            Box(Modifier.size(32.dp).scale(wkScale).clip(CircleShape)
                .background(if (claimedWeek) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.1f))
                .border(1.dp, if (claimedWeek) Color(0xFF4CAF50).copy(alpha = 0.4f) else Color.Gray.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center) {
                Text(if (claimedWeek) "✓" else "🪙", fontSize = 13.sp)
            }
        }

        // ─── Share Rewards ──────────────────────────────────────────────
        val shareState = remember(coinBalance) { tracker.getShareState() }
        var claimedShare by remember(coinBalance) { mutableStateOf(shareState.todayClaimed) }
        HorizontalDivider(color = bd)
        Text("Daily Share Rewards", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = tc)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            for (d in 1..7) {
                val isClaimed = d < shareState.day || (d == shareState.day && claimedShare)
                val isToday = d == shareState.day && !claimedShare
                val entryAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 300f),
                    label = "share_entry_$d"
                )

                val bgColor by animateColorAsState(
                    targetValue = when { isClaimed -> Color(0xFF4CAF50).copy(alpha = 0.2f); isToday -> Color(0xFFFF9800).copy(alpha = 0.12f); else -> bg },
                    animationSpec = springColor, label = "share_bg_$d"
                )
                val bdColor by animateColorAsState(
                    targetValue = when { isClaimed -> Color(0xFF4CAF50).copy(alpha = 0.5f); isToday -> Color(0xFFFF9800); else -> bd },
                    animationSpec = springColor, label = "share_bd_$d"
                )

                Box(
                    modifier = Modifier.weight(1f).height(48.dp)
                        .graphicsLayer {
                            alpha = entryAlpha.coerceAtMost(1f)
                            translationY = (1f - entryAlpha) * 12f
                        }
                        .clip(MaterialTheme.shapes.small)
                        .background(bgColor)
                        .border(if (isToday) 1.5.dp else 0.5.dp, bdColor, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (isClaimed) Text("✓", fontSize = 15.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                        else Text("+$d", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isToday) Color(0xFFFF9800) else dim)
                    }
                }
            }
        }
        Text("Share a verse from Random Sloka", fontSize = 10.sp, color = dim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}
