package rtx.nv.utils.render.render2d.ripple;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.ripple.BuiltRipple;
import rtx.nv.utils.render.render2d.ripple.RippleRenderer;

public final class RippleRenderState
implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final BuiltRipple ripple;
    private final ScreenRect scissorArea;

    public RippleRenderState(Matrix3x2f matrix3x2f, BuiltRipple builtRipple, ScreenRect screenRect) {
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.ripple = builtRipple;
        this.scissorArea = screenRect;
    }

    public ScreenRect bounds() {
        return new ScreenRect(Math.round(this.ripple.x), Math.round(this.ripple.y), Math.round(this.ripple.width), Math.round(this.ripple.height)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
    }

    public RenderPipeline pipeline() {
        return RippleRenderer.RIPPLE_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = RippleRenderer.getInstance().reserve(this.ripple);
        if (n < 0) {
            return;
        }
        this.vertex(vertices, this.ripple.x, this.ripple.y, 0.0f, 0.0f, n);
        this.vertex(vertices, this.ripple.x, this.ripple.y + this.ripple.height, 0.0f, 1.0f, n);
        this.vertex(vertices, this.ripple.x + this.ripple.width, this.ripple.y + this.ripple.height, 1.0f, 1.0f, n);
        this.vertex(vertices, this.ripple.x + this.ripple.width, this.ripple.y, 1.0f, 0.0f, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).lineWidth((float)(n + 1));
    }
}

