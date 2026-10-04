package rtx.nv.utils.render.render2d.blur;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;

final class BlurRenderState
implements SimpleGuiElementRenderState {
    private final BuiltBlur blur;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private final BlurCapture capture;

    BlurRenderState(Matrix3x2f matrix3x2f, BuiltBlur builtBlur, ScreenRect screenRect, BlurCapture blurCapture) {
        this.blur = builtBlur;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        this.capture = blurCapture;
        ScreenRect screenRect2 = new ScreenRect(Math.round(builtBlur.x()), Math.round(builtBlur.y()), Math.round(builtBlur.width()), Math.round(builtBlur.height())).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return BlurFramebuffer.BATCHED_BLUR_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.capture.setup;
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = BlurFramebuffer.getInstance().reserve(this.blur, this.capture);
        if (n < 0) {
            return;
        }
        float f = this.blur.x();
        float f2 = this.blur.y();
        float f3 = this.blur.x() + this.blur.width();
        float f4 = this.blur.y() + this.blur.height();
        this.vertex(vertices, f, f2, this.blur.colorTopLeft(), n);
        this.vertex(vertices, f, f4, this.blur.colorBottomLeft(), n);
        this.vertex(vertices, f3, f4, this.blur.colorBottomRight(), n);
        this.vertex(vertices, f3, f2, this.blur.colorTopRight(), n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n).lineWidth((float)(n2 + 1));
    }
}

