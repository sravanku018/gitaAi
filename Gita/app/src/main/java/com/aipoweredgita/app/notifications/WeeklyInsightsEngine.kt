package com.aipoweredgita.app.notifications

import android.content.Context
import android.util.Log
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.util.GitaConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

data class WeeklyInsight(
    val summary: String,
    val focusArea: String,
    val accuracyChange: Float,
    val krishnaMessage: String
)

object WeeklyInsightsEngine {

    private const val TAG = "WeeklyInsightsEngine"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun generateWeeklyInsight(context: Context): WeeklyInsight? = withContext(Dispatchers.IO) {
        try {
            val db = GitaDatabase.getDatabase(context)
            val statsDao = db.userStatsDao()
            val activityDao = db.dailyActivityDao()

            val stats = statsDao.getUserStatsOnce() ?: return@withContext null
            val endDate = LocalDate.now()
            val startDate = endDate.minusDays(7)
            val dates = (0..6).map { startDate.plusDays(it.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE) }
            val activities = activityDao.getByDates(dates)

            if (activities.isEmpty()) return@withContext null

            val totalVerses = activities.sumOf { a: DailyActivity -> a.versesRead }
            val totalQuizTime = activities.sumOf { a: DailyActivity -> a.quizSeconds }
            val totalNormalTime = activities.sumOf { a: DailyActivity -> a.normalSeconds }
            val activeDays = activities.count { a: DailyActivity -> a.versesRead > 0 || a.quizSeconds > 0 }

            val prompt = buildString {
                appendLine("You are a wise Gita teacher. Generate a brief weekly learning insight.")
                appendLine()
                appendLine("Student stats this week:")
                appendLine("- Verses read: $totalVerses")
                appendLine("- Quiz time: ${totalQuizTime / 60} minutes")
                appendLine("- Reading time: ${totalNormalTime / 60} minutes")
                appendLine("- Active days: $activeDays / 7")
                appendLine("- Current streak: ${stats.currentStreak} days")
                appendLine("- Total accuracy: ${stats.accuracyPercentage}%")
                appendLine()
                appendLine("Respond in JSON format:")
                appendLine("""{"summary":"2-3 sentence summary","focusArea":"main topic studied","accuracyChange":0.0,"krishnaMessage":"1 line Krishna teaching"}""")
            }

            val response = callProxy(prompt) ?: return@withContext null
            parseInsight(response)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate weekly insight", e)
            null
        }
    }

    private fun callProxy(prompt: String): String? {
        return try {
            val messagesArray = JSONArray()
            messagesArray.put(
                JSONObject()
                    .put("role", "system")
                    .put("content", "You are a wise Gita teacher. Respond only with valid JSON, no extra text.")
            )
            messagesArray.put(
                JSONObject()
                    .put("role", "user")
                    .put("content", prompt)
            )

            val body = JSONObject()
                .put("messages", messagesArray)
                .put("app", "gita")
                .put("provider", "groq")
                .toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(GitaConstants.VOICE_PROXY_URL)
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Proxy error: ${response.code}")
                    return null
                }
                val responseString = response.body?.string() ?: return null
                JSONObject(responseString).getString("reply")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call proxy for weekly insight", e)
            null
        }
    }

    private fun parseInsight(response: String): WeeklyInsight? {
        return try {
            val jsonStr = response.let { s ->
                val start = s.indexOf("{")
                val end = s.lastIndexOf("}") + 1
                if (start >= 0 && end > start) s.substring(start, end) else s
            }
            val json = JSONObject(jsonStr)
            WeeklyInsight(
                summary = json.optString("summary", "Keep up your Gita practice!"),
                focusArea = json.optString("focusArea", "General Study"),
                accuracyChange = json.optDouble("accuracyChange", 0.0).toFloat(),
                krishnaMessage = json.optString("krishnaMessage", "You have the right to work, but never to the fruit of work.")
            )
        } catch (e: Exception) {
            WeeklyInsight(
                summary = "Keep practicing the Gita daily!",
                focusArea = "General Study",
                accuracyChange = 0f,
                krishnaMessage = "You have the right to work, but never to the fruit of work."
            )
        }
    }
}
