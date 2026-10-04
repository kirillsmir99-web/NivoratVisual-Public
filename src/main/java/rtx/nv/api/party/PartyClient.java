package rtx.nv.api.party;
import com.google.gson.JsonObject;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import rtx.nv.api.party.PartyChat;
import rtx.nv.api.party.PartyInvite;
import rtx.nv.api.party.PartyMarker;
import rtx.nv.api.party.PartySnapshot;
import rtx.nv.utils.net.Endpoints;
import rtx.nv.utils.profile.ProfileIdentity;
import rtx.nv.utils.storage.RepositoryStorage;

public final class PartyClient {
    public static final PartyClient INSTANCE = new PartyClient();
    private static final int MAX_MARKERS = 64;
    private static final int MAX_VOICE_BACKLOG = 8;
    private static final int MAX_TEXT_CHARS = 1 << 20;
    private static final int MAX_VOICE_BYTES = 64 << 10;
    private static final int MAX_INVITES = 128;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5L)).build();
    private final AtomicReference<PartySnapshot> snapshot = new AtomicReference<PartySnapshot>(PartySnapshot.NONE);
    private final Map<String, PartyInvite> invites = new ConcurrentHashMap<String, PartyInvite>();
    private final CopyOnWriteArrayList<PartyMarker> markers = new CopyOnWriteArrayList();
    private final AtomicInteger voiceBacklog = new AtomicInteger();
    private ScheduledExecutorService executor;
    private volatile ExecutorService outbound;
    private volatile WebSocket socket;
    private volatile boolean connected;
    private volatile boolean connecting;
    private volatile long connectionEpoch;
    private volatile PartyVoiceSink voiceSink;
    private volatile int localColor;
    private volatile boolean localRainbow;
    private volatile boolean hasLocalColor;
    private volatile String clientId;
    private volatile String lastHelloName = "";

    private PartyClient() {
    }

    private static JsonObject obj(JsonObject jsonObject, String string) {
        return jsonObject != null && jsonObject.has(string) && jsonObject.get(string).isJsonObject() ? jsonObject.getAsJsonObject(string) : null;
    }

    private static String str(JsonObject jsonObject, String string) {
        return jsonObject != null && jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive() ? jsonObject.get(string).getAsString() : "";
    }

    private static String resolveName() { return rtx.nv.utils.net.SocialSession.name(); }

    public synchronized void start() {
        if (this.executor != null) {
            return;
        }
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "nv-party");
            thread.setDaemon(true);
            return thread;
        });
        this.outbound = new java.util.concurrent.ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
            new java.util.concurrent.ArrayBlockingQueue<>(256), runnable -> {
            Thread thread = new Thread(runnable, "nv-party-tx");
            thread.setDaemon(true);
            return thread;
        }, new java.util.concurrent.ThreadPoolExecutor.AbortPolicy());
        this.executor.scheduleWithFixedDelay(this::ensureConnected, 0L, 5L, TimeUnit.SECONDS);
    }

    public PartySnapshot snapshot() {
        return this.snapshot.get();
    }

    public boolean create(String string) {
        return this.send("create", jsonObject -> jsonObject.addProperty("name", string));
    }

    private void handle(JsonObject jsonObject) {
        switch (PartyClient.str(jsonObject, "type")) {
            case "welcome": {
                this.snapshot.set(PartySnapshot.fromJson(PartyClient.obj(jsonObject, "party")));
                break;
            }
            case "party": {
                PartySnapshot partySnapshot = PartySnapshot.fromJson(PartyClient.obj(jsonObject, "party"));
                this.snapshot.set(partySnapshot);
                if (!partySnapshot.exists()) {
                    this.markers.clear();
                }
                if (!PartyClient.bool(jsonObject, "info")) break;
                PartyClient.runGame(() -> PartyChat.renderInfo((PartySnapshot)partySnapshot));
                break;
            }
            case "invite": {
                String string = PartyClient.str(jsonObject, "invite");
                if (string.isEmpty()) break;
                String string2 = PartyClient.str(jsonObject, "from");
                String string3 = PartyClient.str(jsonObject, "party");
                long l = (long)PartyClient.num(jsonObject, "expires", 60000.0);
                long now = System.currentTimeMillis();
                this.invites.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
                if (this.invites.size() >= MAX_INVITES && !this.invites.containsKey(string)) break;
                l = Math.max(1000L, Math.min(300000L, l));
                this.invites.put(string, new PartyInvite(string, string2, string3, System.currentTimeMillis() + l));
                PartyClient.runGame(() -> PartyChat.printInvite((String)string2, (String)string3, (String)string));
                break;
            }
            case "notice": {
                String string = PartyClient.str(jsonObject, "message");
                if (string.isEmpty()) break;
                String string4 = PartyClient.str(jsonObject, "level");
                PartyClient.runGame(() -> PartyChat.printNotice((String)string4, (String)string));
                break;
            }
            case "chat": {
                String string = PartyClient.str(jsonObject, "from");
                String string5 = PartyClient.str(jsonObject, "text");
                if (string5.isEmpty()) break;
                PartyClient.runGame(() -> PartyChat.printChat((String)string, (String)string5));
                break;
            }
            case "marker": {
                PartyMarker partyMarker = new PartyMarker(PartyClient.str(jsonObject, "id"), PartyClient.str(jsonObject, "from"), PartyClient.num(jsonObject, "x", 0.0), PartyClient.num(jsonObject, "y", 0.0), PartyClient.num(jsonObject, "z", 0.0), PartyClient.str(jsonObject, "dim"), System.currentTimeMillis(), (long)PartyClient.num(jsonObject, "ttl", 8000.0), (int)PartyClient.num(jsonObject, "color", 0.0), PartyClient.bool(jsonObject, "rainbow"));
                if (!partyMarker.finiteAndSafe()) break;
                this.markers.add(partyMarker);
                while (this.markers.size() > 64) {
                    this.markers.remove(0);
                }
                break;
            }
            case "marker_remove": {
                String string = PartyClient.str(jsonObject, "id");
                if (string.isEmpty()) break;
                long l = System.currentTimeMillis();
                for (PartyMarker partyMarker : this.markers) {
                    if (!string.equals(partyMarker.id())) continue;
                    partyMarker.forceExpireSoon(l);
                }
                break;
            }
            case "error": {
                String string = PartyClient.str(jsonObject, "message");
                if (string.isEmpty()) break;
                PartyClient.runGame(() -> PartyChat.printNotice((String)"error", (String)string));
                break;
            }
        }
    }

    private static double num(JsonObject jsonObject, String string, double d) {
        try {
            double value = jsonObject != null && jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive() ? jsonObject.get(string).getAsDouble() : d;
            return Double.isFinite(value) ? value : d;
        }
        catch (Throwable throwable) {
            return d;
        }
    }

    public synchronized void stop() {
        ++this.connectionEpoch;
        if (this.executor != null) {
            this.executor.shutdownNow();
            this.executor = null;
        }
        if (this.outbound != null) {
            this.outbound.shutdownNow();
            this.outbound = null;
        }
        this.voiceBacklog.set(0);
        WebSocket webSocket = this.socket;
        this.socket = null;
        this.connected = false;
        this.connecting = false;
        if (webSocket != null) {
            try {
                webSocket.sendClose(1000, "bye");
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        this.markers.clear();
        this.invites.clear();
        this.snapshot.set(PartySnapshot.NONE);
    }

    public boolean isConnected() {
        return this.connected;
    }

    private synchronized String clientId() {
        block6: {
            int n = ProfileIdentity.uid();
            if (n > 0) {
                return Integer.toString(n);
            }
            if (this.clientId != null) {
                return this.clientId;
            }
            try {
                JsonObject jsonObject = RepositoryStorage.readObject("party");
                if (jsonObject.has("clientId") && jsonObject.get("clientId").isJsonPrimitive()) {
                    this.clientId = jsonObject.get("clientId").getAsString();
                }
                if (this.clientId == null || this.clientId.isBlank()) {
                    this.clientId = UUID.randomUUID().toString();
                    jsonObject.addProperty("clientId", this.clientId);
                    RepositoryStorage.write("party", jsonObject);
                }
            }
            catch (Throwable throwable) {
                if (this.clientId != null) break block6;
                this.clientId = UUID.randomUUID().toString();
            }
        }
        return this.clientId;
    }

    private void sendHello(WebSocket webSocket) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        String string = PartyClient.resolveName();
        String string2 = "";
        try {
            if (minecraftClient.getSession() != null && minecraftClient.getSession().getUuidOrNull() != null) {
                string2 = minecraftClient.getSession().getUuidOrNull().toString();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "hello");
        jsonObject.addProperty("id", this.clientId());
        jsonObject.addProperty("name", string);
        jsonObject.addProperty("uuid", string2);
        int n = ProfileIdentity.uid();
        if (n > 0) {
            jsonObject.addProperty("uid", (Number)n);
        }
        if (this.submitReliable(jsonObject.toString())) {
            this.lastHelloName = string;
        }
    }

    public void setLocalColor(int n, boolean bl) {
        this.localColor = n;
        this.localRainbow = bl;
        this.hasLocalColor = true;
        this.send("color", jsonObject -> {
            jsonObject.addProperty("color", (Number)n);
            jsonObject.addProperty("rainbow", Boolean.valueOf(bl));
        });
    }


    public boolean respondInvite(String string, boolean bl) {
        this.invites.remove(string);
        return this.send("invite_response", jsonObject -> {
            jsonObject.addProperty("invite", string);
            jsonObject.addProperty("accept", Boolean.valueOf(bl));
        });
    }

    private synchronized void ensureConnected() {
        if (this.executor == null) return;
        if (this.connected) {
            WebSocket webSocket2 = this.socket;
            if (webSocket2 != null && !PartyClient.resolveName().equals(this.lastHelloName)) {
                this.sendHello(webSocket2);
            }
            return;
        }
        if (this.connecting || this.socket != null) {
            return;
        }
        this.connecting = true;
        long epoch = ++this.connectionEpoch;
        try {
            rtx.nv.utils.net.SocialSession.connect(this.http.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(8L)), URI.create(Endpoints.party()), new Listener(epoch)).whenComplete((webSocket, throwable) -> {
                synchronized (PartyClient.this) {
                if (epoch != connectionEpoch || executor == null) { if (webSocket != null) webSocket.abort(); return; }
                this.connecting = false;
                if (throwable != null) {
                    this.connected = false;
                    this.socket = null;
                }
                }
            });
        }
        catch (Throwable throwable2) {
            this.connecting = false;
            this.connected = false;
            this.socket = null;
        }
    }

    public PartyInvite pendingInvite(String string) {
        return this.invites.get(string);
    }

    public boolean requestInfo() {
        return this.send("info", null);
    }

    private void handleVoice(byte[] byArray) {
        PartyVoiceSink partyVoiceSink = this.voiceSink;
        if (partyVoiceSink == null || byArray.length < 4) {
            return;
        }
        int n = byArray[0] & 0xFF;
        if (byArray.length < 1 + n + 2) {
            return;
        }
        String string = new String(byArray, 1, n, StandardCharsets.UTF_8);
        int n2 = 1 + n;
        int n3 = (byArray[n2] & 0xFF) << 8 | byArray[n2 + 1] & 0xFF;
        byte[] byArray2 = Arrays.copyOfRange(byArray, n2 + 2, byArray.length);
        try {
            partyVoiceSink.accept(string, n3, byArray2);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private boolean submitReliable(String string) {
        ExecutorService executorService = this.outbound;
        if (executorService == null) {
            return false;
        }
        try {
            long epoch = this.connectionEpoch;
            executorService.execute(() -> {
                WebSocket webSocket = this.socket;
                if (epoch != this.connectionEpoch || webSocket == null || !this.connected) {
                    return;
                }
                try {
                    webSocket.sendText(string, true).get(5, TimeUnit.SECONDS);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
            return true;
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    public void setVoiceSink(PartyVoiceSink partyVoiceSink) {
        this.voiceSink = partyVoiceSink;
    }

    public boolean kick(String string) {
        return this.send("kick", jsonObject -> jsonObject.addProperty("target", string));
    }

    public CopyOnWriteArrayList<PartyMarker> markers() {
        return this.markers;
    }

    private boolean send(String string, Consumer<JsonObject> consumer) {
        if (this.socket == null || !this.connected) {
            return false;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", string);
        if (consumer != null) {
            consumer.accept(jsonObject);
        }
        return this.submitReliable(jsonObject.toString());
    }

    public boolean sendPing(double d, double d2, double d3, String string, long l) {
        return this.send("marker", jsonObject -> {
            jsonObject.addProperty("x", (Number)d);
            jsonObject.addProperty("y", (Number)d2);
            jsonObject.addProperty("z", (Number)d3);
            jsonObject.addProperty("dim", string);
            jsonObject.addProperty("ttl", (Number)l);
        });
    }


    public boolean sendChat(String string) {
        return this.send("chat", jsonObject -> jsonObject.addProperty("text", string));
    }

    public boolean inviteUser(String string) {
        return this.send("invite", jsonObject -> jsonObject.addProperty("target", string));
    }

    public boolean leave() {
        return this.send("leave", null);
    }

    public boolean disband() {
        return this.send("disband", null);
    }

    private static void runGame(Runnable runnable) {
        MinecraftClient.getInstance().execute(runnable);
    }

    public void sendVoice(byte[] byArray) {
        ExecutorService executorService = this.outbound;
        if (executorService == null || this.socket == null || !this.connected || byArray == null || byArray.length > MAX_VOICE_BYTES) {
            return;
        }
        if (this.voiceBacklog.get() >= 8) {
            return;
        }
        this.voiceBacklog.incrementAndGet();
        ByteBuffer byteBuffer = ByteBuffer.wrap(byArray);
        long epoch = this.connectionEpoch;
        try {
            executorService.execute(() -> {
                if (epoch != this.connectionEpoch) return;
                this.voiceBacklog.decrementAndGet();
                WebSocket webSocket = this.socket;
                if (webSocket == null || !this.connected) {
                    return;
                }
                try {
                    webSocket.sendBinary(byteBuffer, true).get(5, TimeUnit.SECONDS);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            });
        }
        catch (Throwable throwable) {
            this.voiceBacklog.decrementAndGet();
        }
    }

    private static boolean bool(JsonObject jsonObject, String string) {
        try {
            return jsonObject != null && jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive() && jsonObject.get(string).getAsBoolean();
        }
        catch (Throwable throwable) {
            return false;
        }
    }

    private class Listener implements java.net.http.WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();
        private final ByteArrayOutputStream binaryBuffer = new ByteArrayOutputStream();
        private final long epoch;

        Listener(long epoch) { this.epoch = epoch; }

        @Override
        public void onOpen(java.net.http.WebSocket ws) {
            synchronized (PartyClient.this) {
            if (epoch != connectionEpoch || executor == null) { ws.abort(); return; }
            socket = ws;
            connected = true;
            connecting = false;
            ws.request(1);
            }
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket ws, CharSequence data, boolean last) {
            synchronized (PartyClient.this) {
            if (ws != socket || epoch != connectionEpoch) { ws.abort(); return null; }
            if (data.length() > MAX_TEXT_CHARS - textBuffer.length()) {
                textBuffer.setLength(0); ws.abort(); socket = null; connected = false; return null;
            }
            textBuffer.append(data);
            if (last) {
                try {
                    com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(textBuffer.toString()).getAsJsonObject();
                    handle(obj);
                } catch (Exception ignored) {}
                textBuffer.setLength(0);
            }
            ws.request(1);
            return null;
            }
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onBinary(java.net.http.WebSocket ws, ByteBuffer data, boolean last) {
            synchronized (PartyClient.this) {
            if (ws != socket || epoch != connectionEpoch) { ws.abort(); return null; }
            if (data.remaining() > MAX_VOICE_BYTES - binaryBuffer.size()) {
                binaryBuffer.reset(); ws.abort(); socket = null; connected = false; return null;
            }
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            binaryBuffer.writeBytes(bytes);
            if (last) { handleVoice(binaryBuffer.toByteArray()); binaryBuffer.reset(); }
            ws.request(1);
            return null;
            }
        }

        @Override
        public java.util.concurrent.CompletionStage<?> onClose(java.net.http.WebSocket ws, int statusCode, String reason) {
            synchronized (PartyClient.this) {
            if (ws != socket || epoch != connectionEpoch) return null;
            connected = false;
            connecting = false;
            socket = null;
            return null;
            }
        }

        @Override
        public void onError(java.net.http.WebSocket ws, Throwable error) {
            synchronized (PartyClient.this) {
            if (ws != socket || epoch != connectionEpoch) return;
            connected = false;
            connecting = false;
            socket = null;
            }
        }
    }
}

