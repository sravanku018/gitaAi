package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.google.ai.edge.litertlm.Backend
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * FIXED LLM Inference Engine with production-hardened safety.
 * 
 * Fixes applied:
 * 1. TokenEvent with generationId — prevents mixed responses
 * 2. AtomicBoolean for isGenerating — thread-safe
 * 3. Conversation reset per request — prevents context leakage
 * 4. Timeout calls stopGeneration() — cancels native inference
 * 5. Safe message extraction — trim + fallback
 * 6. OOM tracking — prevents retry after memory failure
 * 7. Executor shutdown — prevents memory leak
 * 8. Cancel previous generation — more responsive UX
 */
class LlmInferenceEngine(private val context: Context) {

    private val TAG = "LlmInferenceEngine"

    companion object {
        private var engine: Engine? = null
        private var conversation: Conversation? = null
        private val sharedMutex = Mutex()
        private val generationMutex = Mutex()

        private val sharedExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "llm-inference-thread").also { it.isDaemon = true }
        }
        private val sharedDispatcher = sharedExecutor.asCoroutineDispatcher()

        // FIX ISSUE 1: TokenEvent with generationId
        data class TokenEvent(
            val genId: Long,
            val token: String,
            val isComplete: Boolean,
            val isFullCleanup: Boolean = false
        )
        private val _tokenFlow = MutableSharedFlow<TokenEvent>(extraBufferCapacity = 100)
        val tokenFlow: SharedFlow<TokenEvent> = _tokenFlow.asSharedFlow()

        private var isInitialized = false
        // FIX ISSUE 2: AtomicBoolean for thread-safety
        private val isGenerating = AtomicBoolean(false)
        private var currentGenerationId = 0L
        private var modelPath: String? = null
        
        private val thinkingRegex = Regex("<\\|channel>thought.*<channel\\|>", RegexOption.DOT_MATCHES_ALL)
        private val thinkingStartTag = "<|channel>thought"

        /** Current generation ID — used by UI to filter stale tokens */
        fun getCurrentGenerationId(): Long = currentGenerationId
        
        // FIX ISSUE 6: Track OOM failures
        private var lastInitFailedDueToOOM = false

        fun stopGeneration() {
            isGenerating.set(false)
            currentGenerationId++
            // FIX ISSUE 4: Cancel underlying native inference
            try { conversation?.cancelProcess() } catch (_: Exception) {}
        }

        /**
         * FIX ISSUE 3: Reset conversation to prevent context leakage.
         * Call before new quiz question or after N messages.
         */
        fun resetConversation() {
            synchronized(this) {
                try {
                    conversation?.close()
                } catch (_: Exception) {}
                val eng = engine ?: return@synchronized
                conversation = eng.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = 64,
                            topP = 0.95,
                            temperature = 1.0
                        )
                    )
                )
                Log.d("LlmInferenceEngine", "Conversation reset")
            }
        }

        /**
         * FIX ISSUE 7: Shutdown executor to prevent memory leak.
         * Call only when engine is truly no longer needed.
         */
        fun shutdown() {
            sharedExecutor.shutdown()
            try { conversation?.close() } catch (_: Exception) {}
            try { engine?.close() } catch (_: Exception) {}
            engine = null
            conversation = null
            isInitialized = false
        }
    }

    fun isReady(): Boolean = engine != null
    fun isReadyOrPending(): Boolean = isReady() || modelPath != null
    fun isInitialized(): Boolean = isInitialized
    fun stopGeneration() { Companion.stopGeneration() }

    fun registerModelPath(path: String) {
        modelPath = path
        Log.d(TAG, "Model path registered for lazy init: $path")
    }

    suspend fun initialize(modelPath: String): Boolean = withContext(sharedDispatcher) {
        sharedMutex.withLock {
            // FIX ISSUE 6: Skip retry if last init failed due to OOM
            if (lastInitFailedDueToOOM) {
                Log.w(TAG, "Skipping init — previous attempt failed due to OOM")
                return@withLock false
            }

            if (isInitialized && engine != null) {
                Log.d(TAG, "LLM already initialized, reusing existing instance")
                return@withLock true
            }

            return@withLock try {
                Log.d(TAG, "=== LLM Initialization Start ===")

                val modelFile = java.io.File(modelPath)
                if (!modelFile.exists()) {
                    Log.e(TAG, "Model file does not exist: $modelPath")
                    return@withLock false
                }

                val fileSizeMb = modelFile.length() / (1024 * 1024)
                Log.d(TAG, "Model file: ${fileSizeMb}MB, canRead: ${modelFile.canRead()}")

                if (fileSizeMb < 100) {
                    Log.w(TAG, "Model file too small (${fileSizeMb}MB), aborting")
                    return@withLock false
                }

                val engineConfig = EngineConfig(
                    modelPath = modelFile.absolutePath,
                    maxNumTokens = 8192,
                    backend = Backend.CPU()
                )
                engine = Engine(engineConfig)
                engine!!.initialize()
                Log.d(TAG, "Engine initialized")

                // FIX ISSUE 3: Create fresh conversation with hardware-optimal Gemma 4 settings
                conversation = engine!!.createConversation(
                    ConversationConfig(
                        samplerConfig = SamplerConfig(
                            topK = 64,
                            topP = 0.95,
                            temperature = 1.0 // REQUIRED for Gemma 4 entropy tuning
                        )
                    )
                )
                Log.d(TAG, "Conversation created")

                isInitialized = true
                isGenerating.set(false)
                Companion.modelPath = modelPath

                Log.d(TAG, "=== LLM Initialization Complete ===")
                true
            } catch (e: Exception) {
                Log.e(TAG, "LLM initialization failed: ${e.javaClass.name}: ${e.message}", e)
                
                // FIX ISSUE 6: Track OOM failures
                val isOOM = e.message?.contains("memory", ignoreCase = true) == true ||
                            e.message?.contains("alloc", ignoreCase = true) == true ||
                            e is OutOfMemoryError
                
                if (isOOM) {
                    lastInitFailedDueToOOM = true
                    Log.e(TAG, "HINT: Out of memory. Device needs 8GB+ RAM for Gemma 2B.")
                } else if (e.message?.contains("file", ignoreCase = true) == true) {
                    Log.e(TAG, "HINT: File access issue")
                } else if (e is UnsatisfiedLinkError) {
                    Log.e(TAG, "HINT: Native library loading failed")
                }
                
                // Cleanup on failure
                try { engine?.close() } catch (_: Exception) {}
                engine = null
                conversation = null
                false
            }
        }
    }

    suspend fun startGeneratingAsync(prompt: String) = withContext(sharedDispatcher) {
        // FIX ISSUE 8: Cancel previous generation instead of queuing
        if (isGenerating.get()) {
            Log.d(TAG, "Cancelling previous generation for new request")
            stopGeneration()
            delay(100)
        }

        generationMutex.withLock {
            if (!isInitialized && modelPath != null) {
                Log.d(TAG, "Lazy initializing LLM...")
                val initSuccess = initialize(modelPath!!)
                if (!initSuccess) {
                    val myGenId = currentGenerationId + 1
                    _tokenFlow.tryEmit(TokenEvent(myGenId, "Model failed to load. Please check Settings.", true))
                    return@withLock
                }
            }

            val conv = conversation
            if (conv == null || engine == null) {
                _tokenFlow.tryEmit(TokenEvent(currentGenerationId + 1, "LLM not initialized", true))
                return@withLock
            }

            // FIX ISSUE 3: Reset conversation before each new generation
            resetConversation()
            val freshConv = conversation ?: run {
                _tokenFlow.tryEmit(TokenEvent(currentGenerationId + 1, "Conversation reset failed", true))
                return@withLock
            }

            val formattedPrompt = """
                <|turn>system
                <|think|><turn|>
                <|turn>user
                ${prompt}<turn|>
                <|turn>model
            """.trimIndent()
            val response = StringBuilder()
            var lastEmittedText = ""

            isGenerating.set(true)
            val myGenId = ++currentGenerationId

            try {
                // FIX ISSUE 4: Timeout calls stopGeneration() to cancel native inference
                withTimeoutOrNull(60_000) {
                    freshConv.sendMessageAsync(formattedPrompt)
                        .catch { throwable ->
                            Log.e(TAG, "LLM Flow error", throwable)
                            if (myGenId == currentGenerationId) {
                                _tokenFlow.tryEmit(TokenEvent(myGenId, "Error: ${throwable.message}", true))
                                isGenerating.set(false)
                            }
                        }
                        .collect { message ->
                            if (myGenId != currentGenerationId) return@collect

                            response.append(message.toString())
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

                                if (cleanText.length > lastEmittedText.length) {
                                    val newChunk = cleanText.substring(lastEmittedText.length)
                                    lastEmittedText = cleanText
                                    
                                    // Emit clean chunk directly to UI (no raw text ever shown)
                                    _tokenFlow.tryEmit(TokenEvent(myGenId, newChunk, false))
                                }
                            }
                        }

                    if (myGenId == currentGenerationId && isGenerating.get()) {
                        // Fallback: If thinking never formally closed (rare), strip it via regex
                        if (response.isNotEmpty() && !response.contains("<channel|>")) {
                            val fallback = response.toString()
                                .replace(Regex("<\\|channel>thought.*?<channel\\|>", RegexOption.DOT_MATCHES_ALL), "")
                                .replace("\n", " ")
                                .replace("\\s+".toRegex(), " ")
                                .replace(" ,", ",")
                                .replace(" .", ".")
                                .replace(" ?", "?")
                                .replace(" !", "!")
                                .trim()
                            
                            if (fallback.isNotEmpty()) {
                                _tokenFlow.tryEmit(TokenEvent(myGenId, fallback, false))
                            }
                        }

                        // Emit normal completion
                        _tokenFlow.tryEmit(TokenEvent(myGenId, "", true))
                        
                        // BACKGROUND CLEANER: Final refinement
                        val finalCleaned = com.aipoweredgita.app.util.TextUtils.deepClean(lastEmittedText)
                        _tokenFlow.tryEmit(TokenEvent(myGenId, finalCleaned, true, isFullCleanup = true))
                        
                        isGenerating.set(false)
                    }
                } ?: run {
                    Log.w(TAG, "LLM generation timed out — forcing stop")
                    // FIX ISSUE 4: Force stop native inference on timeout
                    stopGeneration()
                    if (myGenId == currentGenerationId) {
                        _tokenFlow.tryEmit(TokenEvent(myGenId, "Response timeout", true))
                        isGenerating.set(false)
                    }
                }
            } catch (e: Exception) {
                if (myGenId == currentGenerationId) {
                    Log.e(TAG, "LLM async generation error", e)
                    _tokenFlow.tryEmit(TokenEvent(myGenId, "Error: ${e.message}", true))
                    isGenerating.set(false)
                }
            } finally {
                // FIX ISSUE 4: Always reset isGenerating flag
                isGenerating.set(false)
            }
        }
    }

    suspend fun generateResponse(prompt: String): String = withContext(sharedDispatcher) {
        // FIX ISSUE 8: Cancel previous generation
        if (isGenerating.get()) {
            stopGeneration()
            delay(50)
        }

        sharedMutex.withLock {
            if (!isInitialized && modelPath != null) initialize(modelPath!!)

            val conv = conversation ?: return@withLock "LLM not initialized."
            
            // FIX ISSUE 3: Reset conversation before sync call
            resetConversation()
            val freshConv = conversation ?: return@withLock "Conversation reset failed."

            return@withLock try {
                val start = System.currentTimeMillis()
                val formattedPrompt = "<|turn>user\n$prompt<turn|>\n<|turn>model\n<|channel>thought"
                val message = freshConv.sendMessage(formattedPrompt)
                Log.d(TAG, "LLM sync response: ${System.currentTimeMillis() - start}ms")
                
                // 1. Strip the hidden thinking block so UI only sees the answer
                val cleanOutput = message.toString().replace(thinkingRegex, "").trim()

                // 2. Fix spacing: collapse whitespace, strip space-before-punctuation
                val fixedSpacing = cleanOutput
                    .replace("\n", " ")
                    .replace("\\s+".toRegex(), " ")
                    .replace(" ,", ",")
                    .replace(" .", ".")
                    .replace(" ?", "?")
                    .replace(" !", "!")
                    .trim()

                // FINAL CLEANUP using "Background" process logic
                com.aipoweredgita.app.util.TextUtils.deepClean(fixedSpacing)
            } catch (e: Exception) {
                "Error: ${e.message}"
            }
        }
    }

    fun close() {
        // Don't shutdown — engine persists across app lifecycle
        // Call shutdown() explicitly only when truly needed
    }
}
