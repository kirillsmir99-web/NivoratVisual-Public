package rtx.nv.api.modules.impl.Utils.guishare;
import rtx.nv.api.profile.Profile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.PlayerLikeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import rtx.nv.api.modules.impl.Utils.Globals;
import rtx.nv.api.modules.impl.Utils.guishare.GuiHoldPose;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareClient;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareCloseState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareLocalSnapshot;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareRemoteState;
import rtx.nv.api.modules.impl.Utils.guishare.GuiShareThemeState;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiPanel;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiPanelRenderer;
import rtx.nv.api.ui.UI;

public final class GuiShareController {
    private static final String SYNC_HOST = rtx.nv.utils.net.NvHosts.SOCIAL_HOST;
    private static final int SYNC_PORT = 32118;
    private static final long CONNECT_RETRY_MS = 5000L;
    private static final long OPEN_PUSH_MS = 100L;
    private static final long CLOSED_HEARTBEAT_MS = 2500L;
    private static final double ANCHOR_DISTANCE = 1.55;
    private static final GuiShareClient client = new GuiShareClient();
    private static final Map<String, RemoteGuiPanel> panels = new HashMap<String, RemoteGuiPanel>();
    private static long nextConnectAttemptAt;
    private static long nextPushAt;
    private static GuiShareLocalSnapshot lastSnapshot;
    private static boolean lastOpen;
    private static float localHoldWeight;
    private static Vec3d anchor;
    private static float anchorYaw;
    private static float anchorPitch;

    private GuiShareController() {
    }

    static {
        anchor = Vec3d.ZERO;
    }

    public static void reset() {
        client.disconnect("reset");
        panels.clear();
        lastSnapshot = null;
        lastOpen = false;
        localHoldWeight = 0.0f;
        nextPushAt = 0L;
    }

    private static String currentWorldId(MinecraftClient minecraftClient) {
        try {
            if (minecraftClient.world != null) {
                return minecraftClient.world.getRegistryKey().getValue().toString();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "";
    }

    private static String resolveMinecraftUsername(MinecraftClient minecraftClient) {
        if (minecraftClient.player == null) {
            return "";
        }
        String string = minecraftClient.player.getGameProfile().name();
        return string == null ? "" : string.trim();
    }

    private static String buildIdentityKey(String string, String string2) {
        return string + "|" + string2;
    }

    private static String resolveProfileUsername(String string) {
        try {
            String string2 = Profile.getUsername();
            if (string2 != null && !string2.isBlank()) {
                return string2.trim();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return string;
    }

    public static void tick(Globals globals) {
        boolean bl;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.player == null || minecraftClient.world == null || globals == null || !globals.isEnabled()) {
            GuiShareController.reset();
            return;
        }
        String string = GuiShareController.resolveMinecraftUsername(minecraftClient);
        String string2 = GuiShareController.resolveProfileUsername(string);
        if (string2.isBlank() || string.isBlank()) {
            GuiShareController.reset();
            return;
        }
        boolean bl2 = bl = Globals.shareGuiEnabled() && UI.isOpen();
        if (bl != lastOpen) {
            if (bl) {
                GuiShareController.captureAnchor(minecraftClient);
            }
            nextPushAt = 0L;
        }
        lastOpen = bl;
        localHoldWeight += ((bl ? 1.0f : 0.0f) - localHoldWeight) * 0.28f;
        GuiShareLocalSnapshot guiShareLocalSnapshot = bl ? UI.shareSnapshot() : null;
        boolean bl3 = bl && guiShareLocalSnapshot != null;
        GuiShareCloseState guiShareCloseState = UI.shareCloseState();
        if (guiShareLocalSnapshot != null) {
            lastSnapshot = guiShareLocalSnapshot;
        } else if (guiShareCloseState.active()) {
            guiShareLocalSnapshot = lastSnapshot;
        } else {
            lastSnapshot = null;
        }
        float f = 430.0f;
        float f2 = 290.0f;
        float f3 = 22.0f;
        client.setLocalState(string2, string, GuiShareController.currentWorldId(minecraftClient), minecraftClient.player.getX(), minecraftClient.player.getY(), minecraftClient.player.getZ(), bl3, anchor, anchorYaw, anchorPitch, guiShareLocalSnapshot == null ? "" : guiShareLocalSnapshot.category(), guiShareLocalSnapshot == null ? 0 : guiShareLocalSnapshot.eventsSub(), guiShareLocalSnapshot == null ? 0.0f : guiShareLocalSnapshot.eventsScroll(), guiShareLocalSnapshot == null ? 0.0f : guiShareLocalSnapshot.listScroll(), guiShareLocalSnapshot == null ? 0.0f : guiShareLocalSnapshot.mouseX() * f, guiShareLocalSnapshot == null ? 0.0f : f3 + guiShareLocalSnapshot.mouseY() * f2, guiShareLocalSnapshot == null ? "" : guiShareLocalSnapshot.popupModule(), guiShareLocalSnapshot == null ? 0.0f : guiShareLocalSnapshot.popupScroll(), guiShareLocalSnapshot == null ? 0.0f : guiShareLocalSnapshot.popupX() * f, guiShareLocalSnapshot == null ? 0.0f : f3 + guiShareLocalSnapshot.popupY() * f2, guiShareLocalSnapshot == null ? List.of() : guiShareLocalSnapshot.popupRows(), guiShareLocalSnapshot == null ? "" : guiShareLocalSnapshot.search(), guiShareLocalSnapshot == null ? List.of() : guiShareLocalSnapshot.enabledModules(), guiShareLocalSnapshot == null ? "" : guiShareLocalSnapshot.role(), guiShareLocalSnapshot == null ? "" : guiShareLocalSnapshot.avatarUrl(), guiShareCloseState, guiShareLocalSnapshot == null ? GuiShareThemeState.capture() : guiShareLocalSnapshot.theme(), guiShareLocalSnapshot == null ? "" : guiShareLocalSnapshot.selectedTheme(), guiShareLocalSnapshot == null ? 0.0f : guiShareLocalSnapshot.themesScroll());
        long l = System.currentTimeMillis();
        if (!client.isConnected() && !client.isConnecting() && l >= nextConnectAttemptAt) {
            client.connect(SYNC_HOST, 32118);
            nextConnectAttemptAt = l + 5000L;
        }
        if (client.isConnected() && l >= nextPushAt) {
            client.pushState();
            nextPushAt = l + (guiShareCloseState.active() ? 50L : (bl3 ? 100L : 2500L));
        }
        GuiShareController.tickPanels(minecraftClient, GuiShareController.buildIdentityKey(string2, string));
    }

    public static Vec3d liveAnchorFor(GuiShareRemoteState guiShareRemoteState, float f, float f2) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.world == null || guiShareRemoteState == null) {
            return guiShareRemoteState == null ? Vec3d.ZERO : guiShareRemoteState.anchor();
        }
        for (PlayerEntity playerEntity : minecraftClient.world.getPlayers()) {
            String string;
            if (playerEntity == null || playerEntity.isRemoved() || (string = playerEntity.getGameProfile().name()) == null || !string.equalsIgnoreCase(guiShareRemoteState.minecraftUsername())) continue;
            return GuiShareController.heldAnchor(minecraftClient, playerEntity, f, f2, guiShareRemoteState.anchor());
        }
        return guiShareRemoteState.anchor();
    }

    public static Vec3d liveAnchorFor(GuiShareRemoteState guiShareRemoteState) {
        return GuiShareController.liveAnchorFor(guiShareRemoteState, guiShareRemoteState == null ? 0.0f : guiShareRemoteState.yaw(), guiShareRemoteState == null ? 0.0f : guiShareRemoteState.pitch());
    }

    public static List<RemoteGuiPanel> renderablePanels(int n) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.player == null || panels.isEmpty()) {
            return List.of();
        }
        Vec3d vec3d = minecraftClient.player.getEyePos();
        double d = 2304.0;
        ArrayList<RemoteGuiPanel> arrayList = new ArrayList<RemoteGuiPanel>();
        for (RemoteGuiPanel remoteGuiPanel2 : panels.values()) {
            if (!remoteGuiPanel2.isRenderable() || !(GuiShareController.livePositionOf(remoteGuiPanel2).squaredDistanceTo(vec3d) <= d)) continue;
            arrayList.add(remoteGuiPanel2);
        }
        arrayList.sort(Comparator.comparingDouble(remoteGuiPanel -> GuiShareController.livePositionOf(remoteGuiPanel).squaredDistanceTo(vec3d)));
        if (arrayList.size() > n) {
            arrayList = new ArrayList<RemoteGuiPanel>(arrayList.subList(0, n));
        }
        arrayList.sort(Comparator.comparingDouble((RemoteGuiPanel remoteGuiPanel) -> GuiShareController.livePositionOf(remoteGuiPanel).squaredDistanceTo(vec3d)).reversed());
        return arrayList;
    }

    private static void captureAnchor(MinecraftClient minecraftClient) {
        Vec3d vec3d = minecraftClient.player.getEyePos();
        Vec3d vec3d2 = minecraftClient.player.getRotationVector();
        anchor = vec3d.add(vec3d2.multiply(1.55));
        anchorYaw = minecraftClient.player.getYaw();
        anchorPitch = minecraftClient.player.getPitch();
    }

    public static GuiHoldPose.HoldTarget holdTargetFor(PlayerLikeEntity playerLikeEntity) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || playerLikeEntity == null) {
            return null;
        }
        if (playerLikeEntity == minecraftClient.player) {
            if (localHoldWeight < 0.02f) {
                return null;
            }
            Vec3d vec3d = GuiShareController.heldAnchor(minecraftClient, (PlayerEntity)minecraftClient.player, anchorYaw, anchorPitch, anchor);
            return new GuiHoldPose.HoldTarget(vec3d, anchorYaw, anchorPitch, 1.0f, localHoldWeight);
        }
        String string = playerLikeEntity.getName().getString();
        if (string == null || string.isBlank()) {
            return null;
        }
        for (RemoteGuiPanel remoteGuiPanel : panels.values()) {
            GuiShareRemoteState guiShareRemoteState;
            if (!remoteGuiPanel.isRenderable() || !(guiShareRemoteState = remoteGuiPanel.state()).minecraftUsername().equalsIgnoreCase(string)) continue;
            return new GuiHoldPose.HoldTarget(remoteGuiPanel.resolvedAnchor(), remoteGuiPanel.resolvedYaw(), remoteGuiPanel.resolvedPitch(), Math.min(1.0f, remoteGuiPanel.animScale()), remoteGuiPanel.contentAlpha());
        }
        return null;
    }

    private static Vec3d livePositionOf(RemoteGuiPanel remoteGuiPanel) {
        GuiShareRemoteState guiShareRemoteState = remoteGuiPanel.state();
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient != null && minecraftClient.world != null) {
            for (PlayerEntity playerEntity : minecraftClient.world.getPlayers()) {
                String string;
                if (playerEntity == null || playerEntity.isRemoved() || (string = playerEntity.getGameProfile().name()) == null || !string.equalsIgnoreCase(guiShareRemoteState.minecraftUsername())) continue;
                return playerEntity.getEyePos();
            }
        }
        return new Vec3d(guiShareRemoteState.x(), guiShareRemoteState.y(), guiShareRemoteState.z());
    }

    private static void tickPanels(MinecraftClient minecraftClient, String string) {
        Map<String, GuiShareRemoteState> map = client.snapshotRemoteStates();
        String string2 = GuiShareController.currentWorldId(minecraftClient);
        for (GuiShareRemoteState guiShareRemoteState : map.values()) {
            RemoteGuiPanel remoteGuiPanel;
            if (guiShareRemoteState.identityKey().equalsIgnoreCase(string)) continue;
            if (!(string2.isBlank() || guiShareRemoteState.world().isBlank() || string2.equals(guiShareRemoteState.world()))) {
                remoteGuiPanel = panels.get(guiShareRemoteState.identityKey());
                if (remoteGuiPanel == null) continue;
                remoteGuiPanel.markClosed();
                continue;
            }
            remoteGuiPanel = panels.get(guiShareRemoteState.identityKey());
            if (remoteGuiPanel == null) {
                if (!guiShareRemoteState.open()) continue;
                remoteGuiPanel = new RemoteGuiPanel(guiShareRemoteState);
                panels.put(guiShareRemoteState.identityKey(), remoteGuiPanel);
            }
            remoteGuiPanel.update(guiShareRemoteState);
        }
        panels.entrySet().removeIf(entry -> {
            if (!map.containsKey(entry.getKey())) {
                ((RemoteGuiPanel)entry.getValue()).markClosed();
            }
            return ((RemoteGuiPanel)entry.getValue()).isFinished();
        });
        RemoteGuiPanelRenderer.pruneIdentities(panels.keySet());
    }

    private static Vec3d heldAnchor(MinecraftClient minecraftClient, PlayerEntity playerEntity, float f, float f2, Vec3d vec3d) {
        if (playerEntity == null) {
            return vec3d;
        }
        float f3 = minecraftClient.getRenderTickCounter() == null ? 1.0f : minecraftClient.getRenderTickCounter().getTickProgress(true);
        Vec3d vec3d2 = Vec3d.fromPolar((float)f2, (float)f);
        return playerEntity.getCameraPosVec(f3).add(vec3d2.multiply(1.05)).subtract(0.0, 0.2, 0.0);
    }
}

