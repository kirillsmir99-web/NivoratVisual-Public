package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.player.PlayerEntity;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Visuals.emotions.Emotion;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionPlayback;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionRemoteState;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionSyncClient;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotecraftBridge;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionWheelOverlay;
import rtx.nv.api.modules.impl.Visuals.emotions.EmotionWheelScreen;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BindSetting.Type;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.MultiSelectSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.profile.ProfileIdentity;

public class Emotions
extends Module {
    private static final String SYNC_HOST = rtx.nv.utils.net.NvHosts.SOCIAL_HOST;
    private static final int SYNC_PORT = 32119;
    private static final long CONNECT_RETRY_MS = 5000L;
    private static final long PUSH_MS = 150L;
    private static Emotions instance;
    private final SeparatorSetting menuSeparator = this.register(new SeparatorSetting("\u041c\u0435\u043d\u044e"));
    private final BindSetting menuKey = this.register(new BindSetting("\u041a\u043b\u0430\u0432\u0438\u0448\u0430 \u043c\u0435\u043d\u044e", "\u0417\u0430\u0436\u043c\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u043e\u0442\u043a\u0440\u044b\u0442\u044c \u043a\u043e\u043b\u0435\u0441\u043e \u044d\u043c\u043e\u0446\u0438\u0439.").setType(BindSetting.Type.HOLD).setKey(66));
    private final MultiSelectSetting wheelEmotions = this.register(new MultiSelectSetting("\u042d\u043c\u043e\u0446\u0438\u0438", "\u041a\u0430\u043a\u0438\u0435 \u044d\u043c\u043e\u0446\u0438\u0438 \u043f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u0432 \u043a\u0440\u0443\u0433\u043e\u0432\u043e\u043c \u043c\u0435\u043d\u044e.").value(Emotion.displayNames()).selected(Emotion.displayNames()).minSelectedCount(2));
    private final SeparatorSetting playbackSeparator = this.register(new SeparatorSetting("\u041f\u0440\u043e\u0438\u0433\u0440\u044b\u0432\u0430\u043d\u0438\u0435"));
    private final NumberSetting speed = this.register(new NumberSetting("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "\u041c\u043d\u043e\u0436\u0438\u0442\u0435\u043b\u044c \u0441\u043a\u043e\u0440\u043e\u0441\u0442\u0438 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438.", 1.0, 0.25, 2.5, 0.05));
    private final BooleanSetting looping = this.register(new BooleanSetting("\u0417\u0430\u0446\u0438\u043a\u043b\u0438\u0442\u044c", "\u041f\u0440\u043e\u0438\u0433\u0440\u044b\u0432\u0430\u0442\u044c \u044d\u043c\u043e\u0446\u0438\u044e \u043f\u043e \u043a\u0440\u0443\u0433\u0443, \u043f\u043e\u043a\u0430 \u0435\u0451 \u043d\u0435 \u043e\u0441\u0442\u0430\u043d\u043e\u0432\u044f\u0442.", false));
    private final BooleanSetting thirdPerson = this.register(new BooleanSetting("\u0412\u0438\u0434 \u043e\u0442 \u0442\u0440\u0435\u0442\u044c\u0435\u0433\u043e \u043b\u0438\u0446\u0430", "\u041f\u0435\u0440\u0435\u043a\u043b\u044e\u0447\u0430\u0442\u044c \u043a\u0430\u043c\u0435\u0440\u0443, \u043f\u043e\u043a\u0430 \u043f\u0440\u043e\u0438\u0433\u0440\u044b\u0432\u0430\u0435\u0442\u0441\u044f \u044d\u043c\u043e\u0446\u0438\u044f.", true));
    private final BooleanSetting stopOnMove = this.register(new BooleanSetting("\u041f\u0440\u0435\u0440\u044b\u0432\u0430\u0442\u044c \u043f\u0440\u0438 \u0434\u0432\u0438\u0436\u0435\u043d\u0438\u0438", "\u041e\u0441\u0442\u0430\u043d\u0430\u0432\u043b\u0438\u0432\u0430\u0442\u044c \u044d\u043c\u043e\u0446\u0438\u044e \u043f\u0440\u0438 \u0445\u043e\u0434\u044c\u0431\u0435, \u043f\u0440\u044b\u0436\u043a\u0435 \u0438\u043b\u0438 \u0430\u0442\u0430\u043a\u0435.", true));
    private Perspective previousCamera;
    private final EmotionSyncClient sync = new EmotionSyncClient();
    private long nextConnectAt;
    private long nextPushAt;
    private long localStartedAt;

    public Emotions() {
        super("Emotions", "\u041a\u0440\u0443\u0433\u043e\u0432\u043e\u0435 \u043c\u0435\u043d\u044e \u044d\u043c\u043e\u0446\u0438\u0439 \u0441 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0435\u0439 \u043c\u043e\u0434\u0435\u043b\u0438 \u0438\u0433\u0440\u043e\u043a\u0430.", Category.VISUALS);
        instance = this;
    }

    public static Emotions getInstance() {
        Emotions emotions = ModuleManager.get().get(Emotions.class);
        return emotions != null ? emotions : instance;
    }

    @Override
    protected void onDisable() {
        EmotionPlayback.cancel();
        EmotionPlayback.clearRemote();
        EmotecraftBridge.stop();
        this.sync.disconnect();
        EmotionWheelOverlay.cancel();
        this.restoreCamera();
        if (this.mc.currentScreen instanceof EmotionWheelScreen) {
            this.mc.setScreen(null);
        }
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        List<Emotion> list;
        if (!tickEvent.isPre()) {
            return;
        }
        if (this.mc.player == null || this.mc.world == null) {
            EmotionPlayback.cancel();
            EmotionPlayback.clearRemote();
            EmotecraftBridge.stop();
            this.sync.disconnect();
            this.restoreCamera();
            return;
        }
        EmotionPlayback.setSpeed((float)this.speed.getFloat());
        EmotionPlayback.setLooping((boolean)this.looping.getValue());
        EmotionPlayback.update();
        this.tickSync();
        if (this.mc.currentScreen == null && this.isMenuKeyDown() && !(list = this.wheel()).isEmpty()) {
            this.mc.setScreen((Screen)new EmotionWheelScreen(this, list));
        }
        boolean emotecraftActive = this.mc.player != null && EmotecraftBridge.isPlayerPlayingEmote(this.mc.player.getUuid());
        if (EmotionPlayback.isPlaying() || emotecraftActive) {
            if (this.stopOnMove.getValue() && this.isBusy()) {
                EmotionPlayback.stop();
                EmotecraftBridge.stop();
            } else {
                this.applyCamera();
            }
        }
        if (EmotionPlayback.active() == null && !EmotionPlayback.isStopping() && !emotecraftActive) {
            this.restoreCamera();
        }
    }

    private void tickSync() {
        String string = this.mc.player.getGameProfile().name();
        if (string == null || string.isBlank()) {
            return;
        }
        int n = ProfileIdentity.uid();
        String string2 = n > 0 ? "uid:" + n : "name:" + string;
        this.sync.setLocalState(string2, string, this.mc.world.getRegistryKey().getValue().toString(), EmotionPlayback.active(), this.localStartedAt, this.speed.getFloat(), this.looping.getValue());
        long l = System.currentTimeMillis();
        if (!this.sync.isConnected() && !this.sync.isConnecting() && l >= this.nextConnectAt) {
            this.sync.connect(SYNC_HOST, 32119);
            this.nextConnectAt = l + 5000L;
        }
        if (l >= this.nextPushAt) {
            this.sync.push();
            this.nextPushAt = l + 150L;
        }
        EmotionPlayback.clearRemote();
        String string3 = this.mc.world.getRegistryKey().getValue().toString();
        for (EmotionRemoteState emotionRemoteState : this.sync.snapshot().values()) {
            PlayerEntity playerEntity;
            if (!string3.equals(emotionRemoteState.world()) || emotionRemoteState.minecraftUsername().equalsIgnoreCase(string) || (playerEntity = this.findPlayer(emotionRemoteState.minecraftUsername())) == null) continue;
            EmotionPlayback.setRemote((UUID)playerEntity.getUuid(), (Emotion)emotionRemoteState.emotion(), (long)emotionRemoteState.startedAt(), (float)emotionRemoteState.speed(), (boolean)emotionRemoteState.looping());
        }
    }

    public void playEmotion(Emotion emotion) {
        boolean emotecraftSuccess = EmotecraftBridge.playEmote(emotion);
        if (!emotecraftSuccess) {
            EmotionPlayback.setSpeed((float)this.speed.getFloat());
            EmotionPlayback.setLooping((boolean)this.looping.getValue());
            EmotionPlayback.play((Emotion)emotion);
        }
        this.localStartedAt = System.currentTimeMillis();
        this.applyCamera();
    }

    private void applyCamera() {
        if (!this.thirdPerson.getValue() || this.previousCamera != null) {
            return;
        }
        this.previousCamera = this.mc.options.getPerspective();
        if (this.previousCamera == Perspective.FIRST_PERSON) {
            this.mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
        }
    }

    public boolean isMenuKeyDown() {
        return this.menuKey.isBound() && this.menuKey.getValue().isDown(this.mc.getWindow().getHandle());
    }

    private void restoreCamera() {
        if (this.previousCamera == null) {
            return;
        }
        this.mc.options.setPerspective(this.previousCamera);
        this.previousCamera = null;
    }

    private boolean isBusy() {
        if (this.mc.player == null || this.mc.player.isDead()) {
            return true;
        }
        if (this.mc.player.sidewaysSpeed != 0.0f || this.mc.player.forwardSpeed != 0.0f) {
            return true;
        }
        if (this.mc.options.jumpKey.isPressed() || this.mc.options.attackKey.isPressed()) {
            return true;
        }
        return this.mc.player.handSwinging || this.mc.player.isUsingItem() || this.mc.player.isGliding() || this.mc.player.isSwimming() || this.mc.player.hurtTime > 0;
    }

    public List<Emotion> wheel() {
        ArrayList<Emotion> arrayList = new ArrayList<Emotion>();
        for (Emotion emotion : Emotion.values()) {
            if (!this.wheelEmotions.is(emotion.displayName())) continue;
            arrayList.add(emotion);
        }
        return arrayList;
    }

    private PlayerEntity findPlayer(String string) {
        for (PlayerEntity playerEntity : this.mc.world.getPlayers()) {
            if (!playerEntity.getGameProfile().name().equalsIgnoreCase(string)) continue;
            return playerEntity;
        }
        return null;
    }
}

