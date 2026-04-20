package com.aipoweredgita.app.util

import android.util.Log
import java.text.Normalizer

object TextUtils {
    private const val TAG = "TextUtils"

    fun sanitizeText(text: String?): String {
        if (text == null) return ""
        return text
            .replace("\\r\\n", " ")
            .replace("\\n", " ")
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("\n{3,}"), "\n\n")
    }

    /**
     * SAFE cleaning that preserves intentional spacing.
     * Removes thinking tags, normalizes Unicode, and excessive whitespace.
     */
    fun cleanLlmOutput(text: String): String {
        if (text.isBlank()) return text

        // Step 1: Normalize Unicode to composed form (NFC)
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)

        // Step 2: Remove thinking/internal tags and markers
        val noTags = normalized
            .replace(Regex("<\\|think.*?\\|>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<thought>.*?</thought>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<channel>.*?</channel>", RegexOption.DOT_MATCHES_ALL), "")
            .replace(Regex("<\\|[^>]+\\|>"), "")
            .replace(Regex("<[^>]+>"), "")

        // Step 3: Remove control characters and artifacts (zero-width spaces, etc.)
        val cleaned = noTags
            .replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]"), "")

        // Step 4: Normalize ONLY excessive whitespace
        return cleaned
            .replace(Regex("[ \t]{2,}"), " ")        // 2+ spaces/tabs → 1 space
            .replace(Regex("\n{3,}"), "\n\n")        // 3+ newlines → 2 newlines
            .replace(Regex("\\s+([,.!?;:])"), "$1")   // Space + punctuation → just punctuation
            .trim()
    }

    /**
     * Additional deep cleaning for final output.
     * Still safe - preserves normal spacing and handles shloka formatting.
     */
    fun deepClean(text: String): String {
        val baseCleaned = cleanLlmOutput(text)

        return baseCleaned
            // Fix Sanskrit/Telugu Shloka formatting
            .replace(Regex("।\\s*"), "।\n")
            .replace(Regex("॥\\s*"), "॥\n\n")
            // Normalize excessive newlines (max 2)
            .replace(Regex("\n{3,}"), "\n\n")
            // Ensure single spaces between words (gentle check)
            .replace(Regex(" +"), " ")
            .trim()
    }
}
