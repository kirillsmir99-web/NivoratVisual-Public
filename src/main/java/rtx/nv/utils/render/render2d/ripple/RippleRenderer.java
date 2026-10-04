package rtx.nv.utils.render.render2d.ripple;
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
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.ripple.BuiltRipple;
import rtx.nv.utils.render.render2d.ripple.RippleRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class RippleRenderer
implements AutoCloseable {
    private static final int MAX_RIPPLES = 512;
    private static final int PARAMS_PER_RIPPLE = 5;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 40960;
    private static volatile RippleRenderer instance;
    private static final VertexFormat RIPPLE_VERTEX_FORMAT;
    public static final RenderPipeline RIPPLE_PIPELINE;
    private final List<BuiltRipple> preparedRipples = new ArrayList<BuiltRipple>(128);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private RippleRenderer() {
    }

    static {
        RIPPLE_VERTEX_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("LineWidth", VertexFormatElement.LINE_WIDTH).build();
        RIPPLE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(RippleRenderer.id("pipeline/ripple")).withVertexShader(RippleRenderer.id("core/ripple")).withFragmentShader(RippleRenderer.id("core/ripple")).withVertexFormat(RIPPLE_VERTEX_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("RippleParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static RippleRenderer getInstance() {
        RippleRenderer rippleRenderer = instance;
        if (rippleRenderer != null) return rippleRenderer;
        Class<RippleRenderer> clazz = RippleRenderer.class;
        synchronized (RippleRenderer.class) {
            rippleRenderer = instance;
            if (rippleRenderer != null) return rippleRenderer;
            instance = rippleRenderer = new RippleRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return rippleRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedRipples.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltRipple builtRipple) {
        this.submit(this.activeGraphics, builtRipple);
    }

    private void submit(DrawContext drawContext, BuiltRipple builtRipple) {
        if (drawContext == null || builtRipple == null || builtRipple.width <= 0.0f || builtRipple.height <= 0.0f) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new RippleRenderState(matrix3x2f, builtRipple, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    int reserve(BuiltRipple builtRipple) {
        int n = this.preparedRipples.size();
        if (n == 512) {
            return -1;
        }
        this.preparedRipples.add(builtRipple);
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
        RippleRenderer rippleRenderer = instance;
        if (rippleRenderer != null) {
            rippleRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedRipples.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedRipples.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedRipples);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public boolean isRipplePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == RIPPLE_PIPELINE;
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedRipples.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("RippleParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 40960L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_ripple_params", 136, 40960L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltRipple> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(40960);
        for (int i = 0; i < list.size(); ++i) {
            BuiltRipple builtRipple = list.get(i);
            int n = i * 5 * 4 * 4;
            byteBuffer.putFloat(n, builtRipple.radiusTopLeft);
            byteBuffer.putFloat(n + 4, builtRipple.radiusTopRight);
            byteBuffer.putFloat(n + 8, builtRipple.radiusBottomRight);
            byteBuffer.putFloat(n + 12, builtRipple.radiusBottomLeft);
            byteBuffer.putFloat(n + 16, builtRipple.width);
            byteBuffer.putFloat(n + 20, builtRipple.height);
            byteBuffer.putFloat(n + 24, builtRipple.smoothness);
            byteBuffer.putFloat(n + 28, builtRipple.textured ? 1.0f : 0.0f);
            byteBuffer.putFloat(n + 32, builtRipple.centerX);
            byteBuffer.putFloat(n + 36, builtRipple.centerY);
            byteBuffer.putFloat(n + 40, builtRipple.rippleRadius);
            byteBuffer.putFloat(n + 44, builtRipple.rippleSmoothness);
            RippleRenderer.putColor(byteBuffer, n + 48, builtRipple.sourceColor);
            RippleRenderer.putColor(byteBuffer, n + 64, builtRipple.targetColor);
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedRipples);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_ripple_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

