package com.aipoweredgita.app.data

// Question types for quiz variety
enum class QuestionType {
    MCQ,
    ESSAY,
    COMPARISON,
    APPLICATION
}

data class QuizQuestion(
    val verse: GitaVerse,
    val question: String,
    val options: List<String> = emptyList(),
    val correctAnswerIndex: Int = -1,
    val type: QuestionType = QuestionType.MCQ,
    val explanation: String? = null,
    val rubricKeywords: List<String> = emptyList(),
    val theme: String? = null
)

data class QuizState(
    val currentQuestion: QuizQuestion? = null,
    val score: Int = 0,
    val totalQuestions: Int = 0,
    val maxQuestions: Int = 10,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedAnswerIndex: Int? = null,
    val openEndedAnswer: String = "",
    val showAnswer: Boolean = false,
    val showCorrectAnswer: Boolean = false,
    val isQuizComplete: Boolean = false,
    val difficultyLevel: Int = 5
)
