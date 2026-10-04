package rtx.nv.utils.render.render2d.outline.outline360;
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
import rtx.nv.utils.render.render2d.outline.outline360.BuiltOutline360;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360Range;
import rtx.nv.utils.render.render2d.outline.outline360.Outline360RenderState;

public final class Outline360Renderer
implements AutoCloseable {
    private static final int MAX_OUTLINES = 256;
    private static final int MAX_RANGES = 1024;
    private static final int PARAMS_PER_OUTLINE = 4;
    private static final int PARAMS_PER_RANGE = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int OUTLINE_UNIFORM_BYTES = 16384;
    private static final int RANGE_UNIFORM_BYTES = 49152;
    private static volatile Outline360Renderer instance;
    public static final RenderPipeline OUTLINE_360_PIPELINE;
    private final List<BuiltOutline360> preparedOutlines = new ArrayList<BuiltOutline360>(64);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private GpuBuffer rangesBuffer;
    private boolean paramsDirty = true;

    private Outline360Renderer() {
    }

    static {
        OUTLINE_360_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(Outline360Renderer.id("pipeline/outline_360")).withVertexShader(Outline360Renderer.id("core/outline_360")).withFragmentShader(Outline360Renderer.id("core/outline_360")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("Outline360ParamsArray", UniformType.UNIFORM_BUFFER).withUniform("Outline360RangesArray", UniformType.UNIFORM_BUFFER).build();
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
    public static Outline360Renderer getInstance() {
        Outline360Renderer outline360Renderer = instance;
        if (outline360Renderer != null) return outline360Renderer;
        Class<Outline360Renderer> clazz = Outline360Renderer.class;
        synchronized (Outline360Renderer.class) {
            outline360Renderer = instance;
            if (outline360Renderer != null) return outline360Renderer;
            instance = outline360Renderer = new Outline360Renderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return outline360Renderer;
        }
    }

    @Override
    public void close() {
        this.preparedOutlines.clear();
        this.activeGraphics = null;
        this.closeBuffers();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltOutline360 builtOutline360) {
        this.submit(this.activeGraphics, builtOutline360);
    }

    public void submit(DrawContext drawContext, BuiltOutline360 builtOutline360) {
        if (drawContext == null || builtOutline360 == null || !builtOutline360.visible()) {
            return;
        }
        try {
            BuiltOutline360 builtOutline3602 = this.normalize(builtOutline360);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new Outline360RenderState(matrix3x2f, builtOutline3602));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltOutline360 normalize(BuiltOutline360 builtOutline360) {
        float f = Math.max(0.0f, Math.min(builtOutline360.width(), builtOutline360.height()) * 0.5f);
        float f2 = Math.max(0.0f, Math.min(builtOutline360.width(), builtOutline360.height()) * 0.5f);
        return new BuiltOutline360(builtOutline360.x(), builtOutline360.y(), builtOutline360.width(), builtOutline360.height(), Outline360Renderer.clamp(builtOutline360.radiusTopLeft(), 0.0f, f), Outline360Renderer.clamp(builtOutline360.radiusTopRight(), 0.0f, f), Outline360Renderer.clamp(builtOutline360.radiusBottomRight(), 0.0f, f), Outline360Renderer.clamp(builtOutline360.radiusBottomLeft(), 0.0f, f), Outline360Renderer.clamp(builtOutline360.thickness(), 0.0f, f2), builtOutline360.defaultColor(), Math.max(builtOutline360.smoothness(), 0.0f), Math.max(builtOutline360.blendDegrees(), 0.0f), builtOutline360.angleOffsetDegrees(), builtOutline360.ranges());
    }

    int reserve(BuiltOutline360 builtOutline360) {
        int n = this.preparedOutlines.size();
        if (n == 256) {
            return -1;
        }
        this.preparedOutlines.add(builtOutline360);
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
        Outline360Renderer outline360Renderer = instance;
        if (outline360Renderer != null) {
            outline360Renderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedOutlines.clear();
        this.paramsDirty = false;
    }

    public void prepareBuffers() {
        if (this.preparedOutlines.isEmpty() || !this.paramsDirty) {
            return;
        }
        if (!this.ensureWritableBuffers()) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(16384);
            ByteBuffer byteBuffer2 = memoryStack.calloc(49152);
            this.buildUniformData(byteBuffer, byteBuffer2, this.preparedOutlines);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.paramsBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(this.rangesBuffer.slice(0L, (long)byteBuffer2.remaining()), byteBuffer2);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public boolean isOutline360Pipeline(RenderPipeline renderPipeline) {
        return renderPipeline == OUTLINE_360_PIPELINE;
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltOutline360 builtOutline360) {
        this.beginFrame(drawContext);
        this.enqueue(builtOutline360);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedOutlines.isEmpty()) {
            return;
        }
        this.ensureBuffers();
        if (this.paramsBuffer != null) {
            renderPass.setUniform("Outline360ParamsArray", this.paramsBuffer);
        }
        if (this.rangesBuffer != null) {
            renderPass.setUniform("Outline360RangesArray", this.rangesBuffer);
        }
    }

    private void buildUniformData(ByteBuffer byteBuffer, ByteBuffer byteBuffer2, List<BuiltOutline360> list) {
        int n = 0;
        for (int i = 0; i < list.size(); ++i) {
            BuiltOutline360 builtOutline360 = list.get(i);
            int n2 = i * 4 * 4 * 4;
            int n3 = Math.min(builtOutline360.ranges().size(), 1024 - n);
            byteBuffer.putFloat(n2, builtOutline360.radiusTopLeft());
            byteBuffer.putFloat(n2 + 4, builtOutline360.radiusTopRight());
            byteBuffer.putFloat(n2 + 8, builtOutline360.radiusBottomRight());
            byteBuffer.putFloat(n2 + 12, builtOutline360.radiusBottomLeft());
            byteBuffer.putFloat(n2 + 16, builtOutline360.width());
            byteBuffer.putFloat(n2 + 20, builtOutline360.height());
            byteBuffer.putFloat(n2 + 24, builtOutline360.thickness());
            byteBuffer.putFloat(n2 + 28, builtOutline360.smoothness());
            Outline360Renderer.putColor(byteBuffer, n2 + 32, builtOutline360.defaultColor());
            byteBuffer.putFloat(n2 + 48, n);
            byteBuffer.putFloat(n2 + 52, n3);
            byteBuffer.putFloat(n2 + 56, builtOutline360.blendDegrees());
            byteBuffer.putFloat(n2 + 60, builtOutline360.angleOffsetDegrees());
            for (int j = 0; j < n3; ++j) {
                Outline360Range outline360Range = builtOutline360.ranges().get(j);
                int n4 = (n + j) * 3 * 4 * 4;
                byteBuffer2.putFloat(n4, outline360Range.startDegrees());
                byteBuffer2.putFloat(n4 + 4, outline360Range.endDegrees());
                byteBuffer2.putFloat(n4 + 8, outline360Range.blendStartDegrees());
                byteBuffer2.putFloat(n4 + 12, outline360Range.blendEndDegrees());
                Outline360Renderer.putColor(byteBuffer2, n4 + 16, outline360Range.color());
                Outline360Renderer.putColor(byteBuffer2, n4 + 32, outline360Range.colorEnd());
            }
            n += n3;
        }
        byteBuffer.position(0);
        byteBuffer2.position(0);
    }

    private void ensureBuffers() {
        if (!this.paramsDirty && this.paramsBuffer != null && this.rangesBuffer != null) {
            return;
        }
        this.prepareBuffers();
        if (!this.paramsDirty && this.paramsBuffer != null && this.rangesBuffer != null) {
            return;
        }
        this.closeBuffers();
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(16384);
            ByteBuffer byteBuffer2 = memoryStack.calloc(49152);
            this.buildUniformData(byteBuffer, byteBuffer2, this.preparedOutlines);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_360_params", 128, byteBuffer);
            this.rangesBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_360_ranges", 128, byteBuffer2);
            this.paramsDirty = false;
        }
    }

    private void closeBuffers() {
        if (this.paramsBuffer != null) {
            this.paramsBuffer.close();
            this.paramsBuffer = null;
        }
        if (this.rangesBuffer != null) {
            this.rangesBuffer.close();
            this.rangesBuffer = null;
        }
    }

    private boolean ensureWritableBuffers() {
        boolean bl;
        boolean bl2 = this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 16384L;
        boolean bl3 = bl = this.rangesBuffer != null && !this.rangesBuffer.isClosed() && this.rangesBuffer.size() >= 49152L;
        if (bl2 && bl) {
            return true;
        }
        this.closeBuffers();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_360_params", 136, 16384L);
            this.rangesBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_outline_360_ranges", 136, 49152L);
            return true;
        }
        catch (RuntimeException runtimeException) {
            this.closeBuffers();
            return false;
        }
    }
}

