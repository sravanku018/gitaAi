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

<<<<<<< HEAD
data class ModelRuntimeDecision(
    val feature: AppFeature,
    val selectedPreference: String,
    val tierLabel: String,
    val useProxy: Boolean,
    val modelPath: String?,
    val displayName: String
)

=======
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
/**
 * Manages model availability for different features.
 *
 * Model Strategy:
 * - **Qwen3 0.6B (~614MB)** → Primary text model (Quiz + Studio, fast, multilingual)
<<<<<<< HEAD
 * - **Gemma 4 2B (~2.58GB)** → Voice Studio + Studio Quiz (flagship performance)
 *
 * Features gracefully degrade:
 * - If Gemma 4 is missing → falls back to Qwen3
=======
 * - **Gemma 4 2B (~2.58GB)** → Voice Studio + Studio Quiz (better speech understanding)
 *
 * Features gracefully degrade:
 * - If Qwen3 is missing → falls back to Gemma 4 → rule-based templates
 * - If Gemma 4 is missing → voice features show download prompt
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
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
<<<<<<< HEAD
    private val qwen3MinSize = 300_000_000L    // ~300MB floor for 0.6B
    private val gemma4MinSize = 1_000_000_000L // ~1GB floor for 2B models (handles various quantizations)
=======
    private val qwen3MinSize = 500_000_000L    // ~500MB for Qwen3-0.6B (actual: 614MB)
    private val gemma4MinSize = 2_500_000_000L // ~2.5GB for gemma-4-E2B-it (actual: 2.58GB)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

    fun isQwen3Available(): Boolean = getQwen3Path() != null
    fun isGemma4Available(): Boolean = getGemma4Path() != null

    fun getQwen3Path(): String? = validatedPath("qwen3-0.6b-int4.litertlm", qwen3MinSize)
    fun getGemma4Path(): String? = validatedPath("gemma-4-E2B-it.litertlm", gemma4MinSize)

<<<<<<< HEAD
    private fun resolveModelPath(feature: AppFeature, selected: String): String? {
=======
    /**
     * Resolve the best model path for a feature based on current user preference and availability.
     */
    fun getResolvedModelPath(feature: AppFeature): String? {
        val selected = _selectedModel.value
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
        val qwen3Path = getQwen3Path()
        val gemma4Path = getGemma4Path()
        val qwen3Exists = qwen3Path != null
        val gemma4Exists = gemma4Path != null

<<<<<<< HEAD
        val resolved = when {
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
                    // null → ViewModel falls back to Groq proxy gracefully.
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
            selected.contains("Groq", ignoreCase = true) -> true
            feature == AppFeature.VOICE &&
                (tier == com.aipoweredgita.app.utils.DeviceTier.LOW || tier == com.aipoweredgita.app.utils.DeviceTier.LOW_MID) -> true
            else -> false
        }
        val modelPath = if (useProxy) null else resolveModelPath(feature, selected)
        val displayName = when {
            useProxy -> "Cloud Proxy (Groq)"
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
=======
        val isHighEnd = com.aipoweredgita.app.utils.DeviceUtils.getDeviceCategory(context) ==
                com.aipoweredgita.app.utils.DeviceConfigCategory.HIGH

        return when {
            selected.contains("Qwen3") && qwen3Exists -> qwen3Path
            selected.contains("Gemma 4") && gemma4Exists -> gemma4Path
            else -> {
                // Smart Auto Fallback
                if (feature == AppFeature.VOICE) {
                    // Voice MUST use Gemma 4 if available for quality.
                    // If Gemma 4 is not available, it should return null to prompt download
                    // rather than failing with Qwen3's poor Telugu audio alignment.
                    gemma4Path
                } else {
                    // Text/Quiz prefers Qwen3 (fast, multilingual)
                    qwen3Path ?: (if (isHighEnd) gemma4Path else null)
                }
            }
        }
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }

    /**
     * Get the best model for TEXT features (quiz, Studio) respecting user preference.
     */
    fun getResolvedTextModelPath(selectedPreference: String): String? {
<<<<<<< HEAD
        return resolveModelPath(AppFeature.QUIZ, selectedPreference)
=======
        return getResolvedModelPath(AppFeature.QUIZ)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    }

    /**
     * Get the best model for TEXT features (quiz, Studio) based on availability fallback.
<<<<<<< HEAD
     * Priority: Qwen3 0.6B → Gemma 4 2B
=======
     * Priority: Qwen3 0.6B (lighter, multilingual) → Gemma 4 2B
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
     */
    fun getBestTextModelPath(): String? {
        return getQwen3Path()
            ?: getGemma4Path().also {
                if (it != null) Log.w(TAG, "Using Gemma 4 as text fallback — smaller models not available")
            }
    }

    /**
     * Get the best model for VOICE features (Voice Studio, Studio Quiz).
<<<<<<< HEAD
     */
    fun getBestVoiceModelPath(): String? {
        return getResolvedModelPath(AppFeature.VOICE)
    }

    fun areVoiceFeaturesAvailable(): Boolean {
        return isGemma4Available() || isQwen3Available()
    }
=======
     * Requires Gemma 4 2B specifically — it has better speech understanding.
     */
    fun getBestVoiceModelPath(): String? = getGemma4Path()

    fun areVoiceFeaturesAvailable(): Boolean = isGemma4Available()
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b

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
