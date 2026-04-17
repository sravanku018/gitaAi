package com.aipoweredgita.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.ml.AIBadgeSystem
import com.aipoweredgita.app.ml.UserBadge
import com.aipoweredgita.app.ml.UserLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userStatsDao = GitaDatabase.getDatabase(application).userStatsDao()
    private val badgeSystem = AIBadgeSystem()

    private val _stats = MutableStateFlow<UserStats?>(null)
    val stats: StateFlow<UserStats?> = _stats.asStateFlow()

    private val _userBadges = MutableStateFlow<List<UserBadge>>(emptyList())
    val userBadges: StateFlow<List<UserBadge>> = _userBadges.asStateFlow()

    private val _userLevel = MutableStateFlow<UserLevel?>(null)
    val userLevel: StateFlow<UserLevel?> = _userLevel.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Initialize if needed
            userStatsDao.initializeStatsIfNeeded()

            // Collect stats
            userStatsDao.getUserStats().collect { userStats ->
                _stats.value = userStats

                // AI-generate badges and level based on stats
                if (userStats != null) {
                    generateAIBadgesAndLevel(userStats)
                }
            }
        }
    }

    private fun generateAIBadgesAndLevel(stats: UserStats) {
        viewModelScope.launch {
            try {
                // Generate badges using AI system
                val badges = badgeSystem.generateBadges(
                    versesRead = stats.versesRead,
                    quizzesTaken = stats.totalQuizzesTaken,
                    score = stats.totalCorrectAnswers, // Correct answers
                    totalQuestions = stats.totalQuestionsAnswered.coerceAtLeast(1),
                    timeSpent = stats.totalTimeSpentSeconds,
                    currentStreak = stats.currentStreak,
                    favoriteCount = stats.totalFavorites
                )
                _userBadges.value = badges

                // Calculate level
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
