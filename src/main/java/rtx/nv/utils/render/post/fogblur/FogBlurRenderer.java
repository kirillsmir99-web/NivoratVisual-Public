package rtx.nv.utils.render.post.fogblur;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import rtx.nv.utils.render.others.RenderSampler;

public final class FogBlurRenderer {
    private static final int COMPOSITE_UNIFORM_SIZE = 48;
    private static final int KAWASE_UNIFORM_SIZE = 48;
    private static final int MAX_BLUR_ITERATIONS = 4;
    private static final Identifier KAWASE_DOWN_PIPELINE_ID = FogBlurRenderer.id("pipeline/post/fogblur/kawase_down");
    private static final Identifier KAWASE_UP_PIPELINE_ID = FogBlurRenderer.id("pipeline/post/fogblur/kawase_up");
    private static final Identifier COMPOSITE_PIPELINE_ID = FogBlurRenderer.id("pipeline/post/fogblur/composite");
    private static final Identifier KAWASE_VERTEX = FogBlurRenderer.id("post/fogblur/kawase");
    private static final Identifier KAWASE_DOWN_SHADER = FogBlurRenderer.id("ui/kawase/down");
    private static final Identifier KAWASE_UP_SHADER = FogBlurRenderer.id("ui/kawase/up");
    private static final Identifier COMPOSITE_SHADER = FogBlurRenderer.id("post/fogblur/composite");
    private static final float UNDERWATER_FAR = 32.0f;
    private static float fallbackRed = 0.55f;
    private static float fallbackGreen = 0.65f;
    private static float fallbackBlue = 0.78f;
    private static float tintRed = 0.0f;
    private static float tintGreen = 0.0f;
    private static float tintBlue = 0.0f;
    private static float tintMix = 0.0f;
    private static RenderPipeline kawaseDownPipeline;
    private static RenderPipeline kawaseUpPipeline;
    private static RenderPipeline compositePipeline;
    private static GpuBuffer compositeUniformBuffer;
    private static GpuBuffer kawaseUniformBuffer;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyTextureView;
    private static GpuTexture depthCopyTexture;
    private static GpuTextureView depthCopyTextureView;
    private static final SimpleFramebuffer[] downTargets;
    private static final SimpleFramebuffer[] upTargets;
    private static int sceneWidth;
    private static int sceneHeight;
    private static int blurWidth;
    private static int blurHeight;
    private static int allocatedPasses;
    private static boolean disabledAfterError;
    private static GpuTexture opaqueDepthTexture;
    private static GpuTextureView opaqueDepthTextureView;
    private static int opaqueDepthWidth;
    private static int opaqueDepthHeight;
    private static boolean opaqueCaptured;

    private FogBlurRenderer() {
    }

    static {
        downTargets = new SimpleFramebuffer[4];
        upTargets = new SimpleFramebuffer[4];
        sceneWidth = -1;
        sceneHeight = -1;
        blurWidth = -1;
        blurHeight = -1;
        opaqueDepthWidth = -1;
        opaqueDepthHeight = -1;
    }

    public static void clear() {
        FogBlurRenderer.closeTargets();
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public static void apply(Framebuffer framebuffer, float f, float f2, float f3, float f4, int n) {
        if (disabledAfterError || framebuffer == null || framebuffer.getColorAttachment() == null || framebuffer.getColorAttachmentView() == null || framebuffer.getDepthAttachment() == null) {
            return;
        }
        if (framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }
        FogBlurRenderer.init();
        if (kawaseDownPipeline == null || kawaseUpPipeline == null || compositePipeline == null || compositeUniformBuffer == null || kawaseUniformBuffer == null) {
            return;
        }
        int n2 = FogBlurRenderer.computePasses(f, n);
        if (!FogBlurRenderer.ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight, f4, n2)) {
            return;
        }
        try {
            float f5;
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            commandEncoder.copyTextureToTexture(framebuffer.getDepthAttachment(), depthCopyTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            float f6 = FogBlurRenderer.clamp(0.85f + f * 0.06f, 0.85f, 2.0f);
            GpuTextureView gpuTextureView = FogBlurRenderer.renderBlurChain(sceneCopyTextureView, framebuffer.textureWidth, framebuffer.textureHeight, n2, f6);
            float f7 = FogBlurRenderer.viewDistance();
            if (FogBlurRenderer.isUnderwaterView()) {
                f5 = Math.min(f7, 32.0f);
                f2 *= f5 / f7;
                f7 = f5;
            }
            f5 = Math.max(1.0f, f7 * 0.96f);
            float f8 = FogBlurRenderer.clamp(f2, 1.0f, f5);
            float f9 = FogBlurRenderer.clamp(32.0f + f * 1.8f, 36.0f, 72.0f);
            float f10 = FogBlurRenderer.clamp(f8 / f7, 0.0f, 0.995f);
            float f11 = FogBlurRenderer.clamp((f8 + f9) / f7, f10 + 0.002f, 1.0f);
            float f12 = FogBlurRenderer.clamp(f3 / 100.0f, 0.0f, 1.0f);
            GpuTextureView gpuTextureView2 = depthCopyTextureView;
            FogBlurRenderer.composite(framebuffer.getColorAttachmentView(), gpuTextureView, gpuTextureView2, f12, f7, f10, f11);
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            FogBlurRenderer.closeTargets();
            FogBlurRenderer.closeCompositeBuffer();
            FogBlurRenderer.closeKawaseBuffer();
        }
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static void init() {
        if (disabledAfterError) {
            return;
        }
        try {
            if (kawaseDownPipeline == null) {
                kawaseDownPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(KAWASE_DOWN_PIPELINE_ID).withVertexShader(KAWASE_VERTEX).withFragmentShader(KAWASE_DOWN_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).withSampler("Sampler0").withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (kawaseUpPipeline == null) {
                kawaseUpPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(KAWASE_UP_PIPELINE_ID).withVertexShader(KAWASE_VERTEX).withFragmentShader(KAWASE_UP_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("KawaseParams", UniformType.UNIFORM_BUFFER).withSampler("Sampler0").withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (compositePipeline == null) {
                compositePipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(COMPOSITE_PIPELINE_ID).withVertexShader(FogBlurRenderer.id("post/fogblur/fogblur")).withFragmentShader(COMPOSITE_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("FogBlurData", UniformType.UNIFORM_BUFFER).withSampler("BlurSampler").withSampler("DepthSampler").withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (compositeUniformBuffer == null || compositeUniformBuffer.isClosed() || compositeUniformBuffer.size() < 48L) {
                FogBlurRenderer.closeCompositeBuffer();
                compositeUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:fog_blur_uniforms", 136, 48L);
            }
            if (kawaseUniformBuffer == null || kawaseUniformBuffer.isClosed() || kawaseUniformBuffer.size() < 48L) {
                FogBlurRenderer.closeKawaseBuffer();
                kawaseUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:fog_blur_kawase_uniforms", 136, 48L);
            }
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            kawaseDownPipeline = null;
            kawaseUpPipeline = null;
            compositePipeline = null;
            FogBlurRenderer.closeCompositeBuffer();
            FogBlurRenderer.closeKawaseBuffer();
        }
    }

    public static void beginFrame() {
        opaqueCaptured = false;
    }

    private static void composite(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, float f, float f2, float f3, float f4) {
        if (gpuTextureView == null || gpuTextureView2 == null || gpuTextureView3 == null || f <= 0.0f) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        FogBlurRenderer.writeCompositeUniform(commandEncoder, 0.0f, 0.0f, 0.0f, f, 0.05f, f2, f3, f4);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:fog_blur_composite", gpuTextureView, OptionalInt.empty());){
            renderPass.setPipeline(compositePipeline);
            renderPass.setUniform("FogBlurData", compositeUniformBuffer);
            renderPass.bindTexture("BlurSampler", gpuTextureView2, RenderSampler.linear());
            renderPass.bindTexture("DepthSampler", gpuTextureView3, RenderSampler.nearest());
            renderPass.draw(0, 6);
        }
    }

    public static void setFallbackColor(float f, float f2, float f3) {
        if (!(Float.isFinite(f) && Float.isFinite(f2) && Float.isFinite(f3))) {
            return;
        }
        fallbackRed = FogBlurRenderer.clamp(f, 0.0f, 1.0f);
        fallbackGreen = FogBlurRenderer.clamp(f2, 0.0f, 1.0f);
        fallbackBlue = FogBlurRenderer.clamp(f3, 0.0f, 1.0f);
    }

    private static GpuTextureView renderBlurChain(GpuTextureView gpuTextureView, int n, int n2, int n3, float f) {
        SimpleFramebuffer simpleFramebuffer;
        int n4;
        if (gpuTextureView == null) {
            return null;
        }
        GpuTextureView gpuTextureView2 = gpuTextureView;
        int n5 = n;
        int n6 = n2;
        GpuSampler gpuSampler = RenderSampler.linear();
        for (n4 = 0; n4 < n3; ++n4) {
            simpleFramebuffer = downTargets[n4];
            FogBlurRenderer.renderKawasePass(kawaseDownPipeline, gpuTextureView2, n5, n6, simpleFramebuffer, f, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
            gpuTextureView2 = simpleFramebuffer.getColorAttachmentView();
            n5 = simpleFramebuffer.textureWidth;
            n6 = simpleFramebuffer.textureHeight;
        }
        for (n4 = n3 - 2; n4 >= 0; --n4) {
            simpleFramebuffer = upTargets[n4];
            FogBlurRenderer.renderKawasePass(kawaseUpPipeline, gpuTextureView2, n5, n6, simpleFramebuffer, f, 0.0f, 0.0f, 1.0f, 1.0f, gpuSampler);
            gpuTextureView2 = simpleFramebuffer.getColorAttachmentView();
            n5 = simpleFramebuffer.textureWidth;
            n6 = simpleFramebuffer.textureHeight;
        }
        return gpuTextureView2;
    }

    private static SimpleFramebuffer ensureTarget(SimpleFramebuffer simpleFramebuffer, String string, int n, int n2) {
        if (simpleFramebuffer == null) {
            return new SimpleFramebuffer(string, n, n2, false);
        }
        if (simpleFramebuffer.textureWidth != n || simpleFramebuffer.textureHeight != n2) {
            simpleFramebuffer.resize(n, n2);
        }
        return simpleFramebuffer;
    }

    private static void renderKawasePass(RenderPipeline renderPipeline, GpuTextureView gpuTextureView, int n, int n2, SimpleFramebuffer simpleFramebuffer, float f, float f2, float f3, float f4, float f5, GpuSampler gpuSampler) {
        if (gpuTextureView == null || simpleFramebuffer == null || simpleFramebuffer.getColorAttachmentView() == null) {
            return;
        }
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, f2);
            byteBuffer.putFloat(4, f3);
            byteBuffer.putFloat(8, f4);
            byteBuffer.putFloat(12, f5);
            byteBuffer.putFloat(16, f / (float)Math.max(n, 1));
            byteBuffer.putFloat(20, f / (float)Math.max(n2, 1));
            byteBuffer.putFloat(32, fallbackRed);
            byteBuffer.putFloat(36, fallbackGreen);
            byteBuffer.putFloat(40, fallbackBlue);
            byteBuffer.putFloat(44, 1.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(kawaseUniformBuffer.slice(0L, 48L), byteBuffer);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:fog_blur_kawase", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0));){
                renderPass.setPipeline(renderPipeline);
                renderPass.bindTexture("Sampler0", gpuTextureView, gpuSampler);
                renderPass.setUniform("KawaseParams", kawaseUniformBuffer);
                renderPass.draw(0, 6);
            }
        }
    }

    public static void setBlurTint(float f, float f2, float f3, float f4) {
        if (!(Float.isFinite(f) && Float.isFinite(f2) && Float.isFinite(f3) && Float.isFinite(f4))) {
            tintMix = 0.0f;
            tintBlue = 0.0f;
            tintGreen = 0.0f;
            tintRed = 0.0f;
            return;
        }
        tintRed = FogBlurRenderer.clamp(f, 0.0f, 1.0f);
        tintGreen = FogBlurRenderer.clamp(f2, 0.0f, 1.0f);
        tintBlue = FogBlurRenderer.clamp(f3, 0.0f, 1.0f);
        tintMix = FogBlurRenderer.clamp(f4, 0.0f, 1.0f);
    }

    public static boolean isDisabledAfterError() {
        return disabledAfterError;
    }

    private static void closeTargets() {
        if (sceneCopyTextureView != null) {
            sceneCopyTextureView.close();
            sceneCopyTextureView = null;
        }
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        if (depthCopyTextureView != null) {
            depthCopyTextureView.close();
            depthCopyTextureView = null;
        }
        if (depthCopyTexture != null) {
            depthCopyTexture.close();
            depthCopyTexture = null;
        }
        for (int i = 0; i < 4; ++i) {
            if (downTargets[i] != null) {
                downTargets[i].delete();
                FogBlurRenderer.downTargets[i] = null;
            }
            if (upTargets[i] == null) continue;
            upTargets[i].delete();
            FogBlurRenderer.upTargets[i] = null;
        }
        FogBlurRenderer.closeOpaqueDepth();
        sceneWidth = -1;
        sceneHeight = -1;
        blurWidth = -1;
        blurHeight = -1;
        allocatedPasses = 0;
    }

    private static boolean ensureTargets(int n, int n2, float f, int n3) {
        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        if (gpuDevice == null) {
            return false;
        }
        int n4 = Math.max(1, Math.round((float)n * FogBlurRenderer.clamp(f, 0.25f, 0.75f)));
        int n5 = Math.max(1, Math.round((float)n2 * FogBlurRenderer.clamp(f, 0.25f, 0.75f)));
        if (sceneCopyTexture != null && depthCopyTexture != null && sceneWidth == n && sceneHeight == n2 && blurWidth == n4 && blurHeight == n5 && allocatedPasses >= n3) {
            return true;
        }
        FogBlurRenderer.closeTargets();
        sceneCopyTexture = gpuDevice.createTexture(() -> "nv:fog_blur_scene_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        sceneCopyTextureView = gpuDevice.createTextureView(sceneCopyTexture);
        depthCopyTexture = gpuDevice.createTexture(() -> "nv:fog_blur_depth_copy", 5, TextureFormat.DEPTH32, n, n2, 1, 1);
        depthCopyTextureView = gpuDevice.createTextureView(depthCopyTexture);
        int n6 = n4;
        int n7 = n5;
        for (int i = 0; i < n3; ++i) {
            FogBlurRenderer.downTargets[i] = FogBlurRenderer.ensureTarget(downTargets[i], "nv_fog_blur_down_" + i, n6, n7);
            if (i < n3 - 1) {
                FogBlurRenderer.upTargets[i] = FogBlurRenderer.ensureTarget(upTargets[i], "nv_fog_blur_up_" + i, n6, n7);
            }
            n6 = Math.max(n6 / 2, 1);
            n7 = Math.max(n7 / 2, 1);
        }
        sceneWidth = n;
        sceneHeight = n2;
        blurWidth = n4;
        blurHeight = n5;
        allocatedPasses = n3;
        return true;
    }

    private static void writeCompositeUniform(CommandEncoder commandEncoder, float f, float f2, float f3, float f4, float f5, float f6, float f7, float f8) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(48);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.putFloat(8, f3);
            byteBuffer.putFloat(12, f4);
            byteBuffer.putFloat(16, f5);
            byteBuffer.putFloat(20, f6);
            byteBuffer.putFloat(24, f7);
            byteBuffer.putFloat(28, f8);
            byteBuffer.putFloat(32, tintRed);
            byteBuffer.putFloat(36, tintGreen);
            byteBuffer.putFloat(40, tintBlue);
            byteBuffer.putFloat(44, tintMix);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(compositeUniformBuffer.slice(0L, 48L), byteBuffer);
        }
    }

    private static void closeOpaqueDepth() {
        if (opaqueDepthTextureView != null) {
            opaqueDepthTextureView.close();
            opaqueDepthTextureView = null;
        }
        if (opaqueDepthTexture != null) {
            opaqueDepthTexture.close();
            opaqueDepthTexture = null;
        }
        opaqueDepthWidth = -1;
        opaqueDepthHeight = -1;
        opaqueCaptured = false;
    }

    private static float viewDistance() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.options == null) {
            return 192.0f;
        }
        return Math.max(96.0f, (float)((Integer)minecraftClient.options.getViewDistance().getValue() + 1) * 16.0f);
    }

    private static int computePasses(float f, int n) {
        int n2 = f >= 14.0f ? 4 : (f >= 8.0f ? 3 : (f >= 3.0f ? 2 : 1));
        return Math.clamp((long)n2, 1, Math.max(1, n));
    }

    public static GpuTextureView getGuiSourceTextureView(int n, int n2) {
        if (disabledAfterError || sceneCopyTextureView == null) {
            return null;
        }
        if (sceneWidth != n || sceneHeight != n2) {
            return null;
        }
        return sceneCopyTextureView;
    }

    public static void captureOpaqueDepth(Framebuffer framebuffer) {
        if (disabledAfterError || framebuffer == null || framebuffer.getDepthAttachment() == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }
        try {
            GpuTexture gpuTexture = framebuffer.getDepthAttachment();
            GpuDevice gpuDevice = RenderSystem.tryGetDevice();
            if (gpuDevice == null) {
                return;
            }
            if (opaqueDepthTexture == null || opaqueDepthWidth != framebuffer.textureWidth || opaqueDepthHeight != framebuffer.textureHeight) {
                FogBlurRenderer.closeOpaqueDepth();
                opaqueDepthTexture = gpuDevice.createTexture(() -> "nv:fog_blur_opaque_depth", 5, gpuTexture.getFormat(), framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
                opaqueDepthTextureView = gpuDevice.createTextureView(opaqueDepthTexture);
                opaqueDepthWidth = framebuffer.textureWidth;
                opaqueDepthHeight = framebuffer.textureHeight;
            }
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(gpuTexture, opaqueDepthTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            opaqueCaptured = true;
        }
        catch (Throwable throwable) {
            opaqueCaptured = false;
        }
    }

    private static boolean isUnderwaterView() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.gameRenderer == null || minecraftClient.gameRenderer.getCamera() == null) {
            return false;
        }
        return minecraftClient.gameRenderer.getCamera().getSubmersionType() == CameraSubmersionType.WATER;
    }

    private static void closeCompositeBuffer() {
        if (compositeUniformBuffer != null) {
            compositeUniformBuffer.close();
            compositeUniformBuffer = null;
        }
    }

    private static void closeKawaseBuffer() {
        if (kawaseUniformBuffer != null) {
            kawaseUniformBuffer.close();
            kawaseUniformBuffer = null;
        }
    }
}

