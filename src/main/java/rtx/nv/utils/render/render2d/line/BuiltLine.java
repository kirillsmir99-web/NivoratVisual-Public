package rtx.nv.utils.render.render2d.line;

import net.minecraft.client.gui.DrawContext;

public record BuiltLine(float x1, float y1, float x2, float y2, float thickness, int color, float feather, float smoothness, float fadeStart, float fadeEnd) {
    public BuiltLine(float x1, float y1, float x2, float y2, float thickness, int color, float feather, float smoothness) {
        this(x1, y1, x2, y2, thickness, color, feather, smoothness, 0.0f, 0.0f);
    }

    public BuiltLine(float x1, float y1, float x2, float y2, float thickness, int color) {
        this(x1, y1, x2, y2, thickness, color, 0.5f, 0.5f, 0.0f, 0.0f);
    }

    public float length() {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    public boolean visible() {
        return this.thickness > 0.0f && (this.color >>> 24) != 0;
    }

    public void render(DrawContext drawContext) {
        LineRenderer.getInstance().submit(drawContext, this);
    }
}