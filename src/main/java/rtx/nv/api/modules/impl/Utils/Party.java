package rtx.nv.api.modules.impl.Utils;
import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BindSetting;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.party.PartyChat;
import rtx.nv.api.party.PartyClient;
import rtx.nv.api.party.PartyMarker;
import rtx.nv.api.party.voice.PartyVoice;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.pipeline.ClientPipelines;
import rtx.nv.utils.render.render2d.Render2D;

public final class Party
extends Module {
    private static final Identifier GLOW_TEXTURE = Identifier.of((String)"nv", (String)"textures/particle/glow.png");
    private static final String COLOR_CLIENT = "\u041a\u043b\u0438\u0435\u043d\u0442";
    private static final String COLOR_CUSTOM = "\u0421\u0432\u043e\u0439";
    private static final double LABEL_NEAR = 16.0;
    private static final double LABEL_FULL = 22.0;
    private static final String LABEL_TITLE_FONT = "montserrat-semibold";
    private static final String LABEL_DIST_FONT = "montserrat-medium";
    private static final int LABEL_BG = -435023336;
    private static final float IN_MS = 350.0f;
    private static final float OUT_MS = 300.0f;
    private final BindSetting bind = this.register(new BindSetting("\u041c\u0435\u0442\u043a\u0430", "\u041f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c \u043c\u0435\u0442\u043a\u0443 \u0442\u0443\u0434\u0430, \u043a\u0443\u0434\u0430 \u0441\u043c\u043e\u0442\u0440\u0438\u0442\u0435.").setKey(-1));
    private final SliderSetting duration = this.register(new SliderSetting("\u0412\u0440\u0435\u043c\u044f \u043c\u0435\u0442\u043a\u0438", "\u0421\u043a\u043e\u043b\u044c\u043a\u043e \u0441\u0435\u043a\u0443\u043d\u0434 \u0432\u0438\u0434\u043d\u0430 \u043c\u0435\u0442\u043a\u0430 (\u0434\u043b\u044f \u0432\u0441\u0435\u0445).").setValue(8.0f).range(1, 60).increment(1));
    private final BooleanSetting throughWalls = this.register(new BooleanSetting("\u0421\u043a\u0432\u043e\u0437\u044c \u0441\u0442\u0435\u043d\u044b", "\u0412\u0438\u0434\u0435\u0442\u044c \u043c\u0435\u0442\u043a\u0443 \u0441\u043a\u0432\u043e\u0437\u044c \u0431\u043b\u043e\u043a\u0438.", true));
    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("\u0426\u0432\u0435\u0442 \u0432 \u043a\u043e\u043c\u0430\u043d\u0434\u0435"));
    private final ModeSetting colorMode = this.register(new ModeSetting("\u0420\u0435\u0436\u0438\u043c \u0446\u0432\u0435\u0442\u0430", "\u0426\u0432\u0435\u0442 \u043c\u0435\u0442\u043a\u0438.", "\u0420\u0430\u0434\u0443\u0433\u0430", "\u0420\u0430\u0434\u0443\u0433\u0430", "\u041a\u043b\u0438\u0435\u043d\u0442", "\u0421\u0432\u043e\u0439"));
    private final ColorSetting customColor = this.register(new ColorSetting("\u0426\u0432\u0435\u0442", "\u0421\u0432\u043e\u0439 \u0446\u0432\u0435\u0442 \u043c\u0435\u0442\u043a\u0438.", new Color(0, 200, 255, 255)).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private static final String V_PTT = "\u041a\u043d\u043e\u043f\u043a\u0430";
    private static final String V_VAD = "\u041f\u043e \u0433\u043e\u043b\u043e\u0441\u0443";
    private final SeparatorSetting voiceSeparator = this.register(new SeparatorSetting("\u0413\u043e\u043b\u043e\u0441\u043e\u0432\u043e\u0439 \u0447\u0430\u0442"));
    private final BooleanSetting voiceEnabled = this.register(new BooleanSetting("\u0413\u043e\u043b\u043e\u0441 \u0432 Party", "\u041e\u0431\u0449\u0430\u0442\u044c\u0441\u044f \u0433\u043e\u043b\u043e\u0441\u043e\u043c \u0441 \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u0430\u043c\u0438 Party.", false));
    private final ModeSetting voiceActivation = this.register(new ModeSetting("\u0410\u043a\u0442\u0438\u0432\u0430\u0446\u0438\u044f", "\u041a\u0430\u043a \u0432\u043a\u043b\u044e\u0447\u0430\u0435\u0442\u0441\u044f \u043c\u0438\u043a\u0440\u043e\u0444\u043e\u043d.", "\u041a\u043d\u043e\u043f\u043a\u0430", "\u041a\u043d\u043e\u043f\u043a\u0430", "\u041f\u043e \u0433\u043e\u043b\u043e\u0441\u0443").visibleWhen(this.voiceEnabled::getValue));
    private final BindSetting voiceTalk = this.register(new BindSetting("\u0413\u043e\u0432\u043e\u0440\u0438\u0442\u044c", "\u0417\u0430\u0436\u043c\u0438\u0442\u0435, \u0447\u0442\u043e\u0431\u044b \u0433\u043e\u0432\u043e\u0440\u0438\u0442\u044c (push-to-talk).").setKey(86).visibleWhen(() -> this.voiceEnabled.getValue() && this.voiceActivation.is(V_PTT)));
    private final SliderSetting voiceThreshold = this.register(new SliderSetting("\u041f\u043e\u0440\u043e\u0433 \u0433\u043e\u043b\u043e\u0441\u0430", "\u0427\u0443\u0432\u0441\u0442\u0432\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u0430\u0432\u0442\u043e-\u0430\u043a\u0442\u0438\u0432\u0430\u0446\u0438\u0438 (%): \u0432\u044b\u0448\u0435 \u2014 \u043d\u0443\u0436\u0435\u043d \u0431\u043e\u043b\u0435\u0435 \u0447\u0451\u0442\u043a\u0438\u0439 \u0433\u043e\u043b\u043e\u0441.").setValue(50.0f).range(0, 100).increment(1).visible(() -> this.voiceEnabled.getValue() && this.voiceActivation.is(V_VAD)));
    private final SliderSetting voiceVolume = this.register(new SliderSetting("\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c \u0433\u043e\u043b\u043e\u0441\u0430", "\u0413\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c \u0433\u043e\u043b\u043e\u0441\u0430 \u0442\u0438\u043c\u043c\u0435\u0439\u0442\u043e\u0432 (%).").setValue(100.0f).range(0, 200).increment(5).visible(this.voiceEnabled::getValue));
    private final BooleanSetting voiceAgc = this.register(new BooleanSetting("\u0410\u0432\u0442\u043e-\u0433\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c (AGC)", "\u0410\u0432\u0442\u043e\u043c\u0430\u0442\u0438\u0447\u0435\u0441\u043a\u0438 \u0432\u044b\u0440\u0430\u0432\u043d\u0438\u0432\u0430\u0442\u044c \u0433\u0440\u043e\u043c\u043a\u043e\u0441\u0442\u044c \u043c\u0438\u043a\u0440\u043e\u0444\u043e\u043d\u0430.", true).visibleWhen(this.voiceEnabled::getValue));
    private final SliderSetting voiceMicGain = this.register(new SliderSetting("\u0427\u0443\u0432\u0441\u0442\u0432\u0438\u0442\u0435\u043b\u044c\u043d\u043e\u0441\u0442\u044c \u043c\u0438\u043a\u0440\u043e\u0444\u043e\u043d\u0430", "\u0423\u0441\u0438\u043b\u0435\u043d\u0438\u0435 \u043c\u0438\u043a\u0440\u043e\u0444\u043e\u043d\u0430 (%). \u0420\u0430\u0431\u043e\u0442\u0430\u0435\u0442, \u043a\u043e\u0433\u0434\u0430 AGC \u0432\u044b\u043a\u043b\u044e\u0447\u0435\u043d.").setValue(100.0f).range(0, 200).increment(5).visible(() -> this.voiceEnabled.getValue() && !this.voiceAgc.getValue()));
    private final BooleanSetting voiceDenoise = this.register(new BooleanSetting("\u0428\u0443\u043c\u043e\u043f\u043e\u0434\u0430\u0432\u043b\u0435\u043d\u0438\u0435", "\u0423\u0431\u0438\u0440\u0430\u0442\u044c \u0444\u043e\u043d\u043e\u0432\u044b\u0439 \u0448\u0443\u043c \u043c\u0438\u043a\u0440\u043e\u0444\u043e\u043d\u0430 (RNNoise).", true).visibleWhen(this.voiceEnabled::getValue));
    private final BooleanSetting voiceMute = this.register(new BooleanSetting("\u0417\u0430\u0433\u043b\u0443\u0448\u0438\u0442\u044c \u0432\u0441\u0435\u0445", "\u041d\u0435 \u0441\u043b\u044b\u0448\u0430\u0442\u044c \u0433\u043e\u043b\u043e\u0441 \u0442\u0438\u043c\u043c\u0435\u0439\u0442\u043e\u0432.", false).visibleWhen(this.voiceEnabled::getValue));
    private boolean voiceWarned;
    private final List<Label> pending = new ArrayList<Label>();
    private final List<Label> labels = new ArrayList<Label>();
    private final Vector4f projScratch = new Vector4f();
    private boolean lastDown;
    private static final float L_NAME_SIZE = 6.0f;
    private static final float L_DIST_SIZE = 6.5f;
    private static final float L_PAD_X = 5.0f;
    private static final float L_PAD_Y = 3.0f;
    private static final float L_GAP = 4.0f;
    private static final float L_DOT_R = 1.7f;
    private static final float L_DOT_GAP = 3.0f;
    private static final float L_ARROW_W = 8.0f;
    private static final float L_ARROW_H = 4.5f;
    private static final int L_DIST_COLOR = -4011820;
    private static final int DEFAULT_MARKER_COLOR = -14628609;

    public Party() {
        super("Party", "\u041c\u0435\u0442\u043a\u0438 \u0434\u043b\u044f \u0443\u0447\u0430\u0441\u0442\u043d\u0438\u043a\u043e\u0432 Party. \u041d\u0430\u0436\u043c\u0438\u0442\u0435 \u0431\u0438\u043d\u0434, \u0447\u0442\u043e\u0431\u044b \u043f\u043e\u0441\u0442\u0430\u0432\u0438\u0442\u044c ping.", Category.UTILS);
        this.colorMode.setChangeListener(this::pushColor);
        this.customColor.setChangeListener(this::pushColor);
        this.pushColor();
        this.voiceEnabled.setChangeListener(this::onVoiceToggle);
        this.voiceActivation.setChangeListener(this::pushVoice);
        this.voiceThreshold.setChangeListener(this::pushVoice);
        this.voiceVolume.setChangeListener(this::pushVoice);
        this.voiceAgc.setChangeListener(this::pushVoice);
        this.voiceMicGain.setChangeListener(this::pushVoice);
        this.voiceDenoise.setChangeListener(this::pushVoice);
        this.voiceMute.setChangeListener(this::pushVoice);
        // Keep stored fields for profile compatibility; the current voice backend is a stub.
        this.voiceSeparator.visible(() -> false);
        this.voiceEnabled.visibleWhen(() -> false);
        this.voiceActivation.visibleWhen(() -> false);
        this.voiceTalk.visibleWhen(() -> false);
        this.voiceThreshold.visible(() -> false);
        this.voiceVolume.visible(() -> false);
        this.voiceAgc.visibleWhen(() -> false);
        this.voiceMicGain.visible(() -> false);
        this.voiceDenoise.visibleWhen(() -> false);
        this.voiceMute.visibleWhen(() -> false);
    }

    private static float smooth(float f) {
        f = MathHelper.clamp((float)f, (float)0.0f, (float)1.0f);
        return f * f * f * (f * (f * 6.0f - 15.0f) + 10.0f);
    }

    @EventHandler
    public void onHud(HudRenderEvent hudRenderEvent) {
        if (this.labels.isEmpty()) {
            return;
        }
        DrawContext drawContext = hudRenderEvent.getGraphics();
        for (Label label : this.labels) {
            this.drawLabel(drawContext, label);
        }
    }

    @Override
    protected void onDisable() {
        this.lastDown = false;
        this.labels.clear();
        PartyVoice.INSTANCE.setPttHeld(false);
        PartyVoice.INSTANCE.stop();
        PartyClient.INSTANCE.stop();
    }

    @EventHandler
    public void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre()) {
            return;
        }
        if (PartyVoice.INSTANCE.isAvailable() && this.voiceEnabled.getValue()) {
            String string = PartyVoice.INSTANCE.lastError();
            if (string != null && !this.voiceWarned) {
                this.voiceWarned = true;
                PartyChat.printNotice((String)"error", (String)("\u0413\u043e\u043b\u043e\u0441 \u043d\u0435\u0434\u043e\u0441\u0442\u0443\u043f\u0435\u043d: " + string));
            }
            if (this.voiceActivation.is(V_PTT)) {
                boolean bl = this.voiceTalk.isBound() && this.mc.getWindow() != null && this.mc.currentScreen == null && this.mc.player != null;
                boolean bl2 = bl && this.voiceTalk.getValue().isDown(this.mc.getWindow().getHandle());
                PartyVoice.INSTANCE.setPttHeld(bl2);
            }
        }
        if (this.mc.getWindow() == null || this.mc.currentScreen != null || this.mc.player == null || this.mc.world == null) {
            this.lastDown = false;
                        return;
        }
        if (!this.bind.isBound()) {
            this.lastDown = false;
        } else {
            boolean bl = this.bind.getValue().isDown(this.mc.getWindow().getHandle());
            if (bl && !this.lastDown) {
                this.onBindPressed();
            }
            this.lastDown = bl;
        }
    }

    @Override
    protected void onEnable() {
        PartyClient.INSTANCE.start();
        this.pushColor();
        this.voiceWarned = false;
        if (PartyVoice.INSTANCE.isAvailable() && this.voiceEnabled.getValue()) {
            PartyVoice.INSTANCE.start();
            this.pushVoice();
        }
    }

    private static int markerColor(PartyMarker partyMarker, int n, float f) {
        int n2 = partyMarker.color() != 0 ? partyMarker.color() | 0xFF000000 : -14628609;
        return ColorUtil.multAlpha(n2, f);
    }

    private void ring(VertexConsumer vertexConsumer, MatrixStack.Entry entry, PartyMarker partyMarker, int n, float f, float f2, float f3, float f4, float f5) {
        float f6 = Math.max(0.01f, f - f2);
        float f7 = f + f2;
        for (int i = 0; i < n; ++i) {
            double d = Math.toRadians((double)i * (360.0 / (double)n));
            double d2 = Math.toRadians((double)(i + 1) * (360.0 / (double)n));
            float f8 = (float)Math.cos(d);
            float f9 = (float)Math.sin(d);
            float f10 = (float)Math.cos(d2);
            float f11 = (float)Math.sin(d2);
            int n2 = Party.markerColor(partyMarker, (int)((double)i * (360.0 / (double)n)), f4 * f3);
            if (ColorUtil.alpha(n2) <= 1) continue;
            int n3 = ColorUtil.multAlpha(n2, 0.0f);
            Party.band(vertexConsumer, entry, f8, f9, f10, f11, f6, f, f5, n3, n2);
            Party.band(vertexConsumer, entry, f8, f9, f10, f11, f, f7, f5, n2, n3);
        }
    }

    private Vec3d raycastTarget() {
        Vec3d vec3d = this.mc.player.getCameraPosVec(1.0f);
        Vec3d vec3d2 = this.mc.player.getRotationVec(1.0f);
        double d = 200.0;
        Vec3d vec3d3 = vec3d.add(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d);
        BlockHitResult blockHitResult = this.mc.world.raycast(new RaycastContext(vec3d, vec3d3, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, (Entity)(Object)this.mc.player));
        Vec3d vec3d4 = blockHitResult.getType() != HitResult.Type.MISS ? blockHitResult.getPos() : vec3d3;
        double d2 = vec3d.distanceTo(vec3d4);
        Box box = this.mc.player.getBoundingBox().stretch(vec3d2.x * d, vec3d2.y * d, vec3d2.z * d).expand(1.0);
        for (Entity entity2 : this.mc.world.getOtherEntities((Entity)(Object)this.mc.player, box, entity -> entity != null && entity.canHit() && !entity.isSpectator())) {
            double d3;
            Optional optional = entity2.getBoundingBox().expand(0.25).raycast(vec3d, vec3d3);
            if (!optional.isPresent() || !((d3 = vec3d.distanceTo((Vec3d)optional.get())) < d2)) continue;
            d2 = d3;
            vec3d4 = entity2.getEntityPos();
        }
        return vec3d4;
    }

    private float renderMarker(MatrixStack matrixStack, VertexConsumer vertexConsumer, Quaternionf quaternionf, Vec3d vec3d, PartyMarker partyMarker, long l) {
        float f;
        float f2 = MathHelper.clamp((float)((float)partyMarker.ageMs(l) / 350.0f), (float)0.0f, (float)1.0f);
        float f3 = f2 * (f = MathHelper.clamp((float)((float)partyMarker.remainingMs(l) / 300.0f), (float)0.0f, (float)1.0f));
        if (f3 <= 0.01f) {
            return f3;
        }
        float f4 = Math.min(Party.smooth(f2), Party.smooth(f));
        float f5 = 0.97f + 0.03f * (float)Math.sin((double)l / 360.0);
        Vec3d vec3d2 = partyMarker.pos();
        matrixStack.push();
        matrixStack.translate(vec3d2.x - vec3d.x, vec3d2.y - vec3d.y, vec3d2.z - vec3d.z);
        MatrixStack.Entry entry = matrixStack.peek();
        float f6 = 0.78f * f4 * f5;
        float f7 = 0.1f;
        this.ring(vertexConsumer, entry, partyMarker, 56, f6, 0.4f, 0.16f, f3, f7);
        this.ring(vertexConsumer, entry, partyMarker, 64, f6, 0.085f, 0.82f, f3, f7 + 0.004f);
        Party.skirt(vertexConsumer, entry, partyMarker, 48, f6, f7, 0.3f * f4, f3);
        Vector3f vector3f = quaternionf.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        float f8 = vector3f.x();
        float f9 = vector3f.z();
        float f10 = (float)Math.sqrt(f8 * f8 + f9 * f9);
        if (f10 < 1.0E-4f) {
            f8 = 1.0f;
            f9 = 0.0f;
        } else {
            f8 /= f10;
            f9 /= f10;
        }
        float f11 = 1.55f * f4;
        Party.shaft(vertexConsumer, entry, partyMarker, f8, f9, 0.17f * f4, f7, f11, f3 * 0.38f);
        Party.shaft(vertexConsumer, entry, partyMarker, f8, f9, 0.07f * f4, f7, f11 * 1.03f, f3 * 0.95f);
        matrixStack.pop();
        return f3;
    }

    private void onBindPressed() {
        long l;
        if (this.mc.world == null || this.mc.player == null) {
            return;
        }
        Vec3d vec3d = this.raycastTarget();
        if (vec3d == null) {
            return;
        }
        String string = this.mc.world.getRegistryKey().getValue().toString();
        if (!PartyClient.INSTANCE.sendPing(vec3d.x, vec3d.y, vec3d.z, string, l = (long)this.duration.getInt() * 1000L)) {
            PartyChat.printNotice((String)"error", (String)"\u041d\u0435\u0442 \u0441\u0432\u044f\u0437\u0438 \u0441 Party-\u0441\u0435\u0440\u0432\u0435\u0440\u043e\u043c.");
        }
    }

    private void resolveLabelOverlap() {
        if (this.pending.size() <= 1) {
            this.labels.addAll(this.pending);
            return;
        }
        this.pending.sort((label, label2) -> Double.compare(label.distSqr, label2.distSqr));
        for (Label label3 : this.pending) {
            boolean bl = false;
            for (Label label4 : this.labels) {
                if (!Party.overlaps(label3, label4, 2.0f)) continue;
                bl = true;
                break;
            }
            if (bl) continue;
            this.labels.add(label3);
        }
    }

    private void projectLabel(Matrix4f matrix4f, Matrix4f matrix4f2, Vec3d vec3d, float f, float f2, PartyMarker partyMarker, Vec3d vec3d2, long l) {
        float f3;
        float f4 = (float)(vec3d2.y + 2.1);
        Vector4f vector4f = this.projScratch.set((float)(vec3d2.x - vec3d.x), (float)((double)f4 - vec3d.y), (float)(vec3d2.z - vec3d.z), 1.0f);
        matrix4f.transform(vector4f);
        matrix4f2.transform(vector4f);
        if (vector4f.w <= 1.0E-4f) {
            return;
        }
        float f5 = (vector4f.x / vector4f.w * 0.5f + 0.5f) * f;
        float f6 = (1.0f - (vector4f.y / vector4f.w * 0.5f + 0.5f)) * f2;
        if (Float.isNaN(f5) || Float.isNaN(f6)) {
            return;
        }
        double d = this.mc.player.squaredDistanceTo(vec3d2.x, vec3d2.y, vec3d2.z);
        double d2 = Math.sqrt(d);
        float f7 = Party.smooth(MathHelper.clamp((float)((float)((d2 - 16.0) / 6.0)), (float)0.0f, (float)1.0f));
        float f8 = MathHelper.clamp((float)((float)partyMarker.ageMs(l) / 350.0f), (float)0.0f, (float)1.0f);
        float f9 = f8 * (f3 = MathHelper.clamp((float)((float)partyMarker.remainingMs(l) / 300.0f), (float)0.0f, (float)1.0f)) * f7;
        if (f9 <= 0.02f) {
            return;
        }
        float f10 = 0.62f + 0.38f * Math.min(Party.smooth(f8), Party.smooth(f3));
        int n = (int)Math.round(d2);
        String string = partyMarker.from();
        float f11 = Render2D.msdfWidth(LABEL_TITLE_FONT, string, 6.0f);
        float f12 = Render2D.msdfWidth(LABEL_DIST_FONT, n + " \u043c", 6.5f);
        float f13 = 16.4f + f11 + 4.0f + f12;
        float f14 = Math.max(6.0f, 6.5f) + 6.0f;
        float f15 = f5 - f13 * 0.5f;
        float f16 = f6 - 4.5f - f14;
        boolean bl = false;
        int n2 = partyMarker.color() != 0 ? partyMarker.color() | 0xFF000000 : -14628609;
        this.pending.add(new Label(f5, f15, f16, f13, f14, string, n, f9, f10, d, bl, n2));
    }


    private int broadcastColor() {
        if (this.colorMode.is(COLOR_CLIENT)) {
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            return interfaceModule != null ? interfaceModule.clientPrimaryColorOpaque() : -14628609;
        }
        return this.customColor.getColor() | 0xFF000000;
    }

    private static void downTriangle(float f, float f2, float f3, float f4, int n) {
        int n2 = Math.max(4, Math.round(f4 * 2.0f));
        for (int i = 0; i < n2; ++i) {
            float f5 = (float)i / (float)n2;
            float f6 = f3 * (1.0f - f5);
            Render2D.rect(f - f6 * 0.5f, f2 + f4 * f5, f6, f4 / (float)n2 + 0.7f, n);
        }
    }

    private void onVoiceToggle() {
        if (PartyVoice.INSTANCE.isAvailable() && this.voiceEnabled.getValue() && this.isEnabled()) {
            PartyVoice.INSTANCE.start();
            this.pushVoice();
        } else {
            PartyVoice.INSTANCE.setPttHeld(false);
            PartyVoice.INSTANCE.stop();
        }
    }




    private static boolean overlaps(Label label, Label label2, float f) {
        return label.boxX - f < label2.boxX + label2.boxW + f && label.boxX + label.boxW + f > label2.boxX - f && label.boxY - f < label2.boxY + label2.boxH + f && label.boxY + label.boxH + f > label2.boxY - f;
    }

    private static void shaft(VertexConsumer vertexConsumer, MatrixStack.Entry entry, PartyMarker partyMarker, float f, float f2, float f3, float f4, float f5, float f6) {
        int n = Party.markerColor(partyMarker, 0, f6);
        if (ColorUtil.alpha(n) <= 1) {
            return;
        }
        int n2 = ColorUtil.multAlpha(n, 0.0f);
        float f7 = f * f3;
        float f8 = f2 * f3;
        float f9 = f4 + f5;
        vertexConsumer.vertex(entry, -f7, f4, -f8).texture(0.0f, 0.5f).color(n);
        vertexConsumer.vertex(entry, f7, f4, f8).texture(1.0f, 0.5f).color(n);
        vertexConsumer.vertex(entry, f7, f9, f8).texture(1.0f, 0.5f).color(n2);
        vertexConsumer.vertex(entry, -f7, f9, -f8).texture(0.0f, 0.5f).color(n2);
    }

    private void pushVoice() {
        PartyVoice.INSTANCE.setActivationMode(this.voiceActivation.is(V_VAD) ? PartyVoice.ActivationMode.VOICE : PartyVoice.ActivationMode.PTT);
        PartyVoice.INSTANCE.setVadSensitivity(this.voiceThreshold.getFloat() / 100.0f);
        PartyVoice.INSTANCE.setOutputGain(this.voiceVolume.getFloat() / 100.0f);
        PartyVoice.INSTANCE.setAgc(this.voiceAgc.getValue());
        PartyVoice.INSTANCE.setInputGain(this.voiceMicGain.getFloat() / 100.0f);
        PartyVoice.INSTANCE.setDenoise(this.voiceDenoise.getValue());
        PartyVoice.INSTANCE.setMuted(this.voiceMute.getValue());
    }

    private static void skirt(VertexConsumer vertexConsumer, MatrixStack.Entry entry, PartyMarker partyMarker, int n, float f, float f2, float f3, float f4) {
        int n2 = Party.markerColor(partyMarker, 0, f4 * 0.4f);
        if (ColorUtil.alpha(n2) <= 1) {
            return;
        }
        int n3 = ColorUtil.multAlpha(n2, 0.0f);
        float f5 = f2 + f3;
        for (int i = 0; i < n; ++i) {
            double d = Math.toRadians((double)i * (360.0 / (double)n));
            double d2 = Math.toRadians((double)(i + 1) * (360.0 / (double)n));
            float f6 = (float)Math.cos(d) * f;
            float f7 = (float)Math.sin(d) * f;
            float f8 = (float)Math.cos(d2) * f;
            float f9 = (float)Math.sin(d2) * f;
            int n4 = Party.markerColor(partyMarker, (int)((double)i * (360.0 / (double)n)), f4 * 0.4f);
            int n5 = ColorUtil.multAlpha(n4, 0.0f);
            vertexConsumer.vertex(entry, f6, f2, f7).texture(0.5f, 0.5f).color(n4);
            vertexConsumer.vertex(entry, f8, f2, f9).texture(0.5f, 0.5f).color(n4);
            vertexConsumer.vertex(entry, f8, f5, f9).texture(0.5f, 0.5f).color(n5);
            vertexConsumer.vertex(entry, f6, f5, f7).texture(0.5f, 0.5f).color(n5);
        }
    }

    private void pushColor() {
        PartyClient.INSTANCE.setLocalColor(this.broadcastColor(), false);
    }

    private static void band(VertexConsumer vertexConsumer, MatrixStack.Entry entry, float f, float f2, float f3, float f4, float f5, float f6, float f7, int n, int n2) {
        vertexConsumer.vertex(entry, f * f5, f7, f2 * f5).texture(0.5f, 0.5f).color(n);
        vertexConsumer.vertex(entry, f3 * f5, f7, f4 * f5).texture(0.5f, 0.5f).color(n);
        vertexConsumer.vertex(entry, f3 * f6, f7, f4 * f6).texture(0.5f, 0.5f).color(n2);
        vertexConsumer.vertex(entry, f * f6, f7, f2 * f6).texture(0.5f, 0.5f).color(n2);
    }

    private void drawLabel(DrawContext drawContext, Label label) {
        boolean bl;
        boolean bl2 = bl = label.scale < 0.999f;
        if (bl) {
            float f = label.boxY + label.boxH * 0.5f;
            drawContext.getMatrices().pushMatrix();
            drawContext.getMatrices().translate(label.cx, f);
            drawContext.getMatrices().scale(label.scale, label.scale);
            drawContext.getMatrices().translate(-label.cx, -f);
        }
        int n = ColorUtil.multAlpha(-435023336, label.alpha);
        int n2 = ColorUtil.multAlpha(label.colorArgb, label.alpha);
        int n3 = ColorUtil.multAlpha(-1, label.alpha);
        int n4 = ColorUtil.multAlpha(-4011820, label.alpha);
        Render2D.beginFrame(drawContext);
        Render2D.rect(label.boxX, label.boxY, label.boxW, label.boxH, label.boxH * 0.5f, n);
        Party.downTriangle(label.cx, label.boxY + label.boxH - 0.5f, 8.0f, 4.5f, n);
        Render2D.flush();
        Render2D.beginFrame(drawContext);
        float f = label.boxY + label.boxH * 0.5f;
        float f2 = label.boxX + 5.0f;
        Render2D.circle(f2 + 1.7f, f, 1.7f, n2);
        Render2D.msdfText(LABEL_TITLE_FONT, label.name, f2 += 6.4f, f - 3.0f - 0.5f, 6.0f, n3);
        Render2D.msdfText(LABEL_DIST_FONT, label.meters + " \u043c", f2 += Render2D.msdfWidth(LABEL_TITLE_FONT, label.name, 6.0f) + 4.0f, f - 3.25f - 0.5f, 6.5f, n4);
        Render2D.flush();
        if (bl) {
            drawContext.getMatrices().popMatrix();
        }
    }

    private PlayerEntity findPlayerByName(String string) {
        for (PlayerEntity playerEntity : this.mc.world.getPlayers()) {
            if (!playerEntity.getGameProfile().name().equalsIgnoreCase(string) && !playerEntity.getName().getString().equalsIgnoreCase(string)) continue;
            return playerEntity;
        }
        return null;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent worldRenderEvent) {
        this.labels.clear();
        this.pending.clear();
        CopyOnWriteArrayList<PartyMarker> copyOnWriteArrayList = PartyClient.INSTANCE.markers();
        if (copyOnWriteArrayList.isEmpty() || this.mc.world == null || this.mc.player == null) {
            return;
        }
        long l = System.currentTimeMillis();
        copyOnWriteArrayList.removeIf(partyMarker -> partyMarker.expired(l));
        if (copyOnWriteArrayList.isEmpty()) {
            return;
        }
        String string = this.mc.world.getRegistryKey().getValue().toString();
        Camera camera = worldRenderEvent.getCamera() != null ? worldRenderEvent.getCamera() : this.mc.gameRenderer.getCamera();
        Vec3d vec3d = camera.getCameraPos();
        Quaternionf quaternionf = camera.getRotation();
        Matrix4f matrix4f = worldRenderEvent.getPositionMatrix();
        Matrix4f matrix4f2 = worldRenderEvent.getProjectionMatrix();
        float f = Position.screenWidth();
        float f2 = Position.screenHeight();
        MatrixStack matrixStack = worldRenderEvent.getStack();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        RenderLayer renderLayer = this.throughWalls.getValue() ? ClientPipelines.TARGET_ESP : ClientPipelines.TARGET_CHAIN;
        VertexConsumer vertexConsumer = immediate.getBuffer(renderLayer);
        for (PartyMarker partyMarker2 : copyOnWriteArrayList) {
            float f3;
            if (!partyMarker2.finiteAndSafe() || !string.equals(partyMarker2.dim()) || (f3 = this.renderMarker(matrixStack, vertexConsumer, quaternionf, vec3d, partyMarker2, l)) <= 0.02f) continue;
            this.projectLabel(matrix4f, matrix4f2, vec3d, f, f2, partyMarker2, partyMarker2.pos(), l);
        }
        immediate.draw(renderLayer);
        this.resolveLabelOverlap();
    }

    public static final class Label {
        String name;
        int meters;
        float cx, cy;
        float boxX, boxY, boxW, boxH;
        float scale;
        float alpha;
        boolean rainbow;
        int colorArgb;
        int targetSlot;
        double distSqr;

        public Label(float cx, float boxX, float boxY, float boxW, float boxH, String name, int meters, float alpha, float scale, double distSqr, boolean rainbow, int colorArgb) {
            this.cx = cx;
            this.boxX = boxX;
            this.boxY = boxY;
            this.boxW = boxW;
            this.boxH = boxH;
            this.name = name;
            this.meters = meters;
            this.alpha = alpha;
            this.scale = scale;
            this.distSqr = distSqr;
            this.rainbow = rainbow;
            this.colorArgb = colorArgb;
        }
    }
}

