package com.aipoweredgita.app.ml

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aipoweredgita.app.data.SegmentWeightageSystem
import com.aipoweredgita.app.data.LearningSegment
import java.util.Calendar

data class UserBadge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val level: Int,
    val unlockedAt: String,
    val category: BadgeCategory
)

enum class BadgeCategory {
    KNOWLEDGE, DEDICATION, ACHIEVEMENT, WISDOM, SPIRITUAL
}

data class UserLevel(
    val level: Int,
    val title: String,
    val description: String,
    val icon: String,
    val experiencePoints: Int,
    val nextLevelXP: Int,
    val category: String
)

class AIBadgeSystem {

    // Get AI-decided badges based on user stats
    suspend fun generateBadges(
        versesRead: Int,
        quizzesTaken: Int,
        score: Int,
        totalQuestions: Int,
        timeSpent: Long,
        currentStreak: Int,
        favoriteCount: Int
    ): List<UserBadge> {
        return withContext(Dispatchers.Default) {
            val badges = mutableListOf<UserBadge>()

            // Knowledge Seeker - Read X verses
            if (versesRead >= 10) {
                badges.add(UserBadge(
                    id = "knowledge_seeker",
                    name = "Knowledge Seeker",
                    description = "Read 10+ verses from the Gita",
                    icon = "📖",
                    level = (versesRead / 10).coerceAtMost(5),
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.KNOWLEDGE
                ))
            }

            // Quiz Master - Take X quizzes
            if (quizzesTaken >= 5) {
                val quizLevel = when {
                    quizzesTaken >= 100 -> 5
                    quizzesTaken >= 50 -> 4
                    quizzesTaken >= 20 -> 3
                    quizzesTaken >= 10 -> 2
                    else -> 1
                }
                badges.add(UserBadge(
                    id = "quiz_master",
                    name = "Quiz Master",
                    description = "Complete $quizzesTaken quizzes",
                    icon = "🎯",
                    level = quizLevel,
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.ACHIEVEMENT
                ))
            }

            // Brilliant Mind - High quiz score
            if (totalQuestions > 0) {
                val accuracy = (score * 100) / totalQuestions
                if (accuracy >= 80) {
                    val brilliantLevel = when {
                        accuracy >= 95 -> 5
                        accuracy >= 90 -> 4
                        accuracy >= 85 -> 3
                        else -> 2
                    }
                    badges.add(UserBadge(
                        id = "brilliant_mind",
                        name = "Brilliant Mind",
                        description = "$accuracy% Quiz Accuracy",
                        icon = "🧠",
                        level = brilliantLevel,
                        unlockedAt = getCurrentDate(),
                        category = BadgeCategory.WISDOM
                    ))
                }
            }

            // Dedicated Learner - Consistent study
            if (currentStreak >= 7) {
                val streakLevel = when {
                    currentStreak >= 100 -> 5
                    currentStreak >= 50 -> 4
                    currentStreak >= 30 -> 3
                    currentStreak >= 14 -> 2
                    else -> 1
                }
                badges.add(UserBadge(
                    id = "dedicated_learner",
                    name = "Dedicated Learner",
                    description = "$currentStreak day streak",
                    icon = "🔥",
                    level = streakLevel,
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.DEDICATION
                ))
            }

            // Verse Enthusiast - Many favorites
            if (favoriteCount >= 5) {
                val favLevel = when {
                    favoriteCount >= 100 -> 5
                    favoriteCount >= 50 -> 4
                    favoriteCount >= 25 -> 3
                    favoriteCount >= 10 -> 2
                    else -> 1
                }
                badges.add(UserBadge(
                    id = "verse_enthusiast",
                    name = "Verse Enthusiast",
                    description = "$favoriteCount favorite verses",
                    icon = "❤️",
                    level = favLevel,
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.SPIRITUAL
                ))
            }

            // Time Master - Long study sessions
            val hoursSpent = timeSpent / 3600
            if (hoursSpent >= 1) {
                val timeLevel = when {
                    hoursSpent >= 1000 -> 5
                    hoursSpent >= 500 -> 4
                    hoursSpent >= 100 -> 3
                    hoursSpent >= 24 -> 2
                    else -> 1
                }
                badges.add(UserBadge(
                    id = "time_master",
                    name = "Time Master",
                    description = "$hoursSpent+ hours studying",
                    icon = "⏱️",
                    level = timeLevel,
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.DEDICATION
                ))
            }

            // Spiritual Warrior - Balanced learning
            val balanceScore = calculateBalance(versesRead, quizzesTaken, currentStreak)
            if (balanceScore >= 70) {
                val spiritualLevel = when {
                    balanceScore >= 95 -> 5
                    balanceScore >= 90 -> 4
                    balanceScore >= 85 -> 3
                    balanceScore >= 75 -> 2
                    else -> 1
                }
                badges.add(UserBadge(
                    id = "spiritual_warrior",
                    name = "Spiritual Warrior",
                    description = "Balanced spiritual journey",
                    icon = "⚔️",
                    level = spiritualLevel,
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.SPIRITUAL
                ))
            }

            // Elite Scholar - All-rounder
            if (versesRead >= 50 && quizzesTaken >= 20 && currentStreak >= 30) {
                badges.add(UserBadge(
                    id = "elite_scholar",
                    name = "Elite Scholar",
                    description = "Master of all learning modes",
                    icon = "👑",
                    level = 5,
                    unlockedAt = getCurrentDate(),
                    category = BadgeCategory.WISDOM
                ))
            }

            badges
        }
    }

    // Calculate user level based on experience (5 Yoga Levels + 19 Steps)
    suspend fun calculateLevel(
        versesRead: Int,
        quizzesTaken: Int,
        score: Int,
        totalQuestions: Int,
        timeSpent: Long,
        currentStreak: Int,
        favoriteCount: Int,
        segmentSystem: SegmentWeightageSystem? = null
    ): UserLevel {
        return withContext(Dispatchers.Default) {
            // Calculate XP
            val verseXP = versesRead * 10
            val quizXP = quizzesTaken * 25
            val scoreXP = if (totalQuestions > 0) (score * 50) / totalQuestions else 0
            val timeXP = (timeSpent / 3600).toInt() * 5 // 5 XP per hour
            val streakXP = currentStreak * 15
            val favoriteXP = favoriteCount * 8

            // Calculate segment mastery bonus
            val segmentMasteryXP = segmentSystem?.let { system ->
                val totalSegments = LearningSegment.values().size
                val masteredSegments = system.segments.values.count { it.accuracy >= 80f }

                // Bonus XP for balanced mastery across segments
                val balanceBonus = if (masteredSegments > 0) {
                    (masteredSegments * 20) + (system.overallAccuracy.toInt() / 10)
                } else {
                    0
                }

                balanceBonus
            } ?: 0

            val totalXP = verseXP + quizXP + scoreXP + timeXP + streakXP + favoriteXP + segmentMasteryXP

            // Map XP to 5 Yoga Levels and 19 Steps
            // Level 1 (Karma Yoga): Steps 1-3 (0-375 XP)
            // Level 2 (Bhakti Yoga): Steps 4-7 (375-750 XP)
            // Level 3 (Jnana Yoga): Steps 8-11 (750-1125 XP)
            // Level 4 (Dhyana Yoga): Steps 12-15 (1125-1500 XP)
            // Level 5 (Moksha/Sanyasa): Steps 16-19 (1500+ XP)

            val level = when {
                totalXP < 375 -> 1
                totalXP < 750 -> 2
                totalXP < 1125 -> 3
                totalXP < 1500 -> 4
                else -> 5
            }

            val step = when {
                totalXP < 125 -> 1
                totalXP < 250 -> 2
                totalXP < 375 -> 3
                totalXP < 500 -> 4
                totalXP < 625 -> 5
                totalXP < 750 -> 6
                totalXP < 875 -> 7
                totalXP < 1000 -> 8
                totalXP < 1125 -> 9
                totalXP < 1250 -> 10
                totalXP < 1375 -> 11
                totalXP < 1500 -> 12
                totalXP < 1625 -> 13
                totalXP < 1750 -> 14
                totalXP < 1875 -> 15
                totalXP < 2000 -> 16
                totalXP < 2125 -> 17
                totalXP < 2250 -> 18
                else -> 19
            }

            val (title, description, icon) = getLevelInfo(level, step)

            UserLevel(
                level = level,
                title = title,
                description = description,
                icon = icon,
                experiencePoints = totalXP,
                nextLevelXP = getNextLevelXP(level, step),
                category = getLevelCategory(level)
            )
        }
    }

    private fun getLevelInfo(level: Int, step: Int): Triple<String, String, String> {
        return when (level) {
            1 -> Triple(
                "Karma Yoga",
                "Action without Attachment - Step $step of 19",
                "🌿"
            )
            2 -> Triple(
                "Bhakti Yoga",
                "Devotion and Love for God - Step $step of 19",
                "🔥"
            )
            3 -> Triple(
                "Jnana Yoga",
                "Knowledge and Wisdom - Step $step of 19",
                "🧠"
            )
            4 -> Triple(
                "Dhyana Yoga",
                "Meditation and Mind Control - Step $step of 19",
                "🌬️"
            )
            5 -> Triple(
                "Moksha/Sanyasa",
                "Liberation and Freedom - Step $step of 19",
                "🌺"
            )
            else -> Triple("Yoga Path", "Spiritual Journey - Step $step of 19", "✨")
        }
    }

    private fun getLevelCategory(level: Int): String {
        return when (level) {
            1 -> "Karma Yoga"
            2 -> "Bhakti Yoga"
            3 -> "Jnana Yoga"
            4 -> "Dhyana Yoga"
            5 -> "Moksha/Sanyasa"
            else -> "Unknown"
        }
    }

    private fun getNextLevelXP(level: Int, step: Int): Int {
        return when {
            // Level 1 (Karma Yoga) - Steps 1-3
            level == 1 && step == 1 -> 125
            level == 1 && step == 2 -> 250
            level == 1 && step == 3 -> 375
            // Level 2 (Bhakti Yoga) - Steps 4-7
            level == 2 && step == 4 -> 500
            level == 2 && step == 5 -> 625
            level == 2 && step == 6 -> 750
            level == 2 && step == 7 -> 875
            // Level 3 (Jnana Yoga) - Steps 8-11
            level == 3 && step == 8 -> 1000
            level == 3 && step == 9 -> 1125
            level == 3 && step == 10 -> 1250
            level == 3 && step == 11 -> 1375
            // Level 4 (Dhyana Yoga) - Steps 12-15
            level == 4 && step == 12 -> 1500
            level == 4 && step == 13 -> 1625
            level == 4 && step == 14 -> 1750
            level == 4 && step == 15 -> 1875
            // Level 5 (Moksha/Sanyasa) - Steps 16-19
            level == 5 && step == 16 -> 2000
            level == 5 && step == 17 -> 2125
            level == 5 && step == 18 -> 2250
            else -> 2500 // Final step max XP
        }
    }

    private fun calculateBalance(versesRead: Int, quizzesTaken: Int, streak: Int): Int {
        // Balance score: how evenly distributed are activities
        val verseScore = (versesRead * 20).coerceAtMost(100)
        val quizScore = (quizzesTaken * 20).coerceAtMost(100)
        val streakScore = (streak * 2).coerceAtMost(100)

        return (verseScore + quizScore + streakScore) / 3
    }

    private fun getCurrentDate(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return String.format("%d-%02d-%02d", year, month, day)
    }
}
