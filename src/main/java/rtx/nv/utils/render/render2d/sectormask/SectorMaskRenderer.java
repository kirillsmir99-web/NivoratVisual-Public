package rtx.nv.utils.render.render2d.sectormask;
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
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.utils.render.render2d.sectormask.BuiltSectorMask;
import rtx.nv.utils.render.render2d.sectormask.SectorMaskRenderState;

public final class SectorMaskRenderer
implements AutoCloseable {
    private static final int MAX_MASKS = 8;
    private static final int PARAMS_PER_MASK = 5;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 640;
    private static volatile SectorMaskRenderer instance;
    public static final RenderPipeline SECTOR_MASK_PIPELINE;
    private final List<BuiltSectorMask> preparedMasks = new ArrayList<BuiltSectorMask>(8);
    private GpuBuffer paramsBuffer;
    private boolean paramsDirty = true;

    private SectorMaskRenderer() {
    }

    static {
        SECTOR_MASK_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(SectorMaskRenderer.id("pipeline/sector_mask")).withVertexShader(SectorMaskRenderer.id("ui/sectormask/sectormask")).withFragmentShader(SectorMaskRenderer.id("ui/sectormask/sectormask")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("SectorMaskParamsArray", UniformType.UNIFORM_BUFFER).build();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public static SectorMaskRenderer getInstance() {
        SectorMaskRenderer sectorMaskRenderer = instance;
        if (sectorMaskRenderer != null) return sectorMaskRenderer;
        Class<SectorMaskRenderer> clazz = SectorMaskRenderer.class;
        synchronized (SectorMaskRenderer.class) {
            sectorMaskRenderer = instance;
            if (sectorMaskRenderer != null) return sectorMaskRenderer;
            instance = sectorMaskRenderer = new SectorMaskRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return sectorMaskRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedMasks.clear();
        this.closeParamsBuffer();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void submit(DrawContext drawContext, BuiltSectorMask builtSectorMask) {
        if (drawContext == null || builtSectorMask == null || !builtSectorMask.visible()) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = rtx.nv.utils.render.render2d.Render2DCoordinateSpace.pose(drawContext);
            ((rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new SectorMaskRenderState(matrix3x2f, builtSectorMask, rtx.nv.utils.render.scissor.ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
        }
    }

    public void submit(GuiRenderState guiRenderState, Matrix3x2f matrix3x2f, BuiltSectorMask builtSectorMask, ScreenRect screenRect) {
        if (guiRenderState == null || builtSectorMask == null || !builtSectorMask.visible()) {
            return;
        }
        try {
            guiRenderState.addSimpleElement((SimpleGuiElementRenderState)new SectorMaskRenderState(matrix3x2f, builtSectorMask, screenRect));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    int reserve(BuiltSectorMask builtSectorMask) {
        int n = this.preparedMasks.size();
        if (n == 8) {
            return -1;
        }
        this.preparedMasks.add(builtSectorMask);
        this.paramsDirty = true;
        return n;
    }

    public static void closeInstance() {
        SectorMaskRenderer sectorMaskRenderer = instance;
        if (sectorMaskRenderer != null) {
            sectorMaskRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedMasks.clear();
        this.paramsDirty = false;
    }

    public boolean isSectorMaskPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == SECTOR_MASK_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedMasks.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedMasks);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedMasks.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("SectorMaskParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 640L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_sector_mask_params", 136, 640L);
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

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltSectorMask> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(640);
        for (int i = 0; i < list.size(); ++i) {
            BuiltSectorMask builtSectorMask = list.get(i);
            int n = i * 5 * 4 * 4;
            byteBuffer.putFloat(n, builtSectorMask.outerRadius());
            byteBuffer.putFloat(n + 4, builtSectorMask.innerRadius());
            byteBuffer.putFloat(n + 8, builtSectorMask.sectorCount());
            byteBuffer.putFloat(n + 12, builtSectorMask.gapRadians());
            byteBuffer.putFloat(n + 16, builtSectorMask.corner());
            byteBuffer.putFloat(n + 20, Math.max(builtSectorMask.feather(), 0.2f));
            byteBuffer.putFloat(n + 24, builtSectorMask.size() * 0.5f);
            byteBuffer.putFloat(n + 28, builtSectorMask.alpha());
            byteBuffer.putFloat(n + 32, builtSectorMask.hoverIndex());
            byteBuffer.putFloat(n + 36, builtSectorMask.hoverGrow());
            byteBuffer.putFloat(n + 40, builtSectorMask.hoverShrink());
            byteBuffer.putFloat(n + 44, builtSectorMask.ringRadius());
            byteBuffer.putFloat(n + 48, builtSectorMask.cellWidth());
            byteBuffer.putFloat(n + 52, builtSectorMask.cellHeight());
            byteBuffer.putFloat(n + 56, builtSectorMask.columns());
            byteBuffer.putFloat(n + 60, builtSectorMask.rows());
            byteBuffer.putFloat(n + 64, Math.max(builtSectorMask.hoverZoom(), 0.05f));
            byteBuffer.putFloat(n + 68, 0.0f);
            byteBuffer.putFloat(n + 72, 0.0f);
            byteBuffer.putFloat(n + 76, 0.0f);
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedMasks);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_sector_mask_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }
}

