package rtx.nv.utils.render.render2d.effecticon;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

public final class EffectIconRenderState implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final EffectIconTexture texture;
    private final List<EffectIconQuad> quads = new ArrayList<>();
    private final ScreenRect bounds;

    public EffectIconRenderState(Matrix3x2f pose, EffectIconTexture texture) {
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.texture = texture;
        this.bounds = new ScreenRect(0, 0, 10000, 10000);
    }

    public void add(EffectIconQuad quad) {
        this.quads.add(quad);
    }

    @Override
    public ScreenRect bounds() { return this.bounds; }
    @Override
    public RenderPipeline pipeline() { return EffectIconRenderer.EFFECT_ICON_PIPELINE; }
    @Override
    public ScreenRect scissorArea() { return null; }
    @Override
    public TextureSetup textureSetup() { return this.texture != null ? this.texture.textureSetup() : TextureSetup.empty(); }

    @Override
    public void setupVertices(VertexConsumer vertices) {
        for (EffectIconQuad q : this.quads) {
            float x1 = q.x();
            float y1 = q.y();
            float x2 = q.x() + q.size();
            float y2 = q.y() + q.size();
            int color = q.color();
            int a = (color >>> 24) & 0xFF;
            int r = (color >>> 16) & 0xFF;
            int g = (color >>> 8) & 0xFF;
            int b = color & 0xFF;
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x1, y1).texture(q.u1(), q.v1()).color(r, g, b, a);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x1, y2).texture(q.u1(), q.v2()).color(r, g, b, a);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x2, y2).texture(q.u2(), q.v2()).color(r, g, b, a);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, x2, y1).texture(q.u2(), q.v1()).color(r, g, b, a);
        }
    }
}