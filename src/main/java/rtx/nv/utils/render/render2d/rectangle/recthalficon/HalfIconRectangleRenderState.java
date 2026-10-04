package rtx.nv.utils.render.render2d.rectangle.recthalficon;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;

final class HalfIconRectangleRenderState implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final ScreenRect bounds;
    private final List<IconQuad> iconQuads = new ArrayList<>();

    HalfIconRectangleRenderState(Matrix3x2f pose, TextureSetup textureSetup) {
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.textureSetup = textureSetup != null ? textureSetup : TextureSetup.empty();
        this.bounds = new ScreenRect(0, 0, 10000, 10000);
    }

    public void add(BuiltHalfIconRectangle rect, List<IconQuad> quads) {
        this.iconQuads.addAll(quads);
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return HalfIconRectangleRenderer.HALF_ICON_RECTANGLE_PIPELINE; }
    public ScreenRect scissorArea() { return null; }
    public TextureSetup textureSetup() { return this.textureSetup; }

    public void setupVertices(VertexConsumer vertices) {
        for (IconQuad q : this.iconQuads) {
            int c = q.color();
            int a = (c >>> 24) & 0xFF;
            int r = (c >>> 16) & 0xFF;
            int g = (c >>> 8) & 0xFF;
            int b = c & 0xFF;
            vertices.vertex((Matrix3x2fc)(Object)this.pose, q.x1(), q.y1()).texture(q.u1(), q.v1()).color(r, g, b, a).lineWidth(1.0f);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, q.x2(), q.y2()).texture(q.u2(), q.v2()).color(r, g, b, a).lineWidth(1.0f);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, q.x3(), q.y3()).texture(q.u3(), q.v3()).color(r, g, b, a).lineWidth(1.0f);
            vertices.vertex((Matrix3x2fc)(Object)this.pose, q.x4(), q.y4()).texture(q.u4(), q.v4()).color(r, g, b, a).lineWidth(1.0f);
        }
    }

    public static record IconQuad(
        float x1, float y1, float u1, float v1,
        float x2, float y2, float u2, float v2,
        float x3, float y3, float u3, float v3,
        float x4, float y4, float u4, float v4,
        int color
    ) {}
}