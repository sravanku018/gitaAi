package com.aipoweredgita.app.utils

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe wrapper for Android TextToSpeech and SpeechRecognizer.
 */
class VoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val TAG = "VoiceManager"
    private val mainHandler = Handler(Looper.getMainLooper())

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsReady = false
    @Volatile private var isListeningActive = false

    private val utteranceCallbacks = ConcurrentHashMap<String, () -> Unit>()
    private val utteranceCounter = java.util.concurrent.atomic.AtomicLong(0)
    private val activeSessionId = java.util.concurrent.atomic.AtomicLong(0)
    @Volatile private var isDestroyed = false
    private var preferredLocale: Locale = Locale.forLanguageTag("te-IN")

    /** Error callback for crash-safe reporting to the ViewModel layer */
    private var pendingError: String? = null

    var onError: ((String) -> Unit)? = null
        set(value) {
            field = value
            pendingError?.let {
                value?.invoke(it)
                pendingError = null
            }
        }

    /** Tracks consecutive STT errors for auto-recovery */
    @Volatile private var consecutiveSttErrors = 0
    private val MAX_CONSECUTIVE_STT_ERRORS = 3

    init {
        mainHandler.post {
            try {
                tts = TextToSpeech(context, this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init TTS", e)
                val msg = "Text-to-Speech initialization failed"
                onError?.invoke(msg) ?: run { pendingError = msg }
            }
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init SpeechRecognizer", e)
                val msg = "Speech recognition initialization failed"
                onError?.invoke(msg) ?: run { pendingError = msg }
            }
        }
    }

    override fun onInit(status: Int) {
        if (isDestroyed) return
        if (status == TextToSpeech.SUCCESS) {
            setLanguage(preferredLocale)
            isTtsReady = true
            setupTtsListener()
        } else {
            Log.e(TAG, "TTS init failed with status $status")
            isTtsReady = false
            val msg = "Text-to-Speech engine failed to start"
            onError?.invoke(msg) ?: run { pendingError = msg }
        }
    }

    fun setPreferredLocale(locale: Locale) {
        this.preferredLocale = locale
        mainHandler.post {
            if (isDestroyed) return@post
            setLanguage(locale)
        }
    }

    fun setSttLocale(sttLocale: String) {
        this.sttLocale = sttLocale
    }

    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        val success = result != null && result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
        if (!success) {
            tts?.setLanguage(Locale.getDefault())
        }
        return success
    }

    /**
     * Set both STT (speech recognition) and TTS (text-to-speech) locales.
     * Used by language mode switching in Voice Studio.
     */
    fun setLocale(sttLocale: String, ttsLocale: String) {
        preferredLocale = Locale.forLanguageTag(ttsLocale)
        if (isTtsReady) {
            setLanguage(preferredLocale)
        }
        // Store STT locale for use in startListening
        this.sttLocale = sttLocale
    }

    private var sttLocale: String = "te-IN"

    private fun setupTtsListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                utteranceId?.let { id ->
                    utteranceCallbacks.remove(id)?.let { cb -> mainHandler.post { cb.invoke() } }
                }
            }
            @Suppress("DEPRECATION")
            override fun onError(utteranceId: String?) {
                utteranceId?.let { id ->
                    utteranceCallbacks.remove(id)?.let { cb -> mainHandler.post { cb.invoke() } }
                }
            }
        })
    }

    private fun flushCallbacks() {
        val callbacks = ArrayList(utteranceCallbacks.values)
        utteranceCallbacks.clear()
        callbacks.forEach { cb ->
            try { cb.invoke() } catch (e: Exception) { Log.e(TAG, "Error flushing callback", e) }
        }
    }

    fun speak(text: String, flush: Boolean = true, onComplete: (() -> Unit)? = null) {
        if (isDestroyed) {
            onComplete?.invoke()
            return
        }
        val utteranceId = "gita_${utteranceCounter.incrementAndGet()}"
        if (onComplete != null) {
            utteranceCallbacks[utteranceId] = onComplete
        }
        mainHandler.post {
            if (isDestroyed) {
                utteranceCallbacks.remove(utteranceId)?.invoke()
                return@post
            }
            try {
                if (isTtsReady) {
                    // Auto-detect Telugu text and switch TTS locale dynamically
                    val isTeluguText = text.any { it in '\u0C00'..'\u0C7F' }
                    val targetLocale = if (isTeluguText) Locale.forLanguageTag("te-IN") else preferredLocale
                    setLanguage(targetLocale)

                    if (flush) {
                        tts?.stop()
                        // Ensure this utteranceCallback stays registered even after flush
                        val currentCallback = utteranceCallbacks.remove(utteranceId)
                        flushCallbacks()
                        if (currentCallback != null) {
                            utteranceCallbacks[utteranceId] = currentCallback
                        }
                    }
                    val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
                    val result = tts?.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, params, utteranceId)
                    if (result == TextToSpeech.ERROR) {
                        utteranceCallbacks.remove(utteranceId)?.invoke()
                    }
                } else {
                    utteranceCallbacks.remove(utteranceId)?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak failed", e)
                onError?.invoke("Failed to speak")
                utteranceCallbacks.remove(utteranceId)?.invoke()
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            if (isDestroyed) return@post
            tts?.stop()
            flushCallbacks()
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: (String) -> Unit = {}
    ) {
        mainHandler.post {
            if (isDestroyed || speechRecognizer == null) {
                if (speechRecognizer == null) onError("Speech recognition not available")
                return@post
            }
            if (isListeningActive) {
                activeSessionId.incrementAndGet() // Invalidate previous session before starting new one
                try { speechRecognizer?.cancel() } catch (_: Exception) {}
                isListeningActive = false
            }
            isListeningActive = true
            val sessionId = activeSessionId.incrementAndGet()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onError(error: Int) {
                    if (sessionId != activeSessionId.get()) return // Ignore stale callbacks
                    activeSessionId.incrementAndGet() // Invalidate session on terminal error
                    isListeningActive = false
                    consecutiveSttErrors++
                    val userMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "Microphone error — check permissions"
                        SpeechRecognizer.ERROR_CLIENT -> "Listening cancelled"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                        SpeechRecognizer.ERROR_NETWORK -> "No network — voice needs internet"
                        SpeechRecognizer.ERROR_NO_MATCH -> "Could not understand — try again"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Voice engine busy — retry in a moment"
                        SpeechRecognizer.ERROR_SERVER -> "Voice service unavailable"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected — try again"
                        else -> "Voice error ($error)"
                    }
                    if (consecutiveSttErrors >= MAX_CONSECUTIVE_STT_ERRORS) {
                        Log.w(TAG, "Too many consecutive STT errors ($consecutiveSttErrors), recreating recognizer")
                        recreateRecognizer()
                        consecutiveSttErrors = 0
                    }
                    onError(userMessage)
                }
                override fun onResults(results: Bundle?) {
                    if (sessionId != activeSessionId.get()) return // Ignore stale callbacks
                    activeSessionId.incrementAndGet() // Invalidate session on terminal result
                    isListeningActive = false
                    consecutiveSttErrors = 0
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) onResult(matches[0])
                    else onError("No speech recognized")
                }
                override fun onPartialResults(partialResults: Bundle?) {
                    if (sessionId != activeSessionId.get()) return
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) onPartialResult(matches[0])
                }
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start listening", e)
                isListeningActive = false
                onError("Failed to start voice recognition")
            }
        }
    }

    private fun recreateRecognizer() {
        mainHandler.post {
            activeSessionId.incrementAndGet()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            try {
                speechRecognizer = if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    SpeechRecognizer.createSpeechRecognizer(context)
                } else null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to recreate recognizer", e)
                speechRecognizer = null
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            if (isDestroyed || speechRecognizer == null) return@post
            speechRecognizer?.stopListening()
        }
    }

    fun cancelListening() {
        mainHandler.post {
            activeSessionId.incrementAndGet() // Invalidate current session
            isListeningActive = false
            try { speechRecognizer?.cancel() } catch (_: Exception) {}
        }
    }

    fun destroy() {
        isDestroyed = true
        activeSessionId.incrementAndGet() // Invalidate active session
        isListeningActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            flushCallbacks()
            try { tts?.stop(); tts?.shutdown() } catch (_: Exception) {}
            try { speechRecognizer?.cancel(); speechRecognizer?.destroy() } catch (_: Exception) {}
            speechRecognizer = null
            tts = null
        }
    }
}
