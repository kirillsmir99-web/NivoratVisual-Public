package rtx.nv.utils.render.render2d.arc;

import net.minecraft.client.gui.DrawContext;

public record BuiltArc(float x, float y, float halfWidth, float lift, float thickness, float feather, int color) {
    public boolean visible() {
        return this.halfWidth > 0.0f && (this.color >>> 24) != 0;
    }

    public void render(DrawContext drawContext) {
        ArcRenderer.getInstance().submit(drawContext, this);
    }
}