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
<<<<<<< HEAD
 * Supports both Gemma and Qwen models with model-aware formatting and cleaning.
=======
 * Fixed: Correct Gemma turn tokens, tight sampler config, thinking block stripping.
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
 */
class LiteRtLmVoiceChatEngine(private val context: Context) {

    private val engineMutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var cleanerScope = CoroutineScope(Dispatchers.Default + Job())

    private var isInitialized = false
    private var modelPath: String? = null
    private var currentSystemInstruction: String? = null
<<<<<<< HEAD

    // ─── Adaptive device config ───────────────────────────────────────────────
    private var timeoutMs     : Long = 240_000L
    private var currentSampler: com.google.ai.edge.litertlm.SamplerConfig = SAMPLER

    companion object {
        private const val TAG = "LiteRtLmVoiceChat"
        private const val MAX_TOKENS = 2048  // Qwen3 0.6B works better with smaller window
        private const val MAX_PROMPT_CHARS = 800  // keep prompt short, leave room for output

        private val SAMPLER = SamplerConfig(
            topK        = 40,   // wider sampling
            topP        = 0.95,
            temperature = 0.7   // higher = less early stopping
        )

        private const val DEFAULT_INSTRUCTION =
            "You are Krishna from the Bhagavad Gita. Answer clearly."
=======
    var currentMode: LanguageMode = LanguageMode.TELUGU

    companion object {
        private const val TAG = "LiteRtLmVoiceChat"
        private const val MAX_TOKENS = 16384
        private const val MAX_PROMPT_CHARS = 4096

        // ✅ Tight sampler — prevents hallucination on 2B model
        private val SAMPLER = SamplerConfig(
            topK        = 10,
            topP        = 0.85,
            temperature = 0.2
        )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

<<<<<<< HEAD
    suspend fun initialize(
        path      : String,
        maxTokens : Int  = MAX_TOKENS,
        timeoutMs : Long = 240_000L,
        sampler   : com.google.ai.edge.litertlm.SamplerConfig = SAMPLER
    ): Boolean = engineMutex.withLock {
            return@withLock try {
                closeInternal()
                cleanerScope       = CoroutineScope(Dispatchers.Default + Job())
                this.timeoutMs     = timeoutMs
                this.currentSampler = sampler
=======
    suspend fun initialize(path: String, maxTokens: Int = MAX_TOKENS): Boolean =
        engineMutex.withLock {
            return@withLock try {
                closeInternal()
                cleanerScope = CoroutineScope(Dispatchers.Default + Job())
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

                val newEngine = Engine(
                    EngineConfig(
                        modelPath    = path,
                        maxNumTokens = maxTokens,
                        backend      = Backend.CPU()
                    )
                )
                newEngine.initialize()

                engine       = newEngine
<<<<<<< HEAD
                conversation = newEngine.createConversation(ConversationConfig(samplerConfig = sampler))
                isInitialized = true
                modelPath     = path
                Log.d(TAG, "LiteRT-LM initialized ($maxTokens tokens, timeout=${timeoutMs}ms)")
=======
                conversation = newEngine.createConversation(ConversationConfig(samplerConfig = SAMPLER))
                isInitialized = true
                modelPath     = path
                Log.d(TAG, "LiteRT-LM initialized ($maxTokens tokens)")
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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

<<<<<<< HEAD
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
                append("<|turn>system\n")
                append(systemInstruction.trim())
                append("<turn|>\n")
                append("<|turn>user\n")
                append(prompt.trim().take(MAX_PROMPT_CHARS))
                append("<turn|>\n")
                append("<|turn>model\n")
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
=======
        val systemInstruction = currentSystemInstruction ?: currentMode.systemInstruction

        // ✅ Gemma 4 exact turn format: <|turn>role … <turn|>
        val formattedPrompt = buildPrompt(currentMode, prompt.take(MAX_PROMPT_CHARS), systemInstruction)

        val responseBuffer = StringBuilder()
        val thinkFilter    = ThinkBlockFilter()
        val accumulator    = ChunkAccumulator()

        return@withLock try {
            val completed = withTimeoutOrNull(240_000) {
                conversation!!.sendMessageAsync(formattedPrompt)
                    .catch { throw it }
                    .collect { message ->
                        val raw = message.toString()
                        if (raw.isEmpty()) return@collect

                        Log.v(TAG, "RAW_CHUNK: '$raw'")
                        responseBuffer.append(raw)

                        // ✅ Filter think-blocks, then accumulate raw — do NOT clean yet
                        val filtered = thinkFilter.process(raw)
                        Log.v(TAG, "FILTERED: '$filtered'")

                        accumulator.feed(filtered)?.let { phrase ->
                            Log.d(TAG, "EMIT: '$phrase'")
                            onPartial?.invoke(phrase)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
                        }
                    }
            }

            if (completed == null) {
                Log.w(TAG, "Generation timed out")
                stopResponse()
                onPartial?.invoke(" (response was cut short)")
            }

<<<<<<< HEAD
            val rawFinal = responseBuffer.toString()
            Log.d(TAG, "RAW:\n$rawFinal")

            // Extract answer after last thinking marker
            val markers = listOf("</think>", "</thought>", "<turn|>", "<|turn>", "<end_of_turn>", "<start_of_turn>")
=======
            // ✅ Flush any remaining buffered text
            accumulator.flush()?.let { phrase ->
                Log.d(TAG, "FLUSH: '$phrase'")
                onPartial?.invoke(phrase)
            }

            val rawFinal = responseBuffer.toString()
            Log.d(TAG, "RAW:\n$rawFinal")

            // Extract answer after last thinking/turn marker (Gemma 4 + legacy)
            val markers = listOf(
                "<|/thinking>", "<|/thought>",          // Gemma 4
                "</think>", "</thought>", "<|think_end|>", // legacy
                "<turn|>", "<end_of_turn>"               // turn tokens
            )
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            var lastMarker = -1
            for (m in markers) {
                val idx = rawFinal.lastIndexOf(m)
                if (idx != -1) lastMarker = maxOf(lastMarker, idx + m.length)
            }

<<<<<<< HEAD
            val extracted = when {
                lastMarker != -1          -> rawFinal.substring(lastMarker)
                answerBuffer.isNotEmpty() -> answerBuffer.toString()
                else                      -> rawFinal
            }
=======
            val extracted = if (lastMarker != -1) rawFinal.substring(lastMarker) else rawFinal
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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

<<<<<<< HEAD
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

=======
    /**
     * Strips think-block tokens from a streaming chunk — stateful across chunks.
     *
     * Priority order: Gemma 4 tokens first, legacy fallbacks last.
     * Each pair is tracked independently via filterPair() so the filter
     * is robust even when open/close tags arrive in different chunks.
     */
    private inner class ThinkBlockFilter {
        // Ordered: Gemma 4 tokens first, legacy fallback last
        private val opens  = arrayOf("<|thinking>", "<|thought>", "<think>",  "<thought>")
        private val closes = arrayOf("<|/thinking>","<|/thought>","</think>", "</thought>")
        private val inside = BooleanArray(opens.size)  // per-pair state

        fun process(chunk: String): String {
            var result = chunk
            for (i in opens.indices) result = filterPair(result, i)
            return result
        }

        /** Removes content between opens[idx]…closes[idx], preserving state across calls. */
        private fun filterPair(chunk: String, pairIdx: Int): String {
            val open  = opens[pairIdx]
            val close = closes[pairIdx]
            val sb    = StringBuilder(chunk.length)
            var i     = 0
            var inBlk = inside[pairIdx]
            while (i < chunk.length) {
                when {
                    !inBlk && chunk.startsWith(open,  i) -> { inBlk = true;  i += open.length  }
                    inBlk  && chunk.startsWith(close, i) -> { inBlk = false; i += close.length }
                    inBlk  -> i++
                    else   -> { sb.append(chunk[i]); i++ }
                }
            }
            inside[pairIdx] = inBlk
            return sb.toString()
        }

        fun reset() { inside.fill(false) }
    }

    /**
     * Accumulates filtered chunks and emits cleaned phrases at sentence/word
     * boundaries.
     *
     * KEY FIX: raw filtered text is appended WITHOUT cleaning so that leading
     * spaces (' క', ' the', …) — the inter-word separators from the model
     * tokeniser — are preserved inside the buffer.  cleanChunk() is called
     * only once, on the complete accumulated phrase, just before emitting.
     */
    private inner class ChunkAccumulator {
        private val buffer = StringBuilder()

        /** Returns a cleaned phrase ready for TTS, or null if no boundary yet. */
        fun feed(filtered: String): String? {
            // ✅ Append BEFORE cleaning — preserve inter-word spaces
            buffer.append(filtered)

            val text        = buffer.toString()
            val boundaryIdx = lastBoundary(text)
            if (boundaryIdx == -1) return null

            val toEmit = text.substring(0, boundaryIdx + 1)
            buffer.delete(0, boundaryIdx + 1)

            Log.d(TAG, "RAW_PHRASE: $toEmit")
            val cleaned = cleanChunk(toEmit, LanguageMode.TELUGU)
            Log.d(TAG, "CLEANED_PHRASE: $cleaned")
            return cleaned.ifBlank { null }
        }

        /** Call after generation ends to emit any leftover text. */
        fun flush(): String? {
            if (buffer.isEmpty()) return null
            val cleaned = cleanChunk(buffer.toString(), LanguageMode.TELUGU)
            buffer.clear()
            return cleaned.ifBlank { null }
        }

        fun reset() { buffer.clear() }

        private fun lastBoundary(text: String): Int {
            val hardBoundaries = charArrayOf('।', '॥', '.', '!', '?', '\n')
            for (i in text.indices.reversed()) {
                if (text[i] in hardBoundaries) return i
            }
            // Soft boundary: break on last space if buffer is getting large
            if (text.length > 80) {
                for (i in text.indices.reversed()) {
                    if (text[i] == ' ') return i
                }
            }
            return -1
        }
    }
    private fun addScriptBoundarySpacing(text: String): String {
        if (text.isBlank()) return text

        val result = StringBuilder(text.length + 16)
        var prevScript = Script.OTHER

        for (i in text.indices) {
            val ch = text[i]
            val currScript = ch.script()

            // Insert space at Telugu→English or English→Telugu boundary
            // only if there isn't already a space
            if (prevScript != Script.OTHER && currScript != Script.OTHER
                && prevScript != currScript
                && result.isNotEmpty()
                && result.last() != ' '
            ) {
                result.append(' ')
            }

            result.append(ch)
            if (!ch.isWhitespace()) prevScript = currScript
        }

        return result.toString()
    }

    private enum class Script { TELUGU, ENGLISH, OTHER }

    /** Declared mode of the current generation — drives system prompt and clean rules. */
    enum class LanguageMode(val systemInstruction: String) {
        AUTO(
            "You are Krishna from the Bhagavad Gita. " +
            "Speak in clear, human-like sentences with proper spacing and punctuation. " +
            "If the user writes in Telugu, respond in Telugu. Otherwise respond in English. " +
            "Never repeat or hallucinate verse text. Only explain what is given to you."
        ),
        TELUGU(
            "మీరు భగవద్గీత నుండి కృష్ణుడు. " +
            "స్పష్టమైన, మానవీయ తెలుగు వాక్యాలలో మాట్లాడండి. " +
            "సరైన అంతర విరామాలు మరియు విరామచిహ్నాలు వాడండి. " +
            "శ్లోక పాఠాన్ని మళ్ళీ చెప్పవద్దు. ఇచ్చిన వాటినే వివరించండి."
        ),
        ENGLISH(
            "You are Krishna from the Bhagavad Gita. " +
            "Always respond in clear, fluent English. " +
            "Use proper spacing and punctuation. " +
            "Never repeat or hallucinate verse text. Only explain what is given to you."
        )
    }

    private fun Char.script(): Script = when {
        this in '\u0C00'..'\u0C7F' -> Script.TELUGU
        this in 'a'..'z' || this in 'A'..'Z' -> Script.ENGLISH
        else -> Script.OTHER
    }

    private fun cleanChunk(chunk: String, mode: LanguageMode = LanguageMode.AUTO): String {
        Log.d("CleanChunk", "MODE: $mode  IN: '$chunk'")

        if (chunk.isBlank()) return ""

        val result = chunk
            // 1. Special tokens — Gemma 4 first, legacy fallback
            .replace(Regex("<\\|thinking>.*?<\\|/thinking>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<\\|thought>.*?<\\|/thought>",   RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<thought>.*?</thought>",         RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<\\|think.*?\\|>",               RegexOption.DOT_MATCHES_ALL), "")
            .replace("<|thinking>",    "").replace("<|/thinking>", "")
            .replace("<|thought>",     "").replace("<|/thought>",  "")
            .replace("<thought>",      "").replace("</thought>",   "")
            .replace("<|think|>",      "").replace("<|think_end|>","")
            .replace("<|turn>",        "").replace("<turn|>",      "")
            .replace("<start_of_turn>","").replace("<end_of_turn>","")
            .replace(Regex("<\\|[^>]+\\|>"), "")

            // 2. Invisible Unicode
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]"), "")

            // 3. Strip characters that aren't Telugu, English, digits, or punctuation
            //    Catches Korean/Japanese/Arabic bleed-through from the model
            .replace(Regex("[^\\u0C00-\\u0C7F\\u0000-\\u007F\\s]"), "")

            // 4. Leading junk before Telugu
            .replace(Regex("^[\\W\\d]+(?=[\\u0C00-\\u0C7F])"), "")

            // 5. Script boundary spacing (handles pure Telugu, pure English,
            //    and mixed — no regex, character-by-character)
            .let { addScriptBoundarySpacing(it) }

            // 6. Whitespace normalisation
            .replace(Regex("[ \t]{2,}"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trimEnd()

        Log.d("CleanChunk", "OUT: '$result'")
        return result
    }

    // ─── Prompt Builder ────────────────────────────────────────────────────────

    /**
     * Builds a Gemma 4-compatible prompt using <|turn> / <turn|> tokens.
     * [systemOverride] lets callers supply a one-off instruction without
     * changing [currentMode].
     */
    private fun buildPrompt(
        mode: LanguageMode,
        userMessage: String,
        systemOverride: String? = null
    ): String {
        val sys = systemOverride ?: mode.systemInstruction
        return "<|turn>system\n$sys<turn|>\n" +
               "<|turn>user\n$userMessage<turn|>\n" +
               "<|turn>model\n"
    }

    // ─── Controls ─────────────────────────────────────────────────────────────

>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    fun stopResponse() {
        try { conversation?.cancelProcess() } catch (_: Exception) {}
    }

<<<<<<< HEAD
=======
    fun updateMode(mode: LanguageMode) {
        currentMode = mode
    }

>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    suspend fun updateSystemInstruction(instruction: String) {
        currentSystemInstruction = instruction
    }

    suspend fun resetConversation() {
        engineMutex.withLock {
            try {
                conversation?.close()
<<<<<<< HEAD
                conversation = engine?.createConversation(ConversationConfig(samplerConfig = currentSampler))
=======
                conversation = engine?.createConversation(ConversationConfig(samplerConfig = SAMPLER))
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
            } catch (_: Exception) {}
        }
    }

    private suspend fun recoverModel() {
        Log.w(TAG, "Recovering model...")
        modelPath?.let { initialize(it) }
    }

<<<<<<< HEAD
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
=======
    fun close() {
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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