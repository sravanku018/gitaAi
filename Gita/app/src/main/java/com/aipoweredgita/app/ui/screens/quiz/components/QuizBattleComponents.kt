package com.aipoweredgita.app.ui.screens.quiz.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.viewmodel.BattleUiState

@Composable
fun BattleGameOverView(
    battleState: BattleUiState,
    onPlayAgain: () -> Unit
) {
    androidx.compose.foundation.layout.Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Battle Over!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text(
            "Score: ${battleState.score}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text("Max Combo: ${battleState.maxCombo}x", style = MaterialTheme.typography.titleLarge)
        Text("Questions: ${battleState.questionsAnswered}", style = MaterialTheme.typography.titleMedium)

        val correct = battleState.correctAnswers
        val accuracy = if (battleState.questionsAnswered > 0) {
            ((correct.toFloat() / battleState.questionsAnswered) * 100).toInt()
        } else 0
        Text(
            "Accuracy: $accuracy%",
            style = MaterialTheme.typography.titleMedium,
            color = if (accuracy >= 80) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurface
        )

        if (battleState.battleCoins > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                "+${battleState.battleCoins} coins",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onPlayAgain, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Play Again")
        }
    }
}

@Composable
fun BattleStatsBar(
    lives: Int,
    timeLeft: Int,
    score: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.semantics { contentDescription = "$lives lives remaining" }
            ) {
                repeat(3) { i ->
                    Icon(
                        if (i < lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (i < lives) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Text(
                "${timeLeft}s",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (timeLeft <= 10) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { contentDescription = "$timeLeft seconds remaining" }
            )

            Text(
                "$score pts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ComboIndicator(combo: Int) {
    val comboScale by animateFloatAsState(
        targetValue = 1f + (combo.coerceAtMost(5) * 0.06f),
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 400f),
        label = "combo_$combo"
    )
    Text(
        "${combo}x COMBO!",
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.scale(comboScale)
    )
}

@Composable
fun BattleQuestionCard(question: QuizQuestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Text(
            question.question,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(20.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun BattleAnswerOptions(
    question: QuizQuestion,
    selectedAnswer: String?,
    isAnswerRevealed: Boolean,
    onAnswerSelected: (Boolean, String) -> Unit
) {
    if (question.options.isEmpty()) {
        Text(
            "No options for this question — loading next…",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    question.options.forEachIndexed { index, option ->
        val isSelected = selectedAnswer == option
        val isCorrect = index == question.correctAnswerIndex
        val showResult = isAnswerRevealed

        val containerColor = when {
            showResult && isCorrect -> MaterialTheme.colorScheme.tertiary
            showResult && isSelected && !isCorrect -> MaterialTheme.colorScheme.error
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        val textColor = when {
            showResult && isCorrect -> MaterialTheme.colorScheme.onTertiary
            showResult && isSelected && !isCorrect -> MaterialTheme.colorScheme.onError
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        val statusLabel = when {
            showResult && isCorrect -> "Correct"
            showResult && isSelected && !isCorrect -> "Incorrect"
            else -> null
        }

        Button(
            onClick = {
                if (!isAnswerRevealed) onAnswerSelected(isCorrect, option)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .heightIn(min = 48.dp)
                .then(
                    if (statusLabel != null) Modifier.semantics { contentDescription = "$option, $statusLabel" }
                    else Modifier
                ),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor),
            enabled = !isAnswerRevealed
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    option,
                    modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge
                )
                if (showResult && isCorrect) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = "Correct", tint = textColor)
                } else if (showResult && isSelected && !isCorrect) {
                    Icon(Icons.Filled.Cancel, contentDescription = "Incorrect", tint = textColor)
                }
            }
        }
    }
}
