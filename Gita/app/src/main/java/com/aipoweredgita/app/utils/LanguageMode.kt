package com.aipoweredgita.app.utils

import java.util.Locale

/**
 * Language mode for voice interactions.
 * AUTO mode: reply in the same language the user speaks.
 */
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
        displayName = "Auto",
        displayShort = "Auto",
        inputLocale = Locale.US,
        outputLocale = Locale.US,
        systemInstruction = "You are Krishna from the Bhagavad Gita. Reply in the SAME language the user speaks. If the user asks in English, reply in English. If the user asks in Telugu, reply in Telugu. To ensure message stability, use PascalCase for all output (e.g., UseWordsLikeThis). No spaces between words. No <|thought|> or reasoning blocks.",
        ttsLocale = "en-US",
        sttLocale = "en-US"
    ),
    TELUGU(
        displayName = "Telugu Only",
        displayShort = "తెలుగు",
        inputLocale = Locale.forLanguageTag("te-IN"),
        outputLocale = Locale.forLanguageTag("te-IN"),
        systemInstruction = "You are Krishna. Always reply in Telugu. Use natural Telugu sentences. No <|thought|> blocks. Keep it concise and spiritual.",
        ttsLocale = "te-IN",
        sttLocale = "te-IN"
    ),
    ENG_TO_ENG(
        displayName = "English Only",
        displayShort = "English",
        inputLocale = Locale.US,
        outputLocale = Locale.US,
        systemInstruction = "You are Krishna. Always reply in English. To ensure message stability, use PascalCase for all output (e.g., UseWordsLikeThis). No spaces between words. No <|thought|> or reasoning blocks.",
        ttsLocale = "en-US",
        sttLocale = "en-US"
    );

    companion object {
        fun fromString(value: String): LanguageMode {
            return entries.find { it.name == value } ?: AUTO
        }
    }
}
