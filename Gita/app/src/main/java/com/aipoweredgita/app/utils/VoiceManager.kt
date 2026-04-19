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
    private var isListeningActive = false

    private val utteranceCallbacks = ConcurrentHashMap<String, () -> Unit>()
    @Volatile private var isDestroyed = false
    private var preferredLocale: Locale = Locale.forLanguageTag("te-IN")

    /** Error callback for crash-safe reporting to the ViewModel layer */
    var onError: ((String) -> Unit)? = null

    /** Tracks consecutive STT errors for auto-recovery */
    private var consecutiveSttErrors = 0
    private val MAX_CONSECUTIVE_STT_ERRORS = 3

    init {
        mainHandler.post {
            try {
                tts = TextToSpeech(context, this)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init TTS", e)
                onError?.invoke("Text-to-Speech initialization failed")
            }
            try {
                if (SpeechRecognizer.isRecognitionAvailable(context)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to init SpeechRecognizer", e)
                onError?.invoke("Speech recognition initialization failed")
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setLanguage(preferredLocale)
            isTtsReady = true
            setupTtsListener()
        } else {
            Log.e(TAG, "TTS init failed with status $status")
            isTtsReady = false
            onError?.invoke("Text-to-Speech engine failed to start")
        }
    }

    fun setPreferredLocale(locale: Locale) {
        this.preferredLocale = locale
        mainHandler.post {
            if (isDestroyed) return@post
            setLanguage(locale)
        }
    }

    fun setLanguage(locale: Locale): Boolean {
        val result = tts?.setLanguage(locale)
        val success = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
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
            override fun onError(utteranceId: String?) {
                utteranceId?.let { id ->
                    utteranceCallbacks.remove(id)?.let { cb -> mainHandler.post { cb.invoke() } }
                }
            }
        })
    }

    fun speak(text: String, flush: Boolean = true, onComplete: (() -> Unit)? = null) {
        mainHandler.post {
            if (isDestroyed) return@post
            try {
                if (isTtsReady) {
                    if (flush) {
                        tts?.stop()
                        utteranceCallbacks.clear()
                    }
                    val utteranceId = "gita_${System.currentTimeMillis()}"
                    if (onComplete != null) utteranceCallbacks[utteranceId] = onComplete
                    val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) }
                    val result = tts?.speak(text, if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, params, utteranceId)
                    if (result == TextToSpeech.ERROR) {
                        utteranceCallbacks.remove(utteranceId)
                        onComplete?.invoke()
                    }
                } else {
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                Log.e(TAG, "TTS speak failed", e)
                onError?.invoke("Failed to speak")
                onComplete?.invoke()
            }
        }
    }

    fun stopSpeaking() {
        mainHandler.post {
            if (isDestroyed) return@post
            tts?.stop()
            utteranceCallbacks.clear()
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onPartialResult: (String) -> Unit = {}
    ) {
        mainHandler.post {
            if (isDestroyed || isListeningActive || speechRecognizer == null) {
                if (speechRecognizer == null) onError("Speech recognition not available")
                return@post
            }
            isListeningActive = true

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, sttLocale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListeningActive = false }
                override fun onError(error: Int) {
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
                    isListeningActive = false
                    consecutiveSttErrors = 0
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) onResult(matches[0])
                    else onError("No speech recognized")
                }
                override fun onPartialResults(partialResults: Bundle?) {
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
            isListeningActive = false
            speechRecognizer?.stopListening()
        }
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            tts?.shutdown()
            speechRecognizer?.destroy()
        }
    }
}
