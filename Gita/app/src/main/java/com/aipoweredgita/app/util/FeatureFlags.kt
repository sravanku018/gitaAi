package com.aipoweredgita.app.util

/**
 * Centralized feature flags for runtime-tunable behavior.
 * In the future, these can be sourced from remote config or BuildConfig fields.
 */
object FeatureFlags {
    // Enable verbose network logging (OkHttp logging interceptor)
    @JvmField val ENABLE_VERBOSE_NETWORK_LOGS: Boolean = true

    // Enable lightweight performance metrics logs (e.g., API timings)
    @JvmField val ENABLE_PERF_METRICS: Boolean = true

    // Show offline banner on main reading screen
    @JvmField val SHOW_OFFLINE_BANNER: Boolean = true

    // Background download constraints (can be tuned per build/flavor)
<<<<<<< HEAD
    // Set to false to allow downloads on mobile data (useful for large models like Gemma 4 2B)
=======
    // Set to false to allow downloads on mobile data (useful for large models like Gemma 2B)
>>>>>>> 401318f91826bfb1f047732aa660110805c4c39b
    @JvmField val DOWNLOADS_REQUIRE_UNMETERED: Boolean = false
    @JvmField val DOWNLOADS_REQUIRE_CHARGING: Boolean = false
    @JvmField val DOWNLOADS_REQUIRE_BATTERY_NOT_LOW: Boolean = false
}
