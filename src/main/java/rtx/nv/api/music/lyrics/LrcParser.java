package rtx.nv.api.music.lyrics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/** Preserves provider timestamps; never shortens a vocal interval using guessed word counts. */
public final class LrcParser {
    private static final Pattern LINE_TIME = Pattern.compile("\\[(\\d+):(\\d{1,2}(?:\\.\\d+)?)\\]");
    private static final Pattern WORD_TIME = Pattern.compile("<(\\d+):(\\d{1,2}(?:\\.\\d+)?)>");
    private static final Pattern OFFSET = Pattern.compile("\\[offset:([+-]?\\d+)\\]", Pattern.CASE_INSENSITIVE);
    private record Raw(long time, String text) {}
    private record Cue(long time, int index) {}

    private LrcParser() {}

    public static List<LyricLine> parse(String input) {
        if (input == null || input.isBlank()) return List.of();
        long offset = 0;
        var offsetMatch = OFFSET.matcher(input);
        if (offsetMatch.find()) try { offset = Long.parseLong(offsetMatch.group(1)); } catch (NumberFormatException ignored) {}
        var rows = new ArrayList<Raw>();
        for (String row : input.split("\\R")) {
            var matcher = LINE_TIME.matcher(row);
            var times = new ArrayList<Long>();
            int textStart = 0;
            while (matcher.find() && (times.isEmpty() ? row.substring(0, matcher.start()).isBlank() : row.substring(textStart, matcher.start()).isBlank())) {
                times.add(time(matcher.group(1), matcher.group(2), offset));
                textStart = matcher.end();
            }
            if (!times.isEmpty()) for (long value : times) rows.add(new Raw(value, row.substring(textStart).trim()));
        }
        rows.sort(Comparator.comparingLong(Raw::time));
        var result = new ArrayList<LyricLine>();
        for (int i = 0; i < rows.size(); i++) {
            Raw row = rows.get(i);
            long end = row.time + 5000;
            for (int next = i + 1; next < rows.size(); next++) {
                if (rows.get(next).time > row.time) { end = rows.get(next).time; break; }
            }
            var cues = new ArrayList<Cue>();
            var plain = new StringBuilder();
            var words = WORD_TIME.matcher(row.text);
            int cursor = 0;
            while (words.find()) {
                plain.append(row.text, cursor, words.start());
                cues.add(new Cue(time(words.group(1), words.group(2), offset), plain.length()));
                cursor = words.end();
            }
            plain.append(row.text, cursor, row.text.length());
            String untrimmed = plain.toString();
            String text = untrimmed.trim();
            if (text.isEmpty() || text.matches("(?i)\\[?(?:instrumental|инструментал|intro|outro|solo|соло|музыка)\\]?")) continue;
            int leading = untrimmed.indexOf(text);
            var timed = new ArrayList<LyricLine.Word>();
            for (int w = 0; w < cues.size(); w++) {
                Cue cue = cues.get(w);
                int startChar = Math.max(0, Math.min(text.length(), cue.index - leading));
                int endChar = w + 1 < cues.size() ? Math.max(startChar, Math.min(text.length(), cues.get(w + 1).index - leading)) : text.length();
                while (startChar < endChar && Character.isWhitespace(text.charAt(startChar))) startChar++;
                while (endChar > startChar && Character.isWhitespace(text.charAt(endChar - 1))) endChar--;
                if (startChar == endChar) continue;
                long start = Math.max(row.time, Math.min(end, cue.time));
                long wordEnd = w + 1 < cues.size() ? Math.min(end, cues.get(w + 1).time) : end;
                if (wordEnd <= start) continue;
                timed.add(new LyricLine.Word(start, wordEnd, text.substring(startChar, endChar), startChar, endChar));
            }
            result.add(new LyricLine(row.time, end, end, text, timed));
        }
        return List.copyOf(result);
    }

    private static long time(String minutes, String seconds, long offset) {
        try { return Math.max(0, Long.parseLong(minutes) * 60000L + Math.round(Double.parseDouble(seconds) * 1000) + offset); }
        catch (NumberFormatException invalid) { return 0; }
    }
}
