package rtx.nv.utils.render.render2d.zippy;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.zippy.BuiltZippy;
import rtx.nv.utils.render.render2d.zippy.ZippyRenderer;

final class ZippyRenderState
implements SimpleGuiElementRenderState {
    private final BuiltZippy zippy;
    private final Matrix3x2f pose;
    private final ScreenRect bounds;

    ZippyRenderState(Matrix3x2f matrix3x2f, BuiltZippy builtZippy) {
        this.zippy = builtZippy;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.bounds = new ScreenRect(Math.round(builtZippy.x()), Math.round(builtZippy.y()), Math.round(builtZippy.width()), Math.round(builtZippy.height())).transformEachVertex((Matrix3x2fc)(Object)this.pose);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return ZippyRenderer.ZIPPY_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return null;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = ZippyRenderer.getInstance().reserve(this.zippy);
        if (n < 0) {
            return;
        }
        float f = this.zippy.x();
        float f2 = this.zippy.y();
        float f3 = this.zippy.x() + this.zippy.width();
        float f4 = this.zippy.y() + this.zippy.height();
        this.vertex(vertices, f, f2, 0, 0, n);
        this.vertex(vertices, f, f4, 0, 255, n);
        this.vertex(vertices, f3, f4, 255, 255, n);
        this.vertex(vertices, f3, f2, 255, 0, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

