package com.aipoweredgita.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.util.TimeUtils
import com.aipoweredgita.app.ui.theme.GoldSpark

// ─── Unified Stat Card ────────────────────────────────────────────────────────
// Merge target for: OverviewStatCard (ActivityHistoryScreen), AHPerformanceCard (ActivityHistoryScreen), 
// and any PerformanceCard elsewhere. Use `compact` mode for smaller cards.
@Composable
fun StatCard(
    label: String,
    value: String,
    icon: String = "",
    color: Color = MaterialTheme.colorScheme.primary,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (compact) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (icon.isNotEmpty()) Text(text = icon, fontSize = 16.sp)
                Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }
            Text(text = value, fontWeight = FontWeight.Bold, color = color, fontSize = 14.sp)
        }
    } else {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (icon.isNotEmpty()) {
                    Text(text = icon, fontSize = 32.sp)
                    Spacer(Modifier.height(8.dp))
                }
                Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
                Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Unified Tip Card ─────────────────────────────────────────────────────────
// Merge target for: AHTipCard (ActivityHistoryScreen) and TipCard (if exists elsewhere)
@Composable
fun TipCard(
    icon: String,
    title: String,
    tip: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(2.dp))
                Text(text = tip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ─── Unified Empty State ──────────────────────────────────────────────────────
// Merge target for: AHEmptyState (ActivityHistoryScreen) and EmptyState (elsewhere)
@Composable
fun EmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

// ─── Unified Quiz Attempt Card ────────────────────────────────────────────────
// Merge target for: AHQuizAttemptCard (ActivityHistoryScreen) and QuizAttemptCard (elsewhere)
@Composable
fun QuizAttemptCard(
    attempt: QuizAttempt,
    showCoins: Boolean = true,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Quiz #${attempt.id}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Score: ${attempt.score}/${attempt.totalQuestions}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (attempt.timeSpentSeconds > 0) {
                    Text(
                        text = "Time: ${TimeUtils.formatTime(attempt.timeSpentSeconds)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showCoins && attempt.coinsEarned > 0) {
                Text(
                    text = "🪙 ${attempt.coinsEarned}",
                    fontWeight = FontWeight.Bold,
                    color = GoldSpark
                )
            }
        }
    }
}

// ─── Unified Tips Tab ──────────────────────────────────────────────────────────
// Merge target for: AHTipsTab (ActivityHistoryScreen) and TipsTab (elsewhere)
@Composable
fun TipsTab(
    averageAccuracy: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(androidx.compose.foundation.rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Tips to Improve",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        val tips = buildList {
            add(Triple("📖", "Read Daily", "Consistency builds understanding"))
            add(Triple("🎯", "Focus on Weak Areas", if (averageAccuracy < 70f) "Review segments where you scored low" else "Challenge yourself with harder topics"))
            add(Triple("🔄", "Regular Review", "Spaced repetition helps retention"))
            add(Triple("🧘", "Take Your Time", "Read the full verse context before answering"))
            if (averageAccuracy < 60f) {
                add(Triple("📝", "Start with 10-Question Quizzes", "Shorter sessions help build momentum"))
            }
        }
        tips.forEach { (icon, title, tip) ->
            TipCard(icon = icon, title = title, tip = tip, color = MaterialTheme.colorScheme.primary)
        }
    }
}

