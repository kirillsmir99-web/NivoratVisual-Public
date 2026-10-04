package rtx.nv.utils.net;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.MinecraftClient;
import rtx.nv.utils.storage.AtomicFiles;
import rtx.nv.utils.storage.RepositoryStorage;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;
import java.util.concurrent.*;

/** Installation identity for social functions. Grants no account role or license. */
public final class SocialSession {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "nv-social-session"); thread.setDaemon(true); return thread;
    });
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static CompletableFuture<String> pending;
    private static String token = "", tokenName = "";
    private static long expiresAt, retryAt;
    private SocialSession() {}
    public static String name() {
        var session = MinecraftClient.getInstance().getSession();
        String name = session == null ? "Player" : session.getUsername();
        if (System.getProperty("fabric.client.gametest") != null) name = System.getProperty("nv.test.social.name", name);
        return name != null && name.matches("[A-Za-z0-9_]{3,16}") ? name : "Player";
    }
    public static synchronized void invalidate() { token = ""; expiresAt = 0; }
    private static synchronized CompletableFuture<String> ensure() {
        String currentName = name();
        if (Boolean.getBoolean("nv.social.offline")) return CompletableFuture.failedFuture(new IllegalStateException("Social test isolation"));
        if (!token.isEmpty() && tokenName.equals(currentName) && System.currentTimeMillis() < expiresAt)
            return CompletableFuture.completedFuture(token);
        if (pending != null && !pending.isDone()) return pending;
        if (System.currentTimeMillis() < retryAt) return CompletableFuture.failedFuture(new IllegalStateException("Social reconnect delay"));
        retryAt = System.currentTimeMillis()+5000;
        pending = CompletableFuture.supplyAsync(() -> {
            try {
                Path path = RepositoryStorage.root().resolve("social-session.private.json");
                JsonObject credentials;
                if (Files.exists(path)) {
                    try (var input = Files.newInputStream(path)) {
                        credentials = JsonParser.parseString(new String(BoundedInput.read(input,4096), StandardCharsets.UTF_8)).getAsJsonObject();
                    }
                } else {
                    byte[] secret = new byte[32]; new SecureRandom().nextBytes(secret);
                    credentials = new JsonObject();
                    credentials.addProperty("installation", UUID.randomUUID().toString());
                    credentials.addProperty("secret", HexFormat.of().formatHex(secret));
                    AtomicFiles.writeUtf8(path, credentials.toString());
                }
                credentials.addProperty("name", currentName);
                HttpRequest request = HttpRequest.newBuilder(URI.create(Endpoints.irc()+"/api/session"))
                    .timeout(Duration.ofSeconds(6)).header("Content-Type","application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(credentials.toString(), StandardCharsets.UTF_8)).build();
                HttpResponse<byte[]> response = HTTP.send(request, LimitedHttpBody.bytes(4096));
                if (response.statusCode() != 200) throw new IllegalStateException("Social session HTTP "+response.statusCode());
                JsonObject result = JsonParser.parseString(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject();
                String newToken = result.get("token").getAsString();
                if (!newToken.matches("[A-Za-z0-9_-]{32,128}")) throw new IllegalStateException("Invalid session response");
                synchronized (SocialSession.class) {
                    token = newToken; tokenName = currentName;
                    expiresAt = System.currentTimeMillis()+Math.max(60,Math.min(86400,result.get("expiresIn").getAsInt())-60)*1000L;
                }
                return newToken;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt(); throw new CompletionException(interrupted);
            } catch (Exception failure) { throw new CompletionException(failure); }
        }, IO);
        return pending;
    }
    /** Called on IRC/presence IO workers, never on the render thread. */
    public static HttpRequest.Builder authorize(HttpRequest.Builder builder) throws Exception {
        return builder.header("Authorization", "Bearer "+ensure().get(8, TimeUnit.SECONDS));
    }
    public static CompletableFuture<WebSocket> connect(WebSocket.Builder builder, URI uri, WebSocket.Listener listener) {
        return ensure().thenCompose(value -> builder.header("Authorization", "Bearer "+value).buildAsync(uri, listener))
            .whenComplete((socket, failure) -> {
                Throwable cause = failure;
                while (cause instanceof CompletionException && cause.getCause() != null) cause = cause.getCause();
                if (cause instanceof WebSocketHandshakeException handshake &&
                    (handshake.getResponse().statusCode() == 401 || handshake.getResponse().statusCode() == 403)) invalidate();
            });
    }
}
