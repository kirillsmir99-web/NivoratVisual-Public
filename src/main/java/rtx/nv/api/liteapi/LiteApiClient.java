package rtx.nv.api.liteapi;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.CustomPayload;
import rtx.nv.NV;
import rtx.nv.api.liteapi.FeatureBlocklist;
import rtx.nv.api.liteapi.LiteApiCodec;
import rtx.nv.api.liteapi.LiteApiCodec.Request;
import rtx.nv.api.liteapi.packets.LiteApiPayload;

public final class LiteApiClient {
    public static final LiteApiClient INSTANCE = new LiteApiClient();
    private static final String CLIENT_ID = "nv";
    private static final long TIMEOUT_MS = 10000L;
    private volatile boolean started = false;
    private final Map<String, Long> pending = new ConcurrentHashMap<String, Long>();

    private LiteApiClient() {
    }

    public void start() {
        if (this.started) {
            return;
        }
        this.started = true;
        PayloadTypeRegistry.playC2S().register(LiteApiPayload.TYPE, LiteApiPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(LiteApiPayload.TYPE, LiteApiPayload.CODEC);
        ClientPlayNetworking.registerGlobalReceiver(LiteApiPayload.TYPE, (liteApiPayload, context) -> context.client().execute(() -> this.handle(liteApiPayload.json())));
        ClientPlayConnectionEvents.JOIN.register((clientPlayNetworkHandler, packetSender, minecraftClient) -> this.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((clientPlayNetworkHandler, minecraftClient) -> this.onDisconnect());
    }

    private void handle(String string) {
        LiteApiCodec.Incoming incoming = LiteApiCodec.parse(string);
        if (incoming == null) {
            NV.LOGGER.warn("[LiteAPI] \u041f\u043e\u043b\u0443\u0447\u0435\u043d \u043d\u0435\u0432\u0430\u043b\u0438\u0434\u043d\u044b\u0439 \u043f\u0430\u043a\u0435\u0442 (\u043d\u0435 JSON-\u043e\u0431\u044a\u0435\u043a\u0442)");
            return;
        }
        if (incoming.isPushEvent()) {
            NV.LOGGER.debug("[LiteAPI] push-\u0441\u043e\u0431\u044b\u0442\u0438\u0435: {}", (Object)incoming.event());
            return;
        }
        if (incoming.id() == null || this.pending.remove(incoming.id()) == null) {
            return;
        }
        if (!incoming.ok()) {
            String string2 = incoming.error() == null ? "UNKNOWN" : incoming.error();
            String string3 = incoming.message() == null ? "" : ": " + incoming.message();
            NV.LOGGER.warn("[LiteAPI] checkFeatures \u043e\u0448\u0438\u0431\u043a\u0430 {}{}", (Object)string2, (Object)string3);
            return;
        }
        JsonObject jsonObject = incoming.payload();
        List<String> list = LiteApiCodec.blocklist(jsonObject);
        FeatureBlocklist.applyBlocklist(list);
        if (MinecraftClient.getInstance() != null && !list.isEmpty()) {
            NV.LOGGER.info("[LiteAPI] \u0421\u0435\u0440\u0432\u0435\u0440 \u0437\u0430\u0431\u043b\u043e\u043a\u0438\u0440\u043e\u0432\u0430\u043b \u0444\u0438\u0447\u0438: {}", list);
        }
    }

    public void stop() {
        this.onDisconnect();
        this.started = false;
    }

    private void onDisconnect() {
        this.pending.clear();
        FeatureBlocklist.clear();
    }

    private void purgeExpired() {
        long l = System.currentTimeMillis();
        this.pending.entrySet().removeIf(entry -> l - (Long)entry.getValue() > 10000L);
    }

    private void onJoin() {
        Set<String> set = FeatureBlocklist.knownFeatures();
        if (set.isEmpty()) {
            return;
        }
        this.purgeExpired();
        LiteApiCodec.Request request = LiteApiCodec.checkFeatures(CLIENT_ID, set);
        this.pending.put(request.id(), System.currentTimeMillis());
        try {
            ClientPlayNetworking.send((CustomPayload)new LiteApiPayload(request.json()));
        }
        catch (RuntimeException runtimeException) {
            this.pending.remove(request.id());
            NV.LOGGER.warn("[LiteAPI] \u041d\u0435 \u0443\u0434\u0430\u043b\u043e\u0441\u044c \u043e\u0442\u043f\u0440\u0430\u0432\u0438\u0442\u044c checkFeatures: {}", (Object)runtimeException.toString());
        }
    }
}

