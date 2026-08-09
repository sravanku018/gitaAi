package com.aipoweredgita.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.DailyActivityDao
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.network.CoinApi
import com.aipoweredgita.app.network.CreateGuestRequest
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
            // Drop guest streak UI so signed-in (incl. old) accounts show claimable day again
            com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(appContext).resetForAccountSwitch()
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
        // Wipe stable guest bucket + any orphan tx_guest_* keys, then re-seed
        com.aipoweredgita.app.coin.CoinTransactionLogger.clear(appContext, guestId)

        // Always seed welcome into stable GUEST_SESSION store (UUID no longer matters)
        authPrefs.guestWelcomeAwarded = true
        com.aipoweredgita.app.coin.CoinTransactionLogger.log(
            appContext,
            50,
            "Welcome bonus (guest)",
            source = "signup",
            userId = guestId
        )
        // Verify write landed (defensive)
        com.aipoweredgita.app.coin.CoinTransactionLogger.ensureGuestWelcome(
            appContext, amount = 50, userId = guestId
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

        // Register this guest on the server (best-effort) so guest accounts appear on the
        // dashboard. The SAME guest_id is sent so the local session and the server row stay
        // identical — no id migration needed. If offline, the guest keeps working locally.
        viewModelScope.launch {
            try {
                val res = CoinApi.retrofitService.createGuest(CreateGuestRequest(guestId))
                // Only adopt the token if this is still the active guest session (the user may
                // have started a new guest session while the request was in flight).
                if (res.guest_id.isNotEmpty() && authPrefs.guestId == guestId) {
                    if (res.token.isNotEmpty()) {
                        authPrefs.token = res.token
                        authPrefs.guestToken = res.token
                    }
                    Log.d("LoginViewModel", "Guest registered on server: ${res.guest_id}")
                }
            } catch (e: Exception) {
                Log.w("LoginViewModel", "Guest server registration skipped (offline?): ${e.message}")
            }
        }
    }
}
