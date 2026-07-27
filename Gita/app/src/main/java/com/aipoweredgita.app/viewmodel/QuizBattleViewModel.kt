package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.data.QuizState
import com.aipoweredgita.app.repository.MahabharataSequenceRepository
import com.aipoweredgita.app.repository.QuizQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizBattleViewModel @Inject constructor(
    private val quizQuestionRepository: QuizQuestionRepository,
    private val mahabharataSequenceRepository: MahabharataSequenceRepository
) : ViewModel() {

    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val sessionAskedIndices = mutableSetOf<Int>()
    private var currentLanguage = "english"

    fun setQuizLanguage(language: String) {
        if (currentLanguage != language) {
            currentLanguage = language
            sessionAskedIndices.clear()
        }
    }

    fun loadNextQuestion() {
        viewModelScope.launch {
            _quizState.value = _quizState.value.copy(isLoading = true, error = null)
            try {
                val seqQuestions = mahabharataSequenceRepository.getQuestions(currentLanguage)
                val availableIndices = seqQuestions.indices.filter { it !in sessionAskedIndices }

                if (availableIndices.isNotEmpty()) {
                    val randomIndex = availableIndices.random()
                    sessionAskedIndices.add(randomIndex)
                    val currentQuizQuestion = seqQuestions[randomIndex]

                    _quizState.value = _quizState.value.copy(
                        isLoading = false,
                        error = null,
                        currentQuestion = currentQuizQuestion,
                        totalQuestions = _quizState.value.totalQuestions + 1
                    )
                } else {
                    // Fallback to room db questions if sequence questions are exhausted
                    val limit = 1
                    val fetchLimit = maxOf(limit, 10)
                    val candidates = quizQuestionRepository.getNextQuestions(1, 10, fetchLimit)
                    val available = candidates.filter { it.id !in sessionAskedIndices }

                    if (available.isNotEmpty()) {
                        val q = available.first()
                        sessionAskedIndices.add(q.id)
                        val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD).filter { it.isNotBlank() }
                        val correctAnswerIndex = when (q.correctAnswer.trim().uppercase()) {
                            "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3
                            else -> options.indexOf(q.correctAnswer).takeIf { it >= 0 } ?: 0
                        }
                        val currentQuizQuestion = QuizQuestion(
                            verse = GitaVerse(chapterNo = q.chapter, verseNo = q.verse),
                            question = q.question,
                            options = options,
                            correctAnswerIndex = correctAnswerIndex,
                            explanation = q.explanation
                        )
                        quizQuestionRepository.markAsAsked(q.id)
                        _quizState.value = _quizState.value.copy(
                            isLoading = false,
                            error = null,
                            currentQuestion = currentQuizQuestion,
                            totalQuestions = _quizState.value.totalQuestions + 1
                        )
                    } else {
                        _quizState.value = _quizState.value.copy(isLoading = false, error = "No questions available")
                    }
                }
            } catch (e: Exception) {
                _quizState.value = _quizState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setQuizLimit(limit: Int) {
        _quizState.value = _quizState.value.copy(maxQuestions = limit)
    }

    fun restartQuiz() {
        sessionAskedIndices.clear()
        _quizState.value = QuizState()
    }
}
