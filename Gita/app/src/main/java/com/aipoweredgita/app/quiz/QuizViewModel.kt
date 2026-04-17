package com.aipoweredgita.app.quiz

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class QuizUiState(
    val showResult: Boolean = false,
    val isCorrect: Boolean? = null,
    val revealedAnswer: String? = null,
    val selectedId: String? = null,
)

class QuizViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun submitAnswer(id: String, correctId: String) {
        val correct = id == correctId
        _uiState.value = _uiState.value.copy(
            selectedId = id,
            isCorrect = correct,
            showResult = true,
            revealedAnswer = null,
        )
    }

    fun revealAnswer(correctText: String) {
        _uiState.value = _uiState.value.copy(revealedAnswer = correctText)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(showResult = false)
    }
}
