package rtx.nv.utils.net;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.minecraft.client.MinecraftClient;
import rtx.nv.utils.net.Endpoints;
import rtx.nv.utils.profile.ProfileIdentity;

public final class ClientPresence {
    public static final ClientPresence INSTANCE = new ClientPresence();
    private static final long POLL_MS = 2000L;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1L)).build();
    private final Set<String> online = ConcurrentHashMap.newKeySet();
    private ScheduledExecutorService executor;
    private volatile long epoch;
    private volatile long lastSuccessfulPollNs;
    private static final long CACHE_TTL_NS = TimeUnit.SECONDS.toNanos(10);

    private ClientPresence() {
    }

    public synchronized void start() {
        if (this.executor != null) {
            return;
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "nv-presence");
            thread.setDaemon(true);
            return thread;
        });
        long attempt = ++epoch;
        this.executor.scheduleWithFixedDelay(() -> poll(attempt), 0L, POLL_MS, TimeUnit.MILLISECONDS);
    }

    private void poll(long attempt) {
        try {
            String string = URLEncoder.encode(this.selfName(), StandardCharsets.UTF_8);
            URI uRI = URI.create(Endpoints.irc() + "/api/presence?me=" + string);
            HttpRequest httpRequest = SocialSession.authorize(HttpRequest.newBuilder(uRI).timeout(Duration.ofSeconds(3L)).header("Accept", "application/json").GET()).build();
            HttpResponse<byte[]> httpResponse = this.httpClient.send(httpRequest, LimitedHttpBody.bytes(65536));
            if (httpResponse.statusCode() == 401) SocialSession.invalidate();
            if (httpResponse.statusCode() != 200) {
                return;
            }
            JsonObject jsonObject = JsonParser.parseString(new String(httpResponse.body(), StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement jsonElement = jsonObject.get("online");
            if (jsonElement == null || !jsonElement.isJsonArray()) {
                return;
            }
            ConcurrentHashMap.KeySetView keySetView = ConcurrentHashMap.newKeySet();
            for (JsonElement jsonElement2 : jsonElement.getAsJsonArray()) {
                String string2;
                if (!jsonElement2.isJsonPrimitive() || (string2 = jsonElement2.getAsString().trim().toLowerCase(Locale.ROOT)).isEmpty()) continue;
                keySetView.add(string2);
            }
            synchronized (this) {
                if (attempt != epoch || executor == null) return;
                this.online.clear();
                this.online.addAll(keySetView);
                this.lastSuccessfulPollNs = System.nanoTime();
            }
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public synchronized void stop() {
        ++epoch;
        if (this.executor == null) {
            return;
        }
        this.executor.shutdownNow();
        this.executor = null;
        this.online.clear();
        this.lastSuccessfulPollNs = 0;
    }

    private String selfName() { return SocialSession.name(); }

    public boolean hasOnline() {
        return cacheFresh() && !this.online.isEmpty();
    }

    private boolean cacheFresh() {
        long success = lastSuccessfulPollNs;
        return success != 0 && System.nanoTime() - success <= CACHE_TTL_NS;
    }

    public boolean isNvUser(String string) {
        if (string == null || string.isBlank() || !hasOnline()) {
            return false;
        }
        return this.online.contains(string.trim().toLowerCase(Locale.ROOT));
    }

    public boolean isNvDisplay(String string) {
        if (string == null || string.isBlank() || !hasOnline()) {
            return false;
        }
        for (String string2 : string.toLowerCase(Locale.ROOT).split("[^a-z0-9_]+")) {
            if (string2.isEmpty() || !this.online.contains(string2)) continue;
            return true;
        }
        return false;
    }
}
