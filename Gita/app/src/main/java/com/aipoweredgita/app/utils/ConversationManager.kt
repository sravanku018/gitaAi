package com.aipoweredgita.app.utils

import android.util.Log
import com.aipoweredgita.app.ml.LlmInferenceEngine
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.conflate

/**
 * Coordinator for LLM and Voice operations to ensure sequential execution
 * and prevent concurrency issues. Handles streaming, conflation, and audio buffering.
 *
 * Features:
 * - Prompt queue: processes one prompt at a time
 * - Lazy initialization: model loads only when first prompt arrives
 * - Cooldown period: prevents rapid successive prompts from overloading
 * - GenerationId filtering: ignores stale tokens from cancelled generations
 */
class ConversationManager(
    private val llm: LlmInferenceEngine,
    private val voice: VoiceManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val ERROR_PREFIX = "__ERROR__:"
    }

    private var job: Job? = null
    private var currentGenId: Long = 0
    private var isProcessing = false
    private val sentenceBuffer = StringBuilder()
    private var lastPromptTime = 0L
    private val PROMPT_COOLDOWN_MS = 1000L

    /**
     * Sends a message and coordinates streaming text and audio.
     */
    fun sendMessage(
        prompt: String,
        useStreaming: Boolean = true,
        onUpdate: (String) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        val now = System.currentTimeMillis()
        val timeSinceLastPrompt = now - lastPromptTime
        if (timeSinceLastPrompt < PROMPT_COOLDOWN_MS && isProcessing) {
            Log.w("ConversationManager", "Prompt rejected: too soon")
            onError("Please wait a moment before sending next message...")
            return
        }

        stop()
        lastPromptTime = System.currentTimeMillis()

        job = scope.launch(Dispatchers.Default) {
            delay(50)
            isProcessing = true

            Log.d("ConversationManager", "Processing prompt: ${prompt.take(50)}...")

            try {
                if (useStreaming) {
                    var fullResponse = ""

                    val tokenJob = launch {
                        LlmInferenceEngine.tokenFlow
                            .conflate()
                            .collect { event ->
                                // FIX: Filter by generationId — ignore stale tokens
                                if (event.genId != currentGenId) return@collect
                                if (event.token.isBlank() && !event.isComplete) return@collect

                                // Check for error prefix in token
                                if (event.token.startsWith(ERROR_PREFIX)) {
                                    val message = event.token.removePrefix(ERROR_PREFIX)
                                    withContext(Dispatchers.Main) { onError(message) }
                                    if (event.isComplete) { onDone() }
                                    cancel()
                                    return@collect
                                }

                                fullResponse += event.token
                                withContext(Dispatchers.Main) {
                                    onUpdate(fullResponse)
                                }

                                processAudio(event.token)

                                if (event.isComplete) {
                                    finalizeAudio { onDone() }
                                    cancel()
                                }
                            }
                    }

                    // FIX: Capture generationId before async starts
                    currentGenId = LlmInferenceEngine.getCurrentGenerationId()
                    llm.startGeneratingAsync(prompt)
                    tokenJob.join()
                } else {
                    val response = llm.generateResponse(prompt)
                    if (response.startsWith(ERROR_PREFIX)) {
                        val message = response.removePrefix(ERROR_PREFIX)
                        withContext(Dispatchers.Main) { onError(message) }
                    } else {
                        withContext(Dispatchers.Main) { onUpdate(response) }
                        finalizeAudio { onDone() }
                    }
                }

            } catch (e: Exception) {
                Log.e("ConversationManager", "Message error", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "శిష్యుడా, క్షమించు… మళ్లీ ప్రయత్నిద్దాం.")
                }
            } finally {
                isProcessing = false
            }
        }
    }

    private fun processAudio(token: String) {
        sentenceBuffer.append(token)
        val bufferStr = sentenceBuffer.toString()
        val boundaries = charArrayOf('.', '!', '?', '\n', '।', '॥')
        val boundaryIndex = bufferStr.indexOfAny(boundaries)

        if (boundaryIndex != -1) {
            val sentence = bufferStr.substring(0, boundaryIndex + 1).trim()
            if (sentence.isNotBlank()) {
                voice.speak(sentence, flush = false)
            }
            
            // Clean buffer
            if (boundaryIndex + 1 < bufferStr.length) {
                val remainder = bufferStr.substring(boundaryIndex + 1)
                sentenceBuffer.setLength(0)
                sentenceBuffer.append(remainder)
            } else {
                sentenceBuffer.setLength(0)
            }
        } else if (sentenceBuffer.length > 100) {
            // Force speak on long chunks without punctuation
            val chunk = sentenceBuffer.toString().trim()
            if (chunk.isNotBlank()) voice.speak(chunk, flush = false)
            sentenceBuffer.setLength(0)
        }
    }

    private fun finalizeAudio(onComplete: () -> Unit) {
        val remainder = sentenceBuffer.toString().trim()
        sentenceBuffer.setLength(0)
        if (remainder.isNotBlank()) {
            voice.speak(remainder, flush = false) {
                onComplete()
            }
        } else {
            onComplete()
        }
    }

    fun stop() {
        job?.cancel()
        llm.stopGeneration()
        voice.stopSpeaking()
        sentenceBuffer.setLength(0)
        isProcessing = false
    }

    fun isBusy(): Boolean = isProcessing
    
    /**
     * Check if LLM is ready and cooldown allows new prompt.
     */
    fun canAcceptPrompt(): Boolean {
        val cooldownElapsed = System.currentTimeMillis() - lastPromptTime >= PROMPT_COOLDOWN_MS
        val llmReady = llm.isReadyOrPending()
        return !isProcessing && cooldownElapsed && llmReady
    }
}
