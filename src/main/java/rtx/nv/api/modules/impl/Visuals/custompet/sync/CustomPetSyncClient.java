package rtx.nv.api.modules.impl.Visuals.custompet.sync;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.sync.CustomPetRemoteState;

public final class CustomPetSyncClient {
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private volatile long connectionEpoch;
    private final ConcurrentMap<String, CustomPetRemoteState> remoteStates = new ConcurrentHashMap<String, CustomPetRemoteState>();
    private volatile WebSocket socket;
    private volatile boolean manualClose;
    private volatile String identityKey = "";
    private volatile String profileUsername = "";
    private volatile String minecraftUsername = "";
    private volatile String world = "";
    private volatile CustomPetVariant variant = CustomPetVariant.NITWIT;
    private volatile int robotType;
    private volatile String petKind = "frog";
    private volatile boolean active;
    private volatile double x;
    private volatile double y;
    private volatile double z;
    private volatile float yaw;
    private volatile boolean moving;
    private volatile boolean umbrella;
    private volatile boolean airborne;
    private volatile double animationSpeed = 1.0;

    private float readFloat(JsonObject jsonObject, String string, String string2, float f) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            float value = jsonObject.get(string).getAsFloat();
            return Float.isFinite(value) ? value % 360.0f : f;
        }
        if (jsonObject.has(string2) && jsonObject.get(string2).isJsonPrimitive()) {
            float value = jsonObject.get(string2).getAsFloat();
            return Float.isFinite(value) ? value % 360.0f : f;
        }
        return f;
    }

    private String normalize(String string, int n) {
        if (string == null) {
            return "";
        }
        String string2 = string.trim();
        if (string2.length() > n) {
            return string2.substring(0, n);
        }
        return string2;
    }

    public void pushState() {
        this.sendPacket("state");
    }

    private boolean readBoolean(JsonObject jsonObject, String string, String string2) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            return jsonObject.get(string).getAsBoolean();
        }
        if (jsonObject.has(string2) && jsonObject.get(string2).isJsonPrimitive()) {
            return jsonObject.get(string2).getAsBoolean();
        }
        return false;
    }

    private double readDouble(JsonObject jsonObject, String string, String string2, double d) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            double value = jsonObject.get(string).getAsDouble();
                return Double.isFinite(value) ? Math.clamp(value, -30000000.0, 30000000.0) : d;
        }
        if (jsonObject.has(string2) && jsonObject.get(string2).isJsonPrimitive()) {
            double value = jsonObject.get(string2).getAsDouble();
            return Double.isFinite(value) ? Math.clamp(value, -30000000.0, 30000000.0) : d;
        }
        return d;
    }

    private String readString(JsonObject jsonObject, String string, String string2) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            return jsonObject.get(string).getAsString();
        }
        if (jsonObject.has(string2) && jsonObject.get(string2).isJsonPrimitive()) {
            return jsonObject.get(string2).getAsString();
        }
        return "";
    }

    public synchronized void connect(String string, int n) {
        URI uRI;
        if (this.connected.get() || this.connecting.get() || !this.hasLocalIdentity()) {
            return;
        }
        try {
            uRI = URI.create(rtx.nv.utils.net.Endpoints.sync("pets"));
        }
        catch (Exception exception) {
            return;
        }
        long attempt = ++this.connectionEpoch;
        this.manualClose = false;
        this.connecting.set(true);
        rtx.nv.utils.net.SocialSession.connect(this.httpClient.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(6L)), uRI, new CustomPetSyncClient.SocketListener(this, attempt)).whenComplete((webSocket, throwable) -> {
            synchronized (this) {
            if (attempt != this.connectionEpoch) { if (webSocket != null) webSocket.abort(); return; }
            if (throwable != null) {
                this.connecting.set(false);
                this.connected.set(false);
                this.socket = null;
                this.remoteStates.clear();
                return;
            }
            this.socket = webSocket;
            }
        });
    }

    public boolean isConnected() {
        return this.connected.get();
    }

    private void sendHello() {
        this.sendPacket("hello");
    }

    private void applyDelta(JsonArray jsonArray, JsonArray jsonArray2) {
        Object object;
        HashMap<String, CustomPetRemoteState> hashMap = new HashMap<String, CustomPetRemoteState>();
        for (JsonElement object2 : jsonArray) {
            if (hashMap.size() >= 128) break;
            if (!object2.isJsonObject()) continue;
            object = object2.getAsJsonObject();
            String string = this.normalize(this.readString((JsonObject)object, "p", "profileUsername"), 48);
            String string2 = this.normalize(this.readString((JsonObject)object, "n", "minecraftUsername"), 32);
            String string3 = this.normalize(this.readString((JsonObject)object, "i", "identityKey"), 96);
            if (string3.isBlank()) {
                string3 = this.buildIdentityKey(string, string2);
            }
            CustomPetVariant customPetVariant = CustomPetVariant.fromSerializedName(this.readString((JsonObject)object, "v", "variant"));
            int n = (int)this.readDouble((JsonObject)object, "rt", "robotType", 0.0);
            String string4 = this.normalize(this.readString((JsonObject)object, "k", "petKind"), 12);
            boolean bl = this.readBoolean((JsonObject)object, "a", "active");
            double d = this.readDouble((JsonObject)object, "x", "x", 0.0);
            double d2 = this.readDouble((JsonObject)object, "y", "y", 0.0);
            double d3 = this.readDouble((JsonObject)object, "z", "z", 0.0);
            float f = this.readFloat((JsonObject)object, "r", "yaw", 0.0f);
            boolean bl2 = this.readBoolean((JsonObject)object, "m", "moving");
            boolean bl3 = this.readBoolean((JsonObject)object, "u", "umbrella");
            boolean bl4 = this.readBoolean((JsonObject)object, "b", "airborne");
            double d4 = this.readDouble((JsonObject)object, "s", "animationSpeed", 1.0);
            if (string3.isBlank() || string.isBlank() || string2.isBlank()) continue;
            hashMap.put(string3, new CustomPetRemoteState(string3, string, string2, customPetVariant, n, bl, d, d2, d3, f, bl2, bl3, bl4, Math.clamp(d4, 0.05, 4.0), string4));
        }
        for (Map.Entry entry : hashMap.entrySet()) {
            if (this.remoteStates.size() >= 128 && !this.remoteStates.containsKey(entry.getKey())) continue;
            this.remoteStates.put((String)entry.getKey(), (CustomPetRemoteState)entry.getValue());
        }
        for (JsonElement jsonElement : jsonArray2) {
            if (!jsonElement.isJsonPrimitive() || ((String)(object = this.normalize(jsonElement.getAsString(), 96))).isBlank()) continue;
            this.remoteStates.remove(object);
        }
    }

    private JsonArray readPets(JsonObject jsonObject) {
        if (jsonObject.has("p") && jsonObject.get("p").isJsonArray()) {
            return jsonObject.getAsJsonArray("p");
        }
        if (jsonObject.has("pets") && jsonObject.get("pets").isJsonArray()) {
            return jsonObject.getAsJsonArray("pets");
        }
        return new JsonArray();
    }

    private void handlePacket(String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        try {
            JsonObject jsonObject = JsonParser.parseString((String)string).getAsJsonObject();
            String string2 = this.readString(jsonObject, "t", "type");
            if ("s".equalsIgnoreCase(string2) || "snapshot".equalsIgnoreCase(string2)) {
                this.applyFullSnapshot(this.readPets(jsonObject));
            } else if ("d".equalsIgnoreCase(string2) || "delta".equalsIgnoreCase(string2)) {
                this.applyDelta(this.readPets(jsonObject), this.readRemoved(jsonObject));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public synchronized void disconnect(String string) {
        ++this.connectionEpoch;
        this.manualClose = true;
        this.connecting.set(false);
        this.connected.set(false);
        this.remoteStates.clear();
        WebSocket webSocket = this.socket;
        this.socket = null;
        if (webSocket != null) {
            try {
                webSocket.sendClose(1000, this.normalize(string, 120));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void sendPacket(String string) {
        WebSocket webSocket = this.socket;
        if (!this.connected.get() || webSocket == null || !this.hasLocalIdentity()) {
            return;
        }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("t", "hello".equals(string) ? "h" : "u");
        jsonObject.addProperty("i", this.identityKey);
        jsonObject.addProperty("p", this.profileUsername);
        jsonObject.addProperty("n", this.minecraftUsername);
        jsonObject.addProperty("w", this.world);
        jsonObject.addProperty("v", this.variant.name());
        jsonObject.addProperty("rt", (Number)(Object)this.robotType);
        jsonObject.addProperty("k", this.petKind);
        jsonObject.addProperty("a", Boolean.valueOf(this.active));
        jsonObject.addProperty("x", (Number)(Object)this.x);
        jsonObject.addProperty("y", (Number)(Object)this.y);
        jsonObject.addProperty("z", (Number)(Object)this.z);
        jsonObject.addProperty("r", (Number)Float.valueOf(this.yaw));
        jsonObject.addProperty("m", Boolean.valueOf(this.moving));
        jsonObject.addProperty("u", Boolean.valueOf(this.umbrella));
        jsonObject.addProperty("b", Boolean.valueOf(this.airborne));
        jsonObject.addProperty("s", (Number)(Object)this.animationSpeed);
        webSocket.sendText(this.gson.toJson((JsonElement)jsonObject), true);
    }

    public boolean isConnecting() {
        return this.connecting.get();
    }

    public Map<String, CustomPetRemoteState> snapshotRemoteStates() {
        return new HashMap<String, CustomPetRemoteState>(this.remoteStates);
    }

    public void setLocalState(String string, String string2, String string3, CustomPetVariant customPetVariant, int n, String string4, boolean bl, double d, double d2, double d3, float f, boolean bl2, boolean bl3, boolean bl4, double d4) {
        this.profileUsername = this.normalize(string, 48);
        this.minecraftUsername = this.normalize(string2, 32);
        this.world = this.normalize(string3, 64);
        this.identityKey = this.buildIdentityKey(this.profileUsername, this.minecraftUsername);
        this.variant = customPetVariant == null ? CustomPetVariant.NITWIT : customPetVariant;
        this.robotType = n;
        this.petKind = string4 == null || string4.isBlank() ? "frog" : string4;
        this.active = bl;
        this.x = d;
        this.y = d2;
        this.z = d3;
        this.yaw = f;
        this.moving = bl2;
        this.umbrella = bl3;
        this.airborne = bl4;
        this.animationSpeed = d4 <= 0.0 ? 1.0 : d4;
    }

    private String buildIdentityKey(String string, String string2) {
        if (string.isBlank() || string2.isBlank()) {
            return "";
        }
        return string + "|" + string2;
    }

    private void applyFullSnapshot(JsonArray jsonArray) {
        this.remoteStates.clear();
        this.applyDelta(jsonArray, new JsonArray());
    }

    private JsonArray readRemoved(JsonObject jsonObject) {
        if (jsonObject.has("r") && jsonObject.get("r").isJsonArray()) {
            return jsonObject.getAsJsonArray("r");
        }
        if (jsonObject.has("removed") && jsonObject.get("removed").isJsonArray()) {
            return jsonObject.getAsJsonArray("removed");
        }
        return new JsonArray();
    }

    private boolean hasLocalIdentity() {
        return !this.profileUsername.isBlank() && !this.minecraftUsername.isBlank() && !this.identityKey.isBlank();
    }


    public static final class SocketListener
    implements WebSocket.Listener {
        final /* synthetic */ CustomPetSyncClient this$0;
        private final StringBuilder buffer = new StringBuilder();
        private final long attempt;
    
        private SocketListener(CustomPetSyncClient customPetSyncClient, long attempt) {
            this.attempt = attempt;
            this.this$0 = customPetSyncClient;
        }
    
        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int n, String string) {
            if (webSocket != this.this$0.socket) return null;
            this.this$0.connecting.set(false);
            this.this$0.connected.set(false);
            this.this$0.socket = null;
            this.this$0.remoteStates.clear();
            return WebSocket.Listener.super.onClose(webSocket, n, string);
        }
    
        @Override
        public void onOpen(WebSocket webSocket) {
            synchronized (this.this$0) {
            if (this.attempt != this.this$0.connectionEpoch || this.this$0.manualClose) { webSocket.abort(); return; }
            this.this$0.socket = webSocket;
            this.this$0.connecting.set(false);
            this.this$0.connected.set(true);
            webSocket.request(1L);
            this.this$0.sendHello();
            }
        }
    
        /*
         * WARNING - Removed try catching itself - possible behaviour change.
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence charSequence, boolean bl) {
            if (webSocket != this.this$0.socket) return null;
            StringBuilder stringBuilder = this.buffer;
            synchronized (stringBuilder) {
                if (!rtx.nv.utils.net.WebSocketMessages.append(this.buffer, webSocket, charSequence)) return null;
                if (bl) {
                    String string = this.buffer.toString();
                    this.buffer.setLength(0);
                    this.this$0.handlePacket(string);
                }
            }
            webSocket.request(1L);
            return null;
        }
    
        @Override
        public void onError(WebSocket webSocket, Throwable throwable) {
            if (webSocket != this.this$0.socket) return;
            this.this$0.connecting.set(false);
            this.this$0.connected.set(false);
            this.this$0.socket = null;
            if (!this.this$0.manualClose) {
                this.this$0.remoteStates.clear();
            }
        }
    }
}

