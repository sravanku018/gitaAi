package com.aipoweredgita.app.ml

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.aipoweredgita.app.data.LearningSegment
import com.aipoweredgita.app.data.SegmentWeightageSystem
import com.aipoweredgita.app.ml.AppFeature
import kotlin.random.Random

class HuggingFaceMLManager(private val context: Context) {

    private val TAG = "HuggingFaceML"
    private val gson = Gson()
    private val engine by lazy { ModelInferenceEngine(context) }
    private val voiceChatEngine by lazy { LiteRtLmVoiceChatEngine(context) }
    private var isLlmReady = false
    
    /** Public accessor for LLM readiness */
    fun isLlmInitialized(): Boolean = isLlmReady

    /** Release ML resources */
    fun close() {
        engine.close()
        voiceChatEngine.close()
    }



    // Segment-Verse mapping for intelligent question selection
    private val segmentVerseMap = mapOf(
        LearningSegment.KARMA_YOGA to listOf(
            Pair(2, 47), Pair(3, 27), Pair(4, 18), Pair(18, 46)
        ),
        LearningSegment.BHAKTI_YOGA to listOf(
            Pair(7, 17), Pair(11, 55), Pair(12, 13), Pair(18, 65)
        ),
        LearningSegment.JNANA_YOGA to listOf(
            Pair(4, 34), Pair(4, 38), Pair(13, 24), Pair(15, 11)
        ),
        LearningSegment.DHYANA_YOGA to listOf(
            Pair(6, 10), Pair(6, 35), Pair(6, 47), Pair(18, 51)
        ),
        LearningSegment.MOKSHA_YOGA to listOf(
            Pair(2, 72), Pair(4, 23), Pair(5, 25), Pair(18, 78)
        ),
        LearningSegment.WARRIOR_CODE to listOf(
            Pair(2, 31), Pair(18, 17), Pair(18, 47), Pair(2, 18)
        ),
        LearningSegment.DIVINE_NATURE to listOf(
            Pair(16, 1), Pair(16, 3), Pair(16, 6), Pair(16, 22)
        ),
        LearningSegment.SURRENDER to listOf(
            Pair(18, 66), Pair(9, 34), Pair(12, 6), Pair(2, 38)
        )
    )

    /**
     * Select verses based on segment weightage system
     * Returns a weighted selection of verses for quiz generation
     */
    suspend fun selectVersesBySegmentWeightage(
        segmentSystem: SegmentWeightageSystem,
        questionCount: Int = 10
    ): List<Pair<Int, Int>> {
        return withContext(Dispatchers.Default) {
            try {
                val weightedSegments = segmentSystem.getWeightedSegments()
                val selectedVerses = mutableListOf<Pair<Int, Int>>()

                // Calculate total weight
                val totalWeight = weightedSegments.sumOf { (_, weight) -> weight.toDouble() }

                // Distribute questions based on weightage
                var questionsRemaining = questionCount

                for ((segment, weight) in weightedSegments) {
                    if (questionsRemaining <= 0) break

                    val versesForSegment = segmentVerseMap[segment] ?: emptyList()
                    if (versesForSegment.isEmpty()) continue

                    // Calculate how many questions to allocate to this segment
                    val segmentWeight = (weight / totalWeight)
                    val questionsForSegment = when {
                        segmentWeight >= 0.3 -> ((questionCount * 0.3).toInt()).coerceAtLeast(1) // Max 30% for any segment
                        else -> ((questionCount * segmentWeight).toInt()).coerceAtLeast(1)
                    }

                    // Ensure at least 1 question for segments with decent performance
                    val finalQuestionCount = questionsForSegment.coerceAtLeast(1).coerceAtMost(questionsRemaining)

                    // Select random verses from this segment
                    val shuffledVerses = versesForSegment.shuffled(Random(System.currentTimeMillis()))
                    val versesToAdd = shuffledVerses.take(finalQuestionCount)

                    selectedVerses.addAll(versesToAdd)
                    questionsRemaining -= finalQuestionCount
                }

                // If we still need more verses, randomly select from all segments
                if (selectedVerses.size < questionCount) {
                    val allVerses = segmentVerseMap.values.flatten().shuffled()
                    for ((chapter, verse) in allVerses) {
                        if (selectedVerses.size >= questionCount) break
                        if (!selectedVerses.contains(Pair(chapter, verse))) {
                            selectedVerses.add(Pair(chapter, verse))
                        }
                    }
                }

                Log.d(TAG, "Selected ${selectedVerses.size} verses based on segment weightage")
                selectedVerses.take(questionCount)

            } catch (e: Exception) {
                Log.e(TAG, "Error selecting verses by segment: ${e.message}")
                // Fallback to random selection
                val fallbackVerses = mutableListOf<Pair<Int, Int>>()
                repeat(questionCount) {
                    val randomChapter = Random.nextInt(1, 19)
                    val randomVerse = Random.nextInt(1, 30)
                    fallbackVerses.add(Pair(randomChapter, randomVerse))
                }
                fallbackVerses
            }
        }
    }

    /**
     * Update segment system based on quiz results
     */
    suspend fun updateSegmentSystem(
        currentSystem: SegmentWeightageSystem,
        quizResults: Map<Pair<Int, Int>, Boolean>,
        selectedSegments: List<LearningSegment>
    ): SegmentWeightageSystem {
        return withContext(Dispatchers.Default) {
            try {
                val updatedSystem = currentSystem.copy()

                // Update each segment based on performance
                for ((verse, isCorrect) in quizResults) {
                    // Find which segment this verse belongs to
                    val segment = findSegmentForVerse(verse)
                    if (segment != null) {
                        updatedSystem.updateSegment(segment, isCorrect)
                    }
                }

                updatedSystem
            } catch (e: Exception) {
                Log.e(TAG, "Error updating segment system: ${e.message}")
                currentSystem
            }
        }
    }

    /**
     * Find which learning segment a verse belongs to
     */
    private fun findSegmentForVerse(verse: Pair<Int, Int>): LearningSegment? {
        for ((segment, verses) in segmentVerseMap) {
            if (verses.contains(verse)) {
                return segment
            }
        }
        return null
    }

    /**
     * Get suggested focus segments based on weakest performance
     */
    fun getSuggestedFocusSegments(segmentSystem: SegmentWeightageSystem, count: Int = 3): List<LearningSegment> {
        return segmentSystem.getWeightedSegments()
            .sortedByDescending { it.second } // Highest weightage first (weakest areas)
            .take(count)
            .map { it.first }
    }

    // Initialize models on first use
    suspend fun initializeModels() = withContext(Dispatchers.IO) {
        try {
            val ma = com.aipoweredgita.app.ml.ModelAvailability.getInstance(context)
            val modelPath = ma.getResolvedModelPath(AppFeature.QUIZ)

            Log.d(TAG, "=== AI Model Initialization ===")
            Log.d(TAG, "Selected model path: $modelPath")
            Log.d(TAG, "Device category: ${com.aipoweredgita.app.utils.DeviceUtils.getDeviceCategory(context)}")
            Log.d(TAG, "Device RAM: ${com.aipoweredgita.app.utils.DeviceUtils.getFormattedRAM(context)}")

            if (modelPath != null) {
                Log.d(TAG, "Attempting to initialize LLM...")
                isLlmReady = voiceChatEngine.initialize(modelPath)
                Log.d(TAG, "LLM initialization result: $isLlmReady")
            } else {
                Log.w(TAG, "No models found. Please download from Settings > Manage AI Models")
                isLlmReady = false
            }

            if (isLlmReady) {
                Log.d(TAG, "✓ ML models loaded successfully")
            } else {
                Log.w(TAG, "⚠ No LLM available — using rule-based question generation")
            }
            Log.d(TAG, "=== End AI Model Initialization ===")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing models: ${e.message}", e)
            Log.w(TAG, "Continuing with rule-based question generation")
            isLlmReady = false
        }
    }

    // Analyze Verse for key lessons
    suspend fun analyzeVerse(verseText: String, translation: String): VerseAnalysis {
        return withContext(Dispatchers.Default) {
            try {
                val keywords = extractKeywords(verseText)
                val themes = identifyThemes(keywords, verseText)
                val sentiment = analyzeSentiment(verseText)

                VerseAnalysis(
                    keyLessons = themes.take(3),
                    keywords = keywords,
                    sentiment = sentiment,
                    summary = generateSummary(themes, verseText)
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error analyzing verse: ${e.message}")
                VerseAnalysis()
            }
        }
    }

    // Find Similar Verses based on content
    suspend fun findSimilarVerses(
        verseText: String,
        allVerses: List<String>
    ): List<Int> {
        return withContext(Dispatchers.Default) {
            try {
                if (engine.isReady()) {
                    fun tokenizeToIds(text: String): Pair<IntArray, IntArray> {
                        val tokens = text.lowercase().split(" ").take(128)
                        val ids = IntArray(tokens.size) { (tokens[it].hashCode() and 0x7fffffff) % 30000 }
                        val mask = IntArray(tokens.size) { 1 }
                        return ids to mask
                    }
                    val (qIds, qMask) = tokenizeToIds(verseText)
                    val qEmb = engine.computeEmbedding(qIds, qMask)
                    if (qEmb != null) {
                        val sims = allVerses.mapIndexed { i, v ->
                            val (ids, mask) = tokenizeToIds(v)
                            val emb = engine.computeEmbedding(ids, mask)
                            val sim = if (emb != null) engine.cosineSim(qEmb, emb) else 0f
                            i to sim
                        }
                        return@withContext sims.sortedByDescending { it.second }.take(5).map { it.first }
                    }
                }

                val queryKeywords = extractKeywords(verseText)
                val queryThemes = identifyThemes(queryKeywords, verseText)
                val similarities = allVerses.mapIndexed { index, verse ->
                    val verseKeywords = extractKeywords(verse)
                    val verseThemes = identifyThemes(verseKeywords, verse)
                    val commonKeywords = queryKeywords.intersect(verseKeywords.toSet()).size
                    val commonThemes = queryThemes.intersect(verseThemes.toSet()).size
                    index to (commonKeywords + commonThemes * 2)
                }
                similarities.sortedByDescending { it.second }.take(5).map { it.first }
            } catch (e: Exception) {
                Log.e(TAG, "Error finding similar verses: ${e.message}")
                emptyList()
            }
        }
    }

    // Extract Keywords from text
    private fun extractKeywords(text: String): List<String> {
        val allKeywords = listOf(
            "dharma", "karma", "yoga", "bhakti", "gyana", "knowledge",
            "duty", "action", "soul", "spirit", "wisdom", "devotion",
            "self", "atman", "brahman", "moksha", "liberation", "enlightenment",
            "path", "truth", "peace", "love", "compassion", "surrender",
            "mind", "heart", "body", "nature", "universe", "creation",
            "god", "divine", "sacred", "holy", "eternal", "immortal",
            "birth", "death", "life", "death", "rebirth", "cycle",
            "attachment", "detachment", "desire", "renunciation",
            "vedas", "scriptures", "teaching", "lesson", "wisdom"
        )

        return allKeywords.filter {
            text.lowercase().contains(it)
        }
    }

    // Identify main themes from keywords
    private fun identifyThemes(keywords: List<String>, verseText: String): List<String> {
        val themes = mutableListOf<String>()

        // Check for dharma/duty theme
        if (keywords.any { it in listOf("dharma", "duty", "obligation", "action") }) {
            themes.add("Duty and Dharma")
        }

        // Check for yoga/unity theme
        if (keywords.any { it in listOf("yoga", "unity", "harmony", "balance") }) {
            themes.add("Yoga and Unity")
        }

        // Check for bhakti/devotion theme
        if (keywords.any { it in listOf("bhakti", "devotion", "love", "surrender") }) {
            themes.add("Devotion and Love")
        }

        // Check for gyana/knowledge theme
        if (keywords.any { it in listOf("gyana", "knowledge", "wisdom", "truth") }) {
            themes.add("Knowledge and Wisdom")
        }

        // Check for liberation theme
        if (keywords.any { it in listOf("moksha", "liberation", "enlightenment", "freedom") }) {
            themes.add("Liberation and Freedom")
        }

        // Check for action/karma theme
        if (keywords.any { it in listOf("karma", "action", "work", "effort") }) {
            themes.add("Karma and Action")
        }

        // Check for self/soul theme
        if (keywords.any { it in listOf("atman", "soul", "self", "spirit") }) {
            themes.add("Self and Soul")
        }

        // Check for renunciation theme
        if (keywords.any { it in listOf("renunciation", "detachment", "non-attachment") }) {
            themes.add("Renunciation and Detachment")
        }

        // If no themes identified, add generic ones
        if (themes.isEmpty()) {
            themes.addAll(listOf("Spiritual Wisdom", "Inner Growth", "Path to Truth"))
        }

        return themes.distinct()
    }

    // Analyze sentiment of verse
    private fun analyzeSentiment(text: String): String {
        val positive = listOf(
            "good", "beautiful", "sacred", "divine", "wisdom", "peace", "love",
            "joy", "bliss", "enlightenment", "liberation", "eternal", "eternal",
            "truth", "compassion", "harmony", "unity"
        )

        val negative = listOf(
            "evil", "dark", "suffering", "pain", "ignorance", "attachment",
            "desire", "anger", "greed", "delusion", "fear", "sorrow"
        )

        val neutral = listOf(
            "action", "work", "duty", "path", "teaching", "knowledge", "verse"
        )

        val lowerText = text.lowercase()
        val posCount = positive.count { lowerText.contains(it) }
        val negCount = negative.count { lowerText.contains(it) }
        val neuCount = neutral.count { lowerText.contains(it) }

        return when {
            posCount > negCount + neuCount -> "Positive/Uplifting"
            negCount > posCount + neuCount -> "Challenging/Deep"
            else -> "Balanced"
        }
    }

    // Question Type 1: Show verse, ask user to guess the chapter
    private fun generateChapterGuessQuestion(
        verseText: String,
        language: String,
        chapter: Int
    ): QuizQuestionData {
        val question = translateToLanguage("Which chapter is this verse from?", language)

        // Create options with correct chapter and 3 random wrong ones
        val chapterOptions = mutableListOf<Int>()
        chapterOptions.add(chapter)  // Correct answer

        // Add 3 different random chapters
        while (chapterOptions.size < 4) {
            val randomChapter = kotlin.random.Random.nextInt(1, 19)
            if (randomChapter !in chapterOptions) {
                chapterOptions.add(randomChapter)
            }
        }

        // Shuffle and create formatted options
        val shuffledOptions = chapterOptions.shuffled()
        val correctIndex = shuffledOptions.indexOf(chapter)
        val formattedOptions = shuffledOptions.map {
            translateToLanguage("Chapter $it", language)
        }

        val verseDisplay = if (language == "tel") verseText else verseText  // verse text stays same in both

        return QuizQuestionData(
            question = verseDisplay + "\n\n" + question,
            options = formattedOptions,
            correctOptionIndex = correctIndex,
            difficulty = "Medium",
            theme = "Chapter Identification",
            sentiment = "Neutral",
            type = "MCQ",
            explanation = translateToLanguage("Identify the chapter context from the verse.", language),
            rubricKeywords = identifyThemes(extractKeywords(verseText), verseText)
        )
    }

    // Question Type 2: Fill in the blank - show verse with missing word, ask to complete
    private fun generateFillInTheBlankQuestion(
        verseText: String,
        language: String
    ): QuizQuestionData {
        // Split verse into words and select a significant word (not articles/prepositions)
        val words = verseText.split(Regex("\\s+"))
        val significantWords = words.filter {
            it.length > 3 && (it.lowercase() !in listOf("the", "and", "that", "with", "from", "into"))
        }

        if (significantWords.isEmpty()) {
            // Fallback if no suitable word found
            return createDefaultQuestion(language)
        }

        val missingWord = significantWords.random()
        val verseWithBlank = verseText.replaceFirst(missingWord, "______", ignoreCase = true)

        val question = translateToLanguage("Complete this verse:", language)

        // Create options: correct answer and 3 similar/wrong answers
        val options = mutableListOf(missingWord)

        // Add similar words or random alternatives
        val wrongOptions = listOf(
            if (missingWord.length > 2) missingWord.reversed() else "other",
            words.filter { it != missingWord }.randomOrNull() ?: "different",
            "meditation"  // Common gita word
        ).distinct().take(3)

        options.addAll(wrongOptions)

        if (options.size < 4) {
            options.add("wisdom")
        }

        val finalOptions = options.take(4).shuffled()
        val correctIndex = finalOptions.indexOf(missingWord)

        val translatedOptions = finalOptions.map { word ->
            translateToLanguage(word, language)
        }

        return QuizQuestionData(
            question = question + "\n\n" + verseWithBlank,
            options = translatedOptions,
            correctOptionIndex = correctIndex,
            difficulty = "Hard",
            theme = "Fill in the Blank",
            sentiment = "Neutral",
            type = "MCQ",
            explanation = translateToLanguage("Choose the word that preserves the verse’s meaning.", language),
            rubricKeywords = identifyThemes(extractKeywords(verseText), verseText)
        )
    }

    // Question Type 3: Show meaning/summary, ask which verse it's from
    private fun generateMeaningGuessQuestion(
        verseText: String,
        language: String,
        chapter: Int,
        verseNum: Int
    ): QuizQuestionData {
        // Generate a meaningful description of the verse
        val keywords = extractKeywords(verseText)
        val themes = identifyThemes(keywords, verseText)

        val meaning = when {
            keywords.any { it in listOf("dharma", "duty", "action") } ->
                "This verse teaches about performing one's duty with dedication and detachment"
            keywords.any { it in listOf("knowledge", "wisdom", "truth") } ->
                "This verse emphasizes the importance of spiritual knowledge and wisdom"
            keywords.any { it in listOf("devotion", "bhakti", "love") } ->
                "This verse speaks about the path of devotion and surrender to the divine"
            keywords.any { it in listOf("yoga", "meditation", "mind") } ->
                "This verse discusses the practice of yoga and mental discipline"
            else ->
                "This verse teaches about spiritual enlightenment and inner peace"
        }

        val question = translateToLanguage("Which verse expresses this meaning?", language)

        // Create verse reference options
        val options = mutableListOf("$chapter.$verseNum")

        // Add 3 random different verses
        while (options.size < 4) {
            val randomChapter = kotlin.random.Random.nextInt(1, 19)
            val randomVerse = kotlin.random.Random.nextInt(1, 50)
            val verseRef = "$randomChapter.$randomVerse"
            if (verseRef !in options) {
                options.add(verseRef)
            }
        }

        val shuffledOptions = options.shuffled()
        val correctIndex = shuffledOptions.indexOf("$chapter.$verseNum")

        Log.d(TAG, "Shuffled options before translation: ${shuffledOptions}")
        Log.d(TAG, "Correct index: $correctIndex")

        val translatedOptions = shuffledOptions.map { ref ->
            val translated = translateToLanguage("Bhagavad Gita $ref", language)
            Log.d(TAG, "Translated option: $translated")
            translated
        }

        Log.d(TAG, "All translated options: $translatedOptions")

        if (translatedOptions.isEmpty()) {
            Log.e(TAG, "ERROR: Translated options list is empty! This should not happen.")
        }

        return QuizQuestionData(
            question = translateToLanguage(meaning, language) + "\n" + question,
            options = translatedOptions,
            correctOptionIndex = correctIndex,
            difficulty = "Easy",
            theme = "Meaning to Verse",
            sentiment = "Positive",
            type = "MCQ",
            explanation = translateToLanguage("Match the described meaning to the correct verse reference.", language),
            rubricKeywords = identifyThemes(extractKeywords(verseText), verseText)
        )
    }

    suspend fun generateQuizQuestion(
        verseText: String,
        verseTranslation: String,
        language: String,
        chapter: Int,
        verseNum: Int,
        desiredDifficulty: Int
    ): QuizQuestionData {
        // Always use LLM if available — it generates contextual questions
        // Falls back to templates only if LLM fails
        if (isLlmReady) {
            return try {
                generateQuizQuestionWithLlm(verseText, verseTranslation, chapter, verseNum, desiredDifficulty)
            } catch (e: Exception) {
                Log.w(TAG, "LLM Quiz Gen failed, falling back to templates", e)
                generateQuizQuestionWithBert(verseText, verseTranslation, language, chapter, verseNum, desiredDifficulty)
            }
        }

        // No LLM available — use keyword-based templates
        return generateQuizQuestionWithBert(verseText, verseTranslation, language, chapter, verseNum, desiredDifficulty)
    }

    private suspend fun generateQuizQuestionWithLlm(
        verseText: String,
        verseTranslation: String,
        chapter: Int,
        verseNum: Int,
        difficulty: Int = 5
    ): QuizQuestionData {
        val prompt = AdaptiveDifficultyEngine().generateLLMPrompt(
            difficulty = difficulty,
            chapter = chapter,
            verseNum = verseNum,
            translation = verseTranslation
        )

        val response = voiceChatEngine.sendMessage(prompt)
        Log.d(TAG, "LLM quiz response: ${response.take(200)}...")
        
        // Try strict JSON parse first
        return try {
            val parsed = gson.fromJson(response, QuizQuestionData::class.java)
            if (parsed?.question?.isNotBlank() == true && parsed.options.size >= 2) {
                parsed
            } else {
                throw IllegalStateException("Parsed but question/options invalid")
            }
        } catch (e: Exception) {
            // Fallback: try to extract JSON from markdown/code blocks
            try {
                val jsonMatch = Regex("```(?:json)?\\s*([\\s\\S]*?)```").find(response)
                if (jsonMatch != null) {
                    val jsonContent = jsonMatch.groupValues[1].trim()
                    val parsed = gson.fromJson(jsonContent, QuizQuestionData::class.java)
                    if (parsed?.question?.isNotBlank() == true && parsed.options.size >= 2) {
                        Log.d(TAG, "Successfully parsed JSON from code block")
                        return parsed
                    }
                }
                
                // Last resort: try to find any JSON object in the response
                val jsonObjMatch = Regex("\\{[^{}]*\"question\"[^{}]*\\}").find(response)
                if (jsonObjMatch != null) {
                    val jsonContent = jsonObjMatch.value
                    val parsed = gson.fromJson(jsonContent, QuizQuestionData::class.java)
                    if (parsed?.question?.isNotBlank() == true && parsed.options.size >= 2) {
                        Log.d(TAG, "Successfully parsed JSON from raw text")
                        return parsed
                    }
                }
            } catch (e2: Exception) {
                Log.w(TAG, "JSON extraction failed: ${e2.message}")
            }
            
            // All JSON parsing failed — throw to trigger BERT fallback
            throw IllegalStateException("LLM response could not be parsed as JSON: ${response.take(200)}", e)
        }
    }

    private fun generateQuizQuestionWithBert(
        verseText: String,
        verseTranslation: String,
        language: String,
        chapter: Int,
        verseNum: Int,
        desiredDifficulty: Int
    ): QuizQuestionData {
        // Extract key concepts from verse text for contextual questions
        val lowerText = (verseText + " " + verseTranslation).lowercase()
        val keywords = listOf(
            "duty" to listOf("dharma", "duty", "action", "perform", "deed", "work"),
            "devotion" to listOf("devotion", "bhakti", "worship", "love", "faith", "surrender"),
            "knowledge" to listOf("knowledge", "wisdom", "jnana", "understand", "learn", "truth"),
            "detachment" to listOf("detach", "desire", "attachment", "renounc", "let go", "desireless"),
            "meditation" to listOf("meditat", "mind", "concentrat", "focus", "yoga", "self-control"),
            "soul" to listOf("soul", "atman", "self", "eternal", "death", "immortal"),
            "karma" to listOf("karma", "action", "result", "fruit", "consequence", "deed"),
            "peace" to listOf("peace", "calm", "equanim", "tranquil", "serene", "joy"),
            "god" to listOf("god", "krishna", "divine", "lord", "supreme", "godhead"),
            "liberation" to listOf("liberat", "moksha", "freedom", "release", "salvation"),
            "anger" to listOf("anger", "lust", "greed", "passion", "ego", "pride"),
            "nature" to listOf("nature", "guna", "sattva", "rajas", "tamas", "qualities"),
        )

        val matchedTopics = keywords.filter { (_, terms) -> terms.any { lowerText.contains(it) } }

        // 30+ question templates
        val questionTemplates = listOf(
            "What is the primary teaching of Chapter $chapter, Verse $verseNum?",
            "According to Chapter $chapter, Verse $verseNum, what should one focus on?",
            "How does Chapter $chapter, Verse $verseNum describe the path to wisdom?",
            "What quality does Chapter $chapter, Verse $verseNum emphasize?",
            "What does Krishna teach in Chapter $chapter, Verse $verseNum?",
            "How should one act according to Chapter $chapter, Verse $verseNum?",
            "What virtue is highlighted in Chapter $chapter, Verse $verseNum?",
            "What does Chapter $chapter, Verse $verseNum say about duty?",
            "How does one achieve peace according to Chapter $chapter, Verse $verseNum?",
            "What lesson does Chapter $chapter, Verse $verseNum impart?",
            "In Chapter $chapter, Verse $verseNum, what does Krishna say about the self?",
            "What does Chapter $chapter, Verse $verseNum reveal about the nature of action?",
            "How should a wise person approach their duties, as taught in Chapter $chapter, Verse $verseNum?",
            "What attitude towards results is recommended in Chapter $chapter, Verse $verseNum?",
            "What does Chapter $chapter, Verse $verseNum say about devotion?",
            "How does one overcome desire and anger, according to Chapter $chapter, Verse $verseNum?",
            "What does Chapter $chapter, Verse $verseNum teach about the eternal soul?",
            "What is the fate of one who neglects their duty, per Chapter $chapter, Verse $verseNum?",
            "How does Chapter $chapter, Verse $verseNum describe a person of steady wisdom?",
            "What does Krishna say about the results of one's actions in Chapter $chapter, Verse $verseNum?",
            "What practice does Chapter $chapter, Verse $verseNum recommend for controlling the mind?",
            "How does one attain liberation according to Chapter $chapter, Verse $verseNum?",
            "What is the greatest gift one can give, as taught in Chapter $chapter, Verse $verseNum?",
            "What does Chapter $chapter, Verse $verseNum say about the power of faith?",
            "How should one view success and failure, per Chapter $chapter, Verse $verseNum?",
            "What quality leads to spiritual growth, according to Chapter $chapter, Verse $verseNum?",
            "What does Chapter $chapter, Verse $verseNum teach about selfless service?",
            "How does one remain steady in both joy and sorrow, per Chapter $chapter, Verse $verseNum?",
        )

        // 20 option sets covering Gita themes
        val optionSets = listOf(
            listOf("Selfless action without attachment to results", "Pursuing personal gain", "Avoiding all action", "Seeking others' approval"),
            listOf("Detachment from the fruits of action", "Working only for rewards", "Refusing to act", "Competing with others"),
            listOf("Equanimity in pleasure and pain", "Seeking only pleasure", "Avoiding all pain", "Ignoring others' suffering"),
            listOf("Devotion and complete surrender to God", "Intellectual pride", "Material accumulation", "Social status"),
            listOf("Knowledge that liberates the soul", "Blind following of rituals", "Accumulating wealth", "Political power"),
            listOf("Meditation and control of the mind", "Constant activity", "Endless sleep", "Idle gossip"),
            listOf("Righteous conduct in all circumstances", "Compromising ethics for success", "Ignoring moral rules", "Judging others"),
            listOf("Faith that sustains through difficulty", "Cynicism about spiritual paths", "Despair at obstacles", "Apathy toward growth"),
            listOf("Treating friend and enemy alike", "Showing favoritism", "Avoiding all relationships", "Seeking revenge"),
            listOf("Performing one's own duty imperfectly", "Perfectly copying another's path", "Avoiding all responsibilities", "Blaming others"),
            listOf("Rising above the three modes of nature", "Indulging in sensory pleasures", "Suppressing all emotions", "Living in isolation"),
            listOf("Offering all actions to the Divine", "Claiming credit for everything", "Ignoring spiritual practice", "Competing in piety"),
            listOf("Steady wisdom unaffected by circumstances", "Temporary enthusiasm", "Intellectual debate", "Social conformity"),
            listOf("Controlling the senses through practice", "Indulging every desire", "Punishing the body", "Ignoring physical needs"),
            listOf("Seeing the Self in all beings", "Judging by outward appearance", "Separating oneself from others", "Fearing the unknown"),
            listOf("Acting according to one's own nature", "Imitating great personalities", "Following trends blindly", "Rebelling without cause"),
            listOf("Finding joy within through meditation", "Seeking happiness externally", "Depending on others' praise", "Avoiding all effort"),
            listOf("Accepting both joy and sorrow with balance", "Chasing only happiness", "Running from all pain", "Numbing the emotions"),
            listOf("Serving others without expectation", "Demanding recognition", "Withholding help from strangers", "Competing in charity"),
            listOf("Understanding the eternal nature of the soul", "Identifying only with the body", "Fearing death excessively", "Ignoring spiritual teachings"),
        )

        // Use combined hash + difficulty for variety
        val hash = (verseText.hashCode().toLong() + verseTranslation.hashCode() * 31L + chapter * 1000 + verseNum).toInt()
        // Adjust question index based on difficulty (harder = different questions)
        val questionIndex = Math.abs((hash + desiredDifficulty * 1000) % questionTemplates.size)
        val optionIndex = Math.abs((hash / 7 + matchedTopics.size * 3 + desiredDifficulty) % optionSets.size)
        val correctIndex = Math.abs((hash / 13) % 4)

        val selectedOptions = optionSets[optionIndex].toMutableList()
        val correctAnswer = selectedOptions[0]
        selectedOptions.removeAt(0)
        selectedOptions.add(correctIndex, correctAnswer)

        val difficultyLabel = when (desiredDifficulty) {
            1, 2 -> "Easy"
            3, 4 -> "Medium"
            5, 6 -> "Medium"
            7, 8 -> "Hard"
            else -> "Hard"
        }

        return QuizQuestionData(
            question = questionTemplates[questionIndex],
            options = selectedOptions,
            correctOptionIndex = correctIndex,
            explanation = "This is based on the teachings in Chapter $chapter, Verse $verseNum of the Bhagavad Gita.",
            type = "MCQ",
            difficulty = difficultyLabel,
            rubricKeywords = emptyList()
        )
    }

    // Essay prompt (open-ended answer)
    private fun generateEssayPrompt(
        verseText: String,
        verseTranslation: String,
        language: String,
        chapter: Int,
        verseNum: Int
    ): QuizQuestionData {
        val themes = identifyThemes(extractKeywords(verseText), verseText)
        val prompt = translateToLanguage("In 3-5 sentences, explain how this verse guides conduct and mindset.", language)
        return QuizQuestionData(
            question = "Chapter $chapter, Verse $verseNum\n\n" + prompt,
            options = emptyList(),
            correctOptionIndex = 0,
            difficulty = "Hard",
            theme = "Reflection",
            sentiment = analyzeSentiment(verseText),
            type = "ESSAY",
            explanation = translateToLanguage("Reference key themes (e.g., ${themes.take(2).joinToString(", ")}) and connect to actions.", language),
            rubricKeywords = themes
        )
    }

    // Comparison (MCQ)
    private fun generateComparisonQuestion(
        verseText: String,
        verseTranslation: String,
        language: String,
        chapter: Int,
        verseNum: Int
    ): QuizQuestionData {
        val question = translateToLanguage("Which best contrasts this verse’s teaching with a common misconception?", language)
        val options = listOf(
            "Detachment vs indifference",
            "Duty vs blind obedience",
            "Devotion vs ritualism",
            "Knowledge vs arrogance"
        ).map { translateToLanguage(it, language) }
        val correctIndex = 0
        return QuizQuestionData(
            question = "Chapter $chapter, Verse $verseNum\n\n" + question,
            options = options,
            correctOptionIndex = correctIndex,
            difficulty = "Medium",
            theme = "Comparison",
            sentiment = analyzeSentiment(verseText),
            type = "COMPARISON",
            explanation = translateToLanguage("True detachment = engaged action without clinging, not apathy.", language),
            rubricKeywords = identifyThemes(extractKeywords(verseText), verseText)
        )
    }

    // Application scenario (open-ended)
    private fun generateApplicationScenario(
        verseText: String,
        verseTranslation: String,
        language: String,
        chapter: Int,
        verseNum: Int
    ): QuizQuestionData {
        val themes = identifyThemes(extractKeywords(verseText), verseText)
        val scenario = translateToLanguage("Describe how you would apply this verse in a workplace challenge.", language)
        return QuizQuestionData(
            question = "Chapter $chapter, Verse $verseNum\n\n" + scenario,
            options = emptyList(),
            correctOptionIndex = 0,
            difficulty = "Hard",
            theme = "Application",
            sentiment = analyzeSentiment(verseText),
            type = "APPLICATION",
            explanation = translateToLanguage("Connect teachings to practical steps (e.g., duty without attachment, compassion).", language),
            rubricKeywords = themes
        )
    }

    // Multi-language translation support
    private fun translateToLanguage(text: String, language: String): String {
        return when (language.lowercase()) {
            "tel" -> translateToTelugu(text)
            "hi" -> translateToHindi(text)
            "ta" -> translateToTamil(text)
            "kn" -> translateToKannada(text)
            else -> text // English default
        }
    }

    private fun translateToTelugu(text: String): String {
        val translations = mapOf(
            // New Question Type 1: Chapter Guess
            "which chapter is this verse from?" to "ఈ శ్లోకం ఏ అధ్యాయం నుండి ఉంది?",
            "chapter" to "అధ్యాయం",

            // New Question Type 2: Fill in the Blank
            "complete this verse:" to "ఈ శ్లోకాన్ని పూర్తి చేయండి:",

            // New Question Type 3: Meaning to Verse
            "which verse expresses this meaning?" to "ఈ అర్థం ఏ శ్లోకం వ్యక్తపరుస్తుంది?",
            "bhagavad gita" to "భగవద్గీత",
            "this verse teaches about performing one's duty with dedication and detachment" to "ఈ శ్లోకం సమర్పణ మరియు విచ్ఛిన్నత్వంతో కర్తవ్యం నిర్వహించడం గురించి నిర్దేశిస్తుంది",
            "this verse emphasizes the importance of spiritual knowledge and wisdom" to "ఈ శ్లోకం ఆధ్యాత్మిక జ్ఞానం మరియు జ్ఞానం యొక్క ప్రాముఖ్యతను నొక్కిచెప్పుతుంది",
            "this verse speaks about the path of devotion and surrender to the divine" to "ఈ శ్లోకం భక్తి మరియు దైవానికి సమర్పణ మార్గం గురించి చెబుతుంది",
            "this verse discusses the practice of yoga and mental discipline" to "ఈ శ్లోకం యోగ అభ్యాసం మరియు మానసిక క్రమశిక్షణ గురించి చర్చిస్తుంది",
            "this verse teaches about spiritual enlightenment and inner peace" to "ఈ శ్లోకం ఆధ్యాత్మిక జ్ఞానోదయం మరియు అంతర్గత శాంతి గురించి నిర్దేశిస్తుంది",

            // Old Question Type translations (keeping for backward compatibility)
            "how should one approach action according to this verse?" to "ఈ శ్లోకం ప్రకారం కర్మను ఎలా చేపట్టాలి?",
            "with detachment and dedication" to "విచ్ఛిన్నంగా మరియు సమర్పణతో",
            "by avoiding all responsibilities" to "అన్ని బాధ్యతలను నివారించడం",
            "through force and compulsion" to "బలవంతం ద్వారా",
            "without any spiritual purpose" to "ఆధ్యాత్మిక ఉద్దేశ్యం లేకుండా",
            "what does this verse teach about one's duty?" to "ఈ శ్లోకం ఒకరి కర్తవ్యం గురించి ఏమి నిర్దేశిస్తుంది?",
            "that duty is unimportant" to "కర్తవ్యం ముఖ్యమైనది కాదు",
            "to escape from all duties" to "అన్ని కర్తవ్యాల నుండి విముక్తి",
            "that duties cause suffering" to "కర్తవ్యాలు కష్టాన్ని కలిగిస్తాయి",
            "what kind of knowledge does this verse emphasize?" to "ఈ శ్లోకం ఏ విధమైన జ్ఞానాన్ని ఒత్తిడిచేస్తుంది?",
            "meditation" to "ధ్యానం",
            "wisdom" to "జ్ఞానం"
        )
        return translations[text.lowercase()] ?: text
    }

    private fun translateToHindi(text: String): String {
        val translations = mapOf(
            "how should one approach action according to this verse?" to "इस श्लोक के अनुसार कर्म को कैसे करें?",
            "with detachment and dedication" to "वैराग्य और समर्पण के साथ",
            "what does this verse teach about one's duty?" to "इस श्लोक में अपने कर्तव्य के बारे में क्या सिखाया जाता है?"
        )
        return translations[text.lowercase()] ?: text
    }

    private fun translateToTamil(text: String): String {
        val translations = mapOf(
            "how should one approach action according to this verse?" to "இந்த பாடலின் படி ஒருவர் செயலை எவ்வாறு அணுக வேண்டும்?",
            "with detachment and dedication" to "விரக்தி மற்றும் அர்ப்பணிப்புடன்"
        )
        return translations[text.lowercase()] ?: text
    }

    private fun translateToKannada(text: String): String {
        val translations = mapOf(
            "how should one approach action according to this verse?" to "ಈ ಶ್లోಕದ ಪ್ರಕಾರ ಕರ್ಮವನ್ನು ಹೇಗೆ ముందువరుసబేకు?",
            "with detachment and dedication" to "ವಿರಾಗ ಮತ್ತು ಸಮರ್ಪಣೆಯೊಂದಿಗೆ"
        )
        return translations[text.lowercase()] ?: text
    }

    // Generate summary of verse
    private fun generateSummary(themes: List<String>, verseText: String): String {
        val themeStr = themes.take(2).joinToString(" and ")
        return "This verse focuses on $themeStr. It teaches about the deeper aspects of spiritual life and wisdom."
    }

    // Create default question when generation fails
    private fun createDefaultQuestion(language: String = "tel"): QuizQuestionData {
        val question = translateToLanguage("What is the core spiritual message of this verse?", language)
        val options = listOf(
            "Duty and Dharma",
            "Knowledge and Wisdom",
            "Devotion and Love",
            "Path to Liberation"
        ).map { translateToLanguage(it, language) }

        return QuizQuestionData(
            question = question,
            options = options,
            correctOptionIndex = 0,
            difficulty = "Medium",
            theme = "Spiritual Wisdom",
            sentiment = "Balanced",
            type = "MCQ",
            explanation = translateToLanguage("Choose the theme most directly taught by the verse.", language),
            rubricKeywords = listOf("dharma", "wisdom", "devotion", "liberation")
        )
    }
}

// Data Classes
@Suppress("PropertyName")
data class QuizQuestionData(
    @SerializedName("question")
    val question: String = "",
    @SerializedName("options")
    val options: List<String> = emptyList(),
    @SerializedName("correctOptionIndex")
    val correctOptionIndex: Int = 0,
    @SerializedName("difficulty")
    val difficulty: String = "Medium",
    @SerializedName("theme")
    val theme: String = "",
    @SerializedName("sentiment")
    val sentiment: String = "Balanced",
    @SerializedName("type")
    val type: String = "MCQ",
    @SerializedName("explanation")
    val explanation: String? = null,
    @SerializedName("rubricKeywords")
    val rubricKeywords: List<String> = emptyList()
)

data class VerseAnalysis(
    val keyLessons: List<String> = emptyList(),
    val keywords: List<String> = emptyList(),
    val sentiment: String = "Balanced",
    val summary: String = ""
)
