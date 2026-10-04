package rtx.nv.utils.render.render2d.glass;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.glass.BuiltGlass;
import rtx.nv.utils.render.render2d.glass.GlassRenderer;

final class GlassRenderState
implements SimpleGuiElementRenderState {
    private final BuiltGlass glass;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private final BlurCapture capture;

    GlassRenderState(Matrix3x2f matrix3x2f, BuiltGlass builtGlass, ScreenRect screenRect, BlurCapture blurCapture) {
        this.glass = builtGlass;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        this.capture = blurCapture;
        float minD = Math.min(builtGlass.width(), builtGlass.height());
        float margin = (builtGlass.liveEdgeEnabled() && builtGlass.liveEdgeActivation() > 0.001f) ? (builtGlass.liveEdgeProfile() == 0 ? (minD < 60.0f ? 10.0f : 18.0f) : 8.0f) : 0.0f;
        ScreenRect screenRect2 = new ScreenRect(
            Math.round(builtGlass.x() - margin),
            Math.round(builtGlass.y() - margin),
            Math.round(builtGlass.width() + 2.0f * margin),
            Math.round(builtGlass.height() + 2.0f * margin)
        ).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return GlassRenderer.GLASS_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.capture.setup;
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = GlassRenderer.getInstance().reserve(this.glass, this.capture);
        if (n < 0) {
            return;
        }
        float minD = Math.min(this.glass.width(), this.glass.height());
        float m = (this.glass.liveEdgeEnabled() && this.glass.liveEdgeActivation() > 0.001f) ? (this.glass.liveEdgeProfile() == 0 ? (minD < 60.0f ? 10.0f : 18.0f) : 8.0f) : 0.0f;
        float f = this.glass.x() - m;
        float f2 = this.glass.y() - m;
        float f3 = this.glass.x() + this.glass.width() + m;
        float f4 = this.glass.y() + this.glass.height() + m;
        this.vertex(vertices, f, f2, n);
        this.vertex(vertices, f, f4, n);
        this.vertex(vertices, f3, f4, n);
        this.vertex(vertices, f3, f2, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(this.glass.color()).lineWidth((float)(n + 1));
    }
}

