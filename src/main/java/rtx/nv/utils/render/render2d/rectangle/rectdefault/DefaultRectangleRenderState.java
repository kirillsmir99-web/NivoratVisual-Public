package rtx.nv.utils.render.render2d.rectangle.rectdefault;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.BuiltRectangle;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.DefaultRectangleRenderer;

final class DefaultRectangleRenderState
implements SimpleGuiElementRenderState {
    static final float AA_PAD = 1.5f;
    private final BuiltRectangle rectangle;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;
    private int slot = -2;

    private int slot() {
        if (this.slot == -2) {
            this.slot = DefaultRectangleRenderer.getInstance().reserve(this.rectangle);
        }
        return this.slot;
    }

    DefaultRectangleRenderState(Matrix3x2f matrix3x2f, BuiltRectangle builtRectangle, ScreenRect screenRect) {
        this.rectangle = builtRectangle;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        ScreenRect screenRect2 = new ScreenRect(Math.round(builtRectangle.x() - 1.5f), Math.round(builtRectangle.y() - 1.5f), Math.round(builtRectangle.width() + 3.0f), Math.round(builtRectangle.height() + 3.0f)).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        int n = this.slot();
        return DefaultRectangleRenderer.PIPELINES[n < 0 ? 0 : DefaultRectangleRenderer.pageOf(n)];
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = this.slot();
        if (n < 0) {
            return;
        }
        int n2 = DefaultRectangleRenderer.localOf(n);
        float f = this.rectangle.x() - 1.5f;
        float f2 = this.rectangle.y() - 1.5f;
        float f3 = this.rectangle.x() + this.rectangle.width() + 1.5f;
        float f4 = this.rectangle.y() + this.rectangle.height() + 1.5f;
        this.vertex(vertices, f, f2, 0, 0, n2);
        this.vertex(vertices, f, f4, 0, 255, n2);
        this.vertex(vertices, f3, f4, 255, 255, n2);
        this.vertex(vertices, f3, f2, 255, 0, n2);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

