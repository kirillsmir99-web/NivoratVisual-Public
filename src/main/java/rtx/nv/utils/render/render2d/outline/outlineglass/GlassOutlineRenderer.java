package rtx.nv.utils.render.render2d.outline.outlineglass;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;
import rtx.nv.utils.render.render2d.outline.outlineglass.BuiltGlassOutline;
import rtx.nv.utils.render.render2d.outline.outlineglass.GlassOutlineRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class GlassOutlineRenderer
implements AutoCloseable {
    private static final int MAX_OUTLINES = 438;
    private static final int PARAMS_PER_OUTLINE = 7;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 49056;
    private static volatile GlassOutlineRenderer instance;
    public static final RenderPipeline GLASS_OUTLINE_PIPELINE;
    private final List<BuiltGlassOutline> preparedOutlines = new ArrayList<BuiltGlassOutline>(128);
    private final List<BlurCapture> preparedCaptures = new ArrayList<BlurCapture>(128);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private GlassOutlineRenderer() {
    }

    static {
        GLASS_OUTLINE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlassOutlineRenderer.id("pipeline/outline_glass")).withVertexShader(GlassOutlineRenderer.id("core/outline_glass")).withFragmentShader(GlassOutlineRenderer.id("core/outline_glass")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("GlassOutlineParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static GlassOutlineRenderer getInstance() {
        GlassOutlineRenderer glassOutlineRenderer = instance;
        if (glassOutlineRenderer != null) return glassOutlineRenderer;
        Class<GlassOutlineRenderer> clazz = GlassOutlineRenderer.class;
        synchronized (GlassOutlineRenderer.class) {
            glassOutlineRenderer = instance;
            if (glassOutlineRenderer != null) return glassOutlineRenderer;
            instance = glassOutlineRenderer = new GlassOutlineRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return glassOutlineRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedOutlines.clear();
        this.preparedCaptures.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltGlassOutline builtGlassOutline) {
        this.submit(this.activeGraphics, builtGlassOutline);
    }

    private void submit(DrawContext drawContext, BuiltGlassOutline builtGlassOutline) {
        if (drawContext == null || builtGlassOutline == null || !builtGlassOutline.visible()) {
            return;
        }
        try {
            BuiltGlassOutline builtGlassOutline2 = this.normalize(builtGlassOutline);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            BlurCapture blurCapture = new BlurCapture();
            BlurFramebuffer.getInstance().requestCapture(drawContext, new BuiltBlur(builtGlassOutline2.x(), builtGlassOutline2.y(), builtGlassOutline2.width(), builtGlassOutline2.height(), builtGlassOutline2.radiusTopLeft(), builtGlassOutline2.radiusTopRight(), builtGlassOutline2.radiusBottomRight(), builtGlassOutline2.radiusBottomLeft(), 1.0f, 30.0f, -1), blurCapture);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new GlassOutlineRenderState(matrix3x2f, builtGlassOutline2, ScissorUtil.current(), blurCapture));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltGlassOutline normalize(BuiltGlassOutline builtGlassOutline) {
        float f = Math.max(0.0f, Math.min(builtGlassOutline.width(), builtGlassOutline.height()) * 0.5f);
        float f2 = Math.max(0.0f, Math.min(builtGlassOutline.width(), builtGlassOutline.height()) * 0.5f);
        return new BuiltGlassOutline(builtGlassOutline.x(), builtGlassOutline.y(), builtGlassOutline.width(), builtGlassOutline.height(), GlassOutlineRenderer.clamp(builtGlassOutline.radiusTopLeft(), 0.0f, f), GlassOutlineRenderer.clamp(builtGlassOutline.radiusTopRight(), 0.0f, f), GlassOutlineRenderer.clamp(builtGlassOutline.radiusBottomRight(), 0.0f, f), GlassOutlineRenderer.clamp(builtGlassOutline.radiusBottomLeft(), 0.0f, f), GlassOutlineRenderer.clamp(builtGlassOutline.thickness(), 0.0f, f2), builtGlassOutline.color(), GlassOutlineRenderer.clamp(builtGlassOutline.globalAlpha(), 0.0f, 1.0f), Math.max(builtGlassOutline.fresnelPower(), 0.001f), builtGlassOutline.fresnelColor(), GlassOutlineRenderer.clamp(builtGlassOutline.baseAlpha(), 0.0f, 1.0f), builtGlassOutline.fresnelInvert(), GlassOutlineRenderer.clamp(builtGlassOutline.fresnelMix(), 0.0f, 1.0f), builtGlassOutline.distortStrength(), Math.max(builtGlassOutline.squirt(), 0.001f), Math.max(builtGlassOutline.smoothness(), 0.0f), builtGlassOutline.z());
    }

    int reserve(BuiltGlassOutline builtGlassOutline, BlurCapture blurCapture) {
        int n = this.preparedOutlines.size();
        if (n == 438) {
            return -1;
        }
        this.preparedOutlines.add(builtGlassOutline);
        this.preparedCaptures.add(blurCapture);
        this.paramsDirty = true;
        return n;
    }

    private static void putColor(ByteBuffer byteBuffer, int n, int n2) {
        byteBuffer.putFloat(n, (float)(n2 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 4, (float)(n2 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 8, (float)(n2 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 12, (float)(n2 >>> 24 & 0xFF) / 255.0f);
    }

    public static void closeInstance() {
        GlassOutlineRenderer glassOutlineRenderer = instance;
        if (glassOutlineRenderer != null) {
            glassOutlineRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedOutlines.clear();
        this.preparedCaptures.clear();
        this.paramsDirty = false;
    }

    public boolean isGlassOutlinePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == GLASS_OUTLINE_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedOutlines.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedOutlines);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltGlassOutline builtGlassOutline) {
        this.beginFrame(drawContext);
        this.enqueue(builtGlassOutline);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedOutlines.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("GlassOutlineParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 49056L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_glass_params", 136, 49056L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltGlassOutline> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(49056);
        for (int i = 0; i < list.size(); ++i) {
            BlurCapture blurCapture;
            BuiltGlassOutline builtGlassOutline = list.get(i);
            int n = i * 7 * 4 * 4;
            byteBuffer.putFloat(n, builtGlassOutline.radiusTopLeft());
            byteBuffer.putFloat(n + 4, builtGlassOutline.radiusTopRight());
            byteBuffer.putFloat(n + 8, builtGlassOutline.radiusBottomRight());
            byteBuffer.putFloat(n + 12, builtGlassOutline.radiusBottomLeft());
            byteBuffer.putFloat(n + 16, builtGlassOutline.width());
            byteBuffer.putFloat(n + 20, builtGlassOutline.height());
            byteBuffer.putFloat(n + 24, builtGlassOutline.thickness());
            byteBuffer.putFloat(n + 28, builtGlassOutline.smoothness());
            byteBuffer.putFloat(n + 32, builtGlassOutline.globalAlpha());
            byteBuffer.putFloat(n + 36, builtGlassOutline.fresnelPower());
            byteBuffer.putFloat(n + 40, builtGlassOutline.baseAlpha());
            byteBuffer.putFloat(n + 44, builtGlassOutline.fresnelMix());
            GlassOutlineRenderer.putColor(byteBuffer, n + 48, builtGlassOutline.fresnelColor());
            byteBuffer.putFloat(n + 64, builtGlassOutline.fresnelInvert() ? 1.0f : 0.0f);
            byteBuffer.putFloat(n + 68, builtGlassOutline.distortStrength());
            byteBuffer.putFloat(n + 72, builtGlassOutline.z());
            byteBuffer.putFloat(n + 76, Math.max(builtGlassOutline.squirt(), 0.001f));
            GlassOutlineRenderer.putColor(byteBuffer, n + 80, builtGlassOutline.color());
            BlurCapture blurCapture2 = blurCapture = i < this.preparedCaptures.size() ? this.preparedCaptures.get(i) : null;
            if (blurCapture == null) continue;
            byteBuffer.putFloat(n + 96, blurCapture.regionX);
            byteBuffer.putFloat(n + 100, blurCapture.regionY);
            byteBuffer.putFloat(n + 104, blurCapture.regionW);
            byteBuffer.putFloat(n + 108, blurCapture.regionH);
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedOutlines);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_glass_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

