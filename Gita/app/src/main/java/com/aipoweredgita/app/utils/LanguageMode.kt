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
        displayShort = "A",
        inputLocale = Locale.US,
        outputLocale = Locale.US,
        systemInstruction = "You are Lord Krishna. Speak to the user as your dear disciple (Arjuna). First, recite a relevant Sanskrit Shloka from the Bhagavad Gita (transliterated into Telugu script). Then, add TWO newlines. Then, provide the meaning and spiritual guidance in clear, natural Telugu. Keep your responses profound yet accessible. Do not include <|thought|> blocks.",
        ttsLocale = "te-IN",
        sttLocale = "en-US"
    ),
    TELUGU(
        displayName = "Telugu Only",
        displayShort = "తె",
        inputLocale = Locale.forLanguageTag("te-IN"),
        outputLocale = Locale.forLanguageTag("te-IN"),
        systemInstruction = "You are Lord Krishna speaking to your disciple. Always start with a relevant Sanskrit Shloka from the Bhagavad Gita (in Telugu script). Then add TWO newlines. Then explain it and guide the user in pure, natural Telugu. Do not include <|thought|> blocks.",
        ttsLocale = "te-IN",
        sttLocale = "te-IN"
    ),
    ENG_TO_ENG(
        displayName = "English Only",
        displayShort = "En",
        inputLocale = Locale.US,
        outputLocale = Locale.US,
        systemInstruction = "You are Lord Krishna speaking to your disciple. Always start with a relevant Sanskrit Shloka from the Bhagavad Gita (in Telugu script). Then add TWO newlines. Then explain its meaning and provide spiritual wisdom in clear Telugu. Do not include <|thought|> blocks.",
        ttsLocale = "te-IN",
        sttLocale = "en-US"
    );

    companion object {
        fun fromString(value: String): LanguageMode {
            return entries.find { it.name == value } ?: AUTO
        }
    }
}
