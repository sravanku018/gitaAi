package com.aipoweredgita.app.coin

/**
 * Voice chat spend: short / medium / long by question character length.
 * App source of truth for offline-first deduct + UI. Keep server voice_chat_rules in sync.
 */
object VoiceCoinPricing {
    const val SHORT_MAX_CHARS = 50
    const val MEDIUM_MAX_CHARS = 150

    const val SHORT_COST = 4
    const val MEDIUM_COST = 6
    const val LONG_COST = 10

    /** Minimum coins needed to open / attempt a question (short tier). */
    const val MIN_COST = SHORT_COST

    data class Quote(val cost: Int, val label: String, val length: Int)

    fun quote(question: String): Quote {
        val len = question.length
        return when {
            len <= SHORT_MAX_CHARS -> Quote(SHORT_COST, "Short", len)
            len <= MEDIUM_MAX_CHARS -> Quote(MEDIUM_COST, "Medium", len)
            else -> Quote(LONG_COST, "Long", len)
        }
    }

    fun costFor(question: String): Int = quote(question).cost
}
