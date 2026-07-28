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
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
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
                val authPrefs = com.aipoweredgita.app.utils.AuthPreferences.getInstance(context)
                val uid = authPrefs.userId
                val token = authPrefs.token
                if (uid != null && token != null) {
                    val serverAttempts = com.aipoweredgita.app.network.CoinApi.retrofitService.getQuizHistory(
                        uid, "Bearer $token", limit = 500
                    )
                    val quizDao = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context).quizAttemptDao()
                    
                    // Cleanup any glitched battle_quiz records from before the fix
                    quizDao.deleteGlitchedBattleQuizzes()

                    val fmtPlain = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val fmtIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val fmtIsoShort = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    fun parseTimestamp(raw: String): Long {
                        if (raw.isBlank()) return 0L
                        // Try OffsetDateTime first to robustly parse ISO-8601 strings (Z, offsets like +05:30)
                        try {
                            return java.time.OffsetDateTime.parse(raw).toInstant().toEpochMilli()
                        } catch (_: Exception) {}
                        try {
                            return raw.toLong()
                        } catch (_: Exception) {}
                        return try { fmtPlain.parse(raw)?.time ?: 0L }
                        catch (_: Exception) {
                            try { fmtIso.parse(raw)?.time ?: 0L }
                            catch (_: Exception) {
                                try { fmtIsoShort.parse(raw)?.time ?: 0L }
                                catch (_: Exception) { 0L }
                            }
                        }
                    }
                    for (dto in serverAttempts) {
                        val parsedTimestamp = parseTimestamp(dto.created_at)
                        if (parsedTimestamp <= 0L) {
                            continue
                        }
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(parsedTimestamp))
                        
                        val attemptId = dto.attempt_id
                        val exists = if (!attemptId.isNullOrEmpty()) {
                            false // UNIQUE constraint will ignore automatically on insert
                        } else {
                            // Fallback to fuzzy match only for legacy server records
                            quizDao.countSimilarAttempts(
                                score = dto.score,
                                totalQuestions = dto.total_questions,
                                quizType = dto.quiz_type,
                                timestamp = parsedTimestamp
                            ) > 0
                        }

                        if (!exists) {
                            quizDao.insertAttempt(
                                com.aipoweredgita.app.database.QuizAttempt(
                                    attemptId = attemptId ?: java.util.UUID.randomUUID().toString(),
                                    syncStatus = "SYNCED",
                                    language = dto.language,
                                    score = dto.score,
                                    totalQuestions = dto.total_questions,
                                    timestamp = parsedTimestamp,
                                    date = dateStr,
                                    quizType = dto.quiz_type,
                                    timeSpentSeconds = dto.time_spent_seconds
                                )
                            )
                        }
                    }
                    
                    // Deduplicate existing local DB records (clean up duplicates from prior sync bugs)
                    try {
                        val allAttempts = quizDao.getAllAttemptsDirect()
                        val seen = mutableSetOf<String>()
                        for (attempt in allAttempts) {
                            val key = "${attempt.score}_${attempt.totalQuestions}_${attempt.quizType}_${attempt.timeSpentSeconds}_${attempt.date}"
                            if (seen.contains(key)) {
                                quizDao.deleteAttempt(attempt)
                            } else {
                                seen.add(key)
                            }
                        }
                    } catch (dedupEx: Exception) {
                        android.util.Log.e("ActivityHistoryVM", "Failed to deduplicate local database: ${dedupEx.message}")
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
        // Battle Quiz segment
        viewModelScope.launch {
            val db = com.aipoweredgita.app.database.GitaDatabase.getDatabase(context)
            db.quizAttemptDao().getAttemptsByType("battle_quiz").collect { list ->
                if (list.isEmpty()) {
                    _uiState.update { it.copy(battleQuizStats = null) }
                } else {
                    val stats = db.quizAttemptDao().getStatsByType("battle_quiz")
                    val best  = db.quizAttemptDao().getBestAttemptByType("battle_quiz")
                    _uiState.update {
                        it.copy(battleQuizStats = QuizSizeStatsData(
                            quizSize = -1,
                            attempts = list,
                            totalAttempts = stats?.totalAttempts ?: list.size,
                            averageAccuracy = stats?.averageAccuracy ?: 0f,
                            averageTime = stats?.averageTime ?: 0L,
                            bestAttempt = best
                        ))
                    }
                }
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
        viewModelScope.launch { 
            spiritualPathRepo.dhyanaYogaCount.collect { count ->
                _uiState.update { it.copy(dhyanaYogaCount = count) }
            } 
        }
        viewModelScope.launch { 
            spiritualPathRepo.rajaYogaCount.collect { count ->
                _uiState.update { it.copy(rajaYogaCount = count) }
            } 
        }
    }
}
