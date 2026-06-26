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
            userStatsDao.insertIfEmpty(UserStats(userId = userId))
            statsRepository.refreshUserState(userId)
        }
    }

    fun handleGuestLogin() {
        viewModelScope.launch {
            val guestId = "guest_${java.util.UUID.randomUUID()}"
            AuthPreferences.getInstance(appContext).saveGuestState(guestId)
            userStatsDao.updateUserId(guestId)
            userStatsDao.updateProfile(name = "Guest User", dob = "")
            if (!AuthPreferences.getInstance(appContext).guestWelcomeAwarded) {
                userStatsDao.updateKrishnaCoins(50)
                AuthPreferences.getInstance(appContext).guestWelcomeAwarded = true
                CoinTransactionLogger.log(appContext, 50, "Welcome bonus (guest)")
            }
        }
    }
}
