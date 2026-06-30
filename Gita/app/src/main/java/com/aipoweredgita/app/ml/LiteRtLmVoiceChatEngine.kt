package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Backend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * PRODUCTION SAFE LiteRT-LM Voice Chat Engine.
 * Supports both Gemma and Qwen models with model-aware formatting and cleaning.
 */
class LiteRtLmVoiceChatEngine(private val context: Context) {

    private val engineMutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var cleanerScope = CoroutineScope(Dispatchers.Default + Job())

    private var isInitialized = false
    private var modelPath: String? = null
    private var currentSystemInstruction: String? = null

    // ─── Adaptive device config ───────────────────────────────────────────────
    private var timeoutMs     : Long = 240_000L
    private var currentSampler: com.google.ai.edge.litertlm.SamplerConfig = SAMPLER

    companion object {
        private const val TAG = "LiteRtLmVoiceChat"
        private const val MAX_PROMPT_CHARS = 1500  // keep prompt short, leave room for output

        private val SAMPLER = SamplerConfig(
            topK        = 40,   // wider sampling
            topP        = 0.95,
            temperature = 0.7   // higher = less early stopping
        )

        private const val DEFAULT_INSTRUCTION =
            "You are Krishna from the Bhagavad Gita. Answer clearly."

        @Volatile
        private var instance: LiteRtLmVoiceChatEngine? = null

        fun getInstance(context: Context): LiteRtLmVoiceChatEngine {
            return instance ?: synchronized(this) {
                instance ?: LiteRtLmVoiceChatEngine(context.applicationContext).also { instance = it }
            }
        }
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    suspend fun initialize(
        path      : String,
        maxTokens : Int? = null,
        timeoutMs : Long = 240_000L,
        sampler   : com.google.ai.edge.litertlm.SamplerConfig = SAMPLER
    ): Boolean = engineMutex.withLock {
            return@withLock try {
                closeInternal()
                cleanerScope       = CoroutineScope(Dispatchers.Default + Job())
                this.timeoutMs     = timeoutMs
                this.currentSampler = sampler

                val resolvedMaxTokens = maxTokens ?: com.aipoweredgita.app.utils.DeviceCapability.getOptimalMaxTokens(context, path)

                val backend = if (com.aipoweredgita.app.utils.DeviceTierDetector.hasVulkan(context)) Backend.GPU() else Backend.CPU()
                val newEngine = Engine(
                    EngineConfig(
                        modelPath    = path,
                        maxNumTokens = resolvedMaxTokens,
                        backend      = backend
                    )
                )
                newEngine.initialize()

                engine       = newEngine
                conversation = newEngine.createConversation(ConversationConfig(samplerConfig = sampler))
                isInitialized = true
                modelPath     = path
                Log.d(TAG, "LiteRT-LM initialized ($resolvedMaxTokens tokens, timeout=${timeoutMs}ms)")
                true
            } catch (e: Exception) {
                Log.e(TAG, "LiteRT-LM init failed", e)
                false
            }
        }

    // ─── Send Message ─────────────────────────────────────────────────────────

    suspend fun sendMessage(
        prompt: String,
        onPartial: ((String) -> Unit)? = null,
        onCleaned: ((String) -> Unit)? = null
    ): String = engineMutex.withLock {

        if (!isInitialized || conversation == null) {
            return@withLock "Error: Engine not initialized"
        }

        val systemInstruction = currentSystemInstruction ?: DEFAULT_INSTRUCTION

        val isQwen = modelPath?.contains("qwen", ignoreCase = true) == true
        val formattedPrompt = buildString {
            if (isQwen) {
                append("<|im_start|>system\n")
                append(systemInstruction.trim())
                append("<|im_end|>\n")
                append("<|im_start|>user\n")
                append(prompt.trim().take(MAX_PROMPT_CHARS))
                append("<|im_end|>\n")
                append("<|im_start|>assistant\n")
            } else {
                // Gemma 4 chat template
                append("<start_of_turn>system\n")
                append(systemInstruction.trim())
                append("<end_of_turn>\n")
                append("<start_of_turn>user\n")
                append(prompt.trim().take(MAX_PROMPT_CHARS))
                append("<end_of_turn>\n")
                append("<start_of_turn>model\n")
            }
        }

        val responseBuffer = StringBuilder()
        val answerBuffer   = StringBuilder()
        var inThinkingBlock = false
        var lastSentLength  = 0

        return@withLock try {
            val completed = withTimeoutOrNull(timeoutMs) {
                conversation!!.sendMessageAsync(formattedPrompt)
                    .catch { throw it }
                    .collect { message ->
                        val chunk = message.toString()
                        if (chunk.isEmpty()) return@collect

                        Log.v(TAG, "Chunk: '$chunk'")
                        responseBuffer.append(chunk)

                        val hasThinkStart = chunk.contains("<think>") || chunk.contains("<|think")  || chunk.contains("<thought>")
                        val hasThinkEnd   = chunk.contains("</think>") || chunk.contains("<|think_end") || chunk.contains("</thought>")

                        when {
                            hasThinkEnd -> {
                                inThinkingBlock = false
                                val endTags = listOf("</think>", "</thought>", "<|think_end|>")
                                var endPos = -1
                                for (tag in endTags) {
                                    val idx = chunk.lastIndexOf(tag)
                                    if (idx != -1) endPos = maxOf(endPos, idx + tag.length)
                                }
                                val afterThink = if (endPos != -1) chunk.substring(endPos) else chunk
                                emitClean(afterThink, answerBuffer, onPartial, lastSentLength)
                                    .also { lastSentLength = it }
                            }
                            inThinkingBlock -> {
                                // skip — inside thinking block
                            }
                            hasThinkStart -> {
                                inThinkingBlock = true
                                val startPos = listOf("<think>", "<|think", "<thought>")
                                    .mapNotNull { tag -> chunk.indexOf(tag).takeIf { it != -1 } }
                                    .minOrNull() ?: chunk.length
                                emitClean(chunk.substring(0, startPos), answerBuffer, onPartial, lastSentLength)
                                    .also { lastSentLength = it }
                            }
                            else -> {
                                emitClean(chunk, answerBuffer, onPartial, lastSentLength)
                                    .also { lastSentLength = it }
                            }
                        }
                    }
            }

            if (completed == null) {
                Log.w(TAG, "Generation timed out")
                stopResponse()
                onPartial?.invoke(" (response was cut short)")
            }

            val rawFinal = responseBuffer.toString()
            Log.d(TAG, "RAW:\n$rawFinal")

            // Extract answer after last thinking marker
            val markers = listOf("</think>", "</thought>", "<turn|>", "<|turn>", "<end_of_turn>", "<start_of_turn>")
            var lastMarker = -1
            for (m in markers) {
                val idx = rawFinal.lastIndexOf(m)
                if (idx != -1) lastMarker = maxOf(lastMarker, idx + m.length)
            }

            val extracted = when {
                lastMarker != -1          -> rawFinal.substring(lastMarker)
                answerBuffer.isNotEmpty() -> answerBuffer.toString()
                else                      -> rawFinal
            }

            val basicCleaned = com.aipoweredgita.app.util.TextUtils.cleanLlmOutput(extracted)
            Log.d(TAG, "CLEANED:\n$basicCleaned")

            // Deep clean on background thread, callback on Main
            cleanerScope.launch {
                try {
                    val deepCleaned = com.aipoweredgita.app.util.TextUtils.deepClean(basicCleaned)
                    withContext(Dispatchers.Main) {
                        onCleaned?.invoke(deepCleaned)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Deep clean failed", e)
                    withContext(Dispatchers.Main) {
                        onCleaned?.invoke(basicCleaned) // fallback to basic
                    }
                }
            }

            basicCleaned
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed, recovering", e)
            recoverModel()
            "Error: ${e.message}"
        }
    }

    /** Cleans a chunk and emits via onPartial. Returns updated lastSentLength. */
    private fun emitClean(
        chunk: String,
        buffer: StringBuilder,
        onPartial: ((String) -> Unit)?,
        lastSentLength: Int
    ): Int {
        val cleaned = cleanChunk(chunk)
        if (cleaned.isEmpty()) return lastSentLength
        buffer.append(cleaned)
        val current = buffer.toString()
        return if (current.length > lastSentLength) {
            onPartial?.invoke(current)
            current.length
        } else lastSentLength
    }

    private fun cleanChunk(chunk: String): String = chunk
        .replace(Regex("<think>.*?</think>",    RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<thought>.*?</thought>", RegexOption.DOT_MATCHES_ALL), "")
        .replace("<think>", "").replace("</think>", "")
        .replace("<thought>", "").replace("</thought>", "")
        .replace("<start_of_turn>", "").replace("<end_of_turn>", "")
        .replace("<|turn>", "").replace("<turn|>", "")
        .replace("<|im_start|>", "").replace("<|im_end|>", "")
        .replace("system\n", "").replace("assistant\n", "")
        .replace("model\n", "").replace("user\n", "")
        .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]"), "")
        .replace(Regex("[ \t]{2,}"), " ")
        .replace(Regex("\n{3,}"), "\n\n")

    // ─── Controls ─────────────────────────────────────────────────────────────

    suspend fun updateSampler(sampler: com.google.ai.edge.litertlm.SamplerConfig) {
        engineMutex.withLock {
            currentSampler = sampler
            try {
                conversation?.close()
                conversation = engine?.createConversation(ConversationConfig(samplerConfig = sampler))
                Log.d(TAG, "Sampler updated: topK=${sampler.topK} temp=${sampler.temperature}")
            } catch (_: Exception) {}
        }
    }

    fun stopResponse() {
        try { conversation?.cancelProcess() } catch (_: Exception) {}
    }

    suspend fun updateSystemInstruction(instruction: String) {
        currentSystemInstruction = instruction
    }

    suspend fun resetConversation() {
        engineMutex.withLock {
            try {
                conversation?.close()
                conversation = engine?.createConversation(ConversationConfig(samplerConfig = currentSampler))
            } catch (_: Exception) {}
        }
    }

    private suspend fun recoverModel() {
        Log.w(TAG, "Recovering model...")
        modelPath?.let { initialize(it) }
    }

    /**
     * Non-blocking close — dispatches cleanup to engine scope.
     * Caller must NOT rely on immediate completion; use [closeBlocking] if needed.
     */
    fun close() {
        cleanerScope.launch {
            engineMutex.withLock { closeInternal() }
        }
    }

    /** Blocking close for lifecycle-critical paths only (e.g. onCleared on IO thread). */
    fun closeBlocking() {
        runBlocking { engineMutex.withLock { closeInternal() } }
    }

    private fun closeInternal() {
        try { cleanerScope.cancel() } catch (_: Exception) {}
        try { conversation?.close() } catch (_: Exception) {}
        try { engine?.close() } catch (_: Exception) {}
        conversation  = null
        engine        = null
        isInitialized = false
    }
}