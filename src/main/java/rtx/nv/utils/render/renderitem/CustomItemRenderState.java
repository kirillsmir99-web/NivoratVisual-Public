package rtx.nv.utils.render.renderitem;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class CustomItemRenderState implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final ItemTexture texture;
    private final ScreenRect bounds;
    private final List<QuadInstance> quads = new ArrayList<>();

    public CustomItemRenderState(Matrix3x2f pose, ItemTexture texture) {
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.texture = texture;
        this.bounds = new ScreenRect(0, 0, 10000, 10000);
    }

    public void add(CachedItemQuad quad, float x, float y, float size, int color, float glintAlpha) {
        this.quads.add(new QuadInstance(quad, x, y, size, color, glintAlpha));
    }

    @Override
    public ScreenRect bounds() { return this.bounds; }
    @Override
    public RenderPipeline pipeline() { return CustomItemRenderer.ITEM_PIPELINE; }
    @Override
    public ScreenRect scissorArea() { return null; }
    @Override
    public TextureSetup textureSetup() { return this.texture != null ? this.texture.textureSetup() : TextureSetup.empty(); }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        for (QuadInstance qi : this.quads) {
            CachedItemQuad q = qi.quad();
            float x = qi.x();
            float y = qi.y();
            float s = qi.size();
            int c = qi.color();
            int a = (c >>> 24) & 0xFF;
            int r = (c >>> 16) & 0xFF;
            int g = (c >>> 8) & 0xFF;
            int b = c & 0xFF;
            float lineW = qi.glintAlpha();
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x + q.x0() * s, y + q.y0() * s).texture(q.u0(), q.v0()).color(r, g, b, a).lineWidth(lineW);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x + q.x1() * s, y + q.y1() * s).texture(q.u1(), q.v1()).color(r, g, b, a).lineWidth(lineW);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x + q.x2() * s, y + q.y2() * s).texture(q.u2(), q.v2()).color(r, g, b, a).lineWidth(lineW);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x + q.x3() * s, y + q.y3() * s).texture(q.u3(), q.v3()).color(r, g, b, a).lineWidth(lineW);
        }
    }

    public record QuadInstance(CachedItemQuad quad, float x, float y, float size, int color, float glintAlpha) {}
}
