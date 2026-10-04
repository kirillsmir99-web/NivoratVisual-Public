package rtx.nv.api.music;

public final class TrackState {
    public static final TrackState INACTIVE = new TrackState(false, "", "", "", "stopped", 0L, 0L, null, 0L, 1.0f);

    private final boolean active;
    private final String title;
    private final String artist;
    private final String album;
    private final String status;
    private final long positionMs;
    private final long durationMs;
    private final String coverHash;
    private final long updateTimestamp;
    private final float playbackSpeed;

    public TrackState(boolean active, String title, String artist, String album, String status,
                      long positionMs, long durationMs, String coverHash, long updateTimestamp, float playbackSpeed) {
        this.active = active;
        this.title = title != null ? title.trim() : "";
        this.artist = artist != null ? artist.trim() : "";
        this.album = album != null ? album.trim() : "";
        this.status = status != null ? status.toLowerCase() : "stopped";
        this.positionMs = Math.max(0L, positionMs);
        this.durationMs = Math.max(0L, durationMs);
        this.coverHash = coverHash;
        this.updateTimestamp = updateTimestamp > 0L ? updateTimestamp : System.currentTimeMillis();
        this.playbackSpeed = playbackSpeed > 0.0f ? playbackSpeed : 1.0f;
    }

    public TrackState withPosition(long posMs) {
        return new TrackState(this.active, this.title, this.artist, this.album, this.status, posMs, this.durationMs, this.coverHash, System.currentTimeMillis(), this.playbackSpeed);
    }

    public boolean isActive() {
        return this.active && (!this.title.isEmpty() || !this.artist.isEmpty());
    }

    public boolean isPlaying() {
        return this.isActive() && "playing".equalsIgnoreCase(this.status);
    }

    public boolean isPaused() {
        return this.isActive() && "paused".equalsIgnoreCase(this.status);
    }

    public String title() {
        return this.title.isEmpty() ? "Неизвестный трек" : this.title;
    }

    public String artist() {
        return this.artist.isEmpty() ? "Неизвестный исполнитель" : this.artist;
    }

    public String album() {
        return this.album;
    }

    public String status() {
        return this.status;
    }

    public String coverHash() {
        return this.coverHash;
    }

    public long durationMs() {
        return this.durationMs;
    }

    public long positionMs() {
        return this.positionMs;
    }

    public long currentPositionMs() {
        if (!this.isPlaying()) {
            return this.positionMs;
        }
        long elapsed = System.currentTimeMillis() - this.updateTimestamp;
        long extrapolated = this.positionMs + (long) (elapsed * this.playbackSpeed);
        if (this.durationMs > 0L) {
            return Math.min(extrapolated, this.durationMs);
        }
        return Math.max(0L, extrapolated);
    }

    public float progress() {
        if (this.durationMs <= 0L) {
            return 0.0f;
        }
        float p = (float) this.currentPositionMs() / (float) this.durationMs;
        return Math.max(0.0f, Math.min(1.0f, p));
    }

    public float playbackSpeed() {
        return this.playbackSpeed;
    }

    public long getInterpolatedPositionMs() {
        return this.currentPositionMs();
    }

    public float getProgress() {
        return this.progress();
    }

    public String formattedPosition() {
        return formatTime(this.currentPositionMs());
    }

    public String formattedDuration() {
        return formatTime(this.durationMs);
    }

    public static String formatTime(long ms) {
        if (ms <= 0L) {
            return "00:00";
        }
        long totalSec = ms / 1000L;
        long min = totalSec / 60L;
        long sec = totalSec % 60L;
        if (min >= 60L) {
            long hr = min / 60L;
            min = min % 60L;
            return String.format("%d:%02d:%02d", hr, min, sec);
        }
        return String.format("%02d:%02d", min, sec);
    }
}
