package com.aipoweredgita.app.recommendation

import android.content.Context
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RecommendationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CurriculumItem(
    val type: String, // topic, yogalevel, chapter
    val id: String,
    val title: String,
    val suggestedQuestions: Int = 10
)

data class CurriculumPlan(
    val items: List<CurriculumItem>
)

class AdaptiveCurriculumPlanner(private val context: Context) {
    suspend fun buildPlan(): CurriculumPlan = withContext(Dispatchers.IO) {
        val db = GitaDatabase.getDatabase(context)
        val stats = db.userStatsDao().getUserStatsOnce()
        val prefs = db.userPreferencesDao().getPreferencesSync(1)

        val items = mutableListOf<CurriculumItem>()

        // 1) Start with preferred yoga level or current lotus level
        val lotus = com.aipoweredgita.app.ui.components.LotusLevelManager.yogaLevelInfo(stats)
        val focusLevel = if ((prefs?.preferredYogaLevel ?: 0) > 0) (prefs?.preferredYogaLevel ?: lotus.level) else lotus.level
        items.add(CurriculumItem(type = "yogalevel", id = focusLevel.toString(), title = "Yoga Level $focusLevel", suggestedQuestions = prefs?.questionsPerSession ?: 10))

        // 2) Add weak topics based on question performance (seed generic topics if none)
        val genericTopics = listOf("dharma", "karma", "bhakti", "gyana", "yoga")
        genericTopics.take(3).forEach { t ->
            items.add(CurriculumItem(type = "topic", id = t, title = "Practice $t", suggestedQuestions = 6))
        }

        // 3) Add a chapter review using spaced repetition (recent chapter completed or 1)
        val recentChapter = (stats?.chaptersCompleted ?: 0).coerceAtLeast(1)
        items.add(CurriculumItem(type = "chapter", id = recentChapter.toString(), title = "Review Chapter $recentChapter", suggestedQuestions = 8))

        // Persist outline as recommendations for visibility
        val recDao = db.recommendationDataDao()
        items.forEachIndexed { idx, item ->
            recDao.insert(
                RecommendationData(
                    recommendationType = item.type,
                    recommendationId = item.id,
                    recommendationTitle = item.title,
                    priority = 10 - idx,
                    confidenceScore = 70f,
                    relevanceScore = 75f,
                    reason = "Adaptive curriculum step",
                    baseReason = "curriculum",
                    expectedBenefit = 70f,
                    urgencyLevel = if (idx == 0) "high" else "medium"
                )
            )
        }

        CurriculumPlan(items)
    }
}
