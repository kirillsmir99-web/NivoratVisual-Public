package rtx.nv.api.modules.impl.Visuals;
import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.impl.render.HudRenderEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.targetesp.TargetBeaconRenderer;
import rtx.nv.utils.render.targetesp.TargetOrbitsRenderer;
import rtx.nv.utils.render.targetesp.TargetSegmentsRenderer;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.image.ImageRenderer;
import rtx.nv.utils.render.targetesp.AuraGlowTargetEspRenderer;
import rtx.nv.utils.render.targetesp.TargetCircleRenderer;
import rtx.nv.utils.render.targetesp.TargetEspColorProvider;
import rtx.nv.utils.render.targetesp.TargetEspMath;
import rtx.nv.utils.render.targetesp.TargetEspRenderContext;

public class TargetESP
extends Module {
    private static final int CLIENT_COLOR_FIRST = ColorUtil.rgba(127, 242, 255, 255);
    private static final int CLIENT_COLOR_SECOND = ColorUtil.rgba(255, 50, 150, 255);
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();
    private static final String COLOR_CLIENT = "\u041a\u043b\u0438\u0435\u043d\u0442";
    private static final String COLOR_CUSTOM = "\u0421\u0432\u043e\u0439";
    private static final String STYLE_AURA = "\u0410\u0443\u0440\u0430";
    private static final String STYLE_CIRCLE = "\u041e\u043a\u0440\u0443\u0436\u043d\u043e\u0441\u0442\u044c";
    private static final String STYLE_ORBITS = "\u041e\u0440\u0431\u0438\u0442\u044b";
    private static final String STYLE_SEGMENTS = "\u0421\u0435\u0433\u043c\u0435\u043d\u0442\u044b";
    private static final String STYLE_BEACON = "\u041c\u0430\u044f\u043a";
    private static final String STYLE_IMAGE = "2D \u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0430";
    private static final String MARKER_TEXTURE = "nv:textures/targetesp/marker.png";
    private static final float MARKER_SIZE = 100.0f;
    private static final long HIDE_DELAY_MS = 500L;
    private static final long SWITCH_BLEND_MS = 250L;
    private static TargetESP instance;
    private final SeparatorSetting styleSeparator = this.register(new SeparatorSetting("\u0421\u0442\u0438\u043b\u044c"));
    private final ModeSetting styleMode = this.register(new ModeSetting("\u0421\u0442\u0438\u043b\u044c", "\u0412\u043d\u0435\u0448\u043d\u0438\u0439 \u0432\u0438\u0434 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438 \u0446\u0435\u043b\u0438.", "\u0410\u0443\u0440\u0430", "\u0410\u0443\u0440\u0430", "\u041e\u043a\u0440\u0443\u0436\u043d\u043e\u0441\u0442\u044c", "\u041e\u0440\u0431\u0438\u0442\u044b", "\u0421\u0435\u0433\u043c\u0435\u043d\u0442\u044b", "\u041c\u0430\u044f\u043a", "2D \u041a\u0430\u0440\u0442\u0438\u043d\u043a\u0430"));
    private final BooleanSetting markerAdditive = this.register(new BooleanSetting("\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435 \u043a\u0430\u0440\u0442\u0438\u043d\u043a\u0438", "\u0421\u043a\u043b\u0430\u0434\u044b\u0432\u0430\u0435\u0442 \u043a\u0430\u0440\u0442\u0438\u043d\u043a\u0443 \u0441 \u0444\u043e\u043d\u043e\u043c: \u0447\u0451\u0440\u043d\u044b\u0439 \u043d\u0435 \u0440\u0438\u0441\u0443\u0435\u0442\u0441\u044f, \u044f\u0440\u043a\u0438\u0435 \u043c\u0435\u0441\u0442\u0430 \u0441\u0432\u0435\u0442\u044f\u0442\u0441\u044f.", true).visible(() -> this.styleMode.is(STYLE_IMAGE)));
    private final SliderSetting circleRadius = this.register(new SliderSetting("\u0420\u0430\u0434\u0438\u0443\u0441", "\u0420\u0430\u0434\u0438\u0443\u0441 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438.").range(0.6f, 2.2f).increment(0.05f).setValue(1.4f).visible(() -> this.styleMode.is(STYLE_CIRCLE) || this.styleMode.is(STYLE_ORBITS) || this.styleMode.is(STYLE_SEGMENTS) || this.styleMode.is(STYLE_BEACON)));
    private final SliderSetting circleTilt = this.register(new SliderSetting("\u041d\u0430\u043a\u043b\u043e\u043d", "\u041d\u0430\u043a\u043b\u043e\u043d \u043f\u043b\u043e\u0441\u043a\u043e\u0441\u0442\u0438 \u043a\u0440\u0443\u0433\u0430, \u0433\u0440\u0430\u0434\u0443\u0441\u044b.").range(20.0f, 60.0f).increment(1.0f).setValue(40.0f).visible(() -> this.styleMode.is(STYLE_CIRCLE)));
    private final SliderSetting circleSpeed = this.register(new SliderSetting("\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c", "\u0421\u043a\u043e\u0440\u043e\u0441\u0442\u044c \u0432\u0440\u0430\u0449\u0435\u043d\u0438\u044f \u0438 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438.").range(0.5f, 4.0f).increment(0.1f).setValue(2.0f).visible(() -> this.styleMode.is(STYLE_CIRCLE) || this.styleMode.is(STYLE_ORBITS) || this.styleMode.is(STYLE_SEGMENTS) || this.styleMode.is(STYLE_BEACON)));
    private final SliderSetting circleThickness = this.register(new SliderSetting("\u0422\u043e\u043b\u0449\u0438\u043d\u0430", "\u0422\u043e\u043b\u0449\u0438\u043d\u0430 \u043b\u0438\u043d\u0438\u0439 \u0438 \u043a\u043e\u043b\u0435\u0446.").range(0.5f, 3.0f).increment(0.05f).setValue(1.75f).visible(() -> this.styleMode.is(STYLE_CIRCLE) || this.styleMode.is(STYLE_ORBITS) || this.styleMode.is(STYLE_SEGMENTS) || this.styleMode.is(STYLE_BEACON)));
    private final SliderSetting circleGlow = this.register(new SliderSetting("\u0421\u0438\u043b\u0430 \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f", "\u0421\u0438\u043b\u0430 \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f \u044d\u0444\u0444\u0435\u043a\u0442\u0430.").range(0.0f, 3.0f).increment(0.1f).setValue(2.0f).visible(() -> this.styleMode.is(STYLE_CIRCLE) || this.styleMode.is(STYLE_ORBITS) || this.styleMode.is(STYLE_SEGMENTS) || this.styleMode.is(STYLE_BEACON)));
    private final SliderSetting espAlpha = this.register(new SliderSetting("\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c", "\u0411\u0430\u0437\u043e\u0432\u0430\u044f \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u044d\u0444\u0444\u0435\u043a\u0442\u0430.").range(0.2f, 1.0f).increment(0.05f).setValue(1.0f).visible(() -> !this.styleMode.is(STYLE_IMAGE)));
    private final BooleanSetting hitReaction = this.register(new BooleanSetting("\u0420\u0435\u0430\u043a\u0446\u0438\u044f \u043d\u0430 \u0443\u0434\u0430\u0440", "\u0414\u0438\u043d\u0430\u043c\u0438\u0447\u0435\u0441\u043a\u0430\u044f \u0440\u0435\u0430\u043a\u0446\u0438\u044f \u043d\u0430 \u043f\u043e\u043b\u0443\u0447\u0435\u043d\u0438\u0435 \u0446\u0435\u043b\u044c\u044e \u0443\u0440\u043e\u043d\u0430.", true).visible(() -> this.styleMode.is(STYLE_ORBITS) || this.styleMode.is(STYLE_SEGMENTS) || this.styleMode.is(STYLE_BEACON)));
    private final SeparatorSetting colorsSeparator = this.register(new SeparatorSetting("\u0426\u0432\u0435\u0442\u0430"));
    private final ModeSetting colorMode = this.register(new ModeSetting("\u0420\u0435\u0436\u0438\u043c \u0446\u0432\u0435\u0442\u0430", "\u0420\u0435\u0436\u0438\u043c \u0446\u0432\u0435\u0442\u0430 \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0438 \u0446\u0435\u043b\u0438.", "\u041a\u043b\u0438\u0435\u043d\u0442", "\u041a\u043b\u0438\u0435\u043d\u0442", "\u0421\u0432\u043e\u0439"));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("\u0412\u0442\u043e\u0440\u043e\u0439 \u0446\u0432\u0435\u0442", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0432\u0442\u043e\u0440\u043e\u0439 \u0441\u0432\u043e\u0439 \u0446\u0432\u0435\u0442.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("\u0426\u0432\u0435\u0442", "\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0446\u0432\u0435\u0442.", new Color(255, 255, 255, 255)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("\u0426\u0432\u0435\u0442 2", "\u0412\u0442\u043e\u0440\u043e\u0439 \u0446\u0432\u0435\u0442.", new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)));
    private final SeparatorSetting displaySeparator = this.register(new SeparatorSetting("\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0435\u043d\u0438\u0435"));
    private final BooleanSetting onlyOnHit = this.register(new BooleanSetting("\u0422\u043e\u043b\u044c\u043a\u043e \u043f\u0440\u0438 \u0443\u0434\u0430\u0440\u0435", "\u041f\u043e\u043a\u0430\u0437\u044b\u0432\u0430\u0442\u044c \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0443 \u0446\u0435\u043b\u0438 \u0442\u043e\u043b\u044c\u043a\u043e \u043f\u043e\u0441\u043b\u0435 \u043d\u0430\u043d\u0435\u0441\u0451\u043d\u043d\u043e\u0433\u043e \u0443\u0434\u0430\u0440\u0430.", false));
    private final BooleanSetting throughWalls = this.register(new BooleanSetting("\u0421\u043a\u0432\u043e\u0437\u044c \u0441\u0442\u0435\u043d\u044b", "\u0412\u0438\u0434\u0435\u0442\u044c \u043f\u043e\u0434\u0441\u0432\u0435\u0442\u043a\u0443 \u0446\u0435\u043b\u0438 \u0441\u043a\u0432\u043e\u0437\u044c \u0441\u0442\u0435\u043d\u044b.", false));
    private final SliderSetting circleHeight = this.register(new SliderSetting("\u0412\u044b\u0441\u043e\u0442\u0430 \u043a\u0440\u0443\u0433\u0430", "\u0412\u044b\u0441\u043e\u0442\u0430 \u0441\u0432\u0435\u0442\u044f\u0449\u0435\u0433\u043e\u0441\u044f \u043a\u043e\u043b\u044c\u0446\u0430.").range(1.0f, 1.75f).increment(0.05f).setValue(1.0f).visible(() -> this.styleMode.is(STYLE_AURA)));
    private final SliderSetting brightness = this.register(new SliderSetting("\u042f\u0440\u043a\u043e\u0441\u0442\u044c", "\u042f\u0440\u043a\u043e\u0441\u0442\u044c \u0441\u0432\u0435\u0447\u0435\u043d\u0438\u044f.").range(0.1f, 0.25f).increment(0.01f).setValue(0.15f).visible(() -> this.styleMode.is(STYLE_AURA)));
    private float markerScreenX;
    private float markerScreenY;
    private boolean markerProjected;
    private boolean markerSmoothed;
    private float markerHurt;
    private LivingEntity renderedTarget;
    private Vec3d smoothedPos;
    private Vec3d transitionStartPos;
    private long switchStartMs;
    private boolean switching;
    private int currentTargetId = Integer.MIN_VALUE;
    private int hitTrackedId = Integer.MIN_VALUE;
    private int lastHurtTime;
    private boolean targetHit;
    private float alpha;
    private float hurtProgress;
    private float chainImpactProgress;
    private long lastSeenMs;
    private long lastFrameMillis = System.currentTimeMillis();
    private float spinAngle;
    private float spinSpeedCurrent = Float.NaN;

    public TargetESP() {
        super("Target ESP", "\u041f\u043e\u0434\u0441\u0432\u0435\u0447\u0438\u0432\u0430\u0435\u0442 \u043f\u043e\u0441\u043b\u0435\u0434\u043d\u044e\u044e \u0430\u0442\u0430\u043a\u043e\u0432\u0430\u043d\u043d\u0443\u044e \u0446\u0435\u043b\u044c.", Category.VISUALS);
        instance = this;
        this.useSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    public static TargetESP getInstance() {
        TargetESP targetESP = ModuleManager.get().get(TargetESP.class);
        return targetESP != null ? targetESP : instance;
    }

    private static int fade(int n, int n2, int n3, int n4) {
        int n5 = (int)((System.currentTimeMillis() / (long)Math.max(1, n) + (long)n2) % 360L);
        n5 = n5 >= 180 ? 360 - n5 : n5;
        return ColorUtil.lerpColor(n3, n4, (float)n5 / 180.0f);
    }

    @Override
    protected void onDisable() {
        this.clearRenderState();
    }

    private static int mixWhite(int n, float f) {
        int n2 = n >>> 24 & 0xFF;
        int n3 = n >> 16 & 0xFF;
        int n4 = n >> 8 & 0xFF;
        int n5 = n & 0xFF;
        n3 += Math.round((float)(255 - n3) * f);
        n4 += Math.round((float)(255 - n4) * f);
        n5 += Math.round((float)(255 - n5) * f);
        return n2 << 24 | n3 << 16 | n4 << 8 | n5;
    }

    private void clearRenderState() {
        this.renderedTarget = null;
        this.smoothedPos = null;
        this.transitionStartPos = null;
        this.switching = false;
        this.currentTargetId = Integer.MIN_VALUE;
        this.alpha = 0.0f;
        this.hurtProgress = 0.0f;
        this.chainImpactProgress = 0.0f;
        this.spinSpeedCurrent = Float.NaN;
        this.markerProjected = false;
        this.markerSmoothed = false;
        this.markerHurt = 0.0f;
    }

    private LivingEntity hoveredTarget() {
        HitResult hitResult = this.mc.crosshairTarget;
        if (!(hitResult instanceof EntityHitResult entityHitResult)) {
            return null;
        }
        net.minecraft.entity.Entity entity = entityHitResult.getEntity();
        if (!(entity instanceof LivingEntity livingEntity) || livingEntity == this.mc.player) {
            return null;
        }
        if (!livingEntity.isAlive() || livingEntity.isRemoved() || livingEntity.getHealth() <= 0.0f) {
            return null;
        }
        return livingEntity;
    }

    private int resolveColor(int n, float f) {
        int n2;
        int n3;
        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] nArray = ClientPalette.colors();
            if (nArray != null && nArray.length >= 2) {
                return ColorUtil.multAlpha(TargetESP.paletteFade(8, n, nArray), f);
            }
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                n3 = interfaceModule.clientPrimaryColorOpaque();
                n2 = interfaceModule.usesSecondClientColor() ? interfaceModule.clientSecondaryColorOpaque() : n3;
            } else {
                n3 = TargetESP.getClientColor();
                n2 = ColorUtil.lerpColor(n3, DARK_SECOND_COLOR, 0.7f);
            }
        } else {
            n3 = this.customColor.getColor();
            int n4 = n2 = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : this.customColor.getColor();
        }
        if (n3 == n2) {
            return ColorUtil.multAlpha(n3, f);
        }
        return ColorUtil.multAlpha(TargetESP.fade(8, n, n3, n2), f);
    }

    private void projectMarker(MatrixStack matrixStack, Matrix4f matrix4f, Vec3d vec3d, LivingEntity livingEntity) {
        float f = Position.screenWidth();
        float f2 = Position.screenHeight();
        if (f < 1.0f || f2 < 1.0f) {
            this.markerProjected = false;
            return;
        }
        if (matrix4f == null) {
            return;
        }
        double d = this.smoothedPos.y + (double)((livingEntity.getStandingEyeHeight() + 0.4f) * 0.5f);
        Matrix4f matrix4f2 = new Matrix4f((Matrix4fc)matrix4f).mul((Matrix4fc)matrixStack.peek().getPositionMatrix());
        Vector4f vector4f = matrix4f2.transform(new Vector4f((float)(this.smoothedPos.x - vec3d.x), (float)(d - vec3d.y), (float)(this.smoothedPos.z - vec3d.z), 1.0f));
        if (vector4f.w <= 1.0E-4f) {
            this.markerProjected = false;
            this.markerSmoothed = false;
            return;
        }
        float f3 = (vector4f.x / vector4f.w * 0.5f + 0.5f) * f;
        float f4 = (1.0f - (vector4f.y / vector4f.w * 0.5f + 0.5f)) * f2;
        if (this.markerSmoothed) {
            this.markerScreenX += (f3 - this.markerScreenX) * 0.5f;
            this.markerScreenY += (f4 - this.markerScreenY) * 0.5f;
        } else {
            this.markerScreenX = f3;
            this.markerScreenY = f4;
            this.markerSmoothed = true;
        }
        this.markerHurt = livingEntity.hurtTime > 0 ? (float)Math.abs(Math.sin((double)livingEntity.hurtTime * 0.3141592653589793)) : 0.0f;
        this.markerProjected = true;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @EventHandler
    private void onHudMarker(HudRenderEvent hudRenderEvent) {
        if (!this.isEnabled() || !this.styleMode.is(STYLE_IMAGE) || !this.markerProjected || this.alpha <= 0.01f) {
            return;
        }
        DrawContext drawContext = hudRenderEvent.getGraphics();
        if (drawContext == null) {
            return;
        }
        long l = System.currentTimeMillis();
        double d = (Math.sin((double)l / 500.0) + 1.0) / 2.0;
        float f = (float)MathHelper.clamp((double)(d * 90.0), (double)0.0, (double)90.0);
        float f2 = (float)MathHelper.clamp((double)(d * 1.1), (double)0.9, (double)1.1);
        float f3 = (float)MathHelper.clamp((double)((Math.sin((double)l / 1000.0) + 1.0) / 2.0 * 360.0), (double)0.0, (double)360.0);
        float f4 = f2;
        float f5 = 100.0f * f4;
        float f6 = 45.0f - (f - 45.0f) + f3;
        int n = this.markerColor(0);
        int n2 = this.markerColor(90);
        int n3 = this.markerColor(180);
        int n4 = this.markerColor(270);
        drawContext.getMatrices().pushMatrix();
        try {
            drawContext.getMatrices().translate(this.markerScreenX, this.markerScreenY);
            drawContext.getMatrices().rotate((float)Math.toRadians(f6));
            drawContext.getMatrices().translate(-this.markerScreenX, -this.markerScreenY);
            Render2D.beginFrame(drawContext);
            ImageRenderer.setAdditive(this.markerAdditive.getValue());
            Render2D.image(MARKER_TEXTURE, this.markerScreenX - f5 * 0.5f, this.markerScreenY - f5 * 0.5f, f5, 0.0f, (float)n, (float)n2, (float)n3, n4);
            Render2D.flush();
            ImageRenderer.setAdditive(false);
        }
        finally {
            ImageRenderer.setAdditive(false);
            drawContext.getMatrices().popMatrix();
            Render2D.beginFrame(drawContext);
        }
    }

    private int markerColor(int n) {
        int n2;
        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] nArray = ClientAccent.currentPalette();
            int n3 = nArray[Math.floorMod(n / 90, nArray.length)];
            n2 = ColorUtil.multAlpha(n3 | 0xFF000000, this.alpha);
        } else {
            boolean bl = this.useSecondColor.getValue() && (n / 90 & 1) != 0;
            n2 = ColorUtil.multAlpha(bl ? this.customSecondColor.getColor() : this.customColor.getColor(), this.alpha);
        }
        if (this.markerHurt <= 0.001f) {
            return n2;
        }
        int n4 = ColorUtil.rgba(255, 0, 0, (int)(255.0f * this.alpha));
        return ColorUtil.lerpColor(n2, n4, MathHelper.clamp((float)this.markerHurt, (float)0.0f, (float)1.0f));
    }

    private float frameDelta(long l) {
        float f = l - this.lastFrameMillis;
        this.lastFrameMillis = l;
        f = Math.max(1.0f, Math.min(f, 100.0f));
        return f / 16.666666f;
    }

    @EventHandler
    private void onAttack(rtx.nv.api.events.impl.player.AttackEntityEvent event) {
        if (event.getTarget() instanceof LivingEntity living && living != this.mc.player && living.isAlive()) {
            this.renderedTarget = living;
            this.lastSeenMs = System.currentTimeMillis();
            this.targetHit = true;
        }
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        float f;
        LivingEntity livingEntity;
        LivingEntity livingEntity2;
        long l = System.currentTimeMillis();
        float f2 = this.frameDelta(l);
        float f3 = worldRenderEvent.getPartialTicks();
        LivingEntity livingEntity3 = this.hoveredTarget();
        if (livingEntity3 != null) {
            this.renderedTarget = livingEntity3;
            this.lastSeenMs = l;
        }
        LivingEntity livingEntity4 = livingEntity2 = livingEntity3 != null ? livingEntity3 : this.renderedTarget;
        if (livingEntity2 != null) {
            if (livingEntity2.getId() != this.hitTrackedId) {
                this.hitTrackedId = livingEntity2.getId();
                this.targetHit = false;
                this.lastHurtTime = livingEntity2.hurtTime;
            }
            if (livingEntity2.hurtTime > this.lastHurtTime) {
                this.targetHit = true;
            }
            this.lastHurtTime = livingEntity2.hurtTime;
        }
        boolean bl = (livingEntity = this.renderedTarget) != null && livingEntity.isAlive() && !livingEntity.isRemoved() && livingEntity.getHealth() > 0.0f;
        boolean bl2 = !this.onlyOnHit.getValue() || this.targetHit;
        boolean bl3 = bl && bl2 && (livingEntity3 != null || l - this.lastSeenMs <= 500L);
        float f4 = bl3 ? 1.0f : 0.0f;
        float f5 = Math.min(1.0f, (bl3 ? 0.16f : 0.12f) * f2);
        this.alpha += (f4 - this.alpha) * f5;
        if (Math.abs(f4 - this.alpha) < 0.002f) {
            this.alpha = f4;
        }
        if (this.alpha <= 0.01f && !bl3) {
            this.clearRenderState();
            return;
        }
        if (bl) {
            Vec3d vec3d = livingEntity.getLerpedPos(f3);
            if (livingEntity.getId() != this.currentTargetId) {
                if (this.smoothedPos != null && this.currentTargetId != Integer.MIN_VALUE) {
                    this.transitionStartPos = this.smoothedPos;
                    this.switchStartMs = l;
                    this.switching = true;
                } else {
                    this.transitionStartPos = vec3d;
                    this.switching = false;
                }
                this.currentTargetId = livingEntity.getId();
            }
            if (this.switching && this.transitionStartPos != null) {
                f = MathHelper.clamp((float)((float)(l - this.switchStartMs) / 250.0f), (float)0.0f, (float)1.0f);
                float f6 = TargetEspMath.easeInOutQuad((float)f);
                this.smoothedPos = this.transitionStartPos.lerp(vec3d, (double)f6);
                if (f >= 1.0f) {
                    this.switching = false;
                    this.transitionStartPos = null;
                }
            } else {
                this.smoothedPos = vec3d;
            }
        }
        if (livingEntity == null || this.smoothedPos == null) {
            return;
        }
        this.hurtProgress = livingEntity.hurtTime > 0 ? (float)livingEntity.hurtTime / 10.0f : Math.max(0.0f, this.hurtProgress - 0.1f * f2);
        float f7 = livingEntity.hurtTime > 0 ? 1.0f : 0.0f;
        f = (f7 > this.chainImpactProgress ? 0.3f : 0.1f) * f2;
        this.chainImpactProgress = TargetEspMath.approach((float)this.chainImpactProgress, (float)f7, (float)f);
        Vec3d vec3d = worldRenderEvent.getCamera() == null ? this.mc.gameRenderer.getCamera().getCameraPos() : worldRenderEvent.getCamera().getCameraPos();
        MatrixStack matrixStack = worldRenderEvent.getStack();
        if (this.styleMode.is(STYLE_CIRCLE)) {
            this.renderCircle(matrixStack, vec3d, livingEntity, f2);
            return;
        }
        if (this.styleMode.is(STYLE_ORBITS)) {
            this.renderOrbits(matrixStack, vec3d, livingEntity, f2);
            return;
        }
        if (this.styleMode.is(STYLE_SEGMENTS)) {
            this.renderSegments(matrixStack, vec3d, livingEntity, f2);
            return;
        }
        if (this.styleMode.is(STYLE_BEACON)) {
            this.renderBeacon(matrixStack, vec3d, livingEntity, f2);
            return;
        }
        if (this.styleMode.is(STYLE_IMAGE)) {
            this.projectMarker(matrixStack, worldRenderEvent.getProjectionMatrix(), vec3d, livingEntity);
            return;
        }
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();
        matrixStack.push();
        matrixStack.translate(this.smoothedPos.x - vec3d.x, this.smoothedPos.y - vec3d.y, this.smoothedPos.z - vec3d.z);
        TargetEspRenderContext targetEspRenderContext = new TargetEspRenderContext(livingEntity, this.alpha, f3, l, -1, -1, this.hurtProgress, this.chainImpactProgress, this.circleHeight.getFloat(), this.brightness.getFloat(), this.throughWalls.getValue());
        AuraGlowTargetEspRenderer.render((MatrixStack)matrixStack, (VertexConsumerProvider.Immediate)immediate, (TargetEspRenderContext)targetEspRenderContext, this::resolveColor);
        AuraGlowTargetEspRenderer.endBatch((VertexConsumerProvider.Immediate)immediate, (boolean)this.throughWalls.getValue());
        matrixStack.pop();
    }

    private static int paletteFade(int n, int n2, int[] nArray) {
        int n3 = nArray.length;
        int n4 = (int)((System.currentTimeMillis() / (long)Math.max(1, n) + (long)n2) % 360L);
        float f = (float)n4 / 360.0f * (float)n3;
        int n5 = (int)f % n3;
        int n6 = (n5 + 1) % n3;
        int n7 = nArray[n5] | 0xFF000000;
        int n8 = nArray[n6] | 0xFF000000;
        return ColorUtil.lerpColor(n7, n8, f - (float)Math.floor(f)) | 0xFF000000;
    }

    private static int getClientColor() {
        float f = (MathHelper.sin((double)((float)System.currentTimeMillis() / 520.0f)) + 1.0f) / 2.0f;
        return ColorUtil.lerpColor(CLIENT_COLOR_FIRST, CLIENT_COLOR_SECOND, f);
    }

    private void renderCircle(MatrixStack matrixStack, Vec3d vec3d, LivingEntity livingEntity, float f2) {
        float f3;
        float f4 = Math.max(0.1f, livingEntity.getWidth());
        float f5 = Math.max(0.1f, livingEntity.getHeight());
        float f6 = Math.max(0.35f, (f4 * 0.5f + 0.3f) * this.circleRadius.getFloat());
        float f7 = f5 * 0.5f;
        float f8 = (float)Math.toRadians(this.circleTilt.getFloat());
        float f9 = this.circleSpeed.getFloat();
        if (Float.isNaN(this.spinSpeedCurrent)) {
            this.spinSpeedCurrent = f9;
        } else {
            f3 = Math.min(1.0f, 0.08f * f2);
            this.spinSpeedCurrent += (f9 - this.spinSpeedCurrent) * f3;
            if (Math.abs(f9 - this.spinSpeedCurrent) < 0.001f) {
                this.spinSpeedCurrent = f9;
            }
        }
        this.spinAngle += this.spinSpeedCurrent * (f2 / 60.0f);
        final float finalF3 = this.spinAngle;
        float f10 = f6 * ((this.circleThickness.getFloat() - 1.0f) / 4.0f * 0.15f);
        Vec3d vec3d2 = new Vec3d(this.smoothedPos.x, this.smoothedPos.y + (double)f7, this.smoothedPos.z);
        double d = vec3d.distanceTo(vec3d2);
        float f11 = this.alpha * (float)MathHelper.clamp((double)((d - (double)f6) / (double)Math.max(f6, 0.01f)), (double)0.0, (double)1.0);
        if (f11 <= 0.02f) {
            return;
        }
        boolean bl = this.throughWalls.getValue();
        VertexConsumerProvider.Immediate immediate2 = this.mc.getBufferBuilders().getEntityVertexConsumers();
        matrixStack.push();
        matrixStack.translate(this.smoothedPos.x - vec3d.x, this.smoothedPos.y - vec3d.y, this.smoothedPos.z - vec3d.z);
        TargetEspColorProvider targetEspColorProvider = (cn, cf) -> TargetESP.mixWhite(this.resolveColor(cn, cf), 0.2f);
        TargetEspColorProvider bloomColorProvider = (cn, cf) -> TargetESP.mixWhite(this.resolveColor(cn, cf), 0.1f);
        TargetCircleRenderer.render((VertexConsumerProvider.Immediate)immediate2, (MatrixStack)matrixStack, (float)f6, (float)f7, (float)f8, (float)finalF3, (float)f10, (float)f11, (boolean)bl, (TargetEspColorProvider)targetEspColorProvider);
        float f12 = this.circleGlow.getFloat();
        if (f12 > 0.001f) {
            for (int layer = 3; layer >= 1; --layer) {
                float thickness = f10 + f6 * 0.035f * layer;
                float opacity = f11 * MathHelper.clamp(f12, 0f, 3f) * 0.09f / layer;
                TargetCircleRenderer.render(immediate2, matrixStack, f6, f7, f8, finalF3, thickness, opacity, bl, bloomColorProvider);
            }
        }
        matrixStack.pop();
    }

    private void renderOrbits(MatrixStack matrixStack, Vec3d cameraPos, LivingEntity livingEntity, float frameDelta) {
        float width = Math.max(0.1f, livingEntity.getWidth());
        float height = Math.max(0.1f, livingEntity.getHeight());
        float radius = Math.max(0.35f, (width * 0.5f + 0.35f) * this.circleRadius.getFloat());
        float midY = height * 0.5f;
        float speed = this.circleSpeed.getFloat();
        float thickness = this.circleThickness.getFloat();
        float effAlpha = this.alpha * this.espAlpha.getFloat();

        if (effAlpha <= 0.02f) {
            return;
        }

        boolean through = this.throughWalls.getValue();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();

        matrixStack.push();
        matrixStack.translate(this.smoothedPos.x - cameraPos.x, this.smoothedPos.y - cameraPos.y, this.smoothedPos.z - cameraPos.z);

        TargetEspColorProvider colorProvider = (idx, a) -> TargetESP.mixWhite(this.resolveColor(idx, a), 0.2f);
        TargetOrbitsRenderer.render(immediate, matrixStack, radius, midY, speed, thickness, effAlpha, through, colorProvider);

        float glow = this.circleGlow.getFloat();
        if (glow > 0.001f) {
            TargetEspColorProvider bloomColorProvider = (idx, a) -> TargetESP.mixWhite(this.resolveColor(idx, a), 0.05f);
            TargetOrbitsRenderer.render(immediate, matrixStack, radius * 1.05f, midY, speed, thickness * 1.6f, effAlpha * glow * 0.25f, through, bloomColorProvider);
        }

        matrixStack.pop();
    }

    private void renderSegments(MatrixStack matrixStack, Vec3d cameraPos, LivingEntity livingEntity, float frameDelta) {
        float width = Math.max(0.1f, livingEntity.getWidth());
        float height = Math.max(0.1f, livingEntity.getHeight());
        float radius = Math.max(0.35f, (width * 0.5f + 0.35f) * this.circleRadius.getFloat());
        float midY = height * 0.5f;
        float speed = this.circleSpeed.getFloat();
        float thickness = this.circleThickness.getFloat();
        float effAlpha = this.alpha * this.espAlpha.getFloat();
        float hurt = this.hitReaction.getValue() ? this.hurtProgress : 0.0f;

        if (effAlpha <= 0.02f) {
            return;
        }

        boolean through = this.throughWalls.getValue();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();

        matrixStack.push();
        matrixStack.translate(this.smoothedPos.x - cameraPos.x, this.smoothedPos.y - cameraPos.y, this.smoothedPos.z - cameraPos.z);

        TargetEspColorProvider colorProvider = (idx, a) -> TargetESP.mixWhite(this.resolveColor(idx, a), hurt > 0.01f ? 0.45f : 0.2f);
        TargetSegmentsRenderer.render(immediate, matrixStack, radius, midY, speed, thickness, effAlpha, hurt, through, colorProvider);

        float glow = this.circleGlow.getFloat();
        if (glow > 0.001f) {
            TargetEspColorProvider bloomColorProvider = (idx, a) -> TargetESP.mixWhite(this.resolveColor(idx, a), 0.1f);
            TargetSegmentsRenderer.render(immediate, matrixStack, radius * 1.04f, midY, speed, thickness * 1.5f, effAlpha * glow * 0.22f, hurt, through, bloomColorProvider);
        }

        matrixStack.pop();
    }

    private void renderBeacon(MatrixStack matrixStack, Vec3d cameraPos, LivingEntity livingEntity, float frameDelta) {
        float width = Math.max(0.1f, livingEntity.getWidth());
        float height = Math.max(0.1f, livingEntity.getHeight());
        float radius = Math.max(0.35f, (width * 0.5f + 0.3f) * this.circleRadius.getFloat());
        float speed = this.circleSpeed.getFloat();
        float thickness = this.circleThickness.getFloat();
        float effAlpha = this.alpha * this.espAlpha.getFloat();
        float hurt = this.hitReaction.getValue() ? this.hurtProgress : 0.0f;

        if (effAlpha <= 0.02f) {
            return;
        }

        boolean through = this.throughWalls.getValue();
        VertexConsumerProvider.Immediate immediate = this.mc.getBufferBuilders().getEntityVertexConsumers();

        matrixStack.push();
        matrixStack.translate(this.smoothedPos.x - cameraPos.x, this.smoothedPos.y - cameraPos.y, this.smoothedPos.z - cameraPos.z);

        TargetEspColorProvider colorProvider = (idx, a) -> TargetESP.mixWhite(this.resolveColor(idx, a), hurt > 0.01f ? 0.35f : 0.15f);
        TargetBeaconRenderer.render(immediate, matrixStack, radius, height, speed, thickness, effAlpha, hurt, through, colorProvider);

        matrixStack.pop();
    }
}
