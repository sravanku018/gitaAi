package com.aipoweredgita.app.ui.screens.quiz.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.ui.screens.quiz.BattleState

@Composable
fun BattleGameOverView(
    battleState: BattleState,
    onPlayAgain: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚔️", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("Battle Over!", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("Score: ${battleState.score}", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Text("Max Combo: ${battleState.maxCombo}x", style = MaterialTheme.typography.titleLarge)
        Text("Questions: ${battleState.questionsAnswered}", style = MaterialTheme.typography.titleMedium)
        
        val correct = battleState.correctAt3Hearts + battleState.correctAt2Hearts + battleState.correctAt1Heart
        val accuracy = if (battleState.questionsAnswered > 0) ((correct.toFloat() / battleState.questionsAnswered) * 100).toInt() else 0
        Text("Accuracy: $accuracy%", style = MaterialTheme.typography.titleMedium, color = if (accuracy >= 80) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface)

        if (battleState.battleCoins > 0) {
            Spacer(Modifier.height(8.dp))
            Text("🪙 +${battleState.battleCoins} coins", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(32.dp))
        Button(onClick = onPlayAgain) {
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
            // Lives
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) { i ->
                    Icon(
                        if (i < lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (i < lives) Color.Red else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Timer
            Text(
                "${timeLeft}s",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (timeLeft <= 10) Color.Red else MaterialTheme.colorScheme.onSurface
            )

            // Score
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
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.3f),
        label = "combo"
    )
    Text(
        "🔥 ${combo}x COMBO!",
        style = MaterialTheme.typography.titleMedium,
        color = Color(0xFFFFD700),
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
    question.options?.forEachIndexed { index, option ->
        val isSelected = selectedAnswer == option
        val isCorrect = index == question.correctAnswerIndex
        val showResult = isAnswerRevealed

        val containerColor = when {
            showResult && isCorrect -> Color(0xFF2E7D32)
            showResult && isSelected && !isCorrect -> Color(0xFFC62828)
            isSelected -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surfaceVariant
        }

        val textColor = when {
            showResult && isCorrect -> Color.White
            showResult && isSelected && !isCorrect -> Color.White
            isSelected -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Button(
            onClick = {
                if (!isAnswerRevealed) {
                    onAnswerSelected(isCorrect, option)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = containerColor),
            enabled = !isAnswerRevealed
        ) {
            Text(
                option,
                modifier = Modifier.padding(vertical = 8.dp),
                color = textColor,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp
            )
        }
    }
}
