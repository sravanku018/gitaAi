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

        // Daily share keys
        private const val KEY_SHARE_DAY = "share_reward_day"
        private const val KEY_SHARE_DATE = "share_reward_last_date"
        private const val KEY_SHARE_PROTECTION = "share_reward_protection"
        private const val KEY_SHARE_WEEK = "share_reward_week"

        // Same-week completion tracking (ISO week-based year + week number)
        private const val KEY_CHECKIN_COMPLETED_WEEK = "checkin_completed_week"
        private const val KEY_SHARE_COMPLETED_WEEK = "share_completed_week"
        // Whether protection was already granted for a completed week
        private const val KEY_CHECKIN_PROT_GRANTED = "checkin_protection_granted"
        private const val KEY_SHARE_PROT_GRANTED = "share_protection_granted"

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

    fun syncWithServer(serverDay: Int, serverWeek: Int, lastCheckin: String? = null) {
        if (serverDay in 1..7) {
            synchronized(this) {
                val currentDay = prefs.getInt(KEY_DAY, 1)
                val currentWeek = prefs.getInt(KEY_WEEK, 1)
                // Don't overwrite higher local day with lower server day in same week
                val safeDay = if (serverWeek >= currentWeek) maxOf(serverDay, currentDay) else serverDay
                prefs.edit().apply {
                    putInt(KEY_DAY, safeDay)
                    if (serverWeek > 0) putInt(KEY_WEEK, serverWeek)
                    // Only overwrite local date if server date is more recent
                    if (lastCheckin != null) {
                        val currentDate = prefs.getString(KEY_DATE, "") ?: ""
                        if (currentDate.isEmpty() || lastCheckin > currentDate) {
                            putString(KEY_DATE, lastCheckin)
                        }
                    }
                }.commit()
            }
        }
    }

    fun syncShareWithServer(serverDay: Int, serverWeek: Int, lastShare: String? = null) {
        if (serverDay in 1..7) {
            synchronized(this) {
                val currentDay = prefs.getInt(KEY_SHARE_DAY, 1)
                val currentWeek = prefs.getInt(KEY_SHARE_WEEK, 1)
                // Don't overwrite higher local day with lower server day in same week
                val safeDay = if (serverWeek >= currentWeek) maxOf(serverDay, currentDay) else serverDay
                prefs.edit().apply {
                    putInt(KEY_SHARE_DAY, safeDay)
                    if (serverWeek > 0) putInt(KEY_SHARE_WEEK, serverWeek)
                    // Only overwrite local date if server date is more recent
                    if (lastShare != null) {
                        val currentDate = prefs.getString(KEY_SHARE_DATE, "") ?: ""
                        if (currentDate.isEmpty() || lastShare > currentDate) {
                            putString(KEY_SHARE_DATE, lastShare)
                        }
                    }
                }.commit()
            }
        }
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

        // Reset to Week 1, Day 1 — persist so stale KEY_WEEK doesn't leak into next claim
        prefs.edit().apply {
            putInt(KEY_DAY, 1)
            putInt(KEY_WEEK, 1)
            putInt(KEY_PROTECTION, 0)
        }.commit()
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
            // Mark check-in week as completed
            val calWeek = currentCalendarWeek()
            prefs.edit().apply {
                putInt(KEY_DAY, 0)
                putString(KEY_CHECKIN_COMPLETED_WEEK, calWeek)
                putBoolean(KEY_CHECKIN_PROT_GRANTED, false)
            }.commit()
            // If share also completed Day 7 this same week, grant protection to both
            grantBothProtectionsIfNeeded(calWeek)
            return 7
        }
        return 0
    }

    fun getCurrentCheckinDay(): Int = prefs.getInt(KEY_DAY, 1).coerceIn(1, 7)

    // ── Weekly Bonus (auto-claimed with Day 7) ──────────────────────────────

    data class WeeklyState(val week: Int, val reward: Int)

    fun getWeeklyState(): WeeklyState {
        val week = prefs.getInt(KEY_WEEK, 1).coerceIn(1, 4)
        return WeeklyState(week, if (week == 4) 20 else 10)
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

        // Reset to Week 1, Day 1 — persist so stale KEY_SHARE_WEEK doesn't leak into next claim
        prefs.edit().apply {
            putInt(KEY_SHARE_DAY, 1)
            putInt(KEY_SHARE_WEEK, 1)
            putInt(KEY_SHARE_PROTECTION, 0)
        }.commit()
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
            // Mark share week as completed
            val calWeek = currentCalendarWeek()
            prefs.edit().apply {
                putInt(KEY_SHARE_DAY, 0)
                putString(KEY_SHARE_COMPLETED_WEEK, calWeek)
                putBoolean(KEY_SHARE_PROT_GRANTED, false)
            }.commit()
            // If check-in also completed Day 7 this same week, grant protection to both
            grantBothProtectionsIfNeeded(calWeek)
            return 7
        }
        return 0
    }

    fun getShareWeeklyState(): WeeklyState {
        val week = prefs.getInt(KEY_SHARE_WEEK, 1).coerceIn(1, 4)
        return WeeklyState(week, if (week == 4) 20 else 10)
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun now() = LocalDate.now(java.time.ZoneId.systemDefault()).toString()
    
    fun nowLocal() = LocalDate.now(java.time.ZoneId.systemDefault()).toString()

    private fun yesterdayStr(date: String) = try {
        LocalDate.parse(date).minusDays(1).toString()
    } catch (_: Exception) {
        ""
    }

    /** ISO week-based year + week number, e.g. "2025-W23" */
    private fun currentCalendarWeek(): String {
        val date = LocalDate.now(java.time.ZoneId.systemDefault())
        val week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
        return "${year}-W${week}"
    }

    /**
     * Called after either activity completes Day 7.
     * If BOTH check-in and share completed Day 7 in the same calendar week,
     * grant 1 protection token to each (once per week).
     */
    private fun grantBothProtectionsIfNeeded(calWeek: String) {
        val checkinWeek = prefs.getString(KEY_CHECKIN_COMPLETED_WEEK, "") ?: ""
        val shareWeek = prefs.getString(KEY_SHARE_COMPLETED_WEEK, "") ?: ""
        if (checkinWeek == calWeek && shareWeek == calWeek) {
            val editor = prefs.edit()
            if (!prefs.getBoolean(KEY_CHECKIN_PROT_GRANTED, false)) {
                editor.putInt(KEY_PROTECTION, prefs.getInt(KEY_PROTECTION, 0) + 1)
                editor.putBoolean(KEY_CHECKIN_PROT_GRANTED, true)
            }
            if (!prefs.getBoolean(KEY_SHARE_PROT_GRANTED, false)) {
                editor.putInt(KEY_SHARE_PROTECTION, prefs.getInt(KEY_SHARE_PROTECTION, 0) + 1)
                editor.putBoolean(KEY_SHARE_PROT_GRANTED, true)
            }
            editor.commit()
        }
    }
}
