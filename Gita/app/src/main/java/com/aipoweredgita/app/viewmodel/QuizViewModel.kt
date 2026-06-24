package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.data.QuizState
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.QuizQuestionBank
import com.aipoweredgita.app.domain.model.QuizEvent
import com.aipoweredgita.app.domain.model.QuizSideEffect
import com.aipoweredgita.app.domain.model.QuizUiState
import com.aipoweredgita.app.ml.AdaptiveDifficultyEngine
import com.aipoweredgita.app.ml.HuggingFaceMLManager
import com.aipoweredgita.app.repository.QuizRepository
import com.aipoweredgita.app.repository.QuizQuestionRepository
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.OfflineCacheRepository
import com.aipoweredgita.app.repository.YogaProgressionRepository
import com.aipoweredgita.app.utils.QuizPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * Quiz ViewModel with UDF pattern
 * Uses constructor injection via Hilt
 */
@HiltViewModel
class QuizViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val quizRepository: QuizRepository,
    private val quizQuestionRepository: QuizQuestionRepository,
    private val offlineCacheRepository: OfflineCacheRepository,
    private val quizPreferences: QuizPreferences,
    private val application: Application
) : ViewModel() {

    // Single UI state
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    // One-time side effects
    private val _sideEffect = MutableSharedFlow<QuizSideEffect>()
    val sideEffect: SharedFlow<QuizSideEffect> = _sideEffect.asSharedFlow()

    // Legacy quiz state for compatibility
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val mlManager = HuggingFaceMLManager(application)
    private val difficultyEngine = AdaptiveDifficultyEngine()
    private var userState = AdaptiveDifficultyEngine.UserState()
    private var quizStartTime: Long = 0
    private var timerJob: kotlinx.coroutines.Job? = null
    // Track question IDs asked in the current quiz session to prevent repeats
    private val sessionAskedIds = mutableSetOf<Int>()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val success = com.aipoweredgita.app.ml.TranslationManager().downloadModelsIfNeeded()
            if (!success) {
                _sideEffect.emit(QuizSideEffect.ShowError("Failed to download translation models"))
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Handle events from the UI
     */
    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.SelectAnswer -> selectAnswer(event.answer)
            is QuizEvent.ConfirmAnswer -> confirmAnswer()
            is QuizEvent.NextQuestion -> loadNextQuestion()
            is QuizEvent.FinishQuiz -> finishQuiz()
            is QuizEvent.RestartQuiz -> restartQuiz()
            is QuizEvent.SetQuizConfig -> setQuizConfig(event.questionCount, event.language)
        }
    }

    /**
     * Set quiz configuration
     */
    private fun setQuizConfig(questionCount: Int, language: String) {
        _quizState.value = com.aipoweredgita.app.data.QuizState(maxQuestions = questionCount, language = language)
        _uiState.value = QuizUiState()
        quizStartTime = System.currentTimeMillis()
        sessionAskedIds.clear()
        viewModelScope.launch {
            quizPreferences.clearQuizState()
        }
    }

    /**
     * Load next question
     */
    fun loadNextQuestion() {
        viewModelScope.launch {
            // Guard: don't load if we've already reached maxQuestions
            val current = _quizState.value
            if (current.totalQuestions >= current.maxQuestions) {
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            _quizState.value = _quizState.value.copy(isLoading = true, error = null)

            try {
                // Fetch next question from DB using repository
                // Over-fetch to have a pool to filter session-asked questions
                val limit = 1
                val minDiff = 1
                val maxDiff = 10
                val fetchLimit = maxOf(limit, 10)
                val candidates = quizQuestionRepository.getNextQuestions(minDiff, maxDiff, fetchLimit)
                
                // Filter out questions already asked in this quiz session
                val available = candidates.filter { it.id !in sessionAskedIds }
                
                if (available.isNotEmpty()) {
                    val q = available.first()
                    sessionAskedIds.add(q.id)
                    val options = listOf(q.optionA, q.optionB, q.optionC, q.optionD).filter { it.isNotBlank() }
                    val correctAnswerIndex = when (q.correctAnswer.trim().uppercase()) {
                        "A" -> 0
                        "B" -> 1
                        "C" -> 2
                        "D" -> 3
                        else -> options.indexOf(q.correctAnswer).takeIf { it >= 0 } ?: 0
                    }
                    val verseObj = GitaVerse(chapterNo = q.chapter, verseNo = q.verse)
                    val currentQuizQuestion = com.aipoweredgita.app.data.QuizQuestion(
                        verse = verseObj,
                        question = q.question,
                        options = options,
                        correctAnswerIndex = correctAnswerIndex,
                        explanation = q.explanation
                    )
                    
                    // Mark question as asked so it won't be returned again soon
                    quizQuestionRepository.markAsAsked(q.id)
                    
                    val newTotal = _quizState.value.totalQuestions + 1
                    _uiState.update { it.copy(isLoading = false, error = null, selectedAnswer = null, isAnswerRevealed = false) }
                    _quizState.value = _quizState.value.copy(
                        isLoading = false,
                        error = null,
                        currentQuestion = currentQuizQuestion,
                        selectedAnswerIndex = null,
                        showAnswer = false,
                        showCorrectAnswer = false,
                        totalQuestions = newTotal
                    )
                    startTimer()
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No questions available in the question bank.") }
                    _quizState.value = _quizState.value.copy(isLoading = false, error = "No questions available in the question bank.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                _quizState.value = _quizState.value.copy(isLoading = false, error = e.message)
                _sideEffect.emit(QuizSideEffect.ShowError(e.message ?: "Failed to load question"))
            }
        }
    }

    /**
     * Select an answer
     */
    private fun selectAnswer(answer: String) {
        stopTimer()
        _uiState.update { it.copy(selectedAnswer = answer) }
        _quizState.value = _quizState.value.copy(selectedAnswerIndex = _quizState.value.currentQuestion?.options?.indexOf(answer))
    }

    /**
     * Confirm answer with known correctness (used by MCQ flow)
     */
    fun confirmAnswerResult(isCorrect: Boolean) {
        stopTimer()
        _uiState.update {
            it.copy(
                isAnswerRevealed = true,
                score = if (isCorrect) it.score + 1 else it.score
            )
        }

        _quizState.value = _quizState.value.copy(
            showAnswer = true,
            showCorrectAnswer = isCorrect,
            score = if (isCorrect) _quizState.value.score + 1 else _quizState.value.score
        )

        difficultyEngine.updateDifficulty(userState, isCorrect, 0L)
        saveProgress()
    }

    /**
     * Confirm the selected answer
     */
    private fun confirmAnswer() {
        val currentQuestion = _quizState.value.currentQuestion ?: return
        val selectedAnswer = _uiState.value.selectedAnswer ?: return

        val isCorrect = selectedAnswer == currentQuestion.options?.getOrNull(currentQuestion.correctAnswerIndex)

        _uiState.update { 
            it.copy(
                isAnswerRevealed = true,
                score = if (isCorrect) it.score + 1 else it.score
            ) 
        }

        _quizState.value = _quizState.value.copy(
            showAnswer = true,
            showCorrectAnswer = isCorrect,
            score = if (isCorrect) _quizState.value.score + 1 else _quizState.value.score
        )

        // Update difficulty
        difficultyEngine.updateDifficulty(userState, isCorrect, 0L)
        saveProgress()
    }

    /**
     * Finish the quiz
     */
    private fun finishQuiz() {
        stopTimer()
        viewModelScope.launch {
            val currentState = _quizState.value
            val timeSpentSeconds = if (quizStartTime > 0) {
                (System.currentTimeMillis() - quizStartTime) / 1000
            } else 0L

            val quizAttempt = QuizAttempt(
                score = currentState.score,
                totalQuestions = currentState.totalQuestions,
                timeSpentSeconds = timeSpentSeconds
            )

            try {
                val result = quizRepository.saveQuizAttemptWithStats(
                    attempt = quizAttempt,
                    score = currentState.score,
                    totalQuestions = currentState.totalQuestions
                )

                _uiState.update { it.copy(isQuizCompleted = true, coinsEarned = result.third) }
                _quizState.value = _quizState.value.copy(
                    isQuizComplete = true,
                    coinsEarned = result.third,
                    totalTimeSeconds = timeSpentSeconds
                )

                _sideEffect.emit(QuizSideEffect.QuizCompleted(
                    score = currentState.score,
                    total = currentState.totalQuestions,
                    coinsEarned = result.third
                ))

                if (result.first && result.second != null) {
                    val level = result.second ?: return@launch
                    com.aipoweredgita.app.notifications.YogaLevelUpNotificationManager.showLevelUpNotification(
                        application,
                        level
                    )
                }

                quizPreferences.clearQuizState()
            } catch (e: Exception) {
                _sideEffect.emit(QuizSideEffect.ShowError(e.message ?: "Failed to save quiz results"))
            }
        }
    }

    /**
     * Restart the quiz
     */
    fun restartQuiz() {
        val currentMaxQuestions = _quizState.value.maxQuestions
        val currentLanguage = _quizState.value.language
        _quizState.value = QuizState(maxQuestions = currentMaxQuestions, language = currentLanguage, isLoading = true)
        _uiState.value = QuizUiState(isLoading = true)
        quizStartTime = System.currentTimeMillis()
        sessionAskedIds.clear()

        viewModelScope.launch {
            quizPreferences.clearQuizState()
            loadNextQuestion()
        }
    }

    // Backward compatibility methods for existing UI code
    fun setQuizLimit(maxQuestions: Int) {
        val currentLanguage = _quizState.value.language
        _quizState.value = com.aipoweredgita.app.data.QuizState(maxQuestions = maxQuestions, language = currentLanguage)
        _uiState.value = QuizUiState()
        quizStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            quizPreferences.clearQuizState()
        }
    }

    fun setQuizLanguage(quizLanguage: String) {
        _quizState.value = _quizState.value.copy(language = quizLanguage)
    }

    fun submitOpenEndedAnswer(text: String) {
        val currentQuestion = _quizState.value.currentQuestion ?: return
        _quizState.value = _quizState.value.copy(openEndedAnswer = text)
        val matched = currentQuestion.rubricKeywords.count { kw -> text.lowercase().contains(kw.lowercase()) }
        val passThreshold = maxOf(1, currentQuestion.rubricKeywords.size / 2)
        val isPass = matched >= passThreshold
        _quizState.value = _quizState.value.copy(showAnswer = true, showCorrectAnswer = isPass)
        if (isPass) {
            _quizState.value = _quizState.value.copy(score = _quizState.value.score + 1)
        }
    }

    fun selectAnswer(index: Int) {
        _quizState.value = _quizState.value.copy(selectedAnswerIndex = index)
    }

    fun revealAnswer() {
        _quizState.value = _quizState.value.copy(showCorrectAnswer = true)
    }

    fun loadNextQuestionLegacy() {
        loadNextQuestion()
    }

    fun resetQuiz() {
        val currentMaxQuestions = _quizState.value.maxQuestions
        val currentLanguage = _quizState.value.language
        _quizState.value = QuizState(maxQuestions = currentMaxQuestions, language = currentLanguage, isLoading = true)
        _uiState.value = QuizUiState(isLoading = true)
        quizStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            quizPreferences.clearQuizState()
            loadNextQuestion()
        }
    }

    fun exitQuiz() {
        val currentState = _quizState.value
        if (currentState.score > 0 && currentState.totalQuestions > 0) {
            viewModelScope.launch {
                try {
                    val timeSpentSeconds = if (quizStartTime > 0) {
                        (System.currentTimeMillis() - quizStartTime) / 1000
                    } else 0L
                    val quizAttempt = QuizAttempt(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions,
                        timeSpentSeconds = timeSpentSeconds
                    )
                    quizRepository.saveQuizAttemptWithStats(
                        attempt = quizAttempt,
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions
                    )
                } catch (e: Exception) {
                    _sideEffect.emit(QuizSideEffect.ShowError(e.message ?: "Failed to save quiz"))
                }
            }
        }
        viewModelScope.launch { quizPreferences.clearQuizState() }
    }

    private fun saveProgress() {
        val state = _quizState.value
        viewModelScope.launch {
            try {
                val usedQuestions = sessionAskedIds.map { it.toString() }.toSet()
                quizPreferences.saveQuizState(
                    score = state.score,
                    totalQuestions = state.totalQuestions,
                    maxQuestions = state.maxQuestions,
                    startTime = quizStartTime,
                    usedQuestions = usedQuestions
                )
            } catch (_: Exception) { }
        }
    }

    fun setLanguage(lang: String) {
        setQuizLanguage(lang)
    }

    // Backward compatibility - events flow for existing UI
    private val _events = MutableSharedFlow<String>()
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun setError(message: String) {
        _quizState.value = _quizState.value.copy(isLoading = false, error = message)
    }

    fun recordQuestionFeedback(questionId: Int, rating: Float, wasSkipped: Boolean = false, changedAnswer: Boolean = false) {
        // Stub for backward compatibility
    }

    override fun onCleared() {
        super.onCleared()
        mlManager.close()
    }

    private fun startTimer() {
        timerJob?.cancel()
        // Give more time for open-ended questions
        val question = _quizState.value.currentQuestion
        val isOpenEnded = question?.type == com.aipoweredgita.app.data.QuestionType.ESSAY ||
                question?.type == com.aipoweredgita.app.data.QuestionType.APPLICATION
        val timeLimit = if (isOpenEnded) 60 else 30
        _quizState.value = _quizState.value.copy(questionTimeLeftSeconds = timeLimit, isTimerRunning = true)
        timerJob = viewModelScope.launch {
            while (_quizState.value.questionTimeLeftSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                synchronized(_quizState) {
                    val current = _quizState.value.questionTimeLeftSeconds
                    if (current > 0) {
                        _quizState.value = _quizState.value.copy(questionTimeLeftSeconds = current - 1)
                    }
                }
            }
            // Time ran out — mark answer as revealed (wrong since no selection)
            stopTimer()
            synchronized(_quizState) {
                val state = _quizState.value
                if (!state.showAnswer) {
                    _uiState.update { it.copy(isAnswerRevealed = true) }
                    _quizState.value = state.copy(showAnswer = true, showCorrectAnswer = false)
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _quizState.value = _quizState.value.copy(isTimerRunning = false)
    }
}
