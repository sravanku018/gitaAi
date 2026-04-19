package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Backend
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

/**
 * PRODUCTION SAFE LiteRT-LM Voice Chat Engine.
 * Incorporates Mutex locking, Prompt limits, and Auto-recovery.
 */
class LiteRtLmVoiceChatEngine(private val context: Context) {

    private val engineMutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    // Dedicated scope for background cleaning tasks
    private var cleanerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.Job())

    private var isInitialized = false
    private var modelPath: String? = null
    private var currentSystemInstruction: String? = null

    companion object {
        private const val TAG = "LiteRtLmVoiceChat"

        // Needs headroom for system instruction + conversation history + response
        private const val MAX_TOKENS = 16384

        // FIX 2: prompt కి reasonable limit — 4000
        private const val MAX_PROMPT_CHARS = 4096

        private const val DEFAULT_INSTRUCTION =
            "You are Krishna from the Bhagavad Gita. " +
                    "Speak in clear, human-like sentences with proper spacing and punctuation. " +
                    "If Telugu mode, use Telugu. If English mode, use English."

        private val thinkingRegex = Regex("<\\|channel>thought.*<channel\\|>", RegexOption.DOT_MATCHES_ALL)
    }

    // =========================
    // INIT
    // =========================
    suspend fun initialize(path: String, maxTokens: Int = MAX_TOKENS): Boolean = engineMutex.withLock {
        return@withLock try {
            closeInternal()

            // Re-initialize the cleaner scope for the new session
            cleanerScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.Job())

            val engineConfig = EngineConfig(
                modelPath = path,
                maxNumTokens = maxTokens,
                backend = Backend.CPU()
            )
            val newEngine = Engine(engineConfig)
            newEngine.initialize()

            engine = newEngine
            conversation = newEngine.createConversation(
                ConversationConfig(
                    samplerConfig = SamplerConfig(
                        topK = 30,
                        topP = 0.9,
                        temperature = 0.9
                    )
                )
            )

            isInitialized = true
            modelPath = path
            Log.d(TAG, "LiteRT-LM initialized successfully ($maxTokens tokens)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "LiteRT-LM init failed", e)
            false
        }
    }

    // =========================
    // SEND MESSAGE
    // =========================
    suspend fun sendMessage(
        prompt: String,
        onPartial: ((String) -> Unit)? = null,
        onCleaned: ((String) -> Unit)? = null
    ): String = engineMutex.withLock {

        if (!isInitialized || conversation == null) {
            return@withLock "Error: Engine not initialized"
        }

        // Official Gallery format for turn-based models
        val systemInstruction = currentSystemInstruction ?: DEFAULT_INSTRUCTION
        val formattedPrompt = """
            <|turn>system
            $systemInstruction
            <|turn>user
            ${prompt.take(MAX_PROMPT_CHARS)}<turn|>
            <|turn>model
        """.trimIndent()

        val responseBuffer = StringBuilder()
        val answerBuffer = StringBuilder()
        var inThinkingBlock = false
        var lastSentLength = 0

        return@withLock try {
            val completed = withTimeoutOrNull(240_000) {
                conversation!!.sendMessageAsync(formattedPrompt)
                    .catch { throw it }
                    .collect { message ->
                        val chunk = message.toString()
                        if (chunk.isNotEmpty()) {
                            Log.v(TAG, "Model Chunk: '$chunk'") // Verbose logging for debugging spaces/artifacts
                            responseBuffer.append(chunk)

                            val hasStart = chunk.contains("<think") || chunk.contains("<|think") || chunk.contains("<thought") || chunk.contains("<channel>")
                            val hasEnd = chunk.contains("</think") || chunk.contains("</thought") || chunk.contains("<|think_end") || chunk.contains("<channel|>")

                            if (hasEnd) {
                                inThinkingBlock = false
                                val endTags = listOf("</think>", "</thought>", "<|think_end|>", "<channel|>")
                                var endPos = -1
                                for (tag in endTags) {
                                    val idx = chunk.lastIndexOf(tag)
                                    if (idx != -1) endPos = maxOf(endPos, idx + tag.length)
                                }
                                val effectiveChunk = if (endPos != -1) chunk.substring(endPos) else chunk
                                val cleaned = cleanChunkForStreaming(effectiveChunk)
                                if (cleaned.isNotEmpty()) {
                                    answerBuffer.append(cleaned)
                                    val currentAnswer = answerBuffer.toString()
                                    if (currentAnswer.length > lastSentLength) {
                                        onPartial?.invoke(currentAnswer)
                                        lastSentLength = currentAnswer.length
                                    }
                                }
                            } else if (!inThinkingBlock) {
                                val effectiveChunk = if (hasStart) {
                                    val startTags = listOf("<think", "<|think", "<thought", "<channel>")
                                    var startPos = chunk.length
                                    for (tag in startTags) {
                                        val idx = chunk.indexOf(tag)
                                        if (idx != -1) startPos = minOf(startPos, idx)
                                    }
                                    inThinkingBlock = true
                                    chunk.substring(0, startPos)
                                } else {
                                    chunk
                                }

                                val cleaned = cleanChunkForStreaming(effectiveChunk)
                                if (cleaned.isNotEmpty()) {
                                    answerBuffer.append(cleaned)
                                    val currentAnswer = answerBuffer.toString()
                                    if (currentAnswer.length > lastSentLength) {
                                        onPartial?.invoke(currentAnswer)
                                        lastSentLength = currentAnswer.length
                                    }
                                }
                            } else if (hasStart) {
                                inThinkingBlock = true
                            }
                        }
                    }
            }

            if (completed == null) {
                Log.w(TAG, "Voice chat generation timed out — forcing stop")
                stopResponse()
                onPartial?.invoke(" (response was cut short)")
            }

            val rawFinal = responseBuffer.toString()
            Log.d(TAG, "--- GENERATION COMPLETE ---")
            Log.d(TAG, "RAW OUTPUT:\n$rawFinal")

            // Final extraction using markers for safety
            val markers = listOf("<channel|>", "</think>", "</thought>", "<|thought_end|>", "<|think_end|>", "<|turn|>")
            var lastMarkerIndex = -1
            for (m in markers) {
                val idx = rawFinal.lastIndexOf(m)
                if (idx != -1) {
                    lastMarkerIndex = maxOf(lastMarkerIndex, idx + m.length)
                }
            }

            val finalAnswer = if (lastMarkerIndex != -1) {
                rawFinal.substring(lastMarkerIndex)
            } else {
                if (answerBuffer.isNotEmpty()) answerBuffer.toString() else rawFinal
            }
            Log.d(TAG, "EXTRACTED ANSWER:\n$finalAnswer")

            val basicCleaned = com.aipoweredgita.app.util.TextUtils.cleanLlmOutput(finalAnswer)
            Log.d(TAG, "CLEANED OUTPUT:\n$basicCleaned")
            Log.d(TAG, "---------------------------")

            cleanerScope.launch {
                try {
                    val deepCleaned = com.aipoweredgita.app.util.TextUtils.deepClean(basicCleaned)
                    withContext(Dispatchers.Main) {
                        onCleaned?.invoke(deepCleaned)
                        Log.d(TAG, "Background cleaner task finished.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Background cleaner failed", e)
                }
            }

            basicCleaned
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed, attempting recovery", e)
            recoverModel()
            "Error: ${e.message}"
        }
    }

    private fun cleanChunkForStreaming(chunk: String): String {
        return chunk
            .replace(Regex("<\\|think.*?\\|>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<thought>.*?</thought>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<channel>.*?</channel>", RegexOption.DOT_MATCHES_ALL), "")
            .replace("<think>", "").replace("</think>", "")
            .replace("<thought>", "").replace("</thought>", "")
            .replace("<channel>", "").replace("<channel|>", "")
            .replace("</channel>", "")
            .replace("<|think|>", "").replace("<|think_end|>", "")
            .replace("<|thought|>", "").replace("<|thought_end|>", "")
            .replace("<|turn|>", "").replace("<turn|>", "")
            .replace(Regex("<\\|[^>]+\\|>"), "")
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]"), "")
            // Normalize ONLY excessive whitespace (2+ -> 1) to preserve normal spaces
            .replace(Regex("[ \t]{2,}"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
    }

    // =========================
    // STOP RESPONSE
    // =========================
    fun stopResponse() {
        try {
            conversation?.cancelProcess()
        } catch (_: Exception) {}
    }

    // =========================
    // UTILITIES
    // =========================
    suspend fun updateSystemInstruction(instruction: String) {
        currentSystemInstruction = instruction
        // In LiteRT-LM 0.10.x, we usually pass instructions via prompt or
        // by recreating conversation if your architecture requires persistent system context.
        // For now, we store it for the prompt builder.
    }

    suspend fun resetConversation() {
        engineMutex.withLock {
            try {
                conversation?.close()
                val eng = engine ?: return@withLock
                conversation = eng.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(topK = 30, topP = 0.9, temperature = 0.9)
                    )
                )
            } catch (_: Exception) {}
        }
    }

    // =========================
    // HARD RESET (RECOVERY)
    // =========================
    private suspend fun recoverModel() {
        Log.w(TAG, "Triggering model recovery...")
        val path = modelPath
        if (path != null) {
            initialize(path)
        }
    }

    fun close() {
        runBlocking {
            engineMutex.withLock {
                closeInternal()
            }
        }
    }

    private fun closeInternal() {
        try {
            // Cancel any pending background cleaning tasks immediately
            cleanerScope.cancel()
            conversation?.close()
            engine?.close()
        } catch (_: Exception) {}
        conversation = null
        engine = null
        isInitialized = false
    }
}