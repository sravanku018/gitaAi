package com.aipoweredgita.app.repository

import com.aipoweredgita.app.database.YogaProgression
import com.aipoweredgita.app.database.YogaProgressionDao
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.min

class YogaProgressionRepository(private val dao: YogaProgressionDao) {
    
    fun getProgressionFlow(): Flow<YogaProgression?> = dao.getProgressionFlow()
    
    suspend fun getProgression(): YogaProgression {
        return dao.getProgression() ?: YogaProgression().also {
            dao.insertProgression(it)
        }
    }
    
    /**
     * Update progression after quiz completion
     * @param score Number of correct answers
     * @param total Total questions
     */
    suspend fun updateFromQuiz(score: Int, total: Int) {
        val progression = getProgression()
        val accuracy = if (total > 0) (score.toFloat() / total * 100f) else 0f
        
        // Update quiz accuracy (weighted average with recent performance)
        val newQuizAccuracy = if (progression.totalQuizzesTaken > 0) {
            (progression.quizAccuracy * 0.7f) + (accuracy * 0.3f)
        } else {
            accuracy
        }
        
        val updated = progression.copy(
            quizAccuracy = newQuizAccuracy,
            totalQuizzesTaken = progression.totalQuizzesTaken + 1,
            lastUpdated = System.currentTimeMillis()
        )
        
        dao.updateProgression(updated)
        calculateAndUpdateProgression()
    }
    
    /**
     * Update progression after reading a verse
     */
    suspend fun updateFromReading() {
        val progression = getProgression()
        val today = LocalDate.now().toString()
        
        // Update consecutive days if reading today
        val consecutiveDays = if (progression.lastActivityDate == today) {
            progression.consecutiveDays
        } else if (progression.lastActivityDate == LocalDate.now().minusDays(1).toString()) {
            progression.consecutiveDays + 1
        } else {
            1 // Reset streak
        }
        
        // Calculate reading consistency (based on total verses and streak)
        val readingConsistency = min(100f, (progression.totalVersesRead + 1) / 10f + consecutiveDays * 2f)
        
        val updated = progression.copy(
            totalVersesRead = progression.totalVersesRead + 1,
            lastActivityDate = today,
            consecutiveDays = consecutiveDays,
            readingConsistency = readingConsistency,
            lastUpdated = System.currentTimeMillis()
        )
        
        dao.updateProgression(updated)
        calculateAndUpdateProgression()
    }
    
    /**
     * Check for inactivity and apply decay (1% per day)
     * @return Triple<Boolean, Int?, Int?> - (didLevelDecrease, oldLevel, newLevel)
     */
    suspend fun checkAndApplyDecay(): Triple<Boolean, Int?, Int?> {
        val progression = getProgression()
        if (progression.lastActivityDate.isEmpty()) {
            return Triple(false, null, null)
        }
        
        val lastDate = try {
            LocalDate.parse(progression.lastActivityDate)
        } catch (e: Exception) {
            LocalDate.now()
        }
        
        val today = LocalDate.now()
        val daysSinceActivity = ChronoUnit.DAYS.between(lastDate, today)
        
        if (daysSinceActivity > 0) {
            val oldLevel = progression.yogaLevel
            val now = System.currentTimeMillis()
            
            // Apply 1% decay per day, max 50% total decay
            val decayAmount = min(50f, daysSinceActivity.toFloat())
            val newPercentage = max(0f, progression.progressionPercentage - decayAmount)
            
            // Check if percentage drop causes level decrease
            if (newPercentage <= 0f && oldLevel > 0) {
                // Level decreased
                val newLevel = oldLevel - 1
                dao.levelUp(newLevel, now)
                // Set percentage to 50% in the lower level (they were making progress)
                dao.updatePercentage(50f, now)
                return Triple(true, oldLevel, newLevel)
            } else {
                dao.updatePercentage(newPercentage, now)
                return Triple(false, null, null)
            }
        }
        
        return Triple(false, null, null)
    }
    
    /**
     * Calculate progression percentage based on performance metrics
     * Formula: (Quiz 40%) + (Reading 30%) + (Streak 30%)
     * @return Pair<Boolean, Int?> - (didLevelUp, newLevel)
     */
    private suspend fun calculateAndUpdateProgression(): Pair<Boolean, Int?> {
        val progression = getProgression()
        
        // Quiz component (40%)
        val quizComponent = (progression.quizAccuracy / 100f) * 40f
        
        // Reading component (30%)
        val readingComponent = (progression.readingConsistency / 100f) * 30f
        
        // Streak component (30%)
        val streakComponent = min(30f, progression.consecutiveDays * 3f)
        
        // Calculate total progression
        var newPercentage = quizComponent + readingComponent + streakComponent
        newPercentage = min(100f, newPercentage)
        
        // Check for level up
        if (newPercentage >= 100f && progression.yogaLevel < 3) {
            val newLevel = progression.yogaLevel + 1
            dao.levelUp(newLevel, System.currentTimeMillis())
            return Pair(true, newLevel)
        } else {
            dao.updatePercentage(newPercentage, System.currentTimeMillis())
            return Pair(false, null)
        }
    }
    
    /**
     * Update progression and return level-up status
     * @return Pair<Boolean, Int?> - (didLevelUp, newLevel)
     */
    suspend fun updateProgressionAndCheckLevelUp(): Pair<Boolean, Int?> {
        return calculateAndUpdateProgression()
    }
    
    /**
     * Get level name for display
     */
    fun getLevelName(level: Int): String {
        return when (level) {
            0 -> "Karma Yoga"
            1 -> "Bhakti Yoga"
            2 -> "Jnana Yoga"
            3 -> "Moksha"
            else -> "Unknown"
        }
    }
}
