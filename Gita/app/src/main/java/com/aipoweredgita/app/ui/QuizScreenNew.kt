package com.aipoweredgita.app.ui

import androidx.compose.runtime.Composable
import com.aipoweredgita.app.quiz.QuizContent

@Composable
fun QuizScreenNew(
    question: String,
    answer: String,
    options: List<String>,
    correctIndex: Int,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
    onProceed: (wasCorrect: Boolean) -> Unit,
) {
    QuizContent(
        question = question,
        answer = answer,
        options = options,
        correctIndex = correctIndex,
        selectedIndex = selectedIndex,
        onSelect = onSelect,
        onProceed = onProceed,
    )
}

