package com.aipoweredgita.app.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            Text(text = TextUtils.sanitizeText(question), style = MaterialTheme.typography.titleLarge)

            if (isOpenEnded) {
                // Open-ended question UI (Essay/Application)
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
                        // Simple scoring: check if user typed something
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
                // Multiple Choice Question UI
                options.forEachIndexed { index, option ->
                    val state = when {
                        selectedIndex == null -> OptionVisualState.Idle
                        selectedIndex == index && !showResultDialog -> OptionVisualState.Selected
                        // After result dialog appears, show correct in green, selected wrong in red
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
                                text = "Excellent! 🎉",
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
                                text = if (isOpenEnded) "Thanks for sharing! 💭" else "Don't worry! 💭",
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