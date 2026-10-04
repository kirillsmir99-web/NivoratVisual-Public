package rtx.nv.api.ui;

import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.color.ColorUtil;

/** The client identity uses the existing NV logo asset. */
public final class BrandMark {
    private BrandMark() {}
    public static void draw(float x, float y, float size, float alpha) {
        if (size <= 0 || alpha <= .005f) return;
        Render2D.image("nv:textures/logo.png", x, y, size, size,
            ColorUtil.rgba(255, 255, 255, Math.round(255 * Math.max(0, Math.min(1, alpha)))));
    }
}
