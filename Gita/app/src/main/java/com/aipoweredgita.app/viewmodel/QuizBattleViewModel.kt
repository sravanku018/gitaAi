package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.data.QuizState
import com.aipoweredgita.app.repository.QuizQuestionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizBattleViewModel @Inject constructor(
    private val quizQuestionRepository: QuizQuestionRepository
) : ViewModel() {

    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val sessionAskedIds = mutableSetOf<Int>()

    fun loadNextQuestion() {
        viewModelScope.launch {
            _quizState.value = _quizState.value.copy(isLoading = true, error = null)
            try {
                val limit = 1
                val fetchLimit = maxOf(limit, 10)
                val candidates = quizQuestionRepository.getNextQuestions(1, 10, fetchLimit)
                val available = candidates.filter { it.id !in sessionAskedIds }

                if (available.isNotEmpty()) {
                    val q = available.first()
                    sessionAskedIds.add(q.id)
                    val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD).filter { it.isNotBlank() }
                    val correctAnswerIndex = when (q.correctAnswer.trim().uppercase()) {
                        "A" -> 0; "B" -> 1; "C" -> 2; "D" -> 3
                        else -> options.indexOf(q.correctAnswer).takeIf { it >= 0 } ?: 0
                    }
                    val verseObj = GitaVerse(chapterNo = q.chapter, verseNo = q.verse)
                    val currentQuizQuestion = QuizQuestion(
                        verse = verseObj,
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
            } catch (e: Exception) {
                _quizState.value = _quizState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun setQuizLimit(limit: Int) {
        _quizState.value = _quizState.value.copy(maxQuestions = limit)
    }

    fun setQuizLanguage(language: String) {
        // Battle mode always uses English
    }

    fun restartQuiz() {
        sessionAskedIds.clear()
        _quizState.value = QuizState()
    }
}
