package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import rtx.nv.NV;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Visuals.custompet.CustomPetVariant;
import rtx.nv.api.modules.impl.Visuals.custompet.control.CustomPetFollowerController;
import rtx.nv.api.modules.impl.Visuals.custompet.entity.CustomPetEntity;
import rtx.nv.api.modules.impl.Visuals.custompet.sync.CustomPetRemoteState;
import rtx.nv.api.modules.impl.Visuals.custompet.sync.CustomPetSyncClient;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.SelectSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;

public class CustomPet
extends Module {
    private static final long CONNECT_RETRY_MS = 5000L;
    private static final long SHARED_STATE_PUSH_MS = 80L;
    private static final long SHARED_HEARTBEAT_MS = 1000L;
    private static final long HIDDEN_STATE_PUSH_MS = 1000L;
    private static final double POSITION_SYNC_THRESHOLD_SQR = 0.0036;
    private static final float YAW_SYNC_THRESHOLD = 2.0f;
    private static final double ANIMATION_SYNC_THRESHOLD = 0.1;
    private static CustomPet instance;
    private final SeparatorSetting petSeparator = new SeparatorSetting("Питомец");
    private final SeparatorSetting syncSeparator = new SeparatorSetting("Синхронизация");
    private final SelectSetting petKind = new SelectSetting("Питомец", "Оригинальный летающий компаньон")
            .value(rtx.nv.ClientEdition.isTrial() ? new String[]{"Маскот"} : new String[]{"Дух", "Астральный дракон", "Светящийся мотылёк", "Маскот"}).selected(rtx.nv.ClientEdition.isTrial() ? "Маскот" : "Дух");
    private final BooleanSetting sharePetToOthers = new BooleanSetting("Показывать питомца другим клиентам", "Передавать позицию и вид питомца через сервис синхронизации").setValue(true);
    private final CustomPetFollowerController localController = new CustomPetFollowerController();
    private final Map<String, CustomPetEntity> remotePets = new HashMap<String, CustomPetEntity>();
    private final CustomPetSyncClient syncClient = new CustomPetSyncClient();
    private boolean petPathBroken;
    private long nextConnectAttemptAt;
    private long nextSyncPushAt;
    private String lastIdentityKey = "";
    private CustomPetVariant lastPublishedVariant = CustomPetVariant.SPIRIT;
    private String lastPublishedPetKind = "fantasy";
    private boolean lastPublishedShareState = true;
    private boolean hasPublishedPose;
    private double lastPublishedX;
    private double lastPublishedY;
    private double lastPublishedZ;
    private float lastPublishedYaw;
    private boolean lastPublishedMoving;
    private boolean lastPublishedUmbrella;
    private boolean lastPublishedAirborne;
    private double lastPublishedAnimationSpeed = 1.0;

    public CustomPet() {
        super("Custom Pet", "Клиентский летающий питомец рядом с игроком", Category.VISUALS);
        instance = this;
        this.register(this.petSeparator, this.petKind, this.syncSeparator, this.sharePetToOthers);
    }

    public static CustomPet getInstance() {
        return instance;
    }

    @Override
    protected void onDisable() {
        this.resetAll();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null) {
            this.resetAll();
            return;
        }
        if (this.petPathBroken) {
            return;
        }
        try {
            CustomPetVariant customPetVariant = this.getSelectedVariant();
            boolean bl = this.isOwlSelected();
            this.localController.tick((PlayerEntity)(Object)this.mc.player, customPetVariant, bl);
            CustomPetEntity customPetEntity = this.localController.getPet();
            if (customPetEntity != null) {
                customPetEntity.setRobotType(this.getSelectedRobotType());
            }
            try {
                this.tickSync(customPetVariant);
                this.tickRemotePets();
            } catch (Throwable syncEx) {
                // Sync server unreachable, ignore
            }
        }
        catch (Throwable throwable) {
            NV.LOGGER.error("[CustomPet] Local pet tick failed", throwable);
        }
    }

    private void resetAll() {
        this.localController.reset();
        this.clearRemotePets();
        this.syncClient.disconnect("reset");
        this.nextConnectAttemptAt = 0L;
        this.nextSyncPushAt = 0L;
        this.lastIdentityKey = "";
        this.lastPublishedVariant = this.getSelectedVariant();
        this.lastPublishedShareState = this.sharePetToOthers.getValue();
        this.hasPublishedPose = false;
        this.lastPublishedX = 0.0;
        this.lastPublishedY = 0.0;
        this.lastPublishedZ = 0.0;
        this.lastPublishedYaw = 0.0f;
        this.lastPublishedMoving = false;
        this.lastPublishedUmbrella = false;
        this.lastPublishedAirborne = false;
        this.lastPublishedAnimationSpeed = 1.0;
    }

    private void tickSync(CustomPetVariant customPetVariant) {
        if (this.mc.isInSingleplayer()) {
            this.syncClient.disconnect("singleplayer");
            this.clearRemotePets();
            return;
        }
        boolean bl;
        String string = this.resolveProfileUsername();
        String string2 = this.resolveMinecraftUsername();
        if (string.isBlank() || string2.isBlank()) {
            this.syncClient.disconnect("missing_identity");
            this.clearRemotePets();
            return;
        }
        CustomPetEntity customPetEntity = this.activeLocalPet();
        if (customPetEntity == null || customPetEntity.isRemoved()) {
            return;
        }
        boolean bl2 = this.sharePetToOthers.getValue();
        String string3 = this.getSelectedPetKind();
        String string4 = this.buildIdentityKey(string, string2);
        boolean bl3 = this.hasSignificantPoseChange(customPetEntity);
        this.syncClient.setLocalState(string, string2, this.currentWorldId(), customPetVariant, this.getSelectedRobotType(), string3, bl2, customPetEntity.getX(), customPetEntity.getY(), customPetEntity.getZ(), customPetEntity.getYaw(), customPetEntity.isPetMoving(), customPetEntity.shouldUseUmbrella(), customPetEntity.isAirborneMode(), customPetEntity.getCurrentAnimationSpeed());
        long l = System.currentTimeMillis();
        if (!this.syncClient.isConnected() && !this.syncClient.isConnecting() && l >= this.nextConnectAttemptAt) {
            this.syncClient.connect(this.resolveSyncHost(), this.resolveSyncPort());
            this.nextConnectAttemptAt = l + 5000L;
        }
        boolean bl4 = bl = l >= this.nextSyncPushAt || !string4.equals(this.lastIdentityKey) || customPetVariant != this.lastPublishedVariant || !string3.equals(this.lastPublishedPetKind) || bl2 != this.lastPublishedShareState || bl2 && bl3;
        if (bl) {
            this.syncClient.pushState();
            this.lastIdentityKey = string4;
            this.lastPublishedVariant = customPetVariant;
            this.lastPublishedPetKind = string3;
            this.lastPublishedShareState = bl2;
            this.capturePublishedPose(customPetEntity);
            this.nextSyncPushAt = l + (bl2 && bl3 ? 80L : (bl2 ? 1000L : 1000L));
        }
    }

    private void tickRemotePets() {
        Map<String, CustomPetRemoteState> map = this.syncClient.snapshotRemoteStates();
        String string = this.buildIdentityKey(this.resolveProfileUsername(), this.resolveMinecraftUsername());
        HashSet<String> hashSet = new HashSet<String>();
        for (CustomPetRemoteState customPetRemoteState : map.values()) {
            PlayerEntity playerEntity;
            if (!customPetRemoteState.active() || customPetRemoteState.identityKey().equalsIgnoreCase(string) || (playerEntity = this.findPlayerByName(customPetRemoteState.minecraftUsername())) == null || playerEntity == this.mc.player || playerEntity.isRemoved()) continue;
            CustomPetEntity customPetEntity = this.ensureRemotePet(customPetRemoteState);
            customPetEntity.applyNetworkState(customPetRemoteState);
            hashSet.add(customPetRemoteState.identityKey());
        }
        this.removeInactiveRemotePets(hashSet);
    }

    private int getSelectedRobotType() { return 0; }

    private String getSelectedPetKind() { return "fantasy"; }

    private String resolveSyncHost() {
        return rtx.nv.utils.net.NvHosts.SOCIAL_HOST;
    }

    private void clearRemotePets() {
        for (CustomPetEntity customPetEntity : this.remotePets.values()) {
            this.removeRemotePetEntity(customPetEntity);
        }
        this.remotePets.clear();
    }

    private String currentWorldId() {
        try {
            if (this.mc.world != null) {
                return this.mc.world.getRegistryKey().getValue().toString();
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return "";
    }

    private void capturePublishedPose(CustomPetEntity customPetEntity) {
        this.hasPublishedPose = true;
        this.lastPublishedX = customPetEntity.getX();
        this.lastPublishedY = customPetEntity.getY();
        this.lastPublishedZ = customPetEntity.getZ();
        this.lastPublishedYaw = customPetEntity.getYaw();
        this.lastPublishedMoving = customPetEntity.isPetMoving();
        this.lastPublishedUmbrella = customPetEntity.shouldUseUmbrella();
        this.lastPublishedAirborne = customPetEntity.isAirborneMode();
        this.lastPublishedAnimationSpeed = customPetEntity.getCurrentAnimationSpeed();
    }

    private PlayerEntity findPlayerByName(String string) {
        if (string == null || string.isBlank() || this.mc.world == null) {
            return null;
        }
        for (PlayerEntity playerEntity : this.mc.world.getPlayers()) {
            String string2;
            if (playerEntity == null || playerEntity.isRemoved() || (string2 = playerEntity.getGameProfile().name()) == null || !string2.equalsIgnoreCase(string)) continue;
            return playerEntity;
        }
        return null;
    }

    private boolean hasSignificantPoseChange(CustomPetEntity customPetEntity) {
        double d;
        double d2;
        if (!this.hasPublishedPose) {
            return true;
        }
        double d3 = customPetEntity.getX() - this.lastPublishedX;
        if (d3 * d3 + (d2 = customPetEntity.getY() - this.lastPublishedY) * d2 + (d = customPetEntity.getZ() - this.lastPublishedZ) * d >= 0.0036) {
            return true;
        }
        float f = Math.abs(MathHelper.wrapDegrees((float)(customPetEntity.getYaw() - this.lastPublishedYaw)));
        if (f >= 2.0f) {
            return true;
        }
        if (customPetEntity.isPetMoving() != this.lastPublishedMoving || customPetEntity.shouldUseUmbrella() != this.lastPublishedUmbrella || customPetEntity.isAirborneMode() != this.lastPublishedAirborne) {
            return true;
        }
        return Math.abs(customPetEntity.getCurrentAnimationSpeed() - this.lastPublishedAnimationSpeed) >= 0.1;
    }

    private CustomPetEntity ensureRemotePet(CustomPetRemoteState customPetRemoteState) {
        if (rtx.nv.ClientEdition.isTrial() && customPetRemoteState.variant() != CustomPetVariant.MASCOT) {
            return null;
        }
        boolean bl = customPetRemoteState.isOwl();
        CustomPetEntity customPetEntity = this.remotePets.get(customPetRemoteState.identityKey());
        if (customPetEntity != null && customPetEntity.getEntityWorld() == this.mc.world && !customPetEntity.isRemoved() && customPetEntity.isOwl() == bl) {
            customPetEntity.setPetVariant(customPetRemoteState.variant());
            return customPetEntity;
        }
        if (customPetEntity != null) {
            this.removeRemotePetEntity(customPetEntity);
        }
        CustomPetEntity customPetEntity2 = new CustomPetEntity((World)(Object)this.mc.world);
        customPetEntity2.setOwl(bl);
        customPetEntity2.setPetVariant(customPetRemoteState.variant());
        customPetEntity2.applyNetworkState(customPetRemoteState);
        customPetEntity2.snapTo(customPetRemoteState.position(), customPetRemoteState.yaw());
        this.mc.world.addEntity((Entity)customPetEntity2);
        this.remotePets.put(customPetRemoteState.identityKey(), customPetEntity2);
        return customPetEntity2;
    }

    private String resolveMinecraftUsername() {
        if (this.mc.player == null) {
            return "";
        }
        String string = this.mc.player.getGameProfile().name();
        return string == null ? "" : string.trim();
    }

    private CustomPetEntity activeLocalPet() {
        return this.localController.getPet();
    }

    private boolean isOwlSelected() { return false; }

    private String buildIdentityKey(String string, String string2) {
        if (string == null || string2 == null) {
            return "";
        }
        String string3 = string.trim();
        String string4 = string2.trim();
        if (string3.isEmpty() || string4.isEmpty()) {
            return "";
        }
        return string3 + "|" + string4;
    }

    private String resolveProfileUsername() {
        String string;
        String string2 = "";
        String string3 = string = string2 == null ? "" : string2.trim();
        if (!string.isEmpty() && !"username".equalsIgnoreCase(string)) {
            return string;
        }
        return this.resolveMinecraftUsername();
    }

    private int resolveSyncPort() {
        String string = this.decodeHiddenValue("YkgyLR8=", "TkpC");
        return Integer.parseInt(string);
    }

    public CustomPetVariant getSelectedVariant() {
        if (rtx.nv.ClientEdition.isTrial()) {
            return CustomPetVariant.MASCOT;
        }
        CustomPetVariant variant = CustomPetVariant.fromSettingValue(this.petKind.getSelected());
        return variant.isFantasy() ? variant : CustomPetVariant.SPIRIT;
    }

    private void removeRemotePetEntity(CustomPetEntity customPetEntity) {
        if (customPetEntity == null || customPetEntity.isRemoved()) {
            return;
        }
        if (this.mc.world != null) {
            this.mc.world.removeEntity(customPetEntity.getId(), Entity.RemovalReason.DISCARDED);
        }
        customPetEntity.discard();
    }

    private void removeInactiveRemotePets(Set<String> set) {
        this.remotePets.entrySet().removeIf(entry -> {
            if (set.contains(entry.getKey())) {
                return false;
            }
            this.removeRemotePetEntity((CustomPetEntity)entry.getValue());
            return true;
        });
    }

    private String decodeHiddenValue(String string, String string2) {
        byte[] byArray = Base64.getDecoder().decode(string);
        byte[] byArray2 = Base64.getDecoder().decode(string2);
        byte[] byArray3 = new byte[byArray.length];
        for (int i = 0; i < byArray.length; ++i) {
            byArray3[i] = (byte)(byArray[i] ^ byArray2[i % byArray2.length] ^ i * 17 + 31);
        }
        return new String(byArray3, StandardCharsets.UTF_8);
    }
}

