package com.aipoweredgita.app.quiz.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aipoweredgita.app.quiz.QuizViewModel
import com.aipoweredgita.app.util.TextUtils

@Composable
fun QuizResultDialog(
    vm: QuizViewModel,
    onDismiss: () -> Unit,
    onReveal: (String) -> Unit,
    correctAnswerText: String,
) {
    val state by vm.uiState.collectAsState()
    if (!state.showResult) return

    val title = if (state.isCorrect == true) "Correct" else "Wrong"
    val message = state.revealedAnswer ?: if (state.isCorrect == true) "Good job!" else "Try again or reveal the answer."
    val revealInPopup = remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LaunchedEffect(revealInPopup.value) {
                if (revealInPopup.value) {
                    kotlinx.coroutines.delay(50)
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .padding(top = 4.dp),
            ) {
                Text(message, textAlign = TextAlign.Start)
                if (revealInPopup.value) {
                    Text(
                        text = TextUtils.sanitizeText(correctAnswerText),
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = {
                revealInPopup.value = true
                onReveal(correctAnswerText)
            }) { Text("Reveal Answer") }
        }
    )
}
