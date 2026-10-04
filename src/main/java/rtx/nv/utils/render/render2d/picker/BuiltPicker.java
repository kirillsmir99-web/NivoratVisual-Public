package rtx.nv.utils.render.render2d.picker;
import net.minecraft.client.gui.DrawContext;
import rtx.nv.utils.render.render2d.picker.PickerRenderer;

public record BuiltPicker(float x, float y, float w, float h, int mode, float radius, float smoothness, float alpha, int solidColor, float checkerPx) {
    public boolean visible() {
        return this.w > 0.0f && this.h > 0.0f && this.alpha > 0.0f;
    }

    public static BuiltPicker alpha(float f, float f2, float f3, float f4, int n, float f5) {
        return new BuiltPicker(f, f2, f3, f4, 1, f4 * 0.25f, 0.75f, f5, n, f4 * 0.5f);
    }

    public void render(DrawContext drawContext) {
        PickerRenderer.getInstance().draw(drawContext, this);
    }

    public static BuiltPicker hue(float f, float f2, float f3, float f4, float f5) {
        return new BuiltPicker(f, f2, f3, f4, 0, f4 * 0.25f, 0.75f, f5, 0, f4);
    }
}

