package rtx.nv.utils.render.render2d.rectangle.recthalficon;

import net.minecraft.client.gui.DrawContext;

public record BuiltHalfIconRectangle(
    float x, float y, float width, float height,
    float radiusTopLeft, float radiusTopRight, float radiusBottomRight, float radiusBottomLeft,
    int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft,
    float smoothness, String fontName, String icon, float iconSize, int iconColor,
    float spacingX, float spacingY, float paddingX, float paddingY,
    float jitterX, float jitterY, float rotationMaxDegrees, long seed
) {
    public BuiltHalfIconRectangle(float x, float y, float w, float h, float radius, int color, String fontName, String icon, float iconSize, int iconColor, float iconX, float iconY, float iconRot) {
        this(x, y, w, h, radius, radius, radius, radius, color, color, color, color, 0.5f, fontName, icon, iconSize, iconColor, iconSize, iconSize, 0.0f, 0.0f, 0.0f, 0.0f, iconRot, 0L);
    }

    public boolean backgroundVisible() {
        return this.width > 0.0f && this.height > 0.0f;
    }

    public boolean iconsVisible() {
        return this.icon != null && !this.icon.isEmpty();
    }

    public void render(DrawContext drawContext) {
        HalfIconRectangleRenderer.getInstance().draw(drawContext, this);
    }
}