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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    private val updateProfileUseCase: UpdateProfileUseCase,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: Context
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
    private val _sideEffect = Channel<ProfileSideEffect>()
    val sideEffect: Flow<ProfileSideEffect> = _sideEffect.receiveAsFlow()

    // Backward-compatible separate state flows for existing UI code
    private val _stats = MutableStateFlow<com.aipoweredgita.app.database.UserStats?>(null)
    val stats: StateFlow<com.aipoweredgita.app.database.UserStats?> = _stats.asStateFlow()

    var yogaLevels = emptyList<com.aipoweredgita.app.network.YogaLevel>()
        private set
    var yogaSubStages = emptyList<com.aipoweredgita.app.network.YogaSubStage>()
        private set

    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    private val _dailyActivity = MutableStateFlow(DailyActivityData())
    val dailyActivity: StateFlow<DailyActivityData> = _dailyActivity.asStateFlow()

    private val _nextAction = MutableStateFlow(NextActionData())
    val nextAction: StateFlow<NextActionData> = _nextAction.asStateFlow()

    private val _recommendations = MutableStateFlow<List<com.aipoweredgita.app.database.RecommendationData>>(emptyList())
    val recommendations: StateFlow<List<com.aipoweredgita.app.database.RecommendationData>> = _recommendations.asStateFlow()

    private val _coinHistory = MutableStateFlow<List<com.aipoweredgita.app.network.CoinHistoryEntry>>(emptyList())
    val coinHistory: StateFlow<List<com.aipoweredgita.app.network.CoinHistoryEntry>> = _coinHistory.asStateFlow()

    init {
        loadStats()
        loadRecommendations()
        loadYogaStages()
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
                _uiState.update { it.copy(isLoading = true) }
                
                statsRepository.initializeStatsIfNeeded()
                statsRepository.getUserStatsFlow().collect { userStats ->
                    _stats.value = userStats
                    _uiState.update { it.copy(stats = userStats, isLoading = false) }
                    if (userStats != null) {
                        generateAIBadgesAndLevel(userStats)
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                _sideEffect.send(ProfileSideEffect.ShowError(e.message ?: "Unknown error"))
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
                _sideEffect.send(ProfileSideEffect.ShowError(error.message ?: "Failed to generate badges"))
            }
        } catch (e: Exception) {
            _sideEffect.send(ProfileSideEffect.ShowError(e.message ?: "Failed to generate badges"))
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
                updateServerLevel(balance)
            }
        }
    }

    private fun loadYogaStages() {
        viewModelScope.launch {
            try {
                val res = com.aipoweredgita.app.network.CoinApi.retrofitService.getYogaStages()
                yogaLevels = res.levels
                yogaSubStages = res.sub_stages
                updateServerLevel(_coinBalance.value)
            } catch (e: Exception) {
                // Ignore failure
            }
        }
    }

    private fun updateServerLevel(balance: Int) {
        if (yogaLevels.isEmpty()) return
        val activeLevel = yogaLevels.find { balance >= it.min_coins && balance <= it.max_coins } ?: yogaLevels.lastOrNull()
        val activeSubStage = yogaSubStages.find { balance >= it.min_coins && balance <= it.max_coins } 
            ?: yogaSubStages.filter { it.level == activeLevel?.level }.maxByOrNull { it.sub_level }
        _uiState.update { it.copy(serverYogaLevel = activeLevel, serverYogaSubStage = activeSubStage) }
    }

    /**
     * Load dashboard data
     */
    fun loadDashboard(context: Context?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            val ctx = context ?: appContext
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
                _sideEffect.send(ProfileSideEffect.ShowError(error.message ?: "Failed to load dashboard"))
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
                setCoinBalance(balance)
            }
            result.onFailure { error ->
                _sideEffect.send(ProfileSideEffect.ShowError(error.message ?: "Failed to refresh coins"))
            }
        }
    }

    /**
     * Load coin history (merges local and server data)
     */
    fun loadCoinHistory() {
        viewModelScope.launch {
            val authPrefs = AuthPreferences.getInstance(appContext)
            val isGuest = authPrefs.isGuestUser
            val effectiveUid = authPrefs.userId?.takeIf { it.isNotEmpty() }
                ?: _stats.value?.userId?.takeIf { it.isNotEmpty() }
            
            if (effectiveUid == null || effectiveUid.isEmpty()) return@launch

            if (isGuest) {
                _coinHistory.value = buildLocalHistory()
            } else {
                val token = authPrefs.token
                var serverLoaded = false
                if (token != null) {
                    try {
                        val serverHistory = com.aipoweredgita.app.network.CoinApi.retrofitService.getHistory(
                            effectiveUid, "Bearer $token", limit = 500
                        )
                        if (serverHistory.isNotEmpty()) {
                            com.aipoweredgita.app.coin.CoinTransactionLogger.syncFromServer(appContext, serverHistory)
                        }
                        
                        val mergedHistory = buildLocalHistory()
                        _coinHistory.value = mergedHistory.filter { entry ->
                            !(entry.source == "signup" && entry.description.contains("Guest", ignoreCase = true))
                        }
                        
                        serverLoaded = true
                    } catch (e: Exception) {
                        // fallback below
                    }
                }
                
                if (!serverLoaded) {
                    val localOnly = buildLocalHistory()
                    _coinHistory.value = localOnly.filter { entry ->
                        !(entry.source == "signup" && entry.description.contains("Guest", ignoreCase = true))
                    }
                }
            }
        }
    }

    private suspend fun buildLocalHistory(): List<com.aipoweredgita.app.network.CoinHistoryEntry> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val utcFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("UTC")
        }
        com.aipoweredgita.app.coin.CoinTransactionLogger.getHistory(appContext).map { tx ->
            com.aipoweredgita.app.network.CoinHistoryEntry(
                amount = tx.amount,
                type = tx.type.name,
                source = tx.description.lowercase().let { desc ->
                    when {
                        desc.contains("welcome") -> "signup"
                        desc.contains("battle") -> "battle_quiz"
                        desc.contains("quiz") -> "quiz_completion"
                        desc.contains("check") || desc.contains("checkin") -> "checkin_day"
                        desc.contains("share") -> "share_sloka"
                        desc.contains("voice") -> "voice_chat"
                        desc.contains("chapter") -> "chapter_completion"
                        desc.contains("level") -> "level_up_bonus"
                        else -> "other"
                    }
                },
                description = tx.description,
                created_at = utcFmt.format(java.util.Date(tx.timestamp))
            )
        }
    }

    /**
     * Claim a daily reward (check-in, meditation, etc.)
     */
    fun claimDailyReward(coins: Int, description: String) {
        viewModelScope.launch {
            statsRepository.claimDailyReward(coins, description)
            refreshCoinBalance()
        }
    }

    /**
     * Claim a share reward
     */
    fun claimShareReward(coins: Int, description: String) {
        viewModelScope.launch {
            statsRepository.claimShareReward(coins, description)
            refreshCoinBalance()
        }
    }

    /**
     * Track a sloka being shared
     */
    fun trackSlokaShared() {
        viewModelScope.launch {
            statsRepository.trackSlokaShared()
            refreshCoinBalance()
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
                _sideEffect.send(ProfileSideEffect.ShowToast("Profile updated successfully"))
            }
            result.onFailure { error ->
                _sideEffect.send(ProfileSideEffect.ShowError(error.message ?: "Failed to update profile"))
            }
        }
    }
}
