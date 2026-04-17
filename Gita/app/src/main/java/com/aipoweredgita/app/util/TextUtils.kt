package com.aipoweredgita.app.util

import java.text.Normalizer

object TextUtils {
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
     * Sanitize LLM output for display and TTS.
     * Removes control characters, normalizes Unicode to NFC,
     * and strips common garbage artifacts from LiteRT-LM output.
     */
    fun cleanLlmOutput(text: String): String {
        if (text.isBlank()) return text

        // Step 1: Normalize Unicode to composed form (NFC)
        // This fixes decomposed Telugu characters (vowel signs, conjuncts)
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFC)

        // Step 2: Remove control characters (except newline, tab)
        val cleaned = normalized.replace(Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]"), "")

        // Step 3: Remove common LLM artifacts
        val noArtifacts = cleaned
            // Remove zero-width spaces and soft hyphens
            .replace(Regex("[\\u200B\\u200C\\u200D\\uFEFF\\u00AD]"), "")
            // Remove isolated combining diacritics (not attached to a base char)
            .replace(Regex("(?<!\\p{L})[\\u0300-\\u036F]"), "")
            // Strip HTML-like tags that some LLMs emit
            .replace(Regex("<[^>]+>"), "")

        // Step 4: Clean up whitespace
        return noArtifacts
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    /**
     * ADVANCED: Deep clean text for final display and TTS.
     * Use this in a background process to refine the raw stream.
     */
    fun deepClean(text: String): String {
        val baseCleaned = cleanLlmOutput(text)

        return baseCleaned
            // Fix Sanskrit/Telugu Shloka formatting
            .replace(Regex("([।॥])\\s*"), "$1\n")
            // Remove lingering LLM markers
            .replace(Regex("<\\|[^>]+\\|>"), "")
            // Fix punctuation spacing
            .replace(Regex("\\s+([,.?!;])"), "$1")
            // Ensure single spaces between words
            .replace(Regex(" +"), " ")
            .trim()
    }
}
