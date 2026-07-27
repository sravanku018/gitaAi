package com.aipoweredgita.app.ml

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aipoweredgita.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Features that depend on AI models.
 */
enum class AppFeature { VOICE, QUIZ }

data class ModelRuntimeDecision(
    val feature: AppFeature,
    val selectedPreference: String,
    val tierLabel: String,
    val useProxy: Boolean,
    val modelPath: String?,
    val displayName: String
)

/**
 * Manages model availability for different features.
 *
 * Model Strategy:
 * - **Groq (Cloud)** → Default for all features (fast, no download)
 * - **NVIDIA 70B (Cloud)** → Secondary cloud option
 * - **Gemma 4 2B (~2.58GB)** → Optional offline download for Voice (flagship only)
 *
 * Features gracefully degrade:
 * - No internet → falls back to rule-based algo
 */
class ModelAvailability(appContext: Context) {

    private val TAG = "ModelAvailability"
    private val context = appContext.applicationContext
    private val modelsDir = File(context.filesDir, "ml_models")
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Default is now Groq (cloud) — no download needed
    private val _selectedModel = MutableStateFlow(
        prefs.getString("selected_ai_model", "Groq (Cloud)") ?: "Groq (Cloud)"
    )
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "selected_ai_model") {
            val newVal = sharedPreferences.getString(key, "Groq (Cloud)") ?: "Groq (Cloud)"
            Log.d(TAG, "Model preference changed: $newVal")
            _selectedModel.value = newVal
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    // Model size thresholds for integrity validation
    private val gemma4MinSize = 1_000_000_000L // ~1GB floor for 2B models

    private fun getDeviceRamGb(): Float {
        val mi = android.app.ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager)
            .getMemoryInfo(mi)
        return mi.totalMem / 1_073_741_824f
    }

    fun isGemma4Available(): Boolean = getGemma4Path() != null

    fun getGemma4Path(): String? {
        val ramGb = getDeviceRamGb()
        if (ramGb < 7.5f) {
            Log.d(TAG, "Gemma 4 blocked: RAM capacity (%.1f GB) is below 7.5 GB".format(ramGb))
            return null
        }
        return validatedPath("gemma-4-E2B-it.litertlm", gemma4MinSize)
    }

    private fun resolveModelPath(feature: AppFeature, selected: String): String? {
        val gemma4Path = getGemma4Path()

        return when {
            // Cloud providers — no local model path
            selected.contains("NVIDIA", ignoreCase = true) -> null
            selected.contains("Groq", ignoreCase = true) -> null
            // Gemma 4 offline (Voice only)
            selected.contains("Gemma 4") && gemma4Path != null -> gemma4Path
            // Auto: prefer cloud (Groq), fall to Gemma if available and voice
            else -> {
                if (feature == AppFeature.VOICE) gemma4Path else null
            }
        }
    }

    fun getRuntimeDecision(feature: AppFeature): ModelRuntimeDecision {
        val selected = _selectedModel.value
        val tier = com.aipoweredgita.app.utils.DeviceTierDetector.detect(context)

        val useProxy = when {
            selected.contains("NVIDIA", ignoreCase = true) -> true
            selected.contains("Groq", ignoreCase = true) -> true
            // Auto or no local model → use Groq proxy
            resolveModelPath(feature, selected) == null -> true
            else -> false
        }

        val modelPath = if (useProxy) null else resolveModelPath(feature, selected)

        val displayName = when {
            useProxy && selected.contains("NVIDIA", ignoreCase = true) -> "NVIDIA 70B (Cloud)"
            useProxy && selected.contains("Groq", ignoreCase = true) -> "Groq (Cloud)"
            useProxy -> "Groq (Cloud)"
            modelPath == null -> "Groq (Cloud)"
            modelPath.contains("gemma", ignoreCase = true) -> "Gemma 4 2B (Advanced)"
            else -> File(modelPath).name
        }

        Log.d(
            TAG,
            "Runtime decision for $feature: display=$displayName useProxy=$useProxy " +
                "selected=$selected tier=${tier.label} path=${modelPath?.let { File(it).name } ?: "NONE"}"
        )

        return ModelRuntimeDecision(
            feature = feature,
            selectedPreference = selected,
            tierLabel = tier.label,
            useProxy = useProxy,
            modelPath = modelPath,
            displayName = displayName
        )
    }

    fun getResolvedModelPath(feature: AppFeature): String? {
        return getRuntimeDecision(feature).modelPath
    }

    fun isGemmaRunning(feature: AppFeature): Boolean {
        val path = getRuntimeDecision(feature).modelPath
        return path != null && path.contains("gemma", ignoreCase = true)
    }

    fun getResolvedTextModelPath(selectedPreference: String): String? {
        return resolveModelPath(AppFeature.QUIZ, selectedPreference)
    }

    /**
     * Text features default to cloud proxy (Groq/NVIDIA).
     * Gemma 4 offline only available for voice on flagship devices.
     */
    fun getBestTextModelPath(): String? = null // Always use cloud for text

    fun getBestVoiceModelPath(): String? {
        return getResolvedModelPath(AppFeature.VOICE)
    }

    fun areVoiceFeaturesAvailable(): Boolean {
        return true // Cloud voice always available; Gemma optional for offline
    }

    fun updateSelectedModel(newModel: String) {
        prefs.edit().putString("selected_ai_model", newModel).apply()
        _selectedModel.value = newModel
    }

    fun getMissingModelMessage(feature: AppFeature): String {
        return when (feature) {
            AppFeature.VOICE -> context.getString(R.string.model_missing_voice)
            AppFeature.QUIZ -> context.getString(R.string.model_missing_quiz)
        }
    }

    private fun validatedPath(fileName: String, minSize: Long): String? {
        val file = File(modelsDir, fileName)
        return if (file.exists() && file.length() > minSize) file.absolutePath else null
    }

    companion object {
        @Volatile
        private var instance: ModelAvailability? = null

        fun getInstance(context: Context): ModelAvailability {
            return instance ?: synchronized(this) {
                instance ?: ModelAvailability(context.applicationContext).also { instance = it }
            }
        }
    }
}
