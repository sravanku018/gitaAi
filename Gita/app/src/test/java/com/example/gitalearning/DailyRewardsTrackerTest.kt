package com.example.gitalearning

import android.content.SharedPreferences
import com.aipoweredgita.app.coin.DailyRewardsTracker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class DailyRewardsTrackerTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var tracker: DailyRewardsTracker

    private val today = LocalDate.now(ZoneId.systemDefault()).toString()
    private val yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString()
    private val twoDaysAgo = LocalDate.now(ZoneId.systemDefault()).minusDays(2).toString()

    @Before
    fun setup() {
        prefs = RuntimeEnvironment.getApplication()
            .getSharedPreferences("daily_rewards_test", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        tracker = DailyRewardsTracker(prefs)
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
    fun `isFreshInstall is true on clean prefs`() {
        assertTrue(tracker.isFreshInstall())
    }

    @Test
    fun `isFreshInstall is false after checkin`() {
        prefs.edit().putString("reward_last_date", today).commit()
        assertFalse(tracker.isFreshInstall())
    }

    @Test
    fun `isFreshInstall is false after share`() {
        prefs.edit().putString("share_reward_last_date", today).commit()
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
    fun `after day 7 claim, KEY_DAY becomes 0`() {
        for (d in 1..7) {
            if (d > 1) simulateNextDay()
            tracker.claimDaily()
        }
        // claimDay7BonusIfEligible sets KEY_DAY=0
        val bonus = tracker.claimDay7BonusIfEligible()
        assertEquals(7, bonus)
        assertEquals(0, prefs.getInt("reward_day", -1))
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
        // Simulate fresh state after week 1 day 7 claim
        prefs.edit()
            .putInt("reward_day", 0)
            .putInt("reward_week", 1)
            .putString("reward_last_date", today)
            .commit()

        tracker.syncWithServer(7, 2, today)

        assertEquals(0, prefs.getInt("reward_day", -1))
        assertEquals(1, prefs.getInt("reward_week", -1))
    }

    @Test
    fun `sync server day 3 week 1 updates day normally`() {
        prefs.edit()
            .putInt("reward_day", 1)
            .putInt("reward_week", 1)
            .commit()

        tracker.syncWithServer(3, 1)

        assertEquals(3, prefs.getInt("reward_day", -1))
        assertEquals(1, prefs.getInt("reward_week", -1))
    }

    // ── syncWithServer — never downgrade ───────────────────────────

    @Test
    fun `sync does not downgrade week`() {
        prefs.edit()
            .putInt("reward_day", 3)
            .putInt("reward_week", 2)
            .commit()

        // Server has older week — should not overwrite
        tracker.syncWithServer(5, 1)

        assertEquals(3, prefs.getInt("reward_day", -1))
        assertEquals(2, prefs.getInt("reward_week", -1))
    }

    @Test
    fun `sync does not downgrade day in same week`() {
        prefs.edit()
            .putInt("reward_day", 5)
            .putInt("reward_week", 1)
            .commit()

        tracker.syncWithServer(3, 1)

        assertEquals(5, prefs.getInt("reward_day", -1))
    }

    @Test
    fun `sync does not downgrade from complete to in-progress`() {
        prefs.edit()
            .putInt("reward_day", 0) // week complete
            .putInt("reward_week", 1)
            .commit()

        // Server says day 3 of same week — local is already complete
        tracker.syncWithServer(3, 1)

        assertEquals(0, prefs.getInt("reward_day", -1))
        assertEquals(1, prefs.getInt("reward_week", -1))
    }

    @Test
    fun `sync upgrades from in-progress to complete`() {
        prefs.edit()
            .putInt("reward_day", 3)
            .putInt("reward_week", 1)
            .commit()

        // Server says day 7 week 2 → effective day 0 week 1 (completed week 1)
        tracker.syncWithServer(7, 2)

        assertEquals(0, prefs.getInt("reward_day", -1))
        assertEquals(1, prefs.getInt("reward_week", -1))
    }

    @Test
    fun `sync upgrades to newer week from another device`() {
        prefs.edit()
            .putInt("reward_day", 2)
            .putInt("reward_week", 1)
            .commit()

        // Server has week 2 day 3 (user completed week 1 on another device)
        tracker.syncWithServer(3, 2)

        assertEquals(3, prefs.getInt("reward_day", -1))
        assertEquals(2, prefs.getInt("reward_week", -1))
    }

    // ── Migration: corrupted KEY_DAY=7 from old sync ──────────────

    @Test
    fun `migration fixes corrupted day 7 week 2 on same day`() {
        // Simulate corrupted state: old sync set KEY_DAY=7, KEY_WEEK=2, KEY_DATE=today
        prefs.edit()
            .putInt("reward_day", 7)
            .putInt("reward_week", 2)
            .putString("reward_last_date", today)
            .commit()

        val state = tracker.getDailyState()

        // Migration resets KEY_DATE to yesterday, so "next day" logic fires:
        // rawDay=0, week=1 → nextDay=1, nextWeek=2
        assertEquals(1, state.day)
        assertEquals(2, state.week)
        assertFalse(state.todayClaimed)

        // Verify persisted fix
        assertEquals(0, prefs.getInt("reward_day", -1))
        assertEquals(1, prefs.getInt("reward_week", -1))
    }

    @Test
    fun `migration does not affect normal day 7 claim`() {
        // Normal state: KEY_DAY=7, KEY_WEEK=1, KEY_DATE=today (claim in progress)
        prefs.edit()
            .putInt("reward_day", 7)
            .putInt("reward_week", 1)
            .putString("reward_last_date", today)
            .commit()

        val state = tracker.getDailyState()

        // Migration: week 1 → week-1=0 → clamped to 1. day 7 → 0 → displayed as 7
        assertEquals(7, state.day)
        assertEquals(1, state.week)
        assertTrue(state.todayClaimed)
    }

    // ── Migration: corrupted KEY_SHARE_DAY=7 ──────────────────────

    @Test
    fun `share migration fixes corrupted day 7 week 2`() {
        prefs.edit()
            .putInt("share_reward_day", 7)
            .putInt("share_reward_week", 2)
            .putString("share_reward_last_date", today)
            .commit()

        val state = tracker.getShareState()

        assertEquals(1, state.day)
        assertEquals(2, state.week)
        assertFalse(state.todayClaimed)
    }

    // ── Full scenario: Week 1 complete → sync → Week 2 starts ────

    @Test
    fun `full week 1 then sync then week 2 day 1`() {
        // 1. Complete week 1 locally
        completeWeek1()

        // 2. Server syncs day=7 week=2 (the old bug scenario)
        tracker.syncWithServer(7, 2, today)

        // 3. Verify state: sync maps to day=0 week=1, KEY_DATE=today
        //    getDailyState sees rawDay=0, lastDate=today → day=7, week=1, todayClaimed=true
        val daily = tracker.getDailyState()
        assertEquals(7, daily.day)
        assertEquals(1, daily.week)
        assertTrue(daily.todayClaimed)

        val weekly = tracker.getWeeklyState()
        assertEquals(1, weekly.week)

        // 4. Next day: should show day 1 of week 2
        simulateNextDay()
        val nextDay = tracker.getDailyState()
        assertEquals(1, nextDay.day)
        assertEquals(2, nextDay.week)
        assertFalse(nextDay.todayClaimed)
    }

    // ── Share sync same behavior ───────────────────────────────────

    @Test
    fun `share sync server day 7 week 2 maps to day 0 week 1`() {
        prefs.edit()
            .putInt("share_reward_day", 0)
            .putInt("share_reward_week", 1)
            .putString("share_reward_last_date", today)
            .commit()

        tracker.syncShareWithServer(7, 2, today)

        assertEquals(0, prefs.getInt("share_reward_day", -1))
        assertEquals(1, prefs.getInt("share_reward_week", -1))
    }

    // ── Week 4 wraps to Week 1 ────────────────────────────────────

    @Test
    fun `week 4 day 7 wraps to week 1`() {
        prefs.edit()
            .putInt("reward_day", 0)
            .putInt("reward_week", 4)
            .putString("reward_last_date", yesterday)
            .commit()

        val state = tracker.getDailyState()
        assertEquals(1, state.day)
        assertEquals(1, state.week)
    }

    @Test
    fun `sync server day 7 week 1 maps to day 0 week 4 (wrap)`() {
        // Server wrapped: day=7, week=1 means week 4 just completed
        prefs.edit()
            .putInt("reward_day", 0)
            .putInt("reward_week", 4)
            .putString("reward_last_date", today)
            .commit()

        tracker.syncWithServer(7, 1, today)

        assertEquals(0, prefs.getInt("reward_day", -1))
        assertEquals(4, prefs.getInt("reward_week", -1))
    }

    @Test
    fun `day 7 week 1 with today date is treated as in-progress claim (not corrupted)`() {
        // KEY_DAY=7, KEY_WEEK=1, lastDate=today — could be:
        // (a) legitimate day 7 claim in week 1 (app crashed before bonus), or
        // (b) corrupted data from week 4 wrap
        // We treat it as (a) since week=1 is the common case for new users.
        prefs.edit()
            .putInt("reward_day", 7)
            .putInt("reward_week", 1)
            .putString("reward_last_date", today)
            .commit()

        val state = tracker.getDailyState()

        assertEquals(7, state.day)
        assertEquals(1, state.week)
        assertTrue(state.todayClaimed)
        // No migration — KEY_DAY stays 7 (will be fixed by claimDay7BonusIfEligible on retry)
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
        // Should be: day=0, week=2 (not corrupted to day=7 week=3)
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
        // Server wrapped: week=1 after completing week 4
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
        prefs.edit()
            .putInt("reward_day", 3)
            .putInt("reward_week", 1)
            .putInt("reward_protection", 1)
            .putString("reward_last_date", twoDaysAgo)
            .commit()

        val state = tracker.getDailyState()
        assertEquals(4, state.day)
        assertTrue(state.hasProtection)
        assertTrue(state.protectionWillAutoAdvance)
    }

    // ── Missed days reset to week 1 ───────────────────────────────

    @Test
    fun `missed days without protection resets to week 1`() {
        prefs.edit()
            .putInt("reward_day", 5)
            .putInt("reward_week", 2)
            .putString("reward_last_date", twoDaysAgo)
            .commit()

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
        // Set up state as if we're at the start of the given week
        if (weekNum > 1) {
            prefs.edit()
                .putInt("reward_day", 0)
                .putInt("reward_week", weekNum - 1)
                .putString("reward_last_date", yesterday)
                .commit()
            simulateNextDay()
        }
        for (d in 1..7) {
            if (d > 1) simulateNextDay()
            tracker.claimDaily()
        }
        tracker.claimDay7BonusIfEligible()
    }

    private fun simulateNextDay() {
        // Set KEY_DATE to yesterday so getDailyState() sees lastDate == yesterday
        prefs.edit().putString("reward_last_date", yesterday)
            .putString("share_reward_last_date", yesterday)
            .commit()
    }
}
