package com.aipoweredgita.app.domain.usecase

import android.content.Context
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.domain.model.DailyActivityData
import com.aipoweredgita.app.domain.model.NextActionData
import com.aipoweredgita.app.recommendation.AdaptiveCurriculumPlanner
import com.aipoweredgita.app.recommendation.M1Predictor
import com.aipoweredgita.app.recommendation.RecommendationEngine
import com.aipoweredgita.app.repository.DailyActivityRepository
import com.aipoweredgita.app.repository.QuizStatsRepository
import com.aipoweredgita.app.repository.ReadingRepository
import com.aipoweredgita.app.util.StringUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case to load dashboard data for the Profile screen
 * Encapsulates the complex business logic of loading dashboard data
 */
class LoadDashboardUseCase @Inject constructor(
    private val readingRepository: ReadingRepository,
    private val dailyActivityRepository: DailyActivityRepository,
    private val quizStatsRepository: QuizStatsRepository,
    private val database: GitaDatabase
) {
    data class DashboardResult(
        val dailyActivity: DailyActivityData,
        val nextAction: NextActionData
    )

    suspend operator fun invoke(context: Context): Result<DashboardResult> {
        return try {
            val today = java.time.LocalDate.now().toString()
            val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

            // 1. Load today's verses
            val dailyActivity = try {
                val (vt, vlist) = withContext(Dispatchers.IO) {
                    val vtCount = readingRepository.totalReadToday(today)
                    val vlistData = readingRepository.getByDate(today)
                    vtCount to vlistData
                }
                DailyActivityData(versesToday = vt, versesListToday = vlist)
            } catch (e: Exception) {
                DailyActivityData()
            }

            // 2. M1 next step predictor
            val nextAction = try {
                val lastSugDate = prefs.getString("next_suggestion_date", "")
                if (lastSugDate != today) {
                    val suggestion = withContext(Dispatchers.IO) {
                        M1Predictor(
                            dailyActivityDao = database.dailyActivityDao(),
                            userStatsDao = database.userStatsDao()
                        ).predictNext()
                    }
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
            } catch (e: Exception) {
                NextActionData()
            }

            // 3. Daily activity + quiz attempts
            val updatedDailyActivity = try {
                val row = withContext(Dispatchers.IO) { dailyActivityRepository.getByDate(today) }
                val quizCount = withContext(Dispatchers.IO) {
                    quizStatsRepository.getAttemptsByDate(today).first().size
                }
                row?.let { dailyRow ->
                    dailyActivity.copy(
                        normalToday = dailyRow.normalSeconds,
                        quizToday = dailyRow.quizSeconds,
                        studioToday = dailyRow.voiceStudioTimeSeconds,
                        quizzesToday = quizCount
                    )
                } ?: dailyActivity
            } catch (e: Exception) {
                dailyActivity
            }

            // 4. Generate recommendations once per day
            try {
                val lastRun = prefs.getString("last_rec_gen", "")
                if (lastRun != today) {
                    withContext(Dispatchers.IO) {
                        RecommendationEngine(context).generateRecommendations()
                        AdaptiveCurriculumPlanner(
                            userStatsDao = database.userStatsDao(),
                            userPreferencesDao = database.userPreferencesDao(),
                            recommendationDataDao = database.recommendationDataDao()
                        ).buildPlan()
                    }
                    prefs.edit().putString("last_rec_gen", today).apply()
                }
            } catch (e: Exception) {
                // Non-critical, continue
            }

            Result.success(DashboardResult(updatedDailyActivity, nextAction))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
