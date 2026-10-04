package rtx.nv.utils.render.render2d.msdf;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.msdf.MsdfTextRenderer;

final class MsdfTextRenderState
implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final TextureSetup textureSetup;
    private final ScreenRect scissorArea;
    private final boolean shimmer;
    private final boolean wave;
    private final List<MsdfTextRenderState.Quad> quads = new ArrayList<MsdfTextRenderState.Quad>(128);
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -3.4028235E38f;
    private float maxY = -3.4028235E38f;

    MsdfTextRenderState(Matrix3x2f matrix3x2f, TextureSetup textureSetup, ScreenRect screenRect, boolean bl, boolean bl2) {
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.textureSetup = textureSetup;
        this.scissorArea = screenRect;
        this.shimmer = bl;
        this.wave = bl2;
    }

    private float[] rotate(float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f - f3;
        float f8 = f2 - f4;
        return new float[]{f3 + f7 * f6 - f8 * f5, f4 + f7 * f5 + f8 * f6};
    }

    private void include(float f, float f2) {
        this.minX = Math.min(this.minX, f);
        this.minY = Math.min(this.minY, f2);
        this.maxX = Math.max(this.maxX, f);
        this.maxY = Math.max(this.maxY, f2);
    }

    public ScreenRect bounds() {
        if (this.quads.isEmpty()) {
            return new ScreenRect(0, 0, 1, 1);
        }
        int n = (int)Math.floor(this.minX);
        int n2 = (int)Math.floor(this.minY);
        int n3 = Math.max(1, (int)Math.ceil(this.maxX - this.minX));
        int n4 = Math.max(1, (int)Math.ceil(this.maxY - this.minY));
        ScreenRect screenRect = new ScreenRect(n, n2, n3, n4).transformEachVertex((Matrix3x2fc)(Object)this.pose);
        return this.scissorArea == null ? screenRect : this.scissorArea.intersection(screenRect);
    }

    public RenderPipeline pipeline() {
        if (this.wave) {
            return MsdfTextRenderer.MSDF_WAVE_PIPELINE;
        }
        return this.shimmer ? MsdfTextRenderer.MSDF_SHIMMER_PIPELINE : MsdfTextRenderer.MSDF_PIPELINE;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.textureSetup;
    }

    public void setupVertices(VertexConsumer vertices) {
        if (this.shimmer || this.wave) {
            for (MsdfTextRenderState.Quad quad : this.quads) {
                this.shimmerVertex(vertices, quad.x0(), quad.y0(), quad.u0(), quad.v0(), quad.colorTopLeft(), quad.l0());
                this.shimmerVertex(vertices, quad.x1(), quad.y1(), quad.u0(), quad.v1(), quad.colorBottomLeft(), quad.l0());
                this.shimmerVertex(vertices, quad.x2(), quad.y2(), quad.u1(), quad.v1(), quad.colorBottomRight(), quad.l1());
                this.shimmerVertex(vertices, quad.x3(), quad.y3(), quad.u1(), quad.v0(), quad.colorTopRight(), quad.l1());
            }
        } else {
            for (MsdfTextRenderState.Quad quad : this.quads) {
                this.vertex(vertices, quad.x0(), quad.y0(), quad.u0(), quad.v0(), quad.colorTopLeft());
                this.vertex(vertices, quad.x1(), quad.y1(), quad.u0(), quad.v1(), quad.colorBottomLeft());
                this.vertex(vertices, quad.x2(), quad.y2(), quad.u1(), quad.v1(), quad.colorBottomRight());
                this.vertex(vertices, quad.x3(), quad.y3(), quad.u1(), quad.v0(), quad.colorTopRight());
            }
        }
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).color(n);
    }

    void addGlyph(float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8, int n, int n2, int n3, int n4, float f9, float f10, float f11, float f12, float f13) {
        float f14 = f;
        float f15 = f2;
        float f16 = f;
        float f17 = f4;
        float f18 = f3;
        float f19 = f4;
        float f20 = f3;
        float f21 = f2;
        if (Math.abs(f11) >= 0.001f) {
            float f22 = (float)Math.toRadians(f11);
            float f23 = (float)Math.sin(f22);
            float f24 = (float)Math.cos(f22);
            float[] fArray = this.rotate(f14, f15, f12, f13, f23, f24);
            float[] fArray2 = this.rotate(f16, f17, f12, f13, f23, f24);
            float[] fArray3 = this.rotate(f18, f19, f12, f13, f23, f24);
            float[] fArray4 = this.rotate(f20, f21, f12, f13, f23, f24);
            f14 = fArray[0];
            f15 = fArray[1];
            f16 = fArray2[0];
            f17 = fArray2[1];
            f18 = fArray3[0];
            f19 = fArray3[1];
            f20 = fArray4[0];
            f21 = fArray4[1];
        }
        this.quads.add(new MsdfTextRenderState.Quad(f14, f15, f16, f17, f18, f19, f20, f21, f5, f6, f7, f8, n, n2, n3, n4, f9, f10));
        this.include(f14, f15);
        this.include(f16, f17);
        this.include(f18, f19);
        this.include(f20, f21);
    }

    private void shimmerVertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n, float f5) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).color(n).lineWidth(f5);
    }


    public static record Quad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, float u0, float v0, float u1, float v1, int colorTopLeft, int colorTopRight, int colorBottomRight, int colorBottomLeft, float l0, float l1) {
    }
}

