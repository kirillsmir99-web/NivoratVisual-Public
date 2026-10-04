package rtx.nv.utils.render.render2d;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.Window;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

import rtx.nv.api.modules.ModuleManager;
import rtx.nv.api.modules.impl.Interface.ClickGui;

public final class Render2DCoordinateSpace {
    private Render2DCoordinateSpace() {
    }

    public static Matrix3x2f pose(DrawContext drawContext) {
        float f = Render2DCoordinateSpace.guiIndependentScale();
        Matrix3x2f matrix3x2f = new Matrix3x2f((Matrix3x2fc)drawContext.getMatrices());
        if (f == 1.0f) {
            return matrix3x2f;
        }
        return new Matrix3x2f().scale(f).mul((Matrix3x2fc)matrix3x2f);
    }

    public static int guiScale() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return 1;
        }
        Window window = minecraftClient.getWindow();
        if (window == null) {
            return 1;
        }
        return Math.max(1, window.getScaleFactor());
    }

    public static float guiIndependentScale() {
        return Render2DCoordinateSpace.designGuiScale() / (float)Render2DCoordinateSpace.guiScale();
    }

    private static float currentAnimatedScale = -1.0f;
    private static long lastScaleUpdateNs = System.nanoTime();

    public static float designGuiScale() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return 2.0f;
        }
        int fbW = mc.getWindow().getFramebufferWidth();
        int fbH = mc.getWindow().getFramebufferHeight();
        if (fbW <= 0 || fbH <= 0) {
            return 2.0f;
        }

        ClickGui clickGui = null;
        try {
            clickGui = ModuleManager.get().get(ClickGui.class);
        } catch (Throwable ignored) {
        }

        String mode = clickGui != null ? clickGui.scaleMode.getValue() : ClickGui.SCALE_AUTO;
        float logicalWidth = mc.currentScreen instanceof rtx.nv.api.ui.UI ? rtx.nv.api.ui.UI.layoutWidth() : 430.0f;
        float targetScale;

        if (ClickGui.SCALE_MINECRAFT.equals(mode)) {
            int mcGuiScale = guiScale();
            float maxSafeScale = Math.min(((float) fbH - 24.0f) / 290.0f, ((float) fbW - 24.0f) / logicalWidth);
            targetScale = Math.max(0.5f, Math.min((float) mcGuiScale, maxSafeScale));
        } else if (ClickGui.SCALE_CUSTOM.equals(mode) && clickGui != null) {
            float customFactor = clickGui.customScale.getFloat();
            float baseAuto = computeAutoScale(fbW, fbH);
            float maxFitW = Math.max(0.4f, ((float) fbW - 16.0f) / logicalWidth);
            float maxFitH = Math.max(0.4f, ((float) fbH - 16.0f) / 290.0f);
            float maxFit = Math.min(maxFitW, maxFitH);
            targetScale = Math.max(0.4f, Math.min(maxFit, baseAuto * customFactor));
        } else {
            targetScale = computeAutoScale(fbW, fbH);
        }

        return Math.min(targetScale, Math.max(0.25f, ((float) fbW - 24.0f) / logicalWidth));
    }

    private static float computeAutoScale(int fbW, int fbH) {
        float maxFitW = Math.max(0.4f, ((float) fbW - 24.0f) / 430.0f);
        float maxFitH = Math.max(0.4f, ((float) fbH - 24.0f) / 290.0f);
        float maxFit = Math.min(maxFitW, maxFitH);

        float t = Math.max(0.0f, Math.min(1.0f, ((float) fbH - 480.0f) / (1080.0f - 480.0f)));
        float targetHeightRatio = 0.85f - t * (0.85f - 0.54f);

        float desiredHeight = (float) fbH * targetHeightRatio;
        float scale = desiredHeight / 290.0f;

        if (fbH > 1080) {
            scale = 2.0f * ((float) fbH / 1080.0f);
        }

        scale = Math.min(scale, maxFit);
        return Math.max(0.5f, Math.min(4.0f, scale));
    }

    public static void applyGuiScaleIndependence(Matrix3x2f matrix3x2f) {
        float f = Render2DCoordinateSpace.guiIndependentScale();
        if (f != 1.0f) {
            matrix3x2f.set((Matrix3x2fc)new Matrix3x2f().scale(f).mul((Matrix3x2fc)matrix3x2f));
        }
    }

    public static float toGui(float f) {
        return f * Render2DCoordinateSpace.guiIndependentScale();
    }

    public static int toGuiInt(float f) {
        return Math.round(Render2DCoordinateSpace.toGui(f));
    }
}

