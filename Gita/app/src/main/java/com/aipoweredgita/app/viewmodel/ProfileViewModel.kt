package com.aipoweredgita.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.coin.DailyRewardsTracker
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.domain.model.NextActionData
import com.aipoweredgita.app.domain.model.ProfileEvent
import com.aipoweredgita.app.domain.model.ProfileSideEffect
import com.aipoweredgita.app.domain.model.ProfileUiState
import com.aipoweredgita.app.domain.usecase.GenerateBadgesUseCase
import com.aipoweredgita.app.domain.usecase.GetCoinBalanceUseCase
import com.aipoweredgita.app.domain.usecase.LoadDashboardUseCase
import com.aipoweredgita.app.domain.usecase.UpdateProfileUseCase
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.UserLevel
import com.aipoweredgita.app.repository.ContentRepository
import com.aipoweredgita.app.repository.StatsRepository
import com.aipoweredgita.app.utils.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Profile ViewModel with UDF pattern
 * Uses constructor injection via Hilt
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val statsRepository: StatsRepository,
    private val contentRepo: ContentRepository,
    private val getCoinBalanceUseCase: GetCoinBalanceUseCase,
    private val loadDashboardUseCase: LoadDashboardUseCase,
    private val generateBadgesUseCase: GenerateBadgesUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    // Backward-compatible type aliases for existing UI code
    data class DailyActivityData(
        val versesToday: Int = 0,
        val quizzesToday: Int = 0,
        val normalToday: Long = 0L,
        val quizToday: Long = 0L,
        val studioToday: Long = 0L,
        val versesListToday: List<com.aipoweredgita.app.database.ReadVerse> = emptyList()
    )

    data class NextActionData(
        val nextStep: String? = null,
        val nextLevel: Int = -1,
        val nextReason: String? = null
    )

    // Single UI state
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // One-time side effects
    private val _sideEffect = MutableSharedFlow<ProfileSideEffect>()
    val sideEffect: SharedFlow<ProfileSideEffect> = _sideEffect.asSharedFlow()

    // Backward-compatible separate state flows for existing UI code
    private val _stats = MutableStateFlow<com.aipoweredgita.app.database.UserStats?>(null)
    val stats: StateFlow<com.aipoweredgita.app.database.UserStats?> = _stats.asStateFlow()

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val _dailyActivity = MutableStateFlow(DailyActivityData())
    val dailyActivity: StateFlow<DailyActivityData> = _dailyActivity.asStateFlow()

    private val _nextAction = MutableStateFlow(NextActionData())
    val nextAction: StateFlow<NextActionData> = _nextAction.asStateFlow()

    private val _recommendations = MutableStateFlow<List<com.aipoweredgita.app.database.RecommendationData>>(emptyList())
    val recommendations: StateFlow<List<com.aipoweredgita.app.database.RecommendationData>> = _recommendations.asStateFlow()

    init {
        loadStats()
        loadRecommendations()
        observeCoinBalance()
    }

    /**
     * Handle events from the UI
     */
    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.LoadDashboard -> loadDashboard(null)
            is ProfileEvent.UpdateProfile -> updateProfile(event.name, event.dob)
            is ProfileEvent.RefreshCoins -> refreshCoinBalance()
            is ProfileEvent.SetCoinBalance -> setCoinBalance(event.balance)
        }
    }

    // Backward-compatible loadDashboardData method
    fun loadDashboardData(context: Context?) {
        loadDashboard(context)
    }

    /**
     * Load user stats and generate badges
     */
    private fun loadStats() {
        viewModelScope.launch {
            try {
                statsRepository // Access to trigger initialization
                _uiState.update { it.copy(isLoading = true) }
                
                // This would need to be injected or accessed differently
                // For now, keeping the existing pattern
                val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(
                    com.aipoweredgita.app.GitaApp.instance
                )
                db.userStatsDao().initializeStatsIfNeeded()
                db.userStatsDao().getUserStats().collect { userStats ->
                    _stats.value = userStats
                    _uiState.update { it.copy(stats = userStats, isLoading = false) }
                    if (userStats != null) {
                        generateAIBadgesAndLevel(userStats)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                _sideEffect.emit(ProfileSideEffect.ShowError(e.message ?: "Unknown error"))
            }
        }
    }

    /**
     * Generate AI badges and level from stats
     */
    private suspend fun generateAIBadgesAndLevel(stats: UserStats) {
        try {
            val result = generateBadgesUseCase(stats)
            result.onSuccess { badgeResult ->
                _uiState.update { 
                    it.copy(
                        badges = badgeResult.badges,
                        level = badgeResult.level
                    ) 
                }
            }
            result.onFailure { error ->
                _sideEffect.emit(ProfileSideEffect.ShowError(error.message ?: "Failed to generate badges"))
            }
        } catch (e: Exception) {
            _sideEffect.emit(ProfileSideEffect.ShowError(e.message ?: "Failed to generate badges"))
        }
    }

    /**
     * Load recommendations
     */
    private fun loadRecommendations() {
        viewModelScope.launch {
            try {
                contentRepo.getActiveRecommendations().collect { recs ->
                    _recommendations.value = recs
                    _uiState.update { it.copy(recommendations = recs) }
                }
            } catch (e: Exception) {
                // Non-critical, continue
            }
        }
    }

    /**
     * Observe coin balance changes
     */
    private fun observeCoinBalance() {
        viewModelScope.launch {
            statsRepository.coinBalance.collect { balance ->
                _coinBalance.value = balance
                _uiState.update { it.copy(coinBalance = balance) }
            }
        }
    }

    /**
     * Load dashboard data
     */
    fun loadDashboard(context: Context?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val ctx = context ?: com.aipoweredgita.app.GitaApp.instance
            val result = loadDashboardUseCase(ctx)
            
            result.onSuccess { dashboardResult ->
                val dailyActivityBc = DailyActivityData(
                    versesToday = dashboardResult.dailyActivity.versesToday,
                    quizzesToday = dashboardResult.dailyActivity.quizzesToday,
                    normalToday = dashboardResult.dailyActivity.normalToday,
                    quizToday = dashboardResult.dailyActivity.quizToday,
                    studioToday = dashboardResult.dailyActivity.studioToday,
                    versesListToday = dashboardResult.dailyActivity.versesListToday
                )
                val nextActionBc = NextActionData(
                    nextStep = dashboardResult.nextAction.nextStep,
                    nextLevel = dashboardResult.nextAction.nextLevel,
                    nextReason = dashboardResult.nextAction.nextReason
                )
                _dailyActivity.value = dailyActivityBc
                _nextAction.value = nextActionBc
                _uiState.update { 
                    it.copy(
                        dailyActivity = dashboardResult.dailyActivity,
                        nextAction = dashboardResult.nextAction,
                        isLoading = false
                    ) 
                }
            }
            
            result.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = error.message) }
                _sideEffect.emit(ProfileSideEffect.ShowError(error.message ?: "Failed to load dashboard"))
            }

            // Fetch coin balance
            refreshCoinBalance()

            // Sync local rewards state to cloud
            try {
                val tracker = DailyRewardsTracker.getInstance(ctx)
                val authPrefs = AuthPreferences.getInstance(ctx)
                if (!authPrefs.isGuestUser) {
                    val dailyState = tracker.getDailyState()
                    if (dailyState.todayClaimed && !tracker.isCheckinSynced) {
                        statsRepository.syncCheckinToCloud()
                    }
                    val shareState = tracker.getShareState()
                    if (shareState.todayClaimed && !tracker.isShareSynced) {
                        statsRepository.syncShareToCloud()
                    }
                }
            } catch (e: Exception) {
                // Non-critical, continue
            }
        }
    }

    /**
     * Refresh coin balance
     */
    fun refreshCoinBalance() {
        viewModelScope.launch {
            val result = getCoinBalanceUseCase()
            result.onSuccess { balance ->
                _coinBalance.value = balance
                _uiState.update { it.copy(coinBalance = balance) }
            }
            result.onFailure { error ->
                _sideEffect.emit(ProfileSideEffect.ShowError(error.message ?: "Failed to refresh coins"))
            }
        }
    }

    /**
     * Set coin balance directly from local value
     */
    fun setCoinBalance(balance: Int) {
        _uiState.update { it.copy(coinBalance = balance) }
    }

    /**
     * Update user profile
     */
    fun updateProfile(name: String, dob: String) {
        viewModelScope.launch {
            val result = updateProfileUseCase(name, dob)
            result.onSuccess {
                _sideEffect.emit(ProfileSideEffect.ShowToast("Profile updated successfully"))
            }
            result.onFailure { error ->
                _sideEffect.emit(ProfileSideEffect.ShowError(error.message ?: "Failed to update profile"))
            }
        }
    }
}
