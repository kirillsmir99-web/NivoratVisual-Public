package rtx.nv.utils.render.post.customsky;
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
import java.util.Random;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.lwjgl.system.MemoryStack;
import rtx.nv.utils.render.others.RenderSampler;

public final class CustomSkyRenderer {
    private static final int RES_SCALE = 2;
    private static final int TYPE_COUNT = 5;
    private static final int TYPE_BLACKHOLE = 1;
    private static final int TYPE_STARFALL = 2;
    private static final int UNIFORM_SIZE = 208;
    private static final int BLOOM_LEVELS = 6;
    private static final int BLOOM_UNIFORM_SIZE = 16;
    private static final float BLOOM_THRESHOLD = 0.28f;
    private static final int NOISE_SIZE = 256;
    private static final int NOISE_SHIFT_X = 37;
    private static final int NOISE_SHIFT_Y = 17;
    private static final Identifier VERTEX_SHADER = CustomSkyRenderer.id("post/customsky/customsky");
    private static final Identifier COMPOSITE_SHADER = CustomSkyRenderer.id("post/customsky/composite");
    private static final Identifier COMPOSITE_PIPELINE_ID = CustomSkyRenderer.id("pipeline/post/customsky/composite");
    private static final Identifier BLOOM_SHADER = CustomSkyRenderer.id("post/customsky/bloom_down");
    private static final Identifier BLOOM_PIPELINE_ID = CustomSkyRenderer.id("pipeline/post/customsky/bloom_down");
    private static final Identifier[] MARCH_SHADERS = new Identifier[]{
        CustomSkyRenderer.id("post/customsky/aurora"),
        CustomSkyRenderer.id("post/customsky/blackhole"),
        CustomSkyRenderer.id("post/customsky/nebula"),
        CustomSkyRenderer.id("post/customsky/starfall"),
        CustomSkyRenderer.id("post/customsky/abyss")
    };
    private static final Identifier[] MARCH_PIPELINE_IDS = new Identifier[]{
        CustomSkyRenderer.id("pipeline/post/customsky/aurora"),
        CustomSkyRenderer.id("pipeline/post/customsky/blackhole"),
        CustomSkyRenderer.id("pipeline/post/customsky/nebula"),
        CustomSkyRenderer.id("pipeline/post/customsky/starfall"),
        CustomSkyRenderer.id("pipeline/post/customsky/abyss")
    };
    private static final String[] MARCH_PASS_NAMES = new String[]{
        "nv:customsky_march_aurora",
        "nv:customsky_march_blackhole",
        "nv:customsky_march_nebula",
        "nv:customsky_march_starfall",
        "nv:customsky_march_abyss"
    };
    private static final Matrix4f INV_VIEW_PROJ = new Matrix4f();
    private static final Matrix4f PREV_VIEW_PROJ = new Matrix4f();
    private static final RenderPipeline[] marchPipelines = new RenderPipeline[5];
    private static RenderPipeline compositePipeline;
    private static RenderPipeline bloomPipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer bloomUniformBuffer;
    private static final GpuTexture[] skyTextures;
    private static final GpuTextureView[] skyTextureViews;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyTextureView;
    private static GpuTexture noiseTexture;
    private static GpuTextureView noiseTextureView;
    private static final GpuTexture[] bloomTextures;
    private static final GpuTextureView[] bloomTextureViews;
    private static final int[] bloomWidths;
    private static final int[] bloomHeights;
    private static int fullWidth;
    private static int fullHeight;
    private static int marchWidth;
    private static int marchHeight;
    private static int currentScale;
    private static int frameIndex;
    private static float taaSequence;
    private static int lastType;
    private static boolean historyValid;
    private static boolean disabledAfterError;

    private CustomSkyRenderer() {
    }

    static {
        skyTextures = new GpuTexture[2];
        skyTextureViews = new GpuTextureView[2];
        bloomTextures = new GpuTexture[6];
        bloomTextureViews = new GpuTextureView[6];
        bloomWidths = new int[6];
        bloomHeights = new int[6];
        fullWidth = -1;
        fullHeight = -1;
        marchWidth = -1;
        marchHeight = -1;
        currentScale = -1;
        lastType = -1;
    }

    public static void clear() {
        CustomSkyRenderer.closeTargets();
        CustomSkyRenderer.closeUniform();
        CustomSkyRenderer.closeBloomUniform();
        CustomSkyRenderer.closeNoise();
    }

    /** Retry only at a resource lifecycle boundary, never every failed frame. */
    public static void reset() {
        clear();
        disabledAfterError = false;
        historyValid = false;
        frameIndex = 0;
    }

    public static void applyPending(Framebuffer framebuffer) {
    }

    public static void apply(Framebuffer framebuffer, Matrix4f matrix4f, float f, int n, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9) {
        apply(framebuffer, matrix4f, f, n, f2, f3, f4, f5, f6, f7, f8, f9, false, 1.0f, 4.0f);
    }

    public static void apply(Framebuffer framebuffer, Matrix4f matrix4f, float f, int n, float f2, float f3, float f4, float f5, float f6, float f7, float f8, float f9, boolean themeMode, float density, float layers) {
        if (disabledAfterError || framebuffer == null || matrix4f == null || framebuffer.getColorAttachment() == null || framebuffer.getColorAttachmentView() == null || framebuffer.getDepthAttachmentView() == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }
        int n2 = Math.clamp((long)n, 0, 4);
        boolean bl = n2 == 1;
        try {
        CustomSkyRenderer.init(n2);
        RenderPipeline renderPipeline = marchPipelines[n2];
        if (renderPipeline == null || compositePipeline == null || bloomPipeline == null || uniformBuffer == null || bloomUniformBuffer == null || bl && noiseTextureView == null || !CustomSkyRenderer.ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight, CustomSkyRenderer.resScale(n2))) {
            return;
        }
        if (lastType != n2) {
            historyValid = false;
            lastType = n2;
        }
        int n3 = frameIndex & 1;
        int n4 = 1 - n3;
        boolean bl2 = bl && historyValid;
            ByteBuffer byteBuffer;
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            INV_VIEW_PROJ.set((Matrix4fc)matrix4f).invert();
            float f10 = CustomSkyRenderer.halton(frameIndex % 8 + 1, 2) - 0.5f;
            float f11 = CustomSkyRenderer.halton(frameIndex % 8 + 1, 3) - 0.5f;
            try (MemoryStack memoryStack = MemoryStack.stackPush()) {
                byteBuffer = memoryStack.calloc(208);
                INV_VIEW_PROJ.get(0, byteBuffer);
                byteBuffer.putFloat(64, f);
                byteBuffer.putFloat(68, framebuffer.textureWidth);
                byteBuffer.putFloat(72, framebuffer.textureHeight);
                byteBuffer.putFloat(76, f9);
                byteBuffer.putFloat(80, f2);
                byteBuffer.putFloat(84, f3);
                byteBuffer.putFloat(88, f4);
                byteBuffer.putFloat(92, f8);
                byteBuffer.putFloat(96, f5);
                byteBuffer.putFloat(100, f6);
                byteBuffer.putFloat(104, f7);
                byteBuffer.putFloat(108, n2);
                byteBuffer.putFloat(112, bl ? f10 : 0.0f);
                byteBuffer.putFloat(116, bl ? f11 : 0.0f);
                byteBuffer.putFloat(120, bl2 ? 1.0f : 0.0f);
                byteBuffer.putFloat(124, taaSequence);
                PREV_VIEW_PROJ.get(128, byteBuffer);
                byteBuffer.putFloat(192, themeMode ? 1.0f : 0.0f);
                byteBuffer.putFloat(196, density);
                byteBuffer.putFloat(200, layers);
                byteBuffer.putFloat(204, 0.0f);
                byteBuffer.position(0);
                commandEncoder.writeToBuffer(uniformBuffer.slice(0L, 208L), byteBuffer);
            }
            final String passName = MARCH_PASS_NAMES[n2];
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> CustomSkyRenderer.lambda_apply_0(passName), skyTextureViews[n3], OptionalInt.empty())) {
                renderPass.setPipeline(renderPipeline);
                renderPass.setUniform("SkyParams", uniformBuffer);
                if (bl) {
                    renderPass.bindTexture("NoiseTex", noiseTextureView, RenderSampler.linearRepeat());
                    renderPass.bindTexture("History", skyTextureViews[n4], RenderSampler.linear());
                }
                renderPass.draw(0, 6);
            }
            CustomSkyRenderer.buildBloom(commandEncoder, skyTextureViews[n3], bl);
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:customsky_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty())) {
                renderPass.setPipeline(compositePipeline);
                renderPass.setUniform("SkyParams", uniformBuffer);
                renderPass.bindTexture("Scene", sceneCopyTextureView, RenderSampler.linear());
                renderPass.bindTexture("DepthTex", framebuffer.getDepthAttachmentView(), RenderSampler.nearest());
                renderPass.bindTexture("Sky", skyTextureViews[n3], RenderSampler.linear());
                for (int i = 0; i < 6; ++i) {
                    renderPass.bindTexture("Bloom" + i, bloomTextureViews[i], RenderSampler.linear());
                }
                renderPass.draw(0, 6);
            }
            PREV_VIEW_PROJ.set((Matrix4fc)matrix4f);
            historyValid = true;
            frameIndex = frameIndex + 1 & 0x3FFFFFFF;
            taaSequence = (taaSequence + 0.618034f) % 1.0f;
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            rtx.nv.NV.LOGGER.error("[CustomSky] Rendering failed; retry after resource reload", throwable);
            CustomSkyRenderer.closeTargets();
            CustomSkyRenderer.closeUniform();
            CustomSkyRenderer.closeBloomUniform();
        }
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static void init(int n) {
        if (disabledAfterError) {
            return;
        }
        try {
            RenderPipeline.Builder builder;
            if (marchPipelines[n] == null) {
                builder = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(MARCH_PIPELINE_IDS[n]).withVertexShader(VERTEX_SHADER).withFragmentShader(MARCH_SHADERS[n]).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("SkyParams", UniformType.UNIFORM_BUFFER).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false);
                if (n == 1) {
                    builder.withSampler("NoiseTex").withSampler("History");
                }
                CustomSkyRenderer.marchPipelines[n] = RenderPipelines.register((RenderPipeline)builder.build());
            }
            if (compositePipeline == null) {
                builder = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(COMPOSITE_PIPELINE_ID).withVertexShader(VERTEX_SHADER).withFragmentShader(COMPOSITE_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("SkyParams", UniformType.UNIFORM_BUFFER).withSampler("Scene").withSampler("DepthTex").withSampler("Sky");
                for (int i = 0; i < 6; ++i) {
                    builder.withSampler("Bloom" + i);
                }
                compositePipeline = RenderPipelines.register((RenderPipeline)builder.withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (bloomPipeline == null) {
                bloomPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BLOOM_PIPELINE_ID).withVertexShader(VERTEX_SHADER).withFragmentShader(BLOOM_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("BloomParams", UniformType.UNIFORM_BUFFER).withSampler("Source").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (bloomUniformBuffer == null || bloomUniformBuffer.isClosed() || bloomUniformBuffer.size() < 16L) {
                CustomSkyRenderer.closeBloomUniform();
                bloomUniformBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:customsky_bloom_uniforms", 136, 16L);
            }
            if (uniformBuffer == null || uniformBuffer.isClosed() || uniformBuffer.size() < 208L) {
                CustomSkyRenderer.closeUniform();
                uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:customsky_uniforms", 136, 208L);
            }
            if (n == 1) {
                CustomSkyRenderer.ensureNoiseTexture();
            }
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            rtx.nv.NV.LOGGER.error("[CustomSky] Initialization failed; retry after resource reload", throwable);
            for (int i = 0; i < TYPE_COUNT; ++i) {
                CustomSkyRenderer.marchPipelines[i] = null;
            }
            compositePipeline = null;
            bloomPipeline = null;
            CustomSkyRenderer.closeUniform();
            CustomSkyRenderer.closeBloomUniform();
            CustomSkyRenderer.closeNoise();
        }
    }

    public static void beginFrame() {
    }

    public static boolean isAllocated() {
        return skyTextures[0] != null || sceneCopyTexture != null;
    }

    private static /* synthetic */ String lambda_apply_0(String string) {
        return string;
    }

    public static boolean isDisabledAfterError() {
        return disabledAfterError;
    }

    private static void closeTargets() {
        int n;
        for (n = 0; n < 2; ++n) {
            if (skyTextureViews[n] != null) {
                skyTextureViews[n].close();
                CustomSkyRenderer.skyTextureViews[n] = null;
            }
            if (skyTextures[n] == null) continue;
            skyTextures[n].close();
            CustomSkyRenderer.skyTextures[n] = null;
        }
        if (sceneCopyTextureView != null) {
            sceneCopyTextureView.close();
            sceneCopyTextureView = null;
        }
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        for (n = 0; n < 6; ++n) {
            if (bloomTextureViews[n] != null) {
                bloomTextureViews[n].close();
                CustomSkyRenderer.bloomTextureViews[n] = null;
            }
            if (bloomTextures[n] != null) {
                bloomTextures[n].close();
                CustomSkyRenderer.bloomTextures[n] = null;
            }
            CustomSkyRenderer.bloomWidths[n] = 0;
            CustomSkyRenderer.bloomHeights[n] = 0;
        }
        fullWidth = -1;
        fullHeight = -1;
        historyValid = false;
    }

    private static boolean ensureTargets(int n, int n2, int n3) {
        int n4;
        int n5;
        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        if (gpuDevice == null) {
            return false;
        }
        if (skyTextures[0] != null && skyTextures[1] != null && sceneCopyTexture != null && fullWidth == n && fullHeight == n2 && currentScale == n3) {
            return true;
        }
        CustomSkyRenderer.closeTargets();
        int n6 = Math.max(1, n / n3);
        int n7 = Math.max(1, n2 / n3);
        for (int k = 0; k < 2; ++k) {
            final int texIdx = k;
            CustomSkyRenderer.skyTextures[k] = gpuDevice.createTexture(() -> "nv:customsky_march" + texIdx, 12, TextureFormat.RGBA8, n6, n7, 1, 1);
            CustomSkyRenderer.skyTextureViews[k] = gpuDevice.createTextureView(skyTextures[k]);
        }
        sceneCopyTexture = gpuDevice.createTexture(() -> "nv:customsky_scene_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        sceneCopyTextureView = gpuDevice.createTextureView(sceneCopyTexture);
        n5 = n6;
        n4 = n7;
        for (int i = 0; i < 6; ++i) {
            n5 = Math.max(1, n5 / 2);
            n4 = Math.max(1, n4 / 2);
            int n8 = i;
            CustomSkyRenderer.bloomTextures[i] = gpuDevice.createTexture(() -> "nv:customsky_bloom" + n8, 12, TextureFormat.RGBA8, n5, n4, 1, 1);
            CustomSkyRenderer.bloomTextureViews[i] = gpuDevice.createTextureView(bloomTextures[i]);
            CustomSkyRenderer.bloomWidths[i] = n5;
            CustomSkyRenderer.bloomHeights[i] = n4;
        }
        fullWidth = n;
        fullHeight = n2;
        marchWidth = n6;
        marchHeight = n7;
        currentScale = n3;
        historyValid = false;
        return true;
    }

    private static void ensureNoiseTexture() {
        if (noiseTexture != null && !noiseTexture.isClosed()) {
            return;
        }
        CustomSkyRenderer.closeNoise();
        GpuDevice gpuDevice = RenderSystem.getDevice();
        int[] nArray = new int[65536];
        int[] nArray2 = new int[65536];
        Random random = new Random(1592639215L);
        for (int i = 0; i < nArray.length; ++i) {
            nArray[i] = random.nextInt(256);
            nArray2[i] = random.nextInt(256);
        }
        NativeImage nativeImage = new NativeImage(256, 256, false);
        for (int i = 0; i < 256; ++i) {
            for (int j = 0; j < 256; ++j) {
                int n = Math.floorMod(j - 37, 256);
                int n2 = Math.floorMod(i - 17, 256);
                int n3 = nArray[i * 256 + j];
                int n4 = nArray[n2 * 256 + n];
                int n5 = nArray2[i * 256 + j];
                nativeImage.setColor(j, i, 0xFF000000 | n5 << 16 | n4 << 8 | n3);
            }
        }
        noiseTexture = gpuDevice.createTexture(() -> "nv:customsky_noise", 5, TextureFormat.RGBA8, 256, 256, 1, 1);
        gpuDevice.createCommandEncoder().writeToTexture(noiseTexture, nativeImage);
        noiseTextureView = gpuDevice.createTextureView(noiseTexture);
        nativeImage.close();
    }

    private static void closeBloomUniform() {
        if (bloomUniformBuffer != null) {
            bloomUniformBuffer.close();
            bloomUniformBuffer = null;
        }
    }

    private static void closeNoise() {
        if (noiseTextureView != null) {
            noiseTextureView.close();
            noiseTextureView = null;
        }
        if (noiseTexture != null) {
            noiseTexture.close();
            noiseTexture = null;
        }
    }

    private static float halton(int n, int n2) {
        float f = 0.0f;
        float f2 = 1.0f;
        for (int i = n; i > 0; i /= n2) {
            f += (f2 /= (float)n2) * (float)(i % n2);
        }
        return f;
    }

    private static int resScale(int n) {
        rtx.nv.api.modules.impl.Visuals.Ambience ambience = rtx.nv.api.modules.impl.Visuals.Ambience.getInstance();
        return ambience == null ? (n == 2 ? 2 : 3) : ambience.skyResolutionScale(n);
    }

    private static void buildBloom(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, boolean bl) {
        GpuTextureView gpuTextureView2 = gpuTextureView;
        int n = marchWidth;
        int n2 = marchHeight;
        for (int i = 0; i < 6; ++i) {
            ByteBuffer byteBuffer;
            int n3 = i;
            try (MemoryStack memoryStack = MemoryStack.stackPush();){
                byteBuffer = memoryStack.calloc(16);
                byteBuffer.putFloat(0, 1.0f / (float)n);
                byteBuffer.putFloat(4, 1.0f / (float)n2);
                byteBuffer.putFloat(8, !bl && i == 0 ? 0.28f : 0.0f);
                byteBuffer.putFloat(12, bl ? 1.0f : 0.0f);
                byteBuffer.position(0);
                commandEncoder.writeToBuffer(bloomUniformBuffer.slice(0L, 16L), byteBuffer);
            }
            GpuTextureView destView = bloomTextureViews[i];
            GpuTextureView srcView = gpuTextureView2;
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:customsky_bloom" + n3, destView, OptionalInt.empty());){
                renderPass.setPipeline(bloomPipeline);
                renderPass.setUniform("BloomParams", bloomUniformBuffer);
                renderPass.bindTexture("Source", srcView, RenderSampler.linear());
                renderPass.draw(0, 6);
            }
            gpuTextureView2 = destView;
            n = bloomWidths[i];
            n2 = bloomHeights[i];
        }
    }

    private static void closeUniform() {
        if (uniformBuffer != null) {
            uniformBuffer.close();
            uniformBuffer = null;
        }
    }
}

