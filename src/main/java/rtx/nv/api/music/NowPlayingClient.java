package rtx.nv.api.music;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class NowPlayingClient {
    private static NowPlayingClient INSTANCE;

    private final AtomicReference<TrackState> currentState = new AtomicReference<>(TrackState.INACTIVE);
    private final CoverTextureManager coverManager;
    private ScheduledExecutorService scheduler;
    private String baseUrl;
    private String lastCoverHash = null;
    private int backoffMs = 250;
    private volatile boolean connected = false;
    private volatile long lastSeekTimeMs = 0L;
    private volatile String lastSeekTrackKey = "";
    private volatile long lastValidStateMs;

    public NowPlayingClient(CoverTextureManager coverManager) {
        this.coverManager = coverManager;
        INSTANCE = this;
    }

    public static NowPlayingClient get() {
        return INSTANCE;
    }

    public TrackState getState() {
        return this.currentState.get();
    }

    private volatile boolean started = false;
    private volatile int currentPort = -1;

    public int getPort() { return this.currentPort; }

    public boolean isConnected() {
        return this.connected;
    }

    public boolean isStarted() {
        return this.started && this.scheduler != null && !this.scheduler.isShutdown();
    }

    public synchronized void start(int port) {
        if (this.isStarted() && this.currentPort == port) {
            return;
        }
        this.stop();
        this.currentPort = port;
        this.baseUrl = "http://127.0.0.1:" + port;
        this.started = true;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "NV-MusicPoller");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduleNextPoll(0);
    }

    public synchronized void stop() {
        this.started = false;
        this.currentPort = -1;
        if (this.scheduler != null) {
            this.scheduler.shutdownNow();
            this.scheduler = null;
        }
        this.connected = false;
        this.currentState.set(TrackState.INACTIVE);
        this.lastValidStateMs = 0;
        this.lastTrackKey = "";
        this.lastCoverHash = null;
        this.coverRequest.incrementAndGet();
    }

    private void scheduleNextPoll(int delayMs) {
        if (this.scheduler != null && !this.scheduler.isShutdown()) {
            this.scheduler.schedule(this::poll, (long) Math.max(0, delayMs), TimeUnit.MILLISECONDS);
        }
    }

    private volatile String lastTrackKey = "";
    private final java.util.concurrent.atomic.AtomicLong coverRequest = new java.util.concurrent.atomic.AtomicLong();

    private void poll() {
        try {
            URL url = URI.create(this.baseUrl + "/now-playing").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(800);
            conn.setReadTimeout(800);
            conn.setUseCaches(false);

            int code = conn.getResponseCode();
            long timestamp = System.currentTimeMillis();

            if (code == 200) {
                this.connected = true;
                this.backoffMs = 250;

                String jsonStr;
                try (InputStream in = conn.getInputStream()) {
                    jsonStr = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }

                JsonObject json = JsonParser.parseString(jsonStr).getAsJsonObject();
                AudioSpectrum.update(json.has("audioBands") && json.get("audioBands").isJsonArray() ? json.getAsJsonArray("audioBands") : null);
                if (json.has("audioAvailable") && json.get("audioAvailable").getAsBoolean()) this.backoffMs = 100;
                boolean active = json.has("isActive") && json.get("isActive").getAsBoolean();

                if (active) {
                    String title = getString(json, "title", "");
                    String artist = getString(json, "artist", "");
                    String album = getString(json, "album", "");
                    String status = getString(json, "status", "stopped");
                    long pos = json.has("positionMs") ? json.get("positionMs").getAsLong() : 0L;
                    long dur = json.has("durationMs") ? json.get("durationMs").getAsLong() : 0L;
                    String cover = json.has("coverHash") && !json.get("coverHash").isJsonNull() ? json.get("coverHash").getAsString() : null;
                    float speed = json.has("playbackSpeed") && !json.get("playbackSpeed").isJsonNull() ? json.get("playbackSpeed").getAsFloat() : 1.0f;

                    if (System.currentTimeMillis() - this.lastSeekTimeMs < 1200L) {
                        TrackState old = this.currentState.get();
                        if (old != null && old.isActive() && lastSeekTrackKey.equals(artist + "___" + title)) {
                            pos = old.currentPositionMs();
                        }
                    }

                    if (isNonMusicSource(title, getString(json, "sourceId", ""))) {
                        this.currentState.set(TrackState.INACTIVE);
                    } else {
                        TrackState old = this.currentState.get();
                        boolean sameTrack = old.isActive() && old.title().equals(title) && old.artist().equals(artist);
                        if (sameTrack && old.isPlaying() && "playing".equals(status) && Math.abs(pos - old.currentPositionMs()) < 700L) {
                            // Suppress sub-second SMTC sampling jitter without hiding a real seek.
                            long predicted = old.currentPositionMs();
                            pos = predicted + Math.round((pos - predicted) * .15);
                        }
                        TrackState state = new TrackState(true, title, artist, album, status, pos, dur, cover, timestamp, speed);
                        this.lastValidStateMs = timestamp;
                        this.currentState.set(state);

                        String trackKey = title + " - " + artist;
                        if (!trackKey.equals(this.lastTrackKey) || (cover != null && !cover.equals(this.lastCoverHash))) {
                            this.lastTrackKey = trackKey;
                            this.fetchCoverAsync(cover);
                        }
                    }
                } else {
                    this.handleMissingState(timestamp);
                }
            } else {
                this.connected = false;
                this.handleMissingState(timestamp);
            }
            conn.disconnect();
        } catch (Exception e) {
            this.connected = false;
            this.handleMissingState(System.currentTimeMillis());
            this.backoffMs = Math.min(Math.max(500, this.backoffMs * 2), 5000);
        }

        this.scheduleNextPoll(this.backoffMs);
    }

    private void handleMissingState(long now) {
        TrackState old = currentState.get();
        if (old.isActive() && now - lastValidStateMs <= 1500L) {
            if (old.isPlaying()) currentState.set(new TrackState(true, old.title(), old.artist(), old.album(), "paused",
                old.currentPositionMs(), old.durationMs(), old.coverHash(), now, old.playbackSpeed()));
        } else currentState.set(TrackState.INACTIVE);
    }

    private static boolean isNonMusicSource(String title, String sourceId) {
        if (title == null || title.isBlank()) return true;
        String source = sourceId == null ? "" : sourceId.toLowerCase(java.util.Locale.ROOT);
        String text = title.toLowerCase(java.util.Locale.ROOT).trim();
        return source.contains("telegram") || source.contains("whatsapp") || source.contains("discord")
            || text.startsWith("voice_") || text.startsWith("audio_")
            || text.contains("voice message") || text.contains("голосовое сообщение")
            || text.contains("видеосообщение") || text.contains("аудиосообщение");
    }

    public CompletableFuture<Boolean> setPreferredSource(String filter) {
        if (!java.util.Set.of("auto", "yandex", "vk", "spotify", "browser").contains(filter)) return CompletableFuture.completedFuture(false);
        return this.sendControl("/source?filter=" + filter);
    }

    private void fetchCoverAsync(String hash) {
        long request = coverRequest.incrementAndGet();
        String endpoint = this.baseUrl;
        CompletableFuture.runAsync(() -> this.fetchCover(hash, request, endpoint));
    }

    private void fetchCover(String hash, long request, String endpoint) {
        try {
            URL url = URI.create(endpoint + "/cover").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(1500);

            if (conn.getResponseCode() == 200) {
                String str;
                try (InputStream in = conn.getInputStream()) {
                    str = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                JsonObject json = JsonParser.parseString(str).getAsJsonObject();
                String base64 = json.has("coverBase64") && !json.get("coverBase64").isJsonNull() ? json.get("coverBase64").getAsString() : null;
                if (this.isStarted() && request == coverRequest.get()) {
                    this.lastCoverHash = hash;
                    this.coverManager.updateCover(base64);
                }
            }
            conn.disconnect();
        } catch (Exception ignored) {
        }
    }

    private static String getString(JsonObject obj, String key, String def) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : def;
    }

    public void sendPlayPause() {
        this.sendControl("/control/play-pause");
    }

    public void sendNext() {
        this.sendControl("/control/next");
    }

    public void sendPrevious() {
        this.sendControl("/control/previous");
    }

    public void sendSeek(long posMs) {
        this.lastSeekTimeMs = System.currentTimeMillis();
        TrackState cur = this.currentState.get();
        this.lastSeekTrackKey = cur.artist() + "___" + cur.title();
        if (cur != null && cur.isActive()) {
            this.currentState.set(cur.withPosition(posMs));
        }
        this.sendControl("/control/seek?pos=" + posMs);

    }

    private CompletableFuture<Boolean> sendControl(String path) {
        if (!this.isStarted() || this.baseUrl == null) return CompletableFuture.completedFuture(false);
        final String endpoint = this.baseUrl;
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = URI.create(endpoint + path).toURL();
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(1000);
                conn.setReadTimeout(1000);
                conn.setDoOutput(true);
                conn.getOutputStream().close();
                int status = conn.getResponseCode();
                conn.disconnect();
                return status >= 200 && status < 300;
            } catch (Exception ignored) {
                return false;
            }
        });
    }
}
