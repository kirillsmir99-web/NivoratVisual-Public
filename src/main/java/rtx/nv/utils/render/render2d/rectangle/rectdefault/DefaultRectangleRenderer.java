package rtx.nv.utils.render.render2d.rectangle.rectdefault;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.others.RoundedScissor;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.BuiltRectangle;
import rtx.nv.utils.render.render2d.rectangle.rectdefault.DefaultRectangleRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class DefaultRectangleRenderer
implements AutoCloseable {
    private static final int PAGE_SIZE = 448;
    private static final int MAX_PAGES = 16;
    private static final int PARAMS_PER_RECTANGLE = 9;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int PAGE_BYTES = 64512;
    private static volatile DefaultRectangleRenderer instance;
    static final RenderPipeline[] PIPELINES;
    public static final RenderPipeline RECTANGLE_PIPELINE;
    private final List<List<BuiltRectangle>> pages = new ArrayList<List<BuiltRectangle>>();
    private final GpuBuffer[] pageBuffers = new GpuBuffer[16];
    private final boolean[] pageDirty = new boolean[16];
    private int totalCount;
    private int boundPage;
    private DrawContext activeGraphics;

    private DefaultRectangleRenderer() {
    }

    static {
        PIPELINES = DefaultRectangleRenderer.buildPipelines();
        RECTANGLE_PIPELINE = PIPELINES[0];
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
    public static DefaultRectangleRenderer getInstance() {
        DefaultRectangleRenderer defaultRectangleRenderer = instance;
        if (defaultRectangleRenderer != null) return defaultRectangleRenderer;
        Class<DefaultRectangleRenderer> clazz = DefaultRectangleRenderer.class;
        synchronized (DefaultRectangleRenderer.class) {
            defaultRectangleRenderer = instance;
            if (defaultRectangleRenderer != null) return defaultRectangleRenderer;
            instance = defaultRectangleRenderer = new DefaultRectangleRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return defaultRectangleRenderer;
        }
    }

    @Override
    public void close() {
        for (List<BuiltRectangle> list : this.pages) {
            list.clear();
        }
        this.activeGraphics = null;
        for (int i = 0; i < this.pageBuffers.length; ++i) {
            if (this.pageBuffers[i] == null) continue;
            this.pageBuffers[i].close();
            this.pageBuffers[i] = null;
        }
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltRectangle builtRectangle) {
        this.submit(this.activeGraphics, builtRectangle);
    }

    private void submit(DrawContext drawContext, BuiltRectangle builtRectangle) {
        if (drawContext == null || builtRectangle == null || !builtRectangle.visible()) {
            return;
        }
        try {
            BuiltRectangle builtRectangle2 = this.normalize(builtRectangle);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new DefaultRectangleRenderState(matrix3x2f, builtRectangle2, ScissorUtil.current()));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltRectangle normalize(BuiltRectangle builtRectangle) {
        boolean bl = builtRectangle.smoothness() < 0.0f;
        float f = bl ? Float.MAX_VALUE : Math.max(0.0f, Math.max(builtRectangle.width(), builtRectangle.height()) * 0.5f);
        float f2 = 0.0f;
        float f3 = 0.0f;
        float f4 = 0.0f;
        float f5 = 0.0f;
        float f6 = 0.0f;
        float f7 = 0.0f;
        float f8 = 0.0f;
        float f9 = 0.0f;
        float f10 = 1.0f;
        float f11 = 0.0f;
        if (RoundedScissor.isEnabled()) {
            f10 = RoundedScissor.cos();
            f11 = RoundedScissor.sin();
            f2 = RoundedScissor.x();
            f3 = RoundedScissor.y();
            f4 = RoundedScissor.width();
            f5 = RoundedScissor.height();
            f6 = RoundedScissor.radiusTopLeft();
            f7 = RoundedScissor.radiusTopRight();
            f8 = RoundedScissor.radiusBottomRight();
            f9 = RoundedScissor.radiusBottomLeft();
        } else {
            f2 = builtRectangle.scissorX();
            f3 = builtRectangle.scissorY();
            f4 = builtRectangle.scissorWidth();
            f5 = builtRectangle.scissorHeight();
            f6 = builtRectangle.scissorRadiusTopLeft();
            f7 = builtRectangle.scissorRadiusTopRight();
            f8 = builtRectangle.scissorRadiusBottomRight();
            f9 = builtRectangle.scissorRadiusBottomLeft();
        }
        return new BuiltRectangle(builtRectangle.x(), builtRectangle.y(), builtRectangle.width(), builtRectangle.height(), DefaultRectangleRenderer.clamp(builtRectangle.radiusTopLeft(), 0.0f, f), DefaultRectangleRenderer.clamp(builtRectangle.radiusTopRight(), 0.0f, f), DefaultRectangleRenderer.clamp(builtRectangle.radiusBottomRight(), 0.0f, f), DefaultRectangleRenderer.clamp(builtRectangle.radiusBottomLeft(), 0.0f, f), builtRectangle.colorTopLeft(), builtRectangle.colorTopRight(), builtRectangle.colorBottomRight(), builtRectangle.colorBottomLeft(), builtRectangle.smoothness(), f2, f3, f4, f5, f6, f7, f8, f9, builtRectangle.paletteMode(), builtRectangle.paletteTint(), builtRectangle.paletteAlpha(), f10, f11);
    }

    int reserve(BuiltRectangle builtRectangle) {
        int n = this.totalCount;
        if (n >= 7168) {
            return -1;
        }
        int n2 = n / 448;
        while (this.pages.size() <= n2) {
            this.pages.add(new ArrayList(448));
        }
        this.pages.get(n2).add(builtRectangle);
        this.pageDirty[n2] = true;
        this.totalCount = n + 1;
        return n;
    }

    private static void putColor(ByteBuffer byteBuffer, int n, int n2) {
        byteBuffer.putFloat(n, (float)(n2 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 4, (float)(n2 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 8, (float)(n2 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n + 12, (float)(n2 >>> 24 & 0xFF) / 255.0f);
    }

    public static void closeInstance() {
        DefaultRectangleRenderer defaultRectangleRenderer = instance;
        if (defaultRectangleRenderer != null) {
            defaultRectangleRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        for (List<BuiltRectangle> list : this.pages) {
            list.clear();
        }
        Arrays.fill(this.pageDirty, false);
        this.totalCount = 0;
        this.boundPage = 0;
    }

    public boolean isRectanglePipeline(RenderPipeline renderPipeline) {
        for (int i = 0; i < PIPELINES.length; ++i) {
            if (renderPipeline != PIPELINES[i]) continue;
            this.boundPage = i;
            return true;
        }
        return false;
    }

    public void prepareBuffers() {
        for (int i = 0; i < this.pages.size(); ++i) {
            if (!this.pageDirty[i] || this.pages.get(i).isEmpty()) continue;
            this.ensurePageBuffer(i);
        }
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltRectangle builtRectangle) {
        this.beginFrame(drawContext);
        this.enqueue(builtRectangle);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        GpuBuffer gpuBuffer;
        if (renderPass == null) {
            return;
        }
        int n = this.boundPage;
        if (n < 0 || n >= this.pages.size() || this.pages.get(n).isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer2 = this.ensurePageBuffer(n);
        if (gpuBuffer2 != null) {
            renderPass.setUniform("RectangleParamsArray", gpuBuffer2);
        }
        if ((gpuBuffer = ClientPalette.buffer()) != null) {
            renderPass.setUniform("PaletteParams", gpuBuffer);
        }
    }

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BuiltRectangle> list) {
        ByteBuffer byteBuffer = memoryStack.calloc(64512);
        for (int i = 0; i < list.size(); ++i) {
            BuiltRectangle builtRectangle = list.get(i);
            int n = i * 9 * 4 * 4;
            byteBuffer.putFloat(n, builtRectangle.radiusTopLeft());
            byteBuffer.putFloat(n + 4, builtRectangle.radiusTopRight());
            byteBuffer.putFloat(n + 8, builtRectangle.radiusBottomRight());
            byteBuffer.putFloat(n + 12, builtRectangle.radiusBottomLeft());
            byteBuffer.putFloat(n + 16, builtRectangle.width());
            byteBuffer.putFloat(n + 20, builtRectangle.height());
            byteBuffer.putFloat(n + 24, builtRectangle.smoothness());
            byteBuffer.putFloat(n + 28, builtRectangle.paletteMode());
            if (builtRectangle.paletteMode() != 0) {
                byteBuffer.putFloat(n + 32, builtRectangle.paletteTint());
                byteBuffer.putFloat(n + 36, builtRectangle.paletteAlpha());
            } else {
                DefaultRectangleRenderer.putColor(byteBuffer, n + 32, builtRectangle.colorTopLeft());
                DefaultRectangleRenderer.putColor(byteBuffer, n + 48, builtRectangle.colorTopRight());
                DefaultRectangleRenderer.putColor(byteBuffer, n + 64, builtRectangle.colorBottomRight());
                DefaultRectangleRenderer.putColor(byteBuffer, n + 80, builtRectangle.colorBottomLeft());
            }
            byteBuffer.putFloat(n + 96, builtRectangle.scissorX());
            byteBuffer.putFloat(n + 100, builtRectangle.scissorY());
            byteBuffer.putFloat(n + 104, builtRectangle.scissorWidth());
            byteBuffer.putFloat(n + 108, builtRectangle.scissorHeight());
            byteBuffer.putFloat(n + 112, builtRectangle.scissorRadiusTopLeft());
            byteBuffer.putFloat(n + 116, builtRectangle.scissorRadiusTopRight());
            byteBuffer.putFloat(n + 120, builtRectangle.scissorRadiusBottomRight());
            byteBuffer.putFloat(n + 124, builtRectangle.scissorRadiusBottomLeft());
            byteBuffer.putFloat(n + 128, builtRectangle.scissorCos());
            byteBuffer.putFloat(n + 132, builtRectangle.scissorSin());
            byteBuffer.putFloat(n + 136, 0.0f);
            byteBuffer.putFloat(n + 140, 0.0f);
        }
        byteBuffer.position(0);
        return byteBuffer;
    }

    private GpuBuffer ensurePageBuffer(int n) {
        GpuBuffer gpuBuffer = this.pageBuffers[n];
        if (gpuBuffer == null || gpuBuffer.isClosed() || gpuBuffer.size() < 64512L) {
            if (gpuBuffer != null && !gpuBuffer.isClosed()) {
                gpuBuffer.close();
            }
            try {
                gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_rect_default_params", 136, 64512L);
            }
            catch (RuntimeException runtimeException) {
                this.pageBuffers[n] = null;
                return null;
            }
            this.pageBuffers[n] = gpuBuffer;
            this.pageDirty[n] = true;
        }
        if (this.pageDirty[n]) {
            try (MemoryStack memoryStack = MemoryStack.stackPush();){
                ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.pages.get(n));
                RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
                this.pageDirty[n] = false;
            }
            catch (RuntimeException runtimeException) {
                this.pageDirty[n] = true;
            }
        }
        return gpuBuffer;
    }

    private static RenderPipeline[] buildPipelines() {
        RenderPipeline[] renderPipelineArray = new RenderPipeline[16];
        for (int i = 0; i < 16; ++i) {
            renderPipelineArray[i] = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(DefaultRectangleRenderer.id((String)(i == 0 ? "pipeline/rect_default" : "pipeline/rect_default_" + i))).withVertexShader(DefaultRectangleRenderer.id("core/rect_default")).withFragmentShader(DefaultRectangleRenderer.id("core/rect_default")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("RectangleParamsArray", UniformType.UNIFORM_BUFFER).withUniform("PaletteParams", UniformType.UNIFORM_BUFFER).build();
        }
        return renderPipelineArray;
    }

    static int pageOf(int n) {
        return n / 448;
    }

    static int localOf(int n) {
        return n % 448;
    }
}

