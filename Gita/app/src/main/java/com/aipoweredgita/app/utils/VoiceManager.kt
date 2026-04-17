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

    init {
        mainHandler.post {
            tts = TextToSpeech(context, this)
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            setLanguage(preferredLocale)
            isTtsReady = true
            setupTtsListener()
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
                    onError("Voice error ($error)")
                }
                override fun onResults(results: Bundle?) {
                    isListeningActive = false
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
            speechRecognizer?.startListening(intent)
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
