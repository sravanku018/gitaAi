package com.aipoweredgita.app.domain.model

import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.QuizAttempt
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.viewmodel.QuizSizeStatsData

data class ActivityHistoryUiState(
    val userStats: UserStats? = null,
    val allActivity: List<DailyActivity> = emptyList(),
    val attempts: List<QuizAttempt> = emptyList(),
    val averageAccuracy: Float = 0f,
    val averageTime: Long = 0L,
    val quiz10Stats: QuizSizeStatsData? = null,
    val quiz15Stats: QuizSizeStatsData? = null,
    val quiz20Stats: QuizSizeStatsData? = null,
    val quiz25Stats: QuizSizeStatsData? = null,
    val quiz30Stats: QuizSizeStatsData? = null,
    val battleQuizStats: QuizSizeStatsData? = null,
    val selectedQuizSize: Int? = null,
    val karmaYogaCount: Int = 0,
    val bhaktiYogaCount: Int = 0,
    val jnanaYogaCount: Int = 0,
    val dhyanaYogaCount: Int = 0,
    val rajaYogaCount: Int = 0,
    val meditationMinutesTotal: Long = 0L,   // total minutes meditated
    val meditationSessionCount: Int = 0,      // number of sessions
    override val isLoading: Boolean = false,
    override val error: String? = null
) : BaseUiState
