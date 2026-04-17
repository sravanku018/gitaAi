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
    val error: String? = null
)

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

    init {
        loadMessages()
        observeModelChanges()
        refreshModelStatus()
    }

    // ─── Text Spacing Fix ──────────────────────────────────────────────────────
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
        val ma = ModelAvailability.getInstance(context)
        val modelPath = ma.getResolvedModelPath(AppFeature.VOICE)

        if (modelPath != null) {
            aiScope.launch {
                initMutex.withLock {
                    val success = voiceChatEngine.initialize(modelPath)
                    if (success) {
                        voiceChatEngine.updateSystemInstruction(currentLanguageMode.systemInstruction)
                    }
                    withContext(Dispatchers.Main) {
                        _state.update {
                            it.copy(
                                isLlmReady = success,
                                error = if (!success) "Failed to initialize AI model" else null
                            )
                        }
                    }
                }
            }
        } else {
            _state.update { it.copy(isLlmReady = false) }
        }
    }

    // ─── Messaging ────────────────────────────────────────────────────────────

    fun updateUserInput(input: String) {
        _state.update { it.copy(userInput = input) }
    }

    fun sendMessage(text: String? = null) {
        val messageText = text ?: _state.value.userInput
        if (messageText.isBlank()) return

        if (text == null) {
            _state.update { it.copy(userInput = "") }
        }

        val userMessage = ChatMessage(text = messageText, isUser = true)
        _state.update { it.copy(messages = it.messages + userMessage) }
        saveMessage(userMessage)
        _state.update { it.copy(isThinking = true) }

        aiScope.launch {
            AiTurnManager.mutex.withLock {
                stopAll()

                val aiMessageId = UUID.randomUUID().toString()

                withContext(Dispatchers.Main) {
                    _state.update { s ->
                        s.copy(messages = s.messages + ChatMessage(id = aiMessageId, text = "", isUser = false))
                    }
                }

                // THE STRICT SEQUENTIAL PROCESS:
                // 1. CATCHING (Stream raw partials to screen for speed)
                voiceChatEngine.sendMessage(
                    prompt = messageText,
                    onPartial = { partial ->
                        viewModelScope.launch(Dispatchers.Main) {
                            _state.update { s ->
                                val updated = s.messages.map { m ->
                                    if (m.id == aiMessageId) m.copy(text = fixSpacing(partial)) else m
                                }
                                s.copy(messages = updated)
                            }
                        }
                    },
                    onCleaned = { deepCleaned ->
                        // 2. SORTING (Background Deep Clean finished)
                        // 3. TTS (Only after sorting is done)
                        viewModelScope.launch(Dispatchers.Main) {
                            _state.update { it.copy(isThinking = false) }
                            _state.update { s ->
                                val updated = s.messages.map { m ->
                                    if (m.id == aiMessageId) m.copy(text = deepCleaned) else m
                                }
                                s.copy(messages = updated)
                            }
                            
                            // Save the final cleaned version
                            saveMessage(ChatMessage(id = aiMessageId, text = deepCleaned, isUser = false))
                            
                            // Step 3: TTS
                            speakResponse(deepCleaned)
                        }
                    }
                )
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
        _state.update { it.copy(isSpeaking = false, isListening = true, liveTranscript = "") }

        voiceManager.startListening(
            onResult = { result ->
                _state.update { it.copy(isListening = false, liveTranscript = "") }
                if (result.isNotBlank()) sendMessage(result)
            },
            onPartialResult = { partial ->
                _state.update { it.copy(liveTranscript = partial) }
            },
            onError = { err ->
                _state.update { it.copy(isListening = false, error = "Speech error: $err") }
            }
        )
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
        voiceChatEngine.stopResponse()
        voiceManager.stopSpeaking()
        voiceManager.stopListening()
        _state.update { it.copy(isSpeaking = false, isListening = false, isThinking = false) }
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
        onStopSession()
        super.onCleared()
        aiScope.cancel()
        stopAll()
        voiceChatEngine.close()
        voiceManager.destroy()
    }

    // ─── Language Mode ────────────────────────────────────────────────────────

    private var currentLanguageMode: com.aipoweredgita.app.utils.LanguageMode =
        com.aipoweredgita.app.utils.LanguageMode.ENG_TO_TEL

    fun setLanguageMode(mode: com.aipoweredgita.app.utils.LanguageMode) {
        currentLanguageMode = mode
        voiceManager.setLocale(mode.sttLocale, mode.ttsLocale)
        aiScope.launch {
            voiceChatEngine.updateSystemInstruction(mode.systemInstruction)
        }
    }
}