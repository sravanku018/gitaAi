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
        // Remove non-printable control characters except \t (0x09), \n (0x0A), and \r (0x0D)
        t = t?.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]"), "")
        return t
    }
}
