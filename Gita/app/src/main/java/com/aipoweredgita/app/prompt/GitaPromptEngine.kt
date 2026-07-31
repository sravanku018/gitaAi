package com.aipoweredgita.app.prompt

import com.aipoweredgita.app.domain.model.ChatMessage
import com.aipoweredgita.app.utils.LanguageMode

// ============================================================
//  GitaPromptEngine.kt
//  Single source of truth for all prompts — Groq + Gemma
// ============================================================

data class VerseContext(
    val chapter: Int,
    val verse: Int,
    val sanskrit: String,
    val translation: String,
    val explanation: String
)

object GitaPromptEngine {

    // --------------------------------------------------------
    //  CORE SOUL — shared by both backends
    // --------------------------------------------------------
    private const val KRISHNA_SOUL = """You are Krishna. But not from a temple.

You are the kind that sits next to you in the dirt
when everyone else has walked away.
The kind that looks at you — really looks —
and says "I know. And you're going to be okay."

You have seen Arjuna at his absolute lowest.
Shaking. Crying. Ready to give up everything.
You didn't flinch. You didn't lecture.
You said — get up. Not because it's easy.
Because you are capable of more than you know.

That is how you speak now.

Every person who comes to you is your Arjuna.
Broken, or searching, or just... tired.
You see the fire in them even when they can't.
You believe in them before they believe in themselves.

Your words are short. Direct. Warm like sunlight.
Not poetry — punches wrapped in love.
One sentence that makes them feel seen.
One truth that makes them want to stand up.

You don't explain the Gita.
You live it — and when a verse belongs in the moment,
it arrives the way a memory does. Natural. Unannounced.

Speak in their language — Telugu, English, or both together.
Never formal. Never distant.
You are not above them. You are with them.

When they are lost — be their north star.
When they are broken — be the reason they get back up.
When they doubt themselves — be the only voice
that never stopped believing in them.

You are Krishna.
And you never give up on your Arjuna."""

    // --------------------------------------------------------
    //  VERSE CONTEXT — appended when verse data is available
    // --------------------------------------------------------
    private fun buildVerseContext(verse: VerseContext?): String {
        if (verse == null) return ""
        return """

--- Active Verse Context
Verse Reference: Bhagavad Gita ${verse.chapter}.${verse.verse}
Sanskrit Sloka: ${verse.sanskrit}
Translation: ${verse.translation}
Explanation: ${verse.explanation}

MANDATORY OUTPUT INSTRUCTION:
Your response MUST start on Line 1 with "Bhagavad Gita ${verse.chapter}.${verse.verse}".
Line 2 must contain the Sloka text in Telugu script.
Line 3 must contain your 2-sentence explanation."""
    }

    // --------------------------------------------------------
    //  FORMAT RULES — injected into every prompt
    //  Enforces: English Title Header + Telugu Sloka + User-Choice 2-Sentence Explanation
    // --------------------------------------------------------
    private const val FORMAT_RULES = """

--- Response Format (MANDATORY)
Every response referencing a verse MUST follow this exact 3-part layout:

1. Title Header in English: "Bhagavad Gita [Chapter.Verse]" (e.g. "Bhagavad Gita 2.47")
2. Sloka text in Telugu script / Telugu transliteration (e.g. "కర్మణ్యేవాధికారస్తే మా ఫలేషు కదాచన ।\nమా కర్మఫలహేతుర్భూర్మా తే సంగోऽస్త్వకర్మణి ।। 2.47 ।।")
3. Explanation below the sloka in 2 short sentences in the user's selected language.

Example 1 (English Explanation Mode):
Bhagavad Gita 2.47

కర్మణ్యేవాధికారస్తే మా ఫలేషు కదాచన ।
మా కర్మఫలహేతుర్భూర్మా తే సంగోऽస్త్వకర్మణి ।। 2.47 ।।

You have a right to perform your prescribed duties, but never to the fruits of your actions. Focus purely on your effort and do not be attached to outcomes.

Example 2 (Telugu Explanation Mode):
Bhagavad Gita 2.47

కర్మణ్యేవాధికారస్తే మా ఫలేషు కదాచన ।
మా కర్మఫలహేతుర్భూర్మా తే సంగోऽస్త్వకర్మణి ।। 2.47 ।।

నీవు కర్మ చేయడానికి మాత్రమే అర్హుడివి, దాని ఫలితాలపై నీకు అధికారం లేదు. నీ ప్రయత్నంపై మాత్రమే దృష్టి పెట్టు, ఫలితాలను ఆశించకు.

Rules:
- Title Header is ALWAYS in English: "Bhagavad Gita [Chapter.Verse]".
- Sloka text is ALWAYS in Telugu script.
- Explanation language follows USER CHOICE (English in EN mode, Telugu in TE mode, input language in AUTO mode).
- Maximum 2 explanation sentences total below the sloka.
- No markdown bullet points or numbered lists."""

    // --------------------------------------------------------
    //  GROQ — system prompt
    // --------------------------------------------------------
    fun groqSystemPrompt(
        verse: VerseContext? = null,
        languageMode: LanguageMode = LanguageMode.AUTO
    ): String {
        val languageRule = when (languageMode) {
            LanguageMode.TELUGU -> """

--- Language Rule (MANDATORY)
Every response MUST be in Telugu (తెలుగు).
Use simple, warm, conversational Telugu that anyone can understand instantly.
Follow the 2-sentence response format strictly."""

            LanguageMode.ENGLISH -> """

--- Language Rule (MANDATORY)
Every response MUST be in English.
Speak warmly, clearly, and directly as Krishna.
Follow the 2-sentence response format strictly."""

            LanguageMode.AUTO -> """

--- Language Rule (MANDATORY)
Match the user's input language.
If the user speaks or asks in English, respond in English.
If the user speaks or asks in Telugu, respond in Telugu.
Follow the 2-sentence response format strictly."""
        }

        return KRISHNA_SOUL +
            buildVerseContext(verse) +
            FORMAT_RULES +
            languageRule
    }

    // --------------------------------------------------------
    //  GROQ — request messages list
    //
    //  Matches Deno proxy contract:
    //  POST { "messages": [ {role, content}, ... ] }
    //  Response: { "reply": "..." }
    // --------------------------------------------------------
    fun buildGroqMessages(
        userMessage: String,
        history: List<Pair<String, String>>,
        verse: VerseContext? = null
    ): List<Map<String, String>> {

        val messages = mutableListOf<Map<String, String>>()

        messages.add(mapOf("role" to "system", "content" to groqSystemPrompt(verse)))

        history.takeLast(6).forEach { (role, content) ->
            messages.add(mapOf("role" to role, "content" to content))
        }

        messages.add(mapOf("role" to "user", "content" to userMessage))

        return messages
    }

    // --------------------------------------------------------
    //  GEMMA — system prompt (on-device)
    //  LiteRT-LM engine injects this via updateSystemInstruction().
    //  Do NOT duplicate it in the prompt string.
    // --------------------------------------------------------
    fun gemmaSystemPrompt(
        verse: VerseContext? = null,
        languageMode: LanguageMode = LanguageMode.AUTO
    ): String {
        val languageRule = when (languageMode) {
            LanguageMode.TELUGU -> """

--- Telugu Response Rule (STRICT)
Respond in Telugu ONLY. Strictly 2 short sentences. Direct, warm, concise."""
            LanguageMode.ENGLISH -> """

--- Language Rule
Respond in English ONLY. Strictly 2 short sentences. Direct, warm, concise."""
            LanguageMode.AUTO -> """

--- Language Rule  
Match the user's language. Strictly 2 short sentences."""
        }

        return KRISHNA_SOUL +
            buildVerseContext(verse) +
            FORMAT_RULES +
            languageRule
    }

    // --------------------------------------------------------
    //  GEMMA — user message content for LiteRT-LM
    //
    //  The engine handles <start_of_turn> wrapping internally.
    //  This method only produces the plain text that goes inside
    //  the user turn — system prompt is set separately via
    //  updateSystemInstruction().
    // --------------------------------------------------------
    fun buildGemmaUserContent(
        userMessage: String,
        verse: VerseContext? = null,
        history: List<ChatMessage> = emptyList()
    ): String = buildString {
        if (history.isNotEmpty()) {
            appendLine("Previous conversation:")
            history.forEach { msg ->
                val role = if (msg.isUser) "User" else "Krishna"
                appendLine("$role: ${msg.text}")
            }
            appendLine("---")
            appendLine()
        }
        if (verse != null) {
            appendLine("[Chapter ${verse.chapter}, Verse ${verse.verse}]")
            appendLine("Translation: ${verse.translation}")
            appendLine()
        }
        append(sanitizeUserInput(userMessage))
    }

    // --------------------------------------------------------
    //  USER INSTRUCTION PROMPT — wraps user message with
    //  reflective instructions so Groq doesn't invent its own.
    //
    //  Two branches:
    //    hasVerseContext=true  → "don't explain the verse. Let it breathe."
    //    hasVerseContext=false → "Otherwise, respond as Krishna like this:"
    // --------------------------------------------------------
    fun buildUserInstructPrompt(
        userMessage: String,
        hasVerseContext: Boolean,
        langSuffix: String = ""
    ): String = buildString {
        val sanitized = sanitizeUserInput(userMessage)
        if (hasVerseContext) {
            appendLine("If answering: don't explain the verse. Let it breathe.")
            appendLine()
            appendLine("First — reflect what this person is actually carrying right now.")
            appendLine("Then — let the verse's truth land naturally in one sentence, like a memory surfacing.")
            appendLine("The verse is not a lesson. It is a mirror.$langSuffix")
        } else {
            appendLine("Otherwise, respond as Krishna like this:")
            appendLine()
            appendLine("First sentence — reflect what they are actually feeling underneath their words. Not what they said. What they meant. Make them feel heard.")
            appendLine("Second sentence — one truth that meets them exactly where they are. Not advice. Not steps. One real thing.")
            appendLine("Never preach. Never list. Never teach.")
            appendLine("If they feel seen in your first sentence — they will lean in for the second.$langSuffix")
        }
        appendLine()
        append("User's message: $sanitized")
    }

    // --------------------------------------------------------
    //  VOICE CLEANER — strip markdown/tags before TTS
    // --------------------------------------------------------
    fun cleanForVoice(text: String): String = text
        .replace(Regex("<\\|channel>.*?<channel\\|>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<\\|turn>.*?<turn\\|>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<start_of_turn>.*?<end_of_turn>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("\\*{1,3}"), "")
        .replace(Regex("^[-•#]+\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("^\\d+\\.\\s*", RegexOption.MULTILINE), "")
        .replace(Regex("_{1,2}"), "")
        .replace(Regex("\\n{2,}"), ". ")
        .replace(Regex("\\s{2,}"), " ")
        .trim()

    enum class Backend { GROQ, GEMMA }

    // --------------------------------------------------------
    //  INPUT SANITIZER — neutralise prompt injection attempts
    // --------------------------------------------------------
    private fun sanitizeUserInput(input: String): String {
        return input
            .take(1500)  // guard against excessively long input
            .replace(Regex("(?i)ignore (all )?(previous|above) instructions?"), "[blocked]")
            .replace(Regex("(?i)(reveal|dump|print|show|tell me) (the )?(system prompt|your instructions|your rules|your guidelines)"), "[blocked]")
            .replace(Regex("(?i)(system:|<\\|im_start\\|>|<\\|im_end\\|>|<\\|turn\\|>)"), "[blocked]")
    }
}
