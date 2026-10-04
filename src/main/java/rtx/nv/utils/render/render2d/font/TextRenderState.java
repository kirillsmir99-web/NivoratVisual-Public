package rtx.nv.utils.render.render2d.font;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import rtx.nv.utils.render.render2d.font.FontQuality;
import rtx.nv.utils.render.render2d.font.GlyphAtlasPage;
import rtx.nv.utils.render.render2d.font.GlyphQuad;
import rtx.nv.utils.render.render2d.font.LayoutGlyph;

final class TextRenderState
implements SimpleGuiElementRenderState {
    private final Matrix3x2f pose;
    private final GlyphAtlasPage page;
    private final ScreenRect scissorArea;
    private final RenderPipeline pipeline;
    private final List<GlyphQuad> quads = new ArrayList<GlyphQuad>(128);
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = -3.4028235E38f;
    private float maxY = -3.4028235E38f;

    TextRenderState(Matrix3x2f matrix3x2f, GlyphAtlasPage glyphAtlasPage, ScreenRect screenRect, RenderPipeline renderPipeline) {
        this.pose = new Matrix3x2f((Matrix3x2fc)matrix3x2f);
        this.page = glyphAtlasPage;
        this.scissorArea = screenRect;
        this.pipeline = renderPipeline;
    }

    private float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    void add(List<LayoutGlyph> list, float f, float f2, int n, int n2, int n3, int n4, float f3, float f4, float f5, boolean bl, boolean bl2, float f6, float f7, float f8, float f9, float f10, boolean bl3) {
        boolean bl4;
        boolean bl5 = bl4 = Math.abs(f3) < 0.001f;
        if (bl4 && bl3) {
            f = FontQuality.snapOrigin(f);
            f2 = FontQuality.snapOrigin(f2);
        }
        float f11 = (float)Math.toRadians(f3);
        float f12 = (float)Math.sin(f11);
        float f13 = (float)Math.cos(f11);
        for (LayoutGlyph layoutGlyph : list) {
            float f14 = layoutGlyph.x0() + f;
            float f15 = layoutGlyph.y0() + f2;
            float f16 = layoutGlyph.x1() + f;
            float f17 = layoutGlyph.y1() + f2;
            float[] fArray = this.rotate(f14, f15, f4, f5, f12, f13);
            float[] fArray2 = this.rotate(f14, f17, f4, f5, f12, f13);
            float[] fArray3 = this.rotate(f16, f17, f4, f5, f12, f13);
            float[] fArray4 = this.rotate(f16, f15, f4, f5, f12, f13);
            this.quads.add(new GlyphQuad(fArray[0], fArray[1], fArray2[0], fArray2[1], fArray3[0], fArray3[1], fArray4[0], fArray4[1], layoutGlyph.u0(), layoutGlyph.v0(), layoutGlyph.u1(), layoutGlyph.v1(), this.fadeColor(n, fArray[0], bl, bl2, f6, f7, f8, f9, f10), this.fadeColor(n2, fArray4[0], bl, bl2, f6, f7, f8, f9, f10), this.fadeColor(n3, fArray3[0], bl, bl2, f6, f7, f8, f9, f10), this.fadeColor(n4, fArray2[0], bl, bl2, f6, f7, f8, f9, f10)));
            this.include(fArray[0], fArray[1]);
            this.include(fArray2[0], fArray2[1]);
            this.include(fArray3[0], fArray3[1]);
            this.include(fArray4[0], fArray4[1]);
        }
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
        return this.pipeline;
    }

    public ScreenRect scissorArea() {
        return this.scissorArea;
    }

    public TextureSetup textureSetup() {
        return this.page.textureSetup();
    }

    public void setupVertices(VertexConsumer vertices) {
        for (GlyphQuad glyphQuad : this.quads) {
            this.vertex(vertices, glyphQuad.x0(), glyphQuad.y0(), glyphQuad.u0(), glyphQuad.v0(), glyphQuad.colorTopLeft());
            this.vertex(vertices, glyphQuad.x1(), glyphQuad.y1(), glyphQuad.u0(), glyphQuad.v1(), glyphQuad.colorBottomLeft());
            this.vertex(vertices, glyphQuad.x2(), glyphQuad.y2(), glyphQuad.u1(), glyphQuad.v1(), glyphQuad.colorBottomRight());
            this.vertex(vertices, glyphQuad.x3(), glyphQuad.y3(), glyphQuad.u1(), glyphQuad.v0(), glyphQuad.colorTopRight());
        }
    }

    private void vertex(VertexConsumer vertexConsumer, float f, float f2, float f3, float f4, int n) {
        vertexConsumer.vertex((Matrix3x2fc)(Object)this.pose, f, f2).texture(f3, f4).color(n);
    }

    private int fadeColor(int n, float f, boolean bl, boolean bl2, float f2, float f3, float f4, float f5, float f6) {
        float f7;
        if (!bl && !bl2 || f4 <= 0.0f || f3 <= f2 || f5 <= 0.001f && f6 <= 0.001f) {
            return n;
        }
        float f8 = 1.0f;
        if (bl) {
            f7 = this.smoothstep(this.clamp((f - f2) / f4, 0.0f, 1.0f));
            f8 = Math.min(f8, 1.0f - this.clamp(f5, 0.0f, 1.0f) * (1.0f - f7));
        }
        if (bl2) {
            f7 = this.smoothstep(this.clamp((f3 - f) / f4, 0.0f, 1.0f));
            f8 = Math.min(f8, 1.0f - this.clamp(f6, 0.0f, 1.0f) * (1.0f - f7));
        }
        int n2 = n >>> 24 & 0xFF;
        return Math.round((float)n2 * f8) << 24 | n & 0xFFFFFF;
    }

    private float smoothstep(float f) {
        return f * f * (3.0f - 2.0f * f);
    }
}

