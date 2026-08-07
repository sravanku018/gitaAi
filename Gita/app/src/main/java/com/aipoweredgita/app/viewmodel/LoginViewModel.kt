package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.utils.AuthPreferences
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val userStatsDao: UserStatsDao,
    private val dailyActivityDao: DailyActivityDao,
    private val statsRepository: StatsRepository,
    @ApplicationContext private val appContext: android.content.Context
) : ViewModel() {

    fun handleLoginSuccess(userId: String) {
        viewModelScope.launch {
            userStatsDao.updateUserId(userId)
            val existing = userStatsDao.getUserStatsOnce()
            if (existing == null) {
                userStatsDao.insertStats(UserStats(id = 1, userId = userId))
            }
            statsRepository.refreshUserState(userId, force = true)
        }
    }

    /**
     * Guest login must finish prefs + local coin history BEFORE navigation.
     * LoginScreen calls this then immediately navigates (does not await coroutines).
     */
    fun handleGuestLogin() {
        val guestId = "guest_${java.util.UUID.randomUUID()}"
        val authPrefs = AuthPreferences.getInstance(appContext)

        // Drop prior account / prior guest history so lists never mix
        authPrefs.userId?.let { prev ->
            com.aipoweredgita.app.coin.CoinTransactionLogger.clear(appContext, prev)
        }
        authPrefs.guestId?.let { prevGuest ->
            if (prevGuest != authPrefs.userId) {
                com.aipoweredgita.app.coin.CoinTransactionLogger.clear(appContext, prevGuest)
            }
        }

        authPrefs.saveGuestState(guestId)
        // Fresh empty bucket for this guest id
        com.aipoweredgita.app.coin.CoinTransactionLogger.clear(appContext, guestId)

        // Always seed welcome for this session (do not gate on guestWelcomeAwarded —
        // that flag survived re-entry and left history empty after clear).
        authPrefs.guestWelcomeAwarded = true
        com.aipoweredgita.app.coin.CoinTransactionLogger.log(
            appContext,
            50,
            "Welcome bonus (guest)",
            source = "signup",
            userId = guestId
        )

        // Room balance / profile can be async; history is already on disk under guestId
        viewModelScope.launch {
            try {
                val existing = userStatsDao.getUserStatsOnce()
                if (existing == null) {
                    userStatsDao.insertStats(
                        UserStats(id = 1, userId = guestId, krishnaCoins = 50, serverUpdatedAt = "")
                    )
                } else {
                    userStatsDao.updateUserId(guestId)
                    userStatsDao.updateProfile(name = "Guest User", dob = "")
                    userStatsDao.updateKrishnaCoins(50)
                }
            } catch (_: Exception) {
                userStatsDao.updateUserId(guestId)
                userStatsDao.updateProfile(name = "Guest User", dob = "")
                userStatsDao.updateKrishnaCoins(50)
            }
        }
    }
}
