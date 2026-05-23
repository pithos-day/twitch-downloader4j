package day.pithos.twitchdownloader4j.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DurationUtil {
    private static final Pattern DURATION_PATTERN = Pattern.compile("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s)?");

    public static int parseToSeconds(String duration) {
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        if (matcher.matches()) {
            int hours = matcher.group(1) != null ? Integer.parseInt(matcher.group(1)) : 0;
            int minutes = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
            int seconds = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
            return hours * 3600 + minutes * 60 + seconds;
        }
        throw new IllegalArgumentException("Invalid duration format: " + duration);
    }

    public static String formatFromSeconds(int totalSeconds) {
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;

        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append("h");
        if (minutes > 0) sb.append(minutes).append("m");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s"); // 0秒のときも"s"は必須

        return sb.toString();
    }
}