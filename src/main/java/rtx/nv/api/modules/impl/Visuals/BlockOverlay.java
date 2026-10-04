package rtx.nv.api.modules.impl.Visuals;

import rtx.nv.api.events.EventHandler;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import rtx.nv.api.events.impl.game.TickEvent;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.Category;
import rtx.nv.api.modules.Module;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.modules.settings.impl.BooleanSetting;
import rtx.nv.api.modules.settings.impl.ColorSetting;
import rtx.nv.api.modules.settings.impl.ModeSetting;
import rtx.nv.api.modules.settings.impl.SeparatorSetting;
import rtx.nv.api.modules.settings.impl.SliderSetting;
import rtx.nv.mixin.accessor.MultiPlayerGameModeAccessor;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.world.BlockOverlayRenderer;

public final class BlockOverlay extends Module {
    private static final long FADE_DURATION_MS = 220L;
    private static final double EPSILON = 0.0015;
    private static final int DARK_SECOND_COLOR = new Color(16, 16, 16, 75).getRGB();
    private static final String COLOR_CLIENT = "\u041a\u043b\u0438\u0435\u043d\u0442";
    private static final String COLOR_CUSTOM = "\u0421\u0432\u043e\u0439";

    // 1. Форма
    private final SeparatorSetting shapeSeparator = this.register(new SeparatorSetting("\u0424\u043e\u0440\u043c\u0430"));
    private final ModeSetting shapeMode = this.register(new ModeSetting("\u0424\u043e\u0440\u043c\u0430", "\u0421\u043f\u043e\u0441\u043e\u0431 \u043f\u043e\u0441\u0442\u0440\u043e\u0435\u043d\u0438\u044f \u0433\u0435\u043e\u043c\u0435\u0442\u0440\u0438\u0438 \u0432\u044b\u0434\u0435\u043b\u0435\u043d\u0438\u044f \u0431\u043b\u043e\u043a\u0430.", "\u041f\u043e \u0431\u043b\u043e\u043a\u0443", "\u041f\u043e \u0431\u043b\u043e\u043a\u0443", "\u041f\u043e\u043b\u043d\u044b\u0439 \u043a\u0443\u0431"));

    // 2. Контур и заливка
    private final SeparatorSetting styleSeparator = this.register(new SeparatorSetting("\u041a\u043e\u043d\u0442\u0443\u0440 \u0438 \u0437\u0430\u043b\u0438\u0432\u043a\u0430"));
    private final BooleanSetting renderOutline = this.register(new BooleanSetting("\u041a\u043e\u043d\u0442\u0443\u0440", "\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0442\u044c \u043b\u0438\u043d\u0438\u0438 \u043a\u043e\u043d\u0442\u0443\u0440\u0430 \u0445\u0438\u0442\u0431\u043e\u043a\u0441\u0430.", true));
    private final BooleanSetting renderFill = this.register(new BooleanSetting("\u0417\u0430\u043b\u0438\u0432\u043a\u0430", "\u041e\u0442\u043e\u0431\u0440\u0430\u0436\u0430\u0442\u044c \u043f\u043e\u043b\u0443\u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u044b\u0435 \u0433\u0440\u0430\u043d\u0438 \u0431\u043b\u043e\u043a\u0430.", true));
    private final ModeSetting surfaceStyle = this.register(new ModeSetting("\u041f\u043e\u0432\u0435\u0440\u0445\u043d\u043e\u0441\u0442\u044c", "\u0421\u0442\u0438\u043b\u044c \u0437\u0430\u043f\u043e\u043b\u043d\u0435\u043d\u0438\u044f \u043f\u043e\u0432\u0435\u0440\u0445\u043d\u043e\u0441\u0442\u0438 \u0431\u043b\u043e\u043a\u0430.", "\u041e\u0431\u044b\u0447\u043d\u0430\u044f", "\u041e\u0431\u044b\u0447\u043d\u0430\u044f", "\u041a\u043e\u043d\u0442\u0443\u0440", "\u041a\u043e\u0441\u043c\u043e\u0441", "\u0411\u0435\u0437\u0434\u043d\u0430"));

    // 3. Цвет
    private final SeparatorSetting colorSeparator = this.register(new SeparatorSetting("\u0426\u0432\u0435\u0442"));
    private final ModeSetting colorMode = this.register(new ModeSetting("\u0420\u0435\u0436\u0438\u043c \u0446\u0432\u0435\u0442\u0430", "\u0426\u0432\u0435\u0442 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438 \u0440\u0430\u0437\u0440\u0443\u0448\u0435\u043d\u0438\u044f \u0431\u043b\u043e\u043a\u0430.", COLOR_CLIENT, COLOR_CLIENT, COLOR_CUSTOM));
    private final BooleanSetting useSecondColor = this.register(new BooleanSetting("\u0412\u0442\u043e\u0440\u043e\u0439 \u0446\u0432\u0435\u0442", "\u0418\u0441\u043f\u043e\u043b\u044c\u0437\u043e\u0432\u0430\u0442\u044c \u0432\u0442\u043e\u0440\u043e\u0439 \u0446\u0432\u0435\u0442 \u0434\u043b\u044f \u0433\u0440\u0430\u0434\u0438\u0435\u043d\u0442\u0430.", false));
    private final ColorSetting customColor = this.register(new ColorSetting("\u0426\u0432\u0435\u0442", "\u041e\u0441\u043d\u043e\u0432\u043d\u043e\u0439 \u0446\u0432\u0435\u0442 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438.", new Color(255, 255, 255, 255)).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM)));
    private final ColorSetting customSecondColor = this.register(new ColorSetting("\u0426\u0432\u0435\u0442 2", "\u0412\u0442\u043e\u0440\u043e\u0439 \u0446\u0432\u0435\u0442 \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u0438.", new Color(ColorUtil.lerpColor(-1, DARK_SECOND_COLOR, 0.7f), true)).visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM) && this.useSecondColor.getValue()));

    // 4. Толщина и прозрачность
    private final SeparatorSetting thicknessSeparator = this.register(new SeparatorSetting("\u0422\u043e\u043b\u0449\u0438\u043d\u0430 \u0438 \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c"));
    private final SliderSetting lineWidth = this.register(new SliderSetting("\u0422\u043e\u043b\u0449\u0438\u043d\u0430 \u043b\u0438\u043d\u0438\u0439", "\u0422\u043e\u043b\u0449\u0438\u043d\u0430 \u0440\u0435\u0431\u0435\u0440 \u043a\u043e\u043d\u0442\u0443\u0440\u0430.").range(0.5f, 5.0f).increment(0.1f).setValue(1.5f).visible(this.renderOutline::getValue));
    private final SliderSetting outlineAlpha = this.register(new SliderSetting("\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u043a\u043e\u043d\u0442\u0443\u0440\u0430", "\u041c\u0430\u043a\u0441\u0438\u043c\u0430\u043b\u044c\u043d\u0430\u044f \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u043b\u0438\u043d\u0438\u0439.").range(0.05f, 1.0f).increment(0.05f).setValue(1.0f).visible(this.renderOutline::getValue));
    private final SliderSetting fillAlpha = this.register(new SliderSetting("\u041f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0437\u0430\u043b\u0438\u0432\u043a\u0438", "\u0411\u0430\u0437\u043e\u0432\u0430\u044f \u043f\u0440\u043e\u0437\u0440\u0430\u0447\u043d\u043e\u0441\u0442\u044c \u0433\u0440\u0430\u043d\u0435\u0439.").range(0.02f, 0.8f).increment(0.01f).setValue(0.15f).visible(this.renderFill::getValue));

    private final List<Box> lastShapeBoxes = new ArrayList<>();
    private VoxelShape lastShape;
    private BlockPos targetPos;
    private float damage;
    private float previousDamage;
    private float previousRawProgress;
    private boolean destroying;
    private boolean fading;
    private long fadeStartedAt;
    private float fadeStartAlpha;
    private float lastRenderedAlpha;

    public BlockOverlay() {
        super("Block Overlay", "\u0420\u0438\u0441\u0443\u0435\u0442 \u043a\u0430\u0441\u0442\u043e\u043c\u043d\u0443\u044e \u0430\u043d\u0438\u043c\u0430\u0446\u0438\u044e \u0440\u0430\u0437\u0440\u0443\u0448\u0435\u043d\u0438\u044f \u0431\u043b\u043e\u043a\u0430.", Category.VISUALS);
        this.useSecondColor.visibleWhen(() -> this.colorMode.is(COLOR_CUSTOM));
    }

    private void reset() {
        lastShapeBoxes.clear();
        lastShape = null;
        targetPos = null;
        damage = previousDamage = previousRawProgress = lastRenderedAlpha = fadeStartAlpha = 0;
        destroying = fading = false;
        fadeStartedAt = 0;
    }

    @Override
    protected void onDisable() {
        reset();
    }

    @EventHandler
    private void onTick(TickEvent event) {
        if (!event.isPre()) return;
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            reset();
            return;
        }
        previousDamage = damage;
        var interaction = (MultiPlayerGameModeAccessor) mc.interactionManager;
        BlockPos pos = interaction.nv_getDestroyBlockPos();
        float raw = interaction.nv_getDestroyProgress();
        boolean active = interaction.nv_isDestroying() || (raw > 0 && raw != previousRawProgress);
        previousRawProgress = raw;
        if (!active || pos == null) {
            if (destroying && (!lastShapeBoxes.isEmpty() || lastShape != null)) {
                startFade();
            } else if (!fading || System.currentTimeMillis() - fadeStartedAt >= FADE_DURATION_MS) {
                reset();
            }
            return;
        }
        boolean changed = !pos.equals(targetPos);
        if (changed) {
            lastShapeBoxes.clear();
            lastShape = null;
        }
        targetPos = pos.toImmutable();
        damage = MathHelper.clamp(raw, 0, 1);
        if (!destroying || changed) {
            previousDamage = damage;
        }
        destroying = true;
        fading = false;
    }

    @EventHandler
    private void onWorldRender(WorldRenderEvent event) {
        if ((!destroying && !fading) || targetPos == null || mc.world == null) return;
        float progress = MathHelper.clamp(MathHelper.lerp(event.getPartialTicks(), previousDamage, damage), 0, 1);
        if (destroying) {
            var state = mc.world.getBlockState(targetPos);
            VoxelShape shape = shapeMode.is("\u041f\u043e\u043b\u043d\u044b\u0439 \u043a\u0443\u0431") ? VoxelShapes.fullCube() : state.getOutlineShape(mc.world, targetPos);
            if (shape.isEmpty()) {
                if (lastShapeBoxes.isEmpty() && lastShape == null) return;
                startFade();
            } else {
                lastShape = shape;
                lastShapeBoxes.clear();
                for (Box part : shape.getBoundingBoxes()) {
                    lastShapeBoxes.add(part.offset(targetPos).expand(EPSILON));
                }
            }
        }
        float alpha;
        if (fading) {
            float elapsed = MathHelper.clamp((System.currentTimeMillis() - fadeStartedAt) / (float) FADE_DURATION_MS, 0, 1);
            alpha = fadeStartAlpha * (1.0f - smoothstep(0, 1, elapsed));
        } else {
            alpha = 0.25f + progress * 0.75f;
            lastRenderedAlpha = alpha;
        }
        if (alpha < 0.001f) return;

        Vec3d camera = event.getCamera() != null ? event.getCamera().getCameraPos() : mc.gameRenderer.getCamera().getCameraPos();
        var consumers = mc.getBufferBuilders().getEntityVertexConsumers();

        // 1. Заливка (если включена)
        if (renderFill.getValue()) {
            float computedFillAlpha = fillAlpha.getValue() * alpha;
            int fillColor = getColor(0, computedFillAlpha);
            boolean isSpecialSurface = !surfaceStyle.is("\u041e\u0431\u044b\u0447\u043d\u0430\u044f") && !surfaceStyle.is("\u041a\u043e\u043d\u0442\u0443\u0440");
            for (Box part : lastShapeBoxes) {
                if (isSpecialSurface && !fading) {
                    BlockOverlayRenderer.renderPortal(consumers, event.getStack(), camera, part, surfaceStyle.is("\u0411\u0435\u0437\u0434\u043d\u0430"));
                } else {
                    BlockOverlayRenderer.renderFill(consumers, event.getStack(), camera, part, fillColor);
                }
            }
        }

        // 2. Контур (если включен)
        if (renderOutline.getValue() && lastShape != null) {
            float computedOutlineAlpha = outlineAlpha.getValue() * alpha;
            int outlineColor = getColor(0, computedOutlineAlpha);
            BlockOverlayRenderer.renderOutline(consumers, event.getStack(), camera, targetPos, lastShape, outlineColor, lineWidth.getValue());
        }
    }

    private void startFade() {
        if (fading) return;
        fadeStartAlpha = lastRenderedAlpha;
        fadeStartedAt = System.currentTimeMillis();
        fading = true;
        destroying = false;
    }

    private static int fade(int n, int n2, int n3, int n4) {
        int n5 = (int)((System.currentTimeMillis() / (long)Math.max(1, n) + (long)n2) % 360L);
        n5 = n5 >= 180 ? 360 - n5 : n5;
        return ColorUtil.lerpColor(n3, n4, (float)n5 / 180.0f);
    }

    private int getColor(int n, float f) {
        int n2;
        int n3;
        if (this.colorMode.is(COLOR_CLIENT)) {
            int[] nArray = ClientPalette.colors();
            if (nArray != null && nArray.length >= 2) {
                return ColorUtil.multAlpha(BlockOverlay.paletteFade(8, n, nArray), f);
            }
            InterfaceModule interfaceModule = InterfaceModule.getInstance();
            if (interfaceModule != null) {
                n3 = interfaceModule.clientPrimaryColorOpaque();
                n2 = interfaceModule.usesSecondClientColor() ? interfaceModule.clientSecondaryColorOpaque() : n3;
            } else {
                n3 = -1;
                n2 = ColorUtil.lerpColor(n3, DARK_SECOND_COLOR, 0.7f);
            }
        } else {
            n3 = this.customColor.getColor();
            n2 = this.useSecondColor.getValue() ? this.customSecondColor.getColor() : n3;
        }
        if (n3 == n2) {
            return ColorUtil.multAlpha(n3, f);
        }
        return ColorUtil.multAlpha(BlockOverlay.fade(8, n, n3, n2), f);
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

    private static float smoothstep(float f, float f2, float f3) {
        float f4 = MathHelper.clamp((float)((f3 - f) / (f2 - f)), (float)0.0f, (float)1.0f);
        return f4 * f4 * (3.0f - 2.0f * f4);
    }
}
