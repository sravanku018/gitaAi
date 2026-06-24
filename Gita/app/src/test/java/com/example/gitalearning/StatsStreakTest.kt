package com.example.gitalearning

import com.aipoweredgita.app.database.PendingSyncEvent
import com.aipoweredgita.app.database.PendingSyncEventDao
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

class FakeUserStatsDao : UserStatsDao {
    var stats: UserStats? = UserStats(userId = "test_user_id")

    override fun getUserStats(): Flow<UserStats?> {
        return flowOf(stats)
    }

    override suspend fun getUserStatsOnce(): UserStats? {
        return stats
    }

    override suspend fun insertStats(stats: UserStats) {
        this.stats = stats
    }

    override suspend fun updateStats(stats: UserStats) {
        this.stats = stats
    }

    override suspend fun incrementQuizzesTaken() {
        stats = stats?.copy(totalQuizzesTaken = (stats?.totalQuizzesTaken ?: 0) + 1)
    }

    override suspend fun addQuestionsAnswered(count: Int) {
        stats = stats?.copy(totalQuestionsAnswered = (stats?.totalQuestionsAnswered ?: 0) + count)
    }

    override suspend fun addCorrectAnswers(count: Int) {
        stats = stats?.copy(totalCorrectAnswers = (stats?.totalCorrectAnswers ?: 0) + count)
    }

    override suspend fun incrementVersesRead() {
        stats = stats?.copy(versesRead = (stats?.versesRead ?: 0) + 1)
    }

    override suspend fun incrementChaptersCompleted() {
        stats = stats?.copy(chaptersCompleted = (stats?.chaptersCompleted ?: 0) + 1)
    }

    override suspend fun updateDistinctVersesRead(count: Int) {
        stats = stats?.copy(distinctVersesRead = count)
    }

    override suspend fun addTimeSpent(seconds: Long) {
        stats = stats?.copy(totalTimeSpentSeconds = (stats?.totalTimeSpentSeconds ?: 0) + seconds)
    }

    override suspend fun addNormalModeTime(seconds: Long) {
        stats = stats?.copy(
            normalModeTimeSeconds = (stats?.normalModeTimeSeconds ?: 0) + seconds,
            totalTimeSpentSeconds = (stats?.totalTimeSpentSeconds ?: 0) + seconds
        )
    }

    override suspend fun addQuizModeTime(seconds: Long) {
        stats = stats?.copy(
            quizModeTimeSeconds = (stats?.quizModeTimeSeconds ?: 0) + seconds,
            totalTimeSpentSeconds = (stats?.totalTimeSpentSeconds ?: 0) + seconds
        )
    }

    override suspend fun addVoiceStudioTime(seconds: Long) {
        stats = stats?.copy(
            voiceStudioTimeSeconds = (stats?.voiceStudioTimeSeconds ?: 0) + seconds,
            totalTimeSpentSeconds = (stats?.totalTimeSpentSeconds ?: 0) + seconds
        )
    }

    override suspend fun updateLastActive(timestamp: Long, date: String) {
        stats = stats?.copy(lastActiveTimestamp = timestamp, lastActiveDate = date)
    }

    override suspend fun updateCurrentStreak(streak: Int) {
        stats = stats?.copy(currentStreak = streak)
    }

    override suspend fun updateDaysActive(count: Int) {
        stats = stats?.copy(daysActive = count)
    }

    override suspend fun updateLongestStreak(streak: Int) {
        stats = stats?.copy(longestStreak = streak)
    }

    override suspend fun updateBestScore(score: Int, outOf: Int) {
        stats = stats?.copy(bestScore = score, bestScoreOutOf = outOf)
    }

    override suspend fun updateFavoritesCount(count: Int) {
        stats = stats?.copy(totalFavorites = count)
    }

    override suspend fun updateProfile(name: String, dob: String) {
        stats = stats?.copy(userName = name, dateOfBirth = dob)
    }

    override suspend fun updateUserId(userId: String) {
        stats = stats?.copy(userId = userId)
    }

    override suspend fun insertIfEmpty(stats: UserStats) {
        if (this.stats == null) this.stats = stats
    }

    override suspend fun updateKrishnaCoins(coins: Int) {
        stats = stats?.copy(krishnaCoins = coins)
    }

    override suspend fun addKrishnaCoins(amount: Int) {
        stats = stats?.copy(krishnaCoins = (stats?.krishnaCoins ?: 0) + amount)
    }

    override suspend fun syncRemoteStats(
        currentStreak: Int,
        longestStreak: Int,
        totalQuizzesTaken: Int,
        totalQuestionsAnswered: Int,
        versesRead: Int,
        chaptersCompleted: Int,
        daysActive: Int,
        lastActiveDate: String
    ) {
        stats = stats?.copy(
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            totalQuizzesTaken = totalQuizzesTaken,
            totalQuestionsAnswered = totalQuestionsAnswered,
            versesRead = versesRead,
            chaptersCompleted = chaptersCompleted,
            daysActive = daysActive,
            lastActiveDate = lastActiveDate
        )
    }
}

class FakePendingSyncEventDao : PendingSyncEventDao {
    private val list = mutableListOf<PendingSyncEvent>()

    override suspend fun insert(event: PendingSyncEvent) {
        list.add(event)
    }

    override suspend fun getPendingEvents(userId: String): List<PendingSyncEvent> {
        return list.filter { it.userId == userId }
    }

    override suspend fun delete(event: PendingSyncEvent) {
        list.remove(event)
    }

    override suspend fun deleteById(id: Int) {
        list.removeAll { it.id == id }
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StatsStreakTest {
    @Test
    fun testStreakUpdates() = runBlocking {
        val dao = FakeUserStatsDao()
        val pendingDao = FakePendingSyncEventDao()
        val repo = StatsRepository(
            userStatsDao = dao,
            appContext = RuntimeEnvironment.getApplication(),
            pendingSyncEventDao = pendingDao
        )

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
