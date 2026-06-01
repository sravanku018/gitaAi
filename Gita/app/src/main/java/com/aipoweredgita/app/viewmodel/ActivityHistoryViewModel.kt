package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.repository.DailyActivityRepository
import com.aipoweredgita.app.repository.QuizStatsRepository
import com.aipoweredgita.app.repository.SpiritualPathRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizSizeStatsData(
    val quizSize: Int,
    val attempts: List<QuizAttempt>,
    val totalAttempts: Int,
    val averageAccuracy: Float,
    val averageTime: Long,
    val bestAttempt: QuizAttempt?
)

class ActivityHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GitaDatabase.getDatabase(application)
    private val quizStatsRepo = QuizStatsRepository(db.quizAttemptDao())
    private val dailyActivityRepo = DailyActivityRepository(db.dailyActivityDao())
    private val spiritualPathRepo = SpiritualPathRepository(db.readVerseDao())
    private val userStatsDao = db.userStatsDao()

    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    private val _allActivity = MutableStateFlow<List<DailyActivity>>(emptyList())
    val allActivity: StateFlow<List<DailyActivity>> = _allActivity.asStateFlow()

    private val _attempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val attempts: StateFlow<List<QuizAttempt>> = _attempts.asStateFlow()

    private val _averageAccuracy = MutableStateFlow(0f)
    val averageAccuracy: StateFlow<Float> = _averageAccuracy.asStateFlow()

    private val _averageTime = MutableStateFlow(0L)
    val averageTime: StateFlow<Long> = _averageTime.asStateFlow()

    private val _quiz10Stats = MutableStateFlow<QuizSizeStatsData?>(null)
    val quiz10Stats: StateFlow<QuizSizeStatsData?> = _quiz10Stats.asStateFlow()

    private val _quiz20Stats = MutableStateFlow<QuizSizeStatsData?>(null)
    val quiz20Stats: StateFlow<QuizSizeStatsData?> = _quiz20Stats.asStateFlow()

    private val _quiz30Stats = MutableStateFlow<QuizSizeStatsData?>(null)
    val quiz30Stats: StateFlow<QuizSizeStatsData?> = _quiz30Stats.asStateFlow()

    private val _selectedQuizSize = MutableStateFlow<Int?>(null)
    val selectedQuizSize: StateFlow<Int?> = _selectedQuizSize.asStateFlow()

    private val _karmaYogaCount = MutableStateFlow(0)
    val karmaYogaCount: StateFlow<Int> = _karmaYogaCount.asStateFlow()

    private val _bhaktiYogaCount = MutableStateFlow(0)
    val bhaktiYogaCount: StateFlow<Int> = _bhaktiYogaCount.asStateFlow()

    private val _jnanaYogaCount = MutableStateFlow(0)
    val jnanaYogaCount: StateFlow<Int> = _jnanaYogaCount.asStateFlow()

    init {
        loadUserStats()
        loadDailyActivity()
        loadQuizStats()
        loadGroupedQuizStats()
        loadSpiritualPathStats()
    }

    fun selectQuizSize(size: Int?) { _selectedQuizSize.value = size }

    private fun loadUserStats() {
        viewModelScope.launch { userStatsDao.initializeStatsIfNeeded() }
        viewModelScope.launch {
            userStatsDao.getUserStats().collect { _userStats.value = it }
        }
    }

    private fun loadDailyActivity() {
        viewModelScope.launch {
            dailyActivityRepo.getAllActivity().collect { _allActivity.value = it }
        }
    }

    private fun loadQuizStats() {
        viewModelScope.launch {
            quizStatsRepo.getAllAttempts().collect { list ->
                _attempts.value = list
                _averageAccuracy.value = quizStatsRepo.getAverageAccuracy() ?: 0f
                _averageTime.value = quizStatsRepo.getAverageTime() ?: 0L
            }
        }
    }

    private fun loadGroupedQuizStats() {
        suspend fun buildStats(size: Int, list: List<QuizAttempt>): QuizSizeStatsData? {
            if (list.isEmpty()) return null
            return QuizSizeStatsData(
                quizSize = size, attempts = list,
                totalAttempts = quizStatsRepo.getTotalAttemptsByQuizSize(size),
                averageAccuracy = quizStatsRepo.getAverageAccuracyByQuizSize(size) ?: 0f,
                averageTime = quizStatsRepo.getAverageTimeByQuizSize(size) ?: 0L,
                bestAttempt = quizStatsRepo.getBestAttemptByQuizSize(size)
            )
        }
        viewModelScope.launch { quizStatsRepo.getAttemptsByQuizSize(10).collect { _quiz10Stats.value = buildStats(10, it) } }
        viewModelScope.launch { quizStatsRepo.getAttemptsByQuizSize(20).collect { _quiz20Stats.value = buildStats(20, it) } }
        viewModelScope.launch { quizStatsRepo.getAttemptsByQuizSize(30).collect { _quiz30Stats.value = buildStats(30, it) } }
    }

    private fun loadSpiritualPathStats() {
        viewModelScope.launch { spiritualPathRepo.karmaYogaCount.collect { _karmaYogaCount.value = it } }
        viewModelScope.launch { spiritualPathRepo.bhaktiYogaCount.collect { _bhaktiYogaCount.value = it } }
        viewModelScope.launch { spiritualPathRepo.jnanaYogaCount.collect { _jnanaYogaCount.value = it } }
    }
}
