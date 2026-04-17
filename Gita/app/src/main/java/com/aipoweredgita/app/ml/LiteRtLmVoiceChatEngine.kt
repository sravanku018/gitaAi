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
        private const val MAX_TOKENS = 8192

        // FIX 2: prompt కి reasonable limit — 1200
        private const val MAX_PROMPT_CHARS = 1200

        private const val DEFAULT_INSTRUCTION =
            "<|think|>\nYou are Krishna from the Bhagavad Gita. " +
                    "Speak in short, clear, human-like sentences. " +
                    "Always use proper spaces between words. " +
                    "Never join words together or use camelCase. " +
                    "Use correct punctuation and spacing. " +
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
                        topK = 64,
                        topP = 0.95,
                        temperature = 1.0 // REQUIRED for Gemma 4
                    )
                )
            )

            isInitialized = true
            modelPath = path
            Log.d(TAG, "LiteRT-LM initialized successfully (8192 tokens)")
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
            <|think|><turn|>
            <|turn>user
            ${prompt.take(MAX_PROMPT_CHARS)}<turn|>
            <|turn>model
        """.trimIndent()
        val response = StringBuilder()

        return@withLock try {
            conversation!!.sendMessageAsync(formattedPrompt)
                .catch { throw it }
                .collect { message ->
                    val chunk = message.toString()
                    if (chunk.isNotEmpty()) {
                        response.append(chunk)
                        val fullSoFar = response.toString()
                        
                        // NEW: Only start showing text AFTER thinking block is closed
                        if (fullSoFar.contains("<channel|>")) {
                            val answerPart = fullSoFar.substringAfter("<channel|>")
                            
                            val cleanText = answerPart
                                .replace("\n", " ")
                                .replace("\\s+".toRegex(), " ")
                                .replace(" ,", ",")
                                .replace(" .", ".")
                                .replace(" ?", "?")
                                .replace(" !", "!")
                                .trim()
                                
                            onPartial?.invoke(cleanText)
                        }
                    }
                }

            // Fallback: If thinking never formally closed (rare), strip it via regex
            if (response.isNotEmpty() && !response.contains("<channel|>")) {
                val fallbackText = response.toString()
                    .replace(Regex("<\\|channel>thought.*?<channel\\|>", RegexOption.DOT_MATCHES_ALL), "")
                    .replace("\n", " ")
                    .replace("\\s+".toRegex(), " ")
                    .replace(" ,", ",")
                    .replace(" .", ".")
                    .replace(" ?", "?")
                    .replace(" !", "!")
                    .trim()
                
                if (fallbackText.isNotEmpty()) {
                    onPartial?.invoke(fallbackText)
                }
            }

            // PROACTIVE CLEANUP: Immediately stop any lingering model processing 
            // once we have our raw accumulated text.
            stopResponse()

            val rawFinal = response.toString()
            val finalAnswer = rawFinal.substringAfter("<channel|>", rawFinal)
            
            // Basic cleanup for the synchronous return
            val basicCleaned = finalAnswer
                .replace("\n", " ")
                .replace("\\s+".toRegex(), " ")
                .replace(" ,", ",")
                .replace(" .", ".")
                .replace(" ?", "?")
                .replace(" !", "!")
                .trim()

            // BACKGROUND "CLEANER" PROCESS:
            // Refine the final answer with deep cleaning in the dedicated scope.
            // The process completes and releases resources immediately after onCleaned.
            cleanerScope.launch {
                try {
                    val deepCleaned = com.aipoweredgita.app.util.TextUtils.deepClean(basicCleaned)
                    withContext(Dispatchers.Main) {
                        onCleaned?.invoke(deepCleaned)
                        Log.d(TAG, "Background cleaner task finished and closed.")
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
                conversation = engine?.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 1.0)
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