package rtx.nv.utils.render.render2d.rectangle.recthalftone;
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
import rtx.nv.utils.render.render2d.rectangle.recthalftone.BuiltHalftoneRectangle;
import rtx.nv.utils.render.render2d.rectangle.recthalftone.HalftoneRectangleRenderState;

public final class HalftoneRectangleRenderer
implements AutoCloseable {
    private static final int MAX_RECTANGLES = 384;
    private static final int PARAMS_PER_RECTANGLE = 8;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 49152;
    private static volatile HalftoneRectangleRenderer instance;
    public static final RenderPipeline HALFTONE_RECTANGLE_PIPELINE;
    private final List<BuiltHalftoneRectangle> preparedRectangles = new ArrayList<BuiltHalftoneRectangle>(64);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private HalftoneRectangleRenderer() {
    }

    static {
        HALFTONE_RECTANGLE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(HalftoneRectangleRenderer.id("pipeline/rect_halftone")).withVertexShader(HalftoneRectangleRenderer.id("core/rect_halftone")).withFragmentShader(HalftoneRectangleRenderer.id("core/rect_halftone")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("HalftoneRectangleParamsArray", UniformType.UNIFORM_BUFFER).build();
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
    public static HalftoneRectangleRenderer getInstance() {
        HalftoneRectangleRenderer halftoneRectangleRenderer = instance;
        if (halftoneRectangleRenderer != null) return halftoneRectangleRenderer;
        Class<HalftoneRectangleRenderer> clazz = HalftoneRectangleRenderer.class;
        synchronized (HalftoneRectangleRenderer.class) {
            halftoneRectangleRenderer = instance;
            if (halftoneRectangleRenderer != null) return halftoneRectangleRenderer;
            instance = halftoneRectangleRenderer = new HalftoneRectangleRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return halftoneRectangleRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedRectangles.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltHalftoneRectangle builtHalftoneRectangle) {
        this.submit(this.activeGraphics, builtHalftoneRectangle);
    }

    private void submit(DrawContext drawContext, BuiltHalftoneRectangle builtHalftoneRectangle) {
        if (drawContext == null || builtHalftoneRectangle == null || !builtHalftoneRectangle.visible()) {
            return;
        }
        try {
            BuiltHalftoneRectangle builtHalftoneRectangle2 = this.normalize(builtHalftoneRectangle);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new HalftoneRectangleRenderState(matrix3x2f, builtHalftoneRectangle2));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltHalftoneRectangle normalize(BuiltHalftoneRectangle builtHalftoneRectangle) {
        float f = Math.round(builtHalftoneRectangle.x());
        float f2 = Math.round(builtHalftoneRectangle.y());
        float f3 = Math.max(1.0f, (float)Math.round(builtHalftoneRectangle.width()));
        float f4 = Math.max(1.0f, (float)Math.round(builtHalftoneRectangle.height()));
        float f5 = Math.max(0.0f, Math.min(f3, f4) * 0.5f);
        float f6 = Math.max(Math.min(f3, f4), 0.0f);
        return new BuiltHalftoneRectangle(f, f2, f3, f4, HalftoneRectangleRenderer.clamp(builtHalftoneRectangle.radiusTopLeft(), 0.0f, f5), HalftoneRectangleRenderer.clamp(builtHalftoneRectangle.radiusTopRight(), 0.0f, f5), HalftoneRectangleRenderer.clamp(builtHalftoneRectangle.radiusBottomRight(), 0.0f, f5), HalftoneRectangleRenderer.clamp(builtHalftoneRectangle.radiusBottomLeft(), 0.0f, f5), builtHalftoneRectangle.colorTopLeft(), builtHalftoneRectangle.colorTopRight(), builtHalftoneRectangle.colorBottomRight(), builtHalftoneRectangle.colorBottomLeft(), Math.max(builtHalftoneRectangle.smoothness(), 0.0f), builtHalftoneRectangle.dotColor(), HalftoneRectangleRenderer.clamp(builtHalftoneRectangle.dotSize(), 0.0f, f6), Math.max(builtHalftoneRectangle.dotSpacing(), 0.0f));
    }

    int reserve(BuiltHalftoneRectangle builtHalftoneRectangle) {
        int n = this.preparedRectangles.size();
        if (n == 384) {
            return -1;
        }
        this.preparedRectangles.add(builtHalftoneRectangle);
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
        HalftoneRectangleRenderer halftoneRectangleRenderer = instance;
        if (halftoneRectangleRenderer != null) {
            halftoneRectangleRenderer.close();
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
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltHalftoneRectangle builtHalftoneRectangle) {
        this.beginFrame(drawContext);
        this.enqueue(builtHalftoneRectangle);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedRectangles.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("HalftoneRectangleParamsArray", gpuBuffer);
        }
    }

    public boolean isHalftoneRectanglePipeline(RenderPipeline renderPipeline) {
        return renderPipeline == HALFTONE_RECTANGLE_PIPELINE;
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 49152L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_rect_halftone_params", 136, 49152L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltHalftoneRectangle> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(49152);
        for (int i = 0; i < list.size(); ++i) {
            BuiltHalftoneRectangle builtHalftoneRectangle = list.get(i);
            int n = i * 8 * 4 * 4;
            byteBuffer.putFloat(n, builtHalftoneRectangle.radiusTopLeft());
            byteBuffer.putFloat(n + 4, builtHalftoneRectangle.radiusTopRight());
            byteBuffer.putFloat(n + 8, builtHalftoneRectangle.radiusBottomRight());
            byteBuffer.putFloat(n + 12, builtHalftoneRectangle.radiusBottomLeft());
            byteBuffer.putFloat(n + 16, builtHalftoneRectangle.width());
            byteBuffer.putFloat(n + 20, builtHalftoneRectangle.height());
            byteBuffer.putFloat(n + 24, builtHalftoneRectangle.smoothness());
            byteBuffer.putFloat(n + 28, builtHalftoneRectangle.dotSize());
            HalftoneRectangleRenderer.putColor(byteBuffer, n + 32, builtHalftoneRectangle.colorTopLeft());
            HalftoneRectangleRenderer.putColor(byteBuffer, n + 48, builtHalftoneRectangle.colorTopRight());
            HalftoneRectangleRenderer.putColor(byteBuffer, n + 64, builtHalftoneRectangle.colorBottomRight());
            HalftoneRectangleRenderer.putColor(byteBuffer, n + 80, builtHalftoneRectangle.colorBottomLeft());
            HalftoneRectangleRenderer.putColor(byteBuffer, n + 96, builtHalftoneRectangle.dotColor());
            byteBuffer.putFloat(n + 112, builtHalftoneRectangle.dotSpacing());
            byteBuffer.putFloat(n + 116, builtHalftoneRectangle.x());
            byteBuffer.putFloat(n + 120, builtHalftoneRectangle.y());
            byteBuffer.putFloat(n + 124, 0.0f);
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
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_rect_halftone_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

