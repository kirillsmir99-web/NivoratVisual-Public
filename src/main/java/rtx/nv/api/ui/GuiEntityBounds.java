package rtx.nv.api.ui;

import net.minecraft.client.gui.DrawContext;
import org.joml.Vector2f;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;

/** Special entity elements do not inherit DrawContext's 2D matrix. */
public record GuiEntityBounds(int left, int top, int right, int bottom, float scale) {
    public static GuiEntityBounds from(DrawContext context, float x, float y, float width, float height) {
        var matrix = context.getMatrices();
        var a = matrix.transformPosition(x, y, new Vector2f());
        var b = matrix.transformPosition(x + width, y + height, new Vector2f());
        float gui = Render2DCoordinateSpace.guiIndependentScale();
        float scale = (float)Math.sqrt(matrix.m00()*matrix.m00() + matrix.m01()*matrix.m01()) * gui;
        return new GuiEntityBounds(Math.round(a.x * gui), Math.round(a.y * gui), Math.round(b.x * gui), Math.round(b.y * gui), scale);
    }
}
