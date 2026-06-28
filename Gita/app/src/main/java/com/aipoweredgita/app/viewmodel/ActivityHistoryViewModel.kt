package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.domain.model.ActivityHistoryEvent
import com.aipoweredgita.app.domain.model.ActivityHistoryUiState
import com.aipoweredgita.app.database.UserStatsDao
import com.aipoweredgita.app.repository.DailyActivityRepository
import com.aipoweredgita.app.repository.QuizStatsRepository
import com.aipoweredgita.app.repository.SpiritualPathRepository
import com.aipoweredgita.app.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class QuizSizeStatsData(
    val quizSize: Int,
    val attempts: List<QuizAttempt>,
    val totalAttempts: Int,
    val averageAccuracy: Float,
    val averageTime: Long,
    val bestAttempt: QuizAttempt?
)

@HiltViewModel
class ActivityHistoryViewModel @Inject constructor(
    private val quizStatsRepo: QuizStatsRepository,
    private val dailyActivityRepo: DailyActivityRepository,
    private val spiritualPathRepo: SpiritualPathRepository,
    private val userStatsDao: UserStatsDao,
    private val statsRepo: StatsRepository
) : ViewModel() {

    val coinBalance: StateFlow<Int> = statsRepo.coinBalance

    private val _uiState = MutableStateFlow(ActivityHistoryUiState())
    val uiState: StateFlow<ActivityHistoryUiState> = _uiState.asStateFlow()

    init {
        loadUserStats()
        loadDailyActivity()
        loadQuizStats()
        loadGroupedQuizStats()
        loadSpiritualPathStats()
    }

    fun onEvent(event: ActivityHistoryEvent) {
        when (event) {
            is ActivityHistoryEvent.SelectQuizSize -> selectQuizSize(event.size)
        }
    }

    private fun selectQuizSize(size: Int?) {
        _uiState.update { it.copy(selectedQuizSize = size) }
    }

    private fun loadUserStats() {
        viewModelScope.launch { userStatsDao.initializeStatsIfNeeded() }
        viewModelScope.launch {
            userStatsDao.getUserStats().collect { userStats ->
                _uiState.update { it.copy(userStats = userStats) }
            }
        }
    }

    private fun loadDailyActivity() {
        viewModelScope.launch {
            dailyActivityRepo.getAllActivity().collect { activity ->
                _uiState.update { it.copy(allActivity = activity) }
            }
        }
        viewModelScope.launch {
            try {
                val context = com.aipoweredgita.app.GitaApp.instance
                val authPrefs = com.aipoweredgita.app.utils.AuthPreferences.getInstance(context)
                val uid = authPrefs.userId
                val token = authPrefs.token
                if (uid != null && token != null) {
                    val serverActivity = com.aipoweredgita.app.network.CoinApi.retrofitService.getActivityHistory(
                        uid, "Bearer $token"
                    )
                    val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
                    val dao = db.dailyActivityDao()
                    for (day in serverActivity) {
                        dao.insertIfAbsent(
                            com.aipoweredgita.app.database.DailyActivity(
                                date = day.date,
                                normalSeconds = 0,
                                quizSeconds = day.quizzes.toLong() * 300,
                                voiceStudioTimeSeconds = day.voice_chats.toLong() * 120,
                                versesRead = day.total_events
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadQuizStats() {
        viewModelScope.launch {
            quizStatsRepo.getAllAttempts().collect { list ->
                _uiState.update { 
                    it.copy(
                        attempts = list,
                        averageAccuracy = quizStatsRepo.getAverageAccuracy() ?: 0f,
                        averageTime = quizStatsRepo.getAverageTime() ?: 0L
                    ) 
                }
            }
        }
        viewModelScope.launch {
            try {
                val context = com.aipoweredgita.app.GitaApp.instance
                val authPrefs = com.aipoweredgita.app.utils.AuthPreferences.getInstance(context)
                val uid = authPrefs.userId
                val token = authPrefs.token
                if (uid != null && token != null) {
                    val serverAttempts = com.aipoweredgita.app.network.CoinApi.retrofitService.getQuizHistory(
                        uid, "Bearer $token", limit = 500
                    )
                    val quizDao = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).quizAttemptDao()
                    val fmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    quizDao.deleteAll()
                    for (dto in serverAttempts) {
                        val ts = try { fmt.parse(dto.created_at)?.time ?: System.currentTimeMillis() } catch (_: Exception) { System.currentTimeMillis() }
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(ts))
                        quizDao.insertAttempt(
                            com.aipoweredgita.app.database.QuizAttempt(
                                score = dto.score,
                                totalQuestions = dto.total_questions,
                                timestamp = ts,
                                date = dateStr,
                                quizType = dto.quiz_type,
                                timeSpentSeconds = dto.time_spent_seconds
                            )
                        )
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadGroupedQuizStats() {
        suspend fun buildStats(size: Int, list: List<QuizAttempt>): QuizSizeStatsData? {
            if (list.isEmpty()) return null
            val stats = quizStatsRepo.getStatsByQuizSize(size)
            return QuizSizeStatsData(
                quizSize = size, attempts = list,
                totalAttempts = stats?.totalAttempts ?: 0,
                averageAccuracy = stats?.averageAccuracy ?: 0f,
                averageTime = stats?.averageTime ?: 0L,
                bestAttempt = quizStatsRepo.getBestAttemptByQuizSize(size)
            )
        }
        viewModelScope.launch {
            quizStatsRepo.getAttemptsByQuizSize(10).collect { list ->
                _uiState.update { it.copy(quiz10Stats = buildStats(10, list)) }
            }
        }
        viewModelScope.launch {
            quizStatsRepo.getAttemptsByQuizSize(15).collect { list ->
                _uiState.update { it.copy(quiz15Stats = buildStats(15, list)) }
            }
        }
        viewModelScope.launch {
            quizStatsRepo.getAttemptsByQuizSize(20).collect { list ->
                _uiState.update { it.copy(quiz20Stats = buildStats(20, list)) }
            }
        }
        viewModelScope.launch {
            quizStatsRepo.getAttemptsByQuizSize(25).collect { list ->
                _uiState.update { it.copy(quiz25Stats = buildStats(25, list)) }
            }
        }
        viewModelScope.launch {
            quizStatsRepo.getAttemptsByQuizSize(30).collect { list ->
                _uiState.update { it.copy(quiz30Stats = buildStats(30, list)) }
            }
        }
    }

    private fun loadSpiritualPathStats() {
        viewModelScope.launch { 
            spiritualPathRepo.karmaYogaCount.collect { count ->
                _uiState.update { it.copy(karmaYogaCount = count) }
            } 
        }
        viewModelScope.launch { 
            spiritualPathRepo.bhaktiYogaCount.collect { count ->
                _uiState.update { it.copy(bhaktiYogaCount = count) }
            } 
        }
        viewModelScope.launch { 
            spiritualPathRepo.jnanaYogaCount.collect { count ->
                _uiState.update { it.copy(jnanaYogaCount = count) }
            } 
        }
    }
}
