package rtx.nv.utils.render.render2d.font;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.font.BuiltText;
import rtx.nv.utils.render.render2d.font.FontManager;
import rtx.nv.utils.render.render2d.font.FontStrike;
import rtx.nv.utils.render.render2d.font.LayoutGlyph;
import rtx.nv.utils.render.render2d.font.TextLayout;
import rtx.nv.utils.render.render2d.font.TextLayout.Page;
import rtx.nv.utils.render.render2d.font.TextRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class TextRenderer
implements AutoCloseable {
    private static volatile TextRenderer instance;
    public static final RenderPipeline TEXT_PIPELINE;
    public static final RenderPipeline TEXT_FADE_PIPELINE;
    private final FontManager fontManager = new FontManager();
    private final Map<TextRenderer.FrameBatchKey, TextRenderState> frameBatches = new LinkedHashMap<TextRenderer.FrameBatchKey, TextRenderState>(64);
    private DrawContext activeGraphics;

    private TextRenderer() {
    }

    static {
        TEXT_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(TextRenderer.id("pipeline/text_atlas")).withVertexShader(TextRenderer.id("core/text_atlas")).withFragmentShader(TextRenderer.id("core/text_atlas")).withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).build();
        TEXT_FADE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(TextRenderer.id("pipeline/text_atlas_fade")).withVertexShader(TextRenderer.id("core/text_atlas_fade")).withFragmentShader(TextRenderer.id("core/text_atlas_fade")).withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
        this.frameBatches.clear();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static TextRenderer getInstance() {
        TextRenderer textRenderer = instance;
        if (textRenderer != null) return textRenderer;
        Class<TextRenderer> clazz = TextRenderer.class;
        synchronized (TextRenderer.class) {
            textRenderer = instance;
            if (textRenderer != null) return textRenderer;
            instance = textRenderer = new TextRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return textRenderer;
        }
    }

    @Override
    public void close() {
        this.activeGraphics = null;
        this.frameBatches.clear();
        this.fontManager.close();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltText builtText) {
        this.submit(this.activeGraphics, builtText);
    }

    private void submit(DrawContext drawContext, BuiltText builtText) {
        BuiltText builtText2;
        if (drawContext == null || builtText == null) {
            return;
        }
        BuiltText builtText3 = builtText2 = this.needsColorNormalization(builtText) ? this.normalize(builtText) : builtText;
        if (!builtText2.visible()) {
            return;
        }
        try {
            FontStrike fontStrike = this.fontManager.strike(builtText2.fontName(), builtText2.size());
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            TextLayout textLayout = fontStrike.layout(builtText2.text());
            if (textLayout.empty()) {
                return;
            }
            fontStrike.uploadDirtyPages();
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            TextRenderer.PoseKey poseKey = TextRenderer.PoseKey.of(matrix3x2f);
            ScreenRect screenRect = ScissorUtil.current();
            RenderPipeline renderPipeline = builtText2.hasHorizontalFade() ? TEXT_FADE_PIPELINE : TEXT_PIPELINE;
            for (TextLayout.Page page : textLayout.pages()) {
                if (page.glyphs().isEmpty()) continue;
                TextRenderer.FrameBatchKey frameBatchKey = new TextRenderer.FrameBatchKey(guiRenderState, n, page.page(), poseKey, screenRect, renderPipeline);
                TextRenderState textRenderState = this.frameBatches.get(frameBatchKey);
                if (textRenderState == null) {
                    textRenderState = new TextRenderState(matrix3x2f, page.page(), screenRect, renderPipeline);
                    this.frameBatches.put(frameBatchKey, textRenderState);
                    guiRenderState.addPreparedTextElement((SimpleGuiElementRenderState)textRenderState);
                }
                textRenderState.add(page.glyphs(), builtText2.x(), builtText2.y(), builtText2.colorTopLeft(), builtText2.colorTopRight(), builtText2.colorBottomRight(), builtText2.colorBottomLeft(), builtText2.rotationDegrees(), builtText2.rotationOriginX(), builtText2.rotationOriginY(), builtText2.fadeLeft(), builtText2.fadeRight(), builtText2.fadeLeftX(), builtText2.fadeRightX(), builtText2.fadeWidth(), builtText2.fadeLeftStrength(), builtText2.fadeRightStrength(), builtText2.snapOrigin());
            }
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[FontRenderer] Failed to submit text: {}", (Object)builtText2.text(), (Object)runtimeException);
        }
    }

    private BuiltText normalize(BuiltText builtText) {
        return new BuiltText(builtText.fontName(), builtText.text(), builtText.x(), builtText.y(), builtText.size(), TextRenderer.normalizeColor(builtText.colorTopLeft()), TextRenderer.normalizeColor(builtText.colorTopRight()), TextRenderer.normalizeColor(builtText.colorBottomRight()), TextRenderer.normalizeColor(builtText.colorBottomLeft()), builtText.rotationDegrees(), builtText.rotationOriginX(), builtText.rotationOriginY(), builtText.fadeLeft(), builtText.fadeRight(), builtText.fadeLeftX(), builtText.fadeRightX(), builtText.fadeWidth(), builtText.fadeLeftStrength(), builtText.fadeRightStrength(), builtText.snapOrigin());
    }

    public float width(String string, String string2, float f) {
        if (string2 == null || string2.isEmpty()) {
            return 0.0f;
        }
        return this.fontManager.strike(string, f).width(string2);
    }

    public GlyphLayout glyphLayout(String string, String string2, float f) {
        if (string2 == null || string2.isEmpty() || f <= 0.0f) {
            return GlyphLayout.EMPTY;
        }
        try {
            FontStrike fontStrike = this.fontManager.strike(string, f);
            TextLayout textLayout = fontStrike.layout(string2);
            if (textLayout.empty()) {
                return GlyphLayout.EMPTY;
            }
            fontStrike.uploadDirtyPages();
            List<GlyphLayout.GlyphPage> arrayList = new ArrayList<>(textLayout.pages().size());
            for (TextLayout.Page page : textLayout.pages()) {
                List<GlyphLayout.Glyph> arrayList2 = new ArrayList<>(page.glyphs().size());
                for (LayoutGlyph layoutGlyph : page.glyphs()) {
                    arrayList2.add(new GlyphLayout.Glyph(layoutGlyph.x0(), layoutGlyph.y0(), layoutGlyph.x1(), layoutGlyph.y1(), layoutGlyph.u0(), layoutGlyph.v0(), layoutGlyph.u1(), layoutGlyph.v1()));
                }
                arrayList.add(new GlyphLayout.GlyphPage(page.page().textureSetup(), arrayList2));
            }
            return new GlyphLayout(arrayList, textLayout.width(), textLayout.height());
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[FontRenderer] Failed to layout glyph pattern: {}", (Object)string2, (Object)runtimeException);
            return GlyphLayout.EMPTY;
        }
    }

    private static int normalizeColor(int n) {
        if (TextRenderer.needsColorNormalization(n)) {
            return n | 0xFF000000;
        }
        return n;
    }

    private static boolean needsColorNormalization(int n) {
        return false;
    }

    private boolean needsColorNormalization(BuiltText builtText) {
        return TextRenderer.needsColorNormalization(builtText.colorTopLeft()) || TextRenderer.needsColorNormalization(builtText.colorTopRight()) || TextRenderer.needsColorNormalization(builtText.colorBottomRight()) || TextRenderer.needsColorNormalization(builtText.colorBottomLeft());
    }

    public static void closeInstance() {
        TextRenderer textRenderer = instance;
        if (textRenderer != null) {
            textRenderer.close();
            instance = null;
        }
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltText builtText) {
        this.beginFrame(drawContext);
        this.enqueue(builtText);
        this.flush();
    }


    public static record FrameBatchKey(GuiRenderState state, int layerSerial, GlyphAtlasPage page, TextRenderer.PoseKey pose, ScreenRect scissorArea, RenderPipeline pipeline) {
    }
    
        public static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
        static PoseKey of(Matrix3x2f matrix3x2f) {
            return new PoseKey(matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21());
        }
    }
}

