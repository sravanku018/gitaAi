package com.aipoweredgita.app.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aipoweredgita.app.R
<<<<<<< HEAD
import com.aipoweredgita.app.ui.theme.GoldSpark
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
import com.aipoweredgita.app.ui.theme.*

@Composable
fun CompletionDialog(
    score: Int,
    total: Int,
<<<<<<< HEAD
    coins: Int = 0,
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    onExit: () -> Unit,
    onRestart: () -> Unit
) {
    val percentage = if (total > 0) (score * 100) / total else 0
    val performanceMessage = when {
        percentage >= 90 -> stringResource(id = R.string.quiz_performance_outstanding)
        percentage >= 75 -> stringResource(id = R.string.quiz_performance_excellent)
        percentage >= 60 -> stringResource(id = R.string.quiz_performance_good)
        percentage >= 40 -> stringResource(id = R.string.quiz_performance_keep_practicing)
        else -> stringResource(id = R.string.quiz_performance_dont_give_up)
    }

    Dialog(onDismissRequest = { }) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
<<<<<<< HEAD
            shape = MaterialTheme.shapes.extraLarge,
=======
            shape = RoundedCornerShape(28.dp),
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.3f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "ॐ",
                    fontSize = 48.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = stringResource(id = R.string.quiz_complete_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(60.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$percentage%",
                            fontSize = 32.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "ACCURACY",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Text(
                    text = performanceMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(id = R.string.quiz_score, score, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

<<<<<<< HEAD
                if (coins > 0) {
                    Surface(
                        color = GoldSpark.copy(alpha = 0.15f),
                        shape = MaterialTheme.shapes.large,
                        border = androidx.compose.foundation.BorderStroke(1.dp, GoldSpark.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text("🪙", fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Earned $coins Krishna Coins!",
                                fontWeight = FontWeight.Bold,
                                color = GoldSpark,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onExit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
<<<<<<< HEAD
                    shape = MaterialTheme.shapes.medium
=======
                    shape = RoundedCornerShape(12.dp)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                ) {
                    Text(stringResource(id = R.string.quiz_exit), fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        stringResource(id = R.string.quiz_restart),
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
