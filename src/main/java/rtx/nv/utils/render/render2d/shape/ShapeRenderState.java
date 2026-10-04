package rtx.nv.utils.render.render2d.shape;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.blur.BlurCapture;

final class ShapeRenderState implements SimpleGuiElementRenderState {
    private final BuiltShape shape;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private final BlurCapture blurCapture;

    ShapeRenderState(Matrix3x2f pose, BuiltShape shape, ScreenRect scissor, BlurCapture blurCapture) {
        this.shape = shape;
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.scissorArea = scissor;
        this.blurCapture = blurCapture;
        int x = (int)Math.floor(shape.x());
        int y = (int)Math.floor(shape.y());
        int w = (int)Math.ceil(shape.width());
        int h = (int)Math.ceil(shape.height());
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, w), Math.max(1, h)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
    }

    ShapeRenderState(Matrix3x2f pose, BuiltShape shape, ScreenRect scissor) {
        this(pose, shape, scissor, null);
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return ShapeRenderer.SHAPE_PIPELINE; }
    public ScreenRect scissorArea() { return this.scissorArea; }
    public TextureSetup textureSetup() { return TextureSetup.empty(); }

    public void setupVertices(VertexConsumer vertices) {
        int id = ShapeRenderer.getInstance().reserve(this.shape, this.blurCapture);
        if (id < 0) return;
        float x1 = this.shape.x();
        float y1 = this.shape.y();
        float x2 = this.shape.x() + this.shape.width();
        float y2 = this.shape.y() + this.shape.height();
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc)(Object)this.pose, x, y).color(u, v, 255, 255).lineWidth((float)(id + 1));
    }
}