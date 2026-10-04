package rtx.nv.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.party.PartyClient;
import rtx.nv.api.modules.impl.Utils.guishare.*;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.sync.CustomPetSyncClient;
import rtx.nv.api.modules.impl.Visuals.emotions.*;
import rtx.nv.utils.net.*;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Actual Java client protocol and TLS against the deployed social service. */
public final class SocialNetworkTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        GuiShareClient gui = new GuiShareClient();
        CustomPetSyncClient pets = new CustomPetSyncClient();
        EmotionSyncClient emotions = new EmotionSyncClient();
        List<WebSocket> remote = new CopyOnWriteArrayList<>();
        String peerName = "NVjava"+UUID.randomUUID().toString().replace("-", "").substring(0,8);
        String worldId = "nv-java-protocol-test";
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            context.runOnClient(client -> {
                String name = SocialSession.name();
                gui.setLocalState(name,name,worldId,0,64,0,true,new Vec3d(0,64,0),0,0,"Visuals",0,
                    0,0,0,0,"",0,0,0,List.of(),"",List.of(),"Guest","",GuiShareCloseState.IDLE,GuiShareThemeState.DEFAULTS,"",0);
                pets.setLocalState(name,name,worldId,CustomPetVariant.SPIRIT,0,"spirit",true,0,64,0,0,false,false,true,1);
                emotions.setLocalState(name,name,worldId,Emotion.WAVE,System.currentTimeMillis(),1,true);
                PartyClient.INSTANCE.start(); ClientPresence.INSTANCE.start();
                gui.connect("",0); pets.connect("",0); emotions.connect("",0);
            });
            context.waitFor(client -> PartyClient.INSTANCE.isConnected() && ClientPresence.INSTANCE.hasOnline()
                && gui.isConnected() && pets.isConnected() && emotions.isConnected(),1200);
            CompletableFuture<Void> fixture = CompletableFuture.runAsync(() -> {
                try {
                    HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
                    JsonObject credentials = new JsonObject();
                    credentials.addProperty("installation", UUID.randomUUID().toString());
                    byte[] bytes = new byte[32]; new java.security.SecureRandom().nextBytes(bytes);
                    credentials.addProperty("secret", HexFormat.of().formatHex(bytes)); credentials.addProperty("name",peerName);
                    HttpResponse<byte[]> response = http.send(HttpRequest.newBuilder(URI.create(Endpoints.irc()+"/api/session"))
                        .timeout(Duration.ofSeconds(8)).header("Content-Type","application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(credentials.toString())).build(),LimitedHttpBody.bytes(4096));
                    if (response.statusCode()!=200) throw new AssertionError("Peer session HTTP "+response.statusCode());
                    String token = JsonParser.parseString(new String(response.body(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().get("token").getAsString();
                    for (String channel : List.of("gui","pets","emotions")) {
                        WebSocket socket = http.newWebSocketBuilder().header("Authorization","Bearer "+token)
                            .connectTimeout(Duration.ofSeconds(8)).buildAsync(URI.create(Endpoints.sync(channel)),new Drain()).get(10,TimeUnit.SECONDS);
                        remote.add(socket);
                        JsonObject state = new JsonObject();
                        state.addProperty("t","h"); state.addProperty("w",worldId); state.addProperty("o",true);
                        state.addProperty("a",true); state.addProperty("x",1); state.addProperty("y",64); state.addProperty("z",1);
                        if (channel.equals("pets")) { state.addProperty("v","MOTH"); state.addProperty("k","moth"); state.addProperty("s",1); }
                        else { state.addProperty("v",1); state.addProperty("s",System.currentTimeMillis()); }
                        state.addProperty("e","WAVE"); state.addProperty("l",true);
                        socket.sendText(state.toString(),true).get(5,TimeUnit.SECONDS);
                    }
                } catch (Exception failure) { throw new CompletionException(failure); }
            });
            context.waitFor(client -> fixture.isDone(),1200); fixture.join();
            context.waitFor(client -> gui.snapshotRemoteStates().values().stream().anyMatch(s -> s.minecraftUsername().equals(peerName))
                && pets.snapshotRemoteStates().values().stream().anyMatch(s -> s.minecraftUsername().equals(peerName) && s.variant()==CustomPetVariant.MOTH)
                && emotions.snapshot().values().stream().anyMatch(s -> s.minecraftUsername().equals(peerName) && s.emotion()==Emotion.WAVE),600);
            remote.forEach(WebSocket::abort); remote.clear();
            context.waitFor(client -> gui.snapshotRemoteStates().isEmpty() && pets.snapshotRemoteStates().isEmpty() && emotions.snapshot().isEmpty(),600);
            context.runOnClient(client -> { gui.disconnect("test"); pets.disconnect("test"); emotions.disconnect(); });
            context.runOnClient(client -> { gui.connect("",0); pets.connect("",0); emotions.connect("",0); });
            context.waitFor(client -> gui.isConnected() && pets.isConnected() && emotions.isConnected(),600);
            rtx.nv.NV.LOGGER.info("[NV-TEST] Live HTTPS Party/presence and WSS GUI/pet/emotion exchange, cleanup and reconnect passed");
        } finally {
            remote.forEach(WebSocket::abort);
            context.runOnClient(client -> { gui.disconnect("test"); pets.disconnect("test"); emotions.disconnect();
                PartyClient.INSTANCE.stop(); ClientPresence.INSTANCE.stop(); });
        }
    }
    private static final class Drain implements WebSocket.Listener {
        public void onOpen(WebSocket ws) { ws.request(1); }
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) { ws.request(1); return null; }
    }
}
