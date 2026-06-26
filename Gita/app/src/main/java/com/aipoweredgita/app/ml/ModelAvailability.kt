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
 * - **Qwen3 0.6B (~614MB)** → Primary text model (Quiz + Studio, fast, multilingual)
 * - **Gemma 4 2B (~2.58GB)** → Voice Studio + Studio Quiz (flagship performance)
 *
 * Features gracefully degrade:
 * - If Gemma 4 is missing → falls back to Qwen3
 */
class ModelAvailability(appContext: Context) {

    private val TAG = "ModelAvailability"
    private val context = appContext.applicationContext
    private val modelsDir = File(context.filesDir, "ml_models")
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _selectedModel = MutableStateFlow(prefs.getString("selected_ai_model", "Auto (Recommended)") ?: "Auto (Recommended)")
    val selectedModel: StateFlow<String> = _selectedModel.asStateFlow()

    private val preferenceListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == "selected_ai_model") {
            val newVal = sharedPreferences.getString(key, "Auto (Recommended)") ?: "Auto (Recommended)"
            Log.d(TAG, "Model preference changed: $newVal")
            _selectedModel.value = newVal
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(preferenceListener)
    }

    // Model size thresholds for integrity validation
    private val qwen3MinSize = 300_000_000L    // ~300MB floor for 0.6B
    private val gemma4MinSize = 1_000_000_000L // ~1GB floor for 2B models (handles various quantizations)

    fun isQwen3Available(): Boolean = getQwen3Path() != null
    fun isGemma4Available(): Boolean = getGemma4Path() != null

    fun getQwen3Path(): String? = validatedPath("qwen3-0.6b-int4.litertlm", qwen3MinSize)
    fun getGemma4Path(): String? = validatedPath("gemma-4-E2B-it.litertlm", gemma4MinSize)

    private fun resolveModelPath(feature: AppFeature, selected: String): String? {
        val qwen3Path = getQwen3Path()
        val gemma4Path = getGemma4Path()
        val qwen3Exists = qwen3Path != null
        val gemma4Exists = gemma4Path != null

        val resolved = when {
            selected.contains("NVIDIA", ignoreCase = true) -> null
            selected.contains("Groq", ignoreCase = true) -> null
            selected.contains("Qwen3") && qwen3Exists -> {
                if (feature == AppFeature.QUIZ) qwen3Path else null
            }
            selected.contains("Gemma 4") && gemma4Exists -> gemma4Path
            else -> {
                if (feature == AppFeature.QUIZ) {
                    qwen3Path ?: gemma4Path
                } else {
                    // VOICE uses only Gemma; Qwen removed from chat path.
                    // null → ViewModel falls back to cloud proxy gracefully.
                    gemma4Path
                }
            }
        }

        return resolved
    }

    fun getRuntimeDecision(feature: AppFeature): ModelRuntimeDecision {
        val selected = _selectedModel.value
        val tier = com.aipoweredgita.app.utils.DeviceTierDetector.detect(context)
        val useProxy = when {
            selected.contains("NVIDIA", ignoreCase = true) -> true
            selected.contains("Groq", ignoreCase = true) -> true
            feature == AppFeature.VOICE &&
                (tier == com.aipoweredgita.app.utils.DeviceTier.LOW || tier == com.aipoweredgita.app.utils.DeviceTier.LOW_MID) -> true
            else -> false
        }
        val modelPath = if (useProxy) null else resolveModelPath(feature, selected)
        val displayName = when {
            useProxy && selected.contains("NVIDIA", ignoreCase = true) -> "NVIDIA 70B (Cloud)"
            useProxy && selected.contains("Groq", ignoreCase = true) -> "Groq (Cloud)"
            useProxy -> "Cloud Proxy"
            modelPath == null -> "No local model"
            modelPath.contains("qwen3", ignoreCase = true) -> "Qwen3 0.6B"
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

    /**
     * Resolve the best model path for a feature based on current user preference and availability.
     */
    fun getResolvedModelPath(feature: AppFeature): String? {
        return getRuntimeDecision(feature).modelPath
    }

    fun isGemmaRunning(feature: AppFeature): Boolean {
        val path = getRuntimeDecision(feature).modelPath
        return path != null && path.contains("gemma", ignoreCase = true)
    }

    /**
     * Get the best model for TEXT features (quiz, Studio) respecting user preference.
     */
    fun getResolvedTextModelPath(selectedPreference: String): String? {
        return resolveModelPath(AppFeature.QUIZ, selectedPreference)
    }

    /**
     * Get the best model for TEXT features (quiz, Studio) based on availability fallback.
     * Priority: Qwen3 0.6B → Gemma 4 2B
     */
    fun getBestTextModelPath(): String? {
        return getQwen3Path()
            ?: getGemma4Path().also {
                if (it != null) Log.w(TAG, "Using Gemma 4 as text fallback — smaller models not available")
            }
    }

    /**
     * Get the best model for VOICE features (Voice Studio, Studio Quiz).
     */
    fun getBestVoiceModelPath(): String? {
        return getResolvedModelPath(AppFeature.VOICE)
    }

    fun areVoiceFeaturesAvailable(): Boolean {
        return isGemma4Available() || isQwen3Available()
    }

    fun updateSelectedModel(newModel: String) {
        prefs.edit().putString("selected_ai_model", newModel).apply()
        _selectedModel.value = newModel
    }

    /**
     * Get a user-friendly message about what's missing.
     */
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
