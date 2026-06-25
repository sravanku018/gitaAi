package com.aipoweredgita.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

data class BattleState(
    val score: Int = 0,
    val lives: Int = 3,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val timeLeft: Int = 60,
    val isGameOver: Boolean = false,
    val currentQuestion: QuizQuestion? = null,
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val questionsAnswered: Int = 0,
    val correctAt3Hearts: Int = 0,
    val correctAt2Hearts: Int = 0,
    val correctAt1Heart: Int = 0
) {
    val battleCoins: Int
        get() {
            val raw = correctAt3Hearts * 1.0 + correctAt2Hearts * 0.5 + correctAt1Heart * 0.25
            return raw.toInt()
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizBattleScreen(
    onBack: () -> Unit = {},
    onGameOver: (score: Int, maxCombo: Int, questionsAnswered: Int, battleCoins: Int) -> Unit = { _, _, _, _ -> }
) {
    val context = LocalContext.current
    var battleState by remember { mutableStateOf(BattleState()) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val quizViewModel: QuizViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    // Timer
    LaunchedEffect(isTimerRunning, battleState.timeLeft) {
        if (isTimerRunning && battleState.timeLeft > 0) {
            delay(1000)
            battleState = battleState.copy(timeLeft = battleState.timeLeft - 1)
        } else if (battleState.timeLeft <= 0 && isTimerRunning) {
            isTimerRunning = false
            battleState = battleState.copy(isGameOver = true)
            onGameOver(battleState.score, battleState.maxCombo, battleState.questionsAnswered, battleState.battleCoins)
        }
    }

    // Load question - triggers on start and after each answer
    LaunchedEffect(isTimerRunning, battleState.questionsAnswered) {
        if (isTimerRunning && !battleState.isGameOver && battleState.lives > 0) {
            quizViewModel.loadNextQuestion()
        }
    }

    val quizState by quizViewModel.quizState.collectAsState()

    LaunchedEffect(quizState.currentQuestion) {
        quizState.currentQuestion?.let { q ->
            battleState = battleState.copy(
                currentQuestion = q,
                selectedAnswer = null,
                isAnswerRevealed = false
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("⚔️ Battle Mode") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (battleState.isGameOver) {
            // Game Over
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
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
                if (battleState.battleCoins > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("🪙 +${battleState.battleCoins} coins", style = MaterialTheme.typography.titleLarge, color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(32.dp))
                Button(onClick = {
                    battleState = BattleState()
                    isTimerRunning = true
                    quizViewModel.restartQuiz()
                }) {
                    Text("Play Again")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ── Stats Bar (compact floating) ──
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
                                    if (i < battleState.lives) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = null,
                                    tint = if (i < battleState.lives) Color.Red else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Timer
                        Text(
                            "${battleState.timeLeft}s",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (battleState.timeLeft <= 10) Color.Red else MaterialTheme.colorScheme.onSurface
                        )

                        // Score
                        Text(
                            "${battleState.score} pts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Combo indicator
                if (battleState.combo > 1) {
                    Spacer(Modifier.height(8.dp))
                    val comboScale by animateFloatAsState(
                        targetValue = 1f,
                        animationSpec = spring(dampingRatio = 0.3f),
                        label = "combo"
                    )
                    Text(
                        "🔥 ${battleState.combo}x COMBO!",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.scale(comboScale)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ── Question ──
                battleState.currentQuestion?.let { question ->
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

                    Spacer(Modifier.height(20.dp))

                    // ── Options ──
                    question.options?.forEachIndexed { index, option ->
                        val isSelected = battleState.selectedAnswer == option
                        val isCorrect = index == question.correctAnswerIndex
                        val showResult = battleState.isAnswerRevealed

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
                                if (!battleState.isAnswerRevealed) {
                                    battleState = battleState.copy(selectedAnswer = option, isAnswerRevealed = true)

                                    if (isCorrect) {
                                        val comboMultiplier = (battleState.combo + 1).coerceAtMost(5)
                                        val points = 10 * comboMultiplier
                                        battleState = battleState.copy(
                                            score = battleState.score + points,
                                            combo = battleState.combo + 1,
                                            maxCombo = maxOf(battleState.maxCombo, battleState.combo + 1),
                                            questionsAnswered = battleState.questionsAnswered + 1,
                                            correctAt3Hearts = if (battleState.lives == 3) battleState.correctAt3Hearts + 1 else battleState.correctAt3Hearts,
                                            correctAt2Hearts = if (battleState.lives == 2) battleState.correctAt2Hearts + 1 else battleState.correctAt2Hearts,
                                            correctAt1Heart = if (battleState.lives == 1) battleState.correctAt1Heart + 1 else battleState.correctAt1Heart
                                        )
                                    } else {
                                        battleState = battleState.copy(
                                            lives = battleState.lives - 1,
                                            combo = 0,
                                            questionsAnswered = battleState.questionsAnswered + 1
                                        )
                                        if (battleState.lives <= 0) {
                                            isTimerRunning = false
                                            battleState = battleState.copy(isGameOver = true)
                                            onGameOver(battleState.score, battleState.maxCombo, battleState.questionsAnswered, battleState.battleCoins)
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = containerColor),
                            enabled = !battleState.isAnswerRevealed
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
                } ?: if (isTimerRunning) {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading question...")
                } else {
                    Spacer(Modifier.height(48.dp))
                }

                Spacer(Modifier.height(24.dp))

                // ── Start Button ──
                if (!isTimerRunning && battleState.questionsAnswered == 0) {
                    Button(
                        onClick = { isTimerRunning = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                    ) {
                        Text(
                            "⚔️ FIGHT!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
