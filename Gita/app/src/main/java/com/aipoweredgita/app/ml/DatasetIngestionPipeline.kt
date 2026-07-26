package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.QuizQuestionBank
import com.aipoweredgita.app.database.QuizQuestionBankDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PRODUCTION INGESTION PIPELINE for the Bhagavad Gita QA dataset.
 * 
 * Raw CSV format: chapter_no, verse_no, question_en, question_te, answer_en, answer_te
 * 
 * Pipeline stages:
 * 1. Read bundled offline CSV asset
 * 2. Parse rows
 * 3. Convert Raw Data → MCQ format (generate 3 distractors from answer context)
 * 4. Normalize text (remove quotes, fix encoding)
 * 5. Deduplicate (by question hash)
 * 6. Assign difficulty based on verse complexity
 * 7. Batch insert to DB
 * 
 * Result: Clean, production-ready MCQ questions ready for quiz use without external network dependencies.
 */
class DatasetIngestionPipeline(
    private val context: Context,
    private val questionBankDao: QuizQuestionBankDao
) {
    companion object {
        private const val TAG = "DatasetIngestion"
        private const val BUNDLED_ASSET_NAME = "english_telugu_bilingual.csv"
        private const val COOLDOWN_MS = 24 * 60 * 60 * 1000L
    }

    /**
     * Full pipeline: load from asset → convert → dedup → normalize → store.
     */
    suspend fun ingestDataset(
        language: String = "english",
        batchSize: Int = 500,
        onProgress: (imported: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var totalImported = 0

        Log.d(TAG, "Ingesting $language dataset from local asset $BUNDLED_ASSET_NAME...")

        try {
            val csvContent = readCsvFromAssets(BUNDLED_ASSET_NAME)
            val rawQuestions = parseCsv(csvContent, language)
            Log.d(TAG, "Parsed ${rawQuestions.size} raw questions from $language")

            // STAGE 3: Convert Raw Data → MCQ
            val mcqQuestions = convertToMCQ(rawQuestions)
            Log.d(TAG, "Converted ${mcqQuestions.size} questions to MCQ format")

            // STAGE 4: Normalize
            val normalized = mcqQuestions.map { normalizeQuestion(it) }

            // STAGE 5: Deduplicate (skip if hash already in DB)
            val dedupedQuestions = mutableListOf<QuizQuestionBank>()
            val existingHashes = mutableSetOf<String>()

            for (question in normalized) {
                if (!existingHashes.contains(question.questionHash)) {
                    val count = questionBankDao.countByHash(question.questionHash)
                    if (count == 0) {
                        dedupedQuestions.add(question)
                        existingHashes.add(question.questionHash)
                    }
                }
            }

            Log.d(TAG, "Deduplicated: ${rawQuestions.size} → ${dedupedQuestions.size} unique questions")

            // STAGE 7: Batch insert
            var batchStart = 0
            while (batchStart < dedupedQuestions.size) {
                val batchEnd = minOf(batchStart + batchSize, dedupedQuestions.size)
                val batch = dedupedQuestions.subList(batchStart, batchEnd)
                
                questionBankDao.insertAll(batch)
                
                totalImported += batch.size
                onProgress(totalImported, dedupedQuestions.size)
                
                batchStart = batchEnd
            }

            Log.d(TAG, "✓ Successfully imported ${dedupedQuestions.size} clean $language questions from bundled asset")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ingest $language dataset from asset: ${e.message}", e)
        }

        try {
            questionBankDao.deactivateLowQualityQuestions()
        } catch (_: Exception) { }

        try {
            questionBankDao.applyQualityDecay()
        } catch (_: Exception) { }

        totalImported
    }

    private fun readCsvFromAssets(filename: String): String {
        return context.assets.open(filename).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun parseCsv(csvContent: String, language: String): List<RawQuestion> {
        val questions = mutableListOf<RawQuestion>()
        val lines = csvContent.split("\n").filter { it.trim().isNotEmpty() }

        val dataLines = if (lines.firstOrNull()?.contains("chapter_no", ignoreCase = true) == true) {
            lines.drop(1)
        } else {
            lines
        }

        for (line in dataLines) {
            try {
                val fields = parseCsvLine(line)
                if (fields.size < 4) continue

                val chapterNo = fields[0].trim().toIntOrNull() ?: continue
                val verseNo = fields[1].trim().toIntOrNull() ?: continue

                val qEn = fields.getOrNull(2)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                val qTe = fields.getOrNull(3)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                val aEn = fields.getOrNull(4)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                val aTe = fields.getOrNull(5)?.trim()?.removeSurrounding("\"")?.trim() ?: ""

                when (language.lowercase()) {
                    "telugu" -> {
                        if (qTe.isNotBlank() && aTe.isNotBlank()) {
                            questions.add(RawQuestion(chapterNo, verseNo, qTe, aTe))
                        } else if (qEn.isNotBlank() && aEn.isNotBlank()) {
                            questions.add(RawQuestion(chapterNo, verseNo, qEn, aEn))
                        }
                    }
                    "english" -> {
                        if (qEn.isNotBlank() && aEn.isNotBlank()) {
                            questions.add(RawQuestion(chapterNo, verseNo, qEn, aEn))
                        }
                    }
                    else -> {
                        // "all" or default: include both English and Telugu questions
                        if (qEn.isNotBlank() && aEn.isNotBlank()) {
                            questions.add(RawQuestion(chapterNo, verseNo, qEn, aEn))
                        }
                        if (qTe.isNotBlank() && aTe.isNotBlank()) {
                            questions.add(RawQuestion(chapterNo, verseNo, qTe, aTe))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse line: ${e.message}")
            }
        }

        return questions
    }

    private fun parseCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    fields.add(sb.toString())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        fields.add(sb.toString())
        return fields
    }

    /**
     * STAGE 3: Convert open-ended raw data to MCQ format.
     * Builds an index of topic → answers, then uses answers from same topic as distractors.
     */
    private fun convertToMCQ(rawQuestions: List<RawQuestion>): List<QuizQuestionBank> {
        val topicAnswers = mutableMapOf<String, MutableList<String>>()
        rawQuestions.forEach { q ->
            val topics = extractKeyConcepts(q.answer)
            topics.forEach { topic ->
                topicAnswers.getOrPut(topic) { mutableListOf() }.add(q.answer)
            }
        }

        return rawQuestions.map { raw ->
            val topics = extractKeyConcepts(raw.answer)
            val distractors = getTopicBasedDistractors(raw.answer, topics, topicAnswers, count = 3)
            
            val options = listOf(raw.answer) + distractors
            val shuffledOptions = options.shuffled()
            val correctIndex = shuffledOptions.indexOf(raw.answer)

            val difficulty = estimateDifficulty(raw.chapterNo, raw.verseNo)

            QuizQuestionBank(
                questionHash = "${raw.question.trim().lowercase().hashCode()}",
                questionType = "MCQ",
                difficulty = difficulty,
                question = raw.question,
                chapter = raw.chapterNo,
                verse = raw.verseNo,
                yogaLevel = 0,
                optionA = shuffledOptions.getOrNull(0) ?: "",
                optionB = shuffledOptions.getOrNull(1) ?: "",
                optionC = shuffledOptions.getOrNull(2) ?: "",
                optionD = shuffledOptions.getOrNull(3) ?: "",
                correctAnswer = listOf("A", "B", "C", "D").getOrElse(correctIndex) { "A" },
                explanation = raw.answer,
                keywords = topics.joinToString(","),
                topics = topics.joinToString(","),
                generatedBy = "Bhagavad-Gita-QA Dataset",
                generationMethod = "dataset_import",
                qualityScore = 80f,
                relevanceScore = 75f,
                isVerified = true,
                isApproved = true,
                usageCount = 0
            )
        }
    }

    private fun getTopicBasedDistractors(
        correctAnswer: String,
        topics: List<String>,
        topicAnswers: Map<String, List<String>>,
        count: Int
    ): List<String> {
        val distractors = mutableSetOf<String>()
        
        topics.forEach { topic ->
            val answers = topicAnswers[topic] ?: emptyList()
            answers.filter { it != correctAnswer && it.length > 10 }.forEach { distractors.add(it) }
        }
        
        val generalDistractors = listOf(
            "By performing rituals and ceremonies",
            "By accumulating wealth and power",
            "By avoiding all worldly duties",
            "By seeking personal glory and fame",
            "By following blind tradition without understanding",
            "By relying solely on intellectual knowledge",
            "By isolating oneself from society"
        )
        
        for (d in generalDistractors) {
            if (distractors.size >= count) break
            if (!distractors.contains(d)) {
                distractors.add(d)
            }
        }
        
        return distractors.take(count).toList()
    }

    private fun extractKeyConcepts(answer: String): List<String> {
        val concepts = mutableListOf<String>()
        val lower = answer.lowercase()

        val conceptMap = mapOf(
            "dharma" to listOf("dharma", "duty", "righteous", "moral"),
            "karma" to listOf("karma", "action", "work", "deed"),
            "devotion" to listOf("devotion", "bhakti", "love", "worship"),
            "knowledge" to listOf("knowledge", "wisdom", "understand"),
            "soul" to listOf("soul", "atman", "self", "eternal"),
            "detachment" to listOf("detachment", "desireless", "renounce"),
            "meditation" to listOf("meditation", "concentrate", "focus", "mind"),
            "peace" to listOf("peace", "calm", "equanimity", "joy"),
            "liberation" to listOf("liberation", "moksha", "freedom"),
            "anger" to listOf("anger", "lust", "greed", "passion"),
        )

        conceptMap.forEach { (concept, keywords) ->
            if (keywords.any { lower.contains(it) }) {
                concepts.add(concept)
            }
        }

        return concepts
    }

    private fun normalizeQuestion(question: QuizQuestionBank): QuizQuestionBank {
        fun clean(text: String) = text
            .replace(Regex("\\s+"), " ")
            .replace("\\n", " ")
            .replace("\\r", " ")
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
            .trim()

        return question.copy(
            question = clean(question.question),
            optionA = clean(question.optionA),
            optionB = clean(question.optionB),
            optionC = clean(question.optionC),
            optionD = clean(question.optionD),
            explanation = clean(question.explanation),
            correctAnswer = clean(question.correctAnswer)
        )
    }

    private fun estimateDifficulty(chapter: Int, verse: Int): Int {
        return when {
            chapter <= 6 -> (3 + chapter / 2).coerceIn(1, 10)
            chapter <= 12 -> (5 + chapter / 3).coerceIn(1, 10)
            else -> (7 + chapter / 4).coerceIn(1, 10)
        }
    }

    suspend fun hasQuestions(): Boolean = withContext(Dispatchers.IO) {
        try {
            val count = questionBankDao.getQuestionsBySource("dataset_import")
            count > 100
        } catch (e: Exception) {
            false
        }
    }

    private data class RawQuestion(
        val chapterNo: Int,
        val verseNo: Int,
        val question: String,
        val answer: String
    )
}
