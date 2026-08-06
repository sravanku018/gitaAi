import java.text.SimpleDateFormat
import java.util.*

fun parseDateRobust(dateStr: String?): Date? {
    if (dateStr.isNullOrEmpty()) return null
    val normalized = dateStr.replace("+00:00", "Z").replace("+0000", "Z")
    val formats = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        "yyyy-MM-dd'T'HH:mm:ss'Z'"
    )
    for (formatStr in formats) {
        try {
            val sdf = SimpleDateFormat(formatStr, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val parsed = sdf.parse(normalized)
            if (parsed != null) return parsed
        } catch (e: Exception) {
            // Ignore
        }
    }
    return null
}

fun main() {
    val date = parseDateRobust("2026-06-24T08:22:36.267992+00:00")
    if (date == null) {
        println("FAILED TO PARSE")
    } else {
        println("PARSED: " + date.toString())
    }
}
