package com.aipoweredgita.app.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes using Kotlin Serialization
 * Replaces string-based routes with compile-time safety
 */

// Base routes without arguments
@Serializable data object HomeKey
@Serializable data object ProfileKey
@Serializable data object FavoritesKey
@Serializable data object OfflineDownloadKey
@Serializable data object ActivityHistoryKey
@Serializable data object WidgetSettingsKey
@Serializable data object BadgesKey
@Serializable data object CoinHistoryKey
@Serializable data object AwakeningKey
@Serializable data object DailyActivityKey
@Serializable data object SettingsKey
@Serializable data object LoginKey
@Serializable data object QuizSectionKey
@Serializable data object RecommendationsKey

// Routes with arguments
@Serializable data class NormalModeKey(val chapter: Int = 0, val verse: Int = 0)
@Serializable data class QuizConfigKey(val questionCount: Int = 10, val language: String = "en")
@Serializable data class QuizModeKey(val questionCount: Int = 10, val language: String = "en")
@Serializable data class FlashcardsKey(val topic: String = "")
@Serializable data class RandomSlokaKey(val chapter: Int = 0, val verse: Int = 0)
