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
<<<<<<< HEAD
import com.aipoweredgita.app.utils.DeviceCapability
import com.aipoweredgita.app.utils.LanguageMode
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.ui.components.CoinAnimationManager
import com.aipoweredgita.app.ui.components.CoinEvent
import com.aipoweredgita.app.ui.components.CoinEventType
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.prompt.GitaPromptEngine
import com.aipoweredgita.app.prompt.VerseContext

=======
import com.aipoweredgita.app.utils.LanguageMode
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
import java.io.File
import java.util.UUID
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
=======
import java.util.UUID
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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
<<<<<<< HEAD
    val errorType      : VoiceChatErrorType? = null,
    val currentModelName: String             = "Unknown",
    val coinBalance     : Int                = 0,
    val coinError       : CoinError?         = null,
    val showCoinConfirmation: Boolean       = false,
    val pendingMessage  : String?           = null,
    val pendingCost     : Int                = 0
=======
    val errorType      : VoiceChatErrorType? = null
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
)

enum class VoiceChatErrorType {
    MODEL_INIT, LLM_INFERENCE, STT, TTS, NETWORK, CRASH_RECOVERY
}

<<<<<<< HEAD
enum class CoinError { NETWORK_ERROR, UNKNOWN_ERROR }

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
// ─── ViewModel ────────────────────────────────────────────────────────────────

class VoiceChatViewModel(application: Application) : AndroidViewModel(application) {

    private val tag = "VoiceChatViewModel"

    private val _state = MutableStateFlow(VoiceChatState())
    val state: StateFlow<VoiceChatState> = _state.asStateFlow()

    private val database        = GitaDatabase.getDatabase(application)
<<<<<<< HEAD
    private val chatRepo = com.aipoweredgita.app.repository.ChatRepository(database.voiceChatMessageDao())
    private val statsRepository = StatsRepository(database.userStatsDao(), database.dailyActivityDao(), application)
    private val voiceManager    = VoiceManager(application)
    private val voiceChatEngine = LiteRtLmVoiceChatEngine(application)

    private var useProxy = false
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    /**
     * Converts current chat messages to (role, content) pairs for the engine.
     */
    private fun buildHistory(): List<Pair<String, String>> =
        _state.value.messages
            .takeLast(10)
            .map { msg -> (if (msg.isUser) "user" else "assistant") to msg.text }

    /**
     * Builds and sends the full message chain to the Deno proxy.
     * System prompt comes from GitaPromptEngine — verse context is a
     * separate system message so it doesn't pollute the soul prompt.
     */
    private suspend fun fetchGroqReply(
        groundedPrompt: String,
        history: List<ChatMessage>,
        verseReference: String? = null
    ): String = withContext(Dispatchers.IO) {

        val messagesArray = JSONArray()

        // 1. Krishna soul + format rules (GitaPromptEngine)
        messagesArray.put(
            JSONObject()
                .put("role", "system")
                .put("content", GitaPromptEngine.groqSystemPrompt())
        )

        // 2. Verse reference context — separate system message
        if (!verseReference.isNullOrBlank()) {
            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", verseReference)
            )
        }

        // 3. Conversation history (skip last — that's groundedPrompt)
        history
            .filter { it.text.isNotEmpty() }
            .dropLast(1)
            .takeLast(6)
            .forEach { msg ->
                messagesArray.put(
                    JSONObject()
                        .put("role", if (msg.isUser) "user" else "assistant")
                        .put("content", msg.text)
                )
            }

        // 4. Current user question (wrapped with reflective instructions)
        val fullUserPrompt = GitaPromptEngine.buildUserInstructPrompt(
            userMessage = groundedPrompt,
            hasVerseContext = !verseReference.isNullOrBlank(),
            langSuffix = ""
        )
        messagesArray.put(
            JSONObject()
                .put("role", "user")
                .put("content", fullUserPrompt)
        )

        val body = JSONObject()
            .put("messages", messagesArray)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://noisy-sheep-76.sravanku018.deno.net/")
            .post(body)
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw java.io.IOException("Proxy error: $response")
            }
            val responseString = response.body?.string()
                ?: throw java.io.IOException("Empty response")
            JSONObject(responseString).getString("reply")
        }
    }

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private lateinit var aiScope: CoroutineScope
=======
    private val chatMessageDao  = database.voiceChatMessageDao()
    private val statsRepository = StatsRepository(database.userStatsDao())
    private val voiceManager    = VoiceManager(application)
    private val voiceChatEngine = LiteRtLmVoiceChatEngine(application)

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val aiScope      = CoroutineScope(aiDispatcher + SupervisorJob())
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    private var activeVerse       : VerseContext? = null

    init {
        // aiScope is child of viewModelScope — cancels automatically on clearance
        aiScope = CoroutineScope(aiDispatcher + SupervisorJob(viewModelScope.coroutineContext[Job]))
        setupVoiceManagerErrorForwarding()
        loadMessages()
        observeModelChanges()
        observeUserStats()
=======

    init {
        setupVoiceManagerErrorForwarding()
        loadMessages()
        observeModelChanges()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        refreshModelStatus()
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

<<<<<<< HEAD
    private fun observeUserStats() {
        viewModelScope.launch {
            database.userStatsDao().getUserStats().collect { stats ->
                val uid = stats?.userId
                if (uid != null && uid.isNotEmpty()) {
                    try {
                        val balance = CoinApi.retrofitService.getBalance(uid).krishna_coins
                        _state.update { it.copy(coinBalance = balance, coinError = null) }
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to fetch coin balance", e)
                        _state.update { it.copy(coinError = CoinError.NETWORK_ERROR) }
                    }
                }
            }
        }
    }

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            val dbMessages = chatRepo.getAllMessages()
=======
            val dbMessages = chatMessageDao.getAllMessages()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD

        val ma = ModelAvailability.getInstance(context)
        val decision = ma.getRuntimeDecision(AppFeature.VOICE)

        if (decision.useProxy) {
            Log.d(tag, "Using proxy runtime for voice chat. tier=${decision.tierLabel} selected=${decision.selectedPreference}")
            useProxy = true
            _state.update {
                it.copy(
                    isLlmReady = true,
                    currentModelName = decision.displayName,
                    error = null,
                    errorType = null
                )
            }
            return
        }

        try {
            val modelPath = decision.modelPath

            if (modelPath != null) {
                val maxTokens  = DeviceCapability.getOptimalMaxTokens(context, decision.displayName)
                val timeoutMs  = DeviceCapability.getOptimalTimeout(context)
                val samplerParams = DeviceCapability.getOptimalSampler(modelPath)
                val sampler    = com.google.ai.edge.litertlm.SamplerConfig(
                    topK        = samplerParams.topK,
                    topP        = samplerParams.topP.toDouble(),
                    temperature = samplerParams.temperature.toDouble()
                )

                Log.d(tag, "Using model: ${decision.displayName} on Device tier: ${decision.tierLabel} " +
                           "tokens=$maxTokens timeout=${timeoutMs}ms " +
                           "topK=${samplerParams.topK} temp=${samplerParams.temperature}")

                _state.update { it.copy(currentModelName = decision.displayName) }

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
                                voiceChatEngine.updateSystemInstruction(
                                    GitaPromptEngine.gemmaSystemPrompt(activeVerse)
                                )
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
=======
        try {
            val ma        = ModelAvailability.getInstance(context)
            val modelPath = ma.getResolvedModelPath(AppFeature.VOICE)

            if (modelPath != null) {
                aiScope.launch {
                    initMutex.withLock {
                        try {
                            val success = voiceChatEngine.initialize(modelPath)
                            if (success) {
                                voiceChatEngine.updateSystemInstruction(currentLanguageMode.systemInstruction)
                                crashCount = 0
                            }
                            withContext(Dispatchers.Main) {
                                _state.update {
                                    it.copy(
                                        isLlmReady = success,
                                        error      = if (!success) "Failed to initialize AI model" else null,
                                        errorType  = if (!success) VoiceChatErrorType.MODEL_INIT else null
                                    )
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(tag, "Model init crashed", e)
                            withContext(Dispatchers.Main) {
                                _state.update {
                                    it.copy(
                                        isLlmReady = false,
                                        error      = "Model init failed: ${e.message ?: "Unknown"}",
                                        errorType  = VoiceChatErrorType.MODEL_INIT
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
<<<<<<< HEAD
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
=======
                _state.update { it.copy(isLlmReady = false) }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to check model status", e)
            _state.update {
                it.copy(
                    isLlmReady = false,
                    error      = "Could not check model status",
                    errorType  = VoiceChatErrorType.MODEL_INIT
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                )
            }
        }
    }

<<<<<<< HEAD
=======
    // ─── Grounded Prompt Builder ──────────────────────────────────────────────

    /**
     * Sweet spot prompt builder:
     * — No verse context → Krishna answers freely from Gita wisdom
     * — Verse context available → grounded answer using only provided data
     */
    private fun buildGroundedPrompt(
        userText    : String,
        cachedVerse : CachedVerse? = null,
        gitaVerse   : GitaVerse?   = null
    ): String {
        // No verse context — let Krishna answer freely
        if (cachedVerse == null && gitaVerse == null) {
            return buildString {
                appendLine("Question: $userText")
                appendLine()
                append("Answer as Krishna in 3 sentences from Bhagavad Gita wisdom.")
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
            val explanation = gitaVerse?.purport?.firstOrNull()?.takeIf { it.isNotBlank() }
                ?: cachedVerse?.explanation?.take(200)

            if (chapterNo != null && verseNo != null)
                appendLine("Chapter: $chapterNo, Verse: $verseNo")
            verseText?.let   { appendLine("Verse: $it") }
            translation?.let { appendLine("Translation: $it") }
            explanation?.let { appendLine("Explanation: $it") }

            appendLine()
            appendLine("Question: $userText")
            appendLine()
            append("Explain this verse in 3 sentences. Do not rewrite it.")
        }
    }

>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    // ─── Messaging ────────────────────────────────────────────────────────────

    fun updateUserInput(input: String) {
        _state.update { it.copy(userInput = input) }
    }

    fun sendMessage(
        text        : String?      = null,
        cachedVerse : CachedVerse? = null,
<<<<<<< HEAD
        gitaVerse   : GitaVerse?   = null,
        confirmed   : Boolean      = true
=======
        gitaVerse   : GitaVerse?   = null
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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

<<<<<<< HEAD
        viewModelScope.launch(Dispatchers.IO) {
            statsRepository.spendCoins(messageText)
            val uid = database.userStatsDao().getUserStatsOnce()?.userId
            if (uid != null && uid.isNotEmpty()) {
                try {
                    val balance = CoinApi.retrofitService.getBalance(uid).krishna_coins
                    _state.update { it.copy(coinBalance = balance, coinError = null) }
                } catch (e: Exception) {
                    Log.e(tag, "Failed to fetch balance after spend", e)
                    _state.update { it.copy(coinError = CoinError.NETWORK_ERROR) }
                }
            }
        }

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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

<<<<<<< HEAD
                    if (useProxy) {
                        val verseRef = activeVerse?.let { v ->
                            "[Chapter ${v.chapter}, Verse ${v.verse}]\nTranslation: ${v.translation}\nDepth: ${v.explanation}"
                        }
                        val reply = fetchGroqReply(
                            groundedPrompt = messageText,
                            history = _state.value.messages,
                            verseReference = verseRef
                        )
                        val finalAnswer = com.aipoweredgita.app.util.TextUtils.deepClean(reply)

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
                        val gemmaMessage = GitaPromptEngine.buildGemmaUserContent(
                            userMessage = messageText,
                            verse = activeVerse
                        )
                        voiceChatEngine.sendMessage(
                            prompt    = gemmaMessage,
                            onPartial = { partial ->
                                val nowMs = System.currentTimeMillis()
                                if (nowMs - lastUpdate > 64) {
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
                                    val finalAnswer = deepCleaned

                                    withContext(Dispatchers.Main) {
                                        _state.update { s ->
                                            s.copy(
                                                messages   = s.messages.map { m ->
                                                    if (m.id == aiMessageId) m.copy(text = finalAnswer) else m
                                                },
                                                isThinking = false,
                                                coinError  = null
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
=======
                    // ✅ FIX: grounded prompt — param verse first, stored verse as fallback
                    val groundedPrompt = buildGroundedPrompt(
                        userText    = messageText,
                        cachedVerse = cachedVerse ?: currentCachedVerse,
                        gitaVerse   = gitaVerse   ?: currentGitaVerse
                    )
                    Log.d(tag, "GROUNDED PROMPT:\n$groundedPrompt")

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
                            viewModelScope.launch(Dispatchers.Main) {
                                _state.update { s ->
                                    s.copy(
                                        messages   = s.messages.map { m ->
                                            if (m.id == aiMessageId) m.copy(text = deepCleaned) else m
                                        },
                                        isThinking = false  // ✅ cleared here on happy path
                                    )
                                }
                                saveMessage(ChatMessage(id = aiMessageId, text = deepCleaned, isUser = false))
                                try {
                                    speakResponse(deepCleaned)
                                } catch (e: Exception) {
                                    Log.e(tag, "TTS failed", e)
                                    _state.update {
                                        it.copy(isSpeaking = false, error = "Voice output failed", errorType = VoiceChatErrorType.TTS)
                                    }
                                }
                            }
                        }
                    )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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

<<<<<<< HEAD
    fun dismissCoinConfirmation() {
        _state.update { it.copy(showCoinConfirmation = false, pendingMessage = null) }
    }

    fun confirmAndSendMessage() {
        val pending = _state.value.pendingMessage
        sendMessage(text = pending, confirmed = true)
    }

    private fun saveMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.insertMessage(
=======
    private fun saveMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatMessageDao.insertMessage(
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
            chatRepo.deleteAllMessages()
=======
            chatMessageDao.deleteAllMessages()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
        val cleaned = GitaPromptEngine.cleanForVoice(text)
        _state.update { it.copy(isSpeaking = true) }
        voiceManager.speak(cleaned, flush = true) {
=======
        _state.update { it.copy(isSpeaking = true) }
        voiceManager.speak(text, flush = true) {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
        activeVerse = (gitaVerse ?: cachedVerse?.toGitaVerse())?.let { v ->
            VerseContext(
                chapter = v.chapterNo,
                verse = v.verseNo,
                sanskrit = v.verse,
                translation = v.translation,
                explanation = v.explanation
            )
        }
        Log.d(tag, "Verse set — chapter=${gitaVerse?.chapterNo ?: cachedVerse?.chapterNo} verse=${gitaVerse?.verseNo ?: cachedVerse?.verseNo}")
        aiScope.launch {
            voiceChatEngine.resetConversation()
            voiceChatEngine.updateSystemInstruction(
                GitaPromptEngine.gemmaSystemPrompt(activeVerse)
            )
        }
=======
        Log.d(tag, "Verse set — chapter=${gitaVerse?.chapterNo ?: cachedVerse?.chapterNo} verse=${gitaVerse?.verseNo ?: cachedVerse?.verseNo}")
        aiScope.launch { voiceChatEngine.resetConversation() }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }

    fun clearCurrentVerse() {
        currentCachedVerse = null
        currentGitaVerse   = null
<<<<<<< HEAD
        activeVerse = null
        aiScope.launch {
            voiceChatEngine.updateSystemInstruction(
                GitaPromptEngine.gemmaSystemPrompt(null)
            )
        }
=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
}
=======
}
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
