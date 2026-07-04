package com.example.gitalearning

import com.aipoweredgita.app.coin.DailyRewardsTracker
import com.aipoweredgita.app.repository.ModeType
import com.aipoweredgita.app.repository.StatsRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class StatsRepositoryTest {

    private lateinit var dao: FakeUserStatsDao
    private lateinit var pendingDao: FakePendingSyncEventDao
    private lateinit var repo: StatsRepository
    private lateinit var mockTracker: DailyRewardsTracker

    @Before
    fun setup() {
        dao = FakeUserStatsDao()
        pendingDao = FakePendingSyncEventDao()

        mockTracker = mockk(relaxed = true)
        every { mockTracker.getCurrentCheckinDay() } returns 1

        // Mock the companion object's getInstance to return our mock tracker
        mockkObject(DailyRewardsTracker.Companion)
        every { DailyRewardsTracker.getInstance(any()) } returns mockTracker

        repo = StatsRepository(
            userStatsDao = dao,
            appContext = RuntimeEnvironment.getApplication(),
            pendingSyncEventDao = pendingDao
        )
    }

    @After
    fun tearDown() {
        unmockkObject(DailyRewardsTracker.Companion)
    }

    @Test
    fun trackQuizCompletion_incrementsQuizCount() = runBlocking {
        repo.trackQuizCompletion(score = 8, totalQuestions = 10, quizType = "general")
        val stats = dao.stats!!
        assertEquals(1, stats.totalQuizzesTaken)
        assertEquals(10, stats.totalQuestionsAnswered)
        assertEquals(8, stats.totalCorrectAnswers)
    }

    @Test
    fun trackQuizCompletion_multipleQuizzes_accumulates() = runBlocking {
        repo.trackQuizCompletion(score = 5, totalQuestions = 10, quizType = "general")
        repo.trackQuizCompletion(score = 7, totalQuestions = 10, quizType = "general")
        val stats = dao.stats!!
        assertEquals(2, stats.totalQuizzesTaken)
        assertEquals(20, stats.totalQuestionsAnswered)
        assertEquals(12, stats.totalCorrectAnswers)
    }

    @Test
    fun trackVerseRead_incrementsCount() = runBlocking {
        repo.trackVerseRead()
        repo.trackVerseRead()
        val stats = dao.stats!!
        assertEquals(2, stats.versesRead)
    }

    @Test
    fun trackChapterCompleted_incrementsCount() = runBlocking {
        repo.trackChapterCompleted(1)
        val stats = dao.stats!!
        assertEquals(1, stats.chaptersCompleted)
    }

    @Test
    fun trackModeTime_updatesTimeSpent() = runBlocking {
        repo.trackModeTime(120, ModeType.NORMAL)
        val stats = dao.stats!!
        assertEquals(120L, stats.normalModeTimeSeconds)
        assertEquals(120L, stats.totalTimeSpentSeconds)
    }

    @Test
    fun trackModeTime_quizMode_tracksSeparately() = runBlocking {
        repo.trackModeTime(60, ModeType.QUIZ)
        val stats = dao.stats!!
        assertEquals(60L, stats.quizModeTimeSeconds)
        assertEquals(60L, stats.totalTimeSpentSeconds)
    }

    @Test
    fun trackModeTime_voiceMode_tracksSeparately() = runBlocking {
        repo.trackModeTime(90, ModeType.VOICE)
        val stats = dao.stats!!
        assertEquals(90L, stats.voiceStudioTimeSeconds)
        assertEquals(90L, stats.totalTimeSpentSeconds)
    }
}
