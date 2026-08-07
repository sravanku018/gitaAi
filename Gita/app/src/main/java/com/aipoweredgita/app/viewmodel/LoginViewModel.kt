package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.coin.CoinTransactionLogger
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

    fun handleGuestLogin() {
        viewModelScope.launch {
            val guestId = "guest_${java.util.UUID.randomUUID()}"
            val authPrefs = AuthPreferences.getInstance(appContext)
            // Clear prior signed-in history so guest never sees another account's txs
            authPrefs.userId?.let { prev ->
                com.aipoweredgita.app.coin.CoinTransactionLogger.clear(appContext, prev)
            }
            authPrefs.saveGuestState(guestId)
            com.aipoweredgita.app.coin.CoinTransactionLogger.clear(appContext, guestId)
            userStatsDao.updateUserId(guestId)
            userStatsDao.updateProfile(name = "Guest User", dob = "")
            // Fresh guest: welcome coins + one local history line under this guestId only
            if (!authPrefs.guestWelcomeAwarded) {
                userStatsDao.updateKrishnaCoins(50)
                authPrefs.guestWelcomeAwarded = true
                com.aipoweredgita.app.coin.CoinTransactionLogger.log(
                    appContext,
                    50,
                    "Welcome bonus (guest)",
                    source = "signup",
                    userId = guestId
                )
            }
        }
    }
}
