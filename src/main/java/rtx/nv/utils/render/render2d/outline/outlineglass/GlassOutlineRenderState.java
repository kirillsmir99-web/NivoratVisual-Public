package rtx.nv.utils.render.render2d.outline.outlineglass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.outline.outlineglass.BuiltGlassOutline;
import rtx.nv.utils.render.render2d.outline.outlineglass.GlassOutlineRenderer;

final class GlassOutlineRenderState
implements SimpleGuiElementRenderState {
    private final BuiltGlassOutline outline;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private final BlurCapture capture;

    GlassOutlineRenderState(Matrix3x2f matrix3x2f, BuiltGlassOutline builtGlassOutline, ScreenRect screenRect, BlurCapture blurCapture) {
        this.outline = builtGlassOutline;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        this.capture = blurCapture;
        ScreenRect screenRect2 = new ScreenRect(Math.round(builtGlassOutline.x()), Math.round(builtGlassOutline.y()), Math.round(builtGlassOutline.width()), Math.round(builtGlassOutline.height())).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return GlassOutlineRenderer.GLASS_OUTLINE_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.capture.setup;
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = GlassOutlineRenderer.getInstance().reserve(this.outline, this.capture);
        if (n < 0) {
            return;
        }
        float f = this.outline.x();
        float f2 = this.outline.y();
        float f3 = this.outline.x() + this.outline.width();
        float f4 = this.outline.y() + this.outline.height();
        this.vertex(vertices, f, f2, 0, 0, n);
        this.vertex(vertices, f, f4, 0, 255, n);
        this.vertex(vertices, f3, f4, 255, 255, n);
        this.vertex(vertices, f3, f2, 255, 0, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

