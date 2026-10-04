package rtx.nv.api.music.lyrics;

import net.minecraft.util.math.Vec3d;

public final class SkyWordBubble {
    private final Vec3d worldPos;
    private final String text;
    private final long spawnTime;
    private final long durationMs;

    public SkyWordBubble(Vec3d worldPos, String text, long durationMs) {
        this.worldPos = worldPos;
        this.text = text != null ? text.trim() : "";
        this.spawnTime = System.currentTimeMillis();
        this.durationMs = Math.max(1200L, durationMs);
    }

    public Vec3d getWorldPos() {
        return this.worldPos;
    }

    public Vec3d getWorldPos(long now) {
        return this.worldPos;
    }

    public String getText() {
        return this.text;
    }

    public boolean isExpired(long now) {
        return now - this.spawnTime >= this.durationMs;
    }

    public float getAlpha(long now) {
        long age = now - this.spawnTime;
        if (age < 0L || age >= this.durationMs) {
            return 0.0f;
        }

        long fadeIn = 350L;
        long fadeOut = 650L;

        if (age < fadeIn) {
            return (float) age / (float) fadeIn;
        }

        long remain = this.durationMs - age;
        if (remain < fadeOut) {
            return (float) remain / (float) fadeOut;
        }

        return 1.0f;
    }
}
