package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.database.QuizQuestionBank

/**
 * UI State for Quiz Screen
 * Single source of truth for all quiz-related UI state
 */
data class QuizUiState(
    val questions: List<QuizQuestionBank> = emptyList(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    override val isLoading: Boolean = false,
    override val error: String? = null,
    val isQuizCompleted: Boolean = false,
    val timeSpentSeconds: Long = 0L,
    val coinsEarned: Int = 0
) : BaseUiState

/**
 * Events that can occur on the Quiz screen
 */
sealed class QuizEvent {
    data class SelectAnswer(val answer: String) : QuizEvent()
    data object ConfirmAnswer : QuizEvent()
    data object NextQuestion : QuizEvent()
    data object FinishQuiz : QuizEvent()
    data object RestartQuiz : QuizEvent()
    data class SetQuizConfig(val questionCount: Int, val language: String) : QuizEvent()
    data class ProceedToNextOrFinish(val wasCorrect: Boolean) : QuizEvent()
}

/**
 * One-time side effects for the Quiz screen
 */
sealed class QuizSideEffect {
    data class ShowToast(val message: String) : QuizSideEffect()
    data class ShowError(val message: String) : QuizSideEffect()
    data class QuizCompleted(val score: Int, val total: Int, val coinsEarned: Int) : QuizSideEffect()
    data object NavigateToStats : QuizSideEffect()
    /** Fired when the question timer hits zero with no answer selected. */
    data object TimeUp : QuizSideEffect()
}
