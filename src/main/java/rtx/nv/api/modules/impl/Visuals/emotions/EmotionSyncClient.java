package rtx.nv.api.modules.impl.Visuals.emotions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import rtx.nv.api.modules.impl.Visuals.emotions.Emotion;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionRemoteState;

public final class EmotionSyncClient {
    private final Gson gson = new Gson();
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private final Map<String, EmotionRemoteState> remote = new ConcurrentHashMap<String, EmotionRemoteState>();
    private volatile long connectionEpoch;
    private volatile WebSocket socket;
    private volatile boolean connecting;
    private volatile String identity = "";
    private volatile String username = "";
    private volatile String world = "";
    private volatile Emotion emotion;
    private volatile long startedAt;
    private volatile float speed = 1.0f;
    private volatile boolean looping;

    public Map<String, EmotionRemoteState> snapshot() {
        return new HashMap<String, EmotionRemoteState>(this.remote);
    }

    private static String clean(String string, int n) {
        if (string == null) return "";
        String value = string.replace("\u00a7", "").trim();
        return value.substring(0, Math.min(n, value.length()));
    }

    private static String read(JsonObject jsonObject, String string) {
        return jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive() ? jsonObject.get(string).getAsString() : "";
    }

    private static long number(JsonObject jsonObject, String string, long l) {
        try {
            return jsonObject.has(string) ? jsonObject.get(string).getAsLong() : l;
        }
        catch (Exception exception) {
            return l;
        }
    }

    public void push() {
        WebSocket webSocket = this.socket;
        if (webSocket == null || this.identity.isBlank()) {
            return;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("t", "u");
        jsonObject.addProperty("i", this.identity);
        jsonObject.addProperty("n", this.username);
        jsonObject.addProperty("w", this.world);
        jsonObject.addProperty("e", this.emotion == null ? "" : this.emotion.name());
        jsonObject.addProperty("s", (Number)(Object)this.startedAt);
        jsonObject.addProperty("v", (Number)Float.valueOf(this.speed));
        jsonObject.addProperty("l", Boolean.valueOf(this.looping));
        webSocket.sendText(this.gson.toJson((JsonElement)jsonObject), true);
    }

    public synchronized void connect(String string, int n) {
        if (this.socket != null || this.connecting || this.identity.isBlank()) {
            return;
        }
        long attempt = ++this.connectionEpoch;
        this.connecting = true;
        try {
            rtx.nv.utils.net.SocialSession.connect(this.http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(6L)), URI.create(rtx.nv.utils.net.Endpoints.sync("emotions")), new Listener(this, attempt)).whenComplete((webSocket, throwable) -> {
                synchronized (this) {
                if (attempt != this.connectionEpoch) { if (webSocket != null) webSocket.abort(); return; }
                if (throwable != null) {
                    this.connecting = false;
                    this.socket = null;
                    this.remote.clear();
                }
                }
            });
        }
        catch (Exception exception) {
            this.connecting = false;
        }
    }

    private static double decimal(JsonObject jsonObject, String string, double d) {
        try {
            double value = jsonObject.has(string) ? jsonObject.get(string).getAsDouble() : d;
            return Double.isFinite(value) ? Math.clamp(value, 0.05, 4.0) : d;
        }
        catch (Exception exception) {
            return d;
        }
    }

    public boolean isConnected() {
        return this.socket != null;
    }

    private void receive(String string) {
        try {
            JsonObject jsonObject = JsonParser.parseString((String)string).getAsJsonObject();
            if (!"d".equals(EmotionSyncClient.read(jsonObject, "t"))) {
                return;
            }
            if (jsonObject.has("p") && jsonObject.get("p").isJsonArray()) {
                for (JsonElement jsonElement : jsonObject.getAsJsonArray("p")) {
                    Emotion emotion;
                    if (!jsonElement.isJsonObject()) continue;
                    JsonObject jsonObject2 = jsonElement.getAsJsonObject();
                    String string2 = EmotionSyncClient.clean(EmotionSyncClient.read(jsonObject2, "i"), 96);
                    String string3 = EmotionSyncClient.clean(EmotionSyncClient.read(jsonObject2, "n"), 32);
                    try {
                        emotion = Emotion.valueOf(EmotionSyncClient.read(jsonObject2, "e"));
                    }
                    catch (Exception exception) {
                        continue;
                    }
                    if (string2.isBlank() || string3.isBlank()) continue;
                    if (this.remote.size() >= 128 && !this.remote.containsKey(string2)) continue;
                    this.remote.put(string2, new EmotionRemoteState(string2, string3, EmotionSyncClient.clean(EmotionSyncClient.read(jsonObject2, "w"), 64), emotion, EmotionSyncClient.number(jsonObject2, "s", 0L), (float)EmotionSyncClient.decimal(jsonObject2, "v", 1.0), EmotionSyncClient.bool(jsonObject2, "l")));
                }
            }
            if (jsonObject.has("r") && jsonObject.get("r").isJsonArray()) {
                for (JsonElement jsonElement : jsonObject.getAsJsonArray("r")) {
                    if (!jsonElement.isJsonPrimitive()) continue;
                    this.remote.remove(EmotionSyncClient.clean(jsonElement.getAsString(), 96));
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public synchronized void disconnect() {
        ++this.connectionEpoch;
        this.remote.clear();
        WebSocket webSocket = this.socket;
        this.socket = null;
        this.connecting = false;
        if (webSocket != null) {
            webSocket.sendClose(1000, "bye");
        }
    }

    public boolean isConnecting() {
        return this.connecting;
    }

    public void setLocalState(String string, String string2, String string3, Emotion emotion, long l, float f, boolean bl) {
        this.username = EmotionSyncClient.clean(string2, 32);
        this.identity = EmotionSyncClient.clean(string, 96);
        this.world = EmotionSyncClient.clean(string3, 64);
        this.emotion = emotion;
        this.startedAt = l;
        this.speed = Math.max(0.05f, f);
        this.looping = bl;
    }

    private static boolean bool(JsonObject jsonObject, String string) {
        return jsonObject.has(string) && jsonObject.get(string).getAsBoolean();
    }

    private class Listener implements java.net.http.WebSocket.Listener {
        private final StringBuilder buffer = new StringBuilder();

        private final long attempt;
        Listener(EmotionSyncClient client, long attempt) { this.attempt = attempt; }

        @Override
        public void onOpen(java.net.http.WebSocket ws) {
            synchronized (EmotionSyncClient.this) {
            if (attempt != connectionEpoch) { ws.abort(); return; }
            socket = ws;
            connecting = false;
            push();
            ws.request(1);
            }
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket ws, CharSequence data, boolean last) {
            if (ws != socket) return null;
            if (!rtx.nv.utils.net.WebSocketMessages.append(buffer, ws, data)) return null;
            if (last) {
                receive(buffer.toString());
                buffer.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket ws, int statusCode, String reason) {
            if (ws != socket) return null;
            socket = null;
            connecting = false;
            remote.clear();
            return null;
        }

        @Override
        public void onError(java.net.http.WebSocket ws, Throwable error) {
            if (ws != socket) return;
            socket = null;
            connecting = false;
            remote.clear();
        }
    }
}

