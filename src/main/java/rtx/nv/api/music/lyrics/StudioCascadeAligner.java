package rtx.nv.api.music.lyrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudioCascadeAligner {
    private static final Pattern RU_VOWELS = Pattern.compile("[аеёиоуыэюяАЕЁИОУЫЭЮЯ]");
    private static final Pattern EN_VOWELS = Pattern.compile("[aeiouyAEIOUY]");
    private static final Pattern SECTION_HEADER = Pattern.compile("^\\[.*?\\]$");

    public static List<LyricLine> align(String rawText, byte[] pcmData, long durationSec) {
        if (rawText != null && !rawText.isBlank()) {
            long durMs = durationSec > 30L ? durationSec * 1000L : 180000L;
            List<Stanza> stanzas = parseStanzas(rawText);
            if (stanzas.isEmpty()) {
                return Collections.emptyList();
            } else {
                List<VocalRegion> regions = null;
                if (pcmData != null && pcmData.length > 44) {
                    try {
                        float[] samples = extractPcmSamples(pcmData);
                        if (samples.length >= 16000) {
                            float[] filtered = applyVocalBandpassFilter(samples, 16000);
                            regions = detectVocalRegions(filtered, 16000, durMs);
                        }
                    } catch (Exception ignored) {
                    }
                }

                if (regions == null || regions.isEmpty()) {
                    regions = generateAcousticStructure(stanzas.size(), durMs);
                }

                return alignStanzasToVocalRegions(stanzas, regions, durMs);
            }
        } else {
            return Collections.emptyList();
        }
    }

    public static List<Stanza> parseStanzas(String text) {
        ArrayList<Stanza> stanzas = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");
        Stanza current = new Stanza("");

        for (String raw : lines) {
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                if (!current.lines.isEmpty()) {
                    stanzas.add(current);
                    current = new Stanza("");
                }
            } else if (SECTION_HEADER.matcher(trimmed).matches()) {
                if (!current.lines.isEmpty()) {
                    stanzas.add(current);
                }
                current = new Stanza(trimmed);
            } else {
                current.lines.add(trimmed);
            }
        }

        if (!current.lines.isEmpty()) {
            stanzas.add(current);
        }

        return stanzas;
    }

    public static float[] applyVocalBandpassFilter(float[] samples, int sampleRate) {
        if (samples != null && samples.length != 0) {
            double freqRatio = 1200.0 / sampleRate;
            double q = 0.9;
            double omega = (Math.PI * 2) * freqRatio;
            double alpha = Math.sin(omega) / (2.0 * q);
            double b0 = alpha;
            double b1 = 0.0;
            double b2 = -alpha;
            double a0 = 1.0 + alpha;
            double a1 = -2.0 * Math.cos(omega);
            double a2 = 1.0 - alpha;
            float[] output = new float[samples.length];
            double x1 = 0.0;
            double x2 = 0.0;
            double y1 = 0.0;
            double y2 = 0.0;

            for (int i = 0; i < samples.length; i++) {
                double x0 = samples[i];
                double y0 = (b0 / a0) * x0 + (b1 / a0) * x1 + (b2 / a0) * x2 - (a1 / a0) * y1 - (a2 / a0) * y2;
                output[i] = (float) y0;
                x2 = x1;
                x1 = x0;
                y2 = y1;
                y1 = y0;
            }

            return output;
        } else {
            return samples;
        }
    }

    public static List<VocalRegion> detectVocalRegions(float[] samples, int sampleRate, long maxDurMs) {
        ArrayList<VocalRegion> regions = new ArrayList<>();
        int frameSize = (int) (sampleRate * 0.02);
        int totalFrames = samples.length / frameSize;
        if (totalFrames <= 0) {
            return regions;
        } else {
            double[] frameEnergy = new double[totalFrames];
            double maxRms = 1.0E-5;

            for (int f = 0; f < totalFrames; f++) {
                int offset = f * frameSize;
                double sum = 0.0;
                for (int s = 0; s < frameSize; s++) {
                    double sample = samples[offset + s];
                    sum += sample * sample;
                }
                double rms = Math.sqrt(sum / frameSize);
                frameEnergy[f] = rms;
                if (rms > maxRms) {
                    maxRms = rms;
                }
            }

            for (int f = 0; f < totalFrames; f++) {
                frameEnergy[f] /= maxRms;
            }

            double threshold = 0.16;
            boolean inVocal = false;
            long regionStart = 0L;

            for (int f = 0; f < totalFrames; f++) {
                long timeMs = f * 20L;
                boolean above = frameEnergy[f] > threshold;
                if (above && !inVocal) {
                    inVocal = true;
                    regionStart = timeMs;
                } else if (!above && inVocal) {
                    int lookahead = Math.min(totalFrames, f + 15);
                    boolean bridge = false;
                    for (int k = f + 1; k < lookahead; k++) {
                        if (frameEnergy[k] > threshold) {
                            bridge = true;
                            break;
                        }
                    }

                    if (!bridge) {
                        inVocal = false;
                        if (timeMs - regionStart >= 2500L) {
                            regions.add(new VocalRegion(regionStart, timeMs));
                        }
                    }
                }
            }

            if (inVocal) {
                regions.add(new VocalRegion(regionStart, Math.min(maxDurMs, totalFrames * 20L)));
            }

            return regions;
        }
    }

    public static List<VocalRegion> generateAcousticStructure(int stanzaCount, long durationMs) {
        ArrayList<VocalRegion> regions = new ArrayList<>();
        int count = Math.max(1, stanzaCount);
        long introDur = Math.max(10000L, (long) (durationMs * 0.08));
        long outroDur = Math.max(6000L, (long) (durationMs * 0.05));
        long bodyDur = durationMs - introDur - outroDur;
        if (count == 1) {
            regions.add(new VocalRegion(introDur, durationMs - outroDur));
            return regions;
        } else {
            long bridgeDur = durationMs > 120000L ? 12000L : 6000L;
            long vocalPool = Math.max(20000L, bodyDur - bridgeDur);
            long stanzaDur = vocalPool / count;
            int midPoint = count / 2;
            long curStart = introDur;

            for (int i = 0; i < count; i++) {
                if (i == midPoint) {
                    curStart += bridgeDur;
                }

                long curEnd = Math.min(durationMs - outroDur, curStart + stanzaDur);
                regions.add(new VocalRegion(curStart, curEnd));
                curStart = curEnd + 1500L;
            }

            return regions;
        }
    }

    public static List<LyricLine> alignStanzasToVocalRegions(List<Stanza> stanzas, List<VocalRegion> regions, long durationMs) {
        ArrayList<LyricLine> result = new ArrayList<>();
        int sCount = stanzas.size();
        int rCount = regions.size();

        for (int sIdx = 0; sIdx < sCount; sIdx++) {
            Stanza stanza = stanzas.get(sIdx);
            if (!stanza.lines.isEmpty()) {
                int rIdx = Math.min(rCount - 1, (int) ((double) sIdx / sCount * rCount));
                VocalRegion reg = regions.get(rIdx);
                long rStart = reg.startMs;
                long rEnd = reg.endMs;
                long rDur = Math.max(3000L, rEnd - rStart);
                int lineCount = stanza.lines.size();
                long slotDur = rDur / lineCount;

                for (int lIdx = 0; lIdx < lineCount; lIdx++) {
                    String lineText = stanza.lines.get(lIdx).trim();
                    if (!lineText.isEmpty()) {
                        long lineStart = rStart + lIdx * slotDur;
                        long estDur = Math.min(6500L, Math.max(2500L, lineText.length() * 160L));
                        long lineEnd = Math.min(rEnd, Math.min(lineStart + estDur, lineStart + slotDur - 300L));
                        if (lineEnd <= lineStart) {
                            lineEnd = lineStart + 2200L;
                        }

                        List<LyricLine.Word> words = segmentWordsBySyllables(lineText, lineStart, lineEnd);
                        result.add(new LyricLine(lineStart, lineEnd, lineText, words));
                    }
                }
            }
        }

        return result;
    }

    public static List<LyricLine.Word> segmentWordsBySyllables(String text, long startMs, long endMs) {
        ArrayList<LyricLine.Word> words = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String[] split = text.trim().split("\\s+");
            if (split.length == 0) {
                return words;
            } else {
                long duration = Math.max(500L, endMs - startMs);
                if (split.length >= 6) {
                    long target = split.length * 240L + 300L;
                    duration = Math.min(duration, Math.max(800L, target));
                }

                int[] weights = new int[split.length];
                int totalWeight = 0;

                for (int i = 0; i < split.length; i++) {
                    String w = split[i];
                    int syl = countSyllables(w);
                    int wt = syl * 3 + Math.min(5, w.length());
                    char lastChar = w.charAt(w.length() - 1);
                    if (lastChar == ',' || lastChar == ';') {
                        wt += 3;
                    } else if (lastChar == '.' || lastChar == '!' || lastChar == '?' || w.endsWith("...")) {
                        wt += 4;
                    }

                    weights[i] = wt;
                    totalWeight += wt;
                }

                int searchOffset = 0;
                long curWordStart = startMs;

                for (int i = 0; i < split.length; i++) {
                    String w = split[i];
                    int charStart = text.indexOf(w, searchOffset);
                    if (charStart < 0) {
                        charStart = searchOffset;
                    }
                    int charEnd = charStart + w.length();
                    searchOffset = charEnd;

                    double fraction = totalWeight > 0 ? (double) weights[i] / totalWeight : 1.0 / split.length;
                    long wordDur = Math.max(80L, (long) (duration * fraction));
                    long wordEnd = (i == split.length - 1) ? curWordStart + wordDur : Math.min(endMs, curWordStart + (long) (wordDur * 0.9));
                    words.add(new LyricLine.Word(curWordStart, wordEnd, w, charStart, charEnd));
                    curWordStart += wordDur;
                }

                return words;
            }
        } else {
            return words;
        }
    }

    public static int countSyllables(String word) {
        if (word != null && !word.isEmpty()) {
            int count = 0;
            Matcher ru = RU_VOWELS.matcher(word);
            while (ru.find()) count++;

            Matcher en = EN_VOWELS.matcher(word);
            while (en.find()) count++;

            return Math.max(1, count);
        } else {
            return 1;
        }
    }

    public static float[] extractPcmSamples(byte[] pcmData) {
        if (pcmData != null && pcmData.length > 44) {
            int headerSize = 44;
            int dataLen = pcmData.length - headerSize;
            int numSamples = dataLen / 2;
            float[] samples = new float[numSamples];

            for (int i = 0; i < numSamples; i++) {
                int idx = headerSize + i * 2;
                short sample = (short) ((pcmData[idx] & 0xFF) | ((pcmData[idx + 1] & 0xFF) << 8));
                samples[i] = sample / 32768.0F;
            }

            return samples;
        } else {
            return new float[0];
        }
    }

    public static AcousticEnergyProfile analyzeAcousticEnergy(byte[] pcmData) {
        if (pcmData != null && pcmData.length > 44) {
            float[] samples = extractPcmSamples(pcmData);
            if (samples.length < 1600) {
                return new AcousticEnergyProfile(0.0, 0.0, 0.0, 0.0, false, false);
            } else {
                double sumSq = 0.0;
                for (float s : samples) {
                    sumSq += (double) s * s;
                }

                double rmsTotal = Math.sqrt(sumSq / samples.length);
                float[] vocalBand = applyVocalBandpassFilter(samples, 16000);
                double vocalSumSq = 0.0;
                for (float v : vocalBand) {
                    vocalSumSq += (double) v * v;
                }

                double rmsVocal = Math.sqrt(vocalSumSq / vocalBand.length);
                double vocalRatio = rmsVocal / Math.max(1.0E-4, rmsTotal);
                short windowSize = 320;
                int numWindows = vocalBand.length / windowSize;
                int voicedWindows = 0;

                for (int w = 0; w < numWindows; w++) {
                    int wOffset = w * windowSize;
                    double wSum = 0.0;
                    for (int k = 0; k < windowSize; k++) {
                        double s = vocalBand[wOffset + k];
                        wSum += s * s;
                    }
                    double wRms = Math.sqrt(wSum / windowSize);
                    if (wRms > 0.012) {
                        voicedWindows++;
                    }
                }

                double voicedFraction = numWindows > 0 ? (double) voicedWindows / numWindows : 0.0;
                boolean isPlayingSound = rmsTotal > 0.005;
                boolean hasVocalPresence = isPlayingSound && ((rmsVocal > 0.014 && voicedFraction > 0.12) || (vocalRatio > 0.2 && rmsVocal > 0.008) || voicedFraction > 0.25);
                return new AcousticEnergyProfile(rmsTotal, rmsVocal, vocalRatio, voicedFraction, isPlayingSound, hasVocalPresence);
            }
        } else {
            return new AcousticEnergyProfile(0.0, 0.0, 0.0, 0.0, false, false);
        }
    }

    public record AcousticEnergyProfile(
        double rmsTotal, double rmsVocal, double vocalRatio, double voicedFrameFraction, boolean isPlayingSound, boolean hasVocalPresence
    ) {
    }

    public static class Stanza {
        public final String header;
        public final List<String> lines = new ArrayList<>();

        public Stanza(String header) {
            this.header = header;
        }
    }

    public static class VocalRegion {
        public final long startMs;
        public final long endMs;

        public VocalRegion(long startMs, long endMs) {
            this.startMs = startMs;
            this.endMs = endMs;
        }

        public long getDuration() {
            return this.endMs - this.startMs;
        }
    }
}
