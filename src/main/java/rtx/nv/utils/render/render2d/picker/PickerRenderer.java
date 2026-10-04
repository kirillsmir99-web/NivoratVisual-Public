package rtx.nv.utils.render.render2d.picker;
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
import rtx.nv.utils.render.render2d.picker.BuiltPicker;
import rtx.nv.utils.render.render2d.picker.PickerRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class PickerRenderer
implements AutoCloseable {
    private static final int MAX_PICKERS = 256;
    private static final int PARAMS_PER_PICKER = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 12288;
    private static volatile PickerRenderer instance;
    public static final RenderPipeline PICKER_PIPELINE;
    private final List<BuiltPicker> preparedPickers = new ArrayList<BuiltPicker>(64);
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private PickerRenderer() {
    }

    static {
        PICKER_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(PickerRenderer.id("pipeline/picker")).withVertexShader(PickerRenderer.id("core/picker")).withFragmentShader(PickerRenderer.id("core/picker")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("PickerParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    public void flush() {
        this.activeGraphics = null;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static PickerRenderer getInstance() {
        PickerRenderer pickerRenderer = instance;
        if (pickerRenderer != null) return pickerRenderer;
        Class<PickerRenderer> clazz = PickerRenderer.class;
        synchronized (PickerRenderer.class) {
            pickerRenderer = instance;
            if (pickerRenderer != null) return pickerRenderer;
            instance = pickerRenderer = new PickerRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return pickerRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedPickers.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltPicker builtPicker) {
        this.submit(this.activeGraphics, builtPicker);
    }

    private void submit(DrawContext drawContext, BuiltPicker builtPicker) {
        if (drawContext == null || builtPicker == null || !builtPicker.visible()) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new PickerRenderState(matrix3x2f, builtPicker, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    int reserve(BuiltPicker builtPicker) {
        int n = this.preparedPickers.size();
        if (n == 256) {
            return -1;
        }
        this.preparedPickers.add(builtPicker);
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
        PickerRenderer pickerRenderer = instance;
        if (pickerRenderer != null) {
            pickerRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedPickers.clear();
        this.paramsDirty = false;
    }

    public boolean isPickerPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == PICKER_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedPickers.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedPickers);
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

    public void draw(DrawContext drawContext, BuiltPicker builtPicker) {
        this.beginFrame(drawContext);
        this.enqueue(builtPicker);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedPickers.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("PickerParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 12288L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_picker_params", 136, 12288L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltPicker> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(12288);
        for (int i = 0; i < list.size(); ++i) {
            BuiltPicker builtPicker = list.get(i);
            int n = i * 3 * 4 * 4;
            byteBuffer.putFloat(n, builtPicker.mode());
            byteBuffer.putFloat(n + 4, Math.max(builtPicker.radius(), 0.0f));
            byteBuffer.putFloat(n + 8, Math.max(builtPicker.smoothness(), 0.5f));
            byteBuffer.putFloat(n + 12, Math.max(0.0f, Math.min(1.0f, builtPicker.alpha())));
            byteBuffer.putFloat(n + 16, builtPicker.w());
            byteBuffer.putFloat(n + 20, builtPicker.h());
            byteBuffer.putFloat(n + 24, Math.max(builtPicker.checkerPx(), 1.0f));
            byteBuffer.putFloat(n + 28, 0.0f);
            PickerRenderer.putColor(byteBuffer, n + 32, builtPicker.solidColor());
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedPickers);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_picker_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

