package rtx.nv.utils.render.render2d.outline.outlinedefault;
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
import rtx.nv.utils.render.others.RoundedScissor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.outline.outlinedefault.BuiltOutline;
import rtx.nv.utils.render.render2d.outline.outlinedefault.DefaultOutlineRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class DefaultOutlineRenderer
implements AutoCloseable {
    private static final int MAX_OUTLINES = 448;
    private static final int PARAMS_PER_OUTLINE = 9;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 64512;
    private static volatile DefaultOutlineRenderer instance;
    public static final RenderPipeline OUTLINE_PIPELINE;
    private final List<BuiltOutline> preparedOutlines = new ArrayList<BuiltOutline>(128);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private DefaultOutlineRenderer() {
    }

    static {
        OUTLINE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(DefaultOutlineRenderer.id("pipeline/outline_default")).withVertexShader(DefaultOutlineRenderer.id("core/outline_default")).withFragmentShader(DefaultOutlineRenderer.id("core/outline_default")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("OutlineParamsArray", UniformType.UNIFORM_BUFFER).build();
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
    public static DefaultOutlineRenderer getInstance() {
        DefaultOutlineRenderer defaultOutlineRenderer = instance;
        if (defaultOutlineRenderer != null) return defaultOutlineRenderer;
        Class<DefaultOutlineRenderer> clazz = DefaultOutlineRenderer.class;
        synchronized (DefaultOutlineRenderer.class) {
            defaultOutlineRenderer = instance;
            if (defaultOutlineRenderer != null) return defaultOutlineRenderer;
            instance = defaultOutlineRenderer = new DefaultOutlineRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return defaultOutlineRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedOutlines.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltOutline builtOutline) {
        this.submit(this.activeGraphics, builtOutline);
    }

    private void submit(DrawContext drawContext, BuiltOutline builtOutline) {
        if (drawContext == null || builtOutline == null || !builtOutline.visible()) {
            return;
        }
        try {
            BuiltOutline builtOutline2 = this.normalize(builtOutline);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new DefaultOutlineRenderState(matrix3x2f, builtOutline2, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltOutline normalize(BuiltOutline builtOutline) {
        float f = Math.max(0.0f, Math.max(builtOutline.width(), builtOutline.height()) * 0.5f);
        float f2 = Math.max(0.0f, Math.min(builtOutline.width(), builtOutline.height()) * 0.5f);
        float f3 = builtOutline.scissorX();
        float f4 = builtOutline.scissorY();
        float f5 = builtOutline.scissorWidth();
        float f6 = builtOutline.scissorHeight();
        float f7 = builtOutline.scissorRadiusTopLeft();
        float f8 = builtOutline.scissorRadiusTopRight();
        float f9 = builtOutline.scissorRadiusBottomRight();
        float f10 = builtOutline.scissorRadiusBottomLeft();
        float f11 = builtOutline.scissorCos();
        float f12 = builtOutline.scissorSin();
        if (RoundedScissor.isEnabled()) {
            f3 = RoundedScissor.x();
            f4 = RoundedScissor.y();
            f5 = RoundedScissor.width();
            f6 = RoundedScissor.height();
            f7 = RoundedScissor.radiusTopLeft();
            f8 = RoundedScissor.radiusTopRight();
            f9 = RoundedScissor.radiusBottomRight();
            f10 = RoundedScissor.radiusBottomLeft();
            f11 = RoundedScissor.cos();
            f12 = RoundedScissor.sin();
        }
        return new BuiltOutline(builtOutline.x(), builtOutline.y(), builtOutline.width(), builtOutline.height(), DefaultOutlineRenderer.clamp(builtOutline.radiusTopLeft(), 0.0f, f), DefaultOutlineRenderer.clamp(builtOutline.radiusTopRight(), 0.0f, f), DefaultOutlineRenderer.clamp(builtOutline.radiusBottomRight(), 0.0f, f), DefaultOutlineRenderer.clamp(builtOutline.radiusBottomLeft(), 0.0f, f), DefaultOutlineRenderer.clamp(builtOutline.thickness(), 0.0f, f2), builtOutline.colorTopLeft(), builtOutline.colorTopRight(), builtOutline.colorBottomRight(), builtOutline.colorBottomLeft(), Math.max(builtOutline.smoothness(), 0.0f), f3, f4, f5, f6, f7, f8, f9, f10, f11, f12);
    }

    int reserve(BuiltOutline builtOutline) {
        int n = this.preparedOutlines.size();
        if (n == 448) {
            return -1;
        }
        this.preparedOutlines.add(builtOutline);
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
        DefaultOutlineRenderer defaultOutlineRenderer = instance;
        if (defaultOutlineRenderer != null) {
            defaultOutlineRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedOutlines.clear();
        this.paramsDirty = false;
    }

    public boolean isOutlinePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == OUTLINE_PIPELINE;
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

    public void draw(DrawContext drawContext, BuiltOutline builtOutline) {
        this.beginFrame(drawContext);
        this.enqueue(builtOutline);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedOutlines.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("OutlineParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 64512L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_default_params", 136, 64512L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltOutline> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(64512);
        for (int i = 0; i < list.size(); ++i) {
            BuiltOutline builtOutline = list.get(i);
            int n = i * 9 * 4 * 4;
            byteBuffer.putFloat(n, builtOutline.radiusTopLeft());
            byteBuffer.putFloat(n + 4, builtOutline.radiusTopRight());
            byteBuffer.putFloat(n + 8, builtOutline.radiusBottomRight());
            byteBuffer.putFloat(n + 12, builtOutline.radiusBottomLeft());
            byteBuffer.putFloat(n + 16, builtOutline.width());
            byteBuffer.putFloat(n + 20, builtOutline.height());
            byteBuffer.putFloat(n + 24, builtOutline.thickness());
            byteBuffer.putFloat(n + 28, builtOutline.smoothness());
            DefaultOutlineRenderer.putColor(byteBuffer, n + 32, builtOutline.colorTopLeft());
            DefaultOutlineRenderer.putColor(byteBuffer, n + 48, builtOutline.colorTopRight());
            DefaultOutlineRenderer.putColor(byteBuffer, n + 64, builtOutline.colorBottomRight());
            DefaultOutlineRenderer.putColor(byteBuffer, n + 80, builtOutline.colorBottomLeft());
            byteBuffer.putFloat(n + 96, builtOutline.scissorX());
            byteBuffer.putFloat(n + 100, builtOutline.scissorY());
            byteBuffer.putFloat(n + 104, builtOutline.scissorWidth());
            byteBuffer.putFloat(n + 108, builtOutline.scissorHeight());
            byteBuffer.putFloat(n + 112, builtOutline.scissorRadiusTopLeft());
            byteBuffer.putFloat(n + 116, builtOutline.scissorRadiusTopRight());
            byteBuffer.putFloat(n + 120, builtOutline.scissorRadiusBottomRight());
            byteBuffer.putFloat(n + 124, builtOutline.scissorRadiusBottomLeft());
            byteBuffer.putFloat(n + 128, builtOutline.scissorCos());
            byteBuffer.putFloat(n + 132, builtOutline.scissorSin());
            byteBuffer.putFloat(n + 136, 0.0f);
            byteBuffer.putFloat(n + 140, 0.0f);
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
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_default_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

