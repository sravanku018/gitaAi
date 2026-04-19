package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.VoiceChatMessage
import com.aipoweredgita.app.ml.AppFeature
import com.aipoweredgita.app.ml.LiteRtLmVoiceChatEngine
import com.aipoweredgita.app.ml.ModelAvailability
import com.aipoweredgita.app.ml.ModelDownloadStateManager
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.utils.AiTurnManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import com.aipoweredgita.app.repository.StatsRepository
import java.io.File
import java.util.UUID

@Immutable
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Stable
data class VoiceChatState(
    val messages: List<ChatMessage> = emptyList(),
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val isThinking: Boolean = false,
    val liveTranscript: String = "",
    val userInput: String = "",
    val isLlmReady: Boolean = false,
    val error: String? = null,
    val errorType: VoiceChatErrorType? = null
)

enum class VoiceChatErrorType {
    MODEL_INIT,
    LLM_INFERENCE,
    STT,
    TTS,
    NETWORK,
    CRASH_RECOVERY
}

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "VoiceChatViewModel"

    private val _state = MutableStateFlow(VoiceChatState())
    val state: StateFlow<VoiceChatState> = _state.asStateFlow()

    private val voiceManager = VoiceManager(application)
    private val voiceChatEngine = LiteRtLmVoiceChatEngine(application)
    private val database = GitaDatabase.getDatabase(application)
    private val chatMessageDao = database.voiceChatMessageDao()

    private val statsRepository = StatsRepository(database.userStatsDao())

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val aiScope = CoroutineScope(aiDispatcher + SupervisorJob())
    private val initMutex = Mutex()

    private var startTime: Long = 0

    /** Crash recovery counter — prevents infinite crash loops */
    private var crashCount = 0
    private val MAX_CRASHES = 3
    private var lastCrashTime = 0L

    init {
        setupVoiceManagerErrorForwarding()
        loadMessages()
        observeModelChanges()
        refreshModelStatus()
    }

    private fun setupVoiceManagerErrorForwarding() {
        voiceManager.onError = { errorMsg ->
            viewModelScope.launch(Dispatchers.Main) {
                _state.update { it.copy(error = errorMsg, errorType = VoiceChatErrorType.TTS) }
            }
        }
    }

    // ─── Text Spacing Fix ──────────────────────────────────────────────────────
    private fun fixSpacing(text: String): String {
        return com.aipoweredgita.app.util.TextUtils.cleanLlmOutput(text)
    }

    // ─── Init & Model ──────────────────────────────────────────────────────────

    private fun observeModelChanges() {
        viewModelScope.launch {
            ModelAvailability.getInstance(getApplication()).selectedModel.collect { modelName ->
                Log.d(tag, "Model preference changed to $modelName, re-initializing engine...")
                refreshModelStatus()
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbMessages = chatMessageDao.getAllMessages()
            withContext(Dispatchers.Main) {
                _state.update {
                    it.copy(messages = dbMessages.map { dbm ->
                        ChatMessage(dbm.id, dbm.text, dbm.isUser, dbm.timestamp)
                    })
                }
            }
        }
    }

    fun refreshModelStatus() {
        val context = getApplication<Application>()
        try {
            val ma = ModelAvailability.getInstance(context)
            val modelPath = ma.getResolvedModelPath(AppFeature.VOICE)

            if (modelPath != null) {
                aiScope.launch {
                    initMutex.withLock {
                        try {
                            val success = voiceChatEngine.initialize(modelPath)
                            if (success) {
                                voiceChatEngine.updateSystemInstruction(currentLanguageMode.systemInstruction)
                                crashCount = 0 // Reset on successful init
                            }
                            withContext(Dispatchers.Main) {
                                _state.update {
                                    it.copy(
                                        isLlmReady = success,
                                        error = if (!success) "Failed to initialize AI model" else null,
                                        errorType = if (!success) VoiceChatErrorType.MODEL_INIT else null
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Model init crashed", e)
                            withContext(Dispatchers.Main) {
                                _state.update { it.copy(
                                    isLlmReady = false,
                                    error = "Model initialization failed: ${e.message ?: "Unknown"}",
                                    errorType = VoiceChatErrorType.MODEL_INIT
                                )}
                            }
                        }
                    }
                }
            } else {
                _state.update { it.copy(isLlmReady = false) }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to check model status", e)
            _state.update { it.copy(
                isLlmReady = false,
                error = "Could not check model status",
                errorType = VoiceChatErrorType.MODEL_INIT
            )}
        }
    }

    // ─── Messaging ────────────────────────────────────────────────────────────

    fun updateUserInput(input: String) {
        _state.update { it.copy(userInput = input) }
    }

    private var lastUpdate = 0L

    fun sendMessage(text: String? = null) {
        val messageText = text ?: _state.value.userInput
        if (messageText.isBlank()) return

        // Crash loop protection
        val nowTime = System.currentTimeMillis()
        if (nowTime - lastCrashTime > 60_000) crashCount = 0 // Reset after 60s of stability
        if (crashCount >= MAX_CRASHES) {
            _state.update { it.copy(
                error = "Voice chat crashed too many times. Please restart the app.",
                errorType = VoiceChatErrorType.CRASH_RECOVERY,
                isThinking = false
            )}
            return
        }

        if (text == null) {
            _state.update { it.copy(userInput = "", error = null, errorType = null) }
        }

        val userMessage = ChatMessage(text = messageText, isUser = true)
        _state.update { it.copy(messages = it.messages + userMessage, error = null, errorType = null) }
        saveMessage(userMessage)
        _state.update { it.copy(isThinking = true) }

        aiScope.launch {
            try {
                // Batch setup outside the time-critical locked section
                val aiMessageId = UUID.randomUUID().toString()
                withContext(Dispatchers.Main) {
                    _state.update { s ->
                        s.copy(messages = s.messages + ChatMessage(id = aiMessageId, text = "", isUser = false))
                    }
                }

                AiTurnManager.mutex.withLock {
                    stopAll()
                    
                    // Throttled streaming to prevent Main Thread choking
                    voiceChatEngine.sendMessage(
                        prompt = messageText,
                        onPartial = { partial ->
                            val now = System.currentTimeMillis()
                            if (now - lastUpdate > 64) { // Update ~15fps for smooth UI without choking
                                lastUpdate = now
                                viewModelScope.launch(Dispatchers.Main) {
                                    _state.update { s ->
                                        val updated = s.messages.map { m ->
                                            if (m.id == aiMessageId) m.copy(text = partial) else m
                                        }
                                        s.copy(messages = updated)
                                    }
                                }
                            }
                        },
                        onCleaned = { deepCleaned ->
                            viewModelScope.launch(Dispatchers.Main) {
                                // Batch final state updates
                                _state.update { s ->
                                    val updated = s.messages.map { m ->
                                        if (m.id == aiMessageId) m.copy(text = deepCleaned) else m
                                    }
                                    s.copy(
                                        messages = updated,
                                        isThinking = false
                                    )
                                }

                                saveMessage(ChatMessage(id = aiMessageId, text = deepCleaned, isUser = false))

                                try {
                                    speakResponse(deepCleaned)
                                } catch (e: Exception) {
                                    Log.e(tag, "TTS failed after cleanup", e)
                                    _state.update { it.copy(isSpeaking = false, error = "Voice output failed", errorType = VoiceChatErrorType.TTS) }
                                }
                            }
                        }
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                crashCount++
                lastCrashTime = System.currentTimeMillis()
                Log.e(tag, "Voice chat crash #$crashCount", e)
                withContext(Dispatchers.Main) {
                    _state.update { it.copy(
                        isThinking = false,
                        error = when (e) {
                            is java.io.IOException -> "Network error — check connection"
                            is IllegalStateException -> "Model error — try restarting chat"
                            else -> "Something went wrong: ${e.message ?: "Unknown error"}"
                        },
                        errorType = when (e) {
                            is java.io.IOException -> VoiceChatErrorType.NETWORK
                            is IllegalStateException -> VoiceChatErrorType.LLM_INFERENCE
                            else -> VoiceChatErrorType.CRASH_RECOVERY
                        }
                    )}
                }
            }
        }
    }

    private fun saveMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.insertMessage(
                VoiceChatMessage(
                    id = message.id,
                    text = message.text,
                    isUser = message.isUser,
                    timestamp = message.timestamp
                )
            )
        }
    }

    // ─── Chat Controls ────────────────────────────────────────────────────────

    fun clearChat() {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.deleteAllMessages()
            voiceChatEngine.resetConversation()
            withContext(Dispatchers.Main) {
                _state.update { it.copy(messages = emptyList()) }
            }
        }
    }

    fun startListening() {
        stopAll()
        _state.update { it.copy(isSpeaking = false, isListening = true, liveTranscript = "", error = null, errorType = null) }

        try {
            voiceManager.startListening(
                onResult = { result ->
                    _state.update { it.copy(isListening = false, liveTranscript = "") }
                    if (result.isNotBlank()) sendMessage(result)
                },
                onPartialResult = { partial ->
                    _state.update { it.copy(liveTranscript = partial) }
                },
                onError = { err ->
                    _state.update { it.copy(isListening = false, error = err, errorType = VoiceChatErrorType.STT) }
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start listening", e)
            _state.update { it.copy(isListening = false, error = "Failed to start voice input", errorType = VoiceChatErrorType.STT) }
        }
    }

    fun stopListening() {
        voiceManager.stopListening()
        _state.update { it.copy(isListening = false) }
    }

    private fun speakResponse(text: String) {
        _state.update { it.copy(isSpeaking = true) }
        voiceManager.speak(text, flush = true) {
            _state.update { it.copy(isSpeaking = false) }
        }
    }

    fun stopAll() {
        try { voiceChatEngine.stopResponse() } catch (_: Exception) {}
        try { voiceManager.stopSpeaking() } catch (_: Exception) {}
        try { voiceManager.stopListening() } catch (_: Exception) {}
        _state.update { it.copy(isSpeaking = false, isListening = false, isThinking = false) }
    }

    fun clearError() {
        _state.update { it.copy(error = null, errorType = null) }
    }

    fun stopSpeaking() {
        stopAll()
    }

    // ─── Session Tracking ─────────────────────────────────────────────────────

    fun onStartSession() {
        startTime = System.currentTimeMillis()
    }

    fun onStopSession() {
        if (startTime > 0) {
            val sessionSeconds = (System.currentTimeMillis() - startTime) / 1000
            if (sessionSeconds > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    statsRepository.trackModeTime(
                        sessionSeconds,
                        com.aipoweredgita.app.repository.ModeType.VOICE
                    )
                }
            }
            startTime = 0
        }
    }

    override fun onCleared() {
        try { onStopSession() } catch (_: Exception) {}
        try { aiScope.cancel() } catch (_: Exception) {}
        try { stopAll() } catch (_: Exception) {}
        try { voiceChatEngine.close() } catch (_: Exception) {}
        try { voiceManager.destroy() } catch (_: Exception) {}
        super.onCleared()
    }

    // ─── Language Mode ────────────────────────────────────────────────────────

    private var currentLanguageMode: com.aipoweredgita.app.utils.LanguageMode =
        com.aipoweredgita.app.utils.LanguageMode.AUTO

    fun setLanguageMode(mode: com.aipoweredgita.app.utils.LanguageMode) {
        currentLanguageMode = mode
        voiceManager.setLocale(mode.sttLocale, mode.ttsLocale)
        aiScope.launch {
            voiceChatEngine.updateSystemInstruction(mode.systemInstruction)
        }
    }
}