package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.database.CachedVerse
import com.aipoweredgita.app.database.VoiceChatMessage
import com.aipoweredgita.app.ml.AppFeature
import com.aipoweredgita.app.ml.LiteRtLmVoiceChatEngine
import com.aipoweredgita.app.ml.ModelAvailability
import com.aipoweredgita.app.utils.AiTurnManager
import com.aipoweredgita.app.utils.DeviceCapability
import com.aipoweredgita.app.utils.LanguageMode
import com.aipoweredgita.app.utils.VoiceManager
import com.aipoweredgita.app.ui.components.CoinAnimationManager
import com.aipoweredgita.app.ui.components.CoinEvent
import com.aipoweredgita.app.ui.components.CoinEventType
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.GitaApi
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.prompt.GitaPromptEngine
import com.aipoweredgita.app.prompt.VerseContext

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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.aipoweredgita.app.domain.model.ChatMessage
import com.aipoweredgita.app.domain.model.CoinError
import com.aipoweredgita.app.domain.model.VoiceChatErrorType
import com.aipoweredgita.app.domain.model.VoiceChatEvent
import com.aipoweredgita.app.domain.model.VoiceChatSideEffect
import com.aipoweredgita.app.domain.model.VoiceChatUiState
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class VoiceChatViewModel @Inject constructor(
    private val application: Application,
    private val chatRepo: com.aipoweredgita.app.repository.ChatRepository,
    private val summaryDao: com.aipoweredgita.app.database.ChatSummaryDao,
    private val statsRepository: StatsRepository,
    private val contentRepository: com.aipoweredgita.app.repository.ContentRepository
) : ViewModel() {

    private fun getCloudProxyName(): String {
        val provider = getAiProvider()
        return if (provider == "groq") "Groq (Cloud)" else "NVIDIA 70B (Cloud)"
    }

    private fun getAiProvider(): String {
        val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val model = prefs.getString("selected_ai_model", "Auto (Recommended)") ?: "Auto (Recommended)"
        return when {
            model.contains("NVIDIA", ignoreCase = true) -> "nvidia-basic"
            model.contains("Groq", ignoreCase = true) -> "groq"
            else -> "nvidia-basic"
        }
    }

    private val tag = "VoiceChatViewModel"

    private val _uiState = MutableStateFlow(VoiceChatUiState())
    val uiState: StateFlow<VoiceChatUiState> = _uiState.asStateFlow()

    private val _sideEffect = MutableSharedFlow<VoiceChatSideEffect>()
    val sideEffect: SharedFlow<VoiceChatSideEffect> = _sideEffect.asSharedFlow()

    // Backward compatibility for existing UI
    val state: StateFlow<VoiceChatUiState> = uiState

    private val voiceManager    = VoiceManager(application)
    private val voiceChatEngine = LiteRtLmVoiceChatEngine.getInstance(application)

    private var useProxy = false
    private val okHttpClient = GitaApi.sharedOkHttpClient

    private val SUMMARY_THRESHOLD = 20

    private val aiDispatcher = Dispatchers.Default.limitedParallelism(1)
    private lateinit var aiScope: CoroutineScope
    private val initMutex    = Mutex()

    private var startTime     = 0L
    private var lastUpdate    = 0L
    private var crashCount    = 0
    private var lastCrashTime = 0L
    private val MAX_CRASHES   = 3

    private val COOLDOWN_DURATION_SECONDS = 300
    private var cooldownJob: Job? = null

    private var currentLanguageMode : LanguageMode = LanguageMode.AUTO
    private var currentUserId = "guest"

    private fun getSessionKey(): String = "krishna-${currentUserId}-${java.time.LocalDate.now()}"

    // ─── Current Verse State ──────────────────────────────────────────────────
    private var currentCachedVerse : CachedVerse? = null
    private var currentGitaVerse   : GitaVerse?   = null
    private var activeVerse       : VerseContext? = null

    init {
        // aiScope is child of viewModelScope — cancels automatically on clearance
        aiScope = CoroutineScope(aiDispatcher + SupervisorJob(viewModelScope.coroutineContext[Job]))
        setupVoiceManagerErrorForwarding()
        loadMessages()
        observeModelChanges()
        observeUserStats()
        refreshModelStatus()
        viewModelScope.launch {
            statsRepository.coinBalance.collect { balance ->
                _uiState.update { it.copy(coinBalance = balance) }
            }
        }
        viewModelScope.launch {
            contentRepository.getActiveRecommendations().collect { recommendations ->
                val dynamicSuggestions = recommendations.take(4).map { r ->
                    mapRecommendationToSuggestion(r.recommendationTitle)
                }.toMutableList()
                
                val defaults = listOf("What is karma?", "Explain dharma", "How to find peace?", "What is Atman?")
                for (d in defaults) {
                    if (dynamicSuggestions.size >= 4) break
                    if (!dynamicSuggestions.contains(d)) {
                        dynamicSuggestions.add(d)
                    }
                }
                _uiState.update { it.copy(suggestions = dynamicSuggestions) }
            }
        }
        checkAndRestoreCooldown()
        checkAndRestoreDailyLimit()
    }

    private fun getTodayDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    fun checkAndRestoreDailyLimit() {
        val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val lastDate = prefs.getString("last_question_date", "") ?: ""
        val count = if (lastDate == today) {
            prefs.getInt("questions_asked_today_count", 0)
        } else {
            0
        }
        _uiState.update { it.copy(dailyQuestionsAsked = count) }
    }

    private fun incrementDailyQuestionCount(): Boolean {
        val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val today = getTodayDateString()
        val lastDate = prefs.getString("last_question_date", "") ?: ""
        var count = if (lastDate == today) {
            prefs.getInt("questions_asked_today_count", 0)
        } else {
            0
        }

        if (count >= 5) {
            return false
        }

        count++
        prefs.edit()
            .putString("last_question_date", today)
            .putInt("questions_asked_today_count", count)
            .apply()

        _uiState.update { it.copy(dailyQuestionsAsked = count) }
        return true
    }

    fun checkAndRestoreCooldown() {
        checkAndRestoreDailyLimit()
        val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        val lastTime = prefs.getLong("last_question_timestamp", 0L)
        if (lastTime > 0) {
            val elapsedSeconds = ((System.currentTimeMillis() - lastTime) / 1000).toInt()
            val remaining = COOLDOWN_DURATION_SECONDS - elapsedSeconds
            if (remaining > 0) {
                startCooldownTimer()
            } else {
                cooldownJob?.cancel()
                _uiState.update { it.copy(cooldownSeconds = 0) }
            }
        } else {
            _uiState.update { it.copy(cooldownSeconds = 0) }
        }
    }

    private fun recordQuestionSentAndStartCooldown() {
        val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().putLong("last_question_timestamp", System.currentTimeMillis()).apply()
        startCooldownTimer()
    }

    private fun startCooldownTimer() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            while (true) {
                val prefs = application.getSharedPreferences("app_prefs", android.content.Context.MODE_PRIVATE)
                val lastTime = prefs.getLong("last_question_timestamp", 0L)
                val elapsedSeconds = ((System.currentTimeMillis() - lastTime) / 1000).toInt()
                val remaining = maxOf(0, COOLDOWN_DURATION_SECONDS - elapsedSeconds)
                _uiState.update { it.copy(cooldownSeconds = remaining) }
                if (remaining <= 0) break
                kotlinx.coroutines.delay(1000L)
            }
        }
    }

    private fun mapRecommendationToSuggestion(title: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("strengthen ") -> {
                val topic = title.substringAfter("Strengthen ").trim()
                "Tell me about $topic"
            }
            lower.contains("review chapter ") -> {
                val ch = title.substringAfter("Review Chapter ").trim()
                "What is Chapter $ch about?"
            }
            lower.contains("yoga level") -> {
                "How to advance in Yoga?"
            }
            else -> title
        }
    }

    fun onEvent(event: VoiceChatEvent) {
        when (event) {
            is VoiceChatEvent.UpdateUserInput -> updateUserInput(event.input)
            is VoiceChatEvent.SendMessage -> sendMessage(event.text, null, null, event.confirmed)
            is VoiceChatEvent.DismissCoinConfirmation -> dismissCoinConfirmation()
            is VoiceChatEvent.ConfirmAndSendMessage -> confirmAndSendMessage()
            is VoiceChatEvent.ClearChat -> clearChat()
            is VoiceChatEvent.StartListening -> startListening()
            is VoiceChatEvent.StopListening -> stopListening()
            is VoiceChatEvent.StopAll -> stopAll()
            is VoiceChatEvent.ClearError -> clearError()
        }
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private fun observeUserStats() {
        viewModelScope.launch {
            statsRepository.getUserStatsFlow().collect { stats ->
                val uid = stats?.userId
                if (uid != null && uid.isNotEmpty()) {
                    currentUserId = uid
                    try {
                        val balance = statsRepository.getBalance()
                        _uiState.update { it.copy(coinBalance = balance, coinError = null, isBalanceLoaded = true) }
                    } catch (e: Exception) {
                        Log.e(tag, "Failed to fetch coin balance", e)
                        _uiState.update { it.copy(coinError = CoinError.NETWORK_ERROR, isBalanceLoaded = true) }
                    }
                } else {
                    try {
                        val balance = statsRepository.getBalance()
                        _uiState.update { it.copy(coinBalance = balance, coinError = null, isBalanceLoaded = true) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(isBalanceLoaded = true) }
                    }
                }
            }
        }
    }

    private fun setupVoiceManagerErrorForwarding() {
        voiceManager.onError = { errorMsg ->
            viewModelScope.launch(Dispatchers.Main) {
                _uiState.update { it.copy(error = errorMsg, errorType = VoiceChatErrorType.TTS) }
            }
        }
    }

    private fun observeModelChanges() {
        viewModelScope.launch {
            ModelAvailability.getInstance(application).selectedModel.collect { modelName ->
                Log.d(tag, "Model changed to $modelName — re-initializing")
                refreshModelStatus()
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch(Dispatchers.IO) {
            val dbMessages = chatRepo.getRecentMessages(limit = 20)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(messages = dbMessages.map { dbm ->
                        ChatMessage(dbm.id, dbm.text, dbm.isUser, dbm.timestamp)
                    })
                }
            }
        }
    }

     /**
      * Builds and sends the full message chain to the Deno proxy.
      * Server handles system prompt injection via app="gita".
      * We only send: verse context + history + user message.
      *
      * History window: 15 messages for natural flow.
      * Session summary is injected when older messages have been compressed.
      */
    private suspend fun fetchGroqReply(
        groundedPrompt: String,
        history: List<ChatMessage>,
        verseReference: String? = null
    ): String = withContext(Dispatchers.IO) {

        val messagesArray = JSONArray()

        // 1. Core System Instruction for Krishna respecting selected language mode
        val systemPrompt = GitaPromptEngine.groqSystemPrompt(activeVerse, currentLanguageMode)
        messagesArray.put(
            JSONObject()
                .put("role", "system")
                .put("content", systemPrompt)
        )

        // 2. Session summary from compressed older messages
        val chatSummary = summaryDao.getSummary(getSessionKey())
        if (chatSummary != null) {
            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", "Conversation summary:\n${chatSummary.summary}")
            )
        }

        // 3. Conversation history (keep recent 5 messages capped at 300 chars to prevent token throttling)
        history
            .filter { it.text.isNotEmpty() }
            .dropLast(1)
            .takeLast(5)
            .forEach { msg ->
                messagesArray.put(
                    JSONObject()
                        .put("role", if (msg.isUser) "user" else "assistant")
                        .put("content", msg.text.take(300))
                )
            }

        // 4. Current user question
        messagesArray.put(
            JSONObject()
                .put("role", "user")
                .put("content", groundedPrompt)
        )

        val body = JSONObject()
            .put("messages", messagesArray)
            .put("app", "gita")
            .put("provider", getAiProvider())
            .put("max_tokens", 300)
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(com.aipoweredgita.app.util.GitaConstants.VOICE_PROXY_URL)
            .post(body)
            .build()

        try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    Log.e(tag, "Proxy error: ${response.code} $errorBody")
                    throw java.io.IOException("Server error ${response.code}")
                }
                val responseString = response.body?.string()
                    ?: throw java.io.IOException("Empty response from server")
                val json = JSONObject(responseString)
                json.getString("reply")
            }
        } catch (e: java.net.SocketTimeoutException) {
            Log.e(tag, "Request timed out", e)
            throw java.io.IOException("Server is taking too long — try again or switch to Groq in Settings")
        }
    }

    /**
     * When the conversation exceeds SUMMARY_THRESHOLD, compresses the oldest
     * messages (those beyond the last 15) into a single summary via the Groq proxy.
     * The summary is stored in Room and injected as context in future requests.
     */
    private suspend fun summarizeOldMessages(): Boolean {
        val allMessages = chatRepo.getAllMessages()
        if (allMessages.size <= SUMMARY_THRESHOLD) return false

        val toSummarize = allMessages.dropLast(15) // keep newest 15
        if (toSummarize.isEmpty()) return false

        return try {
            val summary = callSummarizationProxy(toSummarize)
            if (summary.isNotBlank()) {
                val dao = summaryDao
                dao.setSummary(
                    com.aipoweredgita.app.database.ChatSummary(
                        sessionId = getSessionKey(),
                        summary = summary,
                        lastUpdated = System.currentTimeMillis()
                    )
                )
                // Remove summarized messages from Room
                val idsToDelete = toSummarize.map { it.id }
                // Delete one by one since DAO doesn't have batch delete by IDs
                idsToDelete.forEach { id ->
                    chatRepo.deleteMessageById(id)
                }
                Log.d(tag, "Summarized ${toSummarize.size} old messages")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.w(tag, "Failed to summarize old messages", e)
            // Non-critical — conversation continues without summary
            false
        }
    }

    private suspend fun callSummarizationProxy(messages: List<com.aipoweredgita.app.database.VoiceChatMessage>): String =
        withContext(Dispatchers.IO) {
            val bodyJson = JSONObject()
            val msgsArray = JSONArray()

            msgsArray.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", "Summarize this conversation between a user and Krishna (spiritual guide) in 2-3 sentences. Capture key topics, questions, and guidance given. Omit greetings and pleasantries.")
            )

            for (msg in messages) {
                msgsArray.put(
                    JSONObject()
                        .put("role", if (msg.isUser) "user" else "assistant")
                        .put("content", msg.text)
                )
            }

            bodyJson.put("messages", msgsArray)
            bodyJson.put("app", "gita")
            bodyJson.put("provider", getAiProvider())
            val body = bodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(com.aipoweredgita.app.util.GitaConstants.VOICE_PROXY_URL)
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw java.io.IOException("Summarization proxy error: $response")
                }
                val responseString = response.body?.string()
                    ?: throw java.io.IOException("Empty summary response")
                JSONObject(responseString).getString("reply")
            }
        }

    // ─── Model Init ───────────────────────────────────────────────────────────

    fun refreshModelStatus() {
        val context = application

        val ma = ModelAvailability.getInstance(context)
        val decision = ma.getRuntimeDecision(AppFeature.VOICE)

        if (decision.useProxy) {
            Log.d(tag, "Using proxy runtime for voice chat. tier=${decision.tierLabel} selected=${decision.selectedPreference}")
            useProxy = true
            _uiState.update {
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
                val maxTokens  = DeviceCapability.getOptimalMaxTokens(context, decision.displayName).let {
                    if (currentLanguageMode == com.aipoweredgita.app.utils.LanguageMode.TELUGU) (it * 0.6f).toInt() else it
                }
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

                _uiState.update { it.copy(currentModelName = decision.displayName) }

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
                                    GitaPromptEngine.gemmaSystemPrompt(activeVerse, currentLanguageMode)
                                )
                                crashCount = 0
                                useProxy = false
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
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
                                    _uiState.update {
                                        it.copy(
                                            isLlmReady = true,
                                            currentModelName = getCloudProxyName(),
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
                                _uiState.update {
                                    it.copy(
                                        isLlmReady = true,
                                        currentModelName = getCloudProxyName(),
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
                _uiState.update {
                    it.copy(
                        isLlmReady = true,
                        currentModelName = getCloudProxyName(),
                        error = null,
                        errorType = null
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to check model status, falling back to Deno proxy", e)
            useProxy = true
            _uiState.update {
                it.copy(
                    isLlmReady = true,
                    currentModelName = getCloudProxyName(),
                    error      = null,
                    errorType  = null
                )
            }
        }
    }

    // ─── Messaging ────────────────────────────────────────────────────────────

    fun updateUserInput(input: String) {
        _uiState.update { it.copy(userInput = input) }
    }

    fun sendMessage(
        text        : String?      = null,
        cachedVerse : CachedVerse? = null,
        gitaVerse   : GitaVerse?   = null,
        confirmed   : Boolean      = true
    ) {
        val messageText = text ?: _uiState.value.userInput
        if (messageText.isBlank()) return

        // 5 Questions Per Day Limit Check
        checkAndRestoreDailyLimit()
        if (_uiState.value.dailyQuestionsAsked >= 5) {
            _uiState.update {
                it.copy(
                    error = "Daily limit reached (5/5 questions asked today). Please come back tomorrow!",
                    errorType = VoiceChatErrorType.LLM_INFERENCE,
                    isThinking = false
                )
            }
            return
        }

        // 5-Minute Rate Limit Cooldown Check
        if (_uiState.value.cooldownSeconds > 0) {
            val mins = _uiState.value.cooldownSeconds / 60
            val secs = _uiState.value.cooldownSeconds % 60
            _uiState.update {
                it.copy(
                    error = "5-minute rate limit cooldown active. Please wait ${mins}m ${secs}s.",
                    errorType = VoiceChatErrorType.LLM_INFERENCE
                )
            }
            return
        }

        // Crash loop protection
        val now = System.currentTimeMillis()
        if (now - lastCrashTime > 60_000) crashCount = 0
        if (crashCount >= MAX_CRASHES) {
            _uiState.update {
                it.copy(
                    error      = "Voice chat crashed too many times. Please restart the app.",
                    errorType  = VoiceChatErrorType.CRASH_RECOVERY,
                    isThinking = false
                )
            }
            return
        }

        if (!incrementDailyQuestionCount()) {
            _uiState.update {
                it.copy(
                    error = "Daily limit reached (5/5 questions asked today). Please come back tomorrow!",
                    errorType = VoiceChatErrorType.LLM_INFERENCE,
                    isThinking = false
                )
            }
            return
        }

        if (text == null) _uiState.update { it.copy(userInput = "", error = null, errorType = null) }
        recordQuestionSentAndStartCooldown()

        // Check balance BEFORE adding user message or launching AI — prevents
        // the race where aiScope starts processing while balance check runs concurrently.
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val balance = statsRepository.getBalance()
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(coinBalance = balance, coinError = null, isBalanceLoaded = true) }
                }
                if (balance < 2) {
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                isThinking = false,
                                coinError = CoinError.NETWORK_ERROR,
                                error = "Insufficient coins to ask a question",
                                errorType = VoiceChatErrorType.LLM_INFERENCE
                            )
                        }
                    }
                    return@launch
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to check balance", e)
            }

            // Balance OK — now add user message and start AI
            withContext(Dispatchers.Main) {
                val userMessage = ChatMessage(text = messageText, isUser = true)
                _uiState.update { it.copy(messages = it.messages + userMessage, error = null, errorType = null, isThinking = true) }
                saveMessage(userMessage)
            }

            aiScope.launch {
                val aiMessageId = UUID.randomUUID().toString()

                // Add placeholder AI message for streaming
                withContext(Dispatchers.Main) {
                    _uiState.update { s ->
                        s.copy(messages = s.messages + ChatMessage(id = aiMessageId, text = "", isUser = false))
                    }
                }

                try {
                    AiTurnManager.mutex.withLock {
                        stopAll()

                        if (useProxy) {
                            val verseRef = activeVerse?.let { v ->
                                "[Chapter ${v.chapter}, Verse ${v.verse}]\nTranslation: ${v.translation}\nDepth: ${v.explanation}"
                            }
                            val reply = fetchGroqReply(
                                groundedPrompt = messageText,
                                history = _uiState.value.messages,
                                verseReference = verseRef
                            )
                            val finalAnswer = com.aipoweredgita.app.util.TextUtils.deepClean(reply)

                            // ✅ Spend coins ONLY after successful AI response
                            val spent = withContext(Dispatchers.IO) {
                                statsRepository.spendCoins(messageText)
                            }
                            if (!spent) {
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            isThinking = false,
                                            coinError = CoinError.NETWORK_ERROR,
                                            error = "Insufficient coins to ask a question",
                                            errorType = VoiceChatErrorType.LLM_INFERENCE,
                                            messages = it.messages.filter { m -> m.id != aiMessageId }
                                        )
                                    }
                                }
                                return@withLock
                            }

                            // Update balance after successful spend
                            try {
                                val balance = withContext(Dispatchers.IO) { statsRepository.getBalance() }
                                _uiState.update { it.copy(coinBalance = balance, coinError = null, isBalanceLoaded = true) }
                            } catch (e: Exception) {
                                Log.e(tag, "Failed to fetch balance after spend", e)
                            }

                            withContext(Dispatchers.Main) {
                                _uiState.update { s ->
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
                                    _uiState.update {
                                        it.copy(isSpeaking = false, error = "Voice output failed", errorType = VoiceChatErrorType.TTS)
                                    }
                                }
                            }
                            // Trigger session summarization for long conversations
                            summarizeOldMessages()
                        } else {
                            val recentHistory = _uiState.value.messages
                                .filter { it.text.isNotEmpty() }
                                .dropLast(1)
                                .takeLast(6)

                            val gemmaMessage = GitaPromptEngine.buildGemmaUserContent(
                                userMessage = messageText,
                                verse = activeVerse,
                                history = recentHistory
                            )
                            voiceChatEngine.sendMessage(
                                prompt    = gemmaMessage,
                                onPartial = { partial ->
                                    val nowMs = System.currentTimeMillis()
                                    if (nowMs - lastUpdate > 64) {
                                        lastUpdate = nowMs
                                        viewModelScope.launch(Dispatchers.Main) {
                                            _uiState.update { s ->
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

                                        // ✅ Spend coins ONLY after successful on-device response
                                        val spent = withContext(Dispatchers.IO) {
                                            statsRepository.spendCoins(messageText)
                                        }
                                        if (!spent) {
                                            withContext(Dispatchers.Main) {
                                                _uiState.update {
                                                    it.copy(
                                                        isThinking = false,
                                                        coinError = CoinError.NETWORK_ERROR,
                                                        error = "Insufficient coins to ask a question",
                                                        errorType = VoiceChatErrorType.LLM_INFERENCE,
                                                        messages = it.messages.filter { m -> m.id != aiMessageId }
                                                    )
                                                }
                                            }
                                            return@launch
                                        }

                                        // Update balance after successful spend
                                        try {
                                            val balance = withContext(Dispatchers.IO) { statsRepository.getBalance() }
                                            _uiState.update { it.copy(coinBalance = balance, coinError = null, isBalanceLoaded = true) }
                                        } catch (e: Exception) {
                                            Log.e(tag, "Failed to fetch balance after spend", e)
                                        }

                                        withContext(Dispatchers.Main) {
                                            _uiState.update { s ->
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
                                                _uiState.update {
                                                    it.copy(isSpeaking = false, error = "Voice output failed", errorType = VoiceChatErrorType.TTS)
                                                }
                                            }
                                            if (summarizeOldMessages()) {
                                                voiceChatEngine.resetConversation()
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
                        _uiState.update {
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
                        _uiState.update { if (it.isThinking) it.copy(isThinking = false) else it }
                    }
                }
            }
        }
    }

    fun dismissCoinConfirmation() {
        _uiState.update { it.copy(showCoinConfirmation = false, pendingMessage = null) }
    }

    fun confirmAndSendMessage() {
        val pending = _uiState.value.pendingMessage
        sendMessage(text = pending, confirmed = true)
    }

    private fun saveMessage(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            chatRepo.insertMessage(
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
            chatRepo.deleteAllMessages()
            summaryDao.deleteSummary(getSessionKey())
            voiceChatEngine.resetConversation()
            withContext(Dispatchers.Main) {
                _uiState.update { it.copy(messages = emptyList()) }
            }
        }
    }

    fun startListening() {
        stopAll()
        _uiState.update { it.copy(isSpeaking = false, isListening = true, liveTranscript = "", audioLevel = 0.1f, error = null, errorType = null) }
        try {
            voiceManager.startListening(
                onResult        = { result ->
                    _uiState.update { it.copy(isListening = false, audioLevel = 0f, liveTranscript = "") }
                    if (result.isNotBlank()) sendMessage(
                        text        = result,
                        cachedVerse = currentCachedVerse,
                        gitaVerse   = currentGitaVerse
                    )
                },
                onPartialResult = { partial ->
                    _uiState.update { it.copy(liveTranscript = partial) }
                },
                onError         = { err ->
                    _uiState.update { it.copy(isListening = false, audioLevel = 0f, error = err, errorType = VoiceChatErrorType.STT) }
                },
                onRmsChanged    = { level ->
                    _uiState.update { it.copy(audioLevel = level) }
                }
            )
        } catch (e: Exception) {
            Log.e(tag, "Failed to start listening", e)
            _uiState.update { it.copy(isListening = false, audioLevel = 0f, error = "Failed to start voice input", errorType = VoiceChatErrorType.STT) }
        }
    }

    fun stopListening() {
        voiceManager.stopListening()
        _uiState.update { it.copy(isListening = false, audioLevel = 0f) }
    }

    private fun speakResponse(text: String) {
        val cleaned = GitaPromptEngine.cleanForVoice(text)
        _uiState.update { it.copy(isSpeaking = true) }
        voiceManager.speak(cleaned, flush = true) {
            _uiState.update { it.copy(isSpeaking = false) }
        }
    }

    fun stopAll() {
        try { voiceChatEngine.stopResponse() } catch (_: Exception) {}
        try { voiceManager.stopSpeaking() }    catch (_: Exception) {}
        try { voiceManager.stopListening() }   catch (_: Exception) {}
        _uiState.update { it.copy(isSpeaking = false, isListening = false, isThinking = false) }
    }

    fun clearError()    { _uiState.update { it.copy(error = null, errorType = null) } }
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
                GitaPromptEngine.gemmaSystemPrompt(activeVerse, currentLanguageMode)
            )
        }
    }

    fun clearCurrentVerse() {
        currentCachedVerse = null
        currentGitaVerse   = null
        activeVerse = null
        aiScope.launch {
            voiceChatEngine.updateSystemInstruction(
                GitaPromptEngine.gemmaSystemPrompt(null, currentLanguageMode)
            )
        }
    }

    // ─── Language Mode ────────────────────────────────────────────────────────

    fun setLanguageMode(mode: LanguageMode) {
        currentLanguageMode = mode
        _uiState.update { it.copy(currentLanguageMode = mode) }
        voiceManager.setLocale(mode.sttLocale, mode.ttsLocale)
        // Refresh model to restore/recalculate token window based on language and update instruction
        refreshModelStatus()
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
