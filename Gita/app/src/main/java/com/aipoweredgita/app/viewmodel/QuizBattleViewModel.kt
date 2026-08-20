package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.data.QuizState
import com.aipoweredgita.app.repository.MahabharataSequenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BattleUiState(
    val score: Int = 0,
    val lives: Int = 3,
    val combo: Int = 0,
    val maxCombo: Int = 0,
    val timeLeft: Int = 60,
    val isGameOver: Boolean = false,
    val isTimerRunning: Boolean = false,
    val currentQuestion: QuizQuestion? = null,
    val selectedAnswer: String? = null,
    val isAnswerRevealed: Boolean = false,
    val questionsAnswered: Int = 0,
    val correctAnswers: Int = 0,
    val correctAt3Hearts: Int = 0,
    val correctAt2Hearts: Int = 0,
    val correctAt1Heart: Int = 0,
    val language: String = if (java.util.Locale.getDefault().language.startsWith("te")) "telugu" else "english",
    val isLoadingQuestion: Boolean = false,
    val error: String? = null,
    val revealBeatActive: Boolean = false,
) {
    val battleCoins: Int
        get() {
            if (correctAnswers <= 0) return 0
            var a = 1
            var b = 1
            for (i in 3..correctAnswers) {
                val temp = a + b
                a = b
                b = temp
            }
            return if (correctAnswers == 1) a else b
        }
}

sealed class BattleSideEffect {
    data class GameOver(
        val correctAnswers: Int,
        val maxCombo: Int,
        val questionsAnswered: Int,
        val battleCoins: Int,
        val language: String,
    ) : BattleSideEffect()
}

@HiltViewModel
class QuizBattleViewModel @Inject constructor(
    private val mahabharataSequenceRepository: MahabharataSequenceRepository
) : ViewModel() {

    private val _battleState = MutableStateFlow(BattleUiState())
    val battleState: StateFlow<BattleUiState> = _battleState.asStateFlow()

    /** Kept for question loading compatibility with prior UI. */
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<BattleSideEffect>(extraBufferCapacity = 1)
    val sideEffect: SharedFlow<BattleSideEffect> = _sideEffect.asSharedFlow()

    private val sessionAskedIndices = mutableSetOf<Int>()
    private var timerJob: Job? = null
    private var revealJob: Job? = null
    private var gameOverEmitted = false

    fun setQuizLanguage(language: String) {
        val targetLang = when (language.lowercase().trim()) {
            "te", "telugu" -> "telugu"
            "en", "english" -> "english"
            "both", "bilingual", "all" -> "both"
            else -> if (java.util.Locale.getDefault().language.startsWith("te")) "telugu" else "english"
        }
        if (_battleState.value.language != targetLang) {
            sessionAskedIndices.clear()
            _battleState.update { it.copy(language = targetLang) }
        }
    }

    fun setQuizLimit(limit: Int) {
        _quizState.value = _quizState.value.copy(maxQuestions = limit)
    }

    fun startBattle() {
        if (_battleState.value.isTimerRunning) return
        gameOverEmitted = false
        _battleState.update {
            it.copy(
                isTimerRunning = true,
                isGameOver = false,
                timeLeft = 60,
                error = null
            )
        }
        startTimer()
        loadNextQuestion()
    }

    fun cycleLanguage() {
        val next = when (_battleState.value.language) {
            "english" -> "telugu"
            "telugu" -> "both"
            else -> "english"
        }
        setQuizLanguage(next)
        if (_battleState.value.isTimerRunning && !_battleState.value.isGameOver) {
            loadNextQuestion()
        }
    }

    fun loadNextQuestion() {
        viewModelScope.launch {
            _battleState.update { it.copy(isLoadingQuestion = true, error = null) }
            _quizState.value = _quizState.value.copy(isLoading = true, error = null)
            try {
                val lang = _battleState.value.language
                val seqQuestions = mahabharataSequenceRepository.getQuestions(lang)
                if (seqQuestions.isEmpty()) {
                    _battleState.update {
                        it.copy(isLoadingQuestion = false, error = "No Mahabharata battle questions available")
                    }
                    _quizState.value = _quizState.value.copy(
                        isLoading = false,
                        error = "No Mahabharata battle questions available"
                    )
                    return@launch
                }

                var availableIndices = seqQuestions.indices.filter { it !in sessionAskedIndices }
                if (availableIndices.isEmpty()) {
                    sessionAskedIndices.clear()
                    availableIndices = seqQuestions.indices.toList()
                }

                // Skip questions with empty options (dead-end guard)
                var attempts = 0
                var currentQuizQuestion: QuizQuestion? = null
                while (attempts < seqQuestions.size) {
                    val randomIndex = availableIndices.random()
                    sessionAskedIndices.add(randomIndex)
                    val candidate = seqQuestions[randomIndex]
                    if (candidate.options.isNotEmpty() &&
                        candidate.correctAnswerIndex in candidate.options.indices
                    ) {
                        currentQuizQuestion = candidate
                        break
                    }
                    availableIndices = seqQuestions.indices.filter { it !in sessionAskedIndices }
                    if (availableIndices.isEmpty()) {
                        sessionAskedIndices.clear()
                        availableIndices = seqQuestions.indices.toList()
                    }
                    attempts++
                }

                if (currentQuizQuestion == null) {
                    _battleState.update {
                        it.copy(isLoadingQuestion = false, error = "No valid battle questions with options")
                    }
                    return@launch
                }

                _quizState.value = _quizState.value.copy(
                    isLoading = false,
                    error = null,
                    currentQuestion = currentQuizQuestion,
                    totalQuestions = _quizState.value.totalQuestions + 1
                )
                _battleState.update {
                    it.copy(
                        isLoadingQuestion = false,
                        currentQuestion = currentQuizQuestion,
                        selectedAnswer = null,
                        isAnswerRevealed = false,
                        revealBeatActive = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _battleState.update { it.copy(isLoadingQuestion = false, error = e.message) }
                _quizState.value = _quizState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onAnswerSelected(isCorrect: Boolean, option: String) {
        val state = _battleState.value
        if (state.isAnswerRevealed || state.isGameOver || state.revealBeatActive) return

        var next = state.copy(selectedAnswer = option, isAnswerRevealed = true, revealBeatActive = true)

        if (isCorrect) {
            val comboMultiplier = (next.combo + 1).coerceAtMost(5)
            val points = 10 * comboMultiplier
            next = next.copy(
                score = next.score + points,
                combo = next.combo + 1,
                maxCombo = maxOf(next.maxCombo, next.combo + 1),
                questionsAnswered = next.questionsAnswered + 1,
                correctAnswers = next.correctAnswers + 1,
                correctAt3Hearts = if (next.lives == 3) next.correctAt3Hearts + 1 else next.correctAt3Hearts,
                correctAt2Hearts = if (next.lives == 2) next.correctAt2Hearts + 1 else next.correctAt2Hearts,
                correctAt1Heart = if (next.lives == 1) next.correctAt1Heart + 1 else next.correctAt1Heart
            )
            _battleState.value = next
            // Brief beat so the player sees the correct highlight before next question
            revealJob?.cancel()
            revealJob = viewModelScope.launch {
                delay(900)
                if (!_battleState.value.isGameOver) {
                    _battleState.update { it.copy(revealBeatActive = false) }
                    loadNextQuestion()
                }
            }
        } else {
            next = next.copy(
                lives = next.lives - 1,
                combo = 0,
                questionsAnswered = next.questionsAnswered + 1
            )
            if (next.lives <= 0) {
                next = next.copy(isGameOver = true, isTimerRunning = false, revealBeatActive = false)
                _battleState.value = next
                stopTimer()
                emitGameOverOnce(next)
            } else {
                _battleState.value = next
                revealJob?.cancel()
                revealJob = viewModelScope.launch {
                    delay(900)
                    if (!_battleState.value.isGameOver) {
                        _battleState.update { it.copy(revealBeatActive = false) }
                        loadNextQuestion()
                    }
                }
            }
        }
    }

    fun playAgain() {
        timerJob?.cancel()
        revealJob?.cancel()
        sessionAskedIndices.clear()
        gameOverEmitted = false
        _quizState.value = QuizState(maxQuestions = 999)
        _battleState.value = BattleUiState(language = _battleState.value.language)
        startBattle()
    }

    fun restartQuiz() {
        sessionAskedIndices.clear()
        _quizState.value = QuizState()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                val s = _battleState.value
                if (!s.isTimerRunning || s.isGameOver) break
                val newTime = s.timeLeft - 1
                if (newTime <= 0) {
                    val ended = s.copy(timeLeft = 0, isTimerRunning = false, isGameOver = true)
                    _battleState.value = ended
                    emitGameOverOnce(ended)
                    break
                } else {
                    _battleState.update { it.copy(timeLeft = newTime) }
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _battleState.update { it.copy(isTimerRunning = false) }
    }

    private fun emitGameOverOnce(state: BattleUiState) {
        if (gameOverEmitted) return
        gameOverEmitted = true
        viewModelScope.launch {
            _sideEffect.emit(
                BattleSideEffect.GameOver(
                    correctAnswers = state.correctAnswers,
                    maxCombo = state.maxCombo,
                    questionsAnswered = state.questionsAnswered,
                    battleCoins = state.battleCoins,
                    language = state.language
                )
            )
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        revealJob?.cancel()
        super.onCleared()
    }
}
