package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.VoiceChatMessage
import com.aipoweredgita.app.ml.AppFeature
import com.aipoweredgita.app.ml.LiteRtLmVoiceChatEngine
import com.aipoweredgita.app.ml.ModelAvailability
import com.aipoweredgita.app.utils.AiTurnManager
import com.aipoweredgita.app.utils.DeviceCapability
import com.aipoweredgita.app.utils.LanguageMode
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.ml.TranslationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.aipoweredgita.app.utils.DeviceTier
import com.aipoweredgita.app.utils.DeviceTierDetector

// ─── Data Models ──────────────────────────────────────────────────────────────

@Immutable
data class ChatMessage(
    val id        : String = UUID.randomUUID().toString(),
    val text      : String,
    val isUser    : Boolean,
    val timestamp : Long = System.currentTimeMillis()
)

@Stable
data class VoiceChatState(
    val messages       : List<ChatMessage>  = emptyList(),
    val isListening    : Boolean            = false,
    val isSpeaking     : Boolean            = false,
    val isThinking     : Boolean            = false,
    val liveTranscript : String             = "",
    val userInput      : String             = "",
    val isLlmReady     : Boolean            = false,
    val error          : String?            = null,
    val errorType      : VoiceChatErrorType? = null,
    val currentModelName: String             = "Unknown"
)

enum class VoiceChatErrorType {
    MODEL_INIT, LLM_INFERENCE, STT, TTS, NETWORK, CRASH_RECOVERY
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "VoiceChatViewModel"

    private val _state = MutableStateFlow(VoiceChatState())
    val state: StateFlow<VoiceChatState> = _state.asStateFlow()

    private val database        = GitaDatabase.getDatabase(application)
    private val chatMessageDao  = database.voiceChatMessageDao()
    private val statsRepository = StatsRepository(database.userStatsDao())
    private val voiceManager    = VoiceManager(application)
    private val voiceChatEngine = LiteRtLmVoiceChatEngine(application)
    private val translationManager = TranslationManager()

    private var useProxy = false
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    private suspend fun fetchGroqReply(groundedPrompt: String, history: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val systemPrompt = currentLanguageMode.systemInstruction
        val json = JSONObject()
        val messagesArray = JSONArray()
        
        val systemMsg = JSONObject()
            .put("role", "system")
            .put("content", systemPrompt)
        messagesArray.put(systemMsg)
        
        val historyToSend = history.filter { it.text.isNotEmpty() }
        if (historyToSend.isNotEmpty()) {
            for (i in 0 until historyToSend.size - 1) {
                val msg = historyToSend[i]
                val role = if (msg.isUser) "user" else "assistant"
                messagesArray.put(
                    JSONObject()
                        .put("role", role)
                        .put("content", msg.text)
                )
            }
            messagesArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", groundedPrompt)
            )
        } else {
            messagesArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", groundedPrompt)
            )
        }
        
        json.put("messages", messagesArray)
        
        val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        
        val request = Request.Builder()
            .url("https://noisy-sheep-76.sravanku018.deno.net/")
            .post(body)
            .build()
            
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Unexpected code $response")
            }
            val responseString = response.body?.string() ?: throw java.io.IOException("Empty response body")
            val jsonResponse = JSONObject(responseString)
            jsonResponse.getString("reply")
        }
    }

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val aiScope      = CoroutineScope(aiDispatcher + SupervisorJob())
    private val initMutex    = Mutex()

    private var startTime     = 0L
    private var lastUpdate    = 0L
    private var crashCount    = 0
    private var lastCrashTime = 0L
    private val MAX_CRASHES   = 3

    private var currentLanguageMode : LanguageMode = LanguageMode.AUTO

    // ─── Current Verse State ──────────────────────────────────────────────────
    private var currentCachedVerse : CachedVerse? = null
    private var currentGitaVerse   : GitaVerse?   = null

    init {
        setupVoiceManagerErrorForwarding()
        loadMessages()
        observeModelChanges()
        refreshModelStatus()
        viewModelScope.launch {
            translationManager.downloadModelsIfNeeded()
        }
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private fun setupVoiceManagerErrorForwarding() {
        voiceManager.onError = { errorMsg ->
            viewModelScope.launch(Dispatchers.Main) {
                _state.update { it.copy(error = errorMsg, errorType = VoiceChatErrorType.TTS) }
            }
        }
    }

    private fun observeModelChanges() {
        viewModelScope.launch {
            ModelAvailability.getInstance(getApplication()).selectedModel.collect { modelName ->
                Log.d(tag, "Model changed to $modelName — re-initializing")
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

    // ─── Model Init ───────────────────────────────────────────────────────────

    fun refreshModelStatus() {
        val context = getApplication<Application>()
        val tier = DeviceTierDetector.detect(context)
        val isLowTier = tier == DeviceTier.LOW || tier == DeviceTier.LOW_MID

        val ma = ModelAvailability.getInstance(context)
        val selected = ma.selectedModel.value

        if (isLowTier || selected.contains("Groq", ignoreCase = true)) {
            Log.d(tag, "Low-tier device detected (${tier.label}) or Cloud Proxy selected. Forcing Deno proxy fallback.")
            useProxy = true
            _state.update {
                it.copy(
                    isLlmReady = true,
                    currentModelName = "Cloud Proxy (Groq)",
                    error = null,
                    errorType = null
                )
            }
            return
        }

        try {
            val ma        = ModelAvailability.getInstance(context)
            val modelPath = ma.getResolvedModelPath(AppFeature.VOICE)

            if (modelPath != null) {
                // ✅ Adaptive config based on device RAM tier
                val maxTokens  = DeviceCapability.getOptimalMaxTokens(context)
                val timeoutMs  = DeviceCapability.getOptimalTimeout(context)
                val samplerParams = DeviceCapability.getOptimalSampler(modelPath)
                val sampler    = com.google.ai.edge.litertlm.SamplerConfig(
                    topK        = samplerParams.topK,
                    topP        = samplerParams.topP.toDouble(),
                    temperature = samplerParams.temperature.toDouble()
                )

                val modelName = File(modelPath).name
                Log.d(tag, "Using model: $modelName on Device tier: ${tier.label} " +
                           "tokens=$maxTokens timeout=${timeoutMs}ms " +
                           "topK=${samplerParams.topK} temp=${samplerParams.temperature}")

                _state.update { it.copy(currentModelName = modelName) }

                aiScope.launch {
                    initMutex.withLock {
                        try {
                            val success = voiceChatEngine.initialize(
                                path      = modelPath,
                                maxTokens = maxTokens,
                                timeoutMs = timeoutMs,
                                sampler   = sampler
                            )
                            if (success) {
                                voiceChatEngine.updateSystemInstruction(currentLanguageMode.systemInstruction)
                                crashCount = 0
                                useProxy = false
                                withContext(Dispatchers.Main) {
                                    _state.update {
                                        it.copy(
                                            isLlmReady = true,
                                            error      = null,
                                            errorType  = null
                                        )
                                    }
                                }
                            } else {
                                Log.w(tag, "On-device model initialization failed. Falling back to Deno proxy.")
                                useProxy = true
                                withContext(Dispatchers.Main) {
                                    _state.update {
                                        it.copy(
                                            isLlmReady = true,
                                            currentModelName = "Cloud Proxy (Groq)",
                                            error      = null,
                                            errorType  = null
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Model init crashed, falling back to Deno proxy", e)
                            useProxy = true
                            withContext(Dispatchers.Main) {
                                _state.update {
                                    it.copy(
                                        isLlmReady = true,
                                        currentModelName = "Cloud Proxy (Groq)",
                                        error      = null,
                                        errorType  = null
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Log.d(tag, "No on-device models downloaded. Falling back to Deno proxy.")
                useProxy = true
                _state.update {
                    it.copy(
                        isLlmReady = true,
                        currentModelName = "Cloud Proxy (Groq)",
                        error = null,
                        errorType = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to check model status, falling back to Deno proxy", e)
            useProxy = true
            _state.update {
                it.copy(
                    isLlmReady = true,
                    currentModelName = "Cloud Proxy (Groq)",
                    error      = null,
                    errorType  = null
                )
            }
        }
    }

    // ─── Grounded Prompt Builder ──────────────────────────────────────────────

    /**
     * Sweet spot prompt builder:
     * — No verse context → Krishna answers freely from Gita wisdom
     * — Verse context available → grounded answer using only provided data
     */
    private fun buildGroundedPrompt(
        userText    : String,
        cachedVerse : CachedVerse? = null,
        gitaVerse   : GitaVerse?   = null,
        forceTelugu : Boolean      = false
    ): String {
        val langSuffix = if (forceTelugu) " You MUST respond ENTIRELY in Telugu (తెలుగు). Do not use English." else ""

        // No verse context — let Krishna answer freely
        if (cachedVerse == null && gitaVerse == null) {
            return buildString {
                appendLine("Question: $userText")
                appendLine()
                val sentences = DeviceCapability.getOptimalSentenceCount(getApplication())
                appendLine("If the Question is just a simple greeting (e.g., 'hello', 'hi', 'hey', 'namaste', 'namste', 'నమస్తే', 'నమస్కారం', etc.), respond with a warm, brief Krishna-style greeting (1 sentence max) asking how you can guide them. Do not provide a long sermon or explanation.")
                append("Otherwise, answer the Question as Krishna in $sentences sentences from Bhagavad Gita wisdom.$langSuffix")
            }
        }
        // Verse context available — ground the answer
        return buildString {
            appendLine("Use ONLY the verse data below. Do not invent any text.")
            appendLine()

            // Merge both sources — gitaVerse (API) takes priority,
            // cachedVerse (Room) fills missing fields
            val chapterNo   = gitaVerse?.chapterNo   ?: cachedVerse?.chapterNo
            val verseNo     = gitaVerse?.verseNo     ?: cachedVerse?.verseNo
            val verseText   = gitaVerse?.verse?.takeIf       { it.isNotBlank() } ?: cachedVerse?.verse
            val translation = gitaVerse?.translation?.takeIf { it.isNotBlank() } ?: cachedVerse?.translation
            val explanationLimit = DeviceCapability.getOptimalExplanationLength(getApplication())
            val explanation = gitaVerse?.purport?.firstOrNull()?.takeIf { it.isNotBlank() }?.take(explanationLimit)
                              ?: cachedVerse?.explanation?.take(explanationLimit)

            if (chapterNo != null && verseNo != null)
                appendLine("Chapter: $chapterNo, Verse: $verseNo")
            verseText?.let   { appendLine("Verse: $it") }
            translation?.let { appendLine("Translation: $it") }
            explanation?.let { appendLine("Explanation: $it") }

            appendLine()
            appendLine("Question: $userText")
            appendLine()
            val sentences = DeviceCapability.getOptimalSentenceCount(getApplication())
            append("Explain this verse by telling a unique, creative, and non-repetitive mini-story or practical analogy in $sentences sentences. Keep it simple, engaging, and easy to understand. Do not use complex jargon. Do not rewrite the verse itself.$langSuffix")
        }
    }

    // ─── Messaging ────────────────────────────────────────────────────────────

    fun updateUserInput(input: String) {
        _state.update { it.copy(userInput = input) }
    }

    fun sendMessage(
        text        : String?      = null,
        cachedVerse : CachedVerse? = null,
        gitaVerse   : GitaVerse?   = null
    ) {
        val messageText = text ?: _state.value.userInput
        if (messageText.isBlank()) return

        // Crash loop protection
        val now = System.currentTimeMillis()
        if (now - lastCrashTime > 60_000) crashCount = 0
        if (crashCount >= MAX_CRASHES) {
            _state.update {
                it.copy(
                    error      = "Voice chat crashed too many times. Please restart the app.",
                    errorType  = VoiceChatErrorType.CRASH_RECOVERY,
                    isThinking = false
                )
            }
            return
        }

        if (text == null) _state.update { it.copy(userInput = "", error = null, errorType = null) }

        val userMessage = ChatMessage(text = messageText, isUser = true)
        _state.update { it.copy(messages = it.messages + userMessage, error = null, errorType = null, isThinking = true) }
        saveMessage(userMessage)

        aiScope.launch {
            val aiMessageId = UUID.randomUUID().toString()

            // Add placeholder AI message for streaming
            withContext(Dispatchers.Main) {
                _state.update { s ->
                    s.copy(messages = s.messages + ChatMessage(id = aiMessageId, text = "", isUser = false))
                }
            }

            try {
                AiTurnManager.mutex.withLock {
                    stopAll()

                    val isTeluguMode = currentLanguageMode.sttLocale.contains("te", ignoreCase = true)
                    val ma           = ModelAvailability.getInstance(getApplication())
                    val isUsingGemma = ma.isGemmaRunning(AppFeature.VOICE)
                    val useMlKit     = isTeluguMode && !isUsingGemma && !useProxy

                    val processedUserText = if (useMlKit) {
                        withContext(Dispatchers.Main) {
                            _state.update { s ->
                                s.copy(messages = s.messages.map { m ->
                                    if (m.id == aiMessageId) m.copy(text = "") else m
                                })
                            }
                        }
                        translationManager.translateTeluguToEnglish(messageText)
                    } else {
                        messageText
                    }

                    // ✅ FIX: grounded prompt using English (if ML Kit) or forced Telugu (if native)
                    val groundedPrompt = buildGroundedPrompt(
                        userText    = processedUserText,
                        cachedVerse = cachedVerse ?: currentCachedVerse,
                        gitaVerse   = gitaVerse   ?: currentGitaVerse,
                        forceTelugu = isTeluguMode && !useMlKit // Use native Telugu for Gemma 4
                    )
                    Log.d(tag, "GROUNDED PROMPT:\n$groundedPrompt")

                    if (useProxy) {
                        val reply = fetchGroqReply(groundedPrompt, _state.value.messages)
                        val basicCleaned = com.aipoweredgita.app.util.TextUtils.cleanLlmOutput(reply)
                        val finalAnswer = com.aipoweredgita.app.util.TextUtils.deepClean(basicCleaned)

                        withContext(Dispatchers.Main) {
                            _state.update { s ->
                                s.copy(
                                    messages = s.messages.map { m ->
                                        if (m.id == aiMessageId) m.copy(text = finalAnswer) else m
                                    },
                                    isThinking = false
                                )
                            }
                            saveMessage(ChatMessage(id = aiMessageId, text = finalAnswer, isUser = false))
                            try {
                                speakResponse(finalAnswer)
                            } catch (e: Exception) {
                                Log.e(tag, "TTS failed", e)
                                _state.update {
                                    it.copy(isSpeaking = false, error = "Voice output failed", errorType = VoiceChatErrorType.TTS)
                                }
                            }
                        }
                    } else {
                        voiceChatEngine.sendMessage(
                            prompt    = groundedPrompt,
                            onPartial = { partial ->
                                val nowMs = System.currentTimeMillis()
                                if (nowMs - lastUpdate > 64) { // ~15fps throttle
                                    lastUpdate = nowMs
                                    viewModelScope.launch(Dispatchers.Main) {
                                        _state.update { s ->
                                            s.copy(messages = s.messages.map { m ->
                                                if (m.id == aiMessageId) m.copy(text = partial) else m
                                            })
                                        }
                                    }
                                }
                            },
                            onCleaned = { deepCleaned ->
                                aiScope.launch {
                                    val finalAnswer = if (useMlKit) {
                                        withContext(Dispatchers.Main) {
                                            _state.update { s ->
                                                s.copy(messages = s.messages.map { m ->
                                                    if (m.id == aiMessageId) m.copy(text = deepCleaned) else m
                                                })
                                            }
                                        }
                                        translationManager.translateEnglishToTelugu(deepCleaned)
                                    } else {
                                        deepCleaned
                                    }

                                    withContext(Dispatchers.Main) {
                                        _state.update { s ->
                                            s.copy(
                                                messages   = s.messages.map { m ->
                                                    if (m.id == aiMessageId) m.copy(text = finalAnswer) else m
                                                },
                                                isThinking = false  // ✅ cleared here on happy path
                                            )
                                        }
                                        saveMessage(ChatMessage(id = aiMessageId, text = finalAnswer, isUser = false))
                                        try {
                                            speakResponse(finalAnswer)
                                        } catch (e: Exception) {
                                            Log.e(tag, "TTS failed", e)
                                            _state.update {
                                                it.copy(isSpeaking = false, error = "Voice output failed", errorType = VoiceChatErrorType.TTS)
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                crashCount++
                lastCrashTime = System.currentTimeMillis()
                Log.e(tag, "Voice chat crash #$crashCount", e)
                withContext(Dispatchers.Main) {
                    _state.update {
                        it.copy(
                            isThinking = false, // ✅ FIX: always cleared on error too
                            error      = when (e) {
                                is java.io.IOException   -> "Network error — check connection"
                                is IllegalStateException -> "Model error — try restarting chat"
                                else                     -> "Something went wrong: ${e.message ?: "Unknown"}"
                            },
                            errorType  = when (e) {
                                is java.io.IOException   -> VoiceChatErrorType.NETWORK
                                is IllegalStateException -> VoiceChatErrorType.LLM_INFERENCE
                                else                     -> VoiceChatErrorType.CRASH_RECOVERY
                            }
                        )
                    }
                }
            } finally {
                // ✅ FIX: safety net — isThinking cleared even if onCleaned never fires
                withContext(Dispatchers.Main) {
                    _state.update { if (it.isThinking) it.copy(isThinking = false) else it }
                }
            }
        }
    }

    private fun saveMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.insertMessage(
                VoiceChatMessage(
                    id        = message.id,
                    text      = message.text,
                    isUser    = message.isUser,
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
                onResult        = { result ->
                    _state.update { it.copy(isListening = false, liveTranscript = "") }
                    if (result.isNotBlank()) sendMessage(
                        text        = result,
                        cachedVerse = currentCachedVerse,
                        gitaVerse   = currentGitaVerse
                    )
                },
                onPartialResult = { partial ->
                    _state.update { it.copy(liveTranscript = partial) }
                },
                onError         = { err ->
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
        try { voiceManager.stopSpeaking() }    catch (_: Exception) {}
        try { voiceManager.stopListening() }   catch (_: Exception) {}
        _state.update { it.copy(isSpeaking = false, isListening = false, isThinking = false) }
    }

    fun clearError()    { _state.update { it.copy(error = null, errorType = null) } }
    fun stopSpeaking()  { stopAll() }

    // ─── Verse Context ────────────────────────────────────────────────────────

    /**
     * Call this whenever the user opens or navigates to a new verse.
     * Stores verse context for all subsequent sendMessage calls.
     * Resets conversation to prevent context bleed from previous verse.
     */
    fun setCurrentVerse(
        cachedVerse : CachedVerse? = null,
        gitaVerse   : GitaVerse?   = null
    ) {
        currentCachedVerse = cachedVerse
        currentGitaVerse   = gitaVerse
        Log.d(tag, "Verse set — chapter=${gitaVerse?.chapterNo ?: cachedVerse?.chapterNo} verse=${gitaVerse?.verseNo ?: cachedVerse?.verseNo}")
        aiScope.launch { voiceChatEngine.resetConversation() }
    }

    fun clearCurrentVerse() {
        currentCachedVerse = null
        currentGitaVerse   = null
    }

    // ─── Language Mode ────────────────────────────────────────────────────────

    fun setLanguageMode(mode: LanguageMode) {
        currentLanguageMode = mode
        voiceManager.setLocale(mode.sttLocale, mode.ttsLocale)
        aiScope.launch {
            voiceChatEngine.updateSystemInstruction(mode.systemInstruction)
        }
    }

    // ─── Session Tracking ─────────────────────────────────────────────────────

    fun onStartSession() { startTime = System.currentTimeMillis() }

    fun onStopSession() {
        if (startTime > 0) {
            val sessionSeconds = (System.currentTimeMillis() - startTime) / 1000
            if (sessionSeconds > 0) {
                viewModelScope.launch(Dispatchers.IO) {
                    statsRepository.trackModeTime(sessionSeconds, ModeType.VOICE)
                }
            }
            startTime = 0
        }
    }

    override fun onCleared() {
        try { onStopSession() }         catch (_: Exception) {}
        try { aiScope.cancel() }        catch (_: Exception) {}
        try { stopAll() }               catch (_: Exception) {}
        try { voiceChatEngine.close() } catch (_: Exception) {}
        try { voiceManager.destroy() }  catch (_: Exception) {}
        super.onCleared()
    }
}