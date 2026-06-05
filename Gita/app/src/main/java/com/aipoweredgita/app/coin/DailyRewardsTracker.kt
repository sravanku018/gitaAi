package com.aipoweredgita.app.coin

import android.content.Context
import java.time.LocalDate

/**
 * Daily check-in, weekly bonus, and share reward tracking.
 * All state persisted in SharedPreferences — no Room needed.
 *
 * Thread-safe: all write methods synchronize.
 * Corrupt dates: silently default rather than crash.
 */
class DailyRewardsTracker(private val prefs: android.content.SharedPreferences) {

    companion object {
        // Check-in keys
        private const val KEY_DAY = "reward_day"
        private const val KEY_DATE = "reward_last_date"
        private const val KEY_PROTECTION = "reward_protection"
        private const val KEY_WEEK = "reward_week"
        private const val KEY_WEEK_CLAIMED = "reward_week_claimed"

        // Daily share keys
        private const val KEY_SHARE_DAY = "share_reward_day"
        private const val KEY_SHARE_DATE = "share_reward_last_date"
        private const val KEY_SHARE_PROTECTION = "share_reward_protection"
        private const val KEY_SHARE_WEEK = "share_reward_week"
        private const val KEY_SHARE_WEEK_CLAIMED = "share_reward_week_claimed"

        fun getInstance(context: Context): DailyRewardsTracker {
            val p = context.getSharedPreferences("daily_rewards", Context.MODE_PRIVATE)
            return DailyRewardsTracker(p)
        }
    }

    var isCheckinSynced: Boolean
        get() {
            val today = now()
            val lastDate = prefs.getString(KEY_DATE, "") ?: ""
            if (lastDate != today) return false
            return prefs.getBoolean("checkin_synced", false)
        }
        set(value) {
            prefs.edit().putBoolean("checkin_synced", value).apply()
        }

    var isShareSynced: Boolean
        get() {
            val today = now()
            val lastDate = prefs.getString(KEY_SHARE_DATE, "") ?: ""
            if (lastDate != today) return false
            return prefs.getBoolean("share_synced", false)
        }
        set(value) {
            prefs.edit().putBoolean("share_synced", value).apply()
        }

    // ── Daily Check-in ───────────────────────────────────────────────────────

    data class DailyState(
        val day: Int, 
        val todayClaimed: Boolean, 
        val reward: Int,
        val hasProtection: Boolean, 
        val protectionWillAutoAdvance: Boolean, 
        val week: Int
    )

    fun getDailyState(): DailyState {
        val today = now()
        val lastDate = prefs.getString(KEY_DATE, "") ?: ""
        val week = prefs.getInt(KEY_WEEK, 1).coerceIn(1, 4)
        val protection = prefs.getInt(KEY_PROTECTION, 0).coerceAtLeast(0)
        val rawDay = prefs.getInt(KEY_DAY, 1).coerceIn(0, 7)

        if (lastDate == today) {
            return DailyState(
                day = if (rawDay == 0) 7 else rawDay,
                todayClaimed = true,
                reward = if (rawDay == 0) 7 else rawDay,
                hasProtection = protection > 0,
                protectionWillAutoAdvance = false,
                week = week
            )
        }

        if (lastDate.isEmpty()) {
            return DailyState(
                day = 1,
                todayClaimed = false,
                reward = 1,
                hasProtection = protection > 0,
                protectionWillAutoAdvance = false,
                week = 1
            )
        }

        val yesterday = yesterdayStr(today)
        if (lastDate == yesterday) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return DailyState(
                day = nextDay,
                todayClaimed = false,
                reward = nextDay,
                hasProtection = protection > 0,
                protectionWillAutoAdvance = false,
                week = nextWeek
            )
        }

        // Missed day(s)
        if (protection > 0) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return DailyState(
                day = nextDay,
                todayClaimed = false,
                reward = nextDay,
                hasProtection = true,
                protectionWillAutoAdvance = true,
                week = nextWeek
            )
        }

        // Reset to Week 1, Day 1
        return DailyState(
            day = 1,
            todayClaimed = false,
            reward = 1,
            hasProtection = false,
            protectionWillAutoAdvance = false,
            week = 1
        )
    }

    fun claimDaily(): Int {
        val s = getDailyState()
        if (s.todayClaimed) return 0
        synchronized(this) {
            if (prefs.getString(KEY_DATE, "") == now()) return 0
            val editor = prefs.edit()
            if (s.protectionWillAutoAdvance) {
                val p = (prefs.getInt(KEY_PROTECTION, 0) - 1).coerceAtLeast(0)
                editor.putInt(KEY_PROTECTION, p)
            }
            editor.putInt(KEY_DAY, s.day)
            editor.putInt(KEY_WEEK, s.week)
            editor.putString(KEY_DATE, now())
            editor.putBoolean("checkin_synced", false)
            editor.commit()  // synchronous — prevents race with async reads
            return s.reward
        }
    }

    fun claimDay7BonusIfEligible(): Int = synchronized(this) {
        val day = prefs.getInt(KEY_DAY, 1)
        val last = prefs.getString(KEY_DATE, "") ?: ""
        val today = now()
        if (day == 7 && last == today) {
            prefs.edit().apply {
                putInt(KEY_DAY, 0)
                putInt(KEY_PROTECTION, prefs.getInt(KEY_PROTECTION, 0) + 1)
            }.commit()  // synchronous — Day 7 bonus is important
            return 7
        }
        return 0
    }

    fun getCurrentCheckinDay(): Int = prefs.getInt(KEY_DAY, 1).coerceIn(1, 7)

    // ── Weekly Bonus ─────────────────────────────────────────────────────────

    data class WeeklyState(val week: Int, val claimed: Boolean, val reward: Int)

    fun getWeeklyState(): WeeklyState {
        val week = prefs.getInt(KEY_WEEK, 1).coerceIn(1, 4)
        val claimedWeek = prefs.getString(KEY_WEEK_CLAIMED, "") ?: ""
        val today = now()
        val claimed = claimedWeek == today
        return WeeklyState(week, claimed, if (week == 4) 20 else 10)
    }

    fun claimWeekly(): Int {
        val s = getWeeklyState()
        if (s.claimed) return 0
        synchronized(this) {
            val today = now()
            if (prefs.getString(KEY_WEEK_CLAIMED, "") == today) return 0
            prefs.edit().putString(KEY_WEEK_CLAIMED, today).commit()  // synchronous — weekly claim must persist immediately
            return s.reward
        }
    }

    // ── Daily Share ──────────────────────────────────────────────────────────

    data class ShareState(
        val day: Int, 
        val todayClaimed: Boolean, 
        val reward: Int, 
        val hasProtection: Boolean,
        val protectionWillAutoAdvance: Boolean,
        val week: Int
    )

    fun getShareState(): ShareState {
        val today = now()
        val lastDate = prefs.getString(KEY_SHARE_DATE, "") ?: ""
        val week = prefs.getInt(KEY_SHARE_WEEK, 1).coerceIn(1, 4)
        val protection = prefs.getInt(KEY_SHARE_PROTECTION, 0).coerceAtLeast(0)
        val rawDay = prefs.getInt(KEY_SHARE_DAY, 1).coerceIn(0, 7)

        if (lastDate == today) {
            return ShareState(
                day = if (rawDay == 0) 7 else rawDay,
                todayClaimed = true,
                reward = if (rawDay == 0) 7 else rawDay,
                hasProtection = protection > 0,
                protectionWillAutoAdvance = false,
                week = week
            )
        }

        if (lastDate.isEmpty()) {
            return ShareState(1, false, 1, protection > 0, false, 1)
        }

        val yesterday = yesterdayStr(today)
        if (lastDate == yesterday) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return ShareState(nextDay, false, nextDay, protection > 0, false, nextWeek)
        }

        if (protection > 0) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return ShareState(nextDay, false, nextDay, true, true, nextWeek)
        }

        return ShareState(1, false, 1, false, false, 1)
    }

    fun claimShare(): Int {
        val s = getShareState()
        if (s.todayClaimed) return 0
        synchronized(this) {
            if (prefs.getString(KEY_SHARE_DATE, "") == now()) return 0
            val editor = prefs.edit()
            if (s.protectionWillAutoAdvance) {
                val p = (prefs.getInt(KEY_SHARE_PROTECTION, 0) - 1).coerceAtLeast(0)
                editor.putInt(KEY_SHARE_PROTECTION, p)
            }
            editor.putInt(KEY_SHARE_DAY, s.day)
            editor.putInt(KEY_SHARE_WEEK, s.week)
            editor.putString(KEY_SHARE_DATE, now())
            editor.putBoolean("share_synced", false)
            editor.commit()  // synchronous — prevents race with async reads
            return s.reward
        }
    }

    fun claimShareDay7BonusIfEligible(): Int = synchronized(this) {
        val day = prefs.getInt(KEY_SHARE_DAY, 1)
        val last = prefs.getString(KEY_SHARE_DATE, "") ?: ""
        val today = now()
        if (day == 7 && last == today) {
            prefs.edit().apply {
                putInt(KEY_SHARE_DAY, 0)
                putInt(KEY_SHARE_PROTECTION, prefs.getInt(KEY_SHARE_PROTECTION, 0) + 1)
            }.commit()  // synchronous — Day 7 bonus is important
            return 7
        }
        return 0
    }

    fun getShareWeeklyState(): WeeklyState {
        val week = prefs.getInt(KEY_SHARE_WEEK, 1).coerceIn(1, 4)
        val claimedWeek = prefs.getString(KEY_SHARE_WEEK_CLAIMED, "") ?: ""
        val today = now()
        val claimed = claimedWeek == today
        return WeeklyState(week, claimed, if (week == 4) 20 else 10)
    }

    fun claimShareWeekly(): Int {
        val s = getShareWeeklyState()
        if (s.claimed) return 0
        synchronized(this) {
            val today = now()
            if (prefs.getString(KEY_SHARE_WEEK_CLAIMED, "") == today) return 0
            prefs.edit().putString(KEY_SHARE_WEEK_CLAIMED, today).commit()  // synchronous — weekly claim must persist immediately
            return s.reward
        }
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun now() = LocalDate.now().toString()
    
    private fun yesterdayStr(date: String) = try { 
        LocalDate.parse(date).minusDays(1).toString() 
    } catch (_: Exception) { 
        "" 
    }
}
