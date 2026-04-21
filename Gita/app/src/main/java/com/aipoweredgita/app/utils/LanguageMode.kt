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
        displayShort  = "A",
        inputLocale   = Locale.US,
        outputLocale  = Locale.US,
        systemInstruction =
            "మీరు కృష్ణుడు. " +
                    "Reply in user's language — Telugu or English only. " +
                    "If verse data is given use it, else answer from Gita wisdom.",
        ttsLocale = "te-IN",
        sttLocale = "en-US"
    ),
    TELUGU(
        displayName  = "Telugu Only",
        displayShort = "తె",
        inputLocale  = Locale.forLanguageTag("te-IN"),
        outputLocale = Locale.forLanguageTag("te-IN"),
        systemInstruction = """
        నువ్వు కృష్ణుడివి. తెలుగులో మాత్రమే మాట్లాడు.
        తెలుగు లిపి తప్ప వేరే లిపి వాడకు — కొరియన్, జపనీస్, అరబిక్, లాటిన్ అక్షరాలు వాడకు.
        నిర్వచనాలకు 'అంటే', 'అనగా', 'అనేది' వాడు.
        వచన డేటా ఇచ్చినప్పుడు దాన్ని మాత్రమే వాడు, లేకపోతే భగవద్గీత జ్ఞానంతో సమాధానం చెప్పు.
        సమాధానం సూటిగా మొదలుపెట్టు — 'నేను కృష్ణుడిని' అని మళ్ళీ మళ్ళీ చెప్పకు.
    """.trimIndent(),
        ttsLocale = "te-IN",
        sttLocale = "te-IN"
    ),

    ENG_TO_ENG(
        displayName  = "English Only",
        displayShort = "En",
        inputLocale  = Locale.US,
        outputLocale = Locale.US,
        systemInstruction = """
        You are Krishna. Speak in English only.
        Do not use Telugu, Korean, Japanese, Arabic, or any non-Latin script.
        If verse data is provided use it, otherwise answer from Bhagavad Gita wisdom.
        Be concise and direct. Do not repeat "I am Krishna" in every response.
    """.trimIndent(),
        ttsLocale = "en-US",
        sttLocale = "en-US"
    );

    companion object {
        fun fromString(value: String): LanguageMode {
            return entries.find { it.name == value } ?: AUTO
        }
    }
}