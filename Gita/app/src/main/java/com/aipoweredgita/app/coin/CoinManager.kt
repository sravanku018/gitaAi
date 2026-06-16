package com.aipoweredgita.app.coin

import android.content.Context

/**
 * Unified facade for all coin operations.
 * Screens and ViewModels should use this instead of calling individual classes.
 */
object CoinManager {

    // ── Transaction History ─────────────────────────────────────────────────

    fun logTransaction(context: Context, amount: Int, description: String) =
        CoinTransactionLogger.log(context, amount, description)

    fun getTransactionHistory(context: Context): List<CoinEntry> =
        CoinTransactionLogger.getHistory(context)

    // ── Daily Rewards ───────────────────────────────────────────────────────

    fun dailyRewards(context: Context): DailyRewardsTracker =
        DailyRewardsTracker.getInstance(context)

    // ── Quiz Reward Calculation ─────────────────────────────────────────────

    fun calculateQuizReward(input: CoinRewardEngine.Input): CoinRewardEngine.Result =
        CoinRewardEngine.calculate(input)

    // ── Shortcuts ───────────────────────────────────────────────────────────

    /** Log + emit CoinEvent in one call. */
    fun earn(context: Context, amount: Int, description: String) {
        logTransaction(context, amount, description)
    }

    fun spend(context: Context, amount: Int, description: String) {
        logTransaction(context, -amount, description)
    }
}
