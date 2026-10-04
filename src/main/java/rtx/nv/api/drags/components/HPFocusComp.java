package rtx.nv.api.drags.components;

import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.drags.Draggable;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.HPFocus;
import rtx.nv.api.ui.settings.Setting;
import rtx.nv.api.ui.settings.SettingsFactory;
import rtx.nv.utils.animations.Easings;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.network.Network;
import rtx.nv.utils.render.fonts.Fonts;
import rtx.nv.utils.render.render2d.Render2D;

public final class HPFocusComp extends Draggable {
    private static final Identifier CONTAINER = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier CONTAINER_BLINKING = Identifier.ofVanilla("hud/heart/container_blinking");
    private static final Identifier CONTAINER_HARDCORE = Identifier.ofVanilla("hud/heart/container_hardcore");
    private static final Identifier CONTAINER_HARDCORE_BLINKING = Identifier.ofVanilla("hud/heart/container_hardcore_blinking");

    private static final Identifier FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier FULL_BLINKING = Identifier.ofVanilla("hud/heart/full_blinking");
    private static final Identifier HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HALF_BLINKING = Identifier.ofVanilla("hud/heart/half_blinking");

    private static final Identifier HARDCORE_FULL = Identifier.ofVanilla("hud/heart/hardcore_full");
    private static final Identifier HARDCORE_FULL_BLINKING = Identifier.ofVanilla("hud/heart/hardcore_full_blinking");
    private static final Identifier HARDCORE_HALF = Identifier.ofVanilla("hud/heart/hardcore_half");
    private static final Identifier HARDCORE_HALF_BLINKING = Identifier.ofVanilla("hud/heart/hardcore_half_blinking");

    private static final Identifier ABSORBING_FULL = Identifier.ofVanilla("hud/heart/absorbing_full");
    private static final Identifier ABSORBING_FULL_BLINKING = Identifier.ofVanilla("hud/heart/absorbing_full_blinking");
    private static final Identifier ABSORBING_HALF = Identifier.ofVanilla("hud/heart/absorbing_half");
    private static final Identifier ABSORBING_HALF_BLINKING = Identifier.ofVanilla("hud/heart/absorbing_half_blinking");

    private static final Identifier POISONED_FULL = Identifier.ofVanilla("hud/heart/poisoned_full");
    private static final Identifier POISONED_FULL_BLINKING = Identifier.ofVanilla("hud/heart/poisoned_full_blinking");
    private static final Identifier POISONED_HALF = Identifier.ofVanilla("hud/heart/poisoned_half");
    private static final Identifier POISONED_HALF_BLINKING = Identifier.ofVanilla("hud/heart/poisoned_half_blinking");

    private static final Identifier WITHERED_FULL = Identifier.ofVanilla("hud/heart/withered_full");
    private static final Identifier WITHERED_FULL_BLINKING = Identifier.ofVanilla("hud/heart/withered_full_blinking");
    private static final Identifier WITHERED_HALF = Identifier.ofVanilla("hud/heart/withered_half");
    private static final Identifier WITHERED_HALF_BLINKING = Identifier.ofVanilla("hud/heart/withered_half_blinking");

    private static final Identifier FROZEN_FULL = Identifier.ofVanilla("hud/heart/frozen_full");
    private static final Identifier FROZEN_FULL_BLINKING = Identifier.ofVanilla("hud/heart/frozen_full_blinking");
    private static final Identifier FROZEN_HALF = Identifier.ofVanilla("hud/heart/frozen_half");
    private static final Identifier FROZEN_HALF_BLINKING = Identifier.ofVanilla("hud/heart/frozen_half_blinking");

    private final SmoothAnimation visibility = new SmoothAnimation();

    public HPFocusComp() {
        super("hpfocus", (Position.screenWidth() - 81.0f * 0.85f) * 0.5f, Position.screenHeight() * 0.65f);
        this.visibility.set(0.0);
    }

    private static HPFocus module() {
        return ModuleManager.get().get(HPFocus.class);
    }

    @Override
    public String displayName() {
        return "HP Focus";
    }

    @Override
    public void resetToDefault() {
        float defaultX = (Position.screenWidth() - this.width()) * 0.5f;
        float defaultY = Position.screenHeight() * 0.65f;
        this.getDrag().setTargetX(defaultX);
        this.getDrag().setTargetY(defaultY);
        this.getDrag().syncToTarget();
    }

    @Override
    public float width() {
        return HPFocusComp.calculateMetrics(HPFocusComp.module())[0];
    }

    @Override
    public float height() {
        return HPFocusComp.calculateMetrics(HPFocusComp.module())[1];
    }

    @Override
    public boolean isInteractive() {
        HPFocus hPFocus = HPFocusComp.module();
        return hPFocus != null && hPFocus.isEnabled();
    }

    @Override
    protected List<Setting> buildHudSettings() {
        List<Setting> list = super.buildHudSettings();
        HPFocus hPFocus = HPFocusComp.module();
        if (hPFocus != null) {
            for (rtx.nv.api.modules.settings.Setting s : hPFocus.getSettings().all()) {
                Setting ui = SettingsFactory.create(s);
                if (ui != null) {
                    list.add(ui);
                }
            }
        }
        return list;
    }

    private static float[] calculateMetrics(HPFocus module) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float scale = module != null ? module.scale() : 0.85f;
        boolean showAbs = module == null || module.showAbsorption.getValue();

        float maxHp = 20.0f;
        float absorption = 0.0f;
        if (mc.player != null) {
            maxHp = mc.player.getMaxHealth();
            absorption = showAbs ? mc.player.getAbsorptionAmount() : 0.0f;
        }

        int normalContainers = Math.max(1, (int)Math.ceil(maxHp / 2.0f));
        int absContainers = showAbs ? (int)Math.ceil(absorption / 2.0f) : 0;
        int totalContainers = normalContainers + absContainers;

        int cols = Math.min(10, totalContainers);
        int rows = Math.max(1, (int)Math.ceil((float)totalContainers / 10.0f));

        float unscaledW = cols > 0 ? (cols - 1) * 8 + 9 : 9;
        float unscaledH = rows > 0 ? (rows - 1) * 10 + 9 : 9;

        return new float[]{unscaledW * scale, unscaledH * scale, (float)normalContainers, (float)absContainers};
    }

    @Override
    protected void render(DrawContext drawContext) {
        HPFocus hPFocus = HPFocusComp.module();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean enabled = hPFocus != null && hPFocus.isEnabled();
        boolean dragMode = DragSystem.get().isDragModeActive();
        boolean hudHidden = mc.options != null && mc.options.hudHidden;

        float hp = mc.player != null ? mc.player.getHealth() : 14.0f;
        boolean shouldShow;
        if (!enabled || hudHidden) {
            shouldShow = false;
        } else if (dragMode) {
            shouldShow = true;
        } else if (mc.player == null) {
            shouldShow = false;
        } else if (hPFocus.isAlwaysVisible()) {
            shouldShow = true;
        } else {
            shouldShow = hp <= hPFocus.hpThresholdHp();
        }

        this.visibility.run(shouldShow ? 1.0 : 0.0, shouldShow ? 0.22 : 0.15, Easings.CUBIC_OUT, true);
        this.visibility.update();
        float vis = this.visibility.get();
        if (vis <= 0.01f) {
            return;
        }

        float scale = hPFocus != null ? hPFocus.scale() : 0.85f;
        int hurtTime = mc.player != null ? mc.player.hurtTime : 0;

        float shakeX = 0.0f;
        float shakeY = 0.0f;
        if (hPFocus != null && hPFocus.damageShake.getValue() && hurtTime > 0) {
            float intensity = (float)hurtTime / 10.0f;
            shakeX = (float)Math.sin(System.currentTimeMillis() * 0.08) * 2.5f * intensity;
            shakeY = (float)Math.cos(System.currentTimeMillis() * 0.10) * 2.0f * intensity;
        }

        float originX = this.getX() + shakeX;
        float originY = this.getY() + shakeY;

        // Visual drag helper in HUD edit mode
        if (dragMode) {
            Render2D.beginFrame(drawContext);
            Render2D.outline(originX - 2.0f, originY - 2.0f, this.width() + 4.0f, this.height() + 4.0f, 3.0f, 1.0f, ColorUtil.multAlpha(0xFF8AA4FF, 0.5f));
            Render2D.flush();
        }

        int maxHp = mc.player != null ? MathHelper.ceil(mc.player.getMaxHealth()) : 20;
        int health = mc.player != null ? MathHelper.ceil(mc.player.getHealth()) : 14;
        boolean showAbs = hPFocus == null || hPFocus.showAbsorption.getValue();
        int absorption = (mc.player != null && showAbs) ? MathHelper.ceil(mc.player.getAbsorptionAmount()) : 0;

        int normalContainers = Math.max(1, (int)Math.ceil((float)maxHp / 2.0f));
        int absContainers = showAbs ? (int)Math.ceil((float)absorption / 2.0f) : 0;

        boolean blinking = hurtTime > 0;
        boolean hardcore = mc.world != null && mc.world.getLevelProperties().isHardcore();
        boolean poison = mc.player != null && mc.player.hasStatusEffect(StatusEffects.POISON);
        boolean wither = mc.player != null && mc.player.hasStatusEffect(StatusEffects.WITHER);
        boolean frozen = mc.player != null && mc.player.isFrozen();

        int renderX = Math.round(originX);
        int renderY = Math.round(originY);

        boolean isVector = hPFocus != null && (hPFocus.heartStyle.is("Векторные Client") || hPFocus.heartStyle.is("Векторные NV"));
        boolean isExactOne = Math.abs(scale - 1.0f) < 0.02f && vis >= 0.999f;
        float animScale = isExactOne ? 1.0f : (0.94f + vis * 0.06f) * scale;

        drawContext.getMatrices().pushMatrix();
        drawContext.getMatrices().translate(renderX, renderY);
        if (!isExactOne) {
            drawContext.getMatrices().scale(animScale, animScale);
        }

        if (isVector) {
            Render2D.beginFrame(drawContext);
            float heartSize = 9.0f;
            for (int i = 0; i < normalContainers; ++i) {
                int row = i / 10;
                int col = i % 10;
                float hx = col * 8.0f;
                float hy = row * 10.0f;

                int contColor = blinking ? 0xFFFFFFFF : 0xC0000000;
                Fonts.HEART.msdf("A", hx - 0.5f, hy - 0.5f, heartSize + 1.0f, contColor);

                int value = i * 2 + 1;
                if (value <= health) {
                    boolean half = (value + 1 > health);
                    int heartColor = getVectorHeartColor(hardcore, blinking, poison, wither, frozen);
                    if (half) {
                        drawContext.enableScissor(
                            Math.round(renderX + hx * animScale),
                            Math.round(renderY + hy * animScale),
                            Math.round(renderX + (hx + heartSize * 0.5f) * animScale),
                            Math.round(renderY + (hy + heartSize) * animScale)
                        );
                        Fonts.HEART.msdf("A", hx, hy, heartSize, heartColor);
                        drawContext.disableScissor();
                    } else {
                        Fonts.HEART.msdf("A", hx, hy, heartSize, heartColor);
                    }
                }
            }

            if (absContainers > 0) {
                for (int j = 0; j < absContainers; ++j) {
                    int idx = normalContainers + j;
                    int row = idx / 10;
                    int col = idx % 10;
                    float ax = col * 8.0f;
                    float ay = row * 10.0f;

                    int contColor = blinking ? 0xFFFFFFFF : 0xC0000000;
                    Fonts.HEART.msdf("A", ax - 0.5f, ay - 0.5f, heartSize + 1.0f, contColor);

                    int absValue = j * 2 + 1;
                    if (absValue <= absorption) {
                        boolean half = (absValue + 1 > absorption);
                        int goldColor = blinking ? 0xFFFFFFFF : 0xFFFFD700;
                        if (half) {
                            drawContext.enableScissor(
                                Math.round(renderX + ax * animScale),
                                Math.round(renderY + ay * animScale),
                                Math.round(renderX + (ax + heartSize * 0.5f) * animScale),
                                Math.round(renderY + (ay + heartSize) * animScale)
                            );
                            Fonts.HEART.msdf("A", ax, ay, heartSize, goldColor);
                            drawContext.disableScissor();
                        } else {
                            Fonts.HEART.msdf("A", ax, ay, heartSize, goldColor);
                        }
                    }
                }
            }
            Render2D.flush();
        } else {
            // 1. Draw Normal Health Containers & Hearts (1-to-1 Vanilla InGameHud logic)
            for (int i = 0; i < normalContainers; ++i) {
                int row = i / 10;
                int col = i % 10;
                int hx = col * 8;
                int hy = row * 10;

                // Container
                Identifier containerSprite = getContainerSprite(hardcore, blinking);
                drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, containerSprite, hx, hy, 9, 9);

                // Heart
                int value = i * 2 + 1;
                if (value + 1 <= health) {
                    Identifier heartSprite = getHeartSprite(hardcore, false, blinking, poison, wither, frozen);
                    drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, heartSprite, hx, hy, 9, 9);
                } else if (value <= health) {
                    Identifier heartSprite = getHeartSprite(hardcore, true, blinking, poison, wither, frozen);
                    drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, heartSprite, hx, hy, 9, 9);
                }
            }

            // 2. Draw Absorption Containers & Hearts (1-to-1 Vanilla InGameHud logic)
            if (absContainers > 0) {
                for (int j = 0; j < absContainers; ++j) {
                    int idx = normalContainers + j;
                    int row = idx / 10;
                    int col = idx % 10;
                    int ax = col * 8;
                    int ay = row * 10;

                    // Container
                    Identifier containerSprite = getContainerSprite(false, blinking);
                    drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, containerSprite, ax, ay, 9, 9);

                    // Absorbing Heart
                    int absValue = j * 2 + 1;
                    if (absValue + 1 <= absorption) {
                        Identifier absSprite = blinking ? ABSORBING_FULL_BLINKING : ABSORBING_FULL;
                        drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, absSprite, ax, ay, 9, 9);
                    } else if (absValue <= absorption) {
                        Identifier absSprite = blinking ? ABSORBING_HALF_BLINKING : ABSORBING_HALF;
                        drawContext.drawGuiTexture(RenderPipelines.GUI_TEXTURED, absSprite, ax, ay, 9, 9);
                    }
                }
            }
        }

        drawContext.getMatrices().popMatrix();
    }

    private static int getVectorHeartColor(boolean hardcore, boolean blinking, boolean poison, boolean wither, boolean frozen) {
        if (blinking) return 0xFFFFFFFF;
        if (poison) return 0xFF44E044;
        if (wither) return 0xFF353535;
        if (frozen) return 0xFF50D0FF;
        if (hardcore) return 0xFFD81030;
        return 0xFFFF2548;
    }

    private static Identifier getContainerSprite(boolean hardcore, boolean blinking) {
        if (hardcore) {
            return blinking ? CONTAINER_HARDCORE_BLINKING : CONTAINER_HARDCORE;
        }
        return blinking ? CONTAINER_BLINKING : CONTAINER;
    }

    private static Identifier getHeartSprite(boolean hardcore, boolean half, boolean blinking, boolean poison, boolean wither, boolean frozen) {
        if (poison) {
            if (half) return blinking ? POISONED_HALF_BLINKING : POISONED_HALF;
            return blinking ? POISONED_FULL_BLINKING : POISONED_FULL;
        }
        if (wither) {
            if (half) return blinking ? WITHERED_HALF_BLINKING : WITHERED_HALF;
            return blinking ? WITHERED_FULL_BLINKING : WITHERED_FULL;
        }
        if (frozen) {
            if (half) return blinking ? FROZEN_HALF_BLINKING : FROZEN_HALF;
            return blinking ? FROZEN_FULL_BLINKING : FROZEN_FULL;
        }
        if (hardcore) {
            if (half) return blinking ? HARDCORE_HALF_BLINKING : HARDCORE_HALF;
            return blinking ? HARDCORE_FULL_BLINKING : HARDCORE_FULL;
        }
        if (half) return blinking ? HALF_BLINKING : HALF;
        return blinking ? FULL_BLINKING : FULL;
    }
}
