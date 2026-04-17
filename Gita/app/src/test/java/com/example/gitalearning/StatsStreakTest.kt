package com.example.gitalearning

import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class FakeUserStatsDao : UserStatsDao {
    var stats: UserStats? = UserStats()

    override fun getUserStats() = throw UnsupportedOperationException()
    override suspend fun getUserStatsOnce(): UserStats? = stats
    override suspend fun insertStats(stats: UserStats) { this.stats = stats }
    override suspend fun updateStats(stats: UserStats) { this.stats = stats }
    override suspend fun incrementQuizzesTaken() {}
    override suspend fun addQuestionsAnswered(count: Int) {}
    override suspend fun addCorrectAnswers(count: Int) {}
    override suspend fun incrementVersesRead() {}
    override suspend fun updateDistinctVersesRead(count: Int) {}
    override suspend fun addTimeSpent(seconds: Long) {}
    override suspend fun addNormalModeTime(seconds: Long) {}
    override suspend fun addQuizModeTime(seconds: Long) {}
    override suspend fun updateLastActive(timestamp: Long, date: String) {
        stats = (stats ?: UserStats()).copy(lastActiveTimestamp = timestamp, lastActiveDate = date)
    }
    override suspend fun updateCurrentStreak(streak: Int) { stats = (stats ?: UserStats()).copy(currentStreak = streak) }
    override suspend fun updateLongestStreak(streak: Int) { stats = (stats ?: UserStats()).copy(longestStreak = streak) }
    override suspend fun updateBestScore(score: Int, outOf: Int) {}
    override suspend fun updateFavoritesCount(count: Int) {}
    override suspend fun updateProfile(name: String, dob: String) {}
}

class StatsStreakTest {
    @Test
    fun testStreakUpdates() = runBlocking {
        val dao = FakeUserStatsDao()
        val repo = StatsRepository(dao)

        // First update sets streak to 1
        repo.trackModeTime(60, ModeType.NORMAL)
        var s = dao.stats!!
        assertEquals(1, s.currentStreak)
        assertEquals(1, s.longestStreak)

        // Same day again: streak unchanged
        repo.trackModeTime(30, ModeType.QUIZ)
        s = dao.stats!!
        assertEquals(1, s.currentStreak)

        // Simulate yesterday by setting lastActiveDate to yesterday
        val yesterday = java.time.LocalDate.now().minusDays(1).toString()
        dao.stats = s.copy(lastActiveDate = yesterday)

        // Next update increments streak
        repo.trackModeTime(15, ModeType.QUIZ)
        s = dao.stats!!
        assertEquals(2, s.currentStreak)
        assertEquals(2, s.longestStreak)
    }
}
