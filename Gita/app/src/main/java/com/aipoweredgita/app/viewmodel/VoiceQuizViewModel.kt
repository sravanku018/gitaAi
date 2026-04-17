package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Stable
import com.aipoweredgita.app.ml.AppFeature
import com.aipoweredgita.app.ml.LiteRtLmVoiceChatEngine
import com.aipoweredgita.app.ml.ModelAvailability
import com.aipoweredgita.app.utils.AiTurnManager
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.CachedVerse
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

enum class QuizDifficulty { EASY, MEDIUM, HARD }

@Stable
data class VoiceQuizState(
    val hasStarted: Boolean = false,
    val currentQuestion: String? = null,
    val currentVerse: CachedVerse? = null,
    val transcript: String = "",
    val partialTranscript: String = "",
    val score: Int = 0,
    val questionIndex: Int = 0,
    val totalQuestions: Int = 5,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isLoading: Boolean = false,
    val isGameOver: Boolean = false,
    val feedback: String = "",
    val isLlmReady: Boolean = false,
    val difficulty: QuizDifficulty = QuizDifficulty.EASY
)

class VoiceQuizViewModel(application: Application) : AndroidViewModel(application) {

    private val TAG = "VoiceQuizViewModel"

    private val _state = MutableStateFlow(VoiceQuizState())
    val state: StateFlow<VoiceQuizState> = _state.asStateFlow()

    private val voiceManager = VoiceManager(application)
    private val gemmaEngine = LiteRtLmVoiceChatEngine(application)
    private val database = GitaDatabase.getDatabase(application)
    private val cachedVerseDao = database.cachedVerseDao()
    private val questionBankDao = database.quizQuestionBankDao()

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val aiScope = CoroutineScope(aiDispatcher + SupervisorJob())

    private val llmInitMutex = Mutex()
    private var isLlmInitialized = false
    private var currentLanguageMode: com.aipoweredgita.app.utils.LanguageMode = com.aipoweredgita.app.utils.LanguageMode.ENG_TO_TEL

    private fun fixSpacing(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace("\n", " ")
            .replace("\\s+".toRegex(), " ")
            .replace(" ,", ",")
            .replace(" .", ".")
            .replace(" ?", "?")
            .replace(" !", "!")
            .trim()
    }

    init {
        observeModelChanges()
    }

    private fun observeModelChanges() {
        viewModelScope.launch {
            ModelAvailability.getInstance(getApplication()).selectedModel.collect { modelName ->
                Log.d(TAG, "Model preference changed to $modelName, re-initializing Gemma engine...")
                refreshModelStatus()
            }
        }
    }

    private fun refreshModelStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            llmInitMutex.withLock {
                val modelPath = com.aipoweredgita.app.ml.ModelAvailability.getInstance(getApplication()).getResolvedModelPath(AppFeature.VOICE)
                if (modelPath != null) {
                    try {
                        val success = gemmaEngine.initialize(modelPath)
                        if (success) {
                            val instruction = if (currentLanguageMode == com.aipoweredgita.app.utils.LanguageMode.TEL_TO_TEL || 
                                currentLanguageMode == com.aipoweredgita.app.utils.LanguageMode.ENG_TO_TEL)
                                "మీరు కృష్ణుడు. భగవద్గీత జ్ఞానం ఆధారంగా విద్యార్థి సమాధానాలను మూల్యాంకనం చేయండి. " +
                                "సరైనదైతే ప్రోత్సహించండి, తప్పైతే సున్నితంగా సరిదిద్దండి. " +
                                "సమాధానం ఎప్పుడూ 'సరియైనది' లేదా 'తప్పు' అనే పదంతో ప్రారంభం కావాలి. తెలుగులో మాత్రమే సమాధానం ఇవ్వండి."
                            else
                                "You are Krishna. Evaluate the student's answer based on Bhagavad Gita wisdom. " +
                                "Encourage if correct, gently correct if wrong. " +
                                "The response MUST start with the word 'CORRECT' or 'WRONG'. Reply in English."
                            
                            gemmaEngine.updateSystemInstruction(instruction)
                        }
                        isLlmInitialized = success
                        withContext(Dispatchers.Main) {
                            _state.value = _state.value.copy(isLlmReady = success)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gemma initialization failed", e)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(isLlmReady = false)
                    }
                }
            }
        }
    }

    fun startQuiz() {
        _state.value = VoiceQuizState(hasStarted = true, isLlmReady = isLlmInitialized)
        viewModelScope.launch {
            startQuizInternal()
        }
    }

    private suspend fun startQuizInternal() {
        _state.value = _state.value.copy(isLoading = true)
        loadNextQuestion()
    }

    private suspend fun loadNextQuestion() {
        if (_state.value.questionIndex >= _state.value.totalQuestions) {
            _state.value = _state.value.copy(isGameOver = true, isLoading = false)
            return
        }

        gemmaEngine.stopResponse()
        gemmaEngine.resetConversation()

        _state.value = _state.value.copy(
            isLoading = true,
            transcript = "",
            partialTranscript = "",
            feedback = "",
            isListening = false,
            isSpeaking = false
        )

        aiScope.launch {
            stopAll()
            try {
                val candidates = withContext(Dispatchers.IO) {
                    questionBankDao.getNextQuestions(
                        minDiff = 1,
                        maxDiff = 10,
                        limit = 5,
                        targetDifficulty = 5,
                        cooldownCutoff = System.currentTimeMillis() - 3600_000 // 1 hour cooldown
                    )
                }

                if (candidates.isEmpty()) throw Exception("No questions available")

                var selectedQuestion = candidates.first()

                if (isLlmInitialized) {
                    val selectionPrompt = """
                        As Krishna, select the most profound and spiritually significant question from this list to ask a seeker. 
                        Candidates:
                        ${candidates.mapIndexed { i, q -> "($i) ${q.question}" }.joinToString("\n")}
                        
                        Instruction: Return ONLY the index number (e.g., 0, 1, 2...) of your choice. Do not provide any other text.
                    """.trimIndent()

                    val output = gemmaEngine.sendMessage(prompt = selectionPrompt)
                    val index = output.trim().filter { it.isDigit() }.toIntOrNull()
                    if (index != null && index in candidates.indices) {
                        selectedQuestion = candidates[index]
                    }
                }

                val questionText = fixSpacing(selectedQuestion.question)

                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        currentQuestion = questionText,
                        isLoading = false,
                        isSpeaking = true
                    )
                    voiceManager.speak(questionText) {
                        _state.value = _state.value.copy(isSpeaking = false)
                    }
                }

                withContext(Dispatchers.IO) { 
                    questionBankDao.markAsAsked(selectedQuestion.id)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading question", e)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isLoading = false, feedback = "ప్రశ్నను లోడ్ చేయడంలో లోపం.")
                }
            }
        }
    }

    fun startListening() {
        val s = _state.value
        if (s.isSpeaking || s.isListening || s.isLoading || s.isGameOver) return

        _state.value = _state.value.copy(isListening = true, transcript = "", partialTranscript = "", feedback = "")

        voiceManager.startListening(
            onResult = { result ->
                _state.value = _state.value.copy(transcript = result, isListening = false)
                evaluateAnswer(result)
            },
            onError = { _state.value = _state.value.copy(isListening = false, feedback = "మీ మాట వినబడలేదు.") },
            onPartialResult = { partial -> _state.value = _state.value.copy(partialTranscript = partial) }
        )
    }

    fun stopListening() {
        voiceManager.stopListening()
        _state.value = _state.value.copy(isListening = false)
    }

    private fun evaluateAnswer(answer: String) {
        aiScope.launch {
            stopAll()
            withContext(Dispatchers.Main) {
                _state.value = _state.value.copy(isLoading = true, feedback = "")
            }

            if (!isLlmInitialized) {
                withContext(Dispatchers.Main) {
                    finalizeEvaluation("ధన్యవాదాలు! మీ సమాధానం బాగుంది. 🙏")
                }
                return@launch
            }

            val evaluationPrompt = """
                Question: ${_state.value.currentQuestion}
                Student Answer: ${answer.take(300)}
                Evaluate the student's answer as Krishna. Answer in Telugu.
            """.trimIndent()

            gemmaEngine.sendMessage(
                prompt = evaluationPrompt,
                onPartial = { partial ->
                    if (partial.isNotBlank()) {
                        viewModelScope.launch(Dispatchers.Main) {
                            _state.value = _state.value.copy(feedback = partial)
                        }
                    }
                },
                onCleaned = { deepCleaned ->
                    viewModelScope.launch(Dispatchers.Main) {
                        finalizeEvaluation(deepCleaned)
                    }
                }
            )
        }
    }

    fun stopAll() {
        gemmaEngine.stopResponse()
        voiceManager.stopListening()
        voiceManager.stopSpeaking()
    }

    private fun finalizeEvaluation(feedback: String) {
        val trimmedFeedback = fixSpacing(feedback.trim())
        val isCorrect = trimmedFeedback.startsWith("CORRECT", ignoreCase = true) ||
                        trimmedFeedback.startsWith("సరియైనది", ignoreCase = true) ||
                        trimmedFeedback.startsWith("సరైనది", ignoreCase = true) ||
                        trimmedFeedback.contains("excellent", ignoreCase = true) ||
                        trimmedFeedback.contains("శభాష్", ignoreCase = true)

        _state.value = _state.value.copy(
            score = _state.value.score + if (isCorrect) 1 else 0,
            feedback = feedback,
            isLoading = false,
            isSpeaking = true
        )

        voiceManager.speak(feedback) {
            viewModelScope.launch {
                _state.value = _state.value.copy(isSpeaking = false, questionIndex = _state.value.questionIndex + 1)
                delay(600)
                loadNextQuestion()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        aiScope.cancel()
        gemmaEngine.close()
        voiceManager.destroy()
    }

    fun setLanguageMode(mode: com.aipoweredgita.app.utils.LanguageMode) {
        currentLanguageMode = mode
        voiceManager.setLocale(mode.sttLocale, mode.ttsLocale)
    }
}
