package rtx.nv.api.music.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.font.TextRenderer;
import rtx.nv.api.ui.theme.ThemeManager;

public class LyricLine {
    public final long startMs;
    public final long endMs;
    public final long vocalEndMs;
    public final String text;
    public final List<Word> words;
    private final List<String> wordStrings;
    private final long[] charStarts, charEnds;
    private TextRenderer cachedRenderer;
    private int cachedMaxWidth;
    private List<SubLine> cachedSubLines;

    public LyricLine(long startMs, long endMs, String text) {
        this(startMs, endMs, endMs, text, List.of());
    }

    public LyricLine(long startMs, long endMs, String text, List<Word> words) {
        this(startMs, endMs, endMs, text, words);
    }

    public LyricLine(long startMs, long endMs, long vocalEndMs, String text, List<Word> words) {
        this.startMs = Math.max(0L, startMs);
        this.endMs = Math.max(this.startMs, endMs);
        this.vocalEndMs = Math.max(this.startMs, vocalEndMs);
        this.text = text != null ? text.trim().replace('\u2014', '-').replace('\u2013', '-') : "";
        this.words = words != null ? List.copyOf(words) : List.of();
        this.charStarts = new long[this.text.length()];
        this.charEnds = new long[this.text.length()];
        long duration = Math.max(1, this.vocalEndMs - this.startMs);
        for (int i = 0; i < this.text.length(); i++) {
            charStarts[i] = this.startMs + duration * i / Math.max(1, this.text.length());
            charEnds[i] = Math.min(this.vocalEndMs, charStarts[i] + Math.max(60, Math.min(180, duration / Math.max(1, this.text.length()))));
        }
        for (Word word : this.words) {
            for (int i = Math.max(0, word.charStart()); i < Math.min(this.text.length(), word.charEnd()); i++) {
                charStarts[i] = word.startMs();
                charEnds[i] = Math.max(word.startMs() + 1, word.endMs());
            }
        }

        if (this.text.isEmpty()) {
            this.wordStrings = Collections.emptyList();
        } else {
            String[] split = this.text.split("\\s+");
            List<String> list = new ArrayList<>();
            for (String w : split) {
                if (!w.isBlank()) {
                    list.add(w.trim());
                }
            }
            this.wordStrings = Collections.unmodifiableList(list);
        }
    }

    public long getStartMs() {
        return this.startMs;
    }

    public long getEndMs() {
        return this.endMs;
    }

    public long getVocalEndMs() {
        return this.vocalEndMs;
    }

    public String getText() {
        return this.text;
    }

    public List<String> getWords() {
        return this.wordStrings;
    }

    public List<Word> getWordObjects() {
        return this.words;
    }

    public boolean isActiveAt(long timeMs) {
        return timeMs >= this.startMs && timeMs < this.endMs;
    }

    public LyricLine scaled(double factor) {
        long s = Math.max(0L, (long) (this.startMs * factor));
        long e = Math.max(s + 60L, (long) (this.endMs * factor));
        long v = Math.max(s + 50L, (long) (this.vocalEndMs * factor));
        List<Word> scaledWords = null;
        if (!this.words.isEmpty()) {
            scaledWords = new ArrayList<>(this.words.size());
            for (Word w : this.words) {
                long ws = Math.max(0L, (long) (w.startMs() * factor));
                long we = Math.max(ws + 50L, (long) (w.endMs() * factor));
                scaledWords.add(new Word(ws, we, w.text(), w.charStart(), w.charEnd()));
            }
        }
        return new LyricLine(s, e, v, this.text, scaledWords);
    }

    public float getCharAlpha(int charIndex, long trackPosition) {
        if (charIndex < 0 || charIndex >= charStarts.length) return 1f;
        long start = charStarts[charIndex];
        float progress = Math.max(0, Math.min(1, (trackPosition - start) / 120f));
        return .68f + .32f * progress;
    }

    public boolean isCharActive(int charIndex, long trackPosition) {
        return charIndex >= 0 && charIndex < charStarts.length && trackPosition >= charStarts[charIndex] && trackPosition < charEnds[charIndex];
    }

    public float getCharActiveProgress(int charIndex, long trackPosition) {
        if (charIndex < 0 || charIndex >= charStarts.length) return -1;
        if (trackPosition < charStarts[charIndex]) return -1;
        if (trackPosition >= charEnds[charIndex]) return 2;
        return Math.min(1f, (trackPosition - charStarts[charIndex]) / 160f);
    }

    public float getCharAnimationOffsetY(int charIdx, long trackPos, long now, String anim) {
        if (anim == null) {
            anim = "KARAOKE_BOUNCE";
        }

        switch (anim.toUpperCase()) {
            case "NV_FLOW": {
                float p = this.getCharActiveProgress(charIdx, trackPos);
                return p >= 0 && p < 1 ? -1.5f * (float)Math.sin(p * Math.PI) : 0;
            }
            case "KARAOKE_BOUNCE": {
                float p = this.getCharActiveProgress(charIdx, trackPos);
                if (p >= 0.0F && p <= 1.0F) {
                    if (p < 0.6F) {
                        float t = p / 0.6F;
                        return -5.2F * (float) Math.sin(t * Math.PI);
                    } else {
                        float t = (p - 0.6F) / 0.4F;
                        return 0.85F * (float) Math.sin(t * Math.PI) * (1.0F - t);
                    }
                }
                return 0.0F;
            }
            case "NEON_PULSE": {
                float np = this.getCharActiveProgress(charIdx, trackPos);
                if (np >= 0.0F && np <= 1.0F) {
                    return -3.8F * (float) Math.sin(np * Math.PI);
                } else {
                    return (float) Math.sin(now / 340.0 + charIdx * 0.35) * 1.2F;
                }
            }
            case "WAVE_FLOAT": {
                float baseWave = (float) Math.sin(now / 200.0 + charIdx * 0.38) * 3.2F;
                float wp = this.getCharActiveProgress(charIdx, trackPos);
                if (wp >= 0.0F && wp <= 1.0F) {
                    baseWave -= 3.0F * (float) Math.sin(wp * Math.PI);
                }
                return baseWave;
            }
            case "ELASTIC_POP": {
                float ep = this.getCharActiveProgress(charIdx, trackPos);
                if (ep >= 0.0F && ep <= 1.0F) {
                    float t = ep;
                    return -6.5F * (float) (Math.sin(t * Math.PI * 1.5) * Math.exp(-t * 2.5));
                }
                return 0.0F;
            }
            case "PULSE_GLOW": {
                float pg = this.getCharActiveProgress(charIdx, trackPos);
                if (pg >= 0.0F && pg <= 1.0F) {
                    return -2.4F * (float) Math.sin(pg * Math.PI);
                }
                return 0.0F;
            }
            case "SLIDE_FADE": {
                float sp = this.getCharActiveProgress(charIdx, trackPos);
                if (sp >= 0.0F && sp <= 1.0F) {
                    float t = 1.0F - sp;
                    return 4.5F * (t * t);
                }
                return 0.0F;
            }
            case "CLASSIC":
            default:
                return 0.0F;
        }
    }

    public int getCharColor(int charIdx, long trackPos, long now, String anim, int baseColor, float alpha, int activeColor) {
        if (anim == null) {
            anim = "KARAOKE_BOUNCE";
        }

        int a = Math.min(255, Math.max(16, (int) (alpha * 255.0F)));
        switch (anim.toUpperCase()) {
            case "NV_FLOW":
            case "KARAOKE_BOUNCE": {
                float progress = this.getCharActiveProgress(charIdx, trackPos);
                if (progress >= 0.0F && progress <= 1.0F) {
                    int bright = ThemeManager.accentBright(255.0F);
                    int col = ThemeManager.mix(activeColor, bright, 0.45F);
                    return (a << 24) | (col & 0xFFFFFF);
                } else if (progress > 1.0F) {
                    int sung = ThemeManager.mix(baseColor, activeColor, 0.28F);
                    return (a << 24) | (sung & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.58F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
            case "NEON_PULSE": {
                float progress = this.getCharActiveProgress(charIdx, trackPos);
                if (progress >= 0.0F && progress <= 1.0F) {
                    float pulse = (float) Math.sin(now / 95.0) * 0.5F + 0.5F;
                    int pulseCol = ThemeManager.mix(activeColor, ThemeManager.accentBright(255.0F), pulse);
                    return (a << 24) | (pulseCol & 0xFFFFFF);
                } else if (progress > 1.0F) {
                    int sung = ThemeManager.mix(baseColor, activeColor, 0.35F);
                    return (a << 24) | (sung & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.55F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
            case "WAVE_FLOAT": {
                float progress = this.getCharActiveProgress(charIdx, trackPos);
                if (progress >= 0.0F && progress <= 1.0F) {
                    return (a << 24) | (ThemeManager.accentBright(255.0F) & 0xFFFFFF);
                } else if (progress > 1.0F) {
                    float wave = (float) Math.sin(now / 320.0 + charIdx * 0.28) * 0.5F + 0.5F;
                    int waveCol = ThemeManager.mix(ThemeManager.gradientA(255.0F), ThemeManager.gradientB(255.0F), wave);
                    return (a << 24) | (waveCol & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.60F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
            case "PULSE_GLOW": {
                float pulseProgress = this.getCharActiveProgress(charIdx, trackPos);
                if (pulseProgress >= 0.0F && pulseProgress <= 1.0F) {
                    float wave = (float) Math.sin(now / 110.0) * 0.5F + 0.5F;
                    int pulseCol = ThemeManager.mix(activeColor, ThemeManager.accentBright(255.0F), wave);
                    return (a << 24) | (pulseCol & 0xFFFFFF);
                } else if (pulseProgress > 1.0F) {
                    return (a << 24) | (baseColor & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.60F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
            case "ELASTIC_POP": {
                float progress = this.getCharActiveProgress(charIdx, trackPos);
                if (progress >= 0.0F && progress <= 1.0F) {
                    return (a << 24) | (ThemeManager.accentBright(255.0F) & 0xFFFFFF);
                } else if (progress > 1.0F) {
                    return (a << 24) | (baseColor & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.55F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
            case "SLIDE_FADE": {
                float progress = this.getCharActiveProgress(charIdx, trackPos);
                if (progress >= 0.0F && progress <= 1.0F) {
                    return (a << 24) | (activeColor & 0xFFFFFF);
                } else if (progress > 1.0F) {
                    return (a << 24) | (baseColor & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.55F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
            case "CLASSIC":
            default: {
                float progress = this.getCharActiveProgress(charIdx, trackPos);
                if (progress >= 0.0F && progress <= 1.0F) {
                    return (a << 24) | (activeColor & 0xFFFFFF);
                } else if (progress > 1.0F) {
                    return (a << 24) | (baseColor & 0xFFFFFF);
                } else {
                    int futureA = Math.max(20, (int) (a * 0.55F));
                    return (futureA << 24) | (baseColor & 0xFFFFFF);
                }
            }
        }
    }

    public List<SubLine> getSubLines(TextRenderer textRenderer, int maxW) {
        if (cachedRenderer == textRenderer && cachedMaxWidth == maxW && cachedSubLines != null) return cachedSubLines;
        List<SubLine> list = new ArrayList<>();
        if (this.text.isEmpty()) {
            return list;
        }

        int totalW = textRenderer.getWidth(this.text);
        if (totalW <= maxW) {
            list.add(new SubLine(0, this.text.length(), totalW));
            return cacheLayout(textRenderer, maxW, list);
        }

        int start = 0, cursor = 0, lastSpace = -1, width = 0;
        while (cursor < text.length()) {
            int codepoint = text.codePointAt(cursor);
            int count = Character.charCount(codepoint);
            int glyphWidth = textRenderer.getWidth(new String(Character.toChars(codepoint)));
            if (width + glyphWidth > maxW && cursor > start) {
                int end = lastSpace >= start ? lastSpace : cursor;
                list.add(new SubLine(start, end, textRenderer.getWidth(text.substring(start, end))));
                start = lastSpace >= start ? lastSpace + 1 : cursor;
                while (start < text.length() && Character.isWhitespace(text.charAt(start))) start++;
                cursor = start; width = 0; lastSpace = -1;
                continue;
            }
            if (Character.isWhitespace(codepoint)) lastSpace = cursor;
            width += glyphWidth; cursor += count;
        }
        if (start < text.length()) list.add(new SubLine(start, text.length(), textRenderer.getWidth(text.substring(start))));

        return cacheLayout(textRenderer, maxW, list);
    }

    private List<SubLine> cacheLayout(TextRenderer renderer, int maxWidth, List<SubLine> lines) {
        cachedRenderer = renderer; cachedMaxWidth = maxWidth; cachedSubLines = List.copyOf(lines);
        return cachedSubLines;
    }

    @Override
    public String toString() {
        return "[" + this.startMs + "->" + this.endMs + "] " + this.text;
    }

    public record SubLine(int charStart, int charEnd, int width) {}

    public record Word(long startMs, long endMs, String text, int charStart, int charEnd) {
        public Word(long startMs, long endMs, String text, int charStart, int charEnd) {
            this.text = text != null ? text.replace('\u2014', '-').replace('\u2013', '-') : "";
            this.startMs = startMs;
            this.endMs = endMs;
            this.charStart = charStart;
            this.charEnd = charEnd;
        }
    }
}
