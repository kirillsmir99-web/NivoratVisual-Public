package rtx.nv.api.music.lyrics;

import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;

public class SubtitleBubble {
    public final LyricLine line;
    public final long spawnSystemTimeMs;
    public float screenX;
    public float screenY;
    public Vec3d worldPos;
    public Quaternionf worldRot;
    public float worldScale = 1f;
    private long retiredAt;

    public void retire(long now) { if (retiredAt == 0) retiredAt = now; }

    public boolean isSurfaceAttached = false;
    public boolean isFastPaced = false;
    public boolean isLiveCaption = false;
    public long liveDurationMs = 3500L;
    public net.minecraft.util.math.BlockPos attachedBlockPos = null;
    public boolean isLongLine = false;

    private Vec3d pendingRepositionTarget = null;
    private long repositionTransitionStartMs = 0L;
    private static final long REPOSITION_FADE_MS = 120L;

    public void startReposition(Vec3d newPos) {
        if (newPos == null || (pendingRepositionTarget != null && pendingRepositionTarget.equals(newPos))) return;
        this.pendingRepositionTarget = newPos;
        this.repositionTransitionStartMs = System.currentTimeMillis();
    }

    public boolean isRepositioning() {
        return this.pendingRepositionTarget != null;
    }

    public float getRepositionAlphaMultiplier(long now) {
        if (pendingRepositionTarget == null) return 1.0F;
        long elapsed = now - repositionTransitionStartMs;
        if (elapsed < REPOSITION_FADE_MS) {
            return Math.max(0.0F, 1.0F - (float) elapsed / (float) REPOSITION_FADE_MS);
        } else if (elapsed < REPOSITION_FADE_MS * 2) {
            if (pendingRepositionTarget != null) {
                this.worldPos = this.pendingRepositionTarget;
                this.pendingRepositionTarget = null;
            }
            float progress = (float) (elapsed - REPOSITION_FADE_MS) / (float) REPOSITION_FADE_MS;
            return Math.min(1.0F, progress);
        } else {
            if (pendingRepositionTarget != null) {
                this.worldPos = this.pendingRepositionTarget;
                this.pendingRepositionTarget = null;
            }
            return 1.0F;
        }
    }

    public SubtitleBubble(LyricLine line, float screenX, float screenY) {
        this(line, screenX, screenY, false);
    }

    public SubtitleBubble(LyricLine line, float screenX, float screenY, boolean isFastPaced) {
        this.line = line;
        this.spawnSystemTimeMs = System.currentTimeMillis();
        this.screenX = screenX;
        this.screenY = screenY;
        this.isFastPaced = isFastPaced;
        this.isLongLine = line != null && line.text != null && (line.text.length() >= 28 || (line.words != null && line.words.size() >= 5));
    }

    public SubtitleBubble(LyricLine line, Vec3d worldPos, Quaternionf worldRot) {
        this(line, worldPos, worldRot, false, false);
    }

    public SubtitleBubble(LyricLine line, Vec3d worldPos, Quaternionf worldRot, boolean isSurfaceAttached) {
        this(line, worldPos, worldRot, isSurfaceAttached, false);
    }

    public SubtitleBubble(LyricLine line, Vec3d worldPos, Quaternionf worldRot, boolean isSurfaceAttached, boolean isFastPaced) {
        this.line = line;
        this.spawnSystemTimeMs = System.currentTimeMillis();
        this.worldPos = worldPos;
        this.worldRot = worldRot;
        this.isSurfaceAttached = isSurfaceAttached;
        this.isFastPaced = isFastPaced;
        this.isLongLine = line != null && line.text != null && (line.text.length() >= 28 || (line.words != null && line.words.size() >= 5));
    }

    public float getScale(long now) {
        String anim = MusicSubtitlesConfig.INSTANCE.subtitlesAnimation;
        if (anim == null) {
            anim = "KARAOKE_BOUNCE";
        }

        long elapsed = now - this.spawnSystemTimeMs;
        if (elapsed < 0L) {
            return 0.0F;
        } else if (this.isFastPaced) {
            if (elapsed < 60L) {
                float progress = (float) elapsed / 60.0F;
                return 0.85F + 0.18F * easeOutBack(progress);
            } else {
                return 1.0F;
            }
        } else {
            String upper = anim.toUpperCase();
            switch (upper) {
                case "ELASTIC_POP":
                    if (elapsed < 450L) {
                        float p = (float) elapsed / 450.0F;
                        return 0.5F + 0.5F * (1.0F + 0.75F * (float) (Math.sin(p * Math.PI * 2.5) * Math.exp(-p * 3.5)));
                    }
                    return 1.0F;
                case "WAVE_FLOAT":
                    float base = elapsed < 300L ? 0.7F + 0.3F * easeOutCubic((float) elapsed / 300.0F) : 1.0F;
                    return base + 0.035F * (float) Math.sin((now - this.spawnSystemTimeMs) / 380.0);
                case "NV_FLOW":
                    return elapsed < 260L ? .96f + .04f * easeOutCubic(elapsed / 260f) : 1f;
                case "SLIDE_FADE":
                    if (elapsed < 280L) {
                        return 0.82F + 0.18F * easeOutCubic((float) elapsed / 280.0F);
                    }
                    return 1.0F;
                case "PULSE_GLOW":
                    float pulseBase = elapsed < 220L ? (float) elapsed / 220.0F : 1.0F;
                    return pulseBase + 0.045F * (float) Math.sin((now - this.spawnSystemTimeMs) / 180.0);
                case "CLASSIC":
                    if (elapsed < 180L) {
                        return (float) elapsed / 180.0F;
                    }
                    return 1.0F;
                default:
                    if (elapsed < 320L) {
                        float p = (float) elapsed / 320.0F;
                        return 0.88F + 0.12F * easeOutCubic(p);
                    } else {
                        return 1.0F;
                    }
            }
        }
    }

    public float getLetterEntryOffset(int charIndex, long now) {
        if (!"NV_FLOW".equalsIgnoreCase(MusicSubtitlesConfig.INSTANCE.subtitlesAnimation)) return 0;
        float progress = Math.max(0f, Math.min(1f, (now - spawnSystemTimeMs - Math.min(charIndex * 7L, 120L)) / 260f));
        return 5f * (1f - easeOutCubic(progress));
    }

    public float getOffsetX(long now) {
        if (this.isFastPaced) {
            return 0.0F;
        } else {
            String anim = MusicSubtitlesConfig.INSTANCE.subtitlesAnimation;
            long elapsed = now - this.spawnSystemTimeMs;
            float idle = (float) Math.cos((now - this.spawnSystemTimeMs) / 840.0) * 1.0F;
            if ("SLIDE_FADE".equalsIgnoreCase(anim)) {
                if (elapsed < 320L) {
                    float p = (float) elapsed / 320.0F;
                    return -28.0F * (1.0F - p * p * p) + idle;
                }
            } else if ("WAVE_FLOAT".equalsIgnoreCase(anim)) {
                return (float) Math.sin((now - this.spawnSystemTimeMs) / 600.0) * 2.5F + idle;
            }
            return "NV_FLOW".equalsIgnoreCase(anim) ? 0f : idle;
        }
    }

    public float getOffsetY(long now) {
        String anim = MusicSubtitlesConfig.INSTANCE.subtitlesAnimation;
        long elapsed = now - this.spawnSystemTimeMs;
        float idle = (float) Math.sin((now - this.spawnSystemTimeMs) / 520.0) * 1.8F;
        float entryOffset = 0.0F;
        if (this.isFastPaced) {
            if (elapsed < 60L) {
                float p = (float) elapsed / 60.0F;
                entryOffset = 3.0F * (1.0F - p * p);
            }
            return entryOffset;
        } else {
            if (elapsed < 320L) {
                float p = (float) elapsed / 320.0F;
                entryOffset = 10.0F * (1.0F - easeOutCubic(p));
            }
            if ("WAVE_FLOAT".equalsIgnoreCase(anim)) {
                return (float) Math.sin((now - this.spawnSystemTimeMs) / 320.0) * 3.5F + idle + entryOffset;
            } else if ("SLIDE_FADE".equalsIgnoreCase(anim) && elapsed < 280L) {
                float p = (float) elapsed / 280.0F;
                return 10.0F * (1.0F - p * p) + idle;
            } else {
                return "NV_FLOW".equalsIgnoreCase(anim) ? entryOffset : idle + entryOffset;
            }
        }
    }

    public float getExitOffsetY(long trackPos) {
        if (this.isLiveCaption) {
            long liveElapsed = System.currentTimeMillis() - this.spawnSystemTimeMs;
            long remaining = this.liveDurationMs - liveElapsed;
            if (remaining < 250L && remaining > 0L) {
                float p = 1.0F - (float) remaining / 250.0F;
                return (this.isFastPaced ? -5.0F : -12.0F) * easeOutCubic(p);
            } else {
                return 0.0F;
            }
        } else if (trackPos > this.line.endMs) {
            long overdue = trackPos - this.line.endMs;
            float p = Math.min(1.0F, (float) overdue / (this.isFastPaced ? 140.0F : 400.0F));
            return (this.isFastPaced ? -5.0F : -12.0F) * easeOutCubic(p);
        } else {
            return 0.0F;
        }
    }

    public float getOverallAlpha(long trackPos, long now) {
        long elapsed = now - this.spawnSystemTimeMs;
        if (retiredAt > 0) return Math.max(0f, 1f - (now - retiredAt) / 220f);
        if (elapsed < 0L) {
            return 0.0F;
        } else {
            long fadeInDur = this.isFastPaced ? 45L : 250L;
            float alpha = 1.0F;
            if (elapsed < fadeInDur) {
                alpha = (float) elapsed / (float) fadeInDur;
            }

            float finalAlpha;
            if (this.isLiveCaption) {
                long remaining = this.liveDurationMs - elapsed;
                if (remaining <= 0L) {
                    finalAlpha = 0.0F;
                } else {
                    finalAlpha = remaining < 400L ? alpha * ((float) remaining / 400.0F) : alpha;
                }
            } else if (trackPos > this.line.endMs) {
                long overdue = trackPos - this.line.endMs;
                long fadeOutDur = this.isFastPaced ? 140L : 600L;
                if (overdue >= fadeOutDur) {
                    finalAlpha = 0.0F;
                } else {
                    float fade = 1.0F - (float) overdue / (float) fadeOutDur;
                    finalAlpha = alpha * fade;
                }
            } else {
                finalAlpha = alpha;
            }
            return finalAlpha * this.getRepositionAlphaMultiplier(now);
        }
    }

    public boolean isExpired(long trackPos, long now) {
        if (this.pendingRepositionTarget != null) return false;
        if (retiredAt > 0) return now - retiredAt >= 220;
        if (this.isLiveCaption) {
            return now - this.spawnSystemTimeMs > this.liveDurationMs;
        } else if (trackPos < this.line.startMs - 300L) {
            return true;
        } else {
            long overdueLimit = this.isFastPaced ? 150L : 600L;
            if (trackPos > this.line.endMs + overdueLimit) {
                return true;
            } else {
                return false; // Timed lyrics expire on the track clock, including long pauses.
            }
        }
    }

    private static float easeOutBack(float x) {
        float c1 = 1.70158F;
        float c3 = c1 + 1.0F;
        return 1.0F + c3 * (float) Math.pow(x - 1.0F, 3.0) + c1 * (float) Math.pow(x - 1.0F, 2.0);
    }

    private static float easeOutCubic(float x) {
        float f = x - 1.0F;
        return f * f * f + 1.0F;
    }
}
