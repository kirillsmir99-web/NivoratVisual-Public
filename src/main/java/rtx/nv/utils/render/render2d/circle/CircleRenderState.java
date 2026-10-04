package rtx.nv.utils.render.render2d.circle;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.circle.BuiltCircle;
import rtx.nv.utils.render.render2d.circle.CircleRenderer;

final class CircleRenderState
implements SimpleGuiElementRenderState {
    private final BuiltCircle circle;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    CircleRenderState(Matrix3x2f matrix3x2f, BuiltCircle builtCircle, ScreenRect screenRect) {
        this.circle = builtCircle;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        float f = CircleRenderState.extent(builtCircle);
        int n = (int)Math.floor(builtCircle.x() - f);
        int n2 = (int)Math.floor(builtCircle.y() - f);
        int n3 = (int)Math.ceil(f * 2.0f);
        ScreenRect screenRect2 = new ScreenRect(n, n2, n3, n3).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return CircleRenderer.CIRCLE_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = CircleRenderer.getInstance().reserve(this.circle);
        if (n < 0) {
            return;
        }
        float f = CircleRenderState.extent(this.circle);
        float f2 = this.circle.x() - f;
        float f3 = this.circle.y() - f;
        float f4 = this.circle.x() + f;
        float f5 = this.circle.y() + f;
        this.vertex(vertices, f2, f3, 0, 0, n);
        this.vertex(vertices, f2, f5, 0, 255, n);
        this.vertex(vertices, f4, f5, 255, 255, n);
        this.vertex(vertices, f4, f3, 255, 0, n);
    }

    private static float extent(BuiltCircle builtCircle) {
        return builtCircle.radius() + Math.max(builtCircle.smoothness(), 0.0f);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

