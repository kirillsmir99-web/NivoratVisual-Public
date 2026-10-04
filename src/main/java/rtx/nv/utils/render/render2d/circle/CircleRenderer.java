package rtx.nv.utils.render.render2d.circle;
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
import rtx.nv.utils.render.render2d.circle.BuiltCircle;
import rtx.nv.utils.render.render2d.circle.CircleRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class CircleRenderer
implements AutoCloseable {
    private static final int MAX_CIRCLES = 512;
    private static final int PARAMS_PER_CIRCLE = 2;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 16384;
    private static volatile CircleRenderer instance;
    public static final RenderPipeline CIRCLE_PIPELINE;
    private final List<BuiltCircle> preparedCircles = new ArrayList<BuiltCircle>(128);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private CircleRenderer() {
    }

    static {
        CIRCLE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(CircleRenderer.id("pipeline/circle")).withVertexShader(CircleRenderer.id("core/circle")).withFragmentShader(CircleRenderer.id("core/circle")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("CircleParamsArray", UniformType.UNIFORM_BUFFER).build();
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
    public static CircleRenderer getInstance() {
        CircleRenderer circleRenderer = instance;
        if (circleRenderer != null) return circleRenderer;
        Class<CircleRenderer> clazz = CircleRenderer.class;
        synchronized (CircleRenderer.class) {
            circleRenderer = instance;
            if (circleRenderer != null) return circleRenderer;
            instance = circleRenderer = new CircleRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return circleRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedCircles.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltCircle builtCircle) {
        this.submit(this.activeGraphics, builtCircle);
    }

    private void submit(DrawContext drawContext, BuiltCircle builtCircle) {
        if (drawContext == null || builtCircle == null || !builtCircle.visible()) {
            return;
        }
        try {
            BuiltCircle builtCircle2 = this.normalize(builtCircle);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new CircleRenderState(matrix3x2f, builtCircle2, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltCircle normalize(BuiltCircle builtCircle) {
        return new BuiltCircle(builtCircle.x(), builtCircle.y(), Math.max(builtCircle.radius(), 0.0f), CircleRenderer.clamp(builtCircle.thickness(), 0.0f, builtCircle.radius()), Math.max(builtCircle.smoothness(), 0.0f), builtCircle.color());
    }

    int reserve(BuiltCircle builtCircle) {
        int n = this.preparedCircles.size();
        if (n == 512) {
            return -1;
        }
        this.preparedCircles.add(builtCircle);
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
        CircleRenderer circleRenderer = instance;
        if (circleRenderer != null) {
            circleRenderer.close();
            instance = null;
        }
    }

    public boolean isCirclePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == CIRCLE_PIPELINE;
    }

    public void beginGuiFrame() {
        this.preparedCircles.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedCircles.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedCircles);
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

    public void draw(DrawContext drawContext, BuiltCircle builtCircle) {
        this.beginFrame(drawContext);
        this.enqueue(builtCircle);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedCircles.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("CircleParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 16384L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_circle_params", 136, 16384L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltCircle> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(16384);
        for (int i = 0; i < list.size(); ++i) {
            BuiltCircle builtCircle = list.get(i);
            int n = i * 2 * 4 * 4;
            byteBuffer.putFloat(n, builtCircle.radius());
            byteBuffer.putFloat(n + 4, Math.max(builtCircle.smoothness(), 0.0f));
            byteBuffer.putFloat(n + 8, builtCircle.thickness());
            byteBuffer.putFloat(n + 12, 0.0f);
            CircleRenderer.putColor(byteBuffer, n + 16, builtCircle.color());
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedCircles);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_circle_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

