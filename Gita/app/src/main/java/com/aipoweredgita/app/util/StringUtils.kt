package com.aipoweredgita.app.util

object StringUtils {
    fun clean(s: String?): String? {
        if (s == null) return null
        var t = s
        val map = mapOf(
            "â€¢" to "•", "â€“" to "–", "â€”" to "—", "â€˜" to "‘", "â€™" to "’",
            "â€œ" to "“", "â€ " to "”", "â€¦" to "…", "Ã—" to "×", "Â" to "",
            "ðŸ" to "", "dY" to "", "" to ""
        )
        for ((k, v) in map) t = t?.replace(k, v)
        t = t?.replace(Regex("[\\u0000-\\u001F\\u007F]"), "")
        return t
    }
}
