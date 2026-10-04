package rtx.nv.utils.render.render2d.line;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class LineRenderState implements SimpleGuiElementRenderState {
    private final BuiltLine line;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    LineRenderState(Matrix3x2f pose, BuiltLine line, ScreenRect scissor) {
        this.line = line;
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.scissorArea = scissor;
        int x = (int)Math.floor(Math.min(line.x1(), line.x2()) - line.thickness());
        int y = (int)Math.floor(Math.min(line.y1(), line.y2()) - line.thickness());
        int w = (int)Math.ceil(Math.abs(line.x2() - line.x1()) + line.thickness() * 2.0f);
        int h = (int)Math.ceil(Math.abs(line.y2() - line.y1()) + line.thickness() * 2.0f);
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, w), Math.max(1, h)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return LineRenderer.LINE_PIPELINE; }
    public ScreenRect scissorArea() { return this.scissorArea; }
    public TextureSetup textureSetup() { return TextureSetup.empty(); }

    public void setupVertices(VertexConsumer vertices) {
        int id = LineRenderer.getInstance().reserve(this.line);
        if (id < 0) return;
        float x1 = Math.min(this.line.x1(), this.line.x2()) - this.line.thickness();
        float y1 = Math.min(this.line.y1(), this.line.y2()) - this.line.thickness();
        float x2 = Math.max(this.line.x1(), this.line.x2()) + this.line.thickness();
        float y2 = Math.max(this.line.y1(), this.line.y2()) + this.line.thickness();
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc)(Object)this.pose, x, y).color(u, v, 255, 255).lineWidth((float)(id + 1));
    }
}