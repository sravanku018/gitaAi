package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.QuizAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizSizeStats(
    val quizSize: Int,
    val attempts: List<QuizAttempt>,
    val totalAttempts: Int,
    val averageAccuracy: Float,
    val averageTime: Long,
    val bestAttempt: QuizAttempt?
)

class QuizStatsViewModel(application: Application) : AndroidViewModel(application) {
    private val quizAttemptDao = GitaDatabase.getDatabase(application).quizAttemptDao()

    private val _attempts = MutableStateFlow<List<QuizAttempt>>(emptyList())
    val attempts: StateFlow<List<QuizAttempt>> = _attempts.asStateFlow()

    private val _averageAccuracy = MutableStateFlow(0f)
    val averageAccuracy: StateFlow<Float> = _averageAccuracy.asStateFlow()

    private val _averageTime = MutableStateFlow(0L)
    val averageTime: StateFlow<Long> = _averageTime.asStateFlow()

    // Statistics grouped by quiz size
    private val _quiz10Stats = MutableStateFlow<QuizSizeStats?>(null)
    val quiz10Stats: StateFlow<QuizSizeStats?> = _quiz10Stats.asStateFlow()

    private val _quiz20Stats = MutableStateFlow<QuizSizeStats?>(null)
    val quiz20Stats: StateFlow<QuizSizeStats?> = _quiz20Stats.asStateFlow()

    private val _quiz30Stats = MutableStateFlow<QuizSizeStats?>(null)
    val quiz30Stats: StateFlow<QuizSizeStats?> = _quiz30Stats.asStateFlow()

    private val _selectedQuizSize = MutableStateFlow<Int?>(null)
    val selectedQuizSize: StateFlow<Int?> = _selectedQuizSize.asStateFlow()

    init {
        loadStats()
        loadGroupedStats()
    }

    fun selectQuizSize(size: Int?) {
        _selectedQuizSize.value = size
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Load all attempts and update averages when data changes
            quizAttemptDao.getAllAttempts().collect { attemptsList ->
                _attempts.value = attemptsList

                // Update averages whenever attempts change
                updateAverages()
            }
        }
    }

    private fun loadGroupedStats() {
        // Load stats for 10 question quizzes
        viewModelScope.launch {
            quizAttemptDao.getAttemptsByQuizSize(10).collect { attempts10 ->
                if (attempts10.isNotEmpty()) {
                    _quiz10Stats.value = QuizSizeStats(
                        quizSize = 10,
                        attempts = attempts10,
                        totalAttempts = quizAttemptDao.getTotalAttemptsByQuizSize(10),
                        averageAccuracy = quizAttemptDao.getAverageAccuracyByQuizSize(10) ?: 0f,
                        averageTime = quizAttemptDao.getAverageTimeByQuizSize(10) ?: 0L,
                        bestAttempt = quizAttemptDao.getBestAttemptByQuizSize(10)
                    )
                } else {
                    _quiz10Stats.value = null
                }
            }
        }

        // Load stats for 20 question quizzes
        viewModelScope.launch {
            quizAttemptDao.getAttemptsByQuizSize(20).collect { attempts20 ->
                if (attempts20.isNotEmpty()) {
                    _quiz20Stats.value = QuizSizeStats(
                        quizSize = 20,
                        attempts = attempts20,
                        totalAttempts = quizAttemptDao.getTotalAttemptsByQuizSize(20),
                        averageAccuracy = quizAttemptDao.getAverageAccuracyByQuizSize(20) ?: 0f,
                        averageTime = quizAttemptDao.getAverageTimeByQuizSize(20) ?: 0L,
                        bestAttempt = quizAttemptDao.getBestAttemptByQuizSize(20)
                    )
                } else {
                    _quiz20Stats.value = null
                }
            }
        }

        // Load stats for 30 question quizzes
        viewModelScope.launch {
            quizAttemptDao.getAttemptsByQuizSize(30).collect { attempts30 ->
                if (attempts30.isNotEmpty()) {
                    _quiz30Stats.value = QuizSizeStats(
                        quizSize = 30,
                        attempts = attempts30,
                        totalAttempts = quizAttemptDao.getTotalAttemptsByQuizSize(30),
                        averageAccuracy = quizAttemptDao.getAverageAccuracyByQuizSize(30) ?: 0f,
                        averageTime = quizAttemptDao.getAverageTimeByQuizSize(30) ?: 0L,
                        bestAttempt = quizAttemptDao.getBestAttemptByQuizSize(30)
                    )
                } else {
                    _quiz30Stats.value = null
                }
            }
        }
    }

    private suspend fun updateAverages() {
        // Load average accuracy
        val avgAcc = quizAttemptDao.getAverageAccuracy() ?: 0f
        _averageAccuracy.value = avgAcc

        // Load average time
        val avgTime = quizAttemptDao.getAverageTime() ?: 0L
        _averageTime.value = avgTime
    }
}
