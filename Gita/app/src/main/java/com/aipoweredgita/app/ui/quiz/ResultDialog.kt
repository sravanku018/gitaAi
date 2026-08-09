package com.aipoweredgita.app.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import com.aipoweredgita.app.R
import com.aipoweredgita.app.util.TextUtils

@Composable
fun ResultDialog(
    isCorrect: Boolean,
    correctAnswerText: String?,
    explanation: String,
    responseSeconds: String?,
    onNext: () -> Unit,
    onReveal: (() -> Unit)? = null
) {
    var revealed by remember { mutableStateOf(isCorrect) }
    val scrollState = rememberScrollState()
    AlertDialog(
        onDismissRequest = onNext,
        confirmButton = {
            Button(onClick = onNext) { Text(stringResource(id = R.string.quiz_next_question)) }
        },
        title = {
            val titleText = if (isCorrect)
                "\u2705 " + stringResource(id = R.string.quiz_result_correct)
            else
                "\u274C " + stringResource(id = R.string.quiz_result_incorrect)
            Text(titleText)
        },
        text = {
            LaunchedEffect(revealed) {
                if (revealed) {
                    kotlinx.coroutines.delay(50)
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState)
                    .clickable { onNext() }
            ) {
                if (!revealed && onReveal != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                        OutlinedButton(onClick = {
                            try { onReveal() } catch (e: Exception) {
                                android.util.Log.w("ResultDialog", "onReveal failed", e)
                            }
                            revealed = true
                        }) { Text(stringResource(id = R.string.quiz_reveal_answer)) }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (responseSeconds != null) {
                    Text(
                        text = stringResource(id = R.string.quiz_response_time, responseSeconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                }
                if (revealed && correctAnswerText != null) {
                    Text(text = stringResource(id = R.string.quiz_correct_answer, TextUtils.sanitizeText(correctAnswerText)), style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                }
                if (explanation.isNotBlank()) {
                    Text(text = explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
}
