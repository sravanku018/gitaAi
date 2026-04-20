package com.aipoweredgita.app.quiz

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aipoweredgita.app.R
import com.aipoweredgita.app.data.QuestionType
import com.aipoweredgita.app.quiz.ui.*
import com.aipoweredgita.app.ui.theme.*
import com.aipoweredgita.app.util.TextUtils
import com.aipoweredgita.app.viewmodel.QuizViewModel
import kotlinx.coroutines.delay

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

    Box(modifier = Modifier
        .fillMaxSize()
        .background(BgDark)) {
        // Ambient background image
        Image(
            painter = painterResource(id = R.drawable.krishna), // Using Krishna as background
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.05f),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(contentScroll)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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

            // Ornamental separator
            OrnamentRule()

            Text(
                text = TextUtils.sanitizeText(question),
                style = MaterialTheme.typography.headlineSmall,
                color = TextWhite,
                fontWeight = FontWeight.Medium,
                lineHeight = 28.sp
            )

            if (isOpenEnded) {
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Type your answer here...", color = TextDim) },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        cursorColor = GoldSpark,
                        focusedBorderColor = GoldSpark,
                        unfocusedBorderColor = Surface2,
                        focusedContainerColor = Surface1,
                        unfocusedContainerColor = Surface1
                    ),
                    shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Saffron,
                        disabledContainerColor = Surface2
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Submit Answer", fontWeight = FontWeight.Bold)
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

            Spacer(Modifier.size(24.dp))
        }

        if (showResultDialog) {
            Dialog(onDismissRequest = {
                showResultDialog = false
            }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Surface1),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, GoldSpark.copy(0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                            .heightIn(max = 500.dp)
                            .verticalScroll(dialogScroll),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isCorrect = if (isOpenEnded) {
                            showResult == true
                        } else {
                            selectedIndex == correctIndex
                        }

                        // Icon/Visual feedback
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (isCorrect) Forest.copy(0.2f) else CrimsonDeep.copy(0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCorrect) "✓" else "✕",
                                color = if (isCorrect) GoldSpark else Color.Red,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = if (isCorrect) "Excellent!" else if (isOpenEnded) "Insight Shared" else "Keep Learning",
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isCorrect) GoldSpark else Saffron,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isCorrect) "You have grasped the wisdom correctly." else "Every step is a progress toward mastery.",
                            textAlign = TextAlign.Center,
                            color = TextWhite
                        )

                        if (!isCorrect || isOpenEnded) {
                            HorizontalDivider(color = Surface2, thickness = 1.dp)
                            Text(
                                text = TextUtils.sanitizeText(answer),
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextWhite.copy(0.8f),
                                textAlign = TextAlign.Start,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Button(
                            onClick = {
                                showResultDialog = false
                                onProceed(isCorrect)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Saffron),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Continue Journey", fontWeight = FontWeight.Bold)
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
fun OrnamentRule() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, GoldSpark.copy(0.5f)))))
        Box(modifier = Modifier.padding(horizontal = 8.dp).size(6.dp).clip(CircleShape).background(GoldSpark))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(GoldSpark.copy(0.5f), Color.Transparent))))
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
        shape = RoundedCornerShape(16.dp),
        color = Surface1,
        border = BorderStroke(1.dp, Surface2)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Question counter
                Column {
                    Text(
                        text = "PROGRESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$questionNumber / $totalQuestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldSpark
                    )
                }

                // Timer - Circular
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(48.dp),
                        color = animatedTimerColor,
                        trackColor = Surface2,
                        strokeWidth = 3.dp,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = "${timeLeft}s",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = animatedTimerColor
                    )
                }

                // Score
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "SCORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDim,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldSpark
                    )
                }
            }
        }
    }
}
