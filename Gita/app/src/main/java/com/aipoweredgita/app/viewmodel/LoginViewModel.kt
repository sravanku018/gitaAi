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
        android.util.Log.d("LoginViewModel", "=== handleLoginSuccess START === userId=$userId")
        viewModelScope.launch {
            // Do NOT resetForAccountSwitch here — AuthManager already resets only on
            // account switch and force-applies server check-in. Resetting again wiped
            // the strip back to "unclicked" before/while the second refresh ran.
            userStatsDao.updateUserId(userId)
            val existing = userStatsDao.getUserStatsOnce()
            if (existing == null) {
                userStatsDao.insertStats(UserStats(id = 1, userId = userId))
            }
            android.util.Log.d("LoginViewModel", "Calling refreshUserState(force) for userId=$userId")
            statsRepository.refreshUserState(userId, force = true)
            android.util.Log.d(
                "LoginViewModel",
                "=== handleLoginSuccess DONE revision=${com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(appContext).revision} ==="
            )
        }
    }

    /**
     * Guest login is handled by the AuthManager which creates the guest on the VPS server
     * and correctly returns success or failure. LoginScreen calls authManager.createGuest() directly.
     */
}
