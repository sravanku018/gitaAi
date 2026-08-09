package com.aipoweredgita.app.util

import com.aipoweredgita.app.BuildConfig

/**
 * Centralized feature flags for runtime-tunable behavior.
 * Debug-only flags are tied to BuildConfig.DEBUG to prevent leaking
 * sensitive data (API URLs, user IDs, coin balances) in release builds.
 */
object FeatureFlags {
    // Verbose network logging — debug only (leaks API URLs, headers, bodies)
    val ENABLE_VERBOSE_NETWORK_LOGS: Boolean = BuildConfig.DEBUG

    // Performance metrics logs — debug only
    val ENABLE_PERF_METRICS: Boolean = BuildConfig.DEBUG

    // Show offline banner on main reading screen
    @JvmField val SHOW_OFFLINE_BANNER: Boolean = true

    // Background download constraints (can be tuned per build/flavor)
    // Set to false to allow downloads on mobile data (useful for large models like Gemma 4 2B)
    @JvmField val DOWNLOADS_REQUIRE_UNMETERED: Boolean = false
    @JvmField val DOWNLOADS_REQUIRE_CHARGING: Boolean = false
    @JvmField val DOWNLOADS_REQUIRE_BATTERY_NOT_LOW: Boolean = false
}
