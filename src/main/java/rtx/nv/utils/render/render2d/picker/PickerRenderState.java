package rtx.nv.utils.render.render2d.picker;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.picker.BuiltPicker;
import rtx.nv.utils.render.render2d.picker.PickerRenderer;

final class PickerRenderState
implements SimpleGuiElementRenderState {
    private final BuiltPicker picker;
    private final Matrix3x2f pose;
    private final ScreenRect scissorArea;
    private final ScreenRect bounds;

    PickerRenderState(Matrix3x2f matrix3x2f, BuiltPicker builtPicker, ScreenRect screenRect) {
        this.picker = builtPicker;
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.scissorArea = screenRect;
        int n = (int)Math.floor(builtPicker.x());
        int n2 = (int)Math.floor(builtPicker.y());
        int n3 = (int)Math.ceil(builtPicker.w());
        int n4 = (int)Math.ceil(builtPicker.h());
        ScreenRect screenRect2 = new ScreenRect(n, n2, n3, n4).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        this.bounds = screenRect == null ? screenRect2 : screenRect.intersection(screenRect2);
    }

    public ScreenRect bounds() {
        return this.bounds;
    }

    public RenderPipeline pipeline() {
        return PickerRenderer.PICKER_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }

    public void setupVertices(VertexConsumer vertices) {
        int n = PickerRenderer.getInstance().reserve(this.picker);
        if (n < 0) {
            return;
        }
        float f = this.picker.x();
        float f2 = this.picker.y();
        float f3 = this.picker.x() + this.picker.w();
        float f4 = this.picker.y() + this.picker.h();
        this.vertex(vertices, f, f2, 0, 0, n);
        this.vertex(vertices, f, f4, 0, 255, n);
        this.vertex(vertices, f3, f4, 255, 255, n);
        this.vertex(vertices, f3, f2, 255, 0, n);
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, int n, int n2, int n3) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).color(n, n2, 255, 255).lineWidth((float)(n3 + 1));
    }
}

