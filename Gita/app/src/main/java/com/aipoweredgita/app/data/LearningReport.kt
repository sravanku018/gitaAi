package com.aipoweredgita.app.data

import com.aipoweredgita.app.data.LearningSegment
import java.util.Date

data class LearningReport(
    val reportId: String,
    val period: ReportPeriod,
    val startDate: Long,
    val endDate: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val summary: ReportSummary,
    val segmentProgress: List<SegmentReport>,
    val quizPerformance: QuizPerformanceReport,
    val achievements: List<String>,
    val insights: List<String>,
    val recommendations: List<String>
)

enum class ReportPeriod {
    WEEKLY,
    MONTHLY,
    YEARLY
}

data class ReportSummary(
    val totalVersesRead: Int,
    val totalQuizzesCompleted: Int,
    val totalQuestionsAnswered: Int,
    val totalTimeSpentMinutes: Int,
    val averageAccuracy: Float,
    val streakDays: Int,
    val levelProgress: Int, // How many steps progressed
    val xpEarned: Int,
    val badgesEarned: Int
)

data class SegmentReport(
    val segment: LearningSegment,
    val questionsAttempted: Int,
    val correctAnswers: Int,
    val accuracy: Float,
    val timeSpentMinutes: Int,
    val improvement: Float, // percentage change from previous period
    val focusLevel: Int // 1-10, how much user focused on this segment
)

data class QuizPerformanceReport(
    val totalQuizzes: Int,
    val averageScore: Float,
    val bestScore: Int,
    val worstScore: Int,
    val averageTimePerQuestion: Float,
    val questionTypeBreakdown: Map<String, Int>, // MCQ, Essay, etc.
    val difficultyBreakdown: Map<Int, Int> // 1-10 difficulty levels
)

data class WeeklyReport(
    val weekNumber: Int,
    val year: Int,
    val dailyActivity: Map<String, Int>, // date -> activity count
    val topStrength: LearningSegment,
    val topWeakness: LearningSegment,
    val recommendedFocus: LearningSegment
)

data class MonthlyReport(
    val month: Int,
    val year: Int,
    val weeklySummaries: List<WeeklyReport>,
    val totalGrowth: Float, // overall improvement percentage
    val consistencyScore: Float, // how consistent the user was
    val learningVelocity: Float // how fast they're progressing
)
