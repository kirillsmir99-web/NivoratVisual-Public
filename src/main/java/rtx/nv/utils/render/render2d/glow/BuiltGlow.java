package rtx.nv.utils.render.render2d.glow;

import net.minecraft.client.gui.DrawContext;

public record BuiltGlow(
    float x, float y, float width, float height,
    float[] radii, int color, float intensity, float radius, float alpha,
    int secondColor, float colorOffset, float[] spans, int spanCount,
    boolean leftAligned, boolean bottomAnchored, int splitIndex
) {
    public BuiltGlow(float x, float y, float width, float height, float[] radii, int color, float intensity, float radius, float alpha) {
        this(x, y, width, height, radii, color, intensity, radius, alpha, color, 0.0f, null, 0, false, false, 0);
    }

    public BuiltGlow(float x, float y, float width, float height, float[] radii, int color, float intensity, float radius, float alpha, int splitIndex) {
        this(x, y, width, height, radii, color, intensity, radius, alpha, color, 0.0f, null, 0, false, false, splitIndex);
    }

    public BuiltGlow withSecondColor(int color, float offset) {
        return new BuiltGlow(x, y, width, height, radii, this.color, intensity, radius, alpha, color, offset, spans, spanCount, leftAligned, bottomAnchored, splitIndex);
    }

    public BuiltGlow withSpans(float[] spans, int count) {
        return new BuiltGlow(x, y, width, height, radii, color, intensity, radius, alpha, secondColor, colorOffset, spans, count, leftAligned, bottomAnchored, splitIndex);
    }

    public BuiltGlow withAlignment(boolean left, boolean bottom) {
        return new BuiltGlow(x, y, width, height, radii, color, intensity, radius, alpha, secondColor, colorOffset, spans, spanCount, left, bottom, splitIndex);
    }

    public BuiltGlow withSplitIndex(int split) {
        return new BuiltGlow(x, y, width, height, radii, color, intensity, radius, alpha, secondColor, colorOffset, spans, spanCount, leftAligned, bottomAnchored, split);
    }

    public BuiltGlow withBoxesMode() {
        return this;
    }

    public BuiltGlow withPalette(Object palette, float phase, int styleId, boolean closed) {
        return this;
    }

    public float effectivePad() {
        return this.radius + 4.0f;
    }

    public boolean visible() {
        return this.width > 0.0f && this.height > 0.0f && (this.color >>> 24) != 0;
    }

    public void render(DrawContext drawContext) {
        GlowRenderer.getInstance().draw(drawContext, this);
    }
}