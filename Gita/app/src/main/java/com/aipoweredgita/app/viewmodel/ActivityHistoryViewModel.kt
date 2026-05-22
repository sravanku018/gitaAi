package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.UserStats
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
    private val userStatsDao = db.userStatsDao()
    private val quizAttemptDao = db.quizAttemptDao()
    private val dailyActivityDao = db.dailyActivityDao()
    private val readVerseDao = db.readVerseDao()

    // ─── User Stats (overall time tracking) ───
    private val _userStats = MutableStateFlow<UserStats?>(null)
    val userStats: StateFlow<UserStats?> = _userStats.asStateFlow()

    // ─── Daily Activity (calendar data) ───
    private val _allActivity = MutableStateFlow<List<DailyActivity>>(emptyList())
    val allActivity: StateFlow<List<DailyActivity>> = _allActivity.asStateFlow()

    // ─── Quiz Stats ───
    private val _attempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val attempts: StateFlow<List<QuizAttempt>> = _attempts.asStateFlow()

    private val _averageAccuracy = MutableStateFlow(0f)
    val averageAccuracy: StateFlow<Float> = _averageAccuracy.asStateFlow()

    private val _averageTime = MutableStateFlow(0L)
    val averageTime: StateFlow<Long> = _averageTime.asStateFlow()

    // Quiz size grouping
    private val _quiz10Stats = MutableStateFlow<QuizSizeStatsData?>(null)
    val quiz10Stats: StateFlow<QuizSizeStatsData?> = _quiz10Stats.asStateFlow()

    private val _quiz20Stats = MutableStateFlow<QuizSizeStatsData?>(null)
    val quiz20Stats: StateFlow<QuizSizeStatsData?> = _quiz20Stats.asStateFlow()

    private val _quiz30Stats = MutableStateFlow<QuizSizeStatsData?>(null)
    val quiz30Stats: StateFlow<QuizSizeStatsData?> = _quiz30Stats.asStateFlow()

    private val _selectedQuizSize = MutableStateFlow<Int?>(null)
    val selectedQuizSize: StateFlow<Int?> = _selectedQuizSize.asStateFlow()

    // ─── Spiritual Path ───
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

    fun selectQuizSize(size: Int?) {
        _selectedQuizSize.value = size
    }

    private fun loadUserStats() {
        viewModelScope.launch {
            userStatsDao.initializeStatsIfNeeded()
        }
        viewModelScope.launch {
            userStatsDao.getUserStats().collect { stats ->
                _userStats.value = stats
            }
        }
    }

    private fun loadDailyActivity() {
        viewModelScope.launch {
            dailyActivityDao.getAllActivity().collect { activities ->
                _allActivity.value = activities
            }
        }
    }

    private fun loadQuizStats() {
        viewModelScope.launch {
            quizAttemptDao.getAllAttempts().collect { attemptsList ->
                _attempts.value = attemptsList
                updateAverages()
            }
        }
    }

    private fun loadGroupedQuizStats() {
        viewModelScope.launch {
            quizAttemptDao.getAttemptsByQuizSize(10).collect { attempts10 ->
                _quiz10Stats.value = if (attempts10.isNotEmpty()) {
                    QuizSizeStatsData(
                        quizSize = 10,
                        attempts = attempts10,
                        totalAttempts = quizAttemptDao.getTotalAttemptsByQuizSize(10),
                        averageAccuracy = quizAttemptDao.getAverageAccuracyByQuizSize(10) ?: 0f,
                        averageTime = quizAttemptDao.getAverageTimeByQuizSize(10) ?: 0L,
                        bestAttempt = quizAttemptDao.getBestAttemptByQuizSize(10)
                    )
                } else null
            }
        }
        viewModelScope.launch {
            quizAttemptDao.getAttemptsByQuizSize(20).collect { attempts20 ->
                _quiz20Stats.value = if (attempts20.isNotEmpty()) {
                    QuizSizeStatsData(
                        quizSize = 20,
                        attempts = attempts20,
                        totalAttempts = quizAttemptDao.getTotalAttemptsByQuizSize(20),
                        averageAccuracy = quizAttemptDao.getAverageAccuracyByQuizSize(20) ?: 0f,
                        averageTime = quizAttemptDao.getAverageTimeByQuizSize(20) ?: 0L,
                        bestAttempt = quizAttemptDao.getBestAttemptByQuizSize(20)
                    )
                } else null
            }
        }
        viewModelScope.launch {
            quizAttemptDao.getAttemptsByQuizSize(30).collect { attempts30 ->
                _quiz30Stats.value = if (attempts30.isNotEmpty()) {
                    QuizSizeStatsData(
                        quizSize = 30,
                        attempts = attempts30,
                        totalAttempts = quizAttemptDao.getTotalAttemptsByQuizSize(30),
                        averageAccuracy = quizAttemptDao.getAverageAccuracyByQuizSize(30) ?: 0f,
                        averageTime = quizAttemptDao.getAverageTimeByQuizSize(30) ?: 0L,
                        bestAttempt = quizAttemptDao.getBestAttemptByQuizSize(30)
                    )
                } else null
            }
        }
    }

    private fun loadSpiritualPathStats() {
        viewModelScope.launch {
            readVerseDao.getKarmaYogaReadCountFlow().collect { _karmaYogaCount.value = it }
        }
        viewModelScope.launch {
            readVerseDao.getBhaktiYogaReadCountFlow().collect { _bhaktiYogaCount.value = it }
        }
        viewModelScope.launch {
            readVerseDao.getJnanaYogaReadCountFlow().collect { _jnanaYogaCount.value = it }
        }
    }

    private suspend fun updateAverages() {
        _averageAccuracy.value = quizAttemptDao.getAverageAccuracy() ?: 0f
        _averageTime.value = quizAttemptDao.getAverageTime() ?: 0L
    }
}
