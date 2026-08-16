package com.aipoweredgita.app.coin

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RewardState
import com.aipoweredgita.app.database.RewardStateDao
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

/**
 * Daily check-in, weekly bonus, and share reward tracking.
 * All state persisted in Room Database.
 *
 * Thread-safe: all write methods synchronize.
 * Corrupt dates: silently default rather than crash.
 */
class DailyRewardsTracker(private val dao: RewardStateDao) {

    companion object {
        private const val TAG = "DailyRewardsTracker"

        @Volatile
        private var instance: DailyRewardsTracker? = null

        fun getInstance(context: Context): DailyRewardsTracker {
            return instance ?: synchronized(this) {
                instance ?: DailyRewardsTracker(
                    GitaDatabase.getDatabase(context).rewardStateDao()
                ).also { instance = it }
            }
        }
    }

    /** Bumped on every save so Compose can re-read after async server sync. */
    private val revisionCounter = AtomicInteger(0)
    val revision: Int get() = revisionCounter.get()

    private fun getState(): RewardState = dao.getRewardStateSync() ?: RewardState()

    private fun saveState(state: RewardState) {
        dao.insertOrUpdate(state)
        revisionCounter.incrementAndGet()
    }

    var isCheckinSynced: Boolean
        get() {
            val state = getState()
            val today = now()
            if (state.lastCheckinDate != today) return false
            return state.isCheckinSynced
        }
        set(value) {
            synchronized(this) {
                val state = getState()
                saveState(state.copy(isCheckinSynced = value))
            }
        }

    var isShareSynced: Boolean
        get() {
            val state = getState()
            val today = now()
            if (state.lastShareDate != today) return false
            return state.isShareSynced
        }
        set(value) {
            synchronized(this) {
                val state = getState()
                saveState(state.copy(isShareSynced = value))
            }
        }

    /**
     * Map server check-in (day 1..7, week 1..4) to local storage.
     * Backend stores day=7 and advances week to the *next* week.
     * Local uses day=0 for "week complete" and keeps the *completed* week number.
     * e.g. server (7, 2) → local (0, 1); server (7, 1) → local (0, 4).
     */
    private fun mapServerDayWeek(serverDay: Int, serverWeek: Int): Pair<Int, Int> {
        val d = serverDay.coerceIn(1, 7)
        val w = serverWeek.coerceIn(1, 4)
        return if (d == 7) {
            val completedWeek = if (w == 1) 4 else w - 1
            0 to completedWeek
        } else {
            d to w
        }
    }

    /** Higher = further along the 4-week cycle. day 0 (complete) ranks above day 7. */
    private fun progressScore(day: Int, week: Int): Int {
        val w = week.coerceIn(1, 4)
        val dayScore = when {
            day == 0 -> 8
            day in 1..7 -> day
            else -> 0
        }
        return w * 10 + dayScore
    }

    /**
     * Wipe local check-in/share UI state when switching accounts (login / register / logout).
     * Prevents guest progress from hiding the next user's clickable day-1 streak.
     */
    fun resetForAccountSwitch() {
        synchronized(this) {
            saveState(RewardState())
        }
    }

    /**
     * Apply server check-in to local strip.
     * @param force when true (after login), server wins even if local guest progress looked "ahead".
     * serverDay 0 = no row / never checked in → day 1, not claimed (clickable).
     */
    fun syncWithServer(
        serverDay: Int,
        serverWeek: Int,
        lastCheckin: String? = null,
        force: Boolean = false
    ) {
        synchronized(this) {
            val state = getState()
            // 0 from COALESCE(missing row) or register seed → treat as fresh day 1
            val effectiveServerDay = if (serverDay <= 0) 1 else serverDay.coerceIn(1, 7)
            val (mappedDay, mappedWeek) = mapServerDayWeek(effectiveServerDay, serverWeek.coerceIn(1, 4))

            // Accept ISO "2026-08-14" or "2026-08-14T12:00:00.000Z"
            val cleanDate = lastCheckin?.trim()?.let { raw ->
                val d = raw.take(10)
                if (d.length == 10 && d[4] == '-' && d[7] == '-') d else null
            }

            val today = now()
            // FIX: If we already claimed locally today, don't let a stale server wipe it
            val localClaimedToday = state.lastCheckinDate == today

            var newDate = if (force) (cleanDate ?: "") else state.lastCheckinDate
            if (cleanDate != null && (force || newDate.isEmpty() || cleanDate > newDate)) {
                newDate = cleanDate
            }

            // Preserve local claim if server is behind
            if (localClaimedToday && (cleanDate == null || cleanDate < today)) {
                newDate = today
            }

            // No last_checkin on server + day 0 → ensure empty so UI shows day 1 claimable
            if (serverDay <= 0 && cleanDate == null && !localClaimedToday) {
                newDate = ""
            }

            val localScore = progressScore(state.checkinDay, state.checkinWeek)
            val serverScore = progressScore(mappedDay, mappedWeek)

            val (finalDay, finalWeek) = when {
                localClaimedToday -> state.checkinDay to state.checkinWeek
                force -> mappedDay to mappedWeek
                // Never downgrade local progress mid-session; only adopt server when ahead
                serverScore > localScore -> mappedDay to mappedWeek
                else -> state.checkinDay to state.checkinWeek
            }

            // If server has progress (day/week) but no date, assume claimed today when force-syncing
            // only when last_checkin is today OR date missing but day indicates an active streak row.
            val resolvedDate = when {
                !newDate.isNullOrEmpty() -> newDate
                force && serverDay > 0 && cleanDate == null && !localClaimedToday -> {
                    ""
                }
                else -> newDate
            }
            val claimedToday = resolvedDate.isNotEmpty() && resolvedDate == today

            Log.d(
                TAG,
                "syncWithServer force=$force serverDay=$serverDay serverWeek=$serverWeek " +
                    "cleanDate=$cleanDate finalDay=$finalDay finalWeek=$finalWeek " +
                    "resolvedDate=$resolvedDate claimedToday=$claimedToday today=$today"
            )

            saveState(
                state.copy(
                    checkinDay = finalDay,
                    checkinWeek = finalWeek.coerceIn(1, 4),
                    lastCheckinDate = resolvedDate,
                    isCheckinSynced = claimedToday || (force && resolvedDate.isNotEmpty())
                )
            )
        }
    }

    fun syncShareWithServer(
        serverDay: Int,
        serverWeek: Int,
        lastShare: String? = null,
        force: Boolean = false
    ) {
        synchronized(this) {
            val state = getState()
            val effectiveServerDay = if (serverDay <= 0) 1 else serverDay.coerceIn(1, 7)
            val (mappedDay, mappedWeek) = mapServerDayWeek(effectiveServerDay, serverWeek.coerceIn(1, 4))

            val cleanDate = lastShare?.trim()?.let { raw ->
                val d = raw.take(10)
                if (d.length == 10 && d[4] == '-' && d[7] == '-') d else null
            }

            val today = now()
            // FIX: If we already claimed locally today, don't let a stale server wipe it
            val localClaimedToday = state.lastShareDate == today

            var newDate = if (force) (cleanDate ?: "") else state.lastShareDate
            if (cleanDate != null && (force || newDate.isEmpty() || cleanDate > newDate)) {
                newDate = cleanDate
            }
            
            // Preserve local claim if server is behind
            if (localClaimedToday && (cleanDate == null || cleanDate < today)) {
                newDate = today
            }
            
            if (serverDay <= 0 && cleanDate == null && !localClaimedToday) {
                newDate = ""
            }

            val localScore = progressScore(state.shareDay, state.shareWeek)
            val serverScore = progressScore(mappedDay, mappedWeek)

            val (finalDay, finalWeek) = when {
                localClaimedToday -> state.shareDay to state.shareWeek
                force -> mappedDay to mappedWeek
                serverScore > localScore -> mappedDay to mappedWeek
                else -> state.shareDay to state.shareWeek
            }

            val claimedToday = !newDate.isNullOrEmpty() && newDate == today

            saveState(
                state.copy(
                    shareDay = finalDay,
                    shareWeek = finalWeek.coerceIn(1, 4),
                    lastShareDate = newDate,
                    isShareSynced = claimedToday || (force && !cleanDate.isNullOrEmpty())
                )
            )
        }
    }

    data class DailyState(
        val day: Int, 
        val todayClaimed: Boolean, 
        val reward: Int,
        val hasProtection: Boolean, 
        val protectionWillAutoAdvance: Boolean, 
        val week: Int
    )

    // Using a separate method for logic without hitting DB repeatedly
    private fun getDailyStateInternal(st: RewardState): Pair<DailyState, RewardState> {
        val today = now()
        val yesterday = yesterdayStr(today)
        var lastDate = st.lastCheckinDate
        var week = st.checkinWeek.coerceIn(1, 4)
        val protection = if (st.checkinProtectionUsed) 0 else 1 // Simplified: bool to int logic
        var rawDay = st.checkinDay.coerceIn(0, 7)
        var newState = st

        // FIX: Only wrap the week if day 7 was claimed on a PREVIOUS day
        if (rawDay == 7 && lastDate != today && lastDate.isNotEmpty()) {
            rawDay = 0
            week = if (week == 1) 4 else week - 1
            newState = newState.copy(checkinDay = 0, checkinWeek = week, lastCheckinDate = lastDate)
        }

        if (lastDate == today) {
            return DailyState(
                day = if (rawDay == 0) 7 else rawDay,
                todayClaimed = true,
                reward = if (rawDay == 0) 7 else rawDay,
                hasProtection = !newState.checkinProtectionUsed,
                protectionWillAutoAdvance = false,
                week = week
            ) to newState
        }

        if (lastDate.isEmpty()) {
            return DailyState(1, false, 1, !newState.checkinProtectionUsed, false, 1) to newState
        }

        if (lastDate == yesterday) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return DailyState(nextDay, false, nextDay, !newState.checkinProtectionUsed, false, nextWeek) to newState
        }

        if (!newState.checkinProtectionUsed && newState.checkinProtectionGranted) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return DailyState(nextDay, false, nextDay, true, true, nextWeek) to newState
        }

        newState = newState.copy(
            checkinDay = 1,
            checkinWeek = 1,
            checkinProtectionUsed = false,
            checkinProtectionGranted = false
        )
        return DailyState(1, false, 1, false, false, 1) to newState
    }

    fun getDailyState(): DailyState {
        synchronized(this) {
            val state = getState()
            val (ds, newState) = getDailyStateInternal(state)
            if (state != newState) saveState(newState)
            return ds
        }
    }

    fun claimDaily(): Int {
        synchronized(this) {
            val state = getState()
            val (ds, newState) = getDailyStateInternal(state)
            if (ds.todayClaimed) {
                if (state != newState) saveState(newState)
                return 0
            }
            val today = now()
            if (newState.lastCheckinDate == today) {
                if (state != newState) saveState(newState)
                return 0
            }
            
            var updatedState = newState
            if (ds.protectionWillAutoAdvance) {
                updatedState = updatedState.copy(checkinProtectionUsed = true)
            }
            updatedState = updatedState.copy(
                checkinDay = ds.day,
                checkinWeek = ds.week,
                lastCheckinDate = today,
                isCheckinSynced = false
            )
            saveState(updatedState)
            return ds.reward
        }
    }

    fun claimDay7BonusIfEligible(): Int = synchronized(this) {
        var state = getState()
        val day = state.checkinDay
        val last = state.lastCheckinDate
        val today = now()
        
        if (day == 7 && last == today) {
            val calWeek = currentCalendarWeek()
            state = state.copy(
                checkinDay = 0,
                checkinCompletedWeek = calWeek,
                checkinProtectionGranted = false
            )
            state = grantBothProtectionsIfNeeded(calWeek, state)
            saveState(state)
            return 7
        }
        return 0
    }

    fun getCurrentCheckinDay(): Int = getState().checkinDay.coerceIn(0, 7)

    data class WeeklyState(val week: Int, val reward: Int)

    fun getWeeklyState(): WeeklyState {
        val week = getState().checkinWeek.coerceIn(1, 4)
        return WeeklyState(week, if (week == 4) 20 else 10)
    }

    data class ShareState(
        val day: Int, 
        val todayClaimed: Boolean, 
        val reward: Int, 
        val hasProtection: Boolean,
        val protectionWillAutoAdvance: Boolean,
        val week: Int
    )

    private fun getShareStateInternal(st: RewardState): Pair<ShareState, RewardState> {
        val today = now()
        val yesterday = yesterdayStr(today)
        var lastDate = st.lastShareDate
        var week = st.shareWeek.coerceIn(1, 4)
        var rawDay = st.shareDay.coerceIn(0, 7)
        var newState = st

        // FIX: Only wrap the week if day 7 was claimed on a PREVIOUS day
        if (rawDay == 7 && lastDate != today && lastDate.isNotEmpty()) {
            rawDay = 0
            week = if (week == 1) 4 else week - 1
            newState = newState.copy(shareDay = 0, shareWeek = week, lastShareDate = lastDate)
        }

        if (lastDate == today) {
            return ShareState(
                day = if (rawDay == 0) 7 else rawDay,
                todayClaimed = true,
                reward = if (rawDay == 0) 7 else rawDay,
                hasProtection = !newState.shareProtectionUsed,
                protectionWillAutoAdvance = false,
                week = week
            ) to newState
        }

        if (lastDate.isEmpty()) {
            return ShareState(1, false, 1, !newState.shareProtectionUsed, false, 1) to newState
        }

        if (lastDate == yesterday) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return ShareState(nextDay, false, nextDay, !newState.shareProtectionUsed, false, nextWeek) to newState
        }

        if (!newState.shareProtectionUsed && newState.shareProtectionGranted) {
            val nextDay = if (rawDay == 0 || rawDay == 7) 1 else rawDay + 1
            val nextWeek = if (rawDay == 0 || rawDay == 7) {
                if (week >= 4) 1 else week + 1
            } else week
            return ShareState(nextDay, false, nextDay, true, true, nextWeek) to newState
        }

        newState = newState.copy(
            shareDay = 1,
            shareWeek = 1,
            shareProtectionUsed = false,
            shareProtectionGranted = false
        )
        return ShareState(1, false, 1, false, false, 1) to newState
    }

    fun getShareState(): ShareState {
        synchronized(this) {
            val state = getState()
            val (ds, newState) = getShareStateInternal(state)
            if (state != newState) saveState(newState)
            return ds
        }
    }

    fun claimShare(): Int {
        synchronized(this) {
            val state = getState()
            val (ds, newState) = getShareStateInternal(state)
            val today = now()

            if (ds.todayClaimed) {
                if (state != newState) saveState(newState)
                return 0
            }
            if (newState.lastShareDate == today) {
                if (state != newState) saveState(newState)
                return 0
            }
            
            var updatedState = newState
            if (ds.protectionWillAutoAdvance) {
                updatedState = updatedState.copy(shareProtectionUsed = true)
            }
            updatedState = updatedState.copy(
                shareDay = ds.day,
                shareWeek = ds.week,
                lastShareDate = today,
                isShareSynced = false
            )
            saveState(updatedState)
            return ds.reward
        }
    }

    fun claimShareDay7BonusIfEligible(): Int = synchronized(this) {
        var state = getState()
        val day = state.shareDay
        val last = state.lastShareDate
        val today = now()
        
        if (day == 7 && last == today) {
            val calWeek = currentCalendarWeek()
            state = state.copy(
                shareDay = 0,
                shareCompletedWeek = calWeek,
                shareProtectionGranted = false
            )
            state = grantBothProtectionsIfNeeded(calWeek, state)
            saveState(state)
            return 7
        }
        return 0
    }

    fun getShareWeeklyState(): WeeklyState {
        val week = getState().shareWeek.coerceIn(1, 4)
        return WeeklyState(week, if (week == 4) 20 else 10)
    }

    fun isFreshInstall(): Boolean {
        val state = getState()
        return state.lastCheckinDate.isEmpty() && state.lastShareDate.isEmpty()
    }

    private fun now() = LocalDate.now(java.time.ZoneId.systemDefault()).toString()
    
    fun nowLocal() = LocalDate.now(java.time.ZoneId.systemDefault()).toString()

    private fun yesterdayStr(date: String) = try {
        LocalDate.parse(date).minusDays(1).toString()
    } catch (_: Exception) {
        ""
    }

    private fun currentCalendarWeek(): String {
        val date = LocalDate.now(java.time.ZoneId.systemDefault())
        val week = date.get(java.time.temporal.IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = date.get(java.time.temporal.IsoFields.WEEK_BASED_YEAR)
        return "${year}-W${week}"
    }

    private fun grantBothProtectionsIfNeeded(calWeek: String, currentState: RewardState): RewardState {
        var newState = currentState
        if (newState.checkinCompletedWeek == calWeek && newState.shareCompletedWeek == calWeek) {
            if (!newState.checkinProtectionGranted) {
                newState = newState.copy(
                    checkinProtectionGranted = true,
                    checkinProtectionUsed = false // grant 1 protection
                )
            }
            if (!newState.shareProtectionGranted) {
                newState = newState.copy(
                    shareProtectionGranted = true,
                    shareProtectionUsed = false
                )
            }
        }
        return newState
    }

    fun restoreCheckinAndShareStreaks(targetDay: Int = 7) = synchronized(this) {
        val state = getState()
        val today = now()
        val yesterday = yesterdayStr(today)
        val restoredDay = targetDay.coerceIn(1, 7)
        val updatedState = state.copy(
            checkinDay = restoredDay,
            lastCheckinDate = yesterday,
            isCheckinSynced = false,
            shareDay = restoredDay,
            lastShareDate = yesterday,
            isShareSynced = false
        )
        saveState(updatedState)
    }
}
