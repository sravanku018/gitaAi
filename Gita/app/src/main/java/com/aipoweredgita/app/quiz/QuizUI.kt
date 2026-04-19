package com.aipoweredgita.app.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aipoweredgita.app.quiz.ui.AnimatedOptionCard
import com.aipoweredgita.app.quiz.ui.CelebrationOverlay
import com.aipoweredgita.app.quiz.ui.WrongOverlay
import com.aipoweredgita.app.quiz.ui.OptionVisualState
import com.aipoweredgita.app.quiz.ui.QuizResultDialog
import kotlinx.coroutines.delay
import com.aipoweredgita.app.viewmodel.QuizViewModel
import com.aipoweredgita.app.data.QuestionType
import com.aipoweredgita.app.util.TextUtils

private val TimerGreen = Color(0xFF4CAF50)
private val TimerYellow = Color(0xFFFFC107)
private val TimerRed = Color(0xFFF44336)

@Composable
fun QuizContent(
    question: String,
    answer: String,
    options: List<String>,
    correctIndex: Int,
    selectedIndex: Int?,
    questionType: QuestionType = QuestionType.MCQ,
    onSelect: (Int) -> Unit,
    onSubmitAnswer: ((String) -> Unit)? = null,
    onProceed: (wasCorrect: Boolean) -> Unit,
    vm: QuizViewModel? = null,
) {
    var showResult by remember { mutableStateOf<Boolean?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }
    var userAnswer by remember { mutableStateOf("") }
    val dialogScroll = rememberScrollState()
    val contentScroll = rememberScrollState()

    val isOpenEnded = questionType == QuestionType.ESSAY || questionType == QuestionType.APPLICATION

    // Timer state from ViewModel
    val quizState = vm?.quizState?.collectAsState()
    val timeLeft = quizState?.value?.questionTimeLeftSeconds ?: 30
    val isTimerRunning = quizState?.value?.isTimerRunning ?: false

    if (!isOpenEnded) {
        val appeared = remember(options) { List(options.size) { mutableStateOf(false) } }
        LaunchedEffect(options) {
            appeared.forEachIndexed { i, state ->
                delay(60L)
                state.value = true
            }
        }
    }

    LaunchedEffect(showResult) {
        if (showResult != null) {
            delay(if (showResult == true) 2000L else 1000L)
            showResult = null
            showResultDialog = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(contentScroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Pill-shaped quiz header with timer
            QuizTimerHeader(
                timeLeft = timeLeft,
                maxTime = 30,
                isTimerRunning = isTimerRunning,
                questionNumber = quizState?.value?.totalQuestions ?: 0,
                totalQuestions = quizState?.value?.maxQuestions ?: 0,
                score = quizState?.value?.score ?: 0
            )

            Text(text = TextUtils.sanitizeText(question), style = MaterialTheme.typography.titleLarge)

            if (isOpenEnded) {
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type your answer here...") },
                    minLines = 6,
                    maxLines = 10
                )

                Spacer(Modifier.size(8.dp))

                Button(
                    onClick = {
                        onSubmitAnswer?.invoke(userAnswer)
                        val correct = userAnswer.trim().isNotEmpty()
                        vm?.submitOpenEndedAnswer(userAnswer)
                        showResult = correct
                    },
                    enabled = userAnswer.trim().isNotEmpty() && selectedIndex == null,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Submit Answer")
                }
            } else {
                options.forEachIndexed { index, option ->
                    val state = when {
                        selectedIndex == null -> OptionVisualState.Idle
                        selectedIndex == index && !showResultDialog -> OptionVisualState.Selected
                        showResultDialog && index == correctIndex -> OptionVisualState.Correct
                        showResultDialog && selectedIndex == index && index != correctIndex -> OptionVisualState.Wrong
                        else -> OptionVisualState.Idle
                    }
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })
                    ) {
                        AnimatedOptionCard(
                            text = option,
                            state = state,
                            enabled = selectedIndex == null && showResult == null && !showResultDialog,
                            onClick = {
                                onSelect(index)
                                val correct = index == correctIndex
                                vm?.selectAnswer(index)
                                showResult = correct
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.size(8.dp))
        }

        if (showResultDialog) {
            Dialog(onDismissRequest = {
                showResultDialog = false
            }) {
                Card(colors = CardDefaults.cardColors()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                            .heightIn(max = 420.dp)
                            .verticalScroll(dialogScroll),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val isCorrect = if (isOpenEnded) {
                            showResult == true
                        } else {
                            selectedIndex == correctIndex
                        }

                        if (isCorrect) {
                            Text(
                                text = "Excellent!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(text = if (isOpenEnded) "Great explanation! Well thought out." else "You nailed it! Keep up the great work.")
                            Button(
                                onClick = {
                                    showResultDialog = false
                                    onProceed(true)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Continue")
                            }
                        } else {
                            Text(
                                text = if (isOpenEnded) "Thanks for sharing!" else "Don't worry!",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(text = "Keep learning and you'll master this!")
                            Text(
                                text = TextUtils.sanitizeText(answer),
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Button(
                                onClick = {
                                    showResultDialog = false
                                    onProceed(false)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Continue")
                            }
                        }
                    }
                }
            }
        }

        CelebrationOverlay(show = showResult == true)
        WrongOverlay(show = showResult == false)
    }
}

@Composable
private fun QuizTimerHeader(
    timeLeft: Int,
    maxTime: Int,
    isTimerRunning: Boolean,
    questionNumber: Int,
    totalQuestions: Int,
    score: Int
) {
    val progress = if (maxTime > 0) timeLeft.toFloat() / maxTime else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "timer_progress"
    )

    val timerColor = when {
        timeLeft > 15 -> TimerGreen
        timeLeft > 5 -> TimerYellow
        else -> TimerRed
    }
    val animatedTimerColor by animateColorAsState(targetValue = timerColor, label = "timer_color")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Question counter pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Q $questionNumber/$totalQuestions",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = animatedTimerColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${timeLeft}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = animatedTimerColor,
                        fontSize = 18.sp
                    )
                }

                // Score pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Score: $score",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Timer progress bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = 50.dp, bottomEnd = 50.dp)),
                color = animatedTimerColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
