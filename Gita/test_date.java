import java.text.SimpleDateFormat;
import java.util.*;

public class test_date {
    public static Date parseDateRobust(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String normalized = dateStr.replace("+00:00", "Z").replace("+0000", "Z");
        String[] formats = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ss'Z'"
        };
        for (String formatStr : formats) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(formatStr, Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date parsed = sdf.parse(normalized);
                if (parsed != null) return parsed;
            } catch (Exception e) {
                // Ignore
            }
        }
        return null;
    }

    public static void main(String[] args) {
        Date date = parseDateRobust("2026-06-24T08:22:36.267992+00:00");
        if (date == null) {
            System.out.println("FAILED TO PARSE");
        } else {
            System.out.println("PARSED: " + date.toString());
        }
    }
}
