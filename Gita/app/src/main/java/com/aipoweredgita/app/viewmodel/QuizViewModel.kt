package com.aipoweredgita.app.viewmodel

import android.app.Application
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
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
import com.aipoweredgita.app.ml.ELOEntity
import com.aipoweredgita.app.ml.EntityType
import com.aipoweredgita.app.ml.EloRatingSystem
import com.aipoweredgita.app.ml.HuggingFaceMLManager
import com.aipoweredgita.app.ml.ItemParameters
import com.aipoweredgita.app.ml.ItemResponseTheoryEngine
import com.aipoweredgita.app.ml.StudentAbility
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
    private val userPreferencesDao: com.aipoweredgita.app.database.UserPreferencesDao,
    private val application: Application
) : ViewModel() {

    // Single UI state
    private val _uiState = MutableStateFlow(QuizUiState())
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    // One-time side effects
    private val _sideEffect = kotlinx.coroutines.channels.Channel<QuizSideEffect>()
    val sideEffect: kotlinx.coroutines.flow.Flow<QuizSideEffect> = _sideEffect.receiveAsFlow()

    // Legacy quiz state for compatibility
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val difficultyEngine = AdaptiveDifficultyEngine()
    private var userState = AdaptiveDifficultyEngine.UserState()
    private val eloSystem = EloRatingSystem()
    private val irtEngine = ItemResponseTheoryEngine()
    private var studentEntity = ELOEntity(id = "student", type = EntityType.STUDENT)
    private var studentAbility = StudentAbility(studentId = "student")
    private var quizStartTime: Long = 0
    private var timerJob: kotlinx.coroutines.Job? = null
    // Track question IDs asked in the current quiz session to prevent repeats
    private val sessionAskedIds = mutableSetOf<Int>()

    init {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val prefs = application.getSharedPreferences("quiz_prefs", android.content.Context.MODE_PRIVATE)
            userState = AdaptiveDifficultyEngine.loadState(prefs)
            studentEntity = ELOEntity(id = "student", rating = 1500.0 + (userState.skillLevel - 5) * 50.0, type = EntityType.STUDENT)
            studentAbility = StudentAbility(studentId = "student", theta = (userState.skillLevel - 5) * 0.5)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Handle events from the UI
     */
    fun checkAndDownloadQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val db = GitaDatabase.getDatabase(application)
                val dao = db.quizQuestionBankDao()
                val importer = com.aipoweredgita.app.ml.BhagavadGitaQAImporter(application, dao)

                // Check per-language counts so both are imported correctly
                val enCount = dao.getQuestionsByLanguage("english")
                val teCount = dao.getQuestionsByLanguage("telugu")

                if (enCount < 100) {
                    android.util.Log.d("QuizViewModel", "Importing English questions (current: $enCount)...")
                    val imported = importer.importDataset(language = "english", batchSize = 500)
                    android.util.Log.d("QuizViewModel", "English import done: $imported questions")
                }
                if (teCount < 100) {
                    android.util.Log.d("QuizViewModel", "Importing Telugu questions (current: $teCount)...")
                    val imported = importer.importDataset(language = "telugu", batchSize = 500)
                    android.util.Log.d("QuizViewModel", "Telugu import done: $imported questions")
                }
            } catch (e: Exception) {
                android.util.Log.w("QuizViewModel", "Auto-download failed: ${e.message}")
            }
        }
    }

    fun onEvent(event: QuizEvent) {
        when (event) {
            is QuizEvent.SelectAnswer -> selectAnswer(event.answer)
            is QuizEvent.ConfirmAnswer -> confirmAnswer()
            is QuizEvent.NextQuestion -> loadNextQuestion()
            is QuizEvent.FinishQuiz -> finishQuiz()
            is QuizEvent.RestartQuiz -> restartQuiz()
            is QuizEvent.SetQuizConfig -> setQuizConfig(event.questionCount, event.language)
            is QuizEvent.ProceedToNextOrFinish -> proceedToNextOrFinish(event.wasCorrect)
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
            // Guard: don't load if we've already reached maxQuestions or are already loading
            var shouldLoad = false
            _quizState.update { current ->
                if (current.totalQuestions >= current.maxQuestions || current.isLoading) {
                    current
                } else {
                    shouldLoad = true
                    current.copy(isLoading = true, error = null)
                }
            }
            if (!shouldLoad) return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Fetch next question from DB using repository
                // Over-fetch to have a pool to filter session-asked questions
                val limit = 1
                
                // Fetch user preferences for difficulty range if enabled
                val userPrefs = userPreferencesDao.getPreferencesSync(1) // Assuming default user id 1
                
                val minDiff: Int
                val maxDiff: Int
                
                if (userPrefs != null && userPrefs.enableDifficultyAdaptation == false) {
                    // Use exact preferred range
                    minDiff = userPrefs.preferredDifficultyMin.coerceAtLeast(1)
                    maxDiff = maxOf(minDiff, userPrefs.preferredDifficultyMax.coerceAtMost(10))
                } else {
                    // Adaptive difficulty based on skill level, but clamped to user preferred bounds if present
                    val diff = userState.skillLevel
                    val prefMin = userPrefs?.preferredDifficultyMin?.coerceAtLeast(1) ?: 1
                    val rawPrefMax = userPrefs?.preferredDifficultyMax?.coerceAtMost(10) ?: 10
                    val prefMax = maxOf(prefMin, rawPrefMax)
                    
                    minDiff = (diff - 2).coerceIn(prefMin, prefMax)
                    maxDiff = maxOf(minDiff, (diff + 2).coerceIn(prefMin, prefMax))
                }
                
                val fetchLimit = maxOf(limit, 10)
                // Map short language codes to modelVersion tags used during import
                val currentLanguage = when (_quizState.value.language.lowercase().trim()) {
                    "tel", "telugu", "te" -> "telugu"
                    else -> "english"
                }

                android.util.Log.d("QuizViewModel", "=== NORMAL/TELUGU QUIZ LOAD ===")
                android.util.Log.d("QuizViewModel", "Raw language: '${_quizState.value.language}', Mapped language: '$currentLanguage'")
                android.util.Log.d("QuizViewModel", "Difficulty range: minDiff=$minDiff, maxDiff=$maxDiff, limit=$fetchLimit")

                var candidates = quizQuestionRepository.getNextQuestionsForLanguage(
                    minDiff, maxDiff, fetchLimit, currentLanguage
                )
                android.util.Log.d("QuizViewModel", "Initial candidates for '$currentLanguage': count=${candidates.size}")
                
                // Filter out questions already asked in this quiz session
                var available = candidates.filter { it.id !in sessionAskedIds }
                android.util.Log.d("QuizViewModel", "Available candidates after session filter: count=${available.size}")

                if (available.isEmpty()) {
                    android.util.Log.d("QuizViewModel", "Fallback 1: Loading getFallbackQuestions for '$currentLanguage'...")
                    candidates = quizQuestionRepository.getFallbackQuestions(fetchLimit, currentLanguage)
                    available = candidates.filter { it.id !in sessionAskedIds }
                    android.util.Log.d("QuizViewModel", "Fallback 1 candidates for '$currentLanguage': count=${candidates.size}, available=${available.size}")
                    if (available.isEmpty() && candidates.isNotEmpty()) {
                        sessionAskedIds.clear()
                        available = candidates
                    }
                }

                if (available.isEmpty()) {
                    android.util.Log.d("QuizViewModel", "Fallback 2: Synchronous dataset import for '$currentLanguage'...")
                    try {
                        val db = GitaDatabase.getDatabase(application)
                        val dao = db.quizQuestionBankDao()
                        val importer = com.aipoweredgita.app.ml.BhagavadGitaQAImporter(application, dao)
                        val impTe = importer.importDataset(language = "telugu", batchSize = 500)
                        val impEn = importer.importDataset(language = "english", batchSize = 500)
                        android.util.Log.d("QuizViewModel", "Fallback 2 import results: telugu=$impTe, english=$impEn")
                        candidates = quizQuestionRepository.getFallbackQuestions(fetchLimit, currentLanguage)
                        available = candidates
                    } catch (e: Exception) {
                        android.util.Log.w("QuizViewModel", "Fallback 2 import failed: ${e.message}")
                    }
                }

                if (available.isEmpty()) {
                    android.util.Log.d("QuizViewModel", "Fallback 3: Getting ANY active question regardless of language...")
                    candidates = quizQuestionRepository.getFallbackQuestions(fetchLimit, "")
                    available = candidates
                    android.util.Log.d("QuizViewModel", "Fallback 3 candidates: count=${candidates.size}")
                }
                
                if (available.isNotEmpty()) {
                    val q = available.first()
                    sessionAskedIds.add(q.id)
                    android.util.Log.d("QuizViewModel", "SELECTED QUESTION: id=${q.id}, lang=${q.language}, text=${q.question.take(50)}")
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
                _sideEffect.send(QuizSideEffect.ShowError(e.message ?: "Failed to load question"))
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
        if (_uiState.value.isAnswerRevealed) return
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

        updateMLEngines(isCorrect)
        saveProgress()
    }

    private fun proceedToNextOrFinish(wasCorrect: Boolean) {
        confirmAnswerResult(wasCorrect)
        val state = _quizState.value
        if (!state.isQuizComplete) {
            if (state.totalQuestions >= state.maxQuestions) {
                finishQuiz()
            } else {
                loadNextQuestion()
            }
        }
    }

    /**
     * Confirm the selected answer
     */
    private fun confirmAnswer() {
        val currentQuestion = _quizState.value.currentQuestion ?: return
        val selectedAnswer = _uiState.value.selectedAnswer ?: return

        val isCorrect = selectedAnswer == currentQuestion.options?.getOrNull(currentQuestion.correctAnswerIndex)

        // Delegate to the single source of truth for answer confirmation
        confirmAnswerResult(isCorrect)
    }

    /**
     * Single point of update for all ML engines (difficulty, ELO, IRT).
     * Prevents double-updating if both confirmAnswer flows trigger.
     */
    private fun updateMLEngines(isCorrect: Boolean) {
        difficultyEngine.updateDifficulty(userState, isCorrect, 0L)
        val questionEntity = ELOEntity(id = "q_${_quizState.value.totalQuestions}", type = EntityType.QUESTION)
        eloSystem.updateRatings(studentEntity, questionEntity, if (isCorrect) 1.0 else 0.0)
        val itemParams = ItemParameters(difficulty = userState.skillLevel.toDouble(), discrimination = 1.0)
        studentAbility = irtEngine.updateAbility(studentAbility, listOf(itemParams to isCorrect))
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
                timeSpentSeconds = timeSpentSeconds,
                quizType = currentState.quizType,
                language = currentState.language
            )

            try {
                val result = quizRepository.saveQuizAttemptWithStats(
                    attempt = quizAttempt,
                    score = currentState.score,
                    totalQuestions = currentState.totalQuestions,
                    quizType = currentState.quizType
                )

                // End card shows coins from CoinRewardEngine which now exactly matches server formula
                _uiState.update { it.copy(isQuizCompleted = true, coinsEarned = result.coinsEarned) }
                _quizState.value = _quizState.value.copy(
                    isQuizComplete = true,
                    coinsEarned = result.coinsEarned,
                    coinBreakdown = result.breakdown,
                    totalTimeSeconds = timeSpentSeconds
                )

                _sideEffect.send(QuizSideEffect.QuizCompleted(
                    score = currentState.score,
                    total = currentState.totalQuestions,
                    coinsEarned = result.coinsEarned
                ))

                if (result.didLevelUp && result.newLevel != null) {
                    val level = result.newLevel ?: return@launch
                    com.aipoweredgita.app.notifications.YogaLevelUpNotificationManager.showLevelUpNotification(
                        application,
                        level
                    )
                }

                quizPreferences.clearQuizState()
            } catch (e: Exception) {
                _sideEffect.send(QuizSideEffect.ShowError(e.message ?: "Failed to save quiz results"))
            }
        }
    }

    /**
     * Restart the quiz
     */
    fun restartQuiz() {
        val currentMaxQuestions = _quizState.value.maxQuestions
        val currentLanguage = _quizState.value.language
        _quizState.value = QuizState(maxQuestions = currentMaxQuestions, language = currentLanguage, isLoading = false)
        _uiState.value = QuizUiState(isLoading = false)
        quizStartTime = System.currentTimeMillis()
        sessionAskedIds.clear()

        viewModelScope.launch {
            quizPreferences.clearQuizState()
            loadNextQuestion()
        }
    }

    // Backward compatibility methods for existing UI code
    fun setQuizLimit(maxQuestions: Int) {
        sessionAskedIds.clear()
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
        _quizState.update { it.copy(openEndedAnswer = text) }
        val matched = currentQuestion.rubricKeywords.count { kw -> text.lowercase().contains(kw.lowercase()) }
        val passThreshold = if (currentQuestion.rubricKeywords.isEmpty()) 0 else maxOf(1, currentQuestion.rubricKeywords.size / 2)
        val isPass = text.isNotBlank() && matched >= passThreshold
        // We only set the local states for the UI dialog. 
        // confirmAnswerResult() will be called when user dismisses the dialog and proceeds.
        _quizState.update { it.copy(showAnswer = true, showCorrectAnswer = isPass) }
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
        _quizState.value = QuizState(maxQuestions = currentMaxQuestions, language = currentLanguage, isLoading = false)
        _uiState.value = QuizUiState(isLoading = false)
        quizStartTime = System.currentTimeMillis()
        viewModelScope.launch {
            quizPreferences.clearQuizState()
        }
    }

    fun startQuiz() {
        if (_quizState.value.currentQuestion == null) {
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
                        timeSpentSeconds = timeSpentSeconds,
                        quizType = currentState.quizType,
                        language = currentState.language
                    )
                    quizRepository.saveQuizAttemptWithStats(
                        attempt = quizAttempt,
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions,
                        quizType = currentState.quizType
                    )
                } catch (e: Exception) {
                    _sideEffect.send(QuizSideEffect.ShowError(e.message ?: "Failed to save quiz"))
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
        val prefs = application.getSharedPreferences("quiz_prefs", android.content.Context.MODE_PRIVATE)
        AdaptiveDifficultyEngine.saveState(userState, prefs)
    }

    private fun startTimer() {
        timerJob?.cancel()
        // Give more time for open-ended questions
        val question = _quizState.value.currentQuestion
        val isOpenEnded = question?.type == com.aipoweredgita.app.data.QuestionType.ESSAY ||
                question?.type == com.aipoweredgita.app.data.QuestionType.APPLICATION
        val timeLimit = if (isOpenEnded) 60 else 30
        _quizState.update { it.copy(questionTimeLeftSeconds = timeLimit, isTimerRunning = true) }
        timerJob = viewModelScope.launch {
            while (isActive && _quizState.value.questionTimeLeftSeconds > 0) {
                kotlinx.coroutines.delay(1000)
                if (isActive) {
                    _quizState.update { 
                        val newTime = it.questionTimeLeftSeconds - 1
                        it.copy(
                            questionTimeLeftSeconds = maxOf(0, newTime),
                            isTimerRunning = newTime > 0
                        )
                    }
                }
            }
            // Trigger timeout if they hit 0 and it wasn't cancelled
            if (isActive && !_quizState.value.showAnswer && _uiState.value.selectedAnswer == null) {
                _sideEffect.send(QuizSideEffect.ShowError("Time's up!"))
                confirmAnswerResult(false)
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        _quizState.value = _quizState.value.copy(isTimerRunning = false)
    }
}
