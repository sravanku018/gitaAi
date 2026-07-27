package com.aipoweredgita.app.recommendation

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.RecommendationData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class RecommendationEngine(private val context: Context) {
    private val TAG = "RecommendationEngine"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateRecommendations() = withContext(Dispatchers.IO) {
        try {
            val db = GitaDatabase.getDatabase(context)
            val stats = db.userStatsDao().getUserStatsOnce()
            val prefs = db.userPreferencesDao().getPreferencesSync(1)

            val weakTopics = mutableListOf<String>()
            val perfDao = db.questionPerformanceDao()

            listOf("karma", "bhakti", "gyana", "dharma", "yoga").forEach { topic ->
                val items = try { perfDao.getWeakTopics(topic) } catch (_: Exception) { null }
                if (items == null) {
                    if (prefs?.showOnlyWeakAreas == true) weakTopics.add(topic)
                }
            }

            val recDao = db.recommendationDataDao()
            try { recDao.deleteByStatus("pending") } catch (_: Exception) {}

            if (stats != null) {
                // Try Groq/NVIDIA cloud first
                try {
                    val cloudRecs = generateRecommendationsViaCloud(stats, prefs)
                    if (cloudRecs.isNotEmpty()) {
                        cloudRecs.forEach { rec -> recDao.insert(rec) }
                        Log.d(TAG, "Generated ${cloudRecs.size} recommendations via cloud AI.")
                        return@withContext
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Cloud recommendation failed, using rule-based fallback: ${e.message}")
                }
            }

            // Rule-based fallback (offline)
            generateRuleBasedRecommendations(recDao, stats, prefs, weakTopics)

        } catch (e: Exception) {
            Log.e(TAG, "Error generating recommendations: ${e.message}")
        }
    }

    private suspend fun generateRecommendationsViaCloud(
        stats: com.aipoweredgita.app.database.UserStats,
        prefs: com.aipoweredgita.app.database.UserPreferences?
    ): List<RecommendationData> = withContext(Dispatchers.IO) {
        val prompt = """
            A user is studying the Bhagavad Gita. Here are their stats:
            - Verses read: ${stats.versesRead}
            - Chapters completed: ${stats.chaptersCompleted}
            - Quiz accuracy: ${stats.accuracyPercentage.toInt()}%
            - Total quizzes taken: ${stats.totalQuizzesTaken}
            - Preferred study mode: ${prefs?.preferredStudyMode ?: "quiz"}
            - Study streak: ${stats.currentStreak} days
            
            Generate 3 personalized study recommendations in JSON array format:
            [{"type":"study_mode","id":"quiz","title":"...","reason":"...","priority":8},...]
            Types: study_mode, chapter, topic, yogalevel. Respond ONLY with valid JSON array.
        """.trimIndent()

        val messagesArray = JSONArray().apply {
            put(JSONObject().put("role", "user").put("content", prompt))
        }
        val body = JSONObject()
            .put("messages", messagesArray)
            .put("app", "gita")
            .put("provider", getCloudProvider())
            .toString()
            .toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url(com.aipoweredgita.app.util.GitaConstants.VOICE_PROXY_URL)
            .post(body)
            .build()

        val response = okHttpClient.newCall(request).execute()
        if (!response.isSuccessful) return@withContext emptyList()

        val reply = JSONObject(response.body?.string() ?: "{}").optString("reply", "[]")
        val jsonArray = JSONArray(reply)

        (0 until jsonArray.length()).map { i ->
            val obj = jsonArray.getJSONObject(i)
            RecommendationData(
                recommendationType = obj.optString("type", "study_mode"),
                recommendationId = obj.optString("id", "quiz"),
                recommendationTitle = obj.optString("title", "Continue Studying"),
                priority = obj.optInt("priority", 7),
                confidenceScore = 85f,
                relevanceScore = 90f,
                reason = obj.optString("reason", "Personalized by AI"),
                baseReason = "cloud_ai",
                expectedBenefit = 80f,
                urgencyLevel = "medium"
            )
        }
    }

    private fun getCloudProvider(): String {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val model = prefs.getString("selected_ai_model", "Groq (Cloud)") ?: "Groq (Cloud)"
        return if (model.contains("NVIDIA", ignoreCase = true)) "nvidia" else "groq"
    }

    private suspend fun generateRuleBasedRecommendations(
        recDao: com.aipoweredgita.app.database.RecommendationDataDao,
        stats: com.aipoweredgita.app.database.UserStats?,
        prefs: com.aipoweredgita.app.database.UserPreferences?,
        weakTopics: List<String>
    ) {
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

        val yogaLevelInfo = com.aipoweredgita.app.ui.components.YogaLevelManager.yogaLevelInfo(stats)
        recDao.insert(
            RecommendationData(
                recommendationType = "yogalevel",
                recommendationId = yogaLevelInfo.level.toString(),
                recommendationTitle = "Focus on Yoga Level ${yogaLevelInfo.level}",
                priority = 9,
                confidenceScore = 75f,
                relevanceScore = 80f,
                reason = "Progress towards next yoga level",
                baseReason = "progression",
                expectedBenefit = 80f,
                urgencyLevel = "high"
            )
        )

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

        Log.d(TAG, "Rule-based recommendations generated")
    }
}
