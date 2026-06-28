package day.pithos.twitchdownloader4j.parse;

import java.util.ArrayList;
import java.util.List;

public final class M3u8Parser {

    private M3u8Parser() {}

    public static List<String> parseSegments(String m3u8, int start, int end) {
        String[] lines = m3u8.split("\n");
        double current = 0;
        List<String> result = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].startsWith("#EXTINF:")) continue;

            if (i + 1 >= lines.length) {
                throw new IllegalArgumentException("M3U8 format error: #EXTINF without segment URL");
            }

            double duration = parseDuration(lines[i]);
            double segStart = current;
            double segEnd = current + duration;

            if (segEnd <= start) {
                current = segEnd;
                continue;
            }

            if (end <= segStart) break;
            result.add(lines[i + 1]);
            current = segEnd;
        }
        return result;
    }

    private static double parseDuration(String line) {
        try {
            return Double.parseDouble(line.replace("#EXTINF:", "").replace(",", ""));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid EXTINF: " + line, e);
        }
    }
}
