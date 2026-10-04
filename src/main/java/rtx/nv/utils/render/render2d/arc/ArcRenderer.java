package rtx.nv.utils.render.render2d.arc;
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
import rtx.nv.utils.render.render2d.arc.ArcRenderState;
import rtx.nv.utils.render.render2d.arc.BuiltArc;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class ArcRenderer
implements AutoCloseable {
    private static final int MAX_ARCS = 256;
    private static final int PARAMS_PER_ARC = 2;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 8192;
    private static volatile ArcRenderer instance;
    public static final RenderPipeline ARC_PIPELINE;
    private final List<BuiltArc> preparedArcs = new ArrayList<BuiltArc>(64);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private ArcRenderer() {
    }

    static {
        ARC_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ArcRenderer.id("pipeline/arc_divider")).withVertexShader(ArcRenderer.id("core/arc_divider")).withFragmentShader(ArcRenderer.id("core/arc_divider")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("ArcParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static ArcRenderer getInstance() {
        ArcRenderer arcRenderer = instance;
        if (arcRenderer != null) return arcRenderer;
        Class<ArcRenderer> clazz = ArcRenderer.class;
        synchronized (ArcRenderer.class) {
            arcRenderer = instance;
            if (arcRenderer != null) return arcRenderer;
            instance = arcRenderer = new ArcRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return arcRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedArcs.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltArc builtArc) {
        this.submit(this.activeGraphics, builtArc);
    }

    public void submit(DrawContext drawContext, BuiltArc builtArc) {
        if (drawContext == null || builtArc == null || !builtArc.visible()) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new ArcRenderState(matrix3x2f, builtArc, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    int reserve(BuiltArc builtArc) {
        int n = this.preparedArcs.size();
        if (n == 256) {
            return -1;
        }
        this.preparedArcs.add(builtArc);
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
        ArcRenderer arcRenderer = instance;
        if (arcRenderer != null) {
            arcRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedArcs.clear();
        this.paramsDirty = false;
    }

    public boolean isArcPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == ARC_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedArcs.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedArcs);
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

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedArcs.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("ArcParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 8192L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_arc_params", 136, 8192L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltArc> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(8192);
        for (int i = 0; i < list.size(); ++i) {
            BuiltArc builtArc = list.get(i);
            int n = i * 2 * 4 * 4;
            byteBuffer.putFloat(n, builtArc.halfWidth());
            byteBuffer.putFloat(n + 4, Math.max(builtArc.lift(), 0.0f));
            byteBuffer.putFloat(n + 8, Math.max(builtArc.thickness(), 0.5f));
            byteBuffer.putFloat(n + 12, Math.max(builtArc.feather(), 0.5f));
            ArcRenderer.putColor(byteBuffer, n + 16, builtArc.color());
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedArcs);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_arc_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

