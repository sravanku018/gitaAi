package com.aipoweredgita.app.database
import androidx.room.*
import kotlinx.coroutines.flow.Flow
@Dao
interface QuizQuestionBankDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(question: QuizQuestionBank)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(questions: List<QuizQuestionBank>)

    @Update
    suspend fun update(question: QuizQuestionBank)

    // Mark question as asked (sets lastAskedAt for cooldown)
    @Query("UPDATE quiz_question_bank SET usageCount = usageCount + 1, lastUsed = :timestamp, lastAskedAt = :timestamp WHERE id = :id")
    suspend fun markAsAsked(id: Int, timestamp: Long = System.currentTimeMillis())

    // Record user feedback (👍/👎) and update quality score
    @Query("""
        UPDATE quiz_question_bank SET 
            userRating = (userRating * ratingCount + :rating) / (ratingCount + 1),
            ratingCount = ratingCount + 1,
            qualityScore = CASE 
                WHEN :rating >= 4 THEN min(100, qualityScore + 5)
                WHEN :rating <= 2 THEN max(0, qualityScore - 10)
                ELSE qualityScore
            END
        WHERE id = :id
    """)
    suspend fun recordUserFeedback(id: Int, rating: Float)

    // PRODUCTION QUERY: Cooldown + difficulty proximity + usage.
    // :cooldownCutoff = System.currentTimeMillis() - cooldownDurationMs
    @Query("""
        SELECT * FROM quiz_question_bank
        WHERE difficulty BETWEEN :minDiff AND :maxDiff
        AND (isActive = 1 OR isActive IS NULL)
        AND (lastAskedAt IS NULL OR lastAskedAt = 0 OR lastAskedAt < :cooldownCutoff)
        ORDER BY 
            usageCount ASC,
            ABS(difficulty - :targetDifficulty) ASC,
            RANDOM()
        LIMIT :limit
    """)
    suspend fun getNextQuestions(
        minDiff: Int,
        maxDiff: Int,
        limit: Int,
        targetDifficulty: Int,
        cooldownCutoff: Long
    ): List<QuizQuestionBank>

    // LANGUAGE-FILTERED QUERY: same as above but only returns questions in specified language.
    @Query("""
        SELECT * FROM quiz_question_bank
        WHERE (:language = '' OR LOWER(language) = LOWER(:language))
        AND difficulty BETWEEN :minDiff AND :maxDiff
        AND (isActive = 1 OR isActive IS NULL)
        AND (lastAskedAt IS NULL OR lastAskedAt = 0 OR lastAskedAt < :cooldownCutoff)
        ORDER BY 
            usageCount ASC,
            ABS(difficulty - :targetDifficulty) ASC,
            RANDOM()
        LIMIT :limit
    """)
    suspend fun getNextQuestionsFiltered(
        minDiff: Int,
        maxDiff: Int,
        limit: Int,
        targetDifficulty: Int,
        cooldownCutoff: Long,
        language: String
    ): List<QuizQuestionBank>

    // FALLBACK QUERY: ignores difficulty and cooldown to ensure questions are always returned
    @Query("""
        SELECT * FROM quiz_question_bank
        WHERE (isActive = 1 OR isActive IS NULL)
        AND (:language = '' OR LOWER(language) = LOWER(:language))
        ORDER BY usageCount ASC, RANDOM()
        LIMIT :limit
    """)
    suspend fun getFallbackQuestions(
        limit: Int,
        language: String
    ): List<QuizQuestionBank>

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE (isActive = 1 OR isActive IS NULL)")
    suspend fun getTotalAvailableQuestions(): Int

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE generationMethod = :method")
    suspend fun getQuestionsBySource(method: String): Int

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE LOWER(language) = LOWER(:language) AND (isActive = 1 OR isActive IS NULL)")
    suspend fun getQuestionsByLanguage(language: String): Int

    // Check for duplicate questions by hash
    @Query("SELECT questionHash FROM quiz_question_bank")
    suspend fun getAllQuestionHashes(): List<String>

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE questionHash = :hash")
    suspend fun countByHash(hash: String): Int

    // Get questions by topic for weak topic targeting.
    @Query("""
        SELECT * FROM quiz_question_bank
        WHERE INSTR(topics, :topic) > 0
        AND (isActive = 1 OR isActive IS NULL)
        AND (lastAskedAt IS NULL OR lastAskedAt = 0 OR lastAskedAt < :cooldownCutoff)
        ORDER BY usageCount ASC, qualityScore DESC, RANDOM()
        LIMIT :limit
    """)
    suspend fun getQuestionsByTopic(
        topic: String,
        limit: Int,
        cooldownCutoff: Long
    ): List<QuizQuestionBank>

    // Deactivate low-quality questions instead of deleting them
    @Query("UPDATE quiz_question_bank SET isActive = 0 WHERE qualityScore < 20 AND usageCount > 10")
    suspend fun deactivateLowQualityQuestions()

    // Apply daily decay to quality scores with a baseline floor (only for unverified questions)
    @Query("UPDATE quiz_question_bank SET qualityScore = MAX(50.0, qualityScore * 0.99) WHERE isVerified = 0")
    suspend fun applyQualityDecay()

    @Query("SELECT COUNT(*) FROM quiz_question_bank")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE generatedBy LIKE :prefix || '%'")
    suspend fun getGeneratedCount(prefix: String): Int

    @Query("""
        SELECT * FROM quiz_question_bank 
        WHERE (generatedBy = '' OR generatedBy IS NULL)
        AND difficulty BETWEEN :minDiff AND :maxDiff
        AND isActive = 1
        ORDER BY usageCount ASC
        LIMIT :limit
    """)
    suspend fun getImportedQuestions(minDiff: Int, maxDiff: Int, limit: Int): List<QuizQuestionBank>
}
