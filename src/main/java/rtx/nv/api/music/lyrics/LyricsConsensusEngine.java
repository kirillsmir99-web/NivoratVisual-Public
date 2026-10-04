package rtx.nv.api.music.lyrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LyricsConsensusEngine {
    public static Set<String> extractPhrases(List<LyricLine> lines, String rawText) {
        HashSet<String> phrases = new HashSet<>();
        ArrayList<String> textLines = new ArrayList<>();
        if (lines != null && !lines.isEmpty()) {
            for (LyricLine line : lines) {
                if (line != null && line.text != null) {
                    textLines.add(line.text);
                }
            }
        } else if (rawText != null && !rawText.isBlank()) {
            String[] split = rawText.split("\\r?\\n");
            textLines.addAll(Arrays.asList(split));
        }

        for (String raw : textLines) {
            String norm = normalize(raw);
            if (!norm.isEmpty()) {
                String[] words = norm.split("\\s+");
                if (words.length >= 2) {
                    phrases.add(norm);
                }

                if (words.length >= 3) {
                    for (int i = 0; i <= words.length - 3; i++) {
                        phrases.add(words[i] + " " + words[i + 1] + " " + words[i + 2]);
                    }
                }
            }
        }

        return phrases;
    }

    public static Candidate selectBest(List<Candidate> candidates, long durationSec) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        ArrayList<Candidate> valid = new ArrayList<>();
        for (Candidate c : candidates) {
            if (c != null && (!c.lines.isEmpty() || !c.rawText.isBlank())) {
                valid.add(c);
            }
        }

        if (valid.isEmpty()) {
            return null;
        } else if (valid.size() == 1) {
            Candidate single = valid.get(0);
            single.score = 100.0;
            return single;
        } else {
            for (Candidate c : valid) {
                c.score = 0; c.hasCrossConsensus = false;
                if (c.isSynced) {
                    c.score += 150.0;
                }

                if ("YANDEX_SYNCED".equalsIgnoreCase(c.provider)) {
                    c.score += 120.0;
                } else if ("GENIUS".equalsIgnoreCase(c.provider)) {
                    c.score += 110.0;
                } else if ("LRCLIB_SYNCED".equalsIgnoreCase(c.provider)) {
                    c.score += 90.0;
                } else if ("LRCLIB_PLAIN".equalsIgnoreCase(c.provider)) {
                    c.score += 40.0;
                } else if ("NETEASE".equalsIgnoreCase(c.provider)) {
                    c.score += 15.0;
                } else if ("OVH".equalsIgnoreCase(c.provider)) {
                    c.score += 20.0;
                }
            }

            for (int i = 0; i < valid.size(); i++) {
                Candidate c1 = valid.get(i);
                for (int j = i + 1; j < valid.size(); j++) {
                    Candidate c2 = valid.get(j);
                    if (c1.provider.equalsIgnoreCase(c2.provider)) continue;
                    double overlap = computePhraseOverlap(c1.phrases, c2.phrases);
                    if (overlap >= 0.45) {
                        double boost = 450.0 * overlap;
                        c1.score += boost;
                        c2.score += boost;
                        c1.hasCrossConsensus = true;
                        c2.hasCrossConsensus = true;
                    } else if (overlap < 0.1) {
                        if ("GENIUS".equalsIgnoreCase(c1.provider)) {
                            c2.score -= 550.0;
                        } else if ("GENIUS".equalsIgnoreCase(c2.provider)) {
                            c1.score -= 550.0;
                        }
                    }
                }
            }

            if (durationSec > 25L) {
                for (Candidate c : valid) {
                    if (c.isSynced && !c.lines.isEmpty()) {
                        long firstStart = c.lines.get(0).startMs;
                        long lastEnd = c.lines.get(c.lines.size() - 1).endMs;
                        long durMs = durationSec * 1000L;
                        if (lastEnd > durMs + 12000L) {
                            c.score -= 500.0;
                        } else if (lastEnd < durMs * 0.42 && durMs > 60000L) {
                            c.score -= 400.0;
                        }

                        if (firstStart > durMs * 0.55 && durMs > 45000L) {
                            c.score -= 450.0;
                        }
                    }
                }
            }

            valid.sort((a, b) -> Double.compare(b.score, a.score));
            Candidate best = valid.get(0);
            if (!best.hasCrossConsensus && valid.size() > 1) {
                best.possibleMismatch = true;
            }

            return best;
        }
    }

    public static double computePhraseOverlap(Set<String> set1, Set<String> set2) {
        if (set1 != null && set2 != null && !set1.isEmpty() && !set2.isEmpty()) {
            int minSize = Math.min(set1.size(), set2.size());
            if (minSize == 0) {
                return 0.0;
            } else {
                int count = 0;
                for (String s : set1) {
                    if (set2.contains(s)) {
                        count++;
                    }
                }
                return (double) count / minSize;
            }
        } else {
            return 0.0;
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-zа-я0-9\\s]", "").replaceAll("\\s+", " ").trim();
    }

    public static class Candidate {
        public final String provider;
        public final List<LyricLine> lines;
        public final String rawText;
        public final boolean isSynced;
        public final Set<String> phrases;
        public double score = 0.0;
        public boolean hasCrossConsensus = false;
        public boolean possibleMismatch = false;

        public Candidate(String provider, List<LyricLine> lines, String rawText, boolean isSynced) {
            this.provider = provider != null ? provider : "UNKNOWN";
            this.lines = lines != null ? lines : Collections.emptyList();
            this.rawText = rawText != null ? rawText : "";
            this.isSynced = isSynced;
            this.phrases = LyricsConsensusEngine.extractPhrases(this.lines, this.rawText);
        }
    }
}
