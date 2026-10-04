package rtx.nv.utils.render.render2d.rectangle.recthalftone;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.BuiltHalftoneRectangle;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.HalftoneRectangleRenderer;

final class HalftoneRectangleRenderState
implements SimpleGuiElementRenderState {
    private final BuiltHalftoneRectangle rectangle;
    private final Matrix3x2f pose;
    private final ScreenRect bounds;

    HalftoneRectangleRenderState(Matrix3x2f matrix3x2f, BuiltHalftoneRectangle builtHalftoneRectangle) {
        this.rectangle = builtHalftoneRectangle;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.bounds = new ScreenRect(Math.round(builtHalftoneRectangle.x()), Math.round(builtHalftoneRectangle.y()), Math.round(builtHalftoneRectangle.width()), Math.round(builtHalftoneRectangle.height())).transformEachVertex((Matrix3x2fc)(Object)this.pose);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return HalftoneRectangleRenderer.HALFTONE_RECTANGLE_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return null;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = HalftoneRectangleRenderer.getInstance().reserve(this.rectangle);
        if (n < 0) {
            return;
        }
        float f = this.rectangle.x();
        float f2 = this.rectangle.y();
        float f3 = this.rectangle.x() + this.rectangle.width();
        float f4 = this.rectangle.y() + this.rectangle.height();
        this.vertex(vertices, Math.round(f), Math.round(f2), 0, 0, n);
        this.vertex(vertices, Math.round(f), Math.round(f4), 0, 255, n);
        this.vertex(vertices, Math.round(f3), Math.round(f4), 255, 255, n);
        this.vertex(vertices, Math.round(f3), Math.round(f2), 255, 0, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

