package com.example.gitalearning

import com.aipoweredgita.app.coin.DailyRewardsTracker
import com.aipoweredgita.app.database.RewardState
import com.aipoweredgita.app.database.RewardStateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

class FakeRewardStateDao : RewardStateDao {
    var state: RewardState = RewardState()

    override fun getRewardStateFlow(): Flow<RewardState?> = flowOf(state)

    override fun getRewardStateSync(): RewardState? = state

    override fun insertOrUpdate(state: RewardState) {
        this.state = state
    }

    override fun update(state: RewardState) {
        this.state = state
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DailyRewardsTrackerTest {

    private lateinit var dao: FakeRewardStateDao
    private lateinit var tracker: DailyRewardsTracker

    private val today = LocalDate.now(ZoneId.systemDefault()).toString()
    private val yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString()
    private val twoDaysAgo = LocalDate.now(ZoneId.systemDefault()).minusDays(2).toString()

    @Before
    fun setup() {
        dao = FakeRewardStateDao()
        tracker = DailyRewardsTracker(dao)
    }

    // ── Fresh install ──────────────────────────────────────────────

    @Test
    fun `fresh install returns day 1 week 1`() {
        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(1, state.week)
        assertFalse(state.todayClaimed)
    }

    @Test
    fun `isFreshInstall is true on clean state`() {
        assertTrue(tracker.isFreshInstall())
    }

    @Test
    fun `isFreshInstall is false after checkin`() {
        dao.state = dao.state.copy(lastCheckinDate = today)
        assertFalse(tracker.isFreshInstall())
    }

    @Test
    fun `isFreshInstall is false after share`() {
        dao.state = dao.state.copy(lastShareDate = today)
        assertFalse(tracker.isFreshInstall())
    }

    // ── Normal daily progression ───────────────────────────────────

    @Test
    fun `claim day 1`() {
        val reward = tracker.claimDaily()
        assertEquals(1, reward)
        val state = tracker.getDailyState()
        assertTrue(state.todayClaimed)
        assertEquals(1, state.day)
    }

    @Test
    fun `cannot claim twice same day`() {
        tracker.claimDaily()
        val reward = tracker.claimDaily()
        assertEquals(0, reward)
    }

    @Test
    fun `day 2 appears next day`() {
        tracker.claimDaily()
        simulateNextDay()
        val state = tracker.getDailyState()
        assertEquals(2, state.day)
        assertFalse(state.todayClaimed)
    }

    @Test
    fun `full week 1 progression`() {
        for (d in 1..7) {
            if (d > 1) simulateNextDay()
            val state = tracker.getDailyState()
            assertEquals(d, state.day)
            assertEquals(1, state.week)
            val reward = tracker.claimDaily()
            assertEquals(d, reward)
        }
    }

    // ── Day 7 completion and week transition ──────────────────────

    @Test
    fun `after day 7 claim, checkinDay becomes 0`() {
        for (d in 1..7) {
            if (d > 1) simulateNextDay()
            tracker.claimDaily()
        }
        val bonus = tracker.claimDay7BonusIfEligible()
        assertEquals(7, bonus)
        assertEquals(0, dao.state.checkinDay)
    }

    @Test
    fun `next day after week 1 completion shows day 1 of week 2`() {
        completeWeek1()
        simulateNextDay()
        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(2, state.week)
        assertFalse(state.todayClaimed)
    }

    @Test
    fun `weekly state shows correct week after week 1`() {
        completeWeek1()
        val ws = tracker.getWeeklyState()
        assertEquals(1, ws.week)
        assertEquals(10, ws.reward)
    }

    @Test
    fun `weekly state shows week 2 after claiming day 1 of week 2`() {
        completeWeek1()
        simulateNextDay()
        tracker.claimDaily()
        val ws = tracker.getWeeklyState()
        assertEquals(2, ws.week)
    }

    // ── syncWithServer — server day=7 mapping ─────────────────────

    @Test
    fun `sync server day 7 week 2 maps to day 0 week 1`() {
        dao.state = dao.state.copy(checkinDay = 0, checkinWeek = 1, lastCheckinDate = today)

        tracker.syncWithServer(7, 2, today)

        assertEquals(0, dao.state.checkinDay)
        assertEquals(1, dao.state.checkinWeek)
    }

    @Test
    fun `sync server day 3 week 1 updates day normally`() {
        dao.state = dao.state.copy(checkinDay = 1, checkinWeek = 1)

        tracker.syncWithServer(3, 1)

        assertEquals(3, dao.state.checkinDay)
        assertEquals(1, dao.state.checkinWeek)
    }

    // ── syncWithServer — never downgrade ───────────────────────────

    @Test
    fun `sync does not downgrade week`() {
        dao.state = dao.state.copy(checkinDay = 3, checkinWeek = 2)

        tracker.syncWithServer(5, 1)

        assertEquals(3, dao.state.checkinDay)
        assertEquals(2, dao.state.checkinWeek)
    }

    @Test
    fun `sync does not downgrade day in same week`() {
        dao.state = dao.state.copy(checkinDay = 5, checkinWeek = 1)

        tracker.syncWithServer(3, 1)

        assertEquals(5, dao.state.checkinDay)
    }

    @Test
    fun `sync does not downgrade from complete to in-progress`() {
        dao.state = dao.state.copy(checkinDay = 0, checkinWeek = 1)

        tracker.syncWithServer(3, 1)

        assertEquals(0, dao.state.checkinDay)
        assertEquals(1, dao.state.checkinWeek)
    }

    @Test
    fun `sync upgrades from in-progress to complete`() {
        dao.state = dao.state.copy(checkinDay = 3, checkinWeek = 1)

        tracker.syncWithServer(7, 2)

        assertEquals(0, dao.state.checkinDay)
        assertEquals(1, dao.state.checkinWeek)
    }

    @Test
    fun `sync upgrades to newer week from another device`() {
        dao.state = dao.state.copy(checkinDay = 2, checkinWeek = 1)

        tracker.syncWithServer(3, 2)

        assertEquals(3, dao.state.checkinDay)
        assertEquals(2, dao.state.checkinWeek)
    }

    // ── Migration: corrupted KEY_DAY=7 from old sync ──────────────

    @Test
    fun `migration fixes corrupted day 7 week 2 on same day`() {
        dao.state = dao.state.copy(checkinDay = 7, checkinWeek = 2, lastCheckinDate = today)

        val state = tracker.getDailyState()

        assertEquals(1, state.day)
        assertEquals(2, state.week)
        assertFalse(state.todayClaimed)

        assertEquals(0, dao.state.checkinDay)
        assertEquals(1, dao.state.checkinWeek)
    }

    @Test
    fun `migration does not affect normal day 7 claim`() {
        dao.state = dao.state.copy(checkinDay = 7, checkinWeek = 1, lastCheckinDate = today)

        val state = tracker.getDailyState()

        assertEquals(7, state.day)
        assertEquals(1, state.week)
        assertTrue(state.todayClaimed)
    }

    // ── Migration: corrupted KEY_SHARE_DAY=7 ──────────────────────

    @Test
    fun `share migration fixes corrupted day 7 week 2`() {
        dao.state = dao.state.copy(shareDay = 7, shareWeek = 2, lastShareDate = today)

        val state = tracker.getShareState()

        assertEquals(1, state.day)
        assertEquals(2, state.week)
        assertFalse(state.todayClaimed)
    }

    // ── Full scenario: Week 1 complete → sync → Week 2 starts ────

    @Test
    fun `full week 1 then sync then week 2 day 1`() {
        completeWeek1()

        tracker.syncWithServer(7, 2, today)

        val daily = tracker.getDailyState()
        assertEquals(7, daily.day)
        assertEquals(1, daily.week)
        assertTrue(daily.todayClaimed)

        val weekly = tracker.getWeeklyState()
        assertEquals(1, weekly.week)

        simulateNextDay()
        val nextDay = tracker.getDailyState()
        assertEquals(1, nextDay.day)
        assertEquals(2, nextDay.week)
        assertFalse(nextDay.todayClaimed)
    }

    // ── Share sync same behavior ───────────────────────────────────

    @Test
    fun `share sync server day 7 week 2 maps to day 0 week 1`() {
        dao.state = dao.state.copy(shareDay = 0, shareWeek = 1, lastShareDate = today)

        tracker.syncShareWithServer(7, 2, today)

        assertEquals(0, dao.state.shareDay)
        assertEquals(1, dao.state.shareWeek)
    }

    // ── Week 4 wraps to Week 1 ────────────────────────────────────

    @Test
    fun `week 4 day 7 wraps to week 1`() {
        dao.state = dao.state.copy(checkinDay = 0, checkinWeek = 4, lastCheckinDate = yesterday)

        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(1, state.week)
    }

    @Test
    fun `sync server day 7 week 1 maps to day 0 week 4 (wrap)`() {
        dao.state = dao.state.copy(checkinDay = 0, checkinWeek = 4, lastCheckinDate = today)

        tracker.syncWithServer(7, 1, today)

        assertEquals(0, dao.state.checkinDay)
        assertEquals(4, dao.state.checkinWeek)
    }

    @Test
    fun `day 7 week 1 with today date is treated as in-progress claim (not corrupted)`() {
        dao.state = dao.state.copy(checkinDay = 7, checkinWeek = 1, lastCheckinDate = today)

        val state = tracker.getDailyState()

        assertEquals(7, state.day)
        assertEquals(1, state.week)
        assertTrue(state.todayClaimed)
    }

    @Test
    fun `next day after week 4 completion wraps to week 1 day 1`() {
        completeWeek(4)
        simulateNextDay()
        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(1, state.week)
    }

    // ── Week 2 → Week 3 transition ────────────────────────────────

    @Test
    fun `full week 2 then week 3 starts`() {
        completeWeek(2)
        simulateNextDay()
        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(3, state.week)
    }

    // ── Week 3 → Week 4 transition ────────────────────────────────

    @Test
    fun `full week 3 then week 4 starts`() {
        completeWeek(3)
        simulateNextDay()
        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(4, state.week)
    }

    // ── Multi-week: sync at each boundary ──────────────────────────

    @Test
    fun `week 2 sync then week 3 starts correctly`() {
        completeWeek(2)
        tracker.syncWithServer(7, 3, today)
        val daily = tracker.getDailyState()
        assertEquals(7, daily.day)
        assertEquals(2, daily.week)
        assertTrue(daily.todayClaimed)

        simulateNextDay()
        val next = tracker.getDailyState()
        assertEquals(1, next.day)
        assertEquals(3, next.week)
    }

    @Test
    fun `week 3 sync then week 4 starts correctly`() {
        completeWeek(3)
        tracker.syncWithServer(7, 4, today)
        val daily = tracker.getDailyState()
        assertEquals(7, daily.day)
        assertEquals(3, daily.week)

        simulateNextDay()
        val next = tracker.getDailyState()
        assertEquals(1, next.day)
        assertEquals(4, next.week)
    }

    @Test
    fun `week 4 sync then week 1 starts correctly (wrap)`() {
        completeWeek(4)
        tracker.syncWithServer(7, 1, today)
        val daily = tracker.getDailyState()
        assertEquals(7, daily.day)
        assertEquals(4, daily.week)

        simulateNextDay()
        val next = tracker.getDailyState()
        assertEquals(1, next.day)
        assertEquals(1, next.week)
    }

    // ── Protection ─────────────────────────────────────────────────

    @Test
    fun `protection allows advancing past missed day`() {
        dao.state = dao.state.copy(
            checkinDay = 3,
            checkinWeek = 1,
            checkinProtectionUsed = false,
            checkinProtectionGranted = true,
            lastCheckinDate = twoDaysAgo
        )

        val state = tracker.getDailyState()
        assertEquals(4, state.day)
        assertTrue(state.hasProtection)
        assertTrue(state.protectionWillAutoAdvance)
    }

    // ── Missed days reset to week 1 ───────────────────────────────

    @Test
    fun `missed days without protection resets to week 1`() {
        dao.state = dao.state.copy(
            checkinDay = 5,
            checkinWeek = 2,
            checkinProtectionUsed = true,
            lastCheckinDate = twoDaysAgo
        )

        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(1, state.week)
        assertFalse(state.hasProtection)
    }

    // ── Helpers ────────────────────────────────────────────────────

    private fun completeWeek1() {
        completeWeek(1)
    }

    private fun completeWeek(weekNum: Int) {
        if (weekNum > 1) {
            dao.state = dao.state.copy(
                checkinDay = 0,
                checkinWeek = weekNum - 1,
                lastCheckinDate = yesterday
            )
            simulateNextDay()
        }
        for (d in 1..7) {
            if (d > 1) simulateNextDay()
            tracker.claimDaily()
        }
        tracker.claimDay7BonusIfEligible()
    }

    private fun simulateNextDay() {
        dao.state = dao.state.copy(
            lastCheckinDate = yesterday,
            lastShareDate = yesterday
        )
    }
}
