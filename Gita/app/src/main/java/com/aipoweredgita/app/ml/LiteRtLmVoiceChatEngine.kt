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
 * Fixed: Correct Gemma turn tokens, tight sampler config, thinking block stripping.
 */
class LiteRtLmVoiceChatEngine(private val context: Context) {

    private val engineMutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var cleanerScope = CoroutineScope(Dispatchers.Default + Job())

    private var isInitialized = false
    private var modelPath: String? = null
    private var currentSystemInstruction: String? = null
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
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    suspend fun initialize(path: String, maxTokens: Int = MAX_TOKENS): Boolean =
        engineMutex.withLock {
            return@withLock try {
                closeInternal()
                cleanerScope = CoroutineScope(Dispatchers.Default + Job())

                val newEngine = Engine(
                    EngineConfig(
                        modelPath    = path,
                        maxNumTokens = maxTokens,
                        backend      = Backend.CPU()
                    )
                )
                newEngine.initialize()

                engine       = newEngine
                conversation = newEngine.createConversation(ConversationConfig(samplerConfig = SAMPLER))
                isInitialized = true
                modelPath     = path
                Log.d(TAG, "LiteRT-LM initialized ($maxTokens tokens)")
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
                        }
                    }
            }

            if (completed == null) {
                Log.w(TAG, "Generation timed out")
                stopResponse()
                onPartial?.invoke(" (response was cut short)")
            }

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
            var lastMarker = -1
            for (m in markers) {
                val idx = rawFinal.lastIndexOf(m)
                if (idx != -1) lastMarker = maxOf(lastMarker, idx + m.length)
            }

            val extracted = if (lastMarker != -1) rawFinal.substring(lastMarker) else rawFinal

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

    fun stopResponse() {
        try { conversation?.cancelProcess() } catch (_: Exception) {}
    }

    fun updateMode(mode: LanguageMode) {
        currentMode = mode
    }

    suspend fun updateSystemInstruction(instruction: String) {
        currentSystemInstruction = instruction
    }

    suspend fun resetConversation() {
        engineMutex.withLock {
            try {
                conversation?.close()
                conversation = engine?.createConversation(ConversationConfig(samplerConfig = SAMPLER))
            } catch (_: Exception) {}
        }
    }

    private suspend fun recoverModel() {
        Log.w(TAG, "Recovering model...")
        modelPath?.let { initialize(it) }
    }

    fun close() {
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