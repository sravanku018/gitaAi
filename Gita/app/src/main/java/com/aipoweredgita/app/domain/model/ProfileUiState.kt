package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.database.ReadVerse
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.UserLevel

/**
 * UI State for Profile Screen
 * Single source of truth for all profile-related UI state
 */
data class ProfileUiState(
    val stats: UserStats? = null,
    val badges: List<UserBadge> = emptyList(),
    val level: UserLevel? = null,
    val dailyActivity: DailyActivityData = DailyActivityData(),
    val nextAction: NextActionData = NextActionData(),
    val coinBalance: Int = 0,
    val recommendations: List<RecommendationData> = emptyList(),
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseUiState

/**
 * Daily activity data for the profile dashboard
 */
data class DailyActivityData(
    val versesToday: Int = 0,
    val quizzesToday: Int = 0,
    val normalToday: Long = 0L,
    val quizToday: Long = 0L,
    val studioToday: Long = 0L,
    val versesListToday: List<ReadVerse> = emptyList()
)

/**
 * Next action recommendation data
 */
data class NextActionData(
    val nextStep: String? = null,
    val nextLevel: Int = -1,
    val nextReason: String? = null
)

/**
 * Events that can occur on the Profile screen
 */
sealed class ProfileEvent {
    data object LoadDashboard : ProfileEvent()
    data class UpdateProfile(val name: String, val dob: String) : ProfileEvent()
    data object RefreshCoins : ProfileEvent()
    data class SetCoinBalance(val balance: Int) : ProfileEvent()
}

/**
 * One-time side effects for the Profile screen
 */
sealed class ProfileSideEffect {
    data class ShowError(val message: String) : ProfileSideEffect()
    data class ShowToast(val message: String) : ProfileSideEffect()
    data object NavigateToLogin : ProfileSideEffect()
}
