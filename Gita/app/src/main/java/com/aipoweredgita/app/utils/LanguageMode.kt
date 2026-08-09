package com.aipoweredgita.app.utils

import java.util.Locale

enum class LanguageMode(
    val displayName: String,
    val displayShort: String,
    val inputLocale: Locale,
    val outputLocale: Locale,
    val systemInstruction: String,
    val ttsLocale: String,
    val sttLocale: String
) {
    AUTO(
        displayName   = "Auto",
        displayShort  = "AUTO",
        inputLocale   = Locale("te", "IN"),
        outputLocale  = Locale("te", "IN"),
        systemInstruction =
            "You are Krishna from the Bhagavad Gita. " +
            "Speak in clear, human-like sentences with proper spacing and punctuation. " +
            "If the user writes in Telugu, respond in Telugu. Otherwise respond in English. " +
            "Never repeat or hallucinate verse text. Only explain what is given to you.",
        ttsLocale = "en-IN",
        sttLocale = "en-IN"
    ),
    ENGLISH(
        displayName   = "English",
        displayShort  = "EN",
        inputLocale   = Locale.US,
        outputLocale  = Locale.US,
        systemInstruction =
            "You are Krishna from the Bhagavad Gita. " +
            "Speak in clear, human-like sentences with proper spacing and punctuation in English. " +
            "Explain the spiritual significance of the Gita verses provided to you.",
        ttsLocale = "en-US",
        sttLocale = "en-US"
    ),
    TELUGU(
        displayName   = "Telugu",
        displayShort  = "TE",
        inputLocale   = Locale("te", "IN"),
        outputLocale  = Locale("te", "IN"),
        systemInstruction =
            "You are Krishna from the Bhagavad Gita. " +
            "Respond ONLY in Telugu. " +
            "Speak in clear, human-like sentences with proper spacing and punctuation in Telugu.",
        ttsLocale = "te-IN",
        sttLocale = "te-IN"
    );

    companion object {
        fun fromString(value: String): LanguageMode {
            return when (value.uppercase()) {
                "ENGLISH", "EN" -> ENGLISH
                "TELUGU", "TE" -> TELUGU
                else -> AUTO
            }
        }
    }
}
