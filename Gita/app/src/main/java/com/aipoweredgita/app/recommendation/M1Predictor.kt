package com.aipoweredgita.app.recommendation

import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.DailyActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

data class NextSuggestion(
    val nextStep: String, // "Read", "Studio", or "Quiz"
    val nextLevel: Int,   // 1..10 difficulty suggestion
    val reason: String
)

suspend fun predictNext(db: GitaDatabase): NextSuggestion {
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val today = LocalDate.now().format(dateFormatter)
    val dailyDao = db.dailyActivityDao()

    // Gather last 7 days activity
    val cal = Calendar.getInstance()
    val last7 = mutableListOf<DailyActivity?>()
    repeat(7) {
        val d = LocalDate.now().minusDays(it.toLong()).format(dateFormatter)
        val row = try { dailyDao.getByDate(d) } catch (_: Exception) { null }
        last7.add(row)
    }

    val totals = last7.map { r ->
        Triple(r?.normalSeconds ?: 0L, r?.quizSeconds ?: 0L, r?.voiceStudioTimeSeconds ?: 0L)
    }
    val totalNormal = totals.sumOf { it.first }
    val totalQuiz = totals.sumOf { it.second }
    val totalStudio = totals.sumOf { it.third }

    // Choose next step: prioritize the least-engaged mode to balance learning
    val minMode = listOf(
        "Read" to totalNormal,
        "Quiz" to totalQuiz,
        "Studio" to totalStudio
    ).minByOrNull { it.second }?.first ?: "Read"

    // Estimate streak (consecutive days with any activity)
    var streak = 0
    var dayOffset = 0L
    while (true) {
        val d = LocalDate.now().minusDays(dayOffset).format(dateFormatter)
        val row = try { dailyDao.getByDate(d) } catch (_: Exception) { null }
        val sum = ((row?.normalSeconds ?: 0L) + (row?.quizSeconds ?: 0L) + (row?.voiceStudioTimeSeconds ?: 0L))
        if (sum > 0L) {
            streak++
            dayOffset++
        } else break
    }

    // Difficulty heuristic based on streak and recent activity
    val recentActivity = totals.take(3).sumOf { it.first + it.second + it.third }
    val level = when {
        streak >= 10 || recentActivity >= 3600L -> 8
        streak >= 5 || recentActivity >= 1800L -> 6
        streak >= 2 || recentActivity >= 600L -> 4
        else -> 3
    }

    val readMin = (totalNormal / 60)
    val quizMin = (totalQuiz / 60)
    val studioMin = (totalStudio / 60)
    val reason = "Balancing modes: Read ${readMin}m, Quiz ${quizMin}m, Studio ${studioMin}m. Streak: ${streak} day" + if (streak == 1) "" else "s"
    return NextSuggestion(nextStep = minMode, nextLevel = level, reason = reason)
}
