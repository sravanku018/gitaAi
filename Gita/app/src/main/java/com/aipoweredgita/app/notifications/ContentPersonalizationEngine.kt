package com.aipoweredgita.app.notifications

import android.content.Context
import com.aipoweredgita.app.database.GitaDatabase
import com.aipoweredgita.app.database.UserStats
import com.aipoweredgita.app.data.LearningSegment
import com.aipoweredgita.app.data.SegmentReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

/**
 * ML-powered content personalization engine
 * Selects and personalizes notification content based on user's learning profile
 */
data class PersonalizedContent(
    val title: String,
    val body: String,
    val bigText: String? = null,
    val suggestedAction: String? = null,
    val chapter: Int? = null,
    val verse: Int? = null
)

data class UserLearningProfile(
    val accuracy: Float,
    val preferredSegments: List<LearningSegment>,
    val weakAreas: List<LearningSegment>,
    val strongAreas: List<LearningSegment>,
    val currentLevel: Int,
    val streakDays: Int,
    val totalVersesRead: Int,
    val completionRate: Float
)

class ContentPersonalizationEngine(private val context: Context) {

    /**
     * Generates personalized notification content
     */
    suspend fun generatePersonalizedContent(
        type: NotificationType,
        userStats: UserStats
    ): PersonalizedContent = withContext(Dispatchers.IO) {
        val profile = buildUserProfile(userStats)

        return@withContext when (type) {
            NotificationType.DAILY_REMINDER -> generateDailyReminder(profile)
            NotificationType.STREAK_ALERT -> generateStreakAlert(profile)
            NotificationType.ACHIEVEMENT -> generateAchievement(profile)
            NotificationType.RECOMMENDATION -> generateRecommendation(profile)
            NotificationType.MILESTONE -> generateMilestone(profile)
            NotificationType.WEAK_AREA_FOCUS -> generateWeakAreaFocus(profile)
            NotificationType.STREAK_RECOVERY -> generateStreakRecovery(profile)
            NotificationType.QUIZ_READY -> generateQuizReady(profile)
            NotificationType.CHAPTER_COMPLETE -> generateChapterComplete(profile)
        }
    }

    /**
     * Builds user's learning profile from stats and history
     */
    private suspend fun buildUserProfile(userStats: UserStats): UserLearningProfile {
        // Analyze segments (simplified - in real app, use learning reports)
        val segments = LearningSegment.values()
        val accuracy = userStats.accuracyPercentage

        // Determine weak and strong areas (mock logic - replace with actual analysis)
        val preferredSegments = segments.take(2)
        val weakAreas = if (accuracy < 70f) segments.take(1) else emptyList()
        val strongAreas = if (accuracy > 85f) segments.take(2) else emptyList()

        val currentLevel = calculateUserLevel(userStats)
        val completionRate = calculateCompletionRate(userStats)

        return UserLearningProfile(
            accuracy = accuracy,
            preferredSegments = preferredSegments,
            weakAreas = weakAreas,
            strongAreas = strongAreas,
            currentLevel = currentLevel,
            streakDays = userStats.currentStreak,
            totalVersesRead = userStats.distinctVersesRead,
            completionRate = completionRate
        )
    }

    /**
     * Generates daily reminder notification
     */
    private fun generateDailyReminder(profile: UserLearningProfile): PersonalizedContent {
        val motivationalPhrases = listOf(
            "Begin your day with divine wisdom",
            "Your spiritual journey awaits",
            "Today's verse holds your answer",
            "Inner peace begins with one verse",
            "Feed your soul with ancient wisdom"
        )

        val selectedPhrase = motivationalPhrases.random()

        val encouragement = when {
            profile.streakDays > 0 -> "Keep your ${profile.streakDays}-day streak alive!"
            profile.accuracy > 80f -> "You're on fire! 🔥 Your accuracy is ${profile.accuracy.toInt()}%"
            profile.totalVersesRead > 50 -> "You're becoming a Gita master!"
            else -> "Every master was once a beginner"
        }

        val weakAreaHint = profile.weakAreas.firstOrNull()?.let {
            "\n\nFocus today: ${it.displayName}"
        } ?: ""

        return PersonalizedContent(
            title = "Daily Gita Verse",
            body = "$selectedPhrase ✨\n\n$encouragement",
            bigText = "$selectedPhrase\n\n$encouragement$weakAreaHint",
            suggestedAction = "Read Today's Verse"
        )
    }

    /**
     * Generates streak alert notification
     */
    private fun generateStreakAlert(profile: UserLearningProfile): PersonalizedContent {
        return when (profile.streakDays) {
            0 -> PersonalizedContent(
                title = "Start Your Journey",
                body = "Your spiritual streak begins today! 🌟",
                bigText = "Every great journey begins with a single step.\n\nStart your daily practice today and build an unbreakable habit of wisdom.",
                suggestedAction = "Start Reading"
            )
            1 -> PersonalizedContent(
                title = "Streak at Risk!",
                body = "Don't lose your 1-day streak! 📱",
                bigText = "You're just one verse away from building a powerful habit.\n\nToday's commitment shapes your tomorrow.",
                suggestedAction = "Save Streak"
            )
            7 -> PersonalizedContent(
                title = "Week-long Streak! 🎉",
                body = "Amazing! 7 days of consistent practice",
                bigText = "You've built a weekly rhythm of wisdom.\n\nThis is the foundation of lasting change. Keep going!",
                suggestedAction = "Continue Streak"
            )
            30 -> PersonalizedContent(
                title = "Monthly Mastery! 🏆",
                body = "30 days of dedication - incredible!",
                bigText = "A month of consistent spiritual practice!\n\nYou've developed one of the most powerful habits - daily wisdom.",
                suggestedAction = "View Progress"
            )
            else -> PersonalizedContent(
                title = "Keep Your Streak! 💪",
                body = "Your ${profile.streakDays}-day streak needs you!",
                bigText = "Every day you practice, you're building mental resilience.\n\nDon't let this streak break - you're stronger than you think!",
                suggestedAction = "Continue Reading"
            )
        }
    }

    /**
     * Generates achievement notification
     */
    private fun generateAchievement(profile: UserLearningProfile): PersonalizedContent {
        val achievements = listOf(
            "Quiz Master" to "You've mastered ${profile.totalVersesRead} verses!",
            "Accuracy Expert" to "${profile.accuracy.toInt()}% accuracy - outstanding!",
            "Consistent Learner" to "${profile.streakDays} days of dedication",
            "Wisdom Seeker" to "Your dedication inspires others"
        )

        val (title, description) = achievements.random()

        return PersonalizedContent(
            title = "Achievement Unlocked! $title",
            body = description,
            bigText = "$title\n\n$description\n\nYou've earned this recognition through consistent practice and dedication.",
            suggestedAction = "Share Achievement"
        )
    }

    /**
     * Generates recommendation notification
     */
    private fun generateRecommendation(profile: UserLearningProfile): PersonalizedContent {
        val recommendations = if (profile.accuracy < 70f) {
            val weakArea = profile.weakAreas.firstOrNull() ?: LearningSegment.KARMA_YOGA
            "Based on your learning pattern, focus on ${weakArea.displayName} today. Review previous verses to strengthen your foundation."
        } else if (profile.accuracy > 90f) {
            val strongArea = profile.strongAreas.firstOrNull() ?: LearningSegment.BHAKTI_YOGA
            "You're excelling in ${strongArea.displayName}! Ready for a challenge?"
        } else {
            "Your consistent progress shows! Try exploring new chapters today."
        }

        return PersonalizedContent(
            title = "Personalized Recommendation",
            body = recommendations.take(50) + "...",
            bigText = recommendations,
            suggestedAction = "View Recommendation"
        )
    }

    /**
     * Generates milestone notification
     */
    private fun generateMilestone(profile: UserLearningProfile): PersonalizedContent {
        val milestones = listOf(
            10 to "First 10 verses - the beginning of wisdom! 📖",
            50 to "50 verses - quarter of the Gita mastered! 🎯",
            100 to "100 verses - extraordinary dedication! ⭐",
            200 to "200 verses - you've read half the Gita! 🌟",
            400 to "400 verses - you're a true devotee! 🙏",
            700 to "700 verses - completion achievement! 🏆"
        )

        val (verseCount, message) = milestones.find { profile.totalVersesRead >= it.first }
            ?: (profile.totalVersesRead to "You're making steady progress! Keep going!")

        return PersonalizedContent(
            title = "Milestone: $verseCount Verses!",
            body = message,
            bigText = "$message\n\nEach verse you read is a step toward enlightenment.\nYour dedication is truly inspiring.",
            suggestedAction = "Celebrate"
        )
    }

    /**
     * Generates weak area focus notification
     */
    private fun generateWeakAreaFocus(profile: UserLearningProfile): PersonalizedContent {
        val weakArea = profile.weakAreas.firstOrNull() ?: LearningSegment.KARMA_YOGA

        val messages = mapOf<LearningSegment, String>(
            LearningSegment.KARMA_YOGA to "Focus on Action Without Attachment. Practice selfless service today.",
            LearningSegment.BHAKTI_YOGA to "Deepen your Devotion. Let love guide your spiritual path.",
            LearningSegment.JNANA_YOGA to "Seek Knowledge. Question, learn, and grow in wisdom.",
            LearningSegment.DHYANA_YOGA to "Practice Meditation. Still your mind, find inner peace.",
            LearningSegment.MOKSHA_YOGA to "Embrace Liberation. Seek freedom from material attachments.",
            LearningSegment.WARRIOR_CODE to "Embrace Your Duty. Stand firm in righteousness.",
            LearningSegment.DIVINE_NATURE to "Recognize Your True Self. Awaken to your divine essence.",
            LearningSegment.SURRENDER to "Practice Devotional Surrender. Trust in the Divine plan."
        )

        val message = messages[weakArea] ?: "Focus on your areas of growth today."

        return PersonalizedContent(
            title = "Focus Area: ${weakArea.displayName}",
            body = message,
            bigText = "${weakArea.displayName}\n\n$message\n\nReview related verses to strengthen your understanding.",
            suggestedAction = "Start Practice"
        )
    }

    /**
     * Generates streak recovery notification
     */
    private fun generateStreakRecovery(profile: UserLearningProfile): PersonalizedContent {
        return PersonalizedContent(
            title = "Start Fresh ✨",
            body = "Every day is a new beginning. Your wisdom journey continues!",
            bigText = "Don't let a missed day discourage you.\n\nEven the mightiest rivers face obstacles, but they never stop flowing.\n\nYour journey back to daily practice starts with one verse.",
            suggestedAction = "Resume Reading"
        )
    }

    /**
     * Generates quiz ready notification
     */
    private fun generateQuizReady(profile: UserLearningProfile): PersonalizedContent {
        val difficulty = when {
            profile.accuracy > 85f -> "challenging"
            profile.accuracy > 70f -> "moderate"
            else -> "gentle"
        }

        return PersonalizedContent(
            title = "Quiz Ready! 🎯",
            body = "Test your knowledge with a $difficulty quiz",
            bigText = "Ready to see how much you've learned?\n\nThis $difficulty quiz will help reinforce today's verses.",
            suggestedAction = "Start Quiz"
        )
    }

    /**
     * Generates chapter complete notification
     */
    private fun generateChapterComplete(profile: UserLearningProfile): PersonalizedContent {
        return PersonalizedContent(
            title = "Chapter Complete! 📚",
            body = "You've mastered another chapter of wisdom",
            bigText = "Congratulations on completing this chapter!\n\nEach chapter of the Gita offers unique insights.\nReview what you've learned before moving forward.",
            suggestedAction = "Review Chapter"
        )
    }

    /**
     * Calculates user level based on activity
     */
    private fun calculateUserLevel(userStats: UserStats): Int {
        val totalActivity = userStats.versesRead + userStats.totalQuizzesTaken
        return (totalActivity / 50) + 1 // Level up every 50 activities
    }

    /**
     * Calculates completion rate
     */
    private fun calculateCompletionRate(userStats: UserStats): Float {
        val totalChapters = 18
        val completionPercentage = (userStats.chaptersCompleted / totalChapters.toFloat()) * 100
        return completionPercentage.coerceIn(0f, 100f)
    }
}
