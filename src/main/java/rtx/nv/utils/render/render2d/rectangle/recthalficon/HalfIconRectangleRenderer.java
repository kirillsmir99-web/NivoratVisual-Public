package rtx.nv.utils.render.render2d.rectangle.recthalficon;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.effecticon.PoseKey;
import rtx.nv.utils.render.render2d.font.GlyphLayout;
import rtx.nv.utils.render.render2d.font.TextRenderer;
import rtx.nv.utils.render.render2d.rectangle.recthalficon.BuiltHalfIconRectangle;
import rtx.nv.utils.render.render2d.rectangle.recthalficon.HalfIconRectangleRenderState;

public final class HalfIconRectangleRenderer
implements AutoCloseable {
    private static final int MAX_RECTANGLES = 256;
    private static final int PARAMS_PER_RECTANGLE = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 12288;
    private static final int MAX_ICON_COLUMNS = 96;
    private static final int MAX_ICON_ROWS = 96;
    private static final VertexFormat POSITION_TEX_COLOR_LINE_WIDTH = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).add("LineWidth", VertexFormatElement.LINE_WIDTH).build();
    private static volatile HalfIconRectangleRenderer instance;
    public static final RenderPipeline HALF_ICON_RECTANGLE_PIPELINE;
    private final List<BuiltHalfIconRectangle> preparedRectangles = new ArrayList<BuiltHalfIconRectangle>(32);
    private final Map<FrameBatchKey, HalfIconRectangleRenderState> frameBatches = new LinkedHashMap<FrameBatchKey, HalfIconRectangleRenderState>(32);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private HalfIconRectangleRenderer() {
    }

    static {
        HALF_ICON_RECTANGLE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(HalfIconRectangleRenderer.id("pipeline/rect_half_icon")).withVertexShader(HalfIconRectangleRenderer.id("core/rect_half_icon")).withFragmentShader(HalfIconRectangleRenderer.id("core/rect_half_icon")).withVertexFormat(POSITION_TEX_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("RectHalfIconParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
        this.frameBatches.clear();
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static HalfIconRectangleRenderer getInstance() {
        HalfIconRectangleRenderer halfIconRectangleRenderer = instance;
        if (halfIconRectangleRenderer != null) return halfIconRectangleRenderer;
        Class<HalfIconRectangleRenderer> clazz = HalfIconRectangleRenderer.class;
        synchronized (HalfIconRectangleRenderer.class) {
            halfIconRectangleRenderer = instance;
            if (halfIconRectangleRenderer != null) return halfIconRectangleRenderer;
            instance = halfIconRectangleRenderer = new HalfIconRectangleRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return halfIconRectangleRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedRectangles.clear();
        this.frameBatches.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltHalfIconRectangle builtHalfIconRectangle) {
        this.submit(this.activeGraphics, builtHalfIconRectangle);
    }

    private void submit(DrawContext drawContext, BuiltHalfIconRectangle builtHalfIconRectangle) {
        if (drawContext == null || builtHalfIconRectangle == null || !builtHalfIconRectangle.iconsVisible()) {
            return;
        }
        try {
            BuiltHalfIconRectangle builtHalfIconRectangle2 = this.normalize(builtHalfIconRectangle);
            GlyphLayout glyphLayout = TextRenderer.getInstance().glyphLayout(builtHalfIconRectangle2.fontName(), builtHalfIconRectangle2.icon(), builtHalfIconRectangle2.iconSize());
            if (glyphLayout.empty()) {
                return;
            }
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            PoseKey poseKey = PoseKey.of((Matrix3x2f)matrix3x2f);
            for (GlyphLayout.GlyphPage glyphPage : glyphLayout.pages()) {
                List<HalfIconRectangleRenderState.IconQuad> list;
                if (glyphPage.glyphs().isEmpty() || (list = this.buildIconQuads(builtHalfIconRectangle2, glyphLayout, glyphPage)).isEmpty()) continue;
                FrameBatchKey frameBatchKey = new FrameBatchKey(guiRenderState, n, glyphPage.textureSetup(), poseKey);
                HalfIconRectangleRenderState halfIconRectangleRenderState = this.frameBatches.get(frameBatchKey);
                if (halfIconRectangleRenderState == null) {
                    halfIconRectangleRenderState = new HalfIconRectangleRenderState(matrix3x2f, glyphPage.textureSetup());
                    halfIconRectangleRenderState.add(builtHalfIconRectangle2, list);
                    this.frameBatches.put(frameBatchKey, halfIconRectangleRenderState);
                    guiRenderState.addSimpleElement((SimpleGuiElementRenderState)halfIconRectangleRenderState);
                    continue;
                }
                halfIconRectangleRenderState.add(builtHalfIconRectangle2, list);
            }
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[HalfIconRectangle] Failed to submit icon rectangle", (Throwable)runtimeException);
        }
    }

    private static RotatedPoint rotate(float f, float f2, float f3, float f4, float f5, float f6) {
        float f7 = f - f3;
        float f8 = f2 - f4;
        return new RotatedPoint(f3 + f7 * f6 - f8 * f5, f4 + f7 * f5 + f8 * f6);
    }

    private BuiltHalfIconRectangle normalize(BuiltHalfIconRectangle builtHalfIconRectangle) {
        float f = Math.max(0.0f, Math.min(builtHalfIconRectangle.width(), builtHalfIconRectangle.height()) * 0.5f);
        float f2 = Math.max(builtHalfIconRectangle.iconSize(), 1.0f);
        return new BuiltHalfIconRectangle(builtHalfIconRectangle.x(), builtHalfIconRectangle.y(), builtHalfIconRectangle.width(), builtHalfIconRectangle.height(), HalfIconRectangleRenderer.clamp(builtHalfIconRectangle.radiusTopLeft(), 0.0f, f), HalfIconRectangleRenderer.clamp(builtHalfIconRectangle.radiusTopRight(), 0.0f, f), HalfIconRectangleRenderer.clamp(builtHalfIconRectangle.radiusBottomRight(), 0.0f, f), HalfIconRectangleRenderer.clamp(builtHalfIconRectangle.radiusBottomLeft(), 0.0f, f), builtHalfIconRectangle.colorTopLeft(), builtHalfIconRectangle.colorTopRight(), builtHalfIconRectangle.colorBottomRight(), builtHalfIconRectangle.colorBottomLeft(), Math.max(builtHalfIconRectangle.smoothness(), 0.0f), builtHalfIconRectangle.fontName(), builtHalfIconRectangle.icon(), f2, builtHalfIconRectangle.iconColor(), Math.max(builtHalfIconRectangle.spacingX(), f2), Math.max(builtHalfIconRectangle.spacingY(), f2), Math.max(builtHalfIconRectangle.paddingX(), 0.0f), Math.max(builtHalfIconRectangle.paddingY(), 0.0f), Math.max(builtHalfIconRectangle.jitterX(), 0.0f), Math.max(builtHalfIconRectangle.jitterY(), 0.0f), HalfIconRectangleRenderer.clamp(builtHalfIconRectangle.rotationMaxDegrees(), 0.0f, 90.0f), builtHalfIconRectangle.seed());
    }

    int reserve(BuiltHalfIconRectangle builtHalfIconRectangle) {
        int n = this.preparedRectangles.size();
        if (n == 256) {
            return -1;
        }
        this.preparedRectangles.add(builtHalfIconRectangle);
        this.paramsDirty = true;
        return n;
    }

    public static void closeInstance() {
        HalfIconRectangleRenderer halfIconRectangleRenderer = instance;
        if (halfIconRectangleRenderer != null) {
            halfIconRectangleRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedRectangles.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedRectangles.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedRectangles);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltHalfIconRectangle builtHalfIconRectangle) {
        this.beginFrame(drawContext);
        this.enqueue(builtHalfIconRectangle);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedRectangles.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("RectHalfIconParamsArray", gpuBuffer);
        }
    }

    public boolean isHalfIconRectanglePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == HALF_ICON_RECTANGLE_PIPELINE;
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 12288L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_rect_half_icon_params", 136, 12288L);
            return this.paramsBuffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private void closeParamsBuffer() {
        if (this.paramsBuffer != null) {
            this.paramsBuffer.close();
            this.paramsBuffer = null;
        }
    }

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltHalfIconRectangle> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(12288);
        for (int i = 0; i < list.size(); ++i) {
            BuiltHalfIconRectangle builtHalfIconRectangle = list.get(i);
            int n = i * 3 * 4 * 4;
            byteBuffer.putFloat(n, builtHalfIconRectangle.x());
            byteBuffer.putFloat(n + 4, builtHalfIconRectangle.y());
            byteBuffer.putFloat(n + 8, builtHalfIconRectangle.width());
            byteBuffer.putFloat(n + 12, builtHalfIconRectangle.height());
            byteBuffer.putFloat(n + 16, builtHalfIconRectangle.radiusTopLeft());
            byteBuffer.putFloat(n + 20, builtHalfIconRectangle.radiusTopRight());
            byteBuffer.putFloat(n + 24, builtHalfIconRectangle.radiusBottomRight());
            byteBuffer.putFloat(n + 28, builtHalfIconRectangle.radiusBottomLeft());
            byteBuffer.putFloat(n + 32, builtHalfIconRectangle.smoothness());
            byteBuffer.putFloat(n + 36, 0.0f);
            byteBuffer.putFloat(n + 40, 0.0f);
            byteBuffer.putFloat(n + 44, 0.0f);
        }
        byteBuffer.position(0);
        return byteBuffer;
    }

    private GpuBuffer ensureParamsBuffer() {
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.prepareBuffers();
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedRectangles);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_rect_half_icon_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }

    private List<HalfIconRectangleRenderState.IconQuad> buildIconQuads(BuiltHalfIconRectangle builtHalfIconRectangle, GlyphLayout glyphLayout, GlyphLayout.GlyphPage glyphPage) {
        ArrayList<HalfIconRectangleRenderState.IconQuad> arrayList = new ArrayList<HalfIconRectangleRenderState.IconQuad>(64);
        float f = Math.max(builtHalfIconRectangle.spacingX(), Math.max(glyphLayout.width(), 1.0f));
        float f2 = Math.max(builtHalfIconRectangle.spacingY(), Math.max(glyphLayout.height(), 1.0f));
        float f3 = builtHalfIconRectangle.x() + builtHalfIconRectangle.paddingX() + glyphLayout.width() * 0.5f;
        float f4 = builtHalfIconRectangle.y() + builtHalfIconRectangle.paddingY() + glyphLayout.height() * 0.5f;
        float f5 = builtHalfIconRectangle.x() + builtHalfIconRectangle.width() - builtHalfIconRectangle.paddingX() - glyphLayout.width() * 0.5f;
        float f6 = builtHalfIconRectangle.y() + builtHalfIconRectangle.height() - builtHalfIconRectangle.paddingY() - glyphLayout.height() * 0.5f;
        if (f5 < f3 || f6 < f4) {
            return List.of();
        }
        int n = Math.min(96, Math.max(1, (int)Math.floor((f6 - f4) / f2) + 1));
        int n2 = Math.min(96, Math.max(1, (int)Math.floor((f5 - f3) / f) + 1));
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < n2; ++j) {
                float f7 = f3 + (float)j * f + HalfIconRectangleRenderer.randomSigned(builtHalfIconRectangle.seed(), j, i, 1) * builtHalfIconRectangle.jitterX();
                float f8 = f4 + (float)i * f2 + HalfIconRectangleRenderer.randomSigned(builtHalfIconRectangle.seed(), j, i, 2) * builtHalfIconRectangle.jitterY();
                float f9 = (float)Math.toRadians(HalfIconRectangleRenderer.randomSigned(builtHalfIconRectangle.seed(), j, i, 3) * builtHalfIconRectangle.rotationMaxDegrees());
                float f10 = (float)Math.sin(f9);
                float f11 = (float)Math.cos(f9);
                float f12 = f7 - glyphLayout.width() * 0.5f;
                float f13 = f8 - glyphLayout.height() * 0.5f;
                int n3 = HalfIconRectangleRenderer.withAlphaMultiplier(builtHalfIconRectangle.iconColor(), 1.0f - HalfIconRectangleRenderer.clamp((f8 - builtHalfIconRectangle.y()) / Math.max(builtHalfIconRectangle.height(), 1.0f), 0.0f, 1.0f) * 0.92f);
                for (GlyphLayout.Glyph glyph : glyphPage.glyphs()) {
                    float f14 = f12 + glyph.x0();
                    float f15 = f13 + glyph.y0();
                    float f16 = f12 + glyph.x1();
                    float f17 = f13 + glyph.y1();
                    RotatedPoint rotatedPoint = HalfIconRectangleRenderer.rotate(f14, f15, f7, f8, f10, f11);
                    RotatedPoint rotatedPoint2 = HalfIconRectangleRenderer.rotate(f14, f17, f7, f8, f10, f11);
                    RotatedPoint rotatedPoint3 = HalfIconRectangleRenderer.rotate(f16, f17, f7, f8, f10, f11);
                    RotatedPoint rotatedPoint4 = HalfIconRectangleRenderer.rotate(f16, f15, f7, f8, f10, f11);
                    arrayList.add(new HalfIconRectangleRenderState.IconQuad(rotatedPoint.x(), rotatedPoint.y(), glyph.u0(), glyph.v0(), rotatedPoint2.x(), rotatedPoint2.y(), glyph.u0(), glyph.v1(), rotatedPoint3.x(), rotatedPoint3.y(), glyph.u1(), glyph.v1(), rotatedPoint4.x(), rotatedPoint4.y(), glyph.u1(), glyph.v0(), n3));
                }
            }
        }
        return arrayList;
    }

    private static int withAlphaMultiplier(int n, float f) {
        int n2 = Math.round((float)(n >>> 24 & 0xFF) * HalfIconRectangleRenderer.clamp(f, 0.0f, 1.0f));
        return n2 << 24 | n & 0xFFFFFF;
    }

    private static float randomSigned(long l, int n, int n2, int n3) {
        long l2 = l ^ (long)n * -7046029254386353131L ^ (long)n2 * -4658895280553007687L ^ (long)n3 * -7723592293110705685L;
        l2 ^= l2 >>> 30;
        l2 *= -4658895280553007687L;
        l2 ^= l2 >>> 27;
        l2 *= -7723592293110705685L;
        l2 ^= l2 >>> 31;
        return (float)(l2 & 0xFFFFFFL) / 1.6777215E7f * 2.0f - 1.0f;
    }

    public static record RotatedPoint(float x, float y) {}
    public static record FrameBatchKey(GuiRenderState renderState, int layerSerial, TextureSetup textureSetup, PoseKey poseKey) {}
}

