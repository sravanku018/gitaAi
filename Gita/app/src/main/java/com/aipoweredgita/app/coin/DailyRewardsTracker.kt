package com.aipoweredgita.app.coin

import android.content.Context
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RewardState
import com.aipoweredgita.app.database.RewardStateDao
import java.time.LocalDate

/**
 * Daily check-in, weekly bonus, and share reward tracking.
 * All state persisted in Room Database.
 *
 * Thread-safe: all write methods synchronize.
 * Corrupt dates: silently default rather than crash.
 */
class DailyRewardsTracker(private val dao: RewardStateDao) {

    companion object {
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

    private fun getState(): RewardState = dao.getRewardStateSync() ?: RewardState()
    
    private fun saveState(state: RewardState) = dao.insertOrUpdate(state)

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

    fun syncWithServer(serverDay: Int, serverWeek: Int, lastCheckin: String? = null) {
        if (serverDay !in 1..7) return
        synchronized(this) {
            val state = getState()
            var newDate = state.lastCheckinDate
            val cleanDate = lastCheckin?.take(10)
            if (cleanDate != null && (newDate.isEmpty() || cleanDate > newDate)) {
                newDate = cleanDate
            }

            saveState(state.copy(
                checkinDay = serverDay,
                checkinWeek = serverWeek,
                lastCheckinDate = newDate
            ))
        }
    }

    fun syncShareWithServer(serverDay: Int, serverWeek: Int, lastShare: String? = null) {
        if (serverDay !in 1..7) return
        synchronized(this) {
            val state = getState()
            var newDate = state.lastShareDate
            val cleanDate = lastShare?.take(10)
            if (cleanDate != null && (newDate.isEmpty() || cleanDate > newDate)) {
                newDate = cleanDate
            }

            saveState(state.copy(
                shareDay = serverDay,
                shareWeek = serverWeek,
                lastShareDate = newDate
            ))
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

        if (rawDay == 7 && lastDate == today && week > 1) {
            rawDay = 0
            week = if (week == 1) 4 else week - 1
            newState = newState.copy(checkinDay = 0, checkinWeek = week, lastCheckinDate = yesterday)
            lastDate = yesterday
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

        if (rawDay == 7 && lastDate == today && week > 1) {
            rawDay = 0
            week = if (week == 1) 4 else week - 1
            newState = newState.copy(shareDay = 0, shareWeek = week, lastShareDate = yesterday)
            lastDate = yesterday
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
            if (ds.todayClaimed) {
                if (state != newState) saveState(newState)
                return 0
            }
            val today = now()
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
