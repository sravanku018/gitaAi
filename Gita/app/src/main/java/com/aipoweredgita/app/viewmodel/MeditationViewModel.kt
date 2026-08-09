package com.aipoweredgita.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aipoweredgita.app.repository.StatsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BreathingPhase(val label: String, val seconds: Int) {
    INHALE("Inhale", 4),
    HOLD("Hold", 4),
    EXHALE("Exhale", 4)
}

enum class MeditationDuration(val minutes: Int, val label: String) {
    FIVE(5, "5 min"),
    TEN(10, "10 min"),
    FIFTEEN(15, "15 min"),
    TWENTY(20, "20 min")
}

data class MeditationUiState(
    val selectedDuration: MeditationDuration = MeditationDuration.TEN,
    val isRunning: Boolean = false,
    val isPaused: Boolean = false,
    val timeLeftSeconds: Int = 0,
    val totalSeconds: Int = 0,
    val breathingPhase: BreathingPhase = BreathingPhase.INHALE,
    val breathingTimer: Int = 0,
    val isCompleted: Boolean = false
)

@HiltViewModel
class MeditationViewModel @Inject constructor(
    private val statsRepository: StatsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MeditationUiState())
    val uiState: StateFlow<MeditationUiState> = _uiState.asStateFlow()

    private var timerJob: kotlinx.coroutines.Job? = null

    fun selectDuration(duration: MeditationDuration) {
        if (!_uiState.value.isRunning) {
            _uiState.update { it.copy(selectedDuration = duration) }
        }
    }

    fun startTimer() {
        if (_uiState.value.isRunning && !_uiState.value.isPaused) return
        
        if (!_uiState.value.isRunning) {
            val totalSec = _uiState.value.selectedDuration.minutes * 60
            _uiState.update {
                it.copy(
                    totalSeconds = totalSec,
                    timeLeftSeconds = totalSec,
                    breathingPhase = BreathingPhase.INHALE,
                    breathingTimer = 0,
                    isRunning = true,
                    isPaused = false,
                    isCompleted = false
                )
            }
        } else {
            _uiState.update { it.copy(isPaused = false) }
        }
        startTimerJob()
    }

    private fun startTimerJob() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_uiState.value.timeLeftSeconds > 0) {
                delay(1000)
                if (!_uiState.value.isPaused) {
                    val currentState = _uiState.value
                    val newTimeLeft = currentState.timeLeftSeconds - 1
                    var newBreathingTimer = currentState.breathingTimer + 1
                    var newBreathingPhase = currentState.breathingPhase

                    if (newBreathingTimer >= currentState.breathingPhase.seconds) {
                        newBreathingTimer = 0
                        newBreathingPhase = when (currentState.breathingPhase) {
                            BreathingPhase.INHALE -> BreathingPhase.HOLD
                            BreathingPhase.HOLD -> BreathingPhase.EXHALE
                            BreathingPhase.EXHALE -> BreathingPhase.INHALE
                        }
                    }

                    _uiState.update {
                        it.copy(
                            timeLeftSeconds = newTimeLeft,
                            breathingTimer = newBreathingTimer,
                            breathingPhase = newBreathingPhase
                        )
                    }

                    if (newTimeLeft <= 0) {
                        finishMeditation()
                        break
                    }
                }
            }
        }
    }

    fun pauseTimer() {
        _uiState.update { it.copy(isPaused = true) }
        timerJob?.cancel()
    }

    fun stopTimer() {
        timerJob?.cancel()
        _uiState.update { 
            it.copy(
                isRunning = false, 
                isPaused = false,
                timeLeftSeconds = 0,
                totalSeconds = 0,
                breathingTimer = 0,
                breathingPhase = BreathingPhase.INHALE
            ) 
        }
    }

    private fun finishMeditation() {
        _uiState.update { it.copy(isRunning = false, isCompleted = true) }
        val minutes = _uiState.value.selectedDuration.minutes
        val coins = minutes * 2
        viewModelScope.launch {
            statsRepository.claimDailyReward(coins, "Meditation practice - $minutes mins")
        }
    }

    fun onCompletedAcknowledged() {
        _uiState.update { it.copy(isCompleted = false) }
    }
}
