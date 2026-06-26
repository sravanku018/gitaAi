package com.aipoweredgita.app.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserStatsDao {

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStats(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsOnce(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStats(stats: UserStats)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfEmpty(stats: UserStats)

    @Update
    suspend fun updateStats(stats: UserStats)

    @Query("UPDATE user_stats SET totalQuizzesTaken = totalQuizzesTaken + 1 WHERE id = 1")
    suspend fun incrementQuizzesTaken()

    @Query("UPDATE user_stats SET totalQuestionsAnswered = totalQuestionsAnswered + :count WHERE id = 1")
    suspend fun addQuestionsAnswered(count: Int)

    @Query("UPDATE user_stats SET totalCorrectAnswers = totalCorrectAnswers + :count WHERE id = 1")
    suspend fun addCorrectAnswers(count: Int)

    @Query("UPDATE user_stats SET versesRead = versesRead + 1 WHERE id = 1")
    suspend fun incrementVersesRead()

    @Query("UPDATE user_stats SET chaptersCompleted = chaptersCompleted + 1 WHERE id = 1")
    suspend fun incrementChaptersCompleted()

    @Query("UPDATE user_stats SET distinctVersesRead = :count WHERE id = 1")
    suspend fun updateDistinctVersesRead(count: Int)

    @Query("UPDATE user_stats SET totalTimeSpentSeconds = totalTimeSpentSeconds + :seconds WHERE id = 1")
    suspend fun addTimeSpent(seconds: Long)

    @Query("UPDATE user_stats SET normalModeTimeSeconds = normalModeTimeSeconds + :seconds, totalTimeSpentSeconds = totalTimeSpentSeconds + :seconds WHERE id = 1")
    suspend fun addNormalModeTime(seconds: Long)

    @Query("UPDATE user_stats SET quizModeTimeSeconds = quizModeTimeSeconds + :seconds, totalTimeSpentSeconds = totalTimeSpentSeconds + :seconds WHERE id = 1")
    suspend fun addQuizModeTime(seconds: Long)

    @Query("UPDATE user_stats SET voiceStudioTimeSeconds = voiceStudioTimeSeconds + :seconds, totalTimeSpentSeconds = totalTimeSpentSeconds + :seconds WHERE id = 1")
    suspend fun addVoiceStudioTime(seconds: Long)

    @Query("UPDATE user_stats SET lastActiveTimestamp = :timestamp, lastActiveDate = :date WHERE id = 1")
    suspend fun updateLastActive(timestamp: Long, date: String)

    @Query("UPDATE user_stats SET currentStreak = :streak WHERE id = 1")
    suspend fun updateCurrentStreak(streak: Int)

    @Query("UPDATE user_stats SET daysActive = :count WHERE id = 1")
    suspend fun updateDaysActive(count: Int)

    @Query("UPDATE user_stats SET longestStreak = :streak WHERE id = 1")
    suspend fun updateLongestStreak(streak: Int)

    @Query("UPDATE user_stats SET bestScore = :score, bestScoreOutOf = :outOf WHERE id = 1")
    suspend fun updateBestScore(score: Int, outOf: Int)

    @Query("UPDATE user_stats SET totalFavorites = :count WHERE id = 1")
    suspend fun updateFavoritesCount(count: Int)

    @Query("UPDATE user_stats SET userName = :name, dateOfBirth = :dob WHERE id = 1")
    suspend fun updateProfile(name: String, dob: String)

    @Query("UPDATE user_stats SET userId = :userId WHERE id = 1")
    suspend fun updateUserId(userId: String)

    @Query("UPDATE user_stats SET krishnaCoins = :coins WHERE id = 1")
    suspend fun updateKrishnaCoins(coins: Int)

    @Query("UPDATE user_stats SET krishnaCoins = krishnaCoins + :amount WHERE id = 1")
    suspend fun addKrishnaCoins(amount: Int)

    @Query("""
        UPDATE user_stats 
        SET currentStreak = :currentStreak, 
            longestStreak = MAX(longestStreak, :longestStreak), 
            totalQuizzesTaken = MAX(totalQuizzesTaken, :totalQuizzesTaken), 
            totalQuestionsAnswered = MAX(totalQuestionsAnswered, :totalQuestionsAnswered), 
            versesRead = MAX(versesRead, :versesRead), 
            chaptersCompleted = MAX(chaptersCompleted, :chaptersCompleted), 
            daysActive = MAX(daysActive, :daysActive),
            lastActiveDate = CASE WHEN :lastActiveDate != '' THEN :lastActiveDate ELSE lastActiveDate END
        WHERE id = 1
    """)
    suspend fun syncRemoteStats(
        currentStreak: Int,
        longestStreak: Int,
        totalQuizzesTaken: Int,
        totalQuestionsAnswered: Int,
        versesRead: Int,
        chaptersCompleted: Int,
        daysActive: Int,
        lastActiveDate: String = ""
    )

    @Transaction
    suspend fun initializeStatsIfNeeded() {
        // INSERT OR IGNORE ensures only one row (id=1) ever exists,
        // even if two threads call this concurrently.
        insertIfEmpty(UserStats())
        val stats = getUserStatsOnce() ?: return
        // Generate userId once on first launch
        if (stats.userId.isEmpty()) {
            updateUserId(java.util.UUID.randomUUID().toString())
        }
        // Fix corrupted negative coin balance (from stale server overwrites)
        if (stats.krishnaCoins < 0) {
            updateKrishnaCoins(0)
        }
    }
}
