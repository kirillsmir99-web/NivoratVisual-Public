package rtx.nv.utils.render.render2d.outline.outlinedefault;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.outline.outlinedefault.BuiltOutline;
import rtx.nv.utils.render.render2d.outline.outlinedefault.DefaultOutlineRenderer;

final class DefaultOutlineRenderState
implements SimpleGuiElementRenderState {
    static final float AA_PAD = 1.5f;
    private final BuiltOutline outline;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    DefaultOutlineRenderState(Matrix3x2f matrix3x2f, BuiltOutline builtOutline, ScreenRect screenRect) {
        this.outline = builtOutline;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        ScreenRect screenRect2 = new ScreenRect(Math.round(builtOutline.x() - 1.5f), Math.round(builtOutline.y() - 1.5f), Math.round(builtOutline.width() + 3.0f), Math.round(builtOutline.height() + 3.0f)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return DefaultOutlineRenderer.OUTLINE_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = DefaultOutlineRenderer.getInstance().reserve(this.outline);
        if (n < 0) {
            return;
        }
        float f = 1.5f;
        float f2 = this.outline.x() - f;
        float f3 = this.outline.y() - f;
        float f4 = this.outline.x() + this.outline.width() + f;
        float f5 = this.outline.y() + this.outline.height() + f;
        this.vertex(vertices, f2, f3, 0, 0, n);
        this.vertex(vertices, f2, f5, 0, 255, n);
        this.vertex(vertices, f4, f5, 255, 255, n);
        this.vertex(vertices, f4, f3, 255, 0, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

