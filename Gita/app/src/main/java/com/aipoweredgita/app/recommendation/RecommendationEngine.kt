package com.aipoweredgita.app.recommendation

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RecommendationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class RecommendationEngine(private val context: Context) {
    private val TAG = "RecommendationEngine"

    suspend fun generateRecommendations() = withContext(Dispatchers.IO) {
        try {
            val db = GitaDatabase.getDatabase(context)
            val stats = db.userStatsDao().getUserStatsOnce()
            val prefs = db.userPreferencesDao().getPreferencesSync(1)

            // Basic heuristics from question performance
            val weakTopics = mutableListOf<String>()
            val perfDao = db.questionPerformanceDao()

            // Collect a few weak areas by topic (requires app to have logged performance)
            listOf("karma", "bhakti", "gyana", "dharma", "yoga").forEach { topic ->
                val items = try { perfDao.getWeakTopics(topic) } catch (_: Exception) { null }
                // Flow is not available off main; skip here; rely on saved insights later
                if (items == null) {
                    // Seed topics by preference or generic
                    if (prefs?.showOnlyWeakAreas == true) weakTopics.add(topic)
                }
            }

            val recDao = db.recommendationDataDao()

            // Recommend practice session in preferred study mode
            recDao.insert(
                RecommendationData(
                    recommendationType = "study_mode",
                    recommendationId = prefs?.preferredStudyMode ?: "quiz",
                    recommendationTitle = "Continue in ${prefs?.preferredStudyMode ?: "Quiz"} Mode",
                    priority = 8,
                    confidenceScore = 80f,
                    relevanceScore = 85f,
                    reason = "Matches your preferred study mode and recent activity",
                    baseReason = "preference",
                    expectedBenefit = 70f,
                    urgencyLevel = "medium"
                )
            )

            // Recommend chapter or verses based on favorites or recent reads
            val recentChapter = (stats?.chaptersCompleted ?: 0).coerceAtLeast(1)
            recDao.insert(
                RecommendationData(
                    recommendationType = "chapter",
                    recommendationId = recentChapter.toString(),
                    recommendationTitle = "Review Chapter $recentChapter",
                    priority = 7,
                    confidenceScore = 60f,
                    relevanceScore = 75f,
                    reason = "Spaced repetition to strengthen memory",
                    baseReason = "review",
                    expectedBenefit = 65f,
                    urgencyLevel = "low"
                )
            )

            // Recommend yoga level focus based on Lotus level
            val yogaLevelInfo = com.aipoweredgita.app.ui.components.LotusLevelManager.yogaLevelInfo(stats)
            recDao.insert(
                RecommendationData(
                    recommendationType = "yogalevel",
                    recommendationId = yogaLevelInfo.level.toString(),
                    recommendationTitle = "Focus on Yoga Level ${yogaLevelInfo.level}",
                    priority = 9,
                    confidenceScore = 75f,
                    relevanceScore = 80f,
                    reason = "Progress towards next lotus level",
                    baseReason = "progression",
                    expectedBenefit = 80f,
                    urgencyLevel = "high"
                )
            )

            // Weak topics recommendations (seeded)
            weakTopics.take(3).forEach { topic ->
                recDao.insert(
                    RecommendationData(
                        recommendationType = "topic",
                        recommendationId = topic,
                        recommendationTitle = "Strengthen $topic",
                        priority = 6,
                        confidenceScore = 55f,
                        relevanceScore = 70f,
                        reason = "Improve success rate in $topic",
                        baseReason = "weak_area",
                        expectedBenefit = 60f,
                        urgencyLevel = "medium"
                    )
                )
            }

            Log.d(TAG, "Recommendations generated")
        } catch (e: Exception) {
            Log.e(TAG, "Error generating recommendations: ${e.message}")
        }
    }
}

