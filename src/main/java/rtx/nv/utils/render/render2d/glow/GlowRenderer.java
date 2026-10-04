package rtx.nv.utils.render.render2d.glow;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.render2d.ClientPalette;
import rtx.nv.utils.render.render2d.ClientSplits;
import rtx.nv.utils.render.render2d.GradientSweep;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.glow.BuiltGlow;
import rtx.nv.utils.render.render2d.glow.GlowCapture;
import rtx.nv.utils.render.render2d.glow.GlowRenderState;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class GlowRenderer
implements AutoCloseable {
    private static final float TILE_SCALE = 1.0f;
    private static final int ATLAS_WIDTH = 2048;
    private static final int ATLAS_MAX_HEIGHT = 2048;
    private static final int ATLAS_HEIGHT_BUCKET = 256;
    private static final int TILE_GAP = 8;
    private static final int MAX_GLOWS = 224;
    private static final int PARAMS_PER_GLOW = 5;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 17920;
    private static final int KAWASE_UNIFORM_BYTES = 48;
    private static final int MAX_BLUR_ITERATIONS = 4;
    private static final int MAX_SPANS = 64;
    private static final int PALETTE_BASE_VEC4 = 70;
    private static final int PALETTE_VEC4 = 8;
    private static final int SHAPE_PARAMS_VEC4 = 78;
    private static final int SHAPE_BYTES = 1248;
    private static volatile GlowRenderer instance;
    public static final RenderPipeline GLOW_COMPOSITE_PIPELINE;
    private static final RenderPipeline GLOW_SHAPE_PIPELINE;
    private static final RenderPipeline GLOW_SOURCE_PIPELINE;
    private static final RenderPipeline GLOW_BLUR_DOWN_PIPELINE;
    private static final RenderPipeline GLOW_BLUR_UP_PIPELINE;
    private static final RenderPipeline GLOW_CUTOUT_PIPELINE;
    private final List<SimpleFramebuffer[]> downPool = new ArrayList<SimpleFramebuffer[]>(224);
    private final List<SimpleFramebuffer[]> upPool = new ArrayList<SimpleFramebuffer[]>(224);
    private final List<SimpleFramebuffer> shapeTargets = new ArrayList<SimpleFramebuffer>(224);
    private final List<SimpleFramebuffer> blurTargets = new ArrayList<SimpleFramebuffer>(224);
    private final List<SimpleFramebuffer> resultTargets = new ArrayList<SimpleFramebuffer>(224);
    private final List<PendingGlow> pendingGlows = new ArrayList<PendingGlow>(32);
    private final List<BuiltGlow> preparedGlows = new ArrayList<BuiltGlow>(32);
    private final List<GlowCapture> preparedCaptures = new ArrayList<GlowCapture>(32);
    private int[] tileX = new int[224];
    private int[] tileY = new int[224];
    private int[] tileW = new int[224];
    private int[] tileH = new int[224];
    private DrawContext activeGraphics;
    private GpuBuffer paramsBuffer;
    private GpuBuffer kawaseParamsBuffer;
    private GpuBuffer fullscreenQuadBuffer;
    private boolean paramsDirty = true;
    private boolean globalCompositeSubmitted;
    private final GlowCapture globalCapture = new GlowCapture();

    private GlowRenderer() {
    }

    static {
        GLOW_COMPOSITE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlowRenderer.id("pipeline/glow_composite")).withVertexShader(GlowRenderer.id("ui/glow/glow_composite")).withFragmentShader(GlowRenderer.id("ui/glow/glow_composite")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(new BlendFunction(SourceFactor.SRC_ALPHA, DestFactor.ONE, SourceFactor.ZERO, DestFactor.ONE)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER).build();
        GLOW_SHAPE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlowRenderer.id("pipeline/glow_shape")).withVertexShader(GlowRenderer.id("ui/glow/glow_shape")).withFragmentShader(GlowRenderer.id("ui/glow/glow_shape")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER).withUniform("SplitParams", UniformType.UNIFORM_BUFFER).build();
        GLOW_SOURCE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlowRenderer.id("pipeline/glow_source")).withVertexShader(GlowRenderer.id("ui/glow/glow_source")).withFragmentShader(GlowRenderer.id("ui/glow/glow_shape")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("GlowParamsArray", UniformType.UNIFORM_BUFFER).withUniform("SplitParams", UniformType.UNIFORM_BUFFER).build();
        GLOW_BLUR_DOWN_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlowRenderer.id("pipeline/glow_blur_down")).withVertexShader(GlowRenderer.id("ui/glow/glow_blur")).withFragmentShader(GlowRenderer.id("ui/glow/glow_blur_down")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).build();
        GLOW_BLUR_UP_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlowRenderer.id("pipeline/glow_blur_up")).withVertexShader(GlowRenderer.id("ui/glow/glow_blur")).withFragmentShader(GlowRenderer.id("ui/glow/glow_blur_up")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).build();
        GLOW_CUTOUT_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GlowRenderer.id("pipeline/glow_cutout")).withVertexShader(GlowRenderer.id("ui/glow/glow_blur")).withFragmentShader(GlowRenderer.id("ui/glow/glow_cutout")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withSampler("Sampler1").withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).build();
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
    public static GlowRenderer getInstance() {
        GlowRenderer glowRenderer = instance;
        if (glowRenderer != null) return glowRenderer;
        Class<GlowRenderer> clazz = GlowRenderer.class;
        synchronized (GlowRenderer.class) {
            glowRenderer = instance;
            if (glowRenderer != null) return glowRenderer;
            instance = glowRenderer = new GlowRenderer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return glowRenderer;
        }
    }

    @Override
    public void close() {
        this.preparedGlows.clear();
        this.activeGraphics = null;
        this.closeParamsBuffer();
        this.closeKawaseParamsBuffer();
        if (this.fullscreenQuadBuffer != null) {
            this.fullscreenQuadBuffer.close();
            this.fullscreenQuadBuffer = null;
        }
        for (SimpleFramebuffer simpleFramebuffer : this.shapeTargets) {
            if (simpleFramebuffer == null) continue;
            simpleFramebuffer.delete();
        }
        for (SimpleFramebuffer simpleFramebuffer : this.blurTargets) {
            if (simpleFramebuffer == null) continue;
            simpleFramebuffer.delete();
        }
        for (SimpleFramebuffer simpleFramebuffer : this.resultTargets) {
            if (simpleFramebuffer == null) continue;
            simpleFramebuffer.delete();
        }
        this.shapeTargets.clear();
        this.blurTargets.clear();
        this.resultTargets.clear();
        GlowRenderer.destroyPool(this.downPool);
        GlowRenderer.destroyPool(this.upPool);
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltGlow builtGlow) {
        this.submit(this.activeGraphics, builtGlow);
    }

    private void submit(DrawContext drawContext, BuiltGlow builtGlow) {
        if (drawContext == null || builtGlow == null || !builtGlow.visible()) {
            return;
        }
        try {
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            GlowCapture glowCapture = new GlowCapture();
            if (this.pendingGlows.size() < 224) {
                glowCapture.index = this.pendingGlows.size();
                this.pendingGlows.add(new PendingGlow(builtGlow, glowCapture, matrix3x2f));
            }
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new GlowRenderState(matrix3x2f, builtGlow, ScissorUtil.current(), glowCapture));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    int reserve(BuiltGlow builtGlow, GlowCapture glowCapture) {
        int n = this.preparedGlows.size();
        if (n >= 224) {
            return -1;
        }
        this.preparedGlows.add(builtGlow);
        this.preparedCaptures.add(glowCapture);
        this.paramsDirty = true;
        return n;
    }

    public static void closeInstance() {
        GlowRenderer glowRenderer = instance;
        if (glowRenderer != null) {
            glowRenderer.close();
            instance = null;
        }
    }

    public void beginGuiFrame() {
        this.preparedGlows.clear();
        this.preparedCaptures.clear();
        this.paramsDirty = false;
        this.globalCompositeSubmitted = false;
    }

    public void preparePending() {
        int n;
        int n2;
        if (this.pendingGlows.isEmpty()) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.getWindow() == null) {
            this.pendingGlows.clear();
            return;
        }
        int n3 = this.packTiles(2048);
        int n4 = 2048;
        int n5 = Math.max(256, Math.min(2048, (n3 + 256 - 1) / 256 * 256));
        SimpleFramebuffer simpleFramebuffer = this.ensureShapeTarget(0, n4, n5);
        SimpleFramebuffer simpleFramebuffer2 = this.ensureBlurTarget(0, n4, n5);
        SimpleFramebuffer simpleFramebuffer3 = this.ensureResultTarget(0, n4, n5);
        if (simpleFramebuffer == null || simpleFramebuffer2 == null || simpleFramebuffer3 == null) {
            this.pendingGlows.clear();
            return;
        }
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        this.clearTarget(simpleFramebuffer);
        float f = 0.0f;
        for (n2 = 0; n2 < this.pendingGlows.size(); ++n2) {
            PendingGlow pendingGlow = this.pendingGlows.get(n2);
            if (this.tileW[n2] > 0 && this.tileH[n2] > 0) {
                this.renderSourceShape(n2, pendingGlow.glow(), pendingGlow.capture(), simpleFramebuffer, this.tileX[n2], this.tileY[n2], this.tileW[n2], this.tileH[n2], n4, n5);
            } else {
                pendingGlow.capture().regionU0 = 0.0f;
                pendingGlow.capture().regionV0 = 0.0f;
                pendingGlow.capture().regionUW = 0.0f;
                pendingGlow.capture().regionVH = 0.0f;
            }
            f = Math.max(f, pendingGlow.glow().radius());
        }
        n2 = 3;
        float f2 = GlowRenderer.clamp(f / 20.0f, 0.5f, 4.0f) * 1.0f;
        this.ensureScratchTargets(0, n4, n5, n2);
        SimpleFramebuffer[] simpleFramebufferArray = this.downPool.get(0);
        SimpleFramebuffer[] simpleFramebufferArray2 = this.upPool.get(0);
        GpuTextureView gpuTextureView = simpleFramebuffer.getColorAttachmentView();
        int n6 = simpleFramebuffer.textureWidth;
        int n7 = simpleFramebuffer.textureHeight;
        for (n = 0; n < n2; ++n) {
            this.renderKawasePass(GLOW_BLUR_DOWN_PIPELINE, gpuTextureView, n6, n7, simpleFramebufferArray[n], f2, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
            gpuTextureView = simpleFramebufferArray[n].getColorAttachmentView();
            n6 = simpleFramebufferArray[n].textureWidth;
            n7 = simpleFramebufferArray[n].textureHeight;
        }
        for (n = n2 - 2; n >= 0; --n) {
            this.renderKawasePass(GLOW_BLUR_UP_PIPELINE, gpuTextureView, n6, n7, simpleFramebufferArray2[n], f2, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
            gpuTextureView = simpleFramebufferArray2[n].getColorAttachmentView();
            n6 = simpleFramebufferArray2[n].textureWidth;
            n7 = simpleFramebufferArray2[n].textureHeight;
        }
        this.renderKawasePass(GLOW_BLUR_UP_PIPELINE, gpuTextureView, n6, n7, simpleFramebuffer2, f2, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
        this.renderCutoutPass(simpleFramebuffer2, simpleFramebuffer, simpleFramebuffer3, gpuSampler);
        TextureSetup textureSetup = TextureSetup.of((GpuTextureView)simpleFramebuffer3.getColorAttachmentView(), (GpuSampler)gpuSampler);
        for (int i = 0; i < this.pendingGlows.size(); ++i) {
            this.pendingGlows.get((int)i).capture().setup = textureSetup;
        }
        this.pendingGlows.clear();
    }

    public boolean isGlowPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == GLOW_COMPOSITE_PIPELINE;
    }

    public void prepareBuffers() {
        if (this.preparedGlows.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack);
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

    public void draw(DrawContext drawContext, BuiltGlow builtGlow) {
        this.beginFrame(drawContext);
        this.enqueue(builtGlow);
        this.flush();
    }

    public void bindParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedGlows.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("GlowParamsArray", gpuBuffer);
        }
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 17920L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glow_params", 136, 17920L);
            return this.paramsBuffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private BuiltBuffer buildFullscreenQuad(BufferAllocator bufferAllocator) {
        BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
        bufferBuilder.vertex(-1.0f, -1.0f, 0.0f);
        bufferBuilder.vertex(-1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, 1.0f, 0.0f);
        bufferBuilder.vertex(1.0f, -1.0f, 0.0f);
        return bufferBuilder.end();
    }

    private GpuBuffer ensureKawaseParamsBuffer() {
        if (this.kawaseParamsBuffer != null && !this.kawaseParamsBuffer.isClosed() && this.kawaseParamsBuffer.size() >= 48L) {
            return this.kawaseParamsBuffer;
        }
        this.closeKawaseParamsBuffer();
        try {
            this.kawaseParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glow_kawase_params", 136, 48L);
            return this.kawaseParamsBuffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private void ensureScratchTargets(int n, int n2, int n3, int n4) {
        while (this.downPool.size() <= n) {
            this.downPool.add(new SimpleFramebuffer[4]);
            this.upPool.add(new SimpleFramebuffer[4]);
        }
        SimpleFramebuffer[] simpleFramebufferArray = this.downPool.get(n);
        SimpleFramebuffer[] simpleFramebufferArray2 = this.upPool.get(n);
        int n5 = Math.max(n2, 1);
        int n6 = Math.max(n3, 1);
        for (int i = 0; i < n4; ++i) {
            simpleFramebufferArray[i] = this.ensureTarget(simpleFramebufferArray[i], "nv_glow_down_" + n + "_" + i, n5, n6);
            if (i < n4 - 1) {
                simpleFramebufferArray2[i] = this.ensureTarget(simpleFramebufferArray2[i], "nv_glow_up_" + n + "_" + i, n5, n6);
            }
            n5 = Math.max(n5 / 2, 1);
            n6 = Math.max(n6 / 2, 1);
        }
    }

    private void closeKawaseParamsBuffer() {
        if (this.kawaseParamsBuffer != null) {
            this.kawaseParamsBuffer.close();
            this.kawaseParamsBuffer = null;
        }
    }

    private void closeParamsBuffer() {
        if (this.paramsBuffer != null) {
            this.paramsBuffer.close();
            this.paramsBuffer = null;
        }
    }

    private ByteBuffer buildUniformData(MemoryStack memoryStack) {
        int n = Math.max(1, this.preparedGlows.size());
        int n2 = n * 5 * 4 * 4;
        ByteBuffer byteBuffer = memoryStack.calloc(n2);
        for (int i = 0; i < this.preparedGlows.size(); ++i) {
            BuiltGlow builtGlow = this.preparedGlows.get(i);
            GlowCapture glowCapture = i < this.preparedCaptures.size() ? this.preparedCaptures.get(i) : null;
            float f = glowCapture != null ? glowCapture.regionU0 : 0.0f;
            float f2 = glowCapture != null ? glowCapture.regionV0 : 0.0f;
            float f3 = glowCapture != null ? glowCapture.regionUW : 1.0f;
            float f4 = glowCapture != null ? glowCapture.regionVH : 1.0f;
            GlowRenderer.writeGlowParams(byteBuffer, i, builtGlow, f, f2, f3, f4);
        }
        byteBuffer.position(0);
        return byteBuffer;
    }

    private SimpleFramebuffer ensureResultTarget(int n, int n2, int n3) {
        while (this.resultTargets.size() <= n) {
            this.resultTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = this.ensureTarget(this.resultTargets.get(n), "nv_glow_result_" + n, n2, n3);
        this.resultTargets.set(n, simpleFramebuffer);
        return simpleFramebuffer;
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
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack);
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glow_params", 128, byteBuffer);
            this.paramsDirty = false;
            GpuBuffer gpuBuffer = this.paramsBuffer;
            return gpuBuffer;
        }
    }

    private SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int n, int n2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, n, n2, false);
        }
        if (simpleFramebuffer.textureWidth != n || simpleFramebuffer.textureHeight != n2) {
            simpleFramebuffer.resize(n, n2);
        }
        return simpleFramebuffer;
    }

    private GpuBuffer ensureFullscreenQuadBuffer() {
        if (this.fullscreenQuadBuffer != null) {
            return this.fullscreenQuadBuffer;
        }
        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());
             BuiltBuffer builtBuffer = this.buildFullscreenQuad(bufferAllocator);){
            this.fullscreenQuadBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glow_fullscreen_quad", 32, builtBuffer.getBuffer());
        }
        return this.fullscreenQuadBuffer;
    }

    private void renderKawasePass(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, int n, int n2, SimpleFramebuffer simpleFramebuffer, float f, float f2, float f3, float f4, float f5, GpuSampler gpuSampler) {
        if (gpuTextureView == null || simpleFramebuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
        if (gpuBuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer2 = this.ensureKawaseParamsBuffer();
        if (gpuBuffer2 == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
        GpuBuffer gpuBuffer3 = shapeIndexBuffer.getIndexBuffer(6);
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, f2);
            byteBuffer.putFloat(4, f3);
            byteBuffer.putFloat(8, f4);
            byteBuffer.putFloat(12, f5);
            byteBuffer.putFloat(16, f / (float)Math.max(n, 1));
            byteBuffer.putFloat(20, f / (float)Math.max(n2, 1));
            byteBuffer.putFloat(32, 0.0f);
            byteBuffer.putFloat(36, 0.0f);
            byteBuffer.putFloat(40, 0.0f);
            byteBuffer.putFloat(44, 0.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv_glow_kawase", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty())) {
            renderPass.setPipeline(renderPipeline);
            renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
            renderPass.setUniform("KawaseParams", gpuBuffer2);
            renderPass.setVertexBuffer(0, gpuBuffer);
            renderPass.setIndexBuffer(gpuBuffer3, shapeIndexBuffer.getIndexType());
            renderPass.drawIndexed(0, 0, 6, 1);
        }
    }

    private static void writeShapeSpans(ByteBuffer byteBuffer, BuiltGlow builtGlow) {
        float[] fArray = builtGlow.spans();
        int n = builtGlow.spanCount();
        if (fArray == null || n <= 0) {
            return;
        }
        float f = builtGlow.effectivePad();
        float f2 = Math.min(Math.min(builtGlow.radii()[0], builtGlow.radii()[1]), Math.min(builtGlow.radii()[2], builtGlow.radii()[3]));
        int n2 = 80;
        byteBuffer.putFloat(n2, Math.min(n, 64));
        byteBuffer.putFloat(n2 + 4, f2);
        byteBuffer.putFloat(n2 + 8, builtGlow.leftAligned() ? 1.0f : 0.0f);
        byteBuffer.putFloat(n2 + 12, builtGlow.bottomAnchored() ? 0.0f : 1.0f);
        int n3 = n2 + 16;
        int n4 = Math.min(n, 64);
        float f3 = builtGlow.height() + f * 2.0f;
        for (int i = 0; i < n4; ++i) {
            int n5 = i * 4;
            float f4 = fArray[n5] + f;
            float f5 = fArray[n5 + 1] + f;
            float f6 = fArray[n5 + 2] + f;
            float f7 = fArray[n5 + 3] + f;
            float f8 = f3 - f7;
            float f9 = f3 - f6;
            int n6 = n3 + i * 4 * 4;
            byteBuffer.putFloat(n6, f4);
            byteBuffer.putFloat(n6 + 4, f5);
            byteBuffer.putFloat(n6 + 8, f8);
            byteBuffer.putFloat(n6 + 12, f9);
        }
    }

    private SimpleFramebuffer ensureBlurTarget(int n, int n2, int n3) {
        while (this.blurTargets.size() <= n) {
            this.blurTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = this.ensureTarget(this.blurTargets.get(n), "nv_glow_blur_" + n, n2, n3);
        this.blurTargets.set(n, simpleFramebuffer);
        return simpleFramebuffer;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderSourceShape(int n, BuiltGlow builtGlow, GlowCapture glowCapture, SimpleFramebuffer simpleFramebuffer, int n2, int n3, int n4, int n5, int n6, int n7) {
        if (simpleFramebuffer == null) {
            return;
        }
        float f = builtGlow.effectivePad();
        float f2 = Math.min((builtGlow.width() + f * 2.0f) * 1.0f, (float)n4);
        float f3 = Math.min((builtGlow.height() + f * 2.0f) * 1.0f, (float)n5);
        glowCapture.regionU0 = (float)n2 / (float)n6;
        glowCapture.regionV0 = (float)n3 / (float)n7;
        glowCapture.regionUW = Math.max(f2 / (float)n6, 1.0E-6f);
        glowCapture.regionVH = Math.max(f3 / (float)n7, 1.0E-6f);
        float f4 = (float)n2 / (float)n6 * 2.0f - 1.0f;
        float f5 = (float)n3 / (float)n7 * 2.0f - 1.0f;
        float f6 = ((float)n2 + f2) / (float)n6 * 2.0f - 1.0f;
        float f7 = ((float)n3 + f3) / (float)n7 * 2.0f - 1.0f;
        GpuBuffer gpuBuffer = null;
        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize())) {
            BufferBuilder bufferBuilder = new BufferBuilder(bufferAllocator, VertexFormat.DrawMode.QUADS, VertexFormats.POSITION);
            bufferBuilder.vertex(f4, f5, 0.0f);
            bufferBuilder.vertex(f4, f7, 0.0f);
            bufferBuilder.vertex(f6, f7, 0.0f);
            bufferBuilder.vertex(f6, f5, 0.0f);
            try (BuiltBuffer builtBuffer = bufferBuilder.end()) {
                gpuBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glow_src_quad_" + n, 32, builtBuffer.getBuffer());
            }
        } catch (RuntimeException runtimeException) {
            return;
        }
        if (gpuBuffer == null) {
            return;
        }
        GpuBuffer paramsGpuBuffer = null;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(1248);
            GlowRenderer.writeGlowParams(byteBuffer, 0, builtGlow, 0.0f, 0.0f, 1.0f, 1.0f);
            byteBuffer.putFloat(48, builtGlow.splitIndex());
            GlowRenderer.writeShapeSpans(byteBuffer, builtGlow);
            GlowRenderer.writePalette(byteBuffer, builtGlow);
            byteBuffer.position(0);
            paramsGpuBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_glow_src_params_" + n, 136, byteBuffer);
        } catch (RuntimeException runtimeException) {
            gpuBuffer.close();
            return;
        }
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
            GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(6);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv_glow_source_" + n, simpleFramebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty());){
                renderPass.setPipeline(GLOW_SOURCE_PIPELINE);
                renderPass.setUniform("GlowParamsArray", paramsGpuBuffer);
                GpuBuffer gpuBuffer3 = ClientSplits.buffer();
                if (gpuBuffer3 != null) {
                    renderPass.setUniform("SplitParams", gpuBuffer3);
                }
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                renderPass.drawIndexed(0, 0, 6, 1);
            }
        } finally {
            if (paramsGpuBuffer != null) paramsGpuBuffer.close();
            gpuBuffer.close();
        }
    }

    private void clearTarget(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv_glow_clear", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty());
        if (renderPass != null) {
            renderPass.close();
        }
    }

    private static void destroyPool(List<SimpleFramebuffer[]> list) {
        for (SimpleFramebuffer[] simpleFramebufferArray : list) {
            for (int i = 0; i < simpleFramebufferArray.length; ++i) {
                if (simpleFramebufferArray[i] == null) continue;
                simpleFramebufferArray[i].delete();
                simpleFramebufferArray[i] = null;
            }
        }
        list.clear();
    }

    private void renderCutoutPass(SimpleFramebuffer simpleFramebuffer, SimpleFramebuffer simpleFramebuffer2, SimpleFramebuffer simpleFramebuffer3, GpuSampler gpuSampler) {
        if (simpleFramebuffer == null || simpleFramebuffer2 == null || simpleFramebuffer3 == null) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
        GpuBuffer gpuBuffer2 = this.ensureKawaseParamsBuffer();
        if (gpuBuffer == null || gpuBuffer2 == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
        GpuBuffer gpuBuffer3 = shapeIndexBuffer.getIndexBuffer(6);
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, 0.0f);
            byteBuffer.putFloat(4, 0.0f);
            byteBuffer.putFloat(8, 1.0f);
            byteBuffer.putFloat(12, 1.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(gpuBuffer2.slice(0L, 48L), byteBuffer);
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv_glow_cutout", simpleFramebuffer3.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty())) {
            renderPass.setPipeline(GLOW_CUTOUT_PIPELINE);
            renderPass.bindTexture("Sampler0", simpleFramebuffer.getColorAttachmentView(), gpuSampler);
            renderPass.bindTexture("Sampler1", simpleFramebuffer2.getColorAttachmentView(), gpuSampler);
            renderPass.setUniform("KawaseParams", gpuBuffer2);
            renderPass.setVertexBuffer(0, gpuBuffer);
            renderPass.setIndexBuffer(gpuBuffer3, shapeIndexBuffer.getIndexType());
            renderPass.drawIndexed(0, 0, 6, 1);
        }
    }

    private static void writeGlowParams(ByteBuffer byteBuffer, int n, BuiltGlow builtGlow, float f, float f2, float f3, float f4) {
        int n2 = n * 5 * 4 * 4;
        byteBuffer.putFloat(n2, builtGlow.radii()[0]);
        byteBuffer.putFloat(n2 + 4, builtGlow.radii()[1]);
        byteBuffer.putFloat(n2 + 8, builtGlow.radii()[2]);
        byteBuffer.putFloat(n2 + 12, builtGlow.radii()[3]);
        byteBuffer.putFloat(n2 + 16, builtGlow.width());
        byteBuffer.putFloat(n2 + 20, builtGlow.height());
        byteBuffer.putFloat(n2 + 24, builtGlow.effectivePad());
        byteBuffer.putFloat(n2 + 28, 2.0f);
        int n3 = builtGlow.color();
        byteBuffer.putFloat(n2 + 32, (float)(n3 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 36, (float)(n3 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 40, (float)(n3 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 44, builtGlow.intensity() * builtGlow.alpha());
        byteBuffer.putFloat(n2 + 48, f);
        byteBuffer.putFloat(n2 + 52, f2);
        byteBuffer.putFloat(n2 + 56, f3);
        byteBuffer.putFloat(n2 + 60, f4);
        int n4 = builtGlow.secondColor();
        byteBuffer.putFloat(n2 + 64, (float)(n4 >>> 16 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 68, (float)(n4 >>> 8 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 72, (float)(n4 & 0xFF) / 255.0f);
        byteBuffer.putFloat(n2 + 76, builtGlow.colorOffset());
    }

    private SimpleFramebuffer ensureShapeTarget(int n, int n2, int n3) {
        while (this.shapeTargets.size() <= n) {
            this.shapeTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = this.ensureTarget(this.shapeTargets.get(n), "nv_glow_shape_" + n, n2, n3);
        this.shapeTargets.set(n, simpleFramebuffer);
        return simpleFramebuffer;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void renderShape(int n, BuiltGlow builtGlow, SimpleFramebuffer simpleFramebuffer) {
        GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
        if (gpuBuffer == null || simpleFramebuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer2 = null;
        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
            ByteBuffer byteBuffer = memoryStack.calloc(1248);
            GlowRenderer.writeGlowParams(byteBuffer, 0, builtGlow, 0.0f, 0.0f, 1.0f, 1.0f);
            byteBuffer.putFloat(48, builtGlow.splitIndex());
            GlowRenderer.writeShapeSpans(byteBuffer, builtGlow);
            GlowRenderer.writePalette(byteBuffer, builtGlow);
            byteBuffer.position(0);
            gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "nv_glow_shape_params_" + n, 136, byteBuffer);
        } catch (RuntimeException runtimeException) {
            return;
        }
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
            GpuBuffer gpuBuffer3 = shapeIndexBuffer.getIndexBuffer(6);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv_glow_shape_" + n, simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty());){
                renderPass.setPipeline(GLOW_SHAPE_PIPELINE);
                renderPass.setUniform("GlowParamsArray", gpuBuffer2);
                GpuBuffer gpuBuffer4 = ClientSplits.buffer();
                if (gpuBuffer4 != null) {
                    renderPass.setUniform("SplitParams", gpuBuffer4);
                }
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer3, shapeIndexBuffer.getIndexType());
                renderPass.drawIndexed(0, 0, 6, 1);
            }
        } finally {
            if (gpuBuffer2 != null) gpuBuffer2.close();
        }
    }

    private static void writePalette(ByteBuffer byteBuffer, BuiltGlow builtGlow) {
        int n = 1120;
        int n6 = Math.max(1, Math.min(ClientPalette.count(), 6));
        int[] nArray = ClientPalette.colors();
        byteBuffer.putFloat(n, n6);
        byteBuffer.putFloat(n + 4, ClientPalette.phase());
        byteBuffer.putFloat(n + 8, ClientPalette.styleId());
        byteBuffer.putFloat(n + 12, GradientSweep.progress());
        int n7 = n + 112;
        byteBuffer.putFloat(n7, ClientPalette.prevStyle());
        byteBuffer.putFloat(n7 + 4, ClientPalette.closed());
        for (int i = 0; i < n6 && i < nArray.length; ++i) {
            int n8 = nArray[i] & 0xFFFFFF;
            int n9 = n + (1 + i) * 4 * 4;
            byteBuffer.putFloat(n9, (float)(n8 >>> 16 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n9 + 4, (float)(n8 >>> 8 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n9 + 8, (float)(n8 & 0xFF) / 255.0f);
            byteBuffer.putFloat(n9 + 12, 1.0f);
        }
    }

    private int packTiles(int n) {
        int n2;
        int n3 = this.pendingGlows.size();
        if (this.tileX.length < n3) {
            this.tileX = new int[n3];
            this.tileY = new int[n3];
            this.tileW = new int[n3];
            this.tileH = new int[n3];
        }
        float f = 8.0f;
        for (n2 = 0; n2 < n3; ++n2) {
            f = Math.max(f, this.pendingGlows.get(n2).glow().effectivePad());
        }
        int n4 = n2 = Math.max(8, Math.round(f * 1.0f));
        int n5 = n2;
        int n6 = 0;
        for (int i = 0; i < n3; ++i) {
            BuiltGlow builtGlow = this.pendingGlows.get(i).glow();
            float f2 = builtGlow.effectivePad();
            int n7 = Math.max(Math.round((builtGlow.width() + f2 * 2.0f) * 1.0f), 1);
            int n8 = Math.max(Math.round((builtGlow.height() + f2 * 2.0f) * 1.0f), 1);
            if (n4 + n7 + n2 > n) {
                n4 = n2;
                n5 += n6 + n2;
                n6 = 0;
            }
            if (n7 + n2 * 2 > n || n5 + n8 + n2 > 2048) {
                this.tileW[i] = 0;
                this.tileH[i] = 0;
                continue;
            }
            this.tileX[i] = n4;
            this.tileY[i] = n5;
            this.tileW[i] = n7;
            this.tileH[i] = n8;
            n4 += n7 + n2;
            n6 = Math.max(n6, n8);
        }
        return n5 + n6 + n2;
    }

    public static record PendingGlow(BuiltGlow glow, GlowCapture capture, Matrix3x2f pose) {}
}

