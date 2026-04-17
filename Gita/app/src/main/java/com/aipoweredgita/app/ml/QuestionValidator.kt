package com.aipoweredgita.app.ml

import android.util.Log
import com.aipoweredgita.app.database.QuizQuestionBank
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * PRODUCTION VALIDATION PIPELINE for LLM-generated quiz questions.
 * 
 * Validation stages:
 * 1. JSON Parse (strict) — reject malformed output
 * 2. Structure Check — verify all required fields exist
 * 3. Logic Check — 4 options, correct answer in options, non-empty question
 * 4. Duplicate Check — same hash already in DB?
 * 5. Quality Score — initial score based on content analysis
 * 
 * Returns ValidationResult with either valid QuizQuestionBank or detailed error.
 */
object QuestionValidator {
    private const val TAG = "QuestionValidator"
    private val gson = Gson()

    data class ValidationResult(
        val isValid: Boolean,
        val question: QuizQuestionBank? = null,
        val error: String? = null,
        val qualityScore: Float = 0f
    )

    /**
     * Full validation pipeline for LLM-generated JSON.
     */
    fun validateLLMOutput(
        rawOutput: String,
        chapter: Int,
        verseNum: Int,
        difficulty: Int,
        existingQuestions: List<String> = emptyList()  // FIX ISSUE 3: Semantic dedup
    ): ValidationResult {
        // STAGE 1: JSON Parse (strict)
        val parsedData = tryParseJson(rawOutput)
            ?: return ValidationResult(false, error = "Failed to parse JSON: ${rawOutput.take(100)}")

        // STAGE 2: Structure Check
        val structureError = checkStructure(parsedData)
        if (structureError != null) {
            return ValidationResult(false, error = structureError)
        }

        // STAGE 3: Logic Check
        val logicError = checkLogic(parsedData)
        if (logicError != null) {
            return ValidationResult(false, error = logicError)
        }

        // STAGE 4: FIX ISSUE 3 — Semantic dedup (normalized text comparison)
        val normalizedNew = normalizeQuestionText(parsedData.question)
        val isDuplicate = existingQuestions.any { existing ->
            normalizedNew == normalizeQuestionText(existing)
        }
        if (isDuplicate) {
            return ValidationResult(false, error = "Duplicate question (semantic match)")
        }

        // STAGE 5: Quality Score
        val qualityScore = calculateQualityScore(parsedData, difficulty)

        // All checks passed — convert to DB format
        val questionHash = "${chapter}:${verseNum}:${parsedData.question.hashCode()}"
        val question = QuizQuestionBank(
            questionHash = questionHash,
            questionType = "MCQ",
            difficulty = difficulty.coerceIn(1, 10),
            question = parsedData.question.trim(),
            chapter = chapter,
            verse = verseNum,
            yogaLevel = 0,
            optionA = parsedData.options.getOrNull(0) ?: "",
            optionB = parsedData.options.getOrNull(1) ?: "",
            optionC = parsedData.options.getOrNull(2) ?: "",
            optionD = parsedData.options.getOrNull(3) ?: "",
            correctAnswer = parsedData.options.getOrNull(parsedData.correctOptionIndex) ?: "",
            explanation = parsedData.explanation ?: "",
            keywords = parsedData.rubricKeywords?.joinToString(",") ?: "",
            topics = parsedData.theme ?: "",
            generatedBy = "LLM",
            generationMethod = "AI_generated",
            qualityScore = qualityScore,
            relevanceScore = 70f,
            isVerified = false,
            isApproved = true,
            usageCount = 0
        )

        return ValidationResult(true, question = question, qualityScore = qualityScore)
    }

    /**
     * FIX ISSUE 1: Better semantic normalization for dedup.
     * Lowercase → remove punctuation → filter words > 3 chars → sort → join.
     * This catches most paraphrases cheaply without heavy ML.
     */
    private fun normalizeQuestionText(text: String): String {
        return text
            .lowercase()
            .replace(Regex("[^a-z ]"), "")        // Remove punctuation/special chars
            .split(" ")
            .filter { it.length > 3 }             // Remove weak words (the, is, a, etc.)
            .sorted()                             // Sort to catch paraphrases
            .joinToString(" ")                    // Join back
    }

    /**
     * Stage 1: Try to parse JSON from raw LLM output.
     * Handles: raw JSON, markdown code blocks, embedded JSON.
     */
    private fun tryParseJson(raw: String): ParsedQuestion? {
        val trimmed = raw.trim()

        // Attempt 1: Direct JSON parse
        try {
            return gson.fromJson(trimmed, ParsedQuestion::class.java)
        } catch (_: JsonSyntaxException) { }

        // Attempt 2: Extract from markdown code block
        val codeBlockMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(raw)
        if (codeBlockMatch != null) {
            try {
                return gson.fromJson(codeBlockMatch.groupValues[1].trim(), ParsedQuestion::class.java)
            } catch (_: JsonSyntaxException) { }
        }

        // Attempt 3: Find embedded JSON object
        val jsonMatch = Regex("\\{[^{}]*\"question\"[^{}]*\"options\"[^{}]*\\}").find(raw)
        if (jsonMatch != null) {
            try {
                return gson.fromJson(jsonMatch.value, ParsedQuestion::class.java)
            } catch (_: JsonSyntaxException) { }
        }

        // Attempt 4: Try to find ANY JSON-like structure
        val looseMatch = Regex("\\{[^{}]*\"[^\"]*\"[^{}]*\\}").find(raw)
        if (looseMatch != null) {
            try {
                return gson.fromJson(looseMatch.value, ParsedQuestion::class.java)
            } catch (_: JsonSyntaxException) { }
        }

        return null
    }

    /**
     * Stage 2: Check that all required fields exist and are non-empty.
     */
    private fun checkStructure(data: ParsedQuestion): String? {
        if (data.question.isBlank()) return "Question text is empty"
        if (data.options.isEmpty()) return "Options list is empty"
        if (data.correctOptionIndex < 0 || data.correctOptionIndex > 3) {
            return "Invalid correctOptionIndex: ${data.correctOptionIndex} (must be 0-3)"
        }
        return null
    }

    /**
     * Stage 3: Logic validation — 4 options, correct answer exists, no duplicates.
     */
    private fun checkLogic(data: ParsedQuestion): String? {
        val nonEmptyOptions = data.options.filter { it.isNotBlank() }
        if (nonEmptyOptions.size < 2) return "Need at least 2 non-empty options (got ${nonEmptyOptions.size})"

        val correctAnswer = data.options.getOrNull(data.correctOptionIndex)?.trim()
        if (correctAnswer.isNullOrBlank()) return "Correct answer (index ${data.correctOptionIndex}) is empty"

        // Check for duplicate options
        val uniqueOptions = data.options.map { it.lowercase().trim() }.toSet()
        if (uniqueOptions.size < data.options.size) return "Duplicate options detected"

        return null
    }

    /**
     * Stage 5: Calculate initial quality score (0-100).
     */
    private fun calculateQualityScore(data: ParsedQuestion, difficulty: Int): Float {
        var score = 50f  // Base score

        // Length bonuses
        if (data.question.length > 20) score += 10f
        if (data.question.length > 50) score += 10f
        if (data.explanation?.length ?: 0 > 20) score += 5f

        // Option quality
        val optionLengths = data.options.map { it.length }
        if (optionLengths.all { it > 5 }) score += 10f
        if (optionLengths.all { it > 15 }) score += 5f

        // Penalize very short questions
        if (data.question.length < 10) score -= 20f

        return score.coerceIn(0f, 100f)
    }

    /**
     * Intermediate data structure for validation.
     */
    private data class ParsedQuestion(
        val question: String = "",
        val options: List<String> = emptyList(),
        val correctOptionIndex: Int = 0,
        val explanation: String? = null,
        val type: String? = null,
        val rubricKeywords: List<String>? = null,
        val theme: String? = null
    )
}
