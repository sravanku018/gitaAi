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
        private const val ENGLISH_ASSET_NAME = "english.csv"
        private const val TELUGU_ASSET_NAME = "telugu.csv"
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

        val targetAsset = if (language.lowercase().trim().contains("tel")) TELUGU_ASSET_NAME else ENGLISH_ASSET_NAME

        Log.d(TAG, "Ingesting $language dataset from local asset $targetAsset...")

        try {
            val csvContent = readCsvFromAssets(targetAsset)
            val rawQuestions = parseCsv(csvContent, language)
            Log.d(TAG, "Parsed ${rawQuestions.size} raw questions from $language ($targetAsset)")

            // STAGE 3: Convert Raw Data → MCQ
            val mcqQuestions = convertToMCQ(rawQuestions, language)
            Log.d(TAG, "Converted ${mcqQuestions.size} questions to MCQ format")

            // STAGE 4: Normalize
            val normalized = mcqQuestions.map { normalizeQuestion(it) }

            // STAGE 5: Deduplicate (skip if hash already in DB)
            val dedupedQuestions = mutableListOf<QuizQuestionBank>()
            val existingHashes = questionBankDao.getAllQuestionHashes().toMutableSet()

            for (question in normalized) {
                if (!existingHashes.contains(question.questionHash)) {
                    dedupedQuestions.add(question)
                    existingHashes.add(question.questionHash)
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

            Log.d(TAG, "✓ Successfully imported ${dedupedQuestions.size} clean $language questions from $targetAsset")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ingest $language dataset from asset $targetAsset: ${e.message}", e)
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
            .removePrefix("\uFEFF")
            .trim()
    }

    private fun parseCsv(csvContent: String, language: String): List<RawQuestion> {
        val questions = mutableListOf<RawQuestion>()
        val lines = csvContent.split("\n").filter { it.trim().isNotEmpty() }

        val dataLines = if (lines.firstOrNull()?.contains("chapter_no", ignoreCase = true) == true) {
            lines.drop(1)
        } else {
            lines
        }

        val isTelugu = language.lowercase().contains("tel")
        Log.d(TAG, "parseCsv: Total CSV lines=${lines.size}, dataLines=${dataLines.size}, isTelugu=$isTelugu")

        for (line in dataLines) {
            try {
                val fields = parseCsvLine(line)
                if (fields.size < 4) continue

                val chapterNo = fields[0].trim().toIntOrNull() ?: continue
                val verseNo = fields[1].trim().toIntOrNull() ?: continue

                if (fields.size >= 6) {
                    // Legacy 6-column format: chapter_no, verse_no, question_en, question_te, answer_en, answer_te
                    val qEn = fields.getOrNull(2)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                    val qTe = fields.getOrNull(3)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                    val aEn = fields.getOrNull(4)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                    val aTe = fields.getOrNull(5)?.trim()?.removeSurrounding("\"")?.trim() ?: ""

                    if (isTelugu && qTe.isNotBlank() && aTe.isNotBlank()) {
                        questions.add(RawQuestion(chapterNo, verseNo, qTe, aTe))
                    } else if (qEn.isNotBlank() && aEn.isNotBlank()) {
                        questions.add(RawQuestion(chapterNo, verseNo, qEn, aEn))
                    }
                } else {
                    // Standard 4-column dedicated file format: chapter_no, verse_no, question, answer
                    val q = fields.getOrNull(2)?.trim()?.removeSurrounding("\"")?.trim() ?: ""
                    val a = fields.getOrNull(3)?.trim()?.removeSurrounding("\"")?.trim() ?: ""

                    if (q.isNotBlank() && a.isNotBlank()) {
                        questions.add(RawQuestion(chapterNo, verseNo, q, a))
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
    private fun convertToMCQ(rawQuestions: List<RawQuestion>, language: String = "english"): List<QuizQuestionBank> {
        val isTelugu = language.lowercase().contains("tel")
        val topicAnswers = mutableMapOf<String, MutableList<String>>()
        rawQuestions.forEach { q ->
            val topics = extractKeyConcepts(q.answer)
            topics.forEach { topic ->
                topicAnswers.getOrPut(topic) { mutableListOf() }.add(q.answer)
            }
        }

        val allDatasetAnswers = rawQuestions.map { it.answer }.filter { it.isNotBlank() }

        return rawQuestions.map { raw ->
            val topics = extractKeyConcepts(raw.answer)
            val distractors = getTopicBasedDistractors(
                raw.answer, topics, topicAnswers, allDatasetAnswers, count = 3, isTelugu = isTelugu
            )
            
            val options = listOf(raw.answer) + distractors
            val shuffledOptions = options.shuffled()
            val correctIndex = shuffledOptions.indexOf(raw.answer)

            val difficulty = estimateDifficulty(raw.chapterNo, raw.verseNo)

            QuizQuestionBank(
                language = language.lowercase(),
                questionHash = "${language}_${raw.question.trim().lowercase().hashCode()}",
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
                modelVersion = language.lowercase(), // Store language here for filtering
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
        allDatasetAnswers: List<String>,
        count: Int,
        isTelugu: Boolean
    ): List<String> {
        val distractors = mutableSetOf<String>()
        
        topics.forEach { topic ->
            val answers = topicAnswers[topic] ?: emptyList()
            answers.filter { it != correctAnswer && it.length > 5 }.forEach { distractors.add(it) }
        }

        if (allDatasetAnswers.isNotEmpty()) {
            val random = java.util.Random()
            var attempts = 0
            while (distractors.size < count && attempts < 30) {
                attempts++
                val ans = allDatasetAnswers[random.nextInt(allDatasetAnswers.size)]
                if (ans != correctAnswer && !distractors.contains(ans)) {
                    distractors.add(ans)
                }
            }
        }
        
        val teluguGeneralDistractors = listOf(
            "కర్మలు మరియు ఆచారాలను పాటించడం ద్వారా",
            "సంపద మరియు అధికారాన్ని కూడబెట్టడం ద్వారా",
            "లౌకిక ధర్మాలన్నింటినీ వదిలివేయడం ద్వారా",
            "వ్యక్తిగత కీర్తి మరియు ప్రసిద్ధిని కోరడం ద్వారా",
            "అర్థం చేసుకోకుండా సంప్రదాయాన్ని అనుసరించడం ద్వారా",
            "కేవలం మేధోపరమైన జ్ఞానంపై మాత్రమే ఆధారపడటం ద్వారా",
            "సమాజం నుండి తన్ను తాను వేరు చేసుకోవడం ద్వారా"
        )

        val englishGeneralDistractors = listOf(
            "By performing rituals and ceremonies",
            "By accumulating wealth and power",
            "By avoiding all worldly duties",
            "By seeking personal glory and fame",
            "By following blind tradition without understanding",
            "By relying solely on intellectual knowledge",
            "By isolating oneself from society"
        )
        
        val generalDistractors = if (isTelugu) teluguGeneralDistractors else englishGeneralDistractors
        
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
