package rtx.nv.utils.render.render2d.arc;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class ArcRenderState implements SimpleGuiElementRenderState {
    private final BuiltArc arc;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    ArcRenderState(Matrix3x2f pose, BuiltArc arc, ScreenRect scissor) {
        this.arc = arc;
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.scissorArea = scissor;
        int x = (int)Math.floor(arc.x() - arc.halfWidth());
        int y = (int)Math.floor(arc.y() - arc.lift());
        int w = (int)Math.ceil(arc.halfWidth() * 2.0f);
        int h = (int)Math.ceil(arc.lift() + arc.thickness() + arc.feather() * 2.0f);
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, w), Math.max(1, h)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return ArcRenderer.ARC_PIPELINE; }
    public ScreenRect scissorArea() { return this.scissorArea; }
    public TextureSetup textureSetup() { return TextureSetup.empty(); }

    public void setupVertices(VertexConsumer vertices) {
        int id = ArcRenderer.getInstance().reserve(this.arc);
        if (id < 0) return;
        float x1 = this.arc.x() - this.arc.halfWidth();
        float y1 = this.arc.y() - this.arc.lift();
        float x2 = this.arc.x() + this.arc.halfWidth();
        float y2 = this.arc.y() + this.arc.thickness() + this.arc.feather();
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc)(Object)this.pose, x, y).color(u, v, 255, 255).lineWidth((float)(id + 1));
    }
}