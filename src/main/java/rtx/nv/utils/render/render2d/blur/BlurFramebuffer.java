package rtx.nv.utils.render.render2d.blur;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
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
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.client.util.Window;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.lwjgl.system.MemoryStack;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.render2d.Render2DCoordinateSpace;
import rtx.nv.utils.render.render2d.blur.BlurCapture;
import rtx.nv.utils.render.render2d.blur.BlurRenderState;
import rtx.nv.utils.render.render2d.blur.BuiltBlur;
import rtx.nv.utils.render.scissor.ScissorUtil;

public final class BlurFramebuffer
implements AutoCloseable {
    private static final int MAX_BLUR_RECTS = 512;
    private static final int MAX_BLUR_ITERATIONS = 4;
    private static final int PARAMS_PER_RECT = 3;
    private static final int FLOATS_PER_PARAM = 4;
    private static final int UNIFORM_BYTES = 24576;
    private static final int KAWASE_UNIFORM_BYTES = 48;
    private static final float MAX_BLUR_RENDER_SCALE = 0.5f;
    private static volatile BlurFramebuffer instance;
    private static float skyFallbackRed;
    private static float skyFallbackGreen;
    private static float skyFallbackBlue;
    public static final RenderPipeline BATCHED_BLUR_PIPELINE;
    public static final RenderPipeline KAWASE_DOWN_PIPELINE;
    public static final RenderPipeline KAWASE_UP_PIPELINE;
    private static final int MAX_CAPTURES = 64;
    private static final int GLOBAL_SCRATCH_INDEX = 64;
    private static final int WORLD_GLOBAL_SCRATCH_INDEX = 65;
    private static boolean worldScopeActive;
    private final List<BlurFramebuffer.PendingBlur> pendingBlurs = new ArrayList<BlurFramebuffer.PendingBlur>(32);
    private final List<BlurFramebuffer.PreparedBlur> preparedBlurs = new ArrayList<BlurFramebuffer.PreparedBlur>(32);
    private DrawContext activeGraphics;
    private final List<SimpleFramebuffer> resultTargets = new ArrayList<SimpleFramebuffer>(64);
    private SimpleFramebuffer globalResult;
    private boolean globalBlurReady;
    private float globalBlurRadius;
    private SimpleFramebuffer worldGlobalResult;
    private boolean worldGlobalBlurReady;
    private float worldGlobalBlurRadius;
    private BlurFramebuffer.Region region;
    private BlurFramebuffer.Region worldGlobalRegion;
    private final List<SimpleFramebuffer[]> downPool = new ArrayList<SimpleFramebuffer[]>(64);
    private final List<SimpleFramebuffer[]> upPool = new ArrayList<SimpleFramebuffer[]>(64);
    private GpuBuffer fullscreenQuadBuffer;
    private GpuBuffer paramsBuffer;
    private GpuBuffer kawaseParamsBuffer;
    private boolean captureRequested;
    private boolean paramsDirty = true;
    private int preparedIterations;

    private BlurFramebuffer() {
    }

    static {
        skyFallbackRed = 0.55f;
        skyFallbackGreen = 0.65f;
        skyFallbackBlue = 0.78f;
        BATCHED_BLUR_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BlurFramebuffer.id("pipeline/batched_blur")).withVertexShader(BlurFramebuffer.id("ui/batched_blur/batched_blur")).withFragmentShader(BlurFramebuffer.id("ui/batched_blur/batched_blur")).withVertexFormat(VertexFormats.POSITION_COLOR_LINE_WIDTH, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withUniform("BlurParamsArray", UniformType.UNIFORM_BUFFER).build();
        KAWASE_DOWN_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BlurFramebuffer.id("pipeline/kawase_down")).withVertexShader(BlurFramebuffer.id("ui/kawase/down")).withFragmentShader(BlurFramebuffer.id("ui/kawase/down")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).build();
        KAWASE_UP_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BlurFramebuffer.id("pipeline/kawase_up")).withVertexShader(BlurFramebuffer.id("ui/kawase/down")).withFragmentShader(BlurFramebuffer.id("ui/kawase/up")).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withSampler("Sampler0").withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).build();
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
    public static BlurFramebuffer getInstance() {
        BlurFramebuffer blurFramebuffer = instance;
        if (blurFramebuffer != null) return blurFramebuffer;
        Class<BlurFramebuffer> clazz = BlurFramebuffer.class;
        synchronized (BlurFramebuffer.class) {
            blurFramebuffer = instance;
            if (blurFramebuffer != null) return blurFramebuffer;
            instance = blurFramebuffer = new BlurFramebuffer();
            // ** MonitorExit[var1_1] (shouldn't be in output)
            return blurFramebuffer;
        }
    }

    @Override
    public void close() {
        this.pendingBlurs.clear();
        this.preparedBlurs.clear();
        this.activeGraphics = null;
        this.captureRequested = false;
        this.closeParamsBuffer();
        this.closeKawaseParamsBuffer();
        for (SimpleFramebuffer simpleFramebuffer : this.resultTargets) {
            if (simpleFramebuffer == null) continue;
            simpleFramebuffer.delete();
        }
        this.resultTargets.clear();
        if (this.globalResult != null) {
            this.globalResult.delete();
            this.globalResult = null;
        }
        this.globalBlurReady = false;
        if (this.worldGlobalResult != null) {
            this.worldGlobalResult.delete();
            this.worldGlobalResult = null;
        }
        this.worldGlobalBlurReady = false;
        this.worldGlobalRegion = null;
        BlurFramebuffer.destroyScratchPool(this.downPool);
        BlurFramebuffer.destroyScratchPool(this.upPool);
        if (this.fullscreenQuadBuffer != null) {
            this.fullscreenQuadBuffer.close();
            this.fullscreenQuadBuffer = null;
        }
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void enqueue(BuiltBlur builtBlur) {
        this.submit(this.activeGraphics, builtBlur);
    }

    private void submit(DrawContext drawContext, BuiltBlur builtBlur) {
        if (builtBlur == null || !builtBlur.visible()) {
            return;
        }
        if (drawContext == null) {
            return;
        }
        try {
            BuiltBlur builtBlur2 = this.normalize(builtBlur);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            ScreenRect screenRect = ScissorUtil.current();
            BlurCapture blurCapture = new BlurCapture();
            this.pendingBlurs.add(new BlurFramebuffer.PendingBlur(builtBlur2, matrix3x2f, screenRect, blurCapture, false, worldScopeActive));
            ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState().addSimpleElement((SimpleGuiElementRenderState)new BlurRenderState(matrix3x2f, builtBlur2, screenRect, blurCapture));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    private BuiltBlur normalize(BuiltBlur builtBlur) {
        float f = Math.max(0.0f, Math.min(builtBlur.width(), builtBlur.height()) * 0.5f);
        float f2 = BlurFramebuffer.clamp(builtBlur.radiusTopLeft(), 0.0f, f);
        float f3 = BlurFramebuffer.clamp(builtBlur.radiusTopRight(), 0.0f, f);
        float f4 = BlurFramebuffer.clamp(builtBlur.radiusBottomRight(), 0.0f, f);
        float f5 = BlurFramebuffer.clamp(builtBlur.radiusBottomLeft(), 0.0f, f);
        float f6 = Math.max(0.0f, builtBlur.smoothness());
        float f7 = Math.max(0.0f, builtBlur.blurRadius());
        if (f2 == builtBlur.radiusTopLeft() && f3 == builtBlur.radiusTopRight() && f4 == builtBlur.radiusBottomRight() && f5 == builtBlur.radiusBottomLeft() && f6 == builtBlur.smoothness() && f7 == builtBlur.blurRadius()) {
            return builtBlur;
        }
        return new BuiltBlur(builtBlur.x(), builtBlur.y(), builtBlur.width(), builtBlur.height(), f2, f3, f4, f5, f6, f7, builtBlur.colorTopLeft(), builtBlur.colorTopRight(), builtBlur.colorBottomRight(), builtBlur.colorBottomLeft());
    }

    private static BlurFramebuffer.Region union(BlurFramebuffer.Region region, BlurFramebuffer.Region region2) {
        if (region == null) {
            return region2;
        }
        if (region2 == null) {
            return region;
        }
        int n = Math.min(region.x(), region2.x());
        int n2 = Math.min(region.y(), region2.y());
        int n3 = Math.max(region.x() + region.width(), region2.x() + region2.width());
        int n4 = Math.max(region.y() + region.height(), region2.y() + region2.height());
        return new BlurFramebuffer.Region(n, n2, n3 - n, n4 - n2);
    }

    int reserve(BuiltBlur builtBlur, BlurCapture blurCapture) {
        int n = this.preparedBlurs.size();
        if (n == 512) {
            return -1;
        }
        this.preparedBlurs.add(new BlurFramebuffer.PreparedBlur(builtBlur, blurCapture));
        this.paramsDirty = true;
        return n;
    }

    public void requestCapture(DrawContext drawContext, BuiltBlur builtBlur, BlurCapture blurCapture) {
        if (drawContext == null || builtBlur == null || blurCapture == null || !builtBlur.visible()) {
            return;
        }
        try {
            BuiltBlur builtBlur2 = this.normalize(builtBlur);
            Matrix3x2f matrix3x2f = Render2DCoordinateSpace.pose(drawContext);
            this.pendingBlurs.add(new BlurFramebuffer.PendingBlur(builtBlur2, matrix3x2f, ScissorUtil.current(), blurCapture, true, worldScopeActive));
        }
        catch (RuntimeException runtimeException) {
            // empty catch block
        }
    }

    public static void closeInstance() {
        BlurFramebuffer blurFramebuffer = instance;
        if (blurFramebuffer != null) {
            blurFramebuffer.close();
            instance = null;
        }
    }

    public boolean isBlurPipeline(RenderPipeline renderPipeline) {
        return renderPipeline == BATCHED_BLUR_PIPELINE;
    }

    public void beginGuiFrame() {
        this.preparedBlurs.clear();
        this.captureRequested = false;
        this.paramsDirty = false;
        this.preparedIterations = 0;
    }

    public void preparePending() {
        BlurCapture blurCapture;
        BlurFramebuffer.Region region;
        int n;
        Framebuffer framebuffer;
        if (this.pendingBlurs.isEmpty()) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            this.pendingBlurs.clear();
            return;
        }
        this.preparedIterations = this.computeIterations(0.0f);
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        this.globalBlurReady = false;
        this.worldGlobalBlurReady = false;
        this.worldGlobalRegion = null;
        float f = -1.0f;
        float f2 = -1.0f;
        BlurFramebuffer.Region blurFramebufferRegion2 = null;
        for (BlurFramebuffer.PendingBlur pendingBlur : this.pendingBlurs) {
            if (!pendingBlur.backdrop()) continue;
            if (pendingBlur.worldScope()) {
                if (f2 < 0.0f) {
                    f2 = pendingBlur.blur().blurRadius();
                }
                blurFramebufferRegion2 = BlurFramebuffer.union(blurFramebufferRegion2, this.computeRegionFor(pendingBlur, minecraftClient, framebuffer));
                continue;
            }
            if (!(f < 0.0f)) continue;
            f = pendingBlur.blur().blurRadius();
        }
        if (f >= 0.0f) {
            float f3 = this.computeRenderScale(f, framebuffer.textureWidth, framebuffer.textureHeight);
            int n2 = Math.max(Math.round((float)framebuffer.textureWidth * f3), 1);
            n = Math.max(Math.round((float)framebuffer.textureHeight * f3), 1);
            this.globalResult = this.ensureTarget(this.globalResult, "nv_blur_global", n2, n);
            if (this.globalResult != null && this.globalResult.getColorAttachmentView() != null) {
                this.globalBlurReady = true;
                this.globalBlurRadius = f;
                TextureSetup globalSetup = TextureSetup.of((GpuTextureView)(Object)this.globalResult.getColorAttachmentView(), (GpuSampler)gpuSampler);
                for (BlurFramebuffer.PendingBlur pendingBlur : this.pendingBlurs) {
                    if (!pendingBlur.backdrop() || pendingBlur.worldScope()) continue;
                    blurCapture = pendingBlur.capture();
                    blurCapture.regionX = 0.0f;
                    blurCapture.regionY = 0.0f;
                    blurCapture.regionW = framebuffer.textureWidth;
                    blurCapture.regionH = framebuffer.textureHeight;
                    blurCapture.setup = globalSetup;
                }
                this.captureRequested = true;
            }
        }
        if (f2 >= 0.0f && blurFramebufferRegion2 != null) {
            float f4 = this.computeRenderScale(f2, blurFramebufferRegion2.width(), blurFramebufferRegion2.height());
            int n3 = Math.max(Math.round((float)blurFramebufferRegion2.width() * f4), 1);
            n = Math.max(Math.round((float)blurFramebufferRegion2.height() * f4), 1);
            this.worldGlobalResult = this.ensureTarget(this.worldGlobalResult, "nv_blur_global_world", n3, n);
            if (this.worldGlobalResult != null && this.worldGlobalResult.getColorAttachmentView() != null) {
                this.worldGlobalBlurReady = true;
                this.worldGlobalBlurRadius = f2;
                this.worldGlobalRegion = blurFramebufferRegion2;
                TextureSetup worldGlobalSetup = TextureSetup.of((GpuTextureView)(Object)this.worldGlobalResult.getColorAttachmentView(), (GpuSampler)gpuSampler);
                for (BlurFramebuffer.PendingBlur pendingBlur : this.pendingBlurs) {
                    if (!pendingBlur.backdrop() || !pendingBlur.worldScope()) continue;
                    blurCapture = pendingBlur.capture();
                    blurCapture.regionX = blurFramebufferRegion2.x();
                    blurCapture.regionY = blurFramebufferRegion2.y();
                    blurCapture.regionW = blurFramebufferRegion2.width();
                    blurCapture.regionH = blurFramebufferRegion2.height();
                    blurCapture.setup = worldGlobalSetup;
                }
                this.captureRequested = true;
            }
        }
        for (int i = 0; i < this.pendingBlurs.size() && i < 64; ++i) {
            int n4;
            BlurFramebuffer.PendingBlur pendingBlur = this.pendingBlurs.get(i);
            if (pendingBlur.backdrop()) continue;
            BlurCapture blurCapture2 = pendingBlur.capture();
            region = this.computeRegionFor(pendingBlur, minecraftClient, framebuffer);
            if (region == null) continue;
            float f5 = this.computeRenderScale(pendingBlur.blur().blurRadius(), region.width(), region.height());
            int n5 = Math.max(Math.round((float)region.width() * f5), 1);
            SimpleFramebuffer simpleFramebuffer = this.ensureResultTarget(i, n5, n4 = Math.max(Math.round((float)region.height() * f5), 1));
            if (simpleFramebuffer == null || simpleFramebuffer.getColorAttachmentView() == null) continue;
            pendingBlur.setRegion(region);
            blurCapture2.regionX = region.x();
            blurCapture2.regionY = region.y();
            blurCapture2.regionW = region.width();
            blurCapture2.regionH = region.height();
            blurCapture2.setup = TextureSetup.of((GpuTextureView)simpleFramebuffer.getColorAttachmentView(), (GpuSampler)gpuSampler);
            this.captureRequested = true;
        }
    }

    public void prepareGuiDraw() {
        Framebuffer framebuffer;
        if (!this.captureRequested) {
            this.pendingBlurs.clear();
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            this.pendingBlurs.clear();
            return;
        }
        Framebuffer framebuffer3 = GuiLayerBlurRenderer.worldBackdropSource(framebuffer);
        if (this.globalBlurReady && this.globalResult != null) {
            this.renderBlurChain(64, framebuffer, this.globalResult, new BlurFramebuffer.Region(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), this.globalBlurRadius);
        }
        if (this.worldGlobalBlurReady && this.worldGlobalResult != null && this.worldGlobalRegion != null) {
            this.renderBlurChain(65, framebuffer3, this.worldGlobalResult, this.worldGlobalRegion, this.worldGlobalBlurRadius);
        }
        for (int i = 0; i < this.pendingBlurs.size() && i < 64; ++i) {
            SimpleFramebuffer simpleFramebuffer;
            BlurFramebuffer.PendingBlur pendingBlur = this.pendingBlurs.get(i);
            if (pendingBlur.backdrop()) continue;
            BlurFramebuffer.Region region = pendingBlur.region();
            SimpleFramebuffer simpleFramebuffer2 = simpleFramebuffer = i < this.resultTargets.size() ? this.resultTargets.get(i) : null;
            if (region == null || simpleFramebuffer == null) continue;
            this.renderBlurChain(i, pendingBlur.worldScope() ? framebuffer3 : framebuffer, simpleFramebuffer, region, pendingBlur.blur().blurRadius());
        }
        this.pendingBlurs.clear();
    }

    public void recaptureWorldBackdrop() {
        if (!this.worldGlobalBlurReady || this.worldGlobalResult == null || this.worldGlobalRegion == null || this.worldGlobalResult.getColorAttachmentView() == null) {
            return;
        }
        Framebuffer framebuffer = GuiLayerBlurRenderer.worldSnapshotWithPanels();
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        this.renderBlurChain(65, framebuffer, this.worldGlobalResult, this.worldGlobalRegion, this.worldGlobalBlurRadius);
    }

    public void recaptureBackdrop() {
        Framebuffer framebuffer;
        if (!this.globalBlurReady || this.globalResult == null || this.globalResult.getColorAttachmentView() == null) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient == null ? null : minecraftClient.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        this.renderBlurChain(64, framebuffer, this.globalResult, new BlurFramebuffer.Region(0, 0, framebuffer.textureWidth, framebuffer.textureHeight), this.globalBlurRadius);
    }

    public void prepareBuffers() {
        if (this.preparedBlurs.isEmpty() || !this.paramsDirty) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureWritableParamsBuffer();
        if (gpuBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedBlurs);
            RenderSystem.getDevice().createCommandEncoder().writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            this.paramsDirty = false;
        }
        catch (RuntimeException runtimeException) {
            this.paramsDirty = true;
        }
    }

    public void bindBlurParams(RenderPass renderPass) {
        if (renderPass == null || this.preparedBlurs.isEmpty()) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureParamsBuffer();
        if (gpuBuffer != null) {
            renderPass.setUniform("BlurParamsArray", gpuBuffer);
        }
    }

    public void beginFrame(DrawContext drawContext) {
        this.activeGraphics = drawContext;
    }

    public void draw(DrawContext drawContext, BuiltBlur builtBlur) {
        this.beginFrame(drawContext);
        this.enqueue(builtBlur);
        this.flush();
    }

    public static void setSkyFallbackColor(float f, float f2, float f3) {
        if (!(Float.isFinite(f) && Float.isFinite(f2) && Float.isFinite(f3))) {
            return;
        }
        skyFallbackRed = BlurFramebuffer.clamp(f, 0.0f, 1.0f);
        skyFallbackGreen = BlurFramebuffer.clamp(f2, 0.0f, 1.0f);
        skyFallbackBlue = BlurFramebuffer.clamp(f3, 0.0f, 1.0f);
    }

    private GpuBuffer ensureWritableParamsBuffer() {
        if (this.paramsBuffer != null && !this.paramsBuffer.isClosed() && this.paramsBuffer.size() >= 24576L) {
            return this.paramsBuffer;
        }
        this.closeParamsBuffer();
        try {
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_blur_params", 136, 24576L);
            return this.paramsBuffer;
        }
        catch (RuntimeException runtimeException) {
            return null;
        }
    }

    private int computeIterations(float f) {
        return 3;
    }

    private float computeRenderScale(float f, int n, int n2) {
        return 0.5f;
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
        if (this.kawaseParamsBuffer == null || this.kawaseParamsBuffer.isClosed() || this.kawaseParamsBuffer.size() < 48L) {
            this.closeKawaseParamsBuffer();
            this.kawaseParamsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_kawase_params", 136, 48L);
        }
        return this.kawaseParamsBuffer;
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
            simpleFramebufferArray[i] = this.ensureTarget(simpleFramebufferArray[i], "nv_blur_down_" + n + "_" + i, n5, n6);
            if (i < n4 - 1) {
                simpleFramebufferArray2[i] = this.ensureTarget(simpleFramebufferArray2[i], "nv_blur_up_" + n + "_" + i, n5, n6);
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

    private void renderBlurChain(int n, Framebuffer framebuffer, SimpleFramebuffer simpleFramebuffer, BlurFramebuffer.Region region, float f) {
        SimpleFramebuffer simpleFramebuffer2;
        int n2;
        if (simpleFramebuffer == null || region == null) {
            return;
        }
        int n3 = this.preparedIterations > 0 ? this.preparedIterations : this.computeIterations(f);
        this.ensureScratchTargets(n, simpleFramebuffer.textureWidth, simpleFramebuffer.textureHeight, n3);
        SimpleFramebuffer[] simpleFramebufferArray = this.downPool.get(n);
        SimpleFramebuffer[] simpleFramebufferArray2 = this.upPool.get(n);
        float f2 = BlurFramebuffer.clamp(f / 18.0f, 0.15f, 3.0f);
        GpuSampler gpuSampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
        GpuTextureView gpuTextureView = framebuffer.getColorAttachmentView();
        int n4 = framebuffer.textureWidth;
        int n5 = framebuffer.textureHeight;
        float f3 = (float)region.x() / (float)Math.max(framebuffer.textureWidth, 1);
        float f4 = (float)region.y() / (float)Math.max(framebuffer.textureHeight, 1);
        float f5 = (float)region.width() / (float)Math.max(framebuffer.textureWidth, 1);
        float f6 = (float)region.height() / (float)Math.max(framebuffer.textureHeight, 1);
        for (n2 = 0; n2 < n3; ++n2) {
            simpleFramebuffer2 = simpleFramebufferArray[n2];
            this.renderKawasePass(KAWASE_DOWN_PIPELINE, gpuTextureView, n4, n5, simpleFramebuffer2, f2, f3, f4, f5, f6, gpuSampler);
            gpuTextureView = simpleFramebuffer2.getColorAttachmentView();
            n4 = simpleFramebuffer2.textureWidth;
            n5 = simpleFramebuffer2.textureHeight;
            f3 = 0.0f;
            f4 = 0.0f;
            f5 = 1.0f;
            f6 = 1.0f;
        }
        for (n2 = n3 - 2; n2 >= 0; --n2) {
            simpleFramebuffer2 = simpleFramebufferArray2[n2];
            this.renderKawasePass(KAWASE_UP_PIPELINE, gpuTextureView, n4, n5, simpleFramebuffer2, f2, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
            gpuTextureView = simpleFramebuffer2.getColorAttachmentView();
            n4 = simpleFramebuffer2.textureWidth;
            n5 = simpleFramebuffer2.textureHeight;
        }
        this.renderKawasePass(KAWASE_UP_PIPELINE, gpuTextureView, n4, n5, simpleFramebuffer, f2, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
    }

    private void closeParamsBuffer() {
        if (this.paramsBuffer != null) {
            this.paramsBuffer.close();
            this.paramsBuffer = null;
        }
    }

    private ByteBuffer buildUniformData(MemoryStack memoryStack, List<BlurFramebuffer.PreparedBlur> list) {
        int n = Math.max(1, list.size()) * 3 * 4 * 4;
        ByteBuffer byteBuffer = memoryStack.calloc(n);
        for (int i = 0; i < list.size(); ++i) {
            BlurFramebuffer.PreparedBlur preparedBlur = list.get(i);
            BuiltBlur builtBlur = preparedBlur.blur();
            BlurCapture blurCapture = preparedBlur.capture();
            int n2 = i * 3 * 4 * 4;
            byteBuffer.putFloat(n2, builtBlur.radiusTopLeft());
            byteBuffer.putFloat(n2 + 4, builtBlur.radiusTopRight());
            byteBuffer.putFloat(n2 + 8, builtBlur.radiusBottomRight());
            byteBuffer.putFloat(n2 + 12, builtBlur.radiusBottomLeft());
            byteBuffer.putFloat(n2 + 16, builtBlur.width());
            byteBuffer.putFloat(n2 + 20, builtBlur.height());
            byteBuffer.putFloat(n2 + 24, builtBlur.smoothness());
            byteBuffer.putFloat(n2 + 28, builtBlur.blurRadius());
            byteBuffer.putFloat(n2 + 32, blurCapture.regionX);
            byteBuffer.putFloat(n2 + 36, blurCapture.regionY);
            byteBuffer.putFloat(n2 + 40, blurCapture.regionW);
            byteBuffer.putFloat(n2 + 44, blurCapture.regionH);
        }
        byteBuffer.position(0);
        return byteBuffer;
    }

    private BlurFramebuffer.Region computeRegionFor(BlurFramebuffer.PendingBlur pendingBlur, MinecraftClient minecraftClient, Framebuffer framebuffer) {
        Window window = minecraftClient.getWindow();
        int n = Math.max(window.getScaledWidth(), 1);
        int n2 = Math.max(window.getScaledHeight(), 1);
        float f = (float)framebuffer.textureWidth / (float)n;
        float f2 = (float)framebuffer.textureHeight / (float)n2;
        BuiltBlur builtBlur = pendingBlur.blur();
        float f3 = Render2DCoordinateSpace.toGui(Math.max(16.0f, builtBlur.blurRadius() * 2.0f));
        ScreenRect screenRect = new ScreenRect(Math.round(builtBlur.x()), Math.round(builtBlur.y()), Math.round(builtBlur.width()), Math.round(builtBlur.height())).transformEachVertex((Matrix3x2fc)pendingBlur.pose());
        if (pendingBlur.scissorArea() != null && (screenRect = pendingBlur.scissorArea().intersection(screenRect)) == null) {
            return null;
        }
        float f4 = BlurFramebuffer.clamp((float)screenRect.getLeft() - f3, 0.0f, n);
        float f5 = BlurFramebuffer.clamp((float)screenRect.getTop() - f3, 0.0f, n2);
        float f6 = BlurFramebuffer.clamp((float)screenRect.getRight() + f3, f4 + 1.0f, n);
        float f7 = BlurFramebuffer.clamp((float)screenRect.getBottom() + f3, f5 + 1.0f, n2);
        int n3 = 8;
        int n4 = BlurFramebuffer.clampInt(BlurFramebuffer.snapDown((int)Math.floor(f4 * f), n3), 0, framebuffer.textureWidth - 1);
        int n5 = BlurFramebuffer.clampInt(BlurFramebuffer.snapUp((int)Math.ceil(f6 * f), n3), n4 + 1, framebuffer.textureWidth);
        int n6 = BlurFramebuffer.clampInt(BlurFramebuffer.snapDown((int)Math.floor(((float)n2 - f7) * f2), n3), 0, framebuffer.textureHeight - 1);
        int n7 = BlurFramebuffer.clampInt(BlurFramebuffer.snapUp((int)Math.ceil(((float)n2 - f5) * f2), n3), n6 + 1, framebuffer.textureHeight);
        int n8 = Math.max(n5 - n4, 1);
        int n9 = Math.max(n7 - n6, 1);
        return new BlurFramebuffer.Region(n4, n6, n8, n9);
    }

    private SimpleFramebuffer ensureResultTarget(int n, int n2, int n3) {
        if (n2 <= 0 || n3 <= 0) {
            return null;
        }
        while (this.resultTargets.size() <= n) {
            this.resultTargets.add(null);
        }
        SimpleFramebuffer simpleFramebuffer = this.resultTargets.get(n);
        simpleFramebuffer = this.ensureTarget(simpleFramebuffer, "nv_blur_result_" + n, n2, n3);
        this.resultTargets.set(n, simpleFramebuffer);
        return simpleFramebuffer;
    }

    private GpuBuffer ensureParamsBuffer() {
        if (this.paramsDirty) {
            this.prepareBuffers();
        }
        if (!this.paramsDirty && this.paramsBuffer != null) {
            return this.paramsBuffer;
        }
        if (this.paramsDirty) {
            this.closeParamsBuffer();
            try (MemoryStack memoryStack = MemoryStack.stackPush();){
                ByteBuffer byteBuffer = this.buildUniformData(memoryStack, this.preparedBlurs);
                this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_blur_params", 136, byteBuffer);
            }
            this.paramsDirty = false;
            return this.paramsBuffer;
        }
        if (this.paramsBuffer == null || this.paramsBuffer.isClosed() || this.paramsBuffer.size() < 24576L) {
            this.closeParamsBuffer();
            this.paramsBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_blur_params", 136, 24576L);
        }
        return this.paramsBuffer;
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
        try (BufferAllocator bufferAllocator = new BufferAllocator(4 * VertexFormats.POSITION.getVertexSize());){
            GpuBuffer gpuBuffer;
            block12: {
                BuiltBuffer builtBuffer = this.buildFullscreenQuad(bufferAllocator);
                try {
                    gpuBuffer = this.fullscreenQuadBuffer = RenderSystem.getDevice().createBuffer(() -> "nv_blur_fullscreen_quad", 32, builtBuffer.getBuffer());
                    if (builtBuffer == null) break block12;
                }
                catch (Throwable throwable) {
                    if (builtBuffer != null) {
                        try {
                            builtBuffer.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                    }
                    throw throwable;
                }
                builtBuffer.close();
            }
            return gpuBuffer;
        }
    }

    private void renderKawasePass(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, int n, int n2, SimpleFramebuffer simpleFramebuffer, float f, float f2, float f3, float f4, float f5, GpuSampler gpuSampler) {
        if (gpuTextureView == null || simpleFramebuffer == null) {
            return;
        }
        GpuBuffer gpuBuffer = this.ensureFullscreenQuadBuffer();
        if (gpuBuffer == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
        GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(6);
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, f2);
            byteBuffer.putFloat(4, f3);
            byteBuffer.putFloat(8, f4);
            byteBuffer.putFloat(12, f5);
            byteBuffer.putFloat(16, f / (float)Math.max(n, 1));
            byteBuffer.putFloat(20, f / (float)Math.max(n2, 1));
            byteBuffer.putFloat(32, skyFallbackRed);
            byteBuffer.putFloat(36, skyFallbackGreen);
            byteBuffer.putFloat(40, skyFallbackBlue);
            byteBuffer.putFloat(44, 1.0f);
            byteBuffer.position(0);
            GpuBuffer gpuBuffer3 = this.ensureKawaseParamsBuffer();
            if (gpuBuffer3 == null) {
                return;
            }
            commandEncoder.writeToBuffer(gpuBuffer3.slice(0L, 48L), byteBuffer);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv_kawase_blur", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty());){
                renderPass.setPipeline(renderPipeline);
                renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                renderPass.setUniform("KawaseParams", gpuBuffer3);
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                renderPass.drawIndexed(0, 0, 6, 1);
            }
        }
    }

    private static void destroyScratchPool(List<SimpleFramebuffer[]> list) {
        for (SimpleFramebuffer[] simpleFramebufferArray : list) {
            for (int i = 0; i < simpleFramebufferArray.length; ++i) {
                if (simpleFramebufferArray[i] == null) continue;
                simpleFramebufferArray[i].delete();
                simpleFramebufferArray[i] = null;
            }
        }
        list.clear();
    }

    public static void endWorldScope() {
        worldScopeActive = false;
    }

    public static void beginWorldScope() {
        worldScopeActive = true;
    }

    private static int clampInt(int n, int n2, int n3) {
        return Math.max(n2, Math.min(n3, n));
    }

    private static int snapDown(int n, int n2) {
        return Math.floorDiv(n, n2) * n2;
    }

    private static int snapUp(int n, int n2) {
        return -Math.floorDiv(-n, n2) * n2;
    }


    public static final class PendingBlur {
        private final BuiltBlur blur;
        private final Matrix3x2f pose;
        private final ScreenRect scissorArea;
        private final BlurCapture capture;
        private final boolean backdrop;
        private final boolean worldScope;
        private BlurFramebuffer.Region region;
    
        private PendingBlur(BuiltBlur builtBlur, Matrix3x2f matrix3x2f, ScreenRect screenRect, BlurCapture blurCapture, boolean bl, boolean bl2) {
            this.blur = builtBlur;
            this.pose = matrix3x2f;
            this.scissorArea = screenRect;
            this.capture = blurCapture;
            this.backdrop = bl;
            this.worldScope = bl2;
        }
    
        BlurFramebuffer.Region region() {
            return this.region;
        }
    
        Matrix3x2f pose() {
            return this.pose;
        }
    
        ScreenRect scissorArea() {
            return this.scissorArea;
        }
    
        BuiltBlur blur() {
            return this.blur;
        }
    
        void setRegion(BlurFramebuffer.Region region) {
            this.region = region;
        }
    
        BlurCapture capture() {
            return this.capture;
        }
    
        boolean backdrop() {
            return this.backdrop;
        }
    
        boolean worldScope() {
            return this.worldScope;
        }
    }
    
        public static record PreparedBlur(BuiltBlur blur, BlurCapture capture) {
    }
    
        public static record Region(int x, int y, int width, int height) {
    }
}

