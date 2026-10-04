package rtx.nv.utils.render.render2d.sectormask;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class SectorMaskRenderState implements SimpleGuiElementRenderState {
    private final BuiltSectorMask mask;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    SectorMaskRenderState(Matrix3x2f pose, BuiltSectorMask mask, ScreenRect scissor) {
        this.mask = mask;
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.scissorArea = scissor;
        int x = (int)Math.floor(mask.x() - mask.size() * 0.5f);
        int y = (int)Math.floor(mask.y() - mask.size() * 0.5f);
        int s = (int)Math.ceil(mask.size());
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, s), Math.max(1, s)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return SectorMaskRenderer.SECTOR_MASK_PIPELINE; }
    public ScreenRect scissorArea() { return this.scissorArea; }
    public TextureSetup textureSetup() { return TextureSetup.empty(); }

    public void setupVertices(VertexConsumer vertices) {
        int id = SectorMaskRenderer.getInstance().reserve(this.mask);
        if (id < 0) return;
        float x1 = this.mask.x() - this.mask.size() * 0.5f;
        float y1 = this.mask.y() - this.mask.size() * 0.5f;
        float x2 = this.mask.x() + this.mask.size() * 0.5f;
        float y2 = this.mask.y() + this.mask.size() * 0.5f;
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc)(Object)this.pose, x, y).color(u, v, 255, 255).lineWidth((float)(id + 1));
    }
}