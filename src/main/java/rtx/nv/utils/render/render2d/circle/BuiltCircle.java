package rtx.nv.utils.render.render2d.circle;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.circle.CircleRenderer;

public record BuiltCircle(float x, float y, float radius, float thickness, float smoothness, int color) {
    public static final float DEFAULT_SMOOTHNESS = 0.5f;

    public BuiltCircle(float f, float f2, float f3, int n) {
        this(f, f2, f3, 0.0f, 0.5f, n);
    }

    public boolean visible() {
        return this.radius > 0.0f && this.color >>> 24 != 0;
    }

    public void render(DrawContext drawContext) {
        CircleRenderer.getInstance().draw(drawContext, this);
    }

    public BuiltCircle withThickness(float f) {
        return new BuiltCircle(this.x, this.y, this.radius, f, this.smoothness, this.color);
    }

    public BuiltCircle withSmoothness(float f) {
        return new BuiltCircle(this.x, this.y, this.radius, this.thickness, f, this.color);
    }
}

