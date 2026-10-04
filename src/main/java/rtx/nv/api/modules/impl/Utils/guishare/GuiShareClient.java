package rtx.nv.api.modules.impl.Utils.guishare;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareCloseState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiSharePopupRow;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareRemoteState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareThemeState;

public final class GuiShareClient {
    private final Gson gson = new Gson();
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6L)).build();
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean connecting = new AtomicBoolean(false);
    private volatile long connectionEpoch;
    private final ConcurrentMap<String, GuiShareRemoteState> remoteStates = new ConcurrentHashMap<String, GuiShareRemoteState>();
    private volatile WebSocket socket;
    private volatile boolean manualClose;
    private volatile String identityKey = "";
    private volatile String profileUsername = "";
    private volatile String minecraftUsername = "";
    private volatile String world = "";
    private volatile double x;
    private volatile double y;
    private volatile double z;
    private volatile boolean open;
    private volatile Vec3d anchor = Vec3d.ZERO;
    private volatile float yaw;
    private volatile float pitch;
    private volatile String category = "";
    private volatile int eventsSub;
    private volatile float eventsScroll;
    private volatile float listScroll;
    private volatile float mouseX;
    private volatile float mouseY;
    private volatile String popupModule = "";
    private volatile float popupScroll;
    private volatile float popupX;
    private volatile float popupY;
    private volatile List<GuiSharePopupRow> popupRows = List.of();
    private volatile String search = "";
    private volatile List<String> enabledModules = List.of();
    private volatile String role = "";
    private volatile String avatarUrl = "";
    private volatile GuiShareCloseState closeState = GuiShareCloseState.IDLE;
    private volatile GuiShareThemeState theme = GuiShareThemeState.DEFAULTS;
    private volatile String selectedTheme = "";
    private volatile float themesScroll;

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

    private boolean readBoolean(JsonObject jsonObject, String string) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            try {
                return jsonObject.get(string).getAsBoolean();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return false;
    }

    private double readDouble(JsonObject jsonObject, String string, double d) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            try {
                double value = jsonObject.get(string).getAsDouble();
                return Double.isFinite(value) ? Math.clamp(value, -30000000.0, 30000000.0) : d;
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return d;
    }

    private String readString(JsonObject jsonObject, String string) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonPrimitive()) {
            return jsonObject.get(string).getAsString();
        }
        return "";
    }

    public synchronized void connect(String string, int n) {
        URI uRI;
        if (this.connected.get() || this.connecting.get() || !this.hasLocalIdentity()) {
            return;
        }
        try {
            uRI = URI.create(rtx.nv.utils.net.Endpoints.sync("gui"));
        }
        catch (Exception exception) {
            return;
        }
        long attempt = ++this.connectionEpoch;
        this.manualClose = false;
        this.connecting.set(true);
        rtx.nv.utils.net.SocialSession.connect(this.httpClient.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(6L)), uRI, new GuiShareClient.SocketListener(this, attempt)).whenComplete((webSocket, throwable) -> {
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
        for (JsonElement jsonElement : jsonArray) {
            if (!jsonElement.isJsonObject()) continue;
            JsonObject object = jsonElement.getAsJsonObject();
            String string = this.normalize(this.readString(object, "p"), 48);
            String string2 = this.normalize(this.readString(object, "n"), 32);
            String string3 = this.normalize(this.readString(object, "i"), 96);
            if (string3.isBlank()) {
                string3 = this.buildIdentityKey(string, string2);
            }
            if (string3.isBlank() || string.isBlank() || string2.isBlank()) continue;
            if (this.remoteStates.size() >= 128 && !this.remoteStates.containsKey(string3)) continue;
            List<GuiSharePopupRow> list = this.readPopupRows(object);
            GuiShareCloseState guiShareCloseState = this.readCloseState(object);
            ArrayList<String> arrayList = new ArrayList<String>();
            if (object.has("en") && object.get("en").isJsonArray()) {
                for (JsonElement jsonElement2 : object.getAsJsonArray("en")) {
                    String string4;
                    if (!jsonElement2.isJsonPrimitive() || (string4 = this.normalize(jsonElement2.getAsString(), 32)).isBlank()) continue;
                    if (arrayList.size() >= 128) break;
                    arrayList.add(string4);
                }
            }
            this.remoteStates.put(string3, new GuiShareRemoteState(string3, string, string2, this.normalize(this.readString(object, "w"), 64), this.readDouble(object, "x", 0.0), this.readDouble(object, "y", 0.0), this.readDouble(object, "z", 0.0), this.readBoolean(object, "o"), new Vec3d(this.readDouble(object, "ax", 0.0), this.readDouble(object, "ay", 0.0), this.readDouble(object, "az", 0.0)), (float)this.readDouble(object, "ry", 0.0), (float)this.readDouble(object, "rp", 0.0), this.normalize(this.readString(object, "c"), 32), Math.max(0, Math.min(1, (int)this.readDouble(object, "es", 0.0))), (float)this.readDouble(object, "ec", 0.0), (float)this.readDouble(object, "sc", 0.0), (float)this.readDouble(object, "mx", 0.0), (float)this.readDouble(object, "my", 0.0), this.normalize(this.readString(object, "pm"), 32), (float)this.readDouble(object, "ps", 0.0), (float)this.readDouble(object, "px", 0.0), (float)this.readDouble(object, "py", 0.0), list, this.normalize(this.readString(object, "q"), 32), arrayList, this.normalize(this.readString(object, "ro"), 24), this.normalize(this.readString(object, "av"), 160), guiShareCloseState, GuiShareThemeState.fromJson(object), this.normalize(this.readString(object, "th"), 32), (float)this.readDouble(object, "hs", 0.0)));
        }
        for (JsonElement jsonElement : jsonArray2) {
            if (!jsonElement.isJsonPrimitive()) continue;
            String key = this.normalize(jsonElement.getAsString(), 96);
            if (key.isBlank()) continue;
            this.remoteStates.remove(key);
        }
    }

    private void handlePacket(String string) {
        if (string == null || string.isBlank()) {
            return;
        }
        try {
            JsonObject jsonObject = JsonParser.parseString((String)string).getAsJsonObject();
            String string2 = this.readString(jsonObject, "t");
            if ("s".equalsIgnoreCase(string2)) {
                this.remoteStates.clear();
                this.applyDelta(this.readArray(jsonObject, "p"), new JsonArray());
            } else if ("d".equalsIgnoreCase(string2)) {
                this.applyDelta(this.readArray(jsonObject, "p"), this.readArray(jsonObject, "r"));
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private JsonArray readArray(JsonObject jsonObject, String string) {
        if (jsonObject.has(string) && jsonObject.get(string).isJsonArray()) {
            return jsonObject.getAsJsonArray(string);
        }
        return new JsonArray();
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
        GuiShareCloseState guiShareCloseState;
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
        jsonObject.addProperty("x", (Number)(Object)this.x);
        jsonObject.addProperty("y", (Number)(Object)this.y);
        jsonObject.addProperty("z", (Number)(Object)this.z);
        jsonObject.addProperty("o", Boolean.valueOf(this.open));
        jsonObject.addProperty("ax", (Number)(Object)this.anchor.x);
        jsonObject.addProperty("ay", (Number)(Object)this.anchor.y);
        jsonObject.addProperty("az", (Number)(Object)this.anchor.z);
        jsonObject.addProperty("ry", (Number)Float.valueOf(this.yaw));
        jsonObject.addProperty("rp", (Number)Float.valueOf(this.pitch));
        jsonObject.addProperty("c", this.category);
        jsonObject.addProperty("es", (Number)(Object)this.eventsSub);
        jsonObject.addProperty("ec", (Number)Float.valueOf(this.eventsScroll));
        jsonObject.addProperty("sc", (Number)Float.valueOf(this.listScroll));
        jsonObject.addProperty("mx", (Number)Float.valueOf(this.mouseX));
        jsonObject.addProperty("my", (Number)Float.valueOf(this.mouseY));
        jsonObject.addProperty("pm", this.popupModule);
        jsonObject.addProperty("ps", (Number)Float.valueOf(this.popupScroll));
        jsonObject.addProperty("px", (Number)Float.valueOf(this.popupX));
        jsonObject.addProperty("py", (Number)Float.valueOf(this.popupY));
        List<GuiSharePopupRow> list = this.popupRows;
        if (!list.isEmpty()) {
            JsonArray popupArray = new JsonArray();
            int n = Math.min(list.size(), 28);
            for (int i = 0; i < n; ++i) {
                GuiSharePopupRow object = list.get(i);
                JsonObject jsonObject2 = new JsonObject();
                jsonObject2.addProperty("t", object.type());
                jsonObject2.addProperty("n", object.name());
                jsonObject2.addProperty("v", object.value());
                jsonObject2.addProperty("f", (Number)Float.valueOf(object.fraction()));
                jsonObject2.addProperty("r", (Number)object.rgb());
                if (object.open()) {
                    jsonObject2.addProperty("o", (Number)1);
                }
                if (object.alpha() != 0) {
                    jsonObject2.addProperty("a", (Number)object.alpha());
                }
                if (!object.options().isEmpty()) {
                    JsonArray jsonArray = new JsonArray();
                    for (String string2 : object.options()) {
                        jsonArray.add(string2);
                    }
                    jsonObject2.add("op", (JsonElement)jsonArray);
                    jsonObject2.addProperty("sm", (Number)object.selectedMask());
                }
                popupArray.add((JsonElement)jsonObject2);
            }
            jsonObject.add("pr", (JsonElement)popupArray);
        }
        if ((guiShareCloseState = this.closeState).active()) {
            jsonObject.addProperty("ka", (Number)1);
            jsonObject.addProperty("kv", (Number)Float.valueOf(guiShareCloseState.openness()));
            jsonObject.addProperty("kp", (Number)Float.valueOf(guiShareCloseState.shatterProgress()));
            jsonObject.addProperty("kc", (Number)Float.valueOf(guiShareCloseState.screenScale()));
            jsonObject.addProperty("kd", Long.toString(guiShareCloseState.seed()));
            JsonArray jsonArray = new JsonArray();
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.rectX()));
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.rectY()));
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.rectW()));
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.rectH()));
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.rectPad()));
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.screenW()));
            jsonArray.add((Number)Float.valueOf(guiShareCloseState.screenH()));
            jsonObject.add("kr", (JsonElement)jsonArray);
        }
        jsonObject.addProperty("q", this.search);
        jsonObject.addProperty("ro", this.role);
        jsonObject.addProperty("av", this.avatarUrl);
        jsonObject.addProperty("th", this.selectedTheme);
        jsonObject.addProperty("hs", (Number)Float.valueOf(this.themesScroll));
        this.theme.writeTo(jsonObject);
        JsonArray jsonArray = new JsonArray();
        for (String string3 : this.enabledModules) {
            if (string3 == null || string3.isBlank()) continue;
            jsonArray.add(string3);
        }
        jsonObject.add("en", (JsonElement)jsonArray);
        webSocket.sendText(this.gson.toJson((JsonElement)jsonObject), true);
    }

    public boolean isConnecting() {
        return this.connecting.get();
    }

    public Map<String, GuiShareRemoteState> snapshotRemoteStates() {
        return new HashMap<String, GuiShareRemoteState>(this.remoteStates);
    }

    public void setLocalState(String string, String string2, String string3, double d, double d2, double d3, boolean bl, Vec3d vec3d, float f, float f2, String string4, int n, float f3, float f4, float f5, float f6, String string5, float f7, float f8, float f9, List<GuiSharePopupRow> list, String string6, List<String> list2, String string7, String string8, GuiShareCloseState guiShareCloseState, GuiShareThemeState guiShareThemeState, String string9, float f10) {
        this.profileUsername = this.normalize(string, 48);
        this.minecraftUsername = this.normalize(string2, 32);
        this.identityKey = this.buildIdentityKey(this.profileUsername, this.minecraftUsername);
        this.world = this.normalize(string3, 64);
        this.x = d;
        this.y = d2;
        this.z = d3;
        this.open = bl;
        this.anchor = vec3d == null ? Vec3d.ZERO : vec3d;
        this.yaw = f;
        this.pitch = f2;
        this.category = this.normalize(string4, 32);
        this.eventsSub = n;
        this.eventsScroll = f3;
        this.listScroll = f4;
        this.mouseX = f5;
        this.mouseY = f6;
        this.popupModule = this.normalize(string5, 32);
        this.popupScroll = f7;
        this.popupX = f8;
        this.popupY = f9;
        this.popupRows = list == null ? List.of() : list;
        this.search = this.normalize(string6, 32);
        this.enabledModules = list2 == null ? List.of() : list2;
        this.role = this.normalize(string7, 24);
        this.avatarUrl = this.normalize(string8, 160);
        this.closeState = guiShareCloseState == null ? GuiShareCloseState.IDLE : guiShareCloseState;
        this.theme = guiShareThemeState == null ? GuiShareThemeState.DEFAULTS : guiShareThemeState;
        this.selectedTheme = this.normalize(string9, 32);
        this.themesScroll = f10;
    }

    private String buildIdentityKey(String string, String string2) {
        if (string.isBlank() || string2.isBlank()) {
            return "";
        }
        return string + "|" + string2;
    }

    private boolean hasLocalIdentity() {
        return !this.profileUsername.isBlank() && !this.minecraftUsername.isBlank() && !this.identityKey.isBlank();
    }

    private List<GuiSharePopupRow> readPopupRows(JsonObject jsonObject) {
        if (!jsonObject.has("pr") || !jsonObject.get("pr").isJsonArray()) {
            return List.of();
        }
        ArrayList<GuiSharePopupRow> arrayList = new ArrayList<GuiSharePopupRow>();
        for (JsonElement jsonElement : jsonObject.getAsJsonArray("pr")) {
            JsonObject jsonObject2;
            String string;
            if (arrayList.size() >= 28) break;
            if (!jsonElement.isJsonObject() || (string = this.normalize(this.readString(jsonObject2 = jsonElement.getAsJsonObject(), "t"), 1)).isEmpty()) continue;
            float f = (float)this.readDouble(jsonObject2, "f", 0.0);
            f = f < 0.0f ? 0.0f : Math.min(f, 1.0f);
            int n = (int)this.readDouble(jsonObject2, "r", 0.0);
            n = Math.max(0, Math.min(0xFFFFFF, n));
            int n2 = (int)this.readDouble(jsonObject2, "a", 0.0);
            n2 = Math.max(0, Math.min(255, n2));
            boolean bl = this.readDouble(jsonObject2, "o", 0.0) > 0.5;
            List<String> list = List.of();
            int n3 = 0;
            if (jsonObject2.has("op") && jsonObject2.get("op").isJsonArray()) {
                ArrayList<String> arrayList2 = new ArrayList<String>();
                for (JsonElement jsonElement2 : jsonObject2.getAsJsonArray("op")) {
                    if (arrayList2.size() >= 24) break;
                    if (!jsonElement2.isJsonPrimitive()) continue;
                    arrayList2.add(this.normalize(jsonElement2.getAsString(), 24));
                }
                list = arrayList2;
                n3 = (int)this.readDouble(jsonObject2, "sm", 0.0);
            }
            arrayList.add(new GuiSharePopupRow(string, this.normalize(this.readString(jsonObject2, "n"), 32), this.normalize(this.readString(jsonObject2, "v"), 24), f, n, bl, list, n3, n2));
        }
        return arrayList;
    }

    private GuiShareCloseState readCloseState(JsonObject jsonObject) {
        if (this.readDouble(jsonObject, "ka", 0.0) <= 0.5) {
            return GuiShareCloseState.IDLE;
        }
        float[] fArray = new float[7];
        if (jsonObject.has("kr") && jsonObject.get("kr").isJsonArray()) {
            JsonArray jsonArray = jsonObject.getAsJsonArray("kr");
            for (int i = 0; i < fArray.length && i < jsonArray.size(); ++i) {
                try {
                    fArray[i] = jsonArray.get(i).getAsFloat();
                    continue;
                }
                catch (RuntimeException runtimeException) {
                    // empty catch block
                }
            }
        }
        long l = 0L;
        try {
            l = Long.parseLong(this.readString(jsonObject, "kd"));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
        return new GuiShareCloseState(true, (float)this.readDouble(jsonObject, "kv", 1.0), (float)this.readDouble(jsonObject, "kp", 0.0), (float)this.readDouble(jsonObject, "kc", 1.0), l, fArray[0], fArray[1], fArray[2], fArray[3], fArray[4], fArray[5], fArray[6]);
    }


    public static final class SocketListener
    implements WebSocket.Listener {
        final GuiShareClient this$0;
        private final StringBuilder buffer = new StringBuilder();
        private final long attempt;

        private SocketListener(GuiShareClient guiShareClient, long attempt) {
            this.attempt = attempt;
            this.this$0 = guiShareClient;
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

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (webSocket != this.this$0.socket) return null;
            this.this$0.connecting.set(false);
            this.this$0.connected.set(false);
            this.this$0.socket = null;
            if (!this.this$0.manualClose) {
                this.this$0.remoteStates.clear();
            }
            return null;
        }
    }
}

