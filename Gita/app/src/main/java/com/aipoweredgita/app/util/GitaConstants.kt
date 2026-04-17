package com.aipoweredgita.app.util

object GitaConstants {
    const val DEFAULT_LANGUAGE = "tel"
    const val MAX_CHAPTERS = 18
    const val API_BASE_URL = "https://gita-api.vercel.app/"
    const val NETWORK_CONNECT_TIMEOUT_SEC = 15L
    const val NETWORK_READ_TIMEOUT_SEC = 20L
    const val NETWORK_WRITE_TIMEOUT_SEC = 10L
    const val VERSE_CACHE_MAX_SIZE_KB = 2000
    const val TOTAL_VERSES = 700

    val CHAPTER_VERSE_COUNTS: Map<Int, Int> = mapOf(
        1 to 47, 2 to 72, 3 to 43, 4 to 42, 5 to 29, 6 to 47,
        7 to 30, 8 to 28, 9 to 34, 10 to 42, 11 to 55, 12 to 20,
        13 to 34, 14 to 27, 15 to 20, 16 to 24, 17 to 28, 18 to 78
    )
}
