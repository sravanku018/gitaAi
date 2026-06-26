package com.aipoweredgita.app.notifications

import android.content.Context
import com.aipoweredgita.app.database.DailyActivity
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class WeeklyInsight(
    val summary: String,
    val focusArea: String,
    val accuracyChange: Float,
    val krishnaMessage: String
)

object WeeklyInsightsEngine {

    private const val GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions"

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

            val response = callGroq(prompt) ?: return@withContext null
            parseInsight(response)
        } catch (e: Exception) {
            null
        }
    }

    private fun callGroq(prompt: String): String? {
        return try {
            val apiKey = "gsk_placeholder" // Replace with actual Groq API key
            val url = URL(GROQ_API_URL)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            val body = JSONObject().apply {
                put("model", "llama-3.1-8b-instant")
                put("messages", listOf(
                    JSONObject().put("role", "user").put("content", prompt)
                ))
                put("temperature", 0.7)
                put("max_tokens", 300)
            }

            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            if (conn.responseCode == 200) {
                val json = JSONObject(conn.inputStream.bufferedReader().readText())
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            } else null
        } catch (e: Exception) {
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
