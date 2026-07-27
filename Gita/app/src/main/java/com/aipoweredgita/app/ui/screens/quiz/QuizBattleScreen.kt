package com.aipoweredgita.app.ui.screens.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.ui.screens.quiz.components.*
import com.aipoweredgita.app.viewmodel.QuizBattleViewModel
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
            val correctAnswers = correctAt3Hearts + correctAt2Hearts + correctAt1Heart
            if (correctAnswers <= 0) return 0
            var a = 1
            var b = 1
            for (i in 3..correctAnswers) {
                val temp = a + b
                a = b
                b = temp
            }
            return if (correctAnswers == 1) a else b
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
    var hasTriggeredGameOver by remember { mutableStateOf(false) }
    val quizViewModel: QuizBattleViewModel = androidx.hilt.navigation.compose.hiltViewModel()

    LaunchedEffect(Unit) {
        quizViewModel.setQuizLimit(999)  // Battle = unlimited questions
        val currentLang = java.util.Locale.getDefault().language
        quizViewModel.setQuizLanguage(if (currentLang.startsWith("te")) "telugu" else "english")
    }

    // Timer
    LaunchedEffect(isTimerRunning) {
        if (isTimerRunning) {
            while (battleState.timeLeft > 0 && isTimerRunning) {
                delay(1000)
                if (!isTimerRunning) break
                battleState = battleState.copy(timeLeft = battleState.timeLeft - 1)
            }
            if (battleState.timeLeft <= 0 && isTimerRunning && !battleState.isGameOver && !hasTriggeredGameOver) {
                isTimerRunning = false
                hasTriggeredGameOver = true
                battleState = battleState.copy(isGameOver = true)
                val correctAnswers = battleState.correctAt3Hearts + battleState.correctAt2Hearts + battleState.correctAt1Heart
                onGameOver(correctAnswers, battleState.maxCombo, battleState.questionsAnswered, battleState.battleCoins)
            }
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
            BattleGameOverView(
                battleState = battleState,
                onPlayAgain = {
                    hasTriggeredGameOver = false
                    battleState = BattleState()
                    isTimerRunning = true
                    quizViewModel.restartQuiz()
                }
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Stats Bar
                BattleStatsBar(
                    lives = battleState.lives,
                    timeLeft = battleState.timeLeft,
                    score = battleState.score
                )

                // Combo indicator
                if (battleState.combo > 1) {
                    Spacer(Modifier.height(8.dp))
                    ComboIndicator(combo = battleState.combo)
                }

                Spacer(Modifier.height(16.dp))

                // Question
                battleState.currentQuestion?.let { question ->
                    BattleQuestionCard(question = question)
                    Spacer(Modifier.height(20.dp))
                    BattleAnswerOptions(
                        question = question,
                        selectedAnswer = battleState.selectedAnswer,
                        isAnswerRevealed = battleState.isAnswerRevealed,
                        onAnswerSelected = { isCorrect, option ->
                            var nextState = battleState.copy(selectedAnswer = option, isAnswerRevealed = true)

                            if (isCorrect) {
                                val comboMultiplier = (nextState.combo + 1).coerceAtMost(5)
                                val points = 10 * comboMultiplier
                                nextState = nextState.copy(
                                    score = nextState.score + points,
                                    combo = nextState.combo + 1,
                                    maxCombo = maxOf(nextState.maxCombo, nextState.combo + 1),
                                    questionsAnswered = nextState.questionsAnswered + 1,
                                    correctAt3Hearts = if (nextState.lives == 3) nextState.correctAt3Hearts + 1 else nextState.correctAt3Hearts,
                                    correctAt2Hearts = if (nextState.lives == 2) nextState.correctAt2Hearts + 1 else nextState.correctAt2Hearts,
                                    correctAt1Heart = if (nextState.lives == 1) nextState.correctAt1Heart + 1 else nextState.correctAt1Heart
                                )
                            } else {
                                nextState = nextState.copy(
                                    lives = nextState.lives - 1,
                                    combo = 0,
                                    questionsAnswered = nextState.questionsAnswered + 1
                                )
                                if (nextState.lives <= 0) {
                                    isTimerRunning = false
                                    nextState = nextState.copy(isGameOver = true)
                                    if (!battleState.isGameOver && !hasTriggeredGameOver) {
                                        hasTriggeredGameOver = true
                                        val correctAnswers = nextState.correctAt3Hearts + nextState.correctAt2Hearts + nextState.correctAt1Heart
                                        onGameOver(correctAnswers, nextState.maxCombo, nextState.questionsAnswered, nextState.battleCoins)
                                    }
                                }
                            }
                            battleState = nextState
                        }
                    )
                } ?: if (isTimerRunning) {
                    Spacer(Modifier.height(48.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Loading question...")
                } else {
                    Spacer(Modifier.height(48.dp))
                }

                Spacer(Modifier.height(24.dp))

                // Start Button
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
