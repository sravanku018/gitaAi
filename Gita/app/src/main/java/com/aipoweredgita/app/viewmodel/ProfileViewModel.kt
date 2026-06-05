package com.aipoweredgita.app.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.ReadVerse
import com.aipoweredgita.app.database.RecommendationData
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ml.AIBadgeSystem
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.UserLevel
import com.aipoweredgita.app.recommendation.AdaptiveCurriculumPlanner
import com.aipoweredgita.app.recommendation.RecommendationEngine
import com.aipoweredgita.app.recommendation.predictNext
import com.aipoweredgita.app.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(application)
    private val userStatsDao = db.userStatsDao()
    private val contentRepo = com.aipoweredgita.app.repository.ContentRepository(db.recommendationDataDao())
    private val readingRepo = com.aipoweredgita.app.repository.ReadingRepository(db.readVerseDao(), db.cachedVerseDao())
    private val dailyActivityRepo = com.aipoweredgita.app.repository.DailyActivityRepository(db.dailyActivityDao())
    private val quizStatsRepo = com.aipoweredgita.app.repository.QuizStatsRepository(db.quizAttemptDao())
    private val statsRepository = com.aipoweredgita.app.repository.StatsRepository(
        userStatsDao = db.userStatsDao(),
        dailyActivityDao = db.dailyActivityDao(),
        appContext = getApplication()
    )

    private val badgeSystem = AIBadgeSystem()

    // Badges & Level
    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats.asStateFlow()

    private val _userBadges = MutableStateFlow<List<UserBadge>>(emptyList())
    val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    private val _userLevel = MutableStateFlow<UserLevel?>(null)
    val userLevel: StateFlow<UserLevel?> = _userLevel.asStateFlow()

    // Dashboard daily stats
    data class DailyActivityData(
        val versesToday: Int = 0,
        val quizzesToday: Int = 0,
        val normalToday: Long = 0L,
        val quizToday: Long = 0L,
        val studioToday: Long = 0L,
        val versesListToday: List<ReadVerse> = emptyList()
    )

    data class NextActionData(
        val nextStep: String? = null,
        val nextLevel: Int = -1,
        val nextReason: String? = null
    )

    private val _dailyActivity = MutableStateFlow(DailyActivityData())
    val dailyActivity: StateFlow<DailyActivityData> = _dailyActivity.asStateFlow()

    private val _nextAction = MutableStateFlow(NextActionData())
    val nextAction: StateFlow<NextActionData> = _nextAction.asStateFlow()

    // Coin balance from API
    private val _coinBalance = MutableStateFlow(0)
    val coinBalance: StateFlow<Int> = _coinBalance.asStateFlow()

    // Recommendations
    private val _recommendations = MutableStateFlow<List<RecommendationData>>(emptyList())
    val recommendations: StateFlow<List<RecommendationData>> = _recommendations.asStateFlow()

    init {
        loadStats()
        loadRecommendations()
        viewModelScope.launch {
            statsRepository.coinBalance.collect { balance ->
                _coinBalance.value = balance
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            contentRepo.getActiveRecommendations().collect { recs ->
                _recommendations.value = recs
            }
        }
    }

    fun refreshCoinBalance() {
        viewModelScope.launch {
            _coinBalance.value = statsRepository.getBalance()
        }
    }

    /** Set coin balance directly from local value (avoids server fetch overwrite). */
    fun setCoinBalance(balance: Int) {
        _coinBalance.value = balance
    }

    fun loadDashboardData(context: Context) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now().toString()
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            // 1. Load today's verses
            try {
                val (vt, vlist) = withContext(Dispatchers.IO) {
                    val vtCount = readingRepo.totalReadToday(today)
                    val vlistData = readingRepo.getByDate(today)
                    vtCount to vlistData
                }
                _dailyActivity.update { it.copy(versesToday = vt, versesListToday = vlist) }
            } catch (e: Exception) {
                android.util.Log.w("ProfileVM", "Failed to load verses", e)
            }

            // 2. M1 next step predictor
            try {
                val lastSugDate = prefs.getString("next_suggestion_date", "")
                val nextAction = if (lastSugDate != today) {
                    val suggestion = withContext(Dispatchers.IO) { predictNext(db) }
                    val cleanedStep = StringUtils.clean(suggestion.nextStep)
                    val cleanedReason = StringUtils.clean(suggestion.reason)
                    withContext(Dispatchers.IO) {
                        prefs.edit()
                            .putString("next_step_label", cleanedStep)
                            .putInt("next_level", suggestion.nextLevel)
                            .putString("next_reason", cleanedReason)
                            .putString("next_suggestion_date", today)
                            .apply()
                    }
                    NextActionData(nextStep = cleanedStep, nextLevel = suggestion.nextLevel, nextReason = cleanedReason)
                } else {
                    val rawStep = prefs.getString("next_step_label", null)
                    val rawReason = prefs.getString("next_reason", null)
                    val cleanedStep = StringUtils.clean(rawStep)
                    val cleanedReason = StringUtils.clean(rawReason)
                    if (cleanedStep != rawStep || cleanedReason != rawReason) {
                        withContext(Dispatchers.IO) {
                            prefs.edit().apply {
                                if (cleanedStep != rawStep) putString("next_step_label", cleanedStep)
                                if (cleanedReason != rawReason) putString("next_reason", cleanedReason)
                            }.apply()
                        }
                    }
                    NextActionData(nextStep = cleanedStep, nextLevel = prefs.getInt("next_level", -1), nextReason = cleanedReason)
                }
                _nextAction.value = nextAction
            } catch (e: Exception) {
                android.util.Log.w("ProfileVM", "Failed to predict next", e)
            }

            // 3. Daily activity + quiz attempts
            try {
                val row = withContext(Dispatchers.IO) { dailyActivityRepo.getByDate(today) }
                val quizCount = withContext(Dispatchers.IO) {
                    quizStatsRepo.getAttemptsByDate(today).first().size
                }
                row?.let { dailyRow ->
                    _dailyActivity.update { state ->
                        state.copy(
                            normalToday = dailyRow.normalSeconds,
                            quizToday = dailyRow.quizSeconds,
                            studioToday = dailyRow.voiceStudioTimeSeconds,
                            quizzesToday = quizCount
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("ProfileVM", "Failed to load daily activity", e)
            }

            // 5. Generate recommendations once per day
            try {
                val lastRun = prefs.getString("last_rec_gen", "")
                if (lastRun != today) {
                    withContext(Dispatchers.IO) {
                        RecommendationEngine(context).generateRecommendations()
                        AdaptiveCurriculumPlanner(context).buildPlan()
                    }
                    prefs.edit().putString("last_rec_gen", today).apply()
                }
            } catch (e: Exception) {
                android.util.Log.w("ProfileVM", "Failed to generate recommendations", e)
            }

            // 6. Fetch coin balance from API
            _coinBalance.value = statsRepository.getBalance()

            // 7. Sync local rewards state to cloud
            try {
                val tracker = com.aipoweredgita.app.coin.DailyRewardsTracker.getInstance(getApplication())
                val authPrefs = com.aipoweredgita.app.utils.AuthPreferences.getInstance(getApplication())
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
                android.util.Log.w("ProfileVM", "Failed to sync local rewards: ${e.message}")
            }
        }
    }

    private fun loadStats() {
        viewModelScope.launch {
            userStatsDao.initializeStatsIfNeeded()
            userStatsDao.getUserStats().collect { userStats ->
                _stats.value = userStats
                if (userStats != null) {
                    generateAIBadgesAndLevel(userStats)
                }
            }
        }
    }

    private fun generateAIBadgesAndLevel(stats: UserStats) {
        viewModelScope.launch {
            try {
                val badges = badgeSystem.generateBadges(
                    versesRead = stats.versesRead,
                    quizzesTaken = stats.totalQuizzesTaken,
                    score = stats.totalCorrectAnswers,
                    totalQuestions = stats.totalQuestionsAnswered.coerceAtLeast(1),
                    timeSpent = stats.totalTimeSpentSeconds,
                    currentStreak = stats.currentStreak,
                    favoriteCount = stats.totalFavorites
                )
                _userBadges.value = badges

                val level = badgeSystem.calculateLevel(
                    versesRead = stats.versesRead,
                    quizzesTaken = stats.totalQuizzesTaken,
                    score = stats.totalCorrectAnswers,
                    totalQuestions = stats.totalQuestionsAnswered.coerceAtLeast(1),
                    timeSpent = stats.totalTimeSpentSeconds,
                    currentStreak = stats.currentStreak,
                    favoriteCount = stats.totalFavorites
                )
                _userLevel.value = level
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateProfile(name: String, dob: String) {
        viewModelScope.launch {
            userStatsDao.updateProfile(name, dob)
        }
    }
}
