package rtx.nv.utils.render.render2d.glow;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class GlowRenderState implements SimpleGuiElementRenderState {
    private final BuiltGlow glow;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    private final GlowCapture capture;

    GlowRenderState(Matrix3x2f pose, BuiltGlow glow, ScreenRect scissor, GlowCapture capture) {
        this.glow = glow;
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.scissorArea = scissor;
        int x = (int)Math.floor(glow.x() - glow.radius());
        int y = (int)Math.floor(glow.y() - glow.radius());
        int w = (int)Math.ceil(glow.width() + glow.radius() * 2.0f);
        int h = (int)Math.ceil(glow.height() + glow.radius() * 2.0f);
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, w), Math.max(1, h)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
        this.capture = capture;
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return GlowRenderer.GLOW_COMPOSITE_PIPELINE; }
    public ScreenRect scissorArea() { return this.scissorArea; }
    public TextureSetup textureSetup() {
        return this.capture != null && this.capture.setup != null ? this.capture.setup : TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int id = GlowRenderer.getInstance().reserve(this.glow, this.capture);
        if (id < 0) return;
        float x1 = this.glow.x() - this.glow.radius();
        float y1 = this.glow.y() - this.glow.radius();
        float x2 = this.glow.x() + this.glow.width() + this.glow.radius();
        float y2 = this.glow.y() + this.glow.height() + this.glow.radius();
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc)(Object)this.pose, x, y).color(u, v, 255, 255).lineWidth((float)(id + 1));
    }
}