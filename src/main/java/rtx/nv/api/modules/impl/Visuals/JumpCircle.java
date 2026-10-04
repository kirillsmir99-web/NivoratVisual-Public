package rtx.nv.api.modules.impl.Visuals;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import rtx.nv.api.events.EventHandler;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.player.JumpEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.NumberSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.math.MathUtils;
import rtx.nv.utils.render.post.jumpdistort.JumpDistortRenderer;
import rtx.nv.utils.render.render2d.ClientPalette;

public class JumpCircle extends Module {
    private static final int CLIENT_COLOR_FIRST = ColorUtil.rgba(127, 242, 255, 255);
    private static final int CLIENT_COLOR_SECOND = ColorUtil.rgba(255, 50, 150, 255);
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();
    private static final String COLOR_CLIENT = "Клиент";
    private static final String COLOR_CUSTOM = "Свой";
    private static final String ANIMATION_NORMAL = "Обычная";
    private static final String ANIMATION_ELASTIC = "Эластичная";
    private static final String ANIMATION_BACK_OUT = "Назад";
    private static final float GLOW_SMOKE_INTENSITY = 0.6f;
    private static JumpCircle instance;
    private final List<JumpRenderer> circles = new ArrayList<>();
    private final Matrix4f invViewProj = new Matrix4f();
    private final float[] uniformScratch = new float[412];

    private final SeparatorSetting generalSeparator = this.register(new SeparatorSetting("Основное"));
    private final ModeSetting style = this.register(new ModeSetting("Стиль", "Визуальный стиль эффекта круга.", "Ring", "Ring", "Ripple", "Segments", "Prism", "Pulse"));
    private final NumberSetting maxTime = this.register(new NumberSetting("Время", "Время жизни круга.", 1500.0, 500.0, 5000.0, 50.0));
    private final NumberSetting range = this.register(new NumberSetting("Размер", "Размер круга.", 3.0, 0.5, 5.0, 0.1));
    private final ModeSetting easingMode = this.register(new ModeSetting("Анимация", "Анимация раскрытия круга.", "Обычная", "Обычная", "Эластичная", "Назад"));
    private final SeparatorSetting distortSeparator = this.register(new SeparatorSetting("Искажение"));
    private final NumberSetting distortStrength = this.register(new NumberSetting("Сила искажения", "Насколько сильно волна искривляет мир.", 1.0, 0.2, 3.0, 0.1));
    private final NumberSetting distortThickness = this.register(new NumberSetting("Толщина", "Толщина кольца волны (относительно радиуса).", 0.5, 0.3, 0.8, 0.05));
    private final NumberSetting distortSaturation = this.register(new NumberSetting("Насыщенность", "Насыщенность цвета внутри ряби: -1 серый, 0 обычный, +1 яркий.", 0.0, -1.0, 1.0, 0.05));
    private final BooleanSetting distortWarp = this.register(new BooleanSetting("Турбулентность", "Добавляет волнистое искривление мира внутри ряби.", false));
    private final NumberSetting distortWarpStrength = this.register(new NumberSetting("Сила турбулентности", "Сила внутреннего волнистого искривления.", 0.5, 0.1, 2.0, 0.1).visibleWhen(this.distortWarp::getValue));
    private final SeparatorSetting glowSeparator = this.register(new SeparatorSetting("Свечение"));
    private final BooleanSetting distortGlow = this.register(new BooleanSetting("Свечение", "Светящийся дым, поднимающийся из кольца и испаряющийся вверх.", false));
    private final NumberSetting distortGlowHeight = this.register(new NumberSetting("Высота свечения", "Высота поднимающейся колонны дыма.", 22.0, 15.0, 45.0, 1.0).visibleWhen(this.distortGlow::getValue));
    private final NumberSetting distortGlowWidth = this.register(new NumberSetting("Ширина свечения", "Толщина дымовой стенки.", 18.0, 10.0, 30.0, 1.0).visibleWhen(this.distortGlow::getValue));
    private final NumberSetting distortGlowTint = this.register(new NumberSetting("Сила цвета свечения", "Насколько сильно окрашивается свечение: 30% слабо, 100% полный цвет.", 70.0, 30.0, 100.0, 1.0).visibleWhen(this.distortGlow::getValue));
    private final NumberSetting distortGlowAlpha = this.register(new NumberSetting("Прозрачность свечения", "Непрозрачность свечения: 20% слабо, 80% сильно.", 60.0, 20.0, 80.0, 1.0).visibleWhen(this.distortGlow::getValue));
    private final SeparatorSetting tintSeparator = this.register(new SeparatorSetting("Подкраска"));
    private final BooleanSetting distortTint = this.register(new BooleanSetting("Подкрашивать цветом", "Подкрашивает волну искажения цветами градиента ниже.", false));
    private final NumberSetting distortTintStrength = this.register(new NumberSetting("Сила цвета", "Насколько сильно окрашивается волна: 0% нет, 100% полный цвет.", 55.0, 0.0, 100.0, 1.0).visibleWhen(this.distortTint::getValue));
    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("Цвет").visible(this::colorsApply));
    private final ModeSetting colorMode = this.register(new ModeSetting("Режим цвета", "Режим цвета круга.", "Клиент", "Клиент", "Свой"));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("Второй цвет", "Использовать второй свой цвет.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("Цвет", "Основной цвет круга.", new Color(255, 255, 255, 255)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("Цвет 2", "Второй цвет круга.", new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)));

    public JumpCircle() {
        super("Jump Circle", "Рисует эффект круга при прыжке.", Category.VISUALS);
        instance = this;
        this.colorMode.visibleWhen(this::colorsApply);
        this.useSecondColor.visibleWhen(() -> this.colorsApply() && this.colorMode.is(COLOR_CUSTOM));
        this.customColor.visibleWhen(() -> this.colorsApply() && this.colorMode.is(COLOR_CUSTOM));
        this.customSecondColor.visibleWhen(() -> this.colorsApply() && this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue());
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public static JumpCircle getInstance() {
        JumpCircle jumpCircle = ModuleManager.get().get(JumpCircle.class);
        return jumpCircle != null ? jumpCircle : instance;
    }

    private boolean prevOnGround = true;
    private double prevY = 0.0;
    private long lastSpawnTime = 0L;

    @EventHandler
    private void onJump(JumpEvent jumpEvent) {
        if (this.mc.player == null || jumpEvent.getPlayer() != this.mc.player) {
            return;
        }
        this.addCircleForEntity(this.mc.player);
    }

    @EventHandler
    private void onTick(TickEvent tickEvent) {
        if (!tickEvent.isPre() || this.mc.player == null) {
            return;
        }
        boolean onGround = this.mc.player.isOnGround();
        double currentY = this.mc.player.getY();
        boolean spacePressed = this.mc.options.jumpKey.isPressed();
        boolean upwardJump = !onGround && this.prevOnGround && (this.mc.player.getVelocity().y > 0.02 || currentY > this.prevY + 0.02);
        if ((spacePressed && onGround) || upwardJump) {
            this.addCircleForEntity(this.mc.player);
        }
        this.prevOnGround = onGround;
        this.prevY = currentY;
    }

    private static float backOut(float f) {
        return (float)(1.0 + 2.70158 * Math.pow((double)f - 1.0, 3.0) + 1.70158 * Math.pow((double)f - 1.0, 2.0));
    }

    private static float elasticOut(float f) {
        if (f == 0.0f || f == 1.0f) {
            return f;
        }
        return (float)(Math.pow(2.0, -10.0 * (double)f) * Math.sin(((double)f * 10.0 - 0.75) * 2.0943951023931953) + 1.0);
    }


    private static void putColor(float[] fArray, int n, int n2) {
        fArray[n] = (float)(n2 >> 16 & 0xFF) / 255.0f;
        fArray[n + 1] = (float)(n2 >> 8 & 0xFF) / 255.0f;
        fArray[n + 2] = (float)(n2 & 0xFF) / 255.0f;
    }

    private static int fade(int n, int n2, int n3, int n4) {
        int n5 = (int)((System.currentTimeMillis() / (long)Math.max(1, n) + (long)n2) % 360L);
        n5 = n5 >= 180 ? 360 - n5 : n5;
        return ColorUtil.lerpColor(n3, n4, (float)n5 / 180.0f);
    }

    @Override
    protected void onDisable() {
        this.circles.clear();
        JumpDistortRenderer.clear();
    }

    private float getEasing(float f) {
        float f2 = MathHelper.clamp(f, 0.0f, 1.0f);
        return switch (this.easingMode.getSelected()) {
            case ANIMATION_ELASTIC -> JumpCircle.elasticOut(f2);
            case ANIMATION_BACK_OUT -> JumpCircle.backOut(f2);
            default -> 1.0f - (float) Math.pow(1.0f - f2, 3.0);
        };
    }

    private void drawRingQuad(VertexConsumer consumer, MatrixStack.Entry entry,
                              double cx, double cy, double cz,
                              float rIn, float rOut,
                              float cos1, float sin1, float cos2, float sin2,
                              int cIn1, int cOut1, int cOut2, int cIn2) {
        float x1 = (float) (cx + cos1 * rIn);
        float z1 = (float) (cz + sin1 * rIn);
        float x2 = (float) (cx + cos1 * rOut);
        float z2 = (float) (cz + sin1 * rOut);
        float x3 = (float) (cx + cos2 * rOut);
        float z3 = (float) (cz + sin2 * rOut);
        float x4 = (float) (cx + cos2 * rIn);
        float z4 = (float) (cz + sin2 * rIn);

        // Top face
        consumer.vertex(entry, x1, (float) cy, z1).color(cIn1);
        consumer.vertex(entry, x2, (float) cy, z2).color(cOut1);
        consumer.vertex(entry, x3, (float) cy, z3).color(cOut2);
        consumer.vertex(entry, x4, (float) cy, z4).color(cIn2);

        // Bottom face (reversed winding)
        consumer.vertex(entry, x4, (float) cy, z4).color(cIn2);
        consumer.vertex(entry, x3, (float) cy, z3).color(cOut2);
        consumer.vertex(entry, x2, (float) cy, z2).color(cOut1);
        consumer.vertex(entry, x1, (float) cy, z1).color(cIn1);
    }

    private int getColor(int n, float f) {
        int n2;
        int n3;
        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] nArray = ClientPalette.colors();
            if (nArray != null && nArray.length >= 2) {
                return ColorUtil.multAlpha(JumpCircle.paletteFade(8, n, nArray), f);
            }
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                n3 = interfaceModule.clientPrimaryColorOpaque();
                n2 = interfaceModule.usesSecondClientColor() ? interfaceModule.clientSecondaryColorOpaque() : n3;
            } else {
                n3 = JumpCircle.getClientColor();
                n2 = ColorUtil.lerpColor(n3, DARK_SECOND_COLOR, 0.7f);
            }
        } else {
            n3 = this.customColor.getColor();
            n2 = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : this.customColor.getColor();
        }
        if (n3 == n2) {
            return ColorUtil.multAlpha(n3, f);
        }
        return ColorUtil.multAlpha(JumpCircle.fade(8, n, n3, n2), f);
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent worldRenderEvent) {
        if (this.circles.isEmpty() || this.mc.player == null || this.mc.world == null) {
            return;
        }
        float maxLife = Math.max(1.0f, this.maxTime.getFloat());
        float maxRadius = this.range.getFloat();
        Vec3d cameraPos = worldRenderEvent.getCamera() != null ? worldRenderEvent.getCamera().getCameraPos() : this.mc.gameRenderer.getCamera().getCameraPos();
        VertexConsumerProvider.Immediate consumers = this.mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer consumer = consumers.getBuffer(RenderLayers.lightning());
        MatrixStack matrixStack = worldRenderEvent.getStack();
        MatrixStack.Entry entry = matrixStack.peek();

        String currentStyle = this.style.getSelected();
        int segments = 64;

        for (int i = this.circles.size() - 1; i >= 0; --i) {
            JumpRenderer circle = this.circles.get(i);
            float delta = circle.getDeltaTime(maxLife);
            if (delta > 1.0f || !JumpCircle.isFiniteAndSafe(circle.pos.x, circle.pos.y, circle.pos.z)) {
                this.circles.remove(i);
                continue;
            }
            float eased = this.getEasing(delta);
            float alphaFactor = (1.0f - delta) * (delta < 0.08f ? (delta / 0.08f) : 1.0f);

            double cx = circle.pos.x - cameraPos.x;
            double cy = circle.pos.y - cameraPos.y;
            double cz = circle.pos.z - cameraPos.z;

            if ("Ripple".equalsIgnoreCase(currentStyle) || "Рябь".equalsIgnoreCase(currentStyle)) {
                // Style 2: RIPPLE - 3 concentric expanding wave ripples
                float r0 = Math.max(0.05f, eased * maxRadius);
                float r1 = Math.max(0.05f, eased * maxRadius * 0.70f);
                float r2 = Math.max(0.05f, eased * maxRadius * 0.42f);
                float t0 = 0.22f * (1.0f - delta * 0.35f);
                float t1 = 0.18f * (1.0f - delta * 0.45f);
                float t2 = 0.14f * (1.0f - delta * 0.55f);

                float alpha0 = alphaFactor;
                float alpha1 = alphaFactor * 0.65f * Math.max(0.0f, 1.0f - delta * 0.35f);
                float alpha2 = alphaFactor * 0.38f * Math.max(0.0f, 1.0f - delta * 0.55f);

                for (int s = 0; s < segments; s++) {
                    float a1 = (float) s / (float) segments;
                    float a2 = (float) (s + 1) / (float) segments;
                    double rad1 = a1 * (Math.PI * 2);
                    double rad2 = a2 * (Math.PI * 2);
                    float cos1 = (float) Math.cos(rad1);
                    float sin1 = (float) Math.sin(rad1);
                    float cos2 = (float) Math.cos(rad2);
                    float sin2 = (float) Math.sin(rad2);

                    int c1 = this.getColor((int)(a1 * 360), alpha0);
                    int c2 = this.getColor((int)(a2 * 360), alpha0);
                    drawRingQuad(consumer, entry, cx, cy, cz, Math.max(0.0f, r0 - t0), r0, cos1, sin1, cos2, sin2, ColorUtil.multAlpha(c1, 0.18f), c1, c2, ColorUtil.multAlpha(c2, 0.18f));

                    if (delta > 0.03f) {
                        int c1_1 = this.getColor((int)(a1 * 360 + 60), alpha1);
                        int c2_1 = this.getColor((int)(a2 * 360 + 60), alpha1);
                        drawRingQuad(consumer, entry, cx, cy, cz, Math.max(0.0f, r1 - t1), r1, cos1, sin1, cos2, sin2, ColorUtil.multAlpha(c1_1, 0.15f), c1_1, c2_1, ColorUtil.multAlpha(c2_1, 0.15f));
                    }
                    if (delta > 0.07f) {
                        int c1_2 = this.getColor((int)(a1 * 360 + 120), alpha2);
                        int c2_2 = this.getColor((int)(a2 * 360 + 120), alpha2);
                        drawRingQuad(consumer, entry, cx, cy, cz, Math.max(0.0f, r2 - t2), r2, cos1, sin1, cos2, sin2, ColorUtil.multAlpha(c1_2, 0.12f), c1_2, c2_2, ColorUtil.multAlpha(c2_2, 0.12f));
                    }
                }
            } else if ("Segments".equalsIgnoreCase(currentStyle) || "Сегменты".equalsIgnoreCase(currentStyle)) {
                // Style 3: SEGMENTS - Rotating dashed arc segments
                float currentRadius = Math.max(0.05f, eased * maxRadius);
                float innerRadius = Math.max(0.0f, currentRadius - 0.28f);
                int numArcs = 8;
                float rot = (float) (((System.currentTimeMillis() % 10000L) / 10000.0) * (Math.PI * 2) + delta * 1.8);
                float arcSpan = (float)(Math.PI * 2 / numArcs);
                float activeSpan = arcSpan * 0.70f;
                int microSteps = 6;

                for (int a = 0; a < numArcs; a++) {
                    float startAngle = rot + a * arcSpan;
                    for (int m = 0; m < microSteps; m++) {
                        float f1 = (float) m / (float) microSteps;
                        float f2 = (float) (m + 1) / (float) microSteps;
                        double rad1 = startAngle + f1 * activeSpan;
                        double rad2 = startAngle + f2 * activeSpan;
                        float cos1 = (float) Math.cos(rad1);
                        float sin1 = (float) Math.sin(rad1);
                        float cos2 = (float) Math.cos(rad2);
                        float sin2 = (float) Math.sin(rad2);

                        int deg1 = (int)((rad1 / (Math.PI * 2) * 360.0) % 360);
                        int deg2 = (int)((rad2 / (Math.PI * 2) * 360.0) % 360);
                        int c1 = this.getColor(deg1, alphaFactor * 1.05f);
                        int c2 = this.getColor(deg2, alphaFactor * 1.05f);

                        drawRingQuad(consumer, entry, cx, cy, cz, innerRadius, currentRadius, cos1, sin1, cos2, sin2, ColorUtil.multAlpha(c1, 0.22f), c1, c2, ColorUtil.multAlpha(c2, 0.22f));
                    }
                }
            } else if ("Prism".equalsIgnoreCase(currentStyle) || "Призма".equalsIgnoreCase(currentStyle)) {
                // Style 4: PRISM - Double band with chromatic split (cyan inner band, magenta outer band, bright divider)
                float currentRadius = Math.max(0.05f, eased * maxRadius);
                float bandWidth = 0.16f * (1.0f - delta * 0.3f);
                float rInner = Math.max(0.0f, currentRadius - bandWidth);
                float rOuter = currentRadius + bandWidth;

                for (int s = 0; s < segments; s++) {
                    float a1 = (float) s / (float) segments;
                    float a2 = (float) (s + 1) / (float) segments;
                    double rad1 = a1 * (Math.PI * 2);
                    double rad2 = a2 * (Math.PI * 2);
                    float cos1 = (float) Math.cos(rad1);
                    float sin1 = (float) Math.sin(rad1);
                    float cos2 = (float) Math.cos(rad2);
                    float sin2 = (float) Math.sin(rad2);

                    int cIn1 = ColorUtil.multAlpha(rtx.nv.api.ui.theme.ThemeManager.gradientA(255), alphaFactor * 0.95f);
                    int cIn2 = ColorUtil.multAlpha(rtx.nv.api.ui.theme.ThemeManager.gradientA(255), alphaFactor * 0.95f);
                    int cMid1 = ColorUtil.multAlpha(0xFFFFFFFF, alphaFactor * 1.0f);
                    int cMid2 = ColorUtil.multAlpha(0xFFFFFFFF, alphaFactor * 1.0f);
                    drawRingQuad(consumer, entry, cx, cy, cz, rInner, currentRadius, cos1, sin1, cos2, sin2, ColorUtil.multAlpha(cIn1, 0.1f), cMid1, cMid2, ColorUtil.multAlpha(cIn2, 0.1f));

                    int cOut1 = ColorUtil.multAlpha(rtx.nv.api.ui.theme.ThemeManager.gradientB(255), alphaFactor * 0.95f);
                    int cOut2 = ColorUtil.multAlpha(rtx.nv.api.ui.theme.ThemeManager.gradientB(255), alphaFactor * 0.95f);
                    drawRingQuad(consumer, entry, cx, cy, cz, currentRadius, rOuter, cos1, sin1, cos2, sin2, cMid1, ColorUtil.multAlpha(cOut1, 0.1f), ColorUtil.multAlpha(cOut2, 0.1f), cMid2);
                }
            } else if ("Pulse".equalsIgnoreCase(currentStyle) || "Импульс".equalsIgnoreCase(currentStyle)) {
                // Style 5: PULSE - Core outer ring + soft radial gradient fill to center
                float currentRadius = Math.max(0.05f, eased * maxRadius);
                float innerRadius = Math.max(0.0f, currentRadius - 0.26f);

                for (int s = 0; s < segments; s++) {
                    float a1 = (float) s / (float) segments;
                    float a2 = (float) (s + 1) / (float) segments;
                    double rad1 = a1 * (Math.PI * 2);
                    double rad2 = a2 * (Math.PI * 2);
                    float cos1 = (float) Math.cos(rad1);
                    float sin1 = (float) Math.sin(rad1);
                    float cos2 = (float) Math.cos(rad2);
                    float sin2 = (float) Math.sin(rad2);

                    int c1 = this.getColor((int)(a1 * 360), alphaFactor);
                    int c2 = this.getColor((int)(a2 * 360), alphaFactor);
                    drawRingQuad(consumer, entry, cx, cy, cz, innerRadius, currentRadius, cos1, sin1, cos2, sin2, ColorUtil.multAlpha(c1, 0.2f), c1, c2, ColorUtil.multAlpha(c2, 0.2f));

                    // Radial inner gradient pulse from center to innerRadius
                    int centerCol = ColorUtil.multAlpha(c1, 0.38f * (1.0f - delta) * alphaFactor);
                    int edgeCol1 = ColorUtil.multAlpha(c1, 0.06f * alphaFactor);
                    int edgeCol2 = ColorUtil.multAlpha(c2, 0.06f * alphaFactor);

                    float px1 = (float)(cx + cos1 * innerRadius);
                    float pz1 = (float)(cz + sin1 * innerRadius);
                    float px2 = (float)(cx + cos2 * innerRadius);
                    float pz2 = (float)(cz + sin2 * innerRadius);

                    consumer.vertex(entry, (float)cx, (float)cy, (float)cz).color(centerCol);
                    consumer.vertex(entry, px1, (float)cy, pz1).color(edgeCol1);
                    consumer.vertex(entry, px2, (float)cy, pz2).color(edgeCol2);
                    consumer.vertex(entry, (float)cx, (float)cy, (float)cz).color(centerCol);

                    consumer.vertex(entry, (float)cx, (float)cy, (float)cz).color(centerCol);
                    consumer.vertex(entry, px2, (float)cy, pz2).color(edgeCol2);
                    consumer.vertex(entry, px1, (float)cy, pz1).color(edgeCol1);
                    consumer.vertex(entry, (float)cx, (float)cy, (float)cz).color(centerCol);
                }
            } else {
                // Style 1: RING (Default) - Clean expanding ring with soft inner fade
                float currentRadius = Math.max(0.05f, eased * maxRadius);
                float innerRadius = Math.max(0.0f, currentRadius - 0.35f * (1.0f - delta * 0.5f));

                for (int s = 0; s < segments; s++) {
                    float a1 = (float) s / (float) segments;
                    float a2 = (float) (s + 1) / (float) segments;
                    double rad1 = a1 * (Math.PI * 2);
                    double rad2 = a2 * (Math.PI * 2);
                    float cos1 = (float) Math.cos(rad1);
                    float sin1 = (float) Math.sin(rad1);
                    float cos2 = (float) Math.cos(rad2);
                    float sin2 = (float) Math.sin(rad2);

                    int color1 = this.getColor((int)(a1 * 360), alphaFactor);
                    int color2 = this.getColor((int)(a2 * 360), alphaFactor);
                    int innerColor1 = ColorUtil.multAlpha(color1, 0.15f);
                    int innerColor2 = ColorUtil.multAlpha(color2, 0.15f);

                    drawRingQuad(consumer, entry, cx, cy, cz, innerRadius, currentRadius, cos1, sin1, cos2, sin2, innerColor1, color1, color2, innerColor2);
                }
            }
        }
        consumers.draw(RenderLayers.lightning());
    }

    public void onAfterWorld(Framebuffer framebuffer, Matrix4f matrix4f, Matrix4f matrix4f2, Matrix4f matrix4f3, Camera camera) {
        if (!this.isEnabled() || framebuffer == null || camera == null || this.circles.isEmpty()) {
            return;
        }
        if (framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }
        long l = System.currentTimeMillis();
        float f = Math.max(1.0f, this.maxTime.getFloat());
        float f2 = (float)((double)this.range.getFloat() * 0.75);
        float f3 = Math.max(0.15f, f2 * this.distortThickness.getFloat() / 2.0f);
        float f4 = 0.018f * this.distortStrength.getFloat();
        float f5 = (float)framebuffer.textureWidth / (float)Math.max(1, framebuffer.textureHeight);
        boolean bl = this.distortTint.getValue();
        boolean bl2 = this.distortGlow.getValue();
        boolean bl3 = bl || bl2;
        Vec3d vec3d = camera.getCameraPos();
        this.invViewProj.set((Matrix4fc)matrix4f2).mul((Matrix4fc)matrix4f).invert();
        float[] fArray = this.uniformScratch;
        int n = 0;
        float f6 = 0.0f;
        boolean bl4 = bl3 && this.isStaticPalette();
        int n2 = bl4 ? this.getColor(0, 1.0f) : 0;
        for (JumpRenderer jumpRenderer : this.circles) {
            if (n >= 16) break;
            float f7 = jumpRenderer.getDeltaTime(f);
            if (f7 < 0.0f || f7 >= 1.0f || !JumpCircle.isFiniteAndSafe(jumpRenderer.pos.x, jumpRenderer.pos.y, jumpRenderer.pos.z)) continue;
            float f8 = this.getEasing(f7);
            float f9 = JumpCircle.clamp(f7 / 0.1f, 0.0f, 1.0f);
            float f10 = JumpCircle.clamp((1.0f - f7) / 0.3f, 0.0f, 1.0f);
            float f11 = f9 * f10;
            float f12 = f4 * f11;
            int n3 = 28 + n * 8;
            fArray[n3] = (float)(jumpRenderer.pos.x - vec3d.x);
            fArray[n3 + 1] = (float)(jumpRenderer.pos.y - vec3d.y);
            fArray[n3 + 2] = (float)(jumpRenderer.pos.z - vec3d.z);
            fArray[n3 + 3] = f8 * f2;
            fArray[n3 + 4] = f3;
            fArray[n3 + 5] = f12;
            fArray[n3 + 6] = f2;
            fArray[n3 + 7] = f11;
            if (bl3) {
                int n4 = 156 + n * 4 * 4;
                if (bl4) {
                    JumpCircle.putColor(fArray, n4, n2);
                    JumpCircle.putColor(fArray, n4 + 4, n2);
                    JumpCircle.putColor(fArray, n4 + 8, n2);
                    JumpCircle.putColor(fArray, n4 + 12, n2);
                } else {
                    int n5 = (int)f6;
                    JumpCircle.putColor(fArray, n4, this.getColor(n5, 1.0f));
                    JumpCircle.putColor(fArray, n4 + 4, this.getColor(90 + n5, 1.0f));
                    JumpCircle.putColor(fArray, n4 + 8, this.getColor(180 + n5, 1.0f));
                    JumpCircle.putColor(fArray, n4 + 12, this.getColor(270 + n5, 1.0f));
                }
            }
            f6 += 45.0f * (1.0f - f7);
            ++n;
        }
        if (n == 0) {
            return;
        }
        fArray[0] = n;
        fArray[1] = f5;
        fArray[2] = (float)(l % 100000L) / 1000.0f;
        fArray[3] = this.distortWarp.getValue() ? 0.01f * this.distortWarpStrength.getFloat() : 0.0f;
        fArray[4] = bl ? JumpCircle.clamp(this.distortTintStrength.getFloat() / 100.0f, 0.0f, 1.0f) : 0.0f;
        fArray[5] = JumpCircle.clamp(1.0f + this.distortSaturation.getFloat(), 0.0f, 2.0f);
        fArray[6] = bl2 ? 1.0f : 0.0f;
        fArray[7] = 0.6f;
        fArray[8] = JumpCircle.clamp(this.distortGlowHeight.getFloat() / 100.0f, 0.05f, 5.0f);
        fArray[9] = JumpCircle.clamp(this.distortGlowWidth.getFloat() / 100.0f, 0.05f, 5.0f);
        fArray[10] = JumpCircle.clamp(this.distortGlowTint.getFloat() / 100.0f, 0.0f, 1.0f);
        fArray[11] = JumpCircle.clamp(this.distortGlowAlpha.getFloat() / 100.0f, 0.0f, 1.0f);
        this.invViewProj.get(fArray, 12);
        JumpDistortRenderer.apply((Framebuffer)framebuffer, (float[])fArray);
    }

    private boolean colorsApply() {
        return true;
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

    private boolean isStaticPalette() {
        if (!this.colorMode.is(COLOR_CUSTOM)) {
            return false;
        }
        if (!this.useSecondColor.getValue()) {
            return true;
        }
        return this.customColor.getColor() == this.customSecondColor.getColor();
    }

    private void addCircleForEntity(Entity entity) {
        if (entity == null) return;
        long now = System.currentTimeMillis();
        if (now - this.lastSpawnTime < 150L) {
            return;
        }
        this.lastSpawnTime = now;
        double groundY = entity.getY() + 0.02;
        Vec3d vec3d = new Vec3d(entity.getX(), groundY, entity.getZ());
        if (!JumpCircle.isFiniteAndSafe(vec3d.x, vec3d.y, vec3d.z)) {
            return;
        }
        this.circles.add(new JumpRenderer(vec3d));
    }

    private static boolean isFiniteAndSafe(double d, double d2, double d3) {
        return JumpCircle.isFiniteAndSafe(d) && JumpCircle.isFiniteAndSafe(d2) && JumpCircle.isFiniteAndSafe(d3);
    }

    private static boolean isFiniteAndSafe(double d) {
        return Double.isFinite(d) && Math.abs(d) <= 3.0E7;
    }

    public static class JumpRenderer {
        public final Vec3d pos;
        public final long spawnTime;

        public JumpRenderer(Vec3d pos) {
            this.pos = pos;
            this.spawnTime = System.currentTimeMillis();
        }

        public float getDeltaTime(float maxTime) {
            long elapsed = System.currentTimeMillis() - this.spawnTime;
            return (float) elapsed / maxTime;
        }
    }
}
