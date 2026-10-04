package rtx.nv.utils.render.others;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.api.ui.theme.ThemeManager;
import rtx.nv.api.ui.theme.ThemeProfile;
import rtx.nv.utils.animations.Easing;
import rtx.nv.utils.animations.SmoothAnimation;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.render.render2d.radialglass.BuiltRadialGlass;
import rtx.nv.utils.render.render2d.shape.BuiltShape;

public final class RectUtil {
    private static final float DEFAULT_RADIUS = rtx.nv.api.ui.UiTokens.PANEL_RADIUS;
    private static final float ROW_JOIN_OVERLAP = 0.75f;
    private static final float DEFAULT_BLUR_RADIUS = 18.0f;
    private static final float DEFAULT_BACKDROP_OPACITY = 1.0f;
    private static final float DEFAULT_GLASS_OPACITY = 1.0f;
    private static final float DEFAULT_FILL_OPACITY = 1.0f;
    private static final float DEFAULT_EDGE_STRENGTH = 0.18f;
    private static final float DEFAULT_EDGE_SHARPNESS = 55.0f;
    private static final float DEFAULT_REFRACTION = 0.3f;
    private static final float DEFAULT_LIQUID_CURVE = 0.5f;
    private static final int DEFAULT_COLOR = -857872385;
    private static final long COLOR_MOVEMENT_PERIOD_MS = 2400L;
    private static final long MAX_COLOR_FRAME_DELTA_MS = 100L;
    private static final double COLOR_MOVEMENT_ACCEL_SECONDS = 0.45;
    private static final double COLOR_MOVEMENT_DECEL_SECONDS = 0.7;
    private static final double SECOND_COLOR_FADE_IN_SECONDS = 0.3;
    private static final double SECOND_COLOR_FADE_OUT_SECONDS = 0.42;
    private static final double GLOW_FADE_IN_SECONDS = 0.5;
    private static final double GLOW_FADE_OUT_SECONDS = 0.5;
    private static final Easing COLOR_EASING = d -> d * d * (3.0 - 2.0 * d);
    private static final SmoothAnimation colorMovementSpeed = new SmoothAnimation();
    private static final SmoothAnimation secondColorBlend = new SmoothAnimation();
    private static final SmoothAnimation glowBlend = new SmoothAnimation();
    private static boolean colorMovementTarget;
    private static boolean secondColorTarget;
    private static boolean glowTarget;
    private static float colorOffset;
    private static long lastColorMovementMs;
    private static int lastSecondColor;
    private static boolean splitOverrideArmed;
    private static float splitOverrideX;
    private static float splitOverrideY;
    private static float splitOverrideW;
    private static float splitOverrideH;
    private static float splitOverrideRadius;
    private static int splitOverrideIndex;
    private static float splitOverrideChildX;
    private static float splitOverrideChildY;
    private static float splitOverrideChildW;
    private static float splitOverrideChildH;
    private static float splitOverrideChildRadius;
    private static int hudRenderingDepth = 0;

    public static void setHudRendering(boolean rendering) {
        if (rendering) {
            hudRenderingDepth++;
        } else if (hudRenderingDepth > 0) {
            hudRenderingDepth--;
        }
    }

    public static boolean isHudRendering() {
        if (hudRenderingDepth > 0) {
            return true;
        }
        net.minecraft.client.MinecraftClient mc = net.minecraft.client.MinecraftClient.getInstance();
        if (mc == null) {
            return false;
        }
        if (mc.currentScreen instanceof rtx.nv.api.ui.UI || rtx.nv.api.ui.UI.isPanelActive() || rtx.nv.api.ui.BaseScreen.hasClosingOverlay()) {
            return false;
        }
        if (mc.currentScreen == null || mc.currentScreen instanceof net.minecraft.client.gui.screen.ChatScreen) {
            return true;
        }
        return false;
    }

    private RectUtil() {
    }

    static {
        lastSecondColor = -857872385;
    }

    private static float clamp(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public static void armSplitOverride(float f, float f2, float f3, float f4, float f5, int n, float f6, float f7, float f8, float f9, float f10) {
        splitOverrideArmed = n > 0;
        splitOverrideX = f;
        splitOverrideY = f2;
        splitOverrideW = f3;
        splitOverrideH = f4;
        splitOverrideRadius = f5;
        splitOverrideIndex = n;
        splitOverrideChildX = f6;
        splitOverrideChildY = f7;
        splitOverrideChildW = f8;
        splitOverrideChildH = f9;
        splitOverrideChildRadius = f10;
    }

    public static void clearSplitOverride() {
        splitOverrideArmed = false;
        splitOverrideIndex = 0;
    }

    public static void drawClientShape(float f, float f2, float[] fArray, float[] fArray2, int n, float f3, float f4, float f5, float f6, boolean bl, boolean bl2) {
        float f7;
        float f8;
        float f9;
        float f10;
        float f11;
        float f12;
        boolean bl3;
        if (n <= 0 || f6 <= 0.0f) {
            return;
        }
        boolean bl4 = false;
        float f13 = 0.0f;
        float f14 = 0.0f;
        float f15 = 0.0f;
        float f16 = 0.0f;
        int n2 = 0;
        float f17 = 0.0f;
        float f18 = 0.0f;
        float f19 = 0.0f;
        float f20 = 0.0f;
        float f21 = 0.0f;
        if (splitOverrideArmed) {
            splitOverrideArmed = false;
            bl4 = true;
            f13 = splitOverrideX;
            f14 = splitOverrideY;
            f15 = splitOverrideW;
            f16 = splitOverrideH;
            n2 = splitOverrideIndex;
            f17 = splitOverrideChildX;
            f18 = splitOverrideChildY;
            f19 = splitOverrideChildW;
            f20 = splitOverrideChildH;
            f21 = splitOverrideChildRadius;
        }
        float f22 = 0.0f;
        float f23 = 0.0f;
        for (int i = 0; i < n; ++i) {
            f22 = Math.max(f22, fArray[i]);
            f23 += fArray2[i];
        }
        float f24 = f23 + f3 + f4;
        if (f22 <= 0.0f || f24 <= 0.0f) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f25 = RectUtil.clamp(f6);
        float f26 = interfaceModule == null ? f5 : interfaceModule.rectCornerRadius.getFloat();
        float f27 = Math.max(0.0f, Math.min(f26, Math.min(f22, f24) * 0.5f));
        float f28 = ThemeManager.blurRadius(interfaceModule == null ? 18.0f : interfaceModule.rectBackdropBlur.getFloat());
        float f29 = 1.0f;
        float f30 = 1.0f;
        float f31 = 1.0f;
        int n3 = interfaceModule == null ? -857872385 : interfaceModule.clientPrimaryColor();
        boolean bl5 = bl3 = interfaceModule != null && interfaceModule.usesSecondClientColor();
        if (bl3) {
            lastSecondColor = interfaceModule.clientSecondaryColor();
        }
        float f32 = RectUtil.updateSecondColorBlend(bl3);
        int n4 = ColorUtil.lerpColor(n3, lastSecondColor, f32);
        float f33 = RectUtil.updateColorOffset(interfaceModule != null && interfaceModule.clientColorMovement());
        float f34 = interfaceModule == null ? 0.18f : interfaceModule.rectEdgeStrength.getFloat();
        float f35 = interfaceModule == null ? 55.0f : interfaceModule.rectEdgeSharpness.getFloat();
        float f36 = interfaceModule == null ? 0.3f : interfaceModule.rectRefractionStrength.getFloat();
        float f37 = 0.5f;
        float[] fArray3 = new float[n];
        float[] fArray4 = new float[n];
        float f38 = f3;
        for (int i = 0; i < n; ++i) {
            fArray3[i] = i == 0 ? 0.0f : f38;
            fArray4[i] = i == n - 1 ? f24 : (f38 += fArray2[i]);
        }
        float[] fArray5 = new float[n];
        float[] fArray6 = new float[n];
        float[] fArray7 = new float[n];
        float[] fArray8 = new float[n];
        int n5 = 0;
        int n6 = 0;
        while (n6 < n) {
            int n7 = n6;
            while (n7 + 1 < n && Math.abs(fArray[n7 + 1] - fArray[n6]) < 0.01f) {
                ++n7;
            }
            f12 = bl ? 0.0f : f22 - fArray[n6];
            f11 = bl ? fArray[n6] : f22;
            fArray5[n5] = f12;
            fArray6[n5] = f11;
            fArray7[n5] = fArray3[n6];
            fArray8[n5] = fArray4[n7];
            ++n5;
            n6 = n7 + 1;
        }
        float f39 = Math.min(f27, f23 / (float)Math.max(n5, 1) * 0.5f);
        f12 = fArray7[0] + f39;
        f11 = fArray8[n5 - 1] - f39;
        float[] fArray9 = new float[n5 * 4];
        for (int i = 0; i < n5; ++i) {
            f10 = fArray7[i];
            f9 = fArray8[i];
            f8 = fArray6[i] - fArray5[i];
            if (i > 0) {
                f7 = fArray6[i - 1] - fArray5[i - 1];
                f10 -= f7 >= f8 - 0.01f ? Math.max(fArray8[i - 1] - fArray7[i - 1], 0.75f) : 0.75f;
                f10 = Math.max(f10, Math.min(f12, fArray8[i - 1]));
            }
            if (i < n5 - 1) {
                f7 = fArray6[i + 1] - fArray5[i + 1];
                f9 += f7 >= f8 - 0.01f ? Math.max(fArray8[i + 1] - fArray7[i + 1], 0.75f) : 0.75f;
                f9 = Math.min(f9, Math.max(f11, fArray7[i + 1]));
            }
            fArray9[i * 4] = fArray5[i];
            fArray9[i * 4 + 1] = fArray6[i];
            fArray9[i * 4 + 2] = f10;
            fArray9[i * 4 + 3] = f9;
        }
        n = n5;
        RectUtil.drawGlowWithSpans(interfaceModule, f, f2, f22, f24, f39, f25, n3, n4, f33, fArray9, n, bl, bl2);
        if (bl4 && f19 > 1.0f && f20 > 1.0f) {
            RectUtil.drawGlow(interfaceModule, f17, f18, f19, f20, f21, f25, n3, n4, f33);
        }
        float f40 = f;
        f10 = f2;
        f9 = f22;
        f8 = f24;
        if (bl4) {
            f40 = f13;
            f10 = f14;
            f9 = f15;
            f8 = f16;
            f7 = f - f13;
            float f41 = f2 - f14;
            float[] fArray10 = new float[fArray9.length];
            for (int i = 0; i < n; ++i) {
                fArray10[i * 4] = fArray9[i * 4] + f7;
                fArray10[i * 4 + 1] = fArray9[i * 4 + 1] + f7;
                fArray10[i * 4 + 2] = fArray9[i * 4 + 2] + f41;
                fArray10[i * 4 + 3] = fArray9[i * 4 + 3] + f41;
            }
            fArray9 = fArray10;
        }
        BuiltShape builtShape = new BuiltShape(f40, f10, f9, f8, f27, n3, f25 * f30, f35, n3, f31 * f29, true, f34, f36, f37, 0.0f).withSpans(fArray9, n, f39).withBlurRadius(f28).withSecondColor(n4, f33).withAlignment(bl, bl2).withSplitIndex(bl4 ? n2 : 0);
        Render2D.shape(builtShape);
    }

    public static void drawClientRectNoGlow(float f, float f2, float f3, float f4, float f5, float f6) {
        RectUtil.drawClientRectImpl(f, f2, f3, f4, f5, f6, 0.0f, false, false);
    }

    public static void drawClientRect(DrawContext drawContext, float f, float f2, float f3, float f4) {
        RectUtil.drawClientRect(drawContext, f, f2, f3, f4, 7.0f, 1.0f);
    }

    public static void drawClientRect(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        RectUtil.drawClientRectImpl(f, f2, f3, f4, f5, f6, f7, true, false);
    }

    public static void drawClientRect(DrawContext drawContext, float f, float f2, float f3, float f4, float f5) {
        RectUtil.drawClientRect(drawContext, f, f2, f3, f4, 7.0f, f5);
    }

    public static void drawClientRect(float f, float f2, float f3, float f4) {
        RectUtil.drawClientRect(f, f2, f3, f4, 7.0f, 1.0f);
    }

    public static void drawClientRect(float f, float f2, float f3, float f4, float f5) {
        RectUtil.drawClientRect(f, f2, f3, f4, 7.0f, f5);
    }

    public static void drawClientRect(float f, float f2, float f3, float f4, float f5, float f6) {
        RectUtil.drawClientRect(f, f2, f3, f4, f5, f6, 0.0f);
    }

    public static void drawClientRect(DrawContext drawContext, float f, float f2, float f3, float f4, float f5, float f6) {
        Render2D.beginFrame(drawContext);
        RectUtil.drawClientRect(f, f2, f3, f4, f5, f6);
        Render2D.flush();
    }

    public static void drawClientGlowSpans(float f, float f2, float f3, float f4, float f5, float[] fArray, int n) {
        if (n <= 0 || f5 <= 0.0f) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule == null) {
            return;
        }
        float f6 = RectUtil.updateGlowBlend(interfaceModule.rectGlow.getValue());
        if (f6 <= 0.001f) {
            return;
        }
        float f7 = interfaceModule.rectGlowIntensity.getFloat();
        float f8 = interfaceModule.rectGlowRadius.getFloat();
        if (f7 <= 0.0f || f8 <= 0.0f) {
            return;
        }
        int n2 = interfaceModule.clientPrimaryColor();
        boolean bl = interfaceModule.usesSecondClientColor();
        if (bl) {
            lastSecondColor = interfaceModule.clientSecondaryColor();
        }
        float f9 = RectUtil.updateSecondColorBlend(bl);
        int n3 = ColorUtil.lerpColor(n2, lastSecondColor, f9);
        float f10 = RectUtil.updateColorOffset(interfaceModule.clientColorMovement());
        float f11 = interfaceModule.rectCornerRadius.getFloat();
        float[] fArray2 = new float[]{f11, f11, f11, f11};
        BuiltGlow builtGlow = new BuiltGlow(f, f2, f3, f4, fArray2, n2, f7, f8, RectUtil.clamp(f5) * f6).withSecondColor(n3, f10).withSpans(fArray, n).withBoxesMode();
        Render2D.glow(builtGlow);
    }

    public static void drawClientRectFixedRadius(float f, float f2, float f3, float f4, float f5, float f6) {
        RectUtil.drawClientRectImpl(f, f2, f3, f4, f5, f6, 0.0f, true, true);
    }

    public static void drawClientRectFixedRadiusNoGlow(float f, float f2, float f3, float f4, float f5, float f6) {
        RectUtil.drawClientRectImpl(f, f2, f3, f4, f5, f6, 0.0f, false, true);
    }

    public static void drawClientRectFixedRadius(float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        RectUtil.drawClientRectImpl(f, f2, f3, f4, f5, f6, f7, true, true);
    }

    public static void drawClientSector(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        RectUtil.drawClientSector(f, f2, f3, f4, f5, f6, f7, f8, 0, 0.0f);
    }

    public static void drawClientSector(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, float f9) {
        boolean bl;
        if (f4 <= f3 || f6 <= 0.0f || f8 <= 0.0f) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f10 = RectUtil.clamp(f8);
        float f11 = ThemeManager.blurRadius(interfaceModule == null ? 18.0f : interfaceModule.rectBackdropBlur.getFloat());
        int n2 = interfaceModule == null ? -857872385 : interfaceModule.clientPrimaryColor();
        boolean bl2 = bl = interfaceModule != null && interfaceModule.usesSecondClientColor();
        if (bl) {
            lastSecondColor = interfaceModule.clientSecondaryColor();
        }
        float f12 = RectUtil.updateSecondColorBlend(bl);
        int n3 = ColorUtil.lerpColor(n2, lastSecondColor, f12);
        float f13 = RectUtil.updateColorOffset(interfaceModule != null && interfaceModule.clientColorMovement());
        ThemeProfile profile = ThemeManager.currentProfile();
        float pEdge = profile != null ? profile.edge : 1.0f;
        float pSharp = profile != null ? profile.specular : 1.0f;
        float pRefract = profile != null ? profile.refraction : 1.0f;
        float f14 = (interfaceModule == null ? 0.18f : interfaceModule.rectEdgeStrength.getFloat()) * pEdge;
        float f15 = (interfaceModule == null ? 55.0f : interfaceModule.rectEdgeSharpness.getFloat()) * pSharp;
        float f16 = (interfaceModule == null ? 0.3f : interfaceModule.rectRefractionStrength.getFloat()) * pRefract;
        Render2D.radialGlass(new BuiltRadialGlass(f, f2, f3, f4, (float)Math.toRadians(f5), (float)Math.toRadians(f6) * 0.5f, f7, 1.0f, n2, n3, f13, f10 * 1.0f, f15, n2, 1.0f, true, f14, f16, f11, n, f9, 0.0f));
    }

    private static float updateGlowBlend(boolean bl) {
        if (bl != glowTarget) {
            glowBlend.run(bl ? 1.0 : 0.0, bl ? 0.5 : 0.5, COLOR_EASING, false);
            glowTarget = bl;
        }
        glowBlend.update();
        return RectUtil.clamp(glowBlend.get());
    }

    public static void drawClientGlowOnly(float f, float f2, float f3, float f4, float f5, float f6) {
        if (f3 <= 0.0f || f4 <= 0.0f || f6 <= 0.0f) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        if (interfaceModule == null) {
            return;
        }
        float f7 = RectUtil.clamp(f6);
        int n = interfaceModule.clientPrimaryColor();
        boolean bl = interfaceModule.usesSecondClientColor();
        if (bl) {
            lastSecondColor = interfaceModule.clientSecondaryColor();
        }
        float f8 = RectUtil.updateSecondColorBlend(bl);
        int n2 = ColorUtil.lerpColor(n, lastSecondColor, f8);
        float f9 = RectUtil.updateColorOffset(interfaceModule.clientColorMovement());
        RectUtil.drawGlow(interfaceModule, f, f2, f3, f4, f5, f7, n, n2, f9);
    }

    public static float updateSecondColorBlend(boolean bl) {
        if (bl != secondColorTarget) {
            secondColorBlend.run(bl ? 1.0 : 0.0, bl ? 0.3 : 0.42, COLOR_EASING, false);
            secondColorTarget = bl;
        }
        secondColorBlend.update();
        return RectUtil.clamp(secondColorBlend.get());
    }

    private static void drawGlowWithSpans(InterfaceModule interfaceModule, float f, float f2, float f3, float f4, float f5, float f6, int n, int n2, float f7, float[] fArray, int n3, boolean bl, boolean bl2) {
        if (interfaceModule == null) {
            return;
        }
        float f8 = RectUtil.updateGlowBlend(interfaceModule.rectGlow.getValue());
        if (f8 <= 0.001f) {
            return;
        }
        ThemeProfile profile = ThemeManager.currentProfile();
        float pGlow = profile != null ? profile.glow : 1.0f;
        float f9 = interfaceModule.rectGlowIntensity.getFloat() * pGlow;
        float f10 = interfaceModule.rectGlowRadius.getFloat();
        if (f9 <= 0.0f || f10 <= 0.0f) {
            return;
        }
        int n4 = n;
        int n5 = n2;
        float[] fArray2 = new float[]{f5, f5, f5, f5};
        BuiltGlow builtGlow = new BuiltGlow(f, f2, f3, f4, fArray2, n4, f9, f10, f6 * f8).withSecondColor(n5, f7);
        if (fArray != null && n3 > 0) {
            builtGlow = builtGlow.withSpans(fArray, n3).withAlignment(bl, bl2);
        }
        Render2D.glow(builtGlow);
    }

    public static float updateColorOffset(boolean bl) {
        long l = System.currentTimeMillis();
        if (bl != colorMovementTarget) {
            colorMovementSpeed.run(bl ? 1.0 : 0.0, bl ? 0.45 : 0.7, COLOR_EASING, false);
            colorMovementTarget = bl;
        }
        colorMovementSpeed.update();
        if (lastColorMovementMs == 0L) {
            lastColorMovementMs = l;
            return RectUtil.normalizeOffset(colorOffset + ThemeManager.currentProfile().gradientAngle / 360.0f);
        }
        long l2 = Math.min(Math.max(0L, l - lastColorMovementMs), 100L);
        lastColorMovementMs = l;
        ThemeProfile profile = ThemeManager.currentProfile();
        float pMotion = profile != null ? profile.motion : 1.0f;
        float f = RectUtil.clamp(colorMovementSpeed.get()) * pMotion;
        colorOffset = RectUtil.normalizeOffset(colorOffset + (float)l2 * f / 2400.0f);
        return RectUtil.normalizeOffset(colorOffset + ThemeManager.currentProfile().gradientAngle / 360.0f);
    }

    private static float normalizeOffset(float f) {
        if (!Float.isFinite(f)) {
            return 0.0f;
        }
        return f - (float)Math.floor(f);
    }

    private static void drawGlowSplit(InterfaceModule interfaceModule, float f, float f2, float f3, float f4, float f5, float f6, int n, int n2, float f7, int n3) {
        if (interfaceModule == null || n3 == 0) {
            return;
        }
        float f8 = RectUtil.updateGlowBlend(interfaceModule.rectGlow.getValue());
        if (f8 <= 0.001f) {
            return;
        }
        float f9 = interfaceModule.rectGlowIntensity.getFloat();
        float f10 = interfaceModule.rectGlowRadius.getFloat();
        if (f9 <= 0.0f || f10 <= 0.0f) {
            return;
        }
        float[] fArray = new float[]{f5, f5, f5, f5};
        BuiltGlow builtGlow = new BuiltGlow(f, f2, f3, f4, fArray, n, f9, f10, f6 * f8).withSecondColor(n2, f7).withSplitIndex(n3);
        Render2D.glow(builtGlow);
    }

    private static boolean consumeSplitOverride(float f, float f2, float f3, float f4, float f5) {
        if (!splitOverrideArmed) {
            return false;
        }
        splitOverrideArmed = false;
        RectUtil.drawClientRectSplit(splitOverrideX, splitOverrideY, splitOverrideW, splitOverrideH, splitOverrideRadius, f5, splitOverrideIndex, false, f, f2, f3, f4);
        return true;
    }

    public static void drawClientRectSplit(float f, float f2, float f3, float f4, float f5, float f6, int n, boolean bl, float f7, float f8, float f9, float f10) {
        boolean bl2;
        if (f3 <= 0.0f || f4 <= 0.0f || f6 <= 0.0f || n <= 0) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f11 = RectUtil.clamp(f6);
        float f12 = Math.max(0.0f, f5);
        float f13 = ThemeManager.blurRadius(interfaceModule == null ? 18.0f : interfaceModule.rectBackdropBlur.getFloat());
        int n2 = interfaceModule == null ? -857872385 : interfaceModule.clientPrimaryColor();
        boolean bl3 = bl2 = interfaceModule != null && interfaceModule.usesSecondClientColor();
        if (bl2) {
            lastSecondColor = interfaceModule.clientSecondaryColor();
        }
        float f14 = RectUtil.updateSecondColorBlend(bl2);
        int n3 = ColorUtil.lerpColor(n2, lastSecondColor, f14);
        float f15 = RectUtil.updateColorOffset(interfaceModule != null && interfaceModule.clientColorMovement());
        ThemeProfile profile = ThemeManager.currentProfile();
        float pEdge = profile != null ? profile.edge : 1.0f;
        float pSharp = profile != null ? profile.specular : 1.0f;
        float pRefract = profile != null ? profile.refraction : 1.0f;
        float pDarkening = profile != null ? profile.darkening : 1.0f;
        float f16 = (interfaceModule == null ? 0.18f : interfaceModule.rectEdgeStrength.getFloat()) * pEdge;
        float f17 = (interfaceModule == null ? 55.0f : interfaceModule.rectEdgeSharpness.getFloat()) * pSharp;
        float f18 = (interfaceModule == null ? 0.3f : interfaceModule.rectRefractionStrength.getFloat()) * pRefract;
        RectUtil.drawGlow(interfaceModule, f7, f8, f9, f10, f12, f11, n2, n3, f15);
        if (splitOverrideChildW > 1.0f && splitOverrideChildH > 1.0f) {
            RectUtil.drawGlow(interfaceModule, splitOverrideChildX, splitOverrideChildY, splitOverrideChildW, splitOverrideChildH, splitOverrideChildRadius, f11, n2, n3, f15);
        }
        BuiltGlass builtGlass = new BuiltGlass(f, f2, f3, f4, f12, f12, f12, f12, n2, f11 * 1.0f, f17, n2, 1.0f * pDarkening, true, f16, f18, 0.5f, 0.0f).withBlurRadius(f13).withSecondColor(n3, f15).withSplitIndex(bl ? -n : n);
        float transition = interfaceModule != null ? interfaceModule.getStyleTransition() : 0.0f;
        if (transition > 0.001f) {
            boolean allowMosaic = true;
            if (interfaceModule != null && interfaceModule.isMosaicOnBackgroundOnly()) {
                allowMosaic = f3 >= 250.0f && f4 >= 180.0f;
            }
            if (allowMosaic) {
                builtGlass = builtGlass.withMosaic(true, transition, interfaceModule.mosaicScale.getFloat(), interfaceModule.mosaicSpeed.getFloat(), interfaceModule.mosaicSeam.getFloat(), interfaceModule.mosaicBevel.getFloat(), interfaceModule.mosaicCellGlow.getFloat(), interfaceModule.mosaicMorph.getFloat(), interfaceModule.textReadability.getFloat());
            }
        }
        boolean isHud = RectUtil.isHudRendering();
        if (interfaceModule != null && interfaceModule.isWaveEdgeActiveFor(isHud)) {
            float seed = (float) Math.abs((int)(f * 17.0f + f2 * 31.0f + f3 * 7.0f + f4) % 10000);
            builtGlass = builtGlass.withWaveEdge(
                true,
                1.0f,
                interfaceModule.waveEdgeIntensity.getFloat(),
                interfaceModule.waveEdgeSize.getFloat(),
                interfaceModule.waveEdgeDensity.getFloat(),
                interfaceModule.waveEdgeMotion.getValue(),
                interfaceModule.waveEdgeSpeed.getFloat(),
                interfaceModule.waveEdgeGlow.getFloat(),
                seed,
                isHud ? 2 : 0
            );
        }
        Render2D.glass(builtGlass);
    }

    private static void drawClientRectImpl(float f, float f2, float f3, float f4, float f5, float f6, float f7, boolean bl, boolean bl2) {
        boolean bl3;
        if (f3 <= 0.0f || f4 <= 0.0f || f6 <= 0.0f) {
            return;
        }
        if (RectUtil.consumeSplitOverride(f, f2, f3, f4, RectUtil.clamp(f6))) {
            return;
        }
        InterfaceModule interfaceModule = InterfaceModule.getInstance();
        float f8 = RectUtil.clamp(f6);
        float f9 = (bl2 || interfaceModule == null ? f5 : interfaceModule.rectCornerRadius.getFloat()) + f7;
        float f10 = Math.max(0.0f, Math.min(f9, Math.min(f3, f4) * 0.5f));
        float f11 = ThemeManager.blurRadius(interfaceModule == null ? 18.0f : interfaceModule.rectBackdropBlur.getFloat());
        float f12 = 1.0f;
        float f13 = 1.0f;
        float f14 = 1.0f;
        int n = interfaceModule == null ? -857872385 : interfaceModule.clientPrimaryColor();
        boolean bl4 = bl3 = interfaceModule != null && interfaceModule.usesSecondClientColor();
        if (bl3) {
            lastSecondColor = interfaceModule.clientSecondaryColor();
        }
        float f15 = RectUtil.updateSecondColorBlend(bl3);
        int n2 = ColorUtil.lerpColor(n, lastSecondColor, f15);
        float f16 = RectUtil.updateColorOffset(interfaceModule != null && interfaceModule.clientColorMovement());
        ThemeProfile profile2 = ThemeManager.currentProfile();
        float pEdge2 = profile2 != null ? profile2.edge : 1.0f;
        float pSharp2 = profile2 != null ? profile2.specular : 1.0f;
        float pRefract2 = profile2 != null ? profile2.refraction : 1.0f;
        float pDarkening2 = profile2 != null ? profile2.darkening : 1.0f;
        float f17 = (interfaceModule == null ? 0.18f : interfaceModule.rectEdgeStrength.getFloat()) * pEdge2;
        float f18 = (interfaceModule == null ? 55.0f : interfaceModule.rectEdgeSharpness.getFloat()) * pSharp2;
        float f19 = (interfaceModule == null ? 0.3f : interfaceModule.rectRefractionStrength.getFloat()) * pRefract2;
        float f20 = 0.5f;
        if (bl) {
            RectUtil.drawGlow(interfaceModule, f, f2, f3, f4, f10, f8, n, n2, f16);
        }
        BuiltGlass builtGlass = new BuiltGlass(f, f2, f3, f4, f10, f10, f10, f10, n, f8 * f13, f18, n, f14 * f12 * pDarkening2, true, f17, f19, f20, 0.0f).withBlurRadius(f11).withSecondColor(n2, f16);
        float transition2 = interfaceModule != null ? interfaceModule.getStyleTransition() : 0.0f;
        if (transition2 > 0.001f) {
            boolean allowMosaic = true;
            if (interfaceModule != null && interfaceModule.isMosaicOnBackgroundOnly()) {
                allowMosaic = f3 >= 250.0f && f4 >= 180.0f;
            }
            if (allowMosaic) {
                builtGlass = builtGlass.withMosaic(true, transition2, interfaceModule.mosaicScale.getFloat(), interfaceModule.mosaicSpeed.getFloat(), interfaceModule.mosaicSeam.getFloat(), interfaceModule.mosaicBevel.getFloat(), interfaceModule.mosaicCellGlow.getFloat(), interfaceModule.mosaicMorph.getFloat(), interfaceModule.textReadability.getFloat());
            }
        }
        boolean isHud2 = RectUtil.isHudRendering();
        boolean allowWave = !bl2 && (f3 >= 36.0f && f4 >= 24.0f);
        if (interfaceModule != null && allowWave && interfaceModule.isWaveEdgeActiveFor(isHud2)) {
            float seed = (float) Math.abs((int)(f * 17.0f + f2 * 31.0f + f3 * 7.0f + f4) % 10000);
            builtGlass = builtGlass.withWaveEdge(
                true,
                1.0f,
                interfaceModule.waveEdgeIntensity.getFloat(),
                interfaceModule.waveEdgeSize.getFloat(),
                interfaceModule.waveEdgeDensity.getFloat(),
                interfaceModule.waveEdgeMotion.getValue(),
                interfaceModule.waveEdgeSpeed.getFloat(),
                interfaceModule.waveEdgeGlow.getFloat(),
                seed,
                isHud2 ? 2 : 0
            );
        }
        Render2D.glass(builtGlass);
    }

    private static void drawGlow(InterfaceModule interfaceModule, float f, float f2, float f3, float f4, float f5, float f6, int n, int n2, float f7) {
        RectUtil.drawGlowWithSpans(interfaceModule, f, f2, f3, f4, f5, f6, n, n2, f7, null, 0, true, false);
    }
}

