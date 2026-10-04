package rtx.nv.utils.render.render2d.zippy;
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
import rtx.nv.utils.render.render2d.zippy.BuiltZippy;
import rtx.nv.utils.render.render2d.zippy.ZippyRenderState;

public final class ZippyRenderer
implements AutoCloseable {
    private static final int MAX_ZIPPY = 256;
    private static final int PARAMS_PER_ZIPPY = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 12288;
    private static volatile ZippyRenderer instance;
    public static final RenderPipeline ZIPPY_PIPELINE;
    private final List<BuiltZippy> preparedZippy = new ArrayList<BuiltZippy>(32);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private ZippyRenderer() {
    }

    static {
        ZIPPY_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ZippyRenderer.id("pipeline/zippy")).withVertexShader(ZippyRenderer.id("core/zippy")).withFragmentShader(ZippyRenderer.id("core/zippy")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ZippyParamsArray", UniformType.UNIFORM_BUFFER).build();
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
    public static ZippyRenderer getInstance() {
        ZippyRenderer zippyRenderer = instance;
        if (zippyRenderer != null) return zippyRenderer;
        Class<ZippyRenderer> clazz = ZippyRenderer.class;
        synchronized (ZippyRenderer.class) {
            zippyRenderer = instance;
            if (zippyRenderer != null) return zippyRenderer;
            instance = zippyRenderer = new ZippyRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return zippyRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedZippy.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltZippy builtZippy) {
        this.submit(this.activeGraphics, builtZippy);
    }

    private void submit(DrawContext drawContext, BuiltZippy builtZippy) {
        if (drawContext == null || builtZippy == null || !builtZippy.visible()) {
            return;
        }
        try {
            BuiltZippy builtZippy2 = this.normalize(builtZippy);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new ZippyRenderState(matrix3x2f, builtZippy2));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltZippy normalize(BuiltZippy builtZippy) {
        float f = Math.max(0.0f, Math.min(builtZippy.width(), builtZippy.height()) * 0.5f);
        return new BuiltZippy(builtZippy.x(), builtZippy.y(), builtZippy.width(), builtZippy.height(), ZippyRenderer.clamp(builtZippy.radiusTopLeft(), 0.0f, f), ZippyRenderer.clamp(builtZippy.radiusTopRight(), 0.0f, f), ZippyRenderer.clamp(builtZippy.radiusBottomRight(), 0.0f, f), ZippyRenderer.clamp(builtZippy.radiusBottomLeft(), 0.0f, f), builtZippy.color(), Math.max(builtZippy.smoothness(), 0.0f), builtZippy.timeOffset());
    }

    int reserve(BuiltZippy builtZippy) {
        int n = this.preparedZippy.size();
        if (n == 256) {
            return -1;
        }
        this.preparedZippy.add(builtZippy);
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
        ZippyRenderer zippyRenderer = instance;
        if (zippyRenderer != null) {
            zippyRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedZippy.clear();
        this.paramsDirty = false;
    }

    public boolean isZippyPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == ZIPPY_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedZippy.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedZippy);
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

    public void draw(DrawContext drawContext, BuiltZippy builtZippy) {
        this.beginFrame(drawContext);
        this.enqueue(builtZippy);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedZippy.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("ZippyParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 12288L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_zippy_params", 136, 12288L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltZippy> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(12288);
        float f = (float)(System.currentTimeMillis() % 120000L) / 1000.0f;
        for (int i = 0; i < list.size(); ++i) {
            BuiltZippy builtZippy = list.get(i);
            int n = i * 3 * 4 * 4;
            byteBuffer.putFloat(n, builtZippy.radiusTopLeft());
            byteBuffer.putFloat(n + 4, builtZippy.radiusTopRight());
            byteBuffer.putFloat(n + 8, builtZippy.radiusBottomRight());
            byteBuffer.putFloat(n + 12, builtZippy.radiusBottomLeft());
            byteBuffer.putFloat(n + 16, builtZippy.width());
            byteBuffer.putFloat(n + 20, builtZippy.height());
            byteBuffer.putFloat(n + 24, builtZippy.smoothness());
            byteBuffer.putFloat(n + 28, f + builtZippy.timeOffset());
            ZippyRenderer.putColor(byteBuffer, n + 32, builtZippy.color());
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedZippy);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_zippy_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

