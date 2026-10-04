package rtx.nv.utils.render.render2d.shimmer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.shimmer.BuiltShimmer;
import rtx.nv.utils.render.render2d.shimmer.ShimmerRenderer;

final class ShimmerRenderState
implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final BuiltShimmer shimmer;
    private final ScreenRect scissor;

    ShimmerRenderState(Matrix3x2f matrix3x2f, BuiltShimmer builtShimmer, ScreenRect screenRect) {
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.shimmer = builtShimmer;
        this.scissor = screenRect;
    }

    public ScreenRect bounds() {
        int n = (int)Math.floor(this.shimmer.x());
        int n2 = (int)Math.floor(this.shimmer.y());
        int n3 = Math.max(1, (int)Math.ceil(this.shimmer.width()));
        int n4 = Math.max(1, (int)Math.ceil(this.shimmer.height()));
        ScreenRect screenRect = new ScreenRect(n, n2, n3, n4).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        return this.scissor == null ? screenRect : this.scissor.intersection(screenRect);
    }

    public RenderPipeline pipeline() {
        return ShimmerRenderer.SHIMMER_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissor;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = ShimmerRenderer.getInstance().reserve(this);
        if (n < 0) {
            return;
        }
        float f = this.shimmer.x();
        float f2 = this.shimmer.y();
        float f3 = this.shimmer.x() + this.shimmer.width();
        float f4 = this.shimmer.y() + this.shimmer.height();
        int n2 = this.shimmer.color();
        this.vertex(vertices, f, f2, 0.0f, 0.0f, n2, n);
        this.vertex(vertices, f, f4, 0.0f, 1.0f, n2, n);
        this.vertex(vertices, f3, f4, 1.0f, 1.0f, n2, n);
        this.vertex(vertices, f3, f2, 1.0f, 0.0f, n2, n);
    }

    BuiltShimmer shimmer() {
        return this.shimmer;
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n, int n2) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).color(n).lineWidth((float)(n2 + 1));
    }
}

