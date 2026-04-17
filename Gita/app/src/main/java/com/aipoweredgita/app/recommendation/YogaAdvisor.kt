package com.aipoweredgita.app.recommendation

import com.aipoweredgita.app.database.UserStats

data class YogaAdvice(
    val currentLevel: Int,
    val nextFocusLevels: List<Int>,
    val tips: List<String>
)

object YogaAdvisor {
    fun suggest(stats: UserStats?): YogaAdvice {
        val level = com.aipoweredgita.app.ui.components.LotusLevelManager.levelFor(stats)
        val next = listOf((level + 1).coerceAtMost(5), level) // keep current and next
        val tips = listOf(
            "Practice detachment (Vairagya) while performing duties",
            "Deepen Bhakti through daily devotion and gratitude",
            "Balance action (Karma Yoga) with meditation (Dhyana)"
        )
        return YogaAdvice(currentLevel = level, nextFocusLevels = next.distinct(), tips = tips)
    }
}

