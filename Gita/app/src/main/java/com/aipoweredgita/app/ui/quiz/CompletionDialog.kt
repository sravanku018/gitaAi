package com.aipoweredgita.app.ui.quiz

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aipoweredgita.app.R

@Composable
fun CompletionDialog(
    score: Int,
    total: Int,
    onExit: () -> Unit,
    onRestart: () -> Unit
) {
    val percentage = if (total > 0) (score * 100) / total else 0
    val performanceMessage = when {
        percentage >= 90 -> "\uD83C\uDF1F " + stringResource(id = R.string.quiz_performance_outstanding)
        percentage >= 75 -> "\uD83D\uDC4F " + stringResource(id = R.string.quiz_performance_excellent)
        percentage >= 60 -> "\uD83D\uDC4D " + stringResource(id = R.string.quiz_performance_good)
        percentage >= 40 -> "\uD83D\uDCAA " + stringResource(id = R.string.quiz_performance_keep_practicing)
        else -> "\uD83D\uDE4C " + stringResource(id = R.string.quiz_performance_dont_give_up)
    }
    AlertDialog(
        onDismissRequest = { /* require explicit action */ },
        confirmButton = { Button(onClick = onExit) { Text(stringResource(id = R.string.quiz_exit)) } },
        dismissButton = { OutlinedButton(onClick = onRestart) { Text(stringResource(id = R.string.quiz_restart)) } },
        title = { Text(stringResource(id = R.string.quiz_complete_title)) },
        text = {
            Column {
                Text(text = stringResource(id = R.string.quiz_score, score, total), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(text = stringResource(id = R.string.quiz_accuracy, percentage), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text(text = performanceMessage, style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}
