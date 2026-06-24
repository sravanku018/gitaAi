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

    // Reset local state when question changes (new question = new options list)
    LaunchedEffect(options) {
        showResult = null
        showResultDialog = false
    }

    // Timer state from ViewModel
    val quizState = vm?.quizState?.collectAsState()
    val timeLeft = quizState?.value?.questionTimeLeftSeconds ?: 30
    val isTimerRunning = quizState?.value?.isTimerRunning ?: false
    val language = quizState?.value?.language ?: "en"
    val questionType = quizState?.value?.currentQuestion?.type ?: com.aipoweredgita.app.data.QuestionType.MCQ
    val isOpenEnded = questionType == com.aipoweredgita.app.data.QuestionType.ESSAY || questionType == com.aipoweredgita.app.data.QuestionType.APPLICATION
    val maxTime = if (isOpenEnded) 60 else 30

    // Staggered enter animation state for options
    val appeared = remember(options) { List(options.size) { mutableStateOf(false) } }
    LaunchedEffect(options) {
        if (!isOpenEnded) {
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
        .background(MaterialTheme.colorScheme.background)) {
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
                maxTime = maxTime,
                isTimerRunning = isTimerRunning,
                questionNumber = quizState?.value?.totalQuestions ?: 0,
                totalQuestions = quizState?.value?.maxQuestions ?: 0,
                score = quizState?.value?.score ?: 0,
                language = language
            )

            // Ornamental separator
            OrnamentRule()

            Text(
                text = TextUtils.sanitizeText(question),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                lineHeight = 28.sp
            )

            if (isOpenEnded) {
                OutlinedTextField(
                    value = userAnswer,
                    onValueChange = { userAnswer = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(translateUiText("Type your answer here...", language), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    minLines = 6,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.secondary,
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(Modifier.size(8.dp))

                Button(
                    onClick = {
                        vm?.submitOpenEndedAnswer(userAnswer)
                        showResult = true
                    },
                    enabled = userAnswer.trim().isNotEmpty() && selectedIndex == null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(translateUiText("Submit Answer", language), fontWeight = FontWeight.Bold)
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
                        visible = appeared.getOrNull(index)?.value == true,
                        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
                        exit = fadeOut()
                    ) {
                        AnimatedOptionCard(
                            text = option,
                            state = state,
                            enabled = selectedIndex == null && showResult == null && !showResultDialog,
                            onClick = {
                                onSelect(index)
                                val correct = index == correctIndex
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
            val isCorrect = if (isOpenEnded) {
                quizState?.value?.showCorrectAnswer == true
            } else {
                selectedIndex == correctIndex
            }

            Dialog(onDismissRequest = {
                showResultDialog = false
                onProceed(isCorrect)
            }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(0.2f))
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
                                color = if (isCorrect) MaterialTheme.colorScheme.secondary else Color.Red,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = translateUiText(if (isCorrect) "Excellent!" else if (isOpenEnded) "Insight Shared" else "Keep Learning", language),
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isCorrect) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = translateUiText(if (isCorrect) "You have grasped the wisdom correctly." else "Every step is a progress toward mastery.", language),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        if (!isCorrect || isOpenEnded) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 1.dp)
                            Text(
                                text = TextUtils.sanitizeText(answer),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(0.8f),
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
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(translateUiText("Continue Journey", language), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        AnswerOverlay(show = showResult != null, isCorrect = showResult == true)
    }
}

@Composable
fun OrnamentRule() {
    val ornamentColor = MaterialTheme.colorScheme.secondary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, ornamentColor.copy(0.5f)))))
        Box(modifier = Modifier.padding(horizontal = 8.dp).size(6.dp).clip(CircleShape).background(ornamentColor))
        Box(modifier = Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(ornamentColor.copy(0.5f), Color.Transparent))))
    }
}

@Composable
private fun QuizTimerHeader(
    timeLeft: Int,
    maxTime: Int,
    isTimerRunning: Boolean,
    questionNumber: Int,
    totalQuestions: Int,
    score: Int,
    language: String
) {
    val progress = if (maxTime > 0) timeLeft.toFloat() / maxTime else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = com.aipoweredgita.app.ui.theme.MotionTokens.springSmooth<Float>(),
        label = "progress"
    )

    val timerColor = when {
        timeLeft > 15 -> Forest
        timeLeft > 5 -> Saffron
        else -> MaterialTheme.colorScheme.error
    }
    val animatedTimerColor by animateColorAsState(
        targetValue = timerColor,
        animationSpec = com.aipoweredgita.app.ui.theme.MotionTokens.springSmooth<androidx.compose.ui.graphics.Color>(),
        label = "timer_color"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant)
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
                        text = translateUiText("PROGRESS", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "$questionNumber / $totalQuestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                // Timer - Circular
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.size(48.dp),
                        color = animatedTimerColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
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
                        text = translateUiText("SCORE", language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = score.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

fun translateUiText(text: String, language: String): String {
    if (language.lowercase() != "tel") return text
    return when (text) {
        "Type your answer here..." -> "మీ సమాధానాన్ని ఇక్కడ నమోదు చేయండి..."
        "Submit Answer" -> "సమాధానాన్ని సమర్పించండి"
        "PROGRESS" -> "పురోగతి"
        "SCORE" -> "స్కోరు"
        "Excellent!" -> "అద్భుతం!"
        "Insight Shared" -> "అంతర్దృష్టి పంచుకోబడింది"
        "Keep Learning" -> "నిరంతరం నేర్చుకోండి"
        "You have grasped the wisdom correctly." -> "మీరు జ్ఞానాన్ని సరిగ్గా గ్రహించారు."
        "Every step is a progress toward mastery." -> "ప్రతి అడుగు నిపుణత వైపు సాగే పురోగతి."
        "Continue Journey" -> "ప్రయాణాన్ని కొనసాగించండి"
        else -> text
    }
}
