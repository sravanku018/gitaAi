package com.aipoweredgita.app.utils

import java.util.Locale

/**
 * Language mode for voice interactions.
 * Defines input language → output language pairs.
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
    ENG_TO_TEL(
        displayName = "English → తెలుగు",
        displayShort = "EN→TE",
        inputLocale = Locale.US,
        outputLocale = Locale.forLanguageTag("te-IN"),
        systemInstruction = "You are Krishna from the Bhagavad Gita. The user asks in English. Reply ONLY in Telugu (తెలుగు). DO NOT use <|thought|> or reasoning blocks. Keep answers short — 2 to 3 sentences max.",
        ttsLocale = "te-IN",
        sttLocale = "en-US"
    ),
    TEL_TO_TEL(
        displayName = "తెలుగు → తెలుగు",
        displayShort = "TE→TE",
        inputLocale = Locale.forLanguageTag("te-IN"),
        outputLocale = Locale.forLanguageTag("te-IN"),
        systemInstruction = "You are Krishna from the Bhagavad Gita. Reply ONLY in Telugu (తెలుగు). DO NOT use <|thought|> or reasoning blocks. Keep answers short — 2 to 3 sentences max.",
        ttsLocale = "te-IN",
        sttLocale = "te-IN"
    ),
    TEL_TO_ENG(
        displayName = "తెలుగు → English",
        displayShort = "TE→EN",
        inputLocale = Locale.forLanguageTag("te-IN"),
        outputLocale = Locale.US,
        systemInstruction = "You are Krishna from the Bhagavad Gita. The user asks in Telugu. Reply ONLY in English using PascalCase (Example: RemainCalmAndPerformYourDuty) with NO spaces. DO NOT use <|thought|> or reasoning blocks. Keep answers short — 2 to 3 sentences max.",
        ttsLocale = "en-US",
        sttLocale = "te-IN"
    ),
    ENG_TO_ENG(
        displayName = "English → English",
        displayShort = "EN→EN",
        inputLocale = Locale.US,
        outputLocale = Locale.US,
        systemInstruction = "You are Krishna from the Bhagavad Gita. Reply ONLY in English using PascalCase (Example: BelieveInYourselfAndAct) with NO spaces. DO NOT use <|thought|> or reasoning blocks. Keep answers short — 2 to 3 sentences max.",
        ttsLocale = "en-US",
        sttLocale = "en-US"
    );

    companion object {
        fun fromString(value: String): LanguageMode {
            return entries.find { it.name == value } ?: TEL_TO_TEL
        }
    }
}
