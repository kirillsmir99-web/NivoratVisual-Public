package rtx.nv.utils.render.render2d.radialglass;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.blur.BlurCapture;

final class RadialGlassRenderState implements SimpleGuiElementRenderState {
    private final BuiltRadialGlass glass;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private final BlurCapture blurCapture;

    RadialGlassRenderState(Matrix3x2f pose, BuiltRadialGlass glass, ScreenRect scissor, BlurCapture blurCapture) {
        this.glass = glass;
        this.pose = new Matrix3x2f((Matrix3x2fc)pose);
        this.scissorArea = scissor;
        this.blurCapture = blurCapture;
        int x = (int)Math.floor(glass.centerX() - glass.outerRadius());
        int y = (int)Math.floor(glass.centerY() - glass.outerRadius());
        int s = (int)Math.ceil(glass.outerRadius() * 2.0f);
        ScreenRect transformed = new ScreenRect(x, y, Math.max(1, s), Math.max(1, s)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = scissor == null ? transformed : scissor.intersection(transformed);
    }

    RadialGlassRenderState(Matrix3x2f pose, BuiltRadialGlass glass, ScreenRect scissor) {
        this(pose, glass, scissor, null);
    }

    public ScreenRect bounds() { return this.bounds; }
    public RenderPipeline pipeline() { return RadialGlassRenderer.RADIAL_GLASS_PIPELINE; }
    public ScreenRect scissorArea() { return this.scissorArea; }
    public TextureSetup textureSetup() { return TextureSetup.empty(); }

    public void setupVertices(VertexConsumer vertices) {
        int id = RadialGlassRenderer.getInstance().reserve(this.glass, this.blurCapture);
        if (id < 0) return;
        float x1 = this.glass.centerX() - this.glass.outerRadius();
        float y1 = this.glass.centerY() - this.glass.outerRadius();
        float x2 = this.glass.centerX() + this.glass.outerRadius();
        float y2 = this.glass.centerY() + this.glass.outerRadius();
        this.vertex(vertices, x1, y1, 0, 0, id);
        this.vertex(vertices, x1, y2, 0, 255, id);
        this.vertex(vertices, x2, y2, 255, 255, id);
        this.vertex(vertices, x2, y1, 255, 0, id);
    }

    private void vertex(VertexConsumer vc, float x, float y, int u, int v, int id) {
        vc.vertex((Matrix3x2fc)(Object)this.pose, x, y).color(u, v, 255, 255).lineWidth((float)(id + 1));
    }
}