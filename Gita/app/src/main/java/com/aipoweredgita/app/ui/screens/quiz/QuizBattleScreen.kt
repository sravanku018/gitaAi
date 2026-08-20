package com.aipoweredgita.app.ui.screens.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aipoweredgita.app.ui.screens.quiz.components.BattleAnswerOptions
import com.aipoweredgita.app.ui.screens.quiz.components.BattleGameOverView
import com.aipoweredgita.app.ui.screens.quiz.components.BattleQuestionCard
import com.aipoweredgita.app.ui.screens.quiz.components.BattleStatsBar
import com.aipoweredgita.app.ui.screens.quiz.components.ComboIndicator
import com.aipoweredgita.app.viewmodel.BattleSideEffect
import com.aipoweredgita.app.viewmodel.QuizBattleViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizBattleScreen(
    onBack: () -> Unit = {},
    onGameOver: (score: Int, maxCombo: Int, questionsAnswered: Int, battleCoins: Int, language: String) -> Unit = { _, _, _, _, _ -> },
    viewModel: QuizBattleViewModel = hiltViewModel()
) {
    val battleState by viewModel.battleState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.setQuizLimit(999)
    }

    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collect { effect ->
            when (effect) {
                is BattleSideEffect.GameOver -> onGameOver(
                    effect.correctAnswers,
                    effect.maxCombo,
                    effect.questionsAnswered,
                    effect.battleCoins,
                    effect.language
                )
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Battle Mode", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.cycleLanguage() }) {
                        Text(
                            text = when (battleState.language) {
                                "telugu" -> "తెలుగు"
                                "both" -> "EN + TE"
                                else -> "English"
                            },
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
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
                onPlayAgain = { viewModel.playAgain() }
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
                BattleStatsBar(
                    lives = battleState.lives,
                    timeLeft = battleState.timeLeft,
                    score = battleState.score
                )

                if (battleState.combo > 1) {
                    Spacer(Modifier.height(8.dp))
                    ComboIndicator(combo = battleState.combo)
                }

                Spacer(Modifier.height(16.dp))

                when {
                    battleState.currentQuestion != null -> {
                        val question = battleState.currentQuestion!!
                        BattleQuestionCard(question = question)
                        Spacer(Modifier.height(20.dp))
                        BattleAnswerOptions(
                            question = question,
                            selectedAnswer = battleState.selectedAnswer,
                            isAnswerRevealed = battleState.isAnswerRevealed,
                            onAnswerSelected = { isCorrect, option ->
                                viewModel.onAnswerSelected(isCorrect, option)
                            }
                        )
                    }
                    battleState.isTimerRunning || battleState.isLoadingQuestion -> {
                        Spacer(Modifier.height(48.dp))
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            battleState.error ?: "Loading question...",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    else -> Spacer(Modifier.height(48.dp))
                }

                if (!battleState.error.isNullOrBlank() && battleState.currentQuestion == null) {
                    Spacer(Modifier.height(12.dp))
                    Text(battleState.error!!, color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.loadNextQuestion() }) {
                        Text("Retry")
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (!battleState.isTimerRunning && battleState.questionsAnswered == 0 && !battleState.isGameOver) {
                    Button(
                        onClick = { viewModel.startBattle() },
                        modifier = Modifier
                            .widthIn(min = 140.dp, max = 200.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp)
                    ) {
                        Text(
                            "FIGHT!",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
