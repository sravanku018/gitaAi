package com.aipoweredgita.app.quiz

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevealAnswerSheet(
    open: Boolean,
    question: String,
    answer: String,
    options: List<String> = emptyList(),
    correctIndex: Int? = null,
    selectedIndex: Int? = null,
    onDismiss: () -> Unit,
    onRevealed: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Persist across rotation; reset each time the sheet is opened
    val revealedState = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open) revealedState.value = false
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
        ) {
            val listState = rememberLazyListState()
            val revealed = revealedState.value

            // Auto-scroll to correct on reveal when options are present
            LaunchedEffect(revealed) {
                if (revealed && correctIndex != null && options.isNotEmpty()) {
                    listState.animateScrollToItem(index = correctIndex)
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PaddingValues(horizontal = 20.dp, vertical = 16.dp)),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.titleLarge,
                    )
                }

                if (options.isNotEmpty()) {
                    itemsIndexed(options) { index, option ->
                        val isCorrect = revealed && (correctIndex == index)
                        val isWrong = revealed && (selectedIndex == index && correctIndex != index)
                        val bg: Color = when {
                            isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            isWrong -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else -> Color.Transparent
                        }
                        val textColor: Color = when {
                            isCorrect -> MaterialTheme.colorScheme.primary
                            isWrong -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Text(
                            text = option,
                            color = textColor,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .padding(12.dp)
                        )
                    }
                }

                item {
                    AnimatedVisibility(
                        visible = revealed,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Text(
                            text = answer,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                item { Spacer(modifier = Modifier.size(8.dp)) }

                // Reveal button removed; reveal is handled inside popup dialog now.

                item { Spacer(modifier = Modifier.size(8.dp)) }
            }
        }
    }
}
