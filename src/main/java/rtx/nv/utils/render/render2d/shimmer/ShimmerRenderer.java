package rtx.nv.utils.render.render2d.shimmer;
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
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.NV;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.post.GuiRenderStateLayerAccessor;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.shimmer.BuiltShimmer;
import rtx.nv.utils.render.render2d.shimmer.ShimmerRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class ShimmerRenderer
implements AutoCloseable {
    private static final int MAX_SHIMMERS = 256;
    private static final int FLOATS_PER = 4;
    private static final int UNIFORM_BYTES = 4096;
    private static volatile ShimmerRenderer instance;
    private static final VertexFormat SHIMMER_FMT;
    public static final RenderPipeline SHIMMER_PIPELINE;
    private final List<BuiltShimmer> prepared = new ArrayList<BuiltShimmer>(32);
    private final Map<ShimmerRenderer.FrameBatchKey, ShimmerRenderState> frameBatches = new LinkedHashMap<ShimmerRenderer.FrameBatchKey, ShimmerRenderState>(16);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty;

    private ShimmerRenderer() {
    }

    static {
        SHIMMER_FMT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).add("LineWidth", VertexFormatElement.LINE_WIDTH).build();
        SHIMMER_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ShimmerRenderer.id("pipeline/shimmer")).withVertexShader(ShimmerRenderer.id("core/shimmer")).withFragmentShader(ShimmerRenderer.id("core/shimmer")).withVertexFormat(SHIMMER_FMT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ShimmerParamsArray", UniformType.UNIFORM_BUFFER).build();
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
    public static ShimmerRenderer getInstance() {
        ShimmerRenderer shimmerRenderer = instance;
        if (shimmerRenderer != null) return shimmerRenderer;
        Class<ShimmerRenderer> clazz = ShimmerRenderer.class;
        synchronized (ShimmerRenderer.class) {
            shimmerRenderer = instance;
            if (shimmerRenderer != null) return shimmerRenderer;
            instance = shimmerRenderer = new ShimmerRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return shimmerRenderer;
        }
    }

    @Override
    public void close() {
        this.prepared.clear();
        this.frameBatches.clear();
        this.closeBuffer();
        this.activeGraphics = null;
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltShimmer builtShimmer) {
        if (this.activeGraphics == null || builtShimmer == null) {
            return;
        }
        try {
            GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)(Object)this.activeGraphics).nv_getGuiRenderState();
            int n = ((GuiRenderStateLayerAccessor)guiRenderState).nv_getLayerSerial();
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(this.activeGraphics);
            ScreenRect screenRect = ScissorUtil.current();
            ShimmerRenderer.FrameBatchKey frameBatchKey = new ShimmerRenderer.FrameBatchKey(guiRenderState, n, screenRect);
            ShimmerRenderState shimmerRenderState = this.frameBatches.get(frameBatchKey);
            if (shimmerRenderState == null) {
                shimmerRenderState = new ShimmerRenderState(matrix3x2f, builtShimmer, screenRect);
                this.frameBatches.put(frameBatchKey, shimmerRenderState);
                guiRenderState.addSimpleElement((SimpleGuiElementRenderState)shimmerRenderState);
            }
        }
        catch (RuntimeException runtimeException) {
            NV.LOGGER.warn("[ShimmerRenderer] submit failed", (Throwable)runtimeException);
        }
    }

    int reserve(ShimmerRenderState shimmerRenderState) {
        int n = this.prepared.size();
        if (n >= 256) {
            return -1;
        }
        this.prepared.add(shimmerRenderState.shimmer());
        this.paramsDirty = true;
        return n;
    }

    public static void closeInstance() {
        ShimmerRenderer shimmerRenderer = instance;
        if (shimmerRenderer != null) {
            shimmerRenderer.close();
            instance = null;
        }
    }

    private void closeBuffer() {
        if (this.paramsBuffer != null) {
            this.paramsBuffer.close();
            this.paramsBuffer = null;
        }
    }

    public void beginGuiFrame() {
        this.prepared.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.prepared.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildData(memoryStack);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public boolean isShimmerPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == SHIMMER_PIPELINE;
    }

    public void beginFrame(DrawContext drawContext) {
        if (this.activeGraphics != drawContext) {
            this.frameBatches.clear();
        }
        this.activeGraphics = drawContext;
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.prepared.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("ShimmerParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureParamsBuffer() {
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.prepareBuffers();
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        this.closeBuffer();
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_shimmer_params", 128, this.buildData(memoryStack));
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }

    private ByteBuffer buildData(MemoryStack memoryStack) {
        ByteBuffer byteBuffer = memoryStack.calloc(4096);
        for (int i = 0; i < this.prepared.size(); ++i) {
            BuiltShimmer builtShimmer = this.prepared.get(i);
            int n = i * 4 * 4;
            byteBuffer.putFloat(n, builtShimmer.progress());
            byteBuffer.putFloat(n + 4, builtShimmer.halfWidth());
            byteBuffer.putFloat(n + 8, builtShimmer.intensity());
            byteBuffer.putFloat(n + 12, 0.0f);
        }
        byteBuffer.position(0);
        return byteBuffer;
    }

    private GpuBuffer ensureWritableBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 4096L) {
            return this.paramsBuffer;
        }
        this.closeBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_shimmer_params", 136, 4096L);
            return this.paramsBuffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }


    public static record FrameBatchKey(GuiRenderState state, int layer, ScreenRect scissor) {
    }
}

