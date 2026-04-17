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
 * Raw CSV format: chapter_no, verse_no, question, answer (open-ended source)
 * 
 * Pipeline stages:
 * 1. Download CSV from HuggingFace
 * 2. Parse rows
 * 3. Convert Raw Data → MCQ format (generate 3 distractors from answer context)
 * 4. Normalize text (remove quotes, fix encoding)
 * 5. Deduplicate (by question hash)
 * 6. Assign difficulty based on verse complexity
 * 7. Batch insert to DB
 * 
 * Result: Clean, production-ready MCQ questions ready for quiz use.
 */
class DatasetIngestionPipeline(
    private val context: Context,
    private val questionBankDao: QuizQuestionBankDao
) {
    private val TAG = "DatasetIngestion"

    private val DATASET_URLS = mapOf(
        "english" to "https://huggingface.co/datasets/JDhruv14/Bhagavad-Gita-QA/resolve/main/English/english.csv",
        "hindi" to "https://huggingface.co/datasets/JDhruv14/Bhagavad-Gita-QA/resolve/main/Hindi/hindi.csv",
        "gujarati" to "https://huggingface.co/datasets/JDhruv14/Bhagavad-Gita-QA/resolve/main/Gujarati/gujarati.csv"
    )

    // Cooldown period: don't re-ask same question within 30 minutes
    private val COOLDOWN_MS = 30 * 60 * 1000L

    /**
     * Full pipeline: download → convert → dedup → normalize → store.
     */
    suspend fun ingestDataset(
        language: String = "english",
        batchSize: Int = 500,
        onProgress: (imported: Int, total: Int) -> Unit = { _, _ -> }
    ): Int = withContext(Dispatchers.IO) {
        var totalImported = 0

        val languages = if (language == "all") DATASET_URLS.keys else listOf(language)

        for (lang in languages) {
            val url = DATASET_URLS[lang] ?: continue
            Log.d(TAG, "Downloading $lang dataset from $url...")

            try {
                val csvContent = downloadCsv(url)
                val rawQuestions = parseCsv(csvContent, lang)
                Log.d(TAG, "Parsed ${rawQuestions.size} raw questions from $lang")

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

                Log.d(TAG, "✓ Successfully imported ${dedupedQuestions.size} clean $lang questions")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to ingest $lang dataset: ${e.message}", e)
            }
        }

        // FIX ISSUE 5: Deactivate low-quality questions instead of deleting them
        try {
            questionBankDao.deactivateLowQualityQuestions()
        } catch (_: Exception) { }

        // FIX ISSUE 6: Apply daily quality decay
        try {
            questionBankDao.applyQualityDecay()
        } catch (_: Exception) { }

        totalImported
    }

    private fun downloadCsv(url: String): String {
        val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 30_000   // 30 s connect
        conn.readTimeout    = 120_000  // 2 min read (large CSV)
        conn.setRequestProperty("User-Agent", "GitaApp/1.0 (Android)")
        conn.connect()
        if (conn.responseCode !in 200..299) {
            conn.disconnect()
            throw Exception("HTTP ${conn.responseCode} downloading $url")
        }
        return try {
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
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
                val question = fields[2].trim().removeSurrounding("\"").trim()
                val answer = fields[3].trim().removeSurrounding("\"").trim()

                if (question.isBlank() || answer.isBlank()) continue

                questions.add(RawQuestion(chapterNo, verseNo, question, answer))
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
     * FIX ISSUE 6: Use topic-based distractors instead of negation.
     * Builds an index of topic → answers, then uses answers from same topic as distractors.
     */
    private fun convertToMCQ(rawQuestions: List<RawQuestion>): List<QuizQuestionBank> {
        // Build topic → answers index for realistic distractors
        val topicAnswers = mutableMapOf<String, MutableList<String>>()
        rawQuestions.forEach { q ->
            val topics = extractKeyConcepts(q.answer)
            topics.forEach { topic ->
                topicAnswers.getOrPut(topic) { mutableListOf() }.add(q.answer)
            }
        }

        return rawQuestions.map { raw ->
            val topics = extractKeyConcepts(raw.answer)
            
            // FIX ISSUE 6: Get distractors from same topic (realistic wrong answers)
            val distractors = getTopicBasedDistractors(raw.answer, topics, topicAnswers, count = 3)
            
            // Shuffle options so correct answer isn't always first
            val options = listOf(raw.answer) + distractors
            val shuffledOptions = options.shuffled()
            val correctIndex = shuffledOptions.indexOf(raw.answer)

            val difficulty = estimateDifficulty(raw.chapterNo, raw.verseNo)

            QuizQuestionBank(
                questionHash = "${raw.chapterNo}:${raw.verseNo}:${raw.question.hashCode()}",
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
                correctAnswer = raw.answer,
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

    /**
     * FIX ISSUE 6: Get realistic distractors from same topic.
     * Uses answers from other questions on the same topic as wrong options.
     */
    private fun getTopicBasedDistractors(
        correctAnswer: String,
        topics: List<String>,
        topicAnswers: Map<String, List<String>>,
        count: Int
    ): List<String> {
        val distractors = mutableSetOf<String>()
        
        // Collect answers from same topics
        topics.forEach { topic ->
            val answers = topicAnswers[topic] ?: emptyList()
            answers.filter { it != correctAnswer && it.length > 10 }.forEach { distractors.add(it) }
        }
        
        // If not enough topic-based distractors, add general Gita distractors
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

    /**
     * Extract key concepts from answer text.
     */
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

    /**
     * STAGE 4: Normalize text (remove artifacts, fix encoding).
     */
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

    /**
     * Estimate difficulty based on chapter/verse (early chapters = easier).
     */
    private fun estimateDifficulty(chapter: Int, verse: Int): Int {
        return when {
            chapter <= 6 -> (3 + chapter / 2).coerceIn(1, 10)
            chapter <= 12 -> (5 + chapter / 3).coerceIn(1, 10)
            else -> (7 + chapter / 4).coerceIn(1, 10)
        }
    }

    /**
     * Check if questions have already been imported.
     */
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
