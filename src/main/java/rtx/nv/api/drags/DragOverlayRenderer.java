package rtx.nv.api.drags;

import net.minecraft.client.gui.DrawContext;
import rtx.nv.api.modules.impl.Interface.InterfaceModule;
import rtx.nv.utils.render.render2d.Render2D;

public final class DragOverlayRenderer {
    private DragOverlayRenderer() {}

    public static void render(DrawContext context, DragController controller, float x, float y, float w, float h, float alpha) {
        // Ghost renderer removed per Task 43
    }

    public static void clampToScreen(DragController controller, float w, float h) {
        if (!Float.isFinite(w) || !Float.isFinite(h)) return;
        float x = controller.getTargetX(), y = controller.getTargetY();
        controller.setTargetX(Position.clampX(Float.isFinite(x) ? x : Position.SCREEN_MARGIN, Math.max(0, w)));
        controller.setTargetY(Position.clampY(Float.isFinite(y) ? y : Position.SCREEN_MARGIN, Math.max(0, h)));
    }

    /** Align the center, flush edges, or outer margins when the pointer comes within magnetic threshold. */
    public static void applyGridSnap(DragController controller, float w, float h) {
        if (!Float.isFinite(w) || !Float.isFinite(h)) return;
        float screenW = Position.screenWidth(), screenH = Position.screenHeight();
        float x = snap(controller.getTargetX(), w, screenW);
        float y = snap(controller.getTargetY(), h, screenH);
        if (Float.isFinite(x)) {
            controller.setTargetX(x);
            controller.setSnapLineX(Math.abs(x - (screenW - w) * 0.5f) < 0.01f ? screenW * 0.5f : (x <= 1.0f ? 0.0f : (Math.abs(x - (screenW - w)) < 1.0f ? screenW : (x <= Position.SCREEN_MARGIN ? x : x + w))));
        }
        if (Float.isFinite(y)) {
            controller.setTargetY(y);
            controller.setSnapLineY(Math.abs(y - (screenH - h) * 0.5f) < 0.01f ? screenH * 0.5f : (y <= 1.0f ? 0.0f : (Math.abs(y - (screenH - h)) < 1.0f ? screenH : (y <= Position.SCREEN_MARGIN ? y : y + h))));
        }
        clampToScreen(controller, w, h);
    }

    private static float snap(float value, float size, float extent) {
        if (size > extent) return Float.NaN;
        float center = (extent - size) * 0.5f;
        if (Math.abs(value - center) <= 14.0f) return center;
        if (Math.abs(value - 0.0f) <= 10.0f) return 0.0f;
        if (Math.abs(value - Position.SCREEN_MARGIN) <= 8.0f) return Position.SCREEN_MARGIN;
        float flushEnd = extent - size;
        if (Math.abs(value - flushEnd) <= 10.0f) return flushEnd;
        float end = extent - size - Position.SCREEN_MARGIN;
        if (Math.abs(value - end) <= 8.0f) return end;
        return Float.NaN;
    }
}
