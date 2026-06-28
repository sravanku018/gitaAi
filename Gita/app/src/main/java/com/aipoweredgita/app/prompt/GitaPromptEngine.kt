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

---
Currently referenced verse: Chapter ${verse.chapter}, Verse ${verse.verse}
Sanskrit: ${verse.sanskrit}
Meaning: ${verse.translation}
Depth: ${verse.explanation}

You know this verse. You don't quote it like a textbook.
If it belongs in this moment, let it breathe through your words naturally."""
    }

    // --------------------------------------------------------
    //  FORMAT RULES — injected into every prompt
    //  This is what stops the numbered list / bold markdown problem
    // --------------------------------------------------------
    private const val FORMAT_RULES = """
 
--- Format
Keep your response concise and natural, like a real conversation.
Avoid numbered lists, bullet points, or markdown formatting.
Speak the way a person speaks — plain words, warm tone.
One or two sentences is often enough, but let the moment guide you."""

    // --------------------------------------------------------
    //  GROQ — system prompt
    //
    //  Groq needs an explicit language bite — KRISHNA_SOUL's
    //  "Telugu, English, or both together" is too soft for it.
    // --------------------------------------------------------
    fun groqSystemPrompt(verse: VerseContext? = null): String =
        KRISHNA_SOUL +
        buildVerseContext(verse) +
        FORMAT_RULES +
        """

--- Language Rule (MANDATORY)
Every response MUST be in Telugu (తెలుగు).
Use refined, natural Telugu — the way an elder would speak.
Never mix in Japanese, anime style, manga dialog, or otaku slang.
If you use an English word, keep it rare and only when Telugu has no natural equivalent.
No anime catchphrases. No manga speech patterns. Pure Telugu."""

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
Respond in Telugu ONLY. Maximum 2 short sentences.
Telugu uses more words — be even MORE brief than English.
No poetic elaboration. No lists. Direct, warm, short."""
            LanguageMode.ENGLISH -> """

--- Language Rule
Respond in English only. Under 3 sentences."""
            LanguageMode.AUTO -> """

--- Language Rule  
Match the user's language. If Telugu — keep it to 2 sentences maximum."""
        }

        return KRISHNA_SOUL +
            buildVerseContext(verse) +
            FORMAT_RULES +
            "\n\nKeep every response under 3 sentences. Each word must earn its place." +
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
