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
        AND isActive = 1 AND isApproved = 1
        AND lastAskedAt < :cooldownCutoff
        ORDER BY 
            usageCount ASC,
            ABS(difficulty - :targetDifficulty) ASC,
            createdAt ASC
        LIMIT :limit
    """)
    suspend fun getNextQuestions(
        minDiff: Int,
        maxDiff: Int,
        limit: Int,
        targetDifficulty: Int = 5,
        cooldownCutoff: Long = System.currentTimeMillis() - 30 * 60 * 1000L
    ): List<QuizQuestionBank>

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE isActive = 1 AND isApproved = 1")
    suspend fun getTotalAvailableQuestions(): Int

    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE generationMethod = :method")
    suspend fun getQuestionsBySource(method: String): Int

    // Check for duplicate questions by hash
    @Query("SELECT COUNT(*) FROM quiz_question_bank WHERE questionHash = :hash")
    suspend fun countByHash(hash: String): Int

    // Get questions by topic for weak topic targeting.
    // :cooldownCutoff = System.currentTimeMillis() - cooldownDurationMs
    @Query("""
        SELECT * FROM quiz_question_bank
        WHERE topics LIKE '%' || :topic || '%'
        AND isActive = 1 AND isApproved = 1
        AND lastAskedAt < :cooldownCutoff
        ORDER BY usageCount ASC, qualityScore DESC, RANDOM()
        LIMIT :limit
    """)
    suspend fun getQuestionsByTopic(
        topic: String,
        limit: Int,
        cooldownCutoff: Long = System.currentTimeMillis() - 30 * 60 * 1000L
    ): List<QuizQuestionBank>

    // FIX ISSUE 5: Deactivate low-quality questions instead of deleting them
    @Query("UPDATE quiz_question_bank SET isActive = 0 WHERE qualityScore < 20 AND usageCount > 10")
    suspend fun deactivateLowQualityQuestions()

    // FIX ISSUE 6: Apply daily decay to quality scores (0.99 multiplier)
    @Query("UPDATE quiz_question_bank SET qualityScore = qualityScore * 0.99")
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
