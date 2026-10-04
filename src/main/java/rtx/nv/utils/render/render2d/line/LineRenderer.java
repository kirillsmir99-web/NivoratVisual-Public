package rtx.nv.utils.render.render2d.line;
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
import rtx.nv.utils.render.render2d.line.BuiltLine;
import rtx.nv.utils.render.render2d.line.LineRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class LineRenderer
implements AutoCloseable {
    private static final int MAX_LINES = 2048;
    private static final int PARAMS_PER_LINE = 2;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 65536;
    private static volatile LineRenderer instance;
    public static final RenderPipeline LINE_PIPELINE;
    private final List<BuiltLine> preparedLines = new ArrayList<BuiltLine>(128);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private LineRenderer() {
    }

    static {
        LINE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(LineRenderer.id("pipeline/line")).withVertexShader(LineRenderer.id("core/line")).withFragmentShader(LineRenderer.id("core/line")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("LineParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static LineRenderer getInstance() {
        LineRenderer lineRenderer = instance;
        if (lineRenderer != null) return lineRenderer;
        Class<LineRenderer> clazz = LineRenderer.class;
        synchronized (LineRenderer.class) {
            lineRenderer = instance;
            if (lineRenderer != null) return lineRenderer;
            instance = lineRenderer = new LineRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return lineRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedLines.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltLine builtLine) {
        this.submit(this.activeGraphics, builtLine);
    }

    public void submit(DrawContext drawContext, BuiltLine builtLine) {
        if (drawContext == null || builtLine == null || !builtLine.visible()) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new LineRenderState(matrix3x2f, builtLine, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    int reserve(BuiltLine builtLine) {
        int n = this.preparedLines.size();
        if (n == 2048) {
            return -1;
        }
        this.preparedLines.add(builtLine);
        this.paramsDirty = true;
        return n;
    }

    public static void closeInstance() {
        LineRenderer lineRenderer = instance;
        if (lineRenderer != null) {
            lineRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedLines.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedLines.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedLines);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public boolean isLinePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == LINE_PIPELINE;
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedLines.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("LineParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 65536L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_line_params", 136, 65536L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltLine> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(65536);
        for (int i = 0; i < list.size(); ++i) {
            BuiltLine builtLine = list.get(i);
            int n = builtLine.color();
            int n2 = i * 2 * 4 * 4;
            byteBuffer.putFloat(n2, (float)(n >>> 16 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n2 + 4, (float)(n >>> 8 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n2 + 8, (float)(n & 0xFF) / 255.0f);
            byteBuffer.putFloat(n2 + 12, (float)(n >>> 24 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n2 + 16, builtLine.length());
            byteBuffer.putFloat(n2 + 20, builtLine.thickness());
            byteBuffer.putFloat(n2 + 24, builtLine.fadeStart());
            byteBuffer.putFloat(n2 + 28, builtLine.fadeEnd());
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedLines);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_line_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

