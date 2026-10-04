package rtx.nv.utils.render.render2d.msdf;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.others.RoundedScissor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.msdf.BuiltMsdfText;
import rtx.nv.utils.render.render2d.msdf.MsdfFont;
import rtx.nv.utils.render.render2d.msdf.MsdfFonts;
import rtx.nv.utils.render.render2d.msdf.MsdfGlyph;
import rtx.nv.utils.render.render2d.msdf.MsdfTextRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class MsdfTextRenderer
implements AutoCloseable {
    private static volatile MsdfTextRenderer instance;
    private static final VertexFormat MSDF_SHIMMER_VERTEX_FORMAT;
    public static final RenderPipeline MSDF_PIPELINE;
    public static final RenderPipeline MSDF_SHIMMER_PIPELINE;
    public static final RenderPipeline MSDF_WAVE_PIPELINE;
    private final Map<MsdfTextRenderer.FrameBatchKey, MsdfTextRenderState> frameBatches = new LinkedHashMap<MsdfTextRenderer.FrameBatchKey, MsdfTextRenderState>(32);
    private final Map<Identifier, MsdfTextRenderer.CachedTexture> textures = new HashMap<Identifier, MsdfTextRenderer.CachedTexture>();
    private DrawContext activeGraphics;

    private MsdfTextRenderer() {
    }

    static {
        MSDF_SHIMMER_VERTEX_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).add("LineWidth", VertexFormatElement.LINE_WIDTH).build();
        MSDF_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(MsdfTextRenderer.id("pipeline/msdf_text")).withVertexShader(MsdfTextRenderer.id("core/msdf_text")).withFragmentShader(MsdfTextRenderer.id("core/msdf_text")).withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withSampler("Sampler0").build();
        MSDF_SHIMMER_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(MsdfTextRenderer.id("pipeline/msdf_shimmer")).withVertexShader(MsdfTextRenderer.id("core/msdf_shimmer")).withFragmentShader(MsdfTextRenderer.id("core/msdf_shimmer")).withVertexFormat(MSDF_SHIMMER_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withSampler("Sampler0").build();
        MSDF_WAVE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(MsdfTextRenderer.id("pipeline/msdf_wave")).withVertexShader(MsdfTextRenderer.id("core/msdf_wave")).withFragmentShader(MsdfTextRenderer.id("core/msdf_wave")).withVertexFormat(MSDF_SHIMMER_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withSampler("Sampler0").build();
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
    public static MsdfTextRenderer getInstance() {
        MsdfTextRenderer msdfTextRenderer = instance;
        if (msdfTextRenderer != null) return msdfTextRenderer;
        Class<MsdfTextRenderer> clazz = MsdfTextRenderer.class;
        synchronized (MsdfTextRenderer.class) {
            msdfTextRenderer = instance;
            if (msdfTextRenderer != null) return msdfTextRenderer;
            instance = msdfTextRenderer = new MsdfTextRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return msdfTextRenderer;
        }
    }

    @Override
    public void close() {
        this.clearCaches();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltMsdfText builtMsdfText) {
        this.submit(this.activeGraphics, builtMsdfText, false, 0.0f, 0.0f, 0.0f, 0.0f, false, 0.0f);
    }

    private void submit(DrawContext drawContext, BuiltMsdfText builtMsdfText, boolean bl, float f, float f2, float f3, float f4, boolean bl2, float f5) {
        if (drawContext == null || builtMsdfText == null || !builtMsdfText.visible()) {
            return;
        }
        MsdfFont msdfFont = MsdfFonts.get(builtMsdfText.fontName());
        if (msdfFont == null) {
            return;
        }
        TextureSetup textureSetup = this.resolveTexture(msdfFont.atlasTexture());
        if (textureSetup == null) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            ScreenRect screenRect = ScissorUtil.current();
            int n2 = bl ? MsdfTextRenderer.packShimmerParams(f, f2, f3, f4) : 0;
            MsdfTextRenderer.FrameBatchKey frameBatchKey = new MsdfTextRenderer.FrameBatchKey(guiRenderState, n, msdfFont.atlasTexture(), MsdfTextRenderer.PoseKey.of(matrix3x2f), screenRect, bl, n2, bl2, f5);
            MsdfTextRenderState msdfTextRenderState = this.frameBatches.get(frameBatchKey);
            if (msdfTextRenderState == null) {
                msdfTextRenderState = new MsdfTextRenderState(matrix3x2f, textureSetup, screenRect, bl, bl2);
                this.frameBatches.put(frameBatchKey, msdfTextRenderState);
                guiRenderState.addPreparedTextElement((SimpleGuiElementRenderState)msdfTextRenderState);
            }
            this.layoutInto(msdfTextRenderState, msdfFont, builtMsdfText, bl, n2, bl2, f5, RoundedScissor.localClipFor(matrix3x2f));
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[MSDF] Failed to submit text: {}", (Object)builtMsdfText.text(), (Object)runtimeException);
        }
    }

    public float width(String string, String string2, float f) {
        MsdfFont msdfFont = MsdfFonts.get(string);
        return msdfFont == null ? 0.0f : msdfFont.width(string2, f);
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    public void enqueueWave(BuiltMsdfText builtMsdfText, float f) {
        this.submit(this.activeGraphics, builtMsdfText, false, 0.0f, 0.0f, 0.0f, 0.0f, true, f);
    }

    public float[] glyphBounds(String string, String string2, float f) {
        float[] fArray;
        MsdfFont msdfFont = MsdfFonts.get(string);
        if (msdfFont == null) {
            float[] fArray2 = new float[2];
            fArray2[0] = 0.0f;
            fArray = fArray2;
            fArray2[1] = 0.0f;
        } else {
            fArray = msdfFont.glyphBounds(string2, f);
        }
        return fArray;
    }

    public static void closeInstance() {
        MsdfTextRenderer msdfTextRenderer = instance;
        if (msdfTextRenderer != null) {
            msdfTextRenderer.close();
            instance = null;
        }
    }

    public static void clearResourceCaches() {
        MsdfTextRenderer msdfTextRenderer = instance;
        if (msdfTextRenderer != null) {
            msdfTextRenderer.clearCaches();
            return;
        }
        MsdfFonts.clear();
    }

    public void enqueueShimmer(BuiltMsdfText builtMsdfText, float f, float f2, float f3, float f4) {
        this.submit(this.activeGraphics, builtMsdfText, true, f, f2, f3, f4, false, 0.0f);
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    private static int toByte(float f) {
        return Math.round(MsdfTextRenderer.clamp01(f) * 255.0f);
    }

    private static float packLineColor(float f, int n) {
        int n2 = Math.round(MsdfTextRenderer.clamp01(f) * 63.0f);
        int n3 = Math.round((float)(n >>> 16 & 0xFF) / 255.0f * 63.0f);
        int n4 = Math.round((float)(n >>> 8 & 0xFF) / 255.0f * 63.0f);
        int n5 = Math.round((float)(n & 0xFF) / 255.0f * 63.0f);
        return n2 + n5 * 64 + n4 * 4096 + n3 * 262144;
    }

    private void clearCaches() {
        this.activeGraphics = null;
        this.frameBatches.clear();
        this.textures.clear();
        MsdfFonts.clear();
    }

    private static int packShimmerParams(float f, float f2, float f3, float f4) {
        int n = MsdfTextRenderer.toByte(f4);
        int n2 = MsdfTextRenderer.toByte((f + 0.5f) / 2.0f);
        int n3 = MsdfTextRenderer.toByte(f2);
        int n4 = MsdfTextRenderer.toByte(f3);
        return n << 24 | n2 << 16 | n3 << 8 | n4;
    }

    private TextureSetup resolveTexture(Identifier identifier) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return null;
        }
        TextureManager textureManager = minecraftClient.getTextureManager();
        if (textureManager == null) {
            return null;
        }
        AbstractTexture abstractTexture = textureManager.getTexture(identifier);
        if (abstractTexture == null || abstractTexture.getGlTextureView() == null) {
            return null;
        }
        MsdfTextRenderer.CachedTexture cachedTexture = this.textures.get(identifier);
        if (cachedTexture != null && cachedTexture.texture() == abstractTexture) {
            return cachedTexture.setup();
        }
        TextureSetup textureSetup = TextureSetup.of((GpuTextureView)abstractTexture.getGlTextureView(), (GpuSampler)RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
        this.textures.put(identifier, new MsdfTextRenderer.CachedTexture(abstractTexture, textureSetup));
        return textureSetup;
    }

    private float[] measureLines(MsdfFont msdfFont, String string, float f) {
        int n = 1;
        for (int i = 0; i < string.length(); ++i) {
            if (string.charAt(i) != '\n') continue;
            ++n;
        }
        float[] fArray = new float[n];
        int n2 = 0;
        int n3 = -1;
        int n4 = 0;
        while (n4 < string.length()) {
            int n5 = string.codePointAt(n4);
            n4 += Character.charCount(n5);
            if (n5 == 10) {
                fArray[n2] = Math.max(fArray[n2], 1.0f);
                ++n2;
                n3 = -1;
                continue;
            }
            MsdfGlyph msdfGlyph = msdfFont.glyph(n5);
            if (n3 != -1) {
                int n6 = n2;
                fArray[n6] = fArray[n6] + msdfFont.kerning(n3, n5) * f;
            }
            int n7 = n2;
            fArray[n7] = fArray[n7] + msdfGlyph.advance() * f;
            n3 = n5;
        }
        for (n4 = 0; n4 < fArray.length; ++n4) {
            fArray[n4] = Math.max(fArray[n4], 1.0f);
        }
        return fArray;
    }

    private void layoutInto(MsdfTextRenderState msdfTextRenderState, MsdfFont msdfFont, BuiltMsdfText builtMsdfText, boolean bl, int n, boolean bl2, float f, float[] fArray) {
        float f2 = builtMsdfText.size();
        float f3 = builtMsdfText.x();
        float f4 = builtMsdfText.y() + msdfFont.ascent(f2);
        float f5 = msdfFont.lineHeight(f2);
        float[] fArray2 = this.measureLines(msdfFont, builtMsdfText.text(), f2);
        float f6 = f3;
        float f7 = f4;
        int n2 = 0;
        int n3 = -1;
        String string = builtMsdfText.text();
        int n4 = 0;
        while (n4 < string.length()) {
            int n5 = string.codePointAt(n4);
            n4 += Character.charCount(n5);
            if (n5 == 10) {
                f6 = f3;
                f7 += f5;
                ++n2;
                n3 = -1;
                continue;
            }
            MsdfGlyph msdfGlyph = msdfFont.glyph(n5);
            if (n3 != -1) {
                f6 += msdfFont.kerning(n3, n5) * f2;
            }
            if (msdfGlyph.drawable()) {
                int n6;
                int n7;
                int n8;
                int n9;
                float f8;
                float f9;
                float f10;
                float f11 = f6 + msdfGlyph.planeLeft() * f2;
                float f12 = f6 + msdfGlyph.planeRight() * f2;
                float f13 = f7 - msdfGlyph.planeTop() * f2;
                float f14 = f7 - msdfGlyph.planeBottom() * f2;
                float f15 = f10 = n2 < fArray2.length ? fArray2[n2] : 1.0f;
                if (bl2) {
                    f9 = f;
                    f8 = f;
                    n9 = this.fadeColor(builtMsdfText.colorTopLeft(), f11, builtMsdfText);
                    n8 = this.fadeColor(builtMsdfText.colorTopRight(), f12, builtMsdfText);
                    n7 = this.fadeColor(builtMsdfText.colorBottomRight(), f12, builtMsdfText);
                    n6 = this.fadeColor(builtMsdfText.colorBottomLeft(), f11, builtMsdfText);
                } else if (bl) {
                    int n10 = builtMsdfText.colorTopLeft();
                    f9 = MsdfTextRenderer.packLineColor(MsdfTextRenderer.clamp01((f11 - f3) / f10), n10);
                    f8 = MsdfTextRenderer.packLineColor(MsdfTextRenderer.clamp01((f12 - f3) / f10), n10);
                    n9 = n;
                    n8 = n;
                    n7 = n;
                    n6 = n;
                } else {
                    f9 = 0.0f;
                    f8 = 0.0f;
                    n9 = this.fadeColor(builtMsdfText.colorTopLeft(), f11, builtMsdfText);
                    n8 = this.fadeColor(builtMsdfText.colorTopRight(), f12, builtMsdfText);
                    n7 = this.fadeColor(builtMsdfText.colorBottomRight(), f12, builtMsdfText);
                    n6 = this.fadeColor(builtMsdfText.colorBottomLeft(), f11, builtMsdfText);
                }
                float f16 = msdfGlyph.u0();
                float f17 = msdfGlyph.v0();
                float f18 = msdfGlyph.u1();
                float f19 = msdfGlyph.v1();
                boolean bl3 = true;
                if (fArray != null) {
                    float f20 = fArray[0];
                    float f21 = fArray[1];
                    float f22 = fArray[2];
                    float f23 = fArray[3];
                    float f24 = f12 - f11;
                    float f25 = f14 - f13;
                    if (f12 <= f20 || f11 >= f22 || f14 <= f21 || f13 >= f23 || f24 <= 0.0f || f25 <= 0.0f) {
                        bl3 = false;
                    } else {
                        float f26 = Math.max(f11, f20);
                        float f27 = Math.min(f12, f22);
                        float f28 = Math.max(f13, f21);
                        float f29 = Math.min(f14, f23);
                        float f30 = f18 - f16;
                        float f31 = f19 - f17;
                        f16 += (f26 - f11) / f24 * f30;
                        f18 -= (f12 - f27) / f24 * f30;
                        f17 += (f28 - f13) / f25 * f31;
                        f19 -= (f14 - f29) / f25 * f31;
                        f11 = f26;
                        f12 = f27;
                        f13 = f28;
                        f14 = f29;
                    }
                }
                if (bl3) {
                    msdfTextRenderState.addGlyph(f11, f13, f12, f14, f16, f17, f18, f19, n9, n8, n7, n6, f9, f8, builtMsdfText.rotationDegrees(), builtMsdfText.rotationOriginX(), builtMsdfText.rotationOriginY());
                }
            }
            f6 += msdfGlyph.advance() * f2;
            n3 = n5;
        }
    }

    private int fadeColor(int n, float f, BuiltMsdfText builtMsdfText) {
        float f2;
        if (!builtMsdfText.hasHorizontalFade()) {
            return n;
        }
        float f3 = 1.0f;
        if (builtMsdfText.fadeLeftStrength() > 0.001f) {
            f2 = MsdfTextRenderer.smoothstep(MsdfTextRenderer.clamp01((f - builtMsdfText.fadeLeftX()) / builtMsdfText.fadeWidth()));
            f3 = Math.min(f3, 1.0f - builtMsdfText.fadeLeftStrength() * (1.0f - f2));
        }
        if (builtMsdfText.fadeRightStrength() > 0.001f) {
            f2 = MsdfTextRenderer.smoothstep(MsdfTextRenderer.clamp01((builtMsdfText.fadeRightX() - f) / builtMsdfText.fadeWidth()));
            f3 = Math.min(f3, 1.0f - builtMsdfText.fadeRightStrength() * (1.0f - f2));
        }
        int n2 = n >>> 24 & 0xFF;
        return Math.round((float)n2 * f3) << 24 | n & 0xFFFFFF;
    }

    private static float smoothstep(float f) {
        return f * f * (3.0f - 2.0f * f);
    }


    public static record CachedTexture(AbstractTexture texture, TextureSetup setup) {
    }
    
        public static record FrameBatchKey(GuiRenderState state, int layerSerial, Identifier atlas, MsdfTextRenderer.PoseKey pose, ScreenRect scissorArea, boolean shimmer, int shimmerParams, boolean wave, float wavePhase) {
    }
    
        public static record PoseKey(float m00, float m01, float m10, float m11, float m20, float m21) {
        static PoseKey of(Matrix3x2f matrix3x2f) {
            return new PoseKey(matrix3x2f.m00(), matrix3x2f.m01(), matrix3x2f.m10(), matrix3x2f.m11(), matrix3x2f.m20(), matrix3x2f.m21());
        }
    }
}

