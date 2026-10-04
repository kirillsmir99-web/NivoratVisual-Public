package rtx.nv.utils.render.render2d.outline.outline360;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.outline.outline360.BuiltOutline360;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360Renderer;

final class Outline360RenderState
implements SimpleGuiElementRenderState {
    private final BuiltOutline360 outline;
    private final Matrix3x2f pose;
    private final ScreenRect bounds;

    Outline360RenderState(Matrix3x2f matrix3x2f, BuiltOutline360 builtOutline360) {
        this.outline = builtOutline360;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.bounds = new ScreenRect(Math.round(builtOutline360.x()), Math.round(builtOutline360.y()), Math.round(builtOutline360.width()), Math.round(builtOutline360.height())).transformEachVertex((Matrix3x2fc)(Object)this.pose);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return Outline360Renderer.OUTLINE_360_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return null;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = Outline360Renderer.getInstance().reserve(this.outline);
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

