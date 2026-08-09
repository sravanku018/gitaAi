package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.QuizState
import com.aipoweredgita.app.repository.MahabharataSequenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizBattleViewModel @Inject constructor(
    private val mahabharataSequenceRepository: MahabharataSequenceRepository
) : ViewModel() {

    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val sessionAskedIndices = mutableSetOf<Int>()
    private var currentLanguage = if (java.util.Locale.getDefault().language.startsWith("te")) "telugu" else "english"

    fun setQuizLanguage(language: String) {
        val targetLang = when (language.lowercase().trim()) {
            "te", "telugu" -> "telugu"
            "en", "english" -> "english"
            "both", "bilingual", "all" -> "both"
            else -> if (java.util.Locale.getDefault().language.startsWith("te")) "telugu" else "english"
        }
        if (currentLanguage != targetLang) {
            currentLanguage = targetLang
            sessionAskedIndices.clear()
        }
    }

    fun loadNextQuestion() {
        viewModelScope.launch {
            _quizState.value = _quizState.value.copy(isLoading = true, error = null)
            try {
                val seqQuestions = mahabharataSequenceRepository.getQuestions(currentLanguage)
                if (seqQuestions.isEmpty()) {
                    _quizState.value = _quizState.value.copy(isLoading = false, error = "No Mahabharata battle questions available")
                    return@launch
                }

                var availableIndices = seqQuestions.indices.filter { it !in sessionAskedIndices }
                if (availableIndices.isEmpty()) {
                    // If session asked set covers all questions, reset to repeat without duplicate repetition
                    sessionAskedIndices.clear()
                    availableIndices = seqQuestions.indices.toList()
                }

                val randomIndex = availableIndices.random()
                sessionAskedIndices.add(randomIndex)
                val currentQuizQuestion = seqQuestions[randomIndex]

                _quizState.value = _quizState.value.copy(
                    isLoading = false,
                    error = null,
                    currentQuestion = currentQuizQuestion,
                    totalQuestions = _quizState.value.totalQuestions + 1
                )
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
