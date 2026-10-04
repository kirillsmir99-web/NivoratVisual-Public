package rtx.nv.utils.render.post.guimotionblur;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.platform.DestFactor;
import com.mojang.blaze3d.platform.SourceFactor;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import rtx.nv.NV;
import rtx.nv.utils.render.others.FullscreenQuadBuffer;
import rtx.nv.utils.render.others.RenderSampler;

public final class GuiMotionBlurRenderer {
    private static final int UNIFORM_BYTES = 16;
    private static final int COMPOSITE_UNIFORM_BYTES = 80;
    private static final int MAX_MASK_RECTS = 192;
    public static final int MASK_RECT_STRIDE = 6;
    private static final int MASK_VERTICES_PER_RECT = 6;
    private static final VertexFormat MASK_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("Color", VertexFormatElement.COLOR).build();
    private static final int MASK_VERTEX_STRIDE_BYTES = MASK_FORMAT.getVertexSize();
    private static final int MASK_VERTEX_BYTES = 1152 * MASK_VERTEX_STRIDE_BYTES;
    private static final float MASK_FEATHER_PX = 8.0f;
    private static final float MASK_CORNER_RADIUS_PX = 8.0f;
    private static final float GAUSSIAN_SAMPLE_RADIUS = 8.0f;
    private static final float MOTION_LAYER_BOOST = 1.0f;
    private static final float GUI_MASK_RADIUS = 10.0f;
    private static final float GUI_MASK_FEATHER = 18.0f;
    private static final float MIN_RENDER_SCALE = 0.42f;
    private static final float MAX_RENDER_SCALE = 0.56f;
    private static final Identifier BLUR_PIPELINE_ID = GuiMotionBlurRenderer.id("pipeline/post/guimotionblur/gaussian");
    private static final Identifier MASK_PIPELINE_ID = GuiMotionBlurRenderer.id("pipeline/post/guimotionblur/mask");
    private static final Identifier MASK_REPLACE_PIPELINE_ID = GuiMotionBlurRenderer.id("pipeline/post/guimotionblur/mask_replace");
    private static final Identifier COMPOSITE_PIPELINE_ID = GuiMotionBlurRenderer.id("pipeline/post/guimotionblur/composite");
    private static final Identifier FULLSCREEN_SHADER = GuiMotionBlurRenderer.id("post/guimotionblur/fullscreen");
    private static final Identifier GAUSSIAN_SHADER = GuiMotionBlurRenderer.id("post/guimotionblur/gaussian");
    private static final Identifier MASK_SHADER = GuiMotionBlurRenderer.id("post/guimotionblur/mask");
    private static final Identifier COMPOSITE_SHADER = GuiMotionBlurRenderer.id("post/guimotionblur/composite");
    private static final float GAUSSIAN_TAPS_HALF = 16.0f;
    private static final RenderPipeline BLUR_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BLUR_PIPELINE_ID).withVertexShader(FULLSCREEN_SHADER).withFragmentShader(GAUSSIAN_SHADER).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.TRIANGLES).withSampler("MotionInput").withUniform("MotionBlurParams", UniformType.UNIFORM_BUFFER).withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).build();
    private static final RenderPipeline MASK_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(MASK_PIPELINE_ID).withVertexShader(MASK_SHADER).withFragmentShader(MASK_SHADER).withVertexFormat(MASK_FORMAT, VertexFormat.DrawMode.TRIANGLES).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).build();
    private static final RenderPipeline MASK_REPLACE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(MASK_REPLACE_PIPELINE_ID).withVertexShader(MASK_SHADER).withFragmentShader(MASK_SHADER).withVertexFormat(MASK_FORMAT, VertexFormat.DrawMode.TRIANGLES).withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).build();
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(COMPOSITE_PIPELINE_ID).withVertexShader(FULLSCREEN_SHADER).withFragmentShader(COMPOSITE_SHADER).withVertexFormat(VertexFormats.POSITION, VertexFormat.DrawMode.TRIANGLES).withSampler("MotionScene").withSampler("MotionBackground").withSampler("MotionBlurred").withSampler("MotionBackgroundBlurred").withSampler("MotionMask").withUniform("MotionCompositeParams", UniformType.UNIFORM_BUFFER).withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).build();
    private static GpuBuffer fullscreenVertexBuffer;
    private static GpuBuffer maskVertexBuffer;
    private static GpuBuffer blurUniformBuffer;
    private static GpuBuffer compositeUniformBuffer;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyTextureView;
    private static GpuTexture backgroundCopyTexture;
    private static GpuTextureView backgroundCopyTextureView;
    private static GpuTexture remoteBackgroundCopyTexture;
    private static GpuTextureView remoteBackgroundCopyTextureView;
    private static SimpleFramebuffer maskTarget;
    private static SimpleFramebuffer horizontalTarget;
    private static SimpleFramebuffer verticalTarget;
    private static SimpleFramebuffer backgroundHorizontalTarget;
    private static SimpleFramebuffer backgroundVerticalTarget;
    private static int sourceWidth;
    private static int sourceHeight;
    private static int blurWidth;
    private static int blurHeight;
    private static boolean backgroundCaptured;
    private static Framebuffer backgroundCaptureTarget;
    private static boolean remoteBackgroundCaptured;
    private static Framebuffer remoteBackgroundCaptureTarget;
    private static boolean disabledAfterError;
    private static boolean pipelinesRegistered;

    private GuiMotionBlurRenderer() {
    }

    static {
        sourceWidth = -1;
        sourceHeight = -1;
        blurWidth = -1;
        blurHeight = -1;
    }

    public static void shutdown() {
        GuiMotionBlurRenderer.releaseResources();
        disabledAfterError = false;
    }

    private static float clamp(float f, float f2, float f3) {
        if (f3 < f2) {
            return f2;
        }
        return Math.max(f2, Math.min(f3, f));
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static void closeBuffer(GpuBuffer gpuBuffer) {
        if (gpuBuffer != null) {
            gpuBuffer.close();
        }
    }

    private static void composite(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5, GpuTextureView gpuTextureView6, float f, float f2, int n, int n2, int n3, int n4, int n5, int n6, float[] fArray, int n7, int n8, int n9, int n10, float f3, float f4, float f5) {
        if (gpuTextureView == null || gpuTextureView2 == null || gpuTextureView3 == null || gpuTextureView4 == null || gpuTextureView5 == null || gpuTextureView6 == null) {
            return;
        }
        int n11 = Math.max(0, n6 - (n2 + n4));
        GuiMotionBlurRenderer.writeCompositeUniform(commandEncoder, f, f2, n5, n6, fArray, n7, n8, n9, n10, f3, f4, f5);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_motion_blur_composite", gpuTextureView, OptionalInt.empty(), null, OptionalDouble.empty());){
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            renderPass.setVertexBuffer(0, fullscreenVertexBuffer);
            renderPass.enableScissor(n, n11, n3, n4);
            renderPass.bindTexture("MotionScene", gpuTextureView2, RenderSampler.linear());
            renderPass.bindTexture("MotionBackground", gpuTextureView4, RenderSampler.linear());
            renderPass.bindTexture("MotionBlurred", gpuTextureView3, RenderSampler.linear());
            renderPass.bindTexture("MotionBackgroundBlurred", gpuTextureView5, RenderSampler.linear());
            renderPass.bindTexture("MotionMask", gpuTextureView6, RenderSampler.linear());
            renderPass.setUniform("MotionCompositeParams", compositeUniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }

    private static boolean ensureReady(int n, int n2, float f) {
        try {
            GpuDevice gpuDevice = RenderSystem.tryGetDevice();
            if (gpuDevice == null) {
                return false;
            }
            if (!pipelinesRegistered) {
                RenderPipelines.register((RenderPipeline)BLUR_PIPELINE);
                RenderPipelines.register((RenderPipeline)MASK_PIPELINE);
                RenderPipelines.register((RenderPipeline)MASK_REPLACE_PIPELINE);
                RenderPipelines.register((RenderPipeline)COMPOSITE_PIPELINE);
                pipelinesRegistered = true;
            }
            if (fullscreenVertexBuffer == null || fullscreenVertexBuffer.isClosed()) {
                fullscreenVertexBuffer = FullscreenQuadBuffer.getOrCreate();
            }
            if (maskVertexBuffer == null || maskVertexBuffer.isClosed() || maskVertexBuffer.size() < (long)MASK_VERTEX_BYTES) {
                GuiMotionBlurRenderer.closeBuffer(maskVertexBuffer);
                maskVertexBuffer = gpuDevice.createBuffer(() -> "nv:gui_motion_blur_mask_vertices", 40, (long)MASK_VERTEX_BYTES);
            }
            if (blurUniformBuffer == null || blurUniformBuffer.isClosed() || blurUniformBuffer.size() < 16L) {
                GuiMotionBlurRenderer.closeBuffer(blurUniformBuffer);
                blurUniformBuffer = gpuDevice.createBuffer(() -> "nv:gui_motion_blur_uniform", 136, 16L);
            }
            if (compositeUniformBuffer == null || compositeUniformBuffer.isClosed() || compositeUniformBuffer.size() < 80L) {
                GuiMotionBlurRenderer.closeBuffer(compositeUniformBuffer);
                compositeUniformBuffer = gpuDevice.createBuffer(() -> "nv:gui_motion_blur_composite_uniform", 136, 80L);
            }
            GuiMotionBlurRenderer.ensureTargets(gpuDevice, n, n2, f);
            return fullscreenVertexBuffer != null && maskVertexBuffer != null && blurUniformBuffer != null && compositeUniformBuffer != null && sceneCopyTextureView != null && backgroundCopyTextureView != null && remoteBackgroundCopyTextureView != null && maskTarget != null && horizontalTarget != null && verticalTarget != null && backgroundHorizontalTarget != null && backgroundVerticalTarget != null;
        }
        catch (Throwable throwable) {
            GuiMotionBlurRenderer.disableAfterError(throwable);
            return false;
        }
    }

    public static void captureBackground(Framebuffer framebuffer, float f) {
        boolean bl;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        boolean bl2 = bl = minecraftClient != null && framebuffer == minecraftClient.getFramebuffer();
        if (disabledAfterError || !Float.isFinite(f)) {
            GuiMotionBlurRenderer.clearBackgroundCapture(bl);
            return;
        }
        if (framebuffer == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0 || framebuffer.getColorAttachment() == null) {
            GuiMotionBlurRenderer.clearBackgroundCapture(bl);
            return;
        }
        if (!GuiMotionBlurRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight, 1.0f)) {
            GuiMotionBlurRenderer.clearBackgroundCapture(bl);
            return;
        }
        try {
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getColorAttachment(), bl ? backgroundCopyTexture : remoteBackgroundCopyTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            if (bl) {
                backgroundCaptured = true;
                backgroundCaptureTarget = framebuffer;
            } else {
                remoteBackgroundCaptured = true;
                remoteBackgroundCaptureTarget = framebuffer;
            }
        }
        catch (Throwable throwable) {
            GuiMotionBlurRenderer.clearBackgroundCapture(bl);
            GuiMotionBlurRenderer.disableAfterError(throwable);
        }
    }

    public static void captureBackground(float f) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.gameRenderer == null || minecraftClient.getFramebuffer() == null) {
            backgroundCaptured = false;
            backgroundCaptureTarget = null;
            return;
        }
        GuiMotionBlurRenderer.captureBackground(minecraftClient.getFramebuffer(), f);
    }

    public static void applyWithCopy(Framebuffer framebuffer, float f, float f2, int n, int n2, int n3, int n4, float[] fArray, int n5, int n6, int n7, int n8, int n9, float f3, float f4, float f5, boolean bl) {
        boolean bl2;
        if (disabledAfterError) {
            return;
        }
        float f6 = GuiMotionBlurRenderer.clamp(f, 0.0f, 1.0f);
        float f7 = Float.isFinite(f3) ? GuiMotionBlurRenderer.clamp(f3, 0.25f, 4.0f) : 1.0f;
        boolean bl3 = Math.abs(f7 - 1.0f) > 5.0E-4f;
        int n10 = GuiMotionBlurRenderer.safeMaskRectCount(fArray, n5);
        if (!bl3 && f6 <= 0.002f || !Float.isFinite(f2) || n10 <= 0) {
            return;
        }
        if (framebuffer == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0 || framebuffer.getColorAttachment() == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        int n11 = GuiMotionBlurRenderer.clampInt(n, 0, framebuffer.textureWidth);
        int n12 = GuiMotionBlurRenderer.clampInt(n2, 0, framebuffer.textureHeight);
        int n13 = GuiMotionBlurRenderer.clampInt(n + n3, n11, framebuffer.textureWidth);
        int n14 = GuiMotionBlurRenderer.clampInt(n2 + n4, n12, framebuffer.textureHeight);
        int n15 = n13 - n11;
        int n16 = n14 - n12;
        if (n15 <= 0 || n16 <= 0) {
            return;
        }
        int n17 = bl3 ? GuiMotionBlurRenderer.clampInt(n6, 0, framebuffer.textureWidth) : 0;
        int n18 = bl3 ? GuiMotionBlurRenderer.clampInt(n7, 0, framebuffer.textureHeight) : 0;
        int n19 = bl3 ? GuiMotionBlurRenderer.clampInt(n6 + n8, n17, framebuffer.textureWidth) : 0;
        int n20 = bl3 ? GuiMotionBlurRenderer.clampInt(n7 + n9, n18, framebuffer.textureHeight) : 0;
        int n21 = n19 - n17;
        int n22 = n20 - n18;
        if (bl3 && (n21 <= 0 || n22 <= 0)) {
            return;
        }
        float f8 = GuiMotionBlurRenderer.clamp(f2, 0.0f, 48.0f);
        if (!GuiMotionBlurRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight, 1.0f)) {
            return;
        }
        boolean bl4 = backgroundCaptured && framebuffer == backgroundCaptureTarget;
        boolean bl5 = bl2 = remoteBackgroundCaptured && framebuffer == remoteBackgroundCaptureTarget;
        if (!bl4 && !bl2) {
            return;
        }
        GpuTextureView gpuTextureView = bl4 ? backgroundCopyTextureView : remoteBackgroundCopyTextureView;
        GuiMotionBlurRenderer.clearBackgroundCapture(bl4);
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            GuiMotionBlurRenderer.gaussianChain(commandEncoder, sceneCopyTextureView, framebuffer.textureWidth, framebuffer.textureHeight, horizontalTarget, verticalTarget, f8, n11, n12, n15, n16);
            GuiMotionBlurRenderer.gaussianChain(commandEncoder, gpuTextureView, framebuffer.textureWidth, framebuffer.textureHeight, backgroundHorizontalTarget, backgroundVerticalTarget, f8, n11, n12, n15, n16);
            GuiMotionBlurRenderer.renderMask(commandEncoder, fArray, n10, framebuffer.textureWidth, framebuffer.textureHeight, bl);
            GuiMotionBlurRenderer.composite(commandEncoder, framebuffer.getColorAttachmentView(), sceneCopyTextureView, verticalTarget.getColorAttachmentView(), gpuTextureView, backgroundVerticalTarget.getColorAttachmentView(), maskTarget.getColorAttachmentView(), f6, f8, n11, n12, n15, n16, framebuffer.textureWidth, framebuffer.textureHeight, fArray, n17, n18, n21, n22, f7, f4, f5);
        }
        catch (Throwable throwable) {
            GuiMotionBlurRenderer.disableAfterError(throwable);
        }
    }

    public static void applyWithCopy(float f, float f2, int n, int n2, int n3, int n4, float[] fArray, int n5, int n6, int n7, int n8, int n9, float f3, float f4, float f5, boolean bl) {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.gameRenderer == null || minecraftClient.getFramebuffer() == null) {
            return;
        }
        GuiMotionBlurRenderer.applyWithCopy(minecraftClient.getFramebuffer(), f, f2, n, n2, n3, n4, fArray, n5, n6, n7, n8, n9, f3, f4, f5, bl);
    }

    public static void applyWithCopy(float f, float f2, int n, int n2, int n3, int n4, float[] fArray, int n5, int n6, int n7, int n8, int n9, float f3, float f4, float f5) {
        GuiMotionBlurRenderer.applyWithCopy(f, f2, n, n2, n3, n4, fArray, n5, n6, n7, n8, n9, f3, f4, f5, false);
    }

    public static void applyWithCopy(Framebuffer framebuffer, float f, float f2, int n, int n2, int n3, int n4, float[] fArray, int n5, int n6, int n7, int n8, int n9, float f3, float f4, float f5) {
        GuiMotionBlurRenderer.applyWithCopy(framebuffer, f, f2, n, n2, n3, n4, fArray, n5, n6, n7, n8, n9, f3, f4, f5, false);
    }

    private static void gaussianChain(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, int n, int n2, SimpleFramebuffer simpleFramebuffer, SimpleFramebuffer simpleFramebuffer2, float f, int n3, int n4, int n5, int n6) {
        float f2 = f / 16.0f;
        int n7 = (int)Math.ceil(f) + 8;
        int n8 = GuiMotionBlurRenderer.clampInt(n3 - n7, 0, n);
        int n9 = GuiMotionBlurRenderer.clampInt(n4 - n7, 0, n2);
        int n10 = GuiMotionBlurRenderer.clampInt(n3 + n5 + n7, n8, n);
        int n11 = GuiMotionBlurRenderer.clampInt(n4 + n6 + n7, n9, n2);
        if (n10 - n8 <= 0 || n11 - n9 <= 0) {
            return;
        }
        GuiMotionBlurRenderer.gaussianPass(commandEncoder, gpuTextureView, simpleFramebuffer, f2 / (float)Math.max(1, n), 0.0f, n8, n9, n10 - n8, n11 - n9);
        GuiMotionBlurRenderer.gaussianPass(commandEncoder, simpleFramebuffer.getColorAttachmentView(), simpleFramebuffer2, 0.0f, f2 / (float)Math.max(1, n2), n8, n9, n10 - n8, n11 - n9);
    }

    private static void clearBackgroundCapture(boolean bl) {
        if (bl) {
            backgroundCaptured = false;
            backgroundCaptureTarget = null;
        } else {
            remoteBackgroundCaptured = false;
            remoteBackgroundCaptureTarget = null;
        }
    }

    private static void releaseResources() {
        GuiMotionBlurRenderer.closeTargets();
        GuiMotionBlurRenderer.closeBuffer(maskVertexBuffer);
        GuiMotionBlurRenderer.closeBuffer(blurUniformBuffer);
        GuiMotionBlurRenderer.closeBuffer(compositeUniformBuffer);
        maskVertexBuffer = null;
        blurUniformBuffer = null;
        compositeUniformBuffer = null;
        fullscreenVertexBuffer = null;
    }

    private static int safeMaskRectCount(float[] fArray, int n) {
        if (fArray == null || n <= 0) {
            return 0;
        }
        return Math.min(Math.min(n, 192), fArray.length / 6);
    }

    private static int writeMaskVertices(ByteBuffer byteBuffer, float[] fArray, int n, int n2, int n3) {
        int n4 = 0;
        int n5 = GuiMotionBlurRenderer.safeMaskRectCount(fArray, n);
        float f = Math.max(1.0f, (float)n2);
        float f2 = Math.max(1.0f, (float)n3);
        for (int i = 0; i < n5; ++i) {
            int n6 = i * 6;
            if (fArray[n6 + 4] < 0.0f) continue;
            float f3 = fArray[n6 + 2];
            float f4 = fArray[n6 + 3];
            float f5 = GuiMotionBlurRenderer.clamp(fArray[n6 + 4], 0.0f, 1.0f);
            float f6 = GuiMotionBlurRenderer.clamp(fArray[n6 + 5], 0.0f, 1.0f);
            if (f3 <= 0.5f || f4 <= 0.5f) continue;
            float f7 = fArray[n6] - 8.0f;
            float f8 = fArray[n6 + 1] - 8.0f;
            float f9 = fArray[n6] + f3 + 8.0f;
            float f10 = fArray[n6 + 1] + f4 + 8.0f;
            float f11 = f7 / f * 2.0f - 1.0f;
            float f12 = f9 / f * 2.0f - 1.0f;
            float f13 = 1.0f - f8 / f2 * 2.0f;
            float f14 = 1.0f - f10 / f2 * 2.0f;
            float f15 = Math.min(8.0f, Math.min(f3, f4) * 0.5f);
            float f16 = -8.0f;
            float f17 = -8.0f;
            float f18 = f3 + 8.0f;
            float f19 = f4 + 8.0f;
            int n7 = Math.min(Short.MAX_VALUE, Math.round(f3 * 8.0f));
            int n8 = Math.min(Short.MAX_VALUE, Math.round(f4 * 8.0f));
            int n9 = Math.round(f5 * 255.0f);
            int n10 = Math.round(f6 * 255.0f);
            GuiMotionBlurRenderer.putVertex(byteBuffer, f11, f14, f15, f16, f19, n7, n8, n9, n10);
            GuiMotionBlurRenderer.putVertex(byteBuffer, f12, f14, f15, f18, f19, n7, n8, n9, n10);
            GuiMotionBlurRenderer.putVertex(byteBuffer, f12, f13, f15, f18, f17, n7, n8, n9, n10);
            GuiMotionBlurRenderer.putVertex(byteBuffer, f11, f14, f15, f16, f19, n7, n8, n9, n10);
            GuiMotionBlurRenderer.putVertex(byteBuffer, f12, f13, f15, f18, f17, n7, n8, n9, n10);
            GuiMotionBlurRenderer.putVertex(byteBuffer, f11, f13, f15, f16, f17, n7, n8, n9, n10);
            n4 += 6;
        }
        return n4;
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
        if (backgroundCopyTextureView != null) {
            backgroundCopyTextureView.close();
            backgroundCopyTextureView = null;
        }
        if (backgroundCopyTexture != null) {
            backgroundCopyTexture.close();
            backgroundCopyTexture = null;
        }
        if (remoteBackgroundCopyTextureView != null) {
            remoteBackgroundCopyTextureView.close();
            remoteBackgroundCopyTextureView = null;
        }
        if (remoteBackgroundCopyTexture != null) {
            remoteBackgroundCopyTexture.close();
            remoteBackgroundCopyTexture = null;
        }
        if (maskTarget != null) {
            maskTarget.delete();
            maskTarget = null;
        }
        if (horizontalTarget != null) {
            horizontalTarget.delete();
            horizontalTarget = null;
        }
        if (backgroundHorizontalTarget != null) {
            backgroundHorizontalTarget.delete();
            backgroundHorizontalTarget = null;
        }
        if (verticalTarget != null) {
            verticalTarget.delete();
            verticalTarget = null;
        }
        if (backgroundVerticalTarget != null) {
            backgroundVerticalTarget.delete();
            backgroundVerticalTarget = null;
        }
        sourceWidth = -1;
        sourceHeight = -1;
        blurWidth = -1;
        blurHeight = -1;
        backgroundCaptured = false;
        backgroundCaptureTarget = null;
        remoteBackgroundCaptured = false;
        remoteBackgroundCaptureTarget = null;
    }

    private static void gaussianPass(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, float f, float f2, int n, int n2, int n3, int n4) {
        if (gpuTextureView == null || simpleFramebuffer == null || simpleFramebuffer.getColorAttachmentView() == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(blurUniformBuffer.slice(0L, 16L), byteBuffer);
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_motion_blur_gaussian", simpleFramebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty())) {
            renderPass.setPipeline(BLUR_PIPELINE);
            renderPass.setVertexBuffer(0, fullscreenVertexBuffer);
            int n5 = Math.max(0, simpleFramebuffer.textureHeight - (n2 + n4));
            renderPass.enableScissor(n, n5, n3, n4);
            renderPass.bindTexture("MotionInput", gpuTextureView, RenderSampler.linear());
            renderPass.setUniform("MotionBlurParams", blurUniformBuffer.slice(0L, 16L));
            renderPass.draw(0, 6);
        }
    }

    private static void ensureTargets(GpuDevice gpuDevice, int n, int n2, float f) {
        if (sceneCopyTextureView != null && backgroundCopyTextureView != null && remoteBackgroundCopyTextureView != null && sourceWidth == n && sourceHeight == n2 && blurWidth == n && blurHeight == n2 && maskTarget != null && GuiMotionBlurRenderer.maskTarget.textureWidth == n && GuiMotionBlurRenderer.maskTarget.textureHeight == n2) {
            return;
        }
        GuiMotionBlurRenderer.closeTargets();
        sceneCopyTexture = gpuDevice.createTexture(() -> "nv:gui_motion_blur_scene_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        sceneCopyTextureView = gpuDevice.createTextureView(sceneCopyTexture);
        backgroundCopyTexture = gpuDevice.createTexture(() -> "nv:gui_motion_blur_background_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        backgroundCopyTextureView = gpuDevice.createTextureView(backgroundCopyTexture);
        remoteBackgroundCopyTexture = gpuDevice.createTexture(() -> "nv:gui_motion_blur_remote_background_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        remoteBackgroundCopyTextureView = gpuDevice.createTextureView(remoteBackgroundCopyTexture);
        maskTarget = new SimpleFramebuffer("nv_gui_motion_blur_mask", n, n2, false);
        horizontalTarget = new SimpleFramebuffer("nv_gui_motion_blur_scene_h", n, n2, false);
        verticalTarget = new SimpleFramebuffer("nv_gui_motion_blur_scene_result", n, n2, false);
        backgroundHorizontalTarget = new SimpleFramebuffer("nv_gui_motion_blur_bg_h", n, n2, false);
        backgroundVerticalTarget = new SimpleFramebuffer("nv_gui_motion_blur_bg_result", n, n2, false);
        sourceWidth = n;
        sourceHeight = n2;
        blurWidth = n;
        blurHeight = n2;
    }

    private static void writeCompositeUniform(CommandEncoder commandEncoder, float f, float f2, int n, int n2, float[] fArray, int n3, int n4, int n5, int n6, float f3, float f4, float f5) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(80);
            float f6 = 0.0f;
            float f7 = 0.0f;
            float f8 = n;
            float f9 = n2;
            if (fArray != null && fArray.length >= 4) {
                f6 = fArray[0];
                f7 = fArray[1];
                f8 = Math.max(0.0f, fArray[2]);
                f9 = Math.max(0.0f, fArray[3]);
            }
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, 0.035f);
            byteBuffer.putFloat(8, 1.0f);
            byteBuffer.putFloat(12, 10.0f);
            byteBuffer.putFloat(16, f6);
            byteBuffer.putFloat(20, f7);
            byteBuffer.putFloat(24, f8);
            byteBuffer.putFloat(28, f9);
            byteBuffer.putFloat(32, n);
            byteBuffer.putFloat(36, n2);
            byteBuffer.putFloat(40, 18.0f);
            byteBuffer.putFloat(44, f3);
            byteBuffer.putFloat(48, n3);
            byteBuffer.putFloat(52, n4);
            byteBuffer.putFloat(56, Math.max(0, n5));
            byteBuffer.putFloat(60, Math.max(0, n6));
            byteBuffer.putFloat(64, f4);
            byteBuffer.putFloat(68, f5);
            byteBuffer.putFloat(72, Math.max(0.0f, f2));
            byteBuffer.putFloat(76, 0.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(compositeUniformBuffer.slice(0L, 80L), byteBuffer);
        }
    }

    private static void disableAfterError(Throwable throwable) {
        NV.LOGGER.error("[GuiMotionBlur] disabled after render error", throwable);
        backgroundCaptured = false;
        GuiMotionBlurRenderer.releaseResources();
        disabledAfterError = true;
    }

    private static int clampInt(int n, int n2, int n3) {
        if (n3 < n2) {
            return n2;
        }
        return Math.max(n2, Math.min(n3, n));
    }

    private static float renderScale(float f) {
        return GuiMotionBlurRenderer.clamp(0.56f - f * 0.012f, 0.42f, 0.56f);
    }

    private static void renderMask(CommandEncoder commandEncoder, float[] fArray, int n, int n2, int n3, boolean bl) {
        int n4;
        if (fArray == null || maskTarget == null || maskTarget.getColorAttachmentView() == null || maskVertexBuffer == null) {
            return;
        }
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.malloc(n * 6 * MASK_VERTEX_STRIDE_BYTES);
            n4 = GuiMotionBlurRenderer.writeMaskVertices(byteBuffer, fArray, n, n2, n3);
            if (n4 > 0) {
                byteBuffer.flip();
                commandEncoder.writeToBuffer(maskVertexBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            }
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_motion_blur_mask", maskTarget.getColorAttachmentView(), OptionalInt.of(0), null, OptionalDouble.empty())) {
            if (n4 > 0) {
                renderPass.setPipeline(bl ? MASK_REPLACE_PIPELINE : MASK_PIPELINE);
                renderPass.setVertexBuffer(0, maskVertexBuffer);
                renderPass.draw(0, n4);
            }
        }
    }

    private static void putVertex(ByteBuffer byteBuffer, float f, float f2, float f3, float f4, float f5, int n, int n2, int n3, int n4) {
        byteBuffer.putFloat(f);
        byteBuffer.putFloat(f2);
        byteBuffer.putFloat(f3);
        byteBuffer.putFloat(f4);
        byteBuffer.putFloat(f5);
        byteBuffer.putShort((short)n);
        byteBuffer.putShort((short)n2);
        byteBuffer.put((byte)n3);
        byteBuffer.put((byte)n4);
        byteBuffer.put((byte)0);
        byteBuffer.put((byte)-1);
    }

    public static void applyWithCopy(Framebuffer framebuffer, float f, float f2, float bX, float bY, float bW, float bH, int mask, int cardCount, float oX, float oY, float oW, float oH, float f3, float origX, float origY) {
        applyWithCopy(framebuffer, f, f2, (int)bX, (int)bY, (int)bW, (int)bH, null, mask, (int)oX, (int)oY, (int)oW, (int)oH, f3, origX, origY, false);
    }
}

