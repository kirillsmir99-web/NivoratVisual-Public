package rtx.nv.api.party;

import net.minecraft.util.math.Vec3d;

public record PartyMarker(
    String id,
    String from,
    double x,
    double y,
    double z,
    String dim,
    long createdAt,
    long ttl,
    int color,
    boolean rainbow
) {
    public boolean isExpired() {
        return System.currentTimeMillis() > createdAt + ttl;
    }

    public boolean expired(long now) {
        return now > createdAt + ttl;
    }

    public long ageMs(long now) {
        return Math.max(0L, now - createdAt);
    }

    public long remainingMs(long now) {
        return Math.max(0L, createdAt + ttl - now);
    }

    public Vec3d pos() {
        return new Vec3d(x, y, z);
    }

    public boolean finiteAndSafe() {
        return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
    }

    public void forceExpireSoon(long time) {
    }
}