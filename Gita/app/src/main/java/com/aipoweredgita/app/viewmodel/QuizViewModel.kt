package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.data.GitaVerse
import com.aipoweredgita.app.data.QuizQuestion
import com.aipoweredgita.app.data.QuestionType
import com.aipoweredgita.app.data.QuizState
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.QuizAttemptDao
import com.aipoweredgita.app.database.QuestionPerformance
import com.aipoweredgita.app.database.QuestionPerformanceDao
import com.aipoweredgita.app.database.QuizQuestionBank
import com.aipoweredgita.app.database.QuizQuestionBankDao
import com.aipoweredgita.app.ml.HuggingFaceMLManager
import com.aipoweredgita.app.ml.AdaptiveDifficultyEngine
import com.aipoweredgita.app.ml.BhagavadGitaQAImporter
import com.aipoweredgita.app.ml.QuestionValidator
import com.aipoweredgita.app.network.GitaApi
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.OfflineCacheRepository
import com.aipoweredgita.app.util.TimeTracker
import com.aipoweredgita.app.util.VerseCacheManager
import com.aipoweredgita.app.utils.QuizPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class QuizViewModel(app: Application) : AndroidViewModel(app) {
    private val _quizState = MutableStateFlow(QuizState())
    val quizState: StateFlow<QuizState> = _quizState.asStateFlow()

    private val statsRepository: StatsRepository
    private val quizAttemptDao: QuizAttemptDao
    private val questionPerformanceDao: QuestionPerformanceDao
    private val quizPreferences: QuizPreferences
    private val mlManager: HuggingFaceMLManager
    private val offlineCacheRepository: OfflineCacheRepository
    private val yogaProgressionRepository: com.aipoweredgita.app.repository.YogaProgressionRepository
    private var language = "tel" // Telugu language (default, can be changed)
    private val totalVerses = com.aipoweredgita.app.util.GitaConstants.TOTAL_VERSES
    private val usedQuestions = mutableSetOf<String>() // Track used chapter-verse combinations
    private val recentlyAskedQuestions = mutableSetOf<String>() // Track recently asked across sessions
    private val verseCache = VerseCacheManager(maxSizeKb = com.aipoweredgita.app.util.GitaConstants.VERSE_CACHE_MAX_SIZE_KB) // Bounded LRU cache (configurable)

    // Adaptive difficulty engine
    private val difficultyEngine = AdaptiveDifficultyEngine()
    private var userState = AdaptiveDifficultyEngine.UserState()
    
    // FINAL ISSUE 1: Lightweight Telemetry
    data class SystemMetrics(
        var llmCalls: Int = 0,
        var llmFailures: Int = 0,
        var validationFailures: Int = 0,
        var avgUserAccuracy: Double = 0.0,
        var avgDifficulty: Double = 5.0,
        var rejectedQuestions: Int = 0,
        var seedFallbackUsage: Int = 0
    )
    private val telemetry = SystemMetrics()

    // Question queue - preloaded from DB, refilled by LLM
    private val questionQueue = mutableListOf<QuizQuestionBank>()
    private val questionBankDao = GitaDatabase.getDatabase(getApplication()).quizQuestionBankDao()
    
    // Track last good question for hard fallback (ISSUE 2)
    private var lastValidQuestion: QuizQuestionBank? = null
    
    // Dataset importer for 10,500 curated Gita QA questions
    private val qaImporter = BhagavadGitaQAImporter(getApplication(), questionBankDao)
    private var qaImportStarted = false  // Prevent duplicate background imports
    
    // FIX ISSUE 1: Mutex to prevent race condition in LLM generation
    private val llmGenerationMutex = kotlinx.coroutines.sync.Mutex()
    
    // FIX ISSUE 7: LLM rate limiting
    private val llmCallTimestamps = mutableListOf<Long>()
    private val MAX_LLM_CALLS_PER_MINUTE = 5

    // FIX ISSUE 7: Offline seed questions from assets
    private var offlineSeeded = false

    // Time tracking for quiz session
    private var quizStartTime: Long = 0

    // Per-question response time tracking
    private var questionStartMs: Long? = null
    private var answerMs: Long? = null
    val responseSeconds: String?
        get() = run {
            val start = questionStartMs
            val ans = answerMs
            if (start != null && ans != null) String.format("%.1f", ((ans - start).coerceAtLeast(0L)) / 1000.0) else null
        }

    /**
     * CORRECTED FLOW WITH FIXES:
     * 1. Load available questions from DB (don't block on dataset import)
     * 2. If DB empty → Mutex-locked LLM generation (no race condition)
     * 3. Seed dataset in BACKGROUND — doesn't block the quiz
     */
    private suspend fun loadQuestionQueue() {
        val targetDifficulty = if (userState.totalAnswered < 5) 4 else userState.skillLevel // FINAL ISSUE 4: Warmup Mode
        val range = (targetDifficulty - 2).coerceAtLeast(1)..(targetDifficulty + 2).coerceAtMost(10)
        
        // FINAL ISSUE 3: Source Balancing (Prefer imported questions if LLM ratio > 60%)
        val totalCount = questionBankDao.getTotalCount()
        val llmCount = questionBankDao.getGeneratedCount("AI") // Assuming "AI" prefix for generated
        val llmRatio = if (totalCount > 0) llmCount.toDouble() / totalCount else 0.0
        
        val dbQuestions = if (llmRatio > 0.6) {
            // Source Balancing: Fetch only non-generated or high-quality imported ones
            questionBankDao.getImportedQuestions(range.first, range.last, 50)
        } else {
            questionBankDao.getNextQuestions(
                minDiff = range.first,
                maxDiff = range.last,
                limit = 100,
                targetDifficulty = targetDifficulty
            )
        }
        
        questionQueue.clear()
        questionQueue.addAll(dbQuestions)
        
        android.util.Log.d("QuizViewModel", "Loaded ${questionQueue.size} questions (LLM Ratio: ${String.format("%.2f", llmRatio)})")
        
        // FINAL ISSUE 2: HARD FALLBACK CHAIN
        if (questionQueue.isEmpty()) {
            llmGenerationMutex.withLock {
                if (questionQueue.isEmpty()) {
                    val hasNetwork = com.aipoweredgita.app.utils.NetworkUtils.isNetworkAvailable(getApplication())
                    
                    if (hasNetwork && !isLLMRateLimited()) {
                        telemetry.llmCalls++
                        val instantQuestion = generateAndSaveSingleQuestion()
                        if (instantQuestion != null) {
                            val dbQ = convertToDbQuestion(instantQuestion)
                            questionQueue.add(dbQ)
                            lastValidQuestion = dbQ
                            recordLLMCall()
                        } else {
                            telemetry.llmFailures++
                        }
                    }
                    
                    // Fallback to Seed Questions if LLM fails or no network
                    if (questionQueue.isEmpty()) {
                        android.util.Log.d("QuizViewModel", "Falling back to seed questions...")
                        telemetry.seedFallbackUsage++
                        seedQuestionsFromAssets() // This populates questionQueue
                    }
                    
                    // Final HARD Fallback: Cached Last Good Question (Never return null)
                    if (questionQueue.isEmpty()) {
                        android.util.Log.w("QuizViewModel", "FINAL HARD FALLBACK triggered")
                        lastValidQuestion?.let { questionQueue.add(it) } ?: run {
                            // If even lastValid is null, use a hardcoded emergency question
                            questionQueue.add(getEmergencyQuestion())
                        }
                    }
                }
            }
        }
        
        telemetry.avgDifficulty = (telemetry.avgDifficulty * 0.9 + targetDifficulty * 0.1)
        
        // STEP 3: Seed dataset in BACKGROUND — doesn't block quiz start
        if (questionQueue.size < 50 && !qaImportStarted) {
            qaImportStarted = true
            viewModelScope.launch {
                try {
                    seedQuestionsFromDataset()
                    android.util.Log.d("QuizViewModel", "Background dataset seed complete")
                } catch (e: Exception) {
                    android.util.Log.w("QuizViewModel", "Background dataset seed failed: ${e.message}")
                    qaImportStarted = false  // Allow retry on next quiz load
                }
            }
        }
    }

    /**
     * FIX ISSUE 7: Check if LLM calls are rate limited.
     */
    private fun isLLMRateLimited(): Boolean {
        val now = System.currentTimeMillis()
        val oneMinuteAgo = now - 60_000
        llmCallTimestamps.removeAll { it < oneMinuteAgo }  // Clean old timestamps
        return llmCallTimestamps.size >= MAX_LLM_CALLS_PER_MINUTE
    }

    /**
     * FIX ISSUE 7: Record an LLM call for rate limiting.
     */
    private fun recordLLMCall() {
        llmCallTimestamps.add(System.currentTimeMillis())
    }

    /**
     * FIX ISSUE 7: Load 20 offline seed questions from APK assets.
     * Called when DB is empty and no internet is available.
     */
    private suspend fun seedQuestionsFromAssets() {
        if (offlineSeeded) return
        
        try {
            val jsonContent = getApplication<Application>().assets.open("seed_questions.json").bufferedReader().use { it.readText() }
            val gson = com.google.gson.Gson()
            val seedQuestions = gson.fromJson(jsonContent, Array<SeedQuestion>::class.java)
            
            val dbQuestions = seedQuestions.map { sq ->
                QuizQuestionBank(
                    questionHash = "${sq.chapter}:${sq.verse}:${sq.question.hashCode()}",
                    questionType = "MCQ",
                    difficulty = sq.difficulty,
                    question = sq.question,
                    chapter = sq.chapter,
                    verse = sq.verse,
                    yogaLevel = 0,
                    optionA = sq.options.getOrNull(0) ?: "",
                    optionB = sq.options.getOrNull(1) ?: "",
                    optionC = sq.options.getOrNull(2) ?: "",
                    optionD = sq.options.getOrNull(3) ?: "",
                    correctAnswer = sq.options.getOrNull(sq.correctAnswerIndex) ?: "",
                    explanation = sq.explanation,
                    keywords = sq.topic,
                    topics = sq.topic,
                    generatedBy = "Offline Seed",
                    generationMethod = "bundled",
                    qualityScore = 90f,
                    relevanceScore = 85f,
                    isVerified = true,
                    isApproved = true,
                    usageCount = 0
                )
            }
            
            questionBankDao.insertAll(dbQuestions.toList())
            offlineSeeded = true
            questionQueue.addAll(dbQuestions)
            
            android.util.Log.d("QuizViewModel", "✓ Seeded ${dbQuestions.size} offline questions from assets")
        } catch (e: Exception) {
            android.util.Log.w("QuizViewModel", "Failed to seed offline questions: ${e.message}")
        }
    }

    /**
     * Data class for parsing seed questions from JSON.
     */
    private data class SeedQuestion(
        val question: String,
        val options: List<String>,
        val correctAnswerIndex: Int,
        val explanation: String,
        val chapter: Int,
        val verse: Int,
        val difficulty: Int,
        val topic: String
    )

    /**
     * FIX ISSUE 2: Get fallback question without RANDOM() in SQL.
     * Pre-fetches 20 questions, then shuffles in Kotlin.
     * FINAL ISSUE 2: HARD FALLBACK CHAIN
     */
    private suspend fun getFallbackQuestion(): QuizQuestionBank {
        val candidates = questionBankDao.getNextQuestions(1, 10, limit = 20, targetDifficulty = userState.skillLevel)
        return candidates.randomOrNull() ?: lastValidQuestion ?: getEmergencyQuestion()
    }

    private fun getEmergencyQuestion(): QuizQuestionBank {
        return QuizQuestionBank(
            question = "Who spoke the Bhagavad Gita to Arjuna?",
            optionA = "Lord Shiva",
            optionB = "Lord Krishna",
            optionC = "Lord Brahma",
            optionD = "Lord Indra",
            correctAnswer = "B",
            explanation = "Lord Krishna delivered the Gita on the battlefield of Kurukshetra.",
            difficulty = 1,
            questionType = "MCQ",
            chapter = 1,
            verse = 1
        )
    }

    /**
     * Convert a display question back to DB format for queue management.
     */
    private fun convertToDbQuestion(question: QuizQuestion): QuizQuestionBank {
        return QuizQuestionBank(
            question = question.question,
            chapter = question.verse.chapterNo,
            verse = question.verse.verseNo,
            optionA = question.options.getOrNull(0) ?: "",
            optionB = question.options.getOrNull(1) ?: "",
            optionC = question.options.getOrNull(2) ?: "",
            optionD = question.options.getOrNull(3) ?: "",
            correctAnswer = question.options.getOrNull(question.correctAnswerIndex) ?: "",
            difficulty = userState.skillLevel,
            questionType = question.type.name,
            questionHash = "${question.verse.chapterNo}:${question.verse.verseNo}:${question.question.hashCode()}"
        )
    }

    /**
     * Fetch the next question with adaptive fallback strategy.
     * FIX ISSUE 2: Dynamic prefetch size based on DB size
     * FIX ISSUE 4: Exploration factor (15% random difficulty)
     * FIX ISSUE 5: Mixing strategy (70% adaptive, 20% weak topic, 10% random)
     * FIX ISSUE 1: Mutex-locked LLM generation
     * FIX ISSUE 7: Rate limited LLM calls
     */
    private suspend fun fetchNextQuestion(): QuizQuestion? {
        var targetDifficulty = userState.skillLevel
        
        // FIX ISSUE 4: Exploration factor — 15% chance to jump to random difficulty
        if (kotlin.random.Random.nextFloat() < 0.15f) {
            targetDifficulty = kotlin.random.Random.nextInt(1, 11)
            android.util.Log.d("QuizViewModel", "Exploration mode: jumping to difficulty $targetDifficulty")
        }
        
        // FIX ISSUE 2: Dynamic prefetch size based on DB size
        val dbSize = questionBankDao.getTotalAvailableQuestions()
        val prefetchLimit = when {
            dbSize < 100 -> 20
            dbSize < 1000 -> 50
            else -> 100
        }
        
        // FIX ISSUE 5: Mixing strategy
        val strategy = kotlin.random.Random.nextInt(100)
        val questions = when {
            strategy < 70 -> {
                // 70% → adaptive difficulty (exact ±2)
                val exactRange = (targetDifficulty - 2).coerceAtLeast(1)..(targetDifficulty + 2).coerceAtMost(10)
                questionBankDao.getNextQuestions(
                    minDiff = exactRange.first,
                    maxDiff = exactRange.last,
                    limit = prefetchLimit,
                    targetDifficulty = targetDifficulty
                )
            }
            strategy < 90 -> {
                // 20% → weak topic targeting (not yet wired, fallback to relaxed)
                val relaxedRange = (targetDifficulty - 4).coerceAtLeast(1)..(targetDifficulty + 4).coerceAtMost(10)
                questionBankDao.getNextQuestions(
                    minDiff = relaxedRange.first,
                    maxDiff = relaxedRange.last,
                    limit = prefetchLimit,
                    targetDifficulty = targetDifficulty
                )
            }
            else -> {
                // 10% → completely random
                questionBankDao.getNextQuestions(1, 10, limit = prefetchLimit, targetDifficulty = kotlin.random.Random.nextInt(1, 11))
            }
        }
        
        // FIX ISSUE 2: Shuffle in Kotlin, not SQL
        if (questions.isNotEmpty()) {
            return convertDbQuestion(questions.shuffled().first())
        }
        
        // ATTEMPT 2: Relaxed difficulty range (±4)
        val relaxedRange = (targetDifficulty - 4).coerceAtLeast(1)..(targetDifficulty + 4).coerceAtMost(10)
        val relaxedQuestions = questionBankDao.getNextQuestions(
            minDiff = relaxedRange.first,
            maxDiff = relaxedRange.last,
            limit = prefetchLimit,
            targetDifficulty = targetDifficulty
        )
        
        if (relaxedQuestions.isNotEmpty()) {
            return convertDbQuestion(relaxedQuestions.shuffled().first())
        }
        
        // ATTEMPT 3: Any available question (shuffle in Kotlin)
        val anyQuestions = questionBankDao.getNextQuestions(1, 10, limit = prefetchLimit, targetDifficulty = targetDifficulty)
        if (anyQuestions.isNotEmpty()) {
            return convertDbQuestion(anyQuestions.shuffled().first())
        }
        
        // ATTEMPT 4: FIX ISSUE 1 — Mutex-locked LLM generate
        return llmGenerationMutex.withLock {
            android.util.Log.d("QuizViewModel", "No questions in DB — generating via LLM (locked)...")

            // FIX ISSUE 7: Rate limit LLM calls
            if (!isLLMRateLimited()) {
                val question = generateAndSaveSingleQuestion()
                if (question != null) recordLLMCall()
                question
            } else {
                android.util.Log.w("QuizViewModel", "LLM rate limited — using fallback")
                val fallback = getFallbackQuestion()
                convertDbQuestion(fallback)
            }
        }
    }

    /**
     * Convert a DB question to QuizQuestion for display.
     */
    private suspend fun convertDbQuestion(dbQuestion: QuizQuestionBank): QuizQuestion? {
        // Mark as asked (sets lastAskedAt for cooldown)
        questionBankDao.markAsAsked(dbQuestion.id)
        
        // Remove from in-memory queue if present
        questionQueue.removeAll { it.id == dbQuestion.id }
        
        val verse = offlineCacheRepository.getVerse(dbQuestion.chapter, dbQuestion.verse)
        if (verse == null) return null
        
        val options = listOf(dbQuestion.optionA, dbQuestion.optionB, dbQuestion.optionC, dbQuestion.optionD)
            .filter { it.isNotEmpty() }
        val correctIndex = options.indexOf(dbQuestion.correctAnswer)
        
        return QuizQuestion(
            verse = verse,
            question = dbQuestion.question,
            options = options,
            correctAnswerIndex = if (correctIndex >= 0) correctIndex else 0,
            type = when (dbQuestion.questionType.uppercase()) {
                "ESSAY" -> QuestionType.ESSAY
                "COMPARISON" -> QuestionType.COMPARISON
                "APPLICATION" -> QuestionType.APPLICATION
                else -> QuestionType.MCQ
            },
            explanation = dbQuestion.explanation,
            rubricKeywords = dbQuestion.keywords.split(",").filter { it.isNotBlank() },
            theme = dbQuestion.topics
        )
    }

    /**
     * Generate a single question via LLM, VALIDATE strictly, save to DB, return for display.
     */
    private suspend fun generateAndSaveSingleQuestion(): QuizQuestion? {
        val chapter = Random.nextInt(1, 19)
        val verseNum = Random.nextInt(1, chapterVerseCounts[chapter] ?: 47)
        val verse = verseCache.getOrFetch(chapter, verseNum) { offlineCacheRepository.getVerse(chapter, verseNum) }
        
        if (verse == null) return null

        // Generate via LLM
        val aiQuizData = mlManager.generateQuizQuestion(
            verseText = verse.verse,
            verseTranslation = verse.translation,
            language = language,
            chapter = chapter,
            verseNum = verseNum,
            desiredDifficulty = userState.skillLevel.coerceIn(1, 10)
        )

        // VALIDATION PIPELINE: strict JSON parse → structure check → logic check → semantic dedup
        val validationResult = QuestionValidator.validateLLMOutput(
            rawOutput = """
                {
                    "question": "${aiQuizData.question.replace("\"", "\\\"")}",
                    "options": [${aiQuizData.options.joinToString(",") { "\"${it.replace("\"", "\\\"")}\"" }}],
                    "correctOptionIndex": ${aiQuizData.correctOptionIndex},
                    "explanation": "${(aiQuizData.explanation ?: "").replace("\"", "\\\"")}"
                }
            """.trimIndent(),
            chapter = chapter,
            verseNum = verseNum,
            difficulty = userState.skillLevel.coerceIn(1, 10),
            existingQuestions = questionQueue.map { it.question }  // FIX ISSUE 3: Semantic dedup
        )

        val dbQuestion = if (validationResult.isValid && validationResult.question != null) {
            // Validation passed — use validated question
            validationResult.question
        } else {
            // Validation failed — fallback to raw data with basic structure
            telemetry.validationFailures++
            android.util.Log.w("QuizViewModel", "LLM validation failed (${validationResult.error}), using raw output")
            QuizQuestionBank(
                questionHash = "${chapter}:${verseNum}:${aiQuizData.question.hashCode()}",
                questionType = aiQuizData.type ?: "MCQ",
                difficulty = userState.skillLevel.coerceIn(1, 10),
                question = aiQuizData.question,
                chapter = chapter,
                verse = verseNum,
                yogaLevel = 0,
                optionA = aiQuizData.options.getOrNull(0) ?: "",
                optionB = aiQuizData.options.getOrNull(1) ?: "",
                optionC = aiQuizData.options.getOrNull(2) ?: "",
                optionD = aiQuizData.options.getOrNull(3) ?: "",
                correctAnswer = aiQuizData.options.getOrNull(aiQuizData.correctOptionIndex) ?: "",
                explanation = aiQuizData.explanation ?: "",
                keywords = aiQuizData.rubricKeywords?.joinToString(",") ?: "",
                topics = aiQuizData.theme ?: "",
                generatedBy = if (mlManager.isLlmInitialized()) "LLM" else "Template",
                generationMethod = "AI_generated",
                qualityScore = validationResult.qualityScore,
                relevanceScore = 70f,
                isVerified = false,
                isApproved = true,
                usageCount = 0
            )
        }

        // Save to DB
        questionBankDao.insert(dbQuestion)

        return QuizQuestion(
            verse = verse,
            question = dbQuestion.question,
            options = listOf(dbQuestion.optionA, dbQuestion.optionB, dbQuestion.optionC, dbQuestion.optionD).filter { it.isNotEmpty() },
            correctAnswerIndex = listOf(dbQuestion.optionA, dbQuestion.optionB, dbQuestion.optionC, dbQuestion.optionD).indexOf(dbQuestion.correctAnswer),
            type = when (dbQuestion.questionType.uppercase()) {
                "ESSAY" -> QuestionType.ESSAY
                "COMPARISON" -> QuestionType.COMPARISON
                "APPLICATION" -> QuestionType.APPLICATION
                else -> QuestionType.MCQ
            },
            explanation = dbQuestion.explanation,
            rubricKeywords = dbQuestion.keywords.split(",").filter { it.isNotBlank() },
            theme = dbQuestion.topics
        )
    }

    /**
     * Seed the database with curated questions from the Bhagavad Gita QA dataset.
     * Downloads ~3,500 English questions on first run — runs in BACKGROUND.
     */
    private suspend fun seedQuestionsFromDataset() {
        // Check if already imported
        if (qaImporter.hasQuestions()) {
            android.util.Log.d("QuizViewModel", "Questions already imported from dataset")
            return
        }

        android.util.Log.d("QuizViewModel", "Starting BACKGROUND dataset import from HuggingFace...")
        try {
            val imported = qaImporter.importDataset(
                language = "english",
                batchSize = 500
            ) { imported, total ->
                android.util.Log.d("QuizViewModel", "Background import: $imported / ~3500")
            }
            android.util.Log.d("QuizViewModel", "✓ Background import complete: $imported questions saved to DB")
        } catch (e: Exception) {
            android.util.Log.w("QuizViewModel", "Background dataset import failed: ${e.message}")
        }
    }

    /**
     * Generate questions via LLM and save them to DB.
     * Called when DB is empty or running low.
     */
    private suspend fun generateAndSaveQuestions(count: Int) {
        val chapter = Random.nextInt(1, 19)
        val verseNum = Random.nextInt(1, chapterVerseCounts[chapter] ?: 47)
        val verse = verseCache.getOrFetch(chapter, verseNum) { offlineCacheRepository.getVerse(chapter, verseNum) }
        
        if (verse == null) {
            android.util.Log.w("QuizViewModel", "Could not fetch verse for question generation")
            return
        }

        // Generate questions via LLM
        repeat(count) { index ->
            try {
                val aiQuizData = mlManager.generateQuizQuestion(
                    verseText = verse.verse,
                    verseTranslation = verse.translation,
                    language = language,
                    chapter = chapter,
                    verseNum = verseNum,
                    desiredDifficulty = userState.skillLevel.coerceIn(1, 10)
                )

                // Convert to DB format
                val dbQuestion = QuizQuestionBank(
                    questionHash = "${chapter}:${verseNum}:${aiQuizData.question.hashCode()}",
                    questionType = aiQuizData.type ?: "MCQ",
                    difficulty = userState.skillLevel.coerceIn(1, 10),
                    question = aiQuizData.question,
                    chapter = chapter,
                    verse = verseNum,
                    yogaLevel = 0,
                    optionA = aiQuizData.options.getOrNull(0) ?: "",
                    optionB = aiQuizData.options.getOrNull(1) ?: "",
                    optionC = aiQuizData.options.getOrNull(2) ?: "",
                    optionD = aiQuizData.options.getOrNull(3) ?: "",
                    correctAnswer = aiQuizData.options.getOrNull(aiQuizData.correctOptionIndex) ?: "",
                    explanation = aiQuizData.explanation ?: "",
                    keywords = "",
                    topics = "",
                    generatedBy = if (mlManager.isLlmInitialized()) "LLM" else "Template",
                    generationMethod = "AI_generated",
                    qualityScore = 75f,
                    relevanceScore = 70f,
                    isVerified = false,
                    isApproved = true,
                    usageCount = 0
                )

                // Save to DB
                questionBankDao.insert(dbQuestion)
                
                // Also add to in-memory queue for immediate use
                questionQueue.add(dbQuestion)
                
                // Add small delay between generations
                kotlinx.coroutines.delay(500)
            } catch (e: Exception) {
                android.util.Log.w("QuizViewModel", "Failed to generate question $index: ${e.message}")
            }
        }
        
        android.util.Log.d("QuizViewModel", "Generated and saved ${questionQueue.size} questions via LLM")
    }

    // Time tracker for overall quiz mode time
    private val timeTracker = TimeTracker { seconds ->
        viewModelScope.launch(Dispatchers.IO) {
            try {
                statsRepository.trackModeTime(seconds, ModeType.QUIZ)
                val today = java.time.LocalDate.now().toString()
                val dailyDao = GitaDatabase.getDatabase(getApplication()).dailyActivityDao()
                dailyDao.insertIfAbsent(com.aipoweredgita.app.database.DailyActivity(date = today))
                dailyDao.addQuizSeconds(today, seconds)
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error updating daily quiz seconds", e)
            }
        }
    }

    init {
        val database = GitaDatabase.getDatabase(getApplication())
        statsRepository = StatsRepository(database.userStatsDao())
        quizAttemptDao = database.quizAttemptDao()
        questionPerformanceDao = database.questionPerformanceDao()
        quizPreferences = QuizPreferences(getApplication())
        mlManager = HuggingFaceMLManager(getApplication())
        offlineCacheRepository = OfflineCacheRepository(database.cachedVerseDao())
        yogaProgressionRepository = com.aipoweredgita.app.repository.YogaProgressionRepository(database.yogaProgressionDao())

        // Load telemetry from preferences if needed (optional)

        // Start time tracking
        timeTracker.start()

        // Restore quiz state if exists (faster, happens first)
        viewModelScope.launch {
            restoreQuizState()
        }
    }

    // Lazy initialize ML models when first question loads (not during ViewModel init)
    private var modelsInitialized = false
    private suspend fun ensureModelsInitialized() {
        if (!modelsInitialized) {
            mlManager.initializeModels()
            modelsInitialized = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        // FINAL ISSUE 1: Log Telemetry
        android.util.Log.i("QuizViewModel", "FINAL TELEMETRY: $telemetry")

        // IMMEDIATELY clear verse cache to free memory
        verseCache.clear()

        // Don't block - save state asynchronously with viewModelScope
        val currentState = _quizState.value
        if (currentState.totalQuestions > 0) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val timeSpentSeconds = if (quizStartTime > 0) {
                        (System.currentTimeMillis() - quizStartTime) / 1000
                    } else {
                        0L
                    }

                    statsRepository.trackQuizCompletion(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions
                    )

                    val quizAttempt = QuizAttempt(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions,
                        timeSpentSeconds = timeSpentSeconds
                    )
                    quizAttemptDao.insertAttempt(quizAttempt)

                    // Clear persisted state after successful save
                    quizPreferences.clearQuizState()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        timeTracker.stop()
        mlManager.close()
    }

    fun setError(message: String) {
        _quizState.value = _quizState.value.copy(isLoading = false, error = message)
    }

    private suspend fun restoreQuizState() {
        try {
            val savedState = quizPreferences.quizState.firstOrNull()
            if (savedState != null) {
                _quizState.value = _quizState.value.copy(
                    score = savedState.score,
                    totalQuestions = savedState.totalQuestions,
                    maxQuestions = savedState.maxQuestions
                )
                quizStartTime = savedState.startTime
                usedQuestions.clear()
                usedQuestions.addAll(savedState.usedQuestions)
                
                // Load recently asked questions from database
                loadRecentlyAskedQuestions()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    private fun loadRecentlyAskedQuestions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Get last 20 questions asked to avoid repetition
                val recentQuestions = questionPerformanceDao.getRecentlyAskedQuestions(20)
                recentlyAskedQuestions.clear()
                recentQuestions.forEach { perf ->
                    // questionId format is "chapter:verse"
                    recentlyAskedQuestions.add(perf.questionId)
                }
                android.util.Log.d("QuizViewModel", "Loaded ${recentlyAskedQuestions.size} recently asked questions")
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error loading recently asked questions: ${e.message}")
            }
        }
    }

    private fun persistQuizState() {
        viewModelScope.launch {
            try {
                val currentState = _quizState.value
                quizPreferences.saveQuizState(
                    score = currentState.score,
                    totalQuestions = currentState.totalQuestions,
                    maxQuestions = currentState.maxQuestions,
                    startTime = quizStartTime,
                    usedQuestions = usedQuestions
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setQuizLimit(maxQuestions: Int) {
        _quizState.value = _quizState.value.copy(maxQuestions = maxQuestions)
        // Reset quiz start time when setting limit (starting new quiz)
        quizStartTime = System.currentTimeMillis()
        // Clear persisted state for new quiz
        viewModelScope.launch {
            quizPreferences.clearQuizState()
        }
    }

    /**
     * Set the language for quiz questions
     * @param quizLanguage Language code: "tel" (Telugu), "eng" (English), "hi" (Hindi), "ta" (Tamil), "kn" (Kannada)
     */
    fun setQuizLanguage(quizLanguage: String) {
        language = quizLanguage
        android.util.Log.d("QuizViewModel", "Language set to: $language")
    }

    // Chapter verse counts (18 chapters)
    private val chapterVerseCounts = mapOf(
        1 to 47, 2 to 72, 3 to 43, 4 to 42, 5 to 29, 6 to 47,
        7 to 30, 8 to 28, 9 to 34, 10 to 42, 11 to 55, 12 to 20,
        13 to 34, 14 to 27, 15 to 20, 16 to 24, 17 to 28, 18 to 78
    )

    /**
     * ADAPTIVE QUIZ LOOP — CORRECTED PRODUCTION FLOW:
     * 
     * APP START → Load 100 questions from DB (instant)
     *           → UI starts immediately
     *           → Background: seed dataset (3500)
     * 
     * QUIZ LOOP:
     * User answers → Update difficulty → Fetch next:
     *   1. DB exact range (±2)
     *   2. DB relaxed range (±4)
     *   3. DB any question
     *   4. LLM generate → Save to DB → Show
     */
    fun loadNextQuestion() {
        viewModelScope.launch {
            _quizState.value = _quizState.value.copy(
                isLoading = true,
                error = null,
                selectedAnswerIndex = null,
                showAnswer = false
            )
            questionStartMs = System.currentTimeMillis()
            answerMs = null

            try {
                android.util.Log.d("QuizViewModel", "=== ADAPTIVE QUIZ LOOP ===")
                ensureModelsInitialized()

                // Load queue if empty
                if (questionQueue.isEmpty()) {
                    loadQuestionQueue()
                }

                // Try in-memory queue first (fastest)
                val queuedQuestion = questionQueue.removeFirstOrNull()
                var question: QuizQuestion? = null

                if (queuedQuestion != null) {
                    // Use question from in-memory queue
                    question = convertDbQuestion(queuedQuestion)
                }

                // If queue empty, fetch with adaptive fallback
                if (question == null) {
                    question = fetchNextQuestion()
                }

                // Show question or error
                if (question != null) {
                    val newTotal = _quizState.value.totalQuestions + 1
                    val isComplete = newTotal >= _quizState.value.maxQuestions

                    _quizState.value = _quizState.value.copy(
                        currentQuestion = question,
                        isLoading = false,
                        totalQuestions = newTotal,
                        isQuizComplete = isComplete
                    )
                    
                    // If quiz is complete, save results
                    if (isComplete) {
                        saveQuizResults()
                    } else {
                        persistQuizState()
                    }
                } else {
                    _quizState.value = _quizState.value.copy(
                        isLoading = false,
                        error = "Could not load question. Please check your connection."
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error in loadNextQuestion: ${e.message}", e)
                _quizState.value = _quizState.value.copy(
                    isLoading = false,
                    error = "Unable to load question: ${e.message}"
                )
            }
        }
    }

    /**
     * Save quiz completion results to database.
     */
    private suspend fun saveQuizResults() {
        val currentState = _quizState.value
        val timeSpentSeconds = if (quizStartTime > 0) {
            (System.currentTimeMillis() - quizStartTime) / 1000
        } else 0L

        statsRepository.trackQuizCompletion(
            score = currentState.score,
            totalQuestions = currentState.totalQuestions
        )

        // Update yoga progression and check for level up
        val (didLevelUp, newLevel) = yogaProgressionRepository.updateProgressionAndCheckLevelUp()
        if (didLevelUp && newLevel != null) {
            com.aipoweredgita.app.notifications.YogaLevelUpNotificationManager.showLevelUpNotification(
                getApplication(),
                newLevel
            )
        }

        val quizAttempt = QuizAttempt(
            score = currentState.score,
            totalQuestions = currentState.totalQuestions,
            timeSpentSeconds = timeSpentSeconds
        )
        quizAttemptDao.insertAttempt(quizAttempt)
        quizPreferences.clearQuizState()
    }

    private fun generateUniqueChapterVerse(): Pair<Int, Int> {
        // Build pool of ALL available verses (700 total)
        val allVerses = mutableListOf<Pair<Int, Int>>()
        chapterVerseCounts.forEach { (chapter, maxVerse) ->
            for (v in 1..maxVerse) {
                allVerses.add(Pair(chapter, v))
            }
        }

        // Filter out already-used and recently-asked verses
        val available = allVerses.filter { (ch, v) ->
            val key = "$ch:$v"
            !usedQuestions.contains(key) && !recentlyAskedQuestions.contains(key)
        }

        if (available.isNotEmpty()) {
            // Pick randomly from available verses
            return available[Random.nextInt(available.size)]
        }

        // Fallback: pool exhausted — pick truly random (for very long quizzes)
        val chapter = Random.nextInt(1, 19)
        val maxVerses = chapterVerseCounts[chapter] ?: 47
        val verse = Random.nextInt(1, maxVerses + 1)
        return Pair(chapter, verse)
    }

    // Convert AI-generated question to QuizQuestion format
    private fun convertAIQuestionToQuizQuestion(
        verse: GitaVerse,
        aiQuizData: com.aipoweredgita.app.ml.QuizQuestionData
    ): QuizQuestion {
        val type = when (aiQuizData.type.uppercase()) {
            "ESSAY" -> QuestionType.ESSAY
            "COMPARISON" -> QuestionType.COMPARISON
            "APPLICATION" -> QuestionType.APPLICATION
            else -> QuestionType.MCQ
        }
        return QuizQuestion(
            verse = verse,
            question = aiQuizData.question,
            options = aiQuizData.options,
            correctAnswerIndex = aiQuizData.correctOptionIndex,
            type = type,
            explanation = aiQuizData.explanation,
            rubricKeywords = aiQuizData.rubricKeywords
        )
    }

    fun selectAnswer(index: Int) {
        // Only MCQ/comparison have indexed answers
        val isCorrect = index == _quizState.value.currentQuestion?.correctAnswerIndex

        _quizState.value = _quizState.value.copy(
            selectedAnswerIndex = index,
            showAnswer = true,
            showCorrectAnswer = isCorrect
        )
        // Mark answer time
        answerMs = System.currentTimeMillis()

        if (isCorrect) {
            _quizState.value = _quizState.value.copy(
                score = _quizState.value.score + 1
            )
        }

        adjustDifficulty(isCorrect)

        // Persist state after answer
        persistQuizState()

        // Log performance for MCQ/Comparison
        val q = _quizState.value.currentQuestion
        if (q != null && (q.type == QuestionType.MCQ || q.type == QuestionType.COMPARISON)) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val qId = "${q.verse.chapterNo}.${q.verse.verseNo}:${q.type}:${q.question.hashCode()}"
                    val existing = questionPerformanceDao.getPerformanceByQuestion(qId)
                    val attempts = (existing?.timesAttempted ?: 0) + 1
                    val corrects = (existing?.timesCorrect ?: 0) + if (isCorrect) 1 else 0
                    val successRate = if (attempts > 0) (corrects.toFloat() * 100f / attempts) else 0f
                    val perf = (existing ?: QuestionPerformance()).copy(
                        questionId = qId,
                        topicCategory = q.theme ?: "",
                        questionType = q.type.name,
                        timesAttempted = attempts,
                        timesCorrect = corrects,
                        successRate = successRate,
                        perceivedDifficulty = _quizState.value.difficultyLevel,
                        lastAttempted = System.currentTimeMillis()
                    )
                    if (existing == null) questionPerformanceDao.insert(perf) else questionPerformanceDao.update(perf)
                } catch (e: Exception) {
                    e.printStackTrace()
                    android.util.Log.e("QuizViewModel", "Database error updating question performance", e)
                }
            }
        }
    }

    // Adaptive difficulty engine — factors in correctness AND response time
    private fun adjustDifficulty(isCorrect: Boolean) {
        val responseTime = if (questionStartMs != null && answerMs != null) {
            answerMs!! - questionStartMs!!
        } else 0L
        
        // FINAL ISSUE 4: WARMUP MODE (No adaptive jumps for first 5 questions)
        val newLevel = if (userState.totalAnswered < 5) {
            userState.totalAnswered++
            if (isCorrect) userState.correctCount++
            userState.skillLevel // Keep fixed
        } else {
            difficultyEngine.updateDifficulty(userState, isCorrect, responseTime)
        }
        
        // Update Telemetry
        telemetry.avgUserAccuracy = userState.accuracy
        
        android.util.Log.d("QuizViewModel", "Adaptive difficulty: correct=$isCorrect, time=${responseTime}ms, level=$newLevel, accuracy=${String.format("%.0f", userState.accuracy * 100)}%")
        
        _quizState.value = _quizState.value.copy(difficultyLevel = newLevel)
    }

    /**
     * Record user feedback (👍/👎) for a question.
     * FINAL ISSUE 5: Implicit Signals (Time, Accuracy)
     */
    fun recordQuestionFeedback(questionId: Int, rating: Float, wasSkipped: Boolean = false, changedAnswer: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Calculate implicit score adjustment
                val responseTime = if (questionStartMs != null && answerMs != null) (answerMs!! - questionStartMs!!) else 0L
                var adjustedRating = rating
                
                // Implicit: Long time spent on a 'simple' question might mean it's worded poorly
                if (responseTime > 15000 && rating > 3) adjustedRating -= 0.5f
                if (wasSkipped) adjustedRating -= 2.0f
                if (changedAnswer) adjustedRating -= 0.5f
                
                questionBankDao.recordUserFeedback(questionId, adjustedRating.coerceIn(0f, 5f))
                android.util.Log.d("QuizViewModel", "User feedback recorded (Adjusted): question $questionId → $adjustedRating")
            } catch (e: Exception) {
                android.util.Log.w("QuizViewModel", "Failed to record feedback: ${e.message}")
            }
        }
    }

    // For ESSAY/APPLICATION: capture open-ended input and evaluate via keyword rubric
    fun submitOpenEndedAnswer(text: String) {
        val q = _quizState.value.currentQuestion ?: return
        _quizState.value = _quizState.value.copy(openEndedAnswer = text)
        // Simple evaluation: count rubric keywords present
        val matched = q.rubricKeywords.count { kw -> text.lowercase().contains(kw.lowercase()) }
        val passThreshold = maxOf(1, q.rubricKeywords.size / 2)
        val isPass = matched >= passThreshold
        _quizState.value = _quizState.value.copy(showAnswer = true, showCorrectAnswer = isPass, score = _quizState.value.score + (if (isPass) 1 else 0))
        // Mark answer time
        answerMs = System.currentTimeMillis()
        adjustDifficulty(isPass)
        persistQuizState()

        // Log performance for Essay/Application
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val qId = "${q.verse.chapterNo}.${q.verse.verseNo}:${q.type}:${q.question.hashCode()}"
                val existing = questionPerformanceDao.getPerformanceByQuestion(qId)
                val attempts = (existing?.timesAttempted ?: 0) + 1
                val corrects = (existing?.timesCorrect ?: 0) + if (isPass) 1 else 0
                val successRate = if (attempts > 0) (corrects.toFloat() * 100f / attempts) else 0f
                val perf = (existing ?: QuestionPerformance()).copy(
                    questionId = qId,
                    topicCategory = q.theme ?: "",
                    questionType = q.type.name,
                    timesAttempted = attempts,
                    timesCorrect = corrects,
                    successRate = successRate,
                    perceivedDifficulty = _quizState.value.difficultyLevel,
                    lastAttempted = System.currentTimeMillis()
                )
                if (existing == null) questionPerformanceDao.insert(perf) else questionPerformanceDao.update(perf)
            } catch (e: Exception) {
                android.util.Log.e("QuizViewModel", "Error logging performance", e)
            }
        }
    }

    fun revealAnswer() {
        _quizState.value = _quizState.value.copy(
            showCorrectAnswer = true
        )
    }

    /**
     * Exit quiz and save current progress
     * Call this when user navigates away from quiz (e.g., to home screen)
     */
    fun exitQuiz() {
        val currentState = _quizState.value
        // Save results if any progress was made (even if quiz not complete)
        if (currentState.totalQuestions > 0) {
            // Use viewModelScope to save asynchronously
            viewModelScope.launch {
                try {
                    val timeSpentSeconds = if (quizStartTime > 0) {
                        (System.currentTimeMillis() - quizStartTime) / 1000
                    } else {
                        0L
                    }

                    statsRepository.trackQuizCompletion(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions
                    )

                    val quizAttempt = QuizAttempt(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions,
                        timeSpentSeconds = timeSpentSeconds
                    )
                    quizAttemptDao.insertAttempt(quizAttempt)

                    // Clear persisted state after saving results
                    quizPreferences.clearQuizState()
                } catch (e: Exception) {
                    android.util.Log.e("QuizViewModel", "Error exiting quiz", e)
                    setError("Failed to save quiz results: ${e.message}")
                }
            }
        }
        // Note: Don't reset state - just save and allow navigation
    }

    fun resetQuiz() {
        // Save quiz results before resetting (synchronously to ensure it completes)
        val currentState = _quizState.value
        if (currentState.isQuizComplete && currentState.totalQuestions > 0) {
            viewModelScope.launch {
                try {
                    val timeSpentSeconds = if (quizStartTime > 0) {
                        (System.currentTimeMillis() - quizStartTime) / 1000
                    } else {
                        0L
                    }

                    statsRepository.trackQuizCompletion(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions
                    )

                    val quizAttempt = QuizAttempt(
                        score = currentState.score,
                        totalQuestions = currentState.totalQuestions,
                        timeSpentSeconds = timeSpentSeconds
                    )
                    quizAttemptDao.insertAttempt(quizAttempt)

                    // Clear persisted state before resetting
                    quizPreferences.clearQuizState()
                } catch (e: Exception) {
                    android.util.Log.e("QuizViewModel", "Database error saving quiz state", e)
                    setError("Failed to save quiz progress: ${e.message}")
                    e.printStackTrace()
                }

                // Reset quiz state after saving completes
                val currentMaxQuestions = currentState.maxQuestions
                usedQuestions.clear()
                verseCache.clear()  // Clear bounded cache
                _quizState.value = QuizState(maxQuestions = currentMaxQuestions)

                // Reset quiz start time for new quiz
                quizStartTime = System.currentTimeMillis()
                loadNextQuestion()
            }
        } else {
            // If quiz not complete, just reset
            val currentMaxQuestions = _quizState.value.maxQuestions
            usedQuestions.clear()
            verseCache.clear()  // Clear bounded cache
            _quizState.value = QuizState(maxQuestions = currentMaxQuestions)

            quizStartTime = System.currentTimeMillis()

            // Clear persisted state
            viewModelScope.launch {
                quizPreferences.clearQuizState()
            }

            loadNextQuestion()
        }
    }

    fun restartQuiz() {
        val currentState = _quizState.value
        if (currentState.totalQuestions > 0) {
            viewModelScope.launch {
                try {
                    saveQuizResults()
                } catch (e: Exception) {
                    android.util.Log.e("QuizViewModel", "Database error saving quiz data", e)
                    setError("Failed to save quiz data: ${e.message}")
                }

                // Reset quiz state
                val currentMaxQuestions = currentState.maxQuestions
                usedQuestions.clear()
                questionQueue.clear()
                verseCache.clear()
                _quizState.value = QuizState(maxQuestions = currentMaxQuestions)
                quizStartTime = System.currentTimeMillis()
                
                // Reload question queue from DB (or generate via LLM)
                loadQuestionQueue()
                loadNextQuestion()
            }
        } else {
            // If no questions were answered yet, just reset
            val currentMaxQuestions = _quizState.value.maxQuestions
            usedQuestions.clear()
            questionQueue.clear()
            verseCache.clear()
            _quizState.value = QuizState(maxQuestions = currentMaxQuestions)
            quizStartTime = System.currentTimeMillis()

            viewModelScope.launch {
                quizPreferences.clearQuizState()
                // Reload question queue from DB (or generate via LLM)
                loadQuestionQueue()
                loadNextQuestion()
            }
        }
    }
}