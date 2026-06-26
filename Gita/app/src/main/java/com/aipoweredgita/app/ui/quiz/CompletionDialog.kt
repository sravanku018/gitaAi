package com.aipoweredgita.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aipoweredgita.app.R
import com.aipoweredgita.app.ui.theme.GoldSpark

@Composable
fun CompletionDialog(
    score: Int,
    total: Int,
    coins: Int = 0,
    totalTimeSeconds: Long = 0,
    onExit: () -> Unit,
    onRestart: () -> Unit
) {
    val percentage = if (total > 0) (score * 100) / total else 0
    val statusColor = when {
        percentage >= 90 -> MaterialTheme.colorScheme.primary
        percentage >= 75 -> MaterialTheme.colorScheme.secondary
        percentage >= 60 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val performanceMessage = when {
        percentage >= 90 -> stringResource(id = R.string.quiz_performance_outstanding)
        percentage >= 75 -> stringResource(id = R.string.quiz_performance_excellent)
        percentage >= 60 -> stringResource(id = R.string.quiz_performance_good)
        percentage >= 40 -> stringResource(id = R.string.quiz_performance_keep_practicing)
        else -> stringResource(id = R.string.quiz_performance_dont_give_up)
    }
    val minutes = totalTimeSeconds / 60
    val seconds = totalTimeSeconds % 60
    val timeText = if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
    val avgSeconds = if (total > 0) totalTimeSeconds / total else 0L
    val avgText = "${avgSeconds}s"

    Dialog(onDismissRequest = { }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Accuracy circle (smaller)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(statusColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$percentage%",
                        fontSize = 24.sp,
                        color = statusColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Performance message
                Text(
                    text = performanceMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )

                // Stats row: score + total time + avg time + coins
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$score/$total",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Score", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = timeText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = avgText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text("Avg/Q", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (coins > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$coins",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldSpark
                            )
                            Text("Coins", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Buttons
                Button(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(stringResource(id = R.string.quiz_exit), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                TextButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text(
                        stringResource(id = R.string.quiz_restart),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
