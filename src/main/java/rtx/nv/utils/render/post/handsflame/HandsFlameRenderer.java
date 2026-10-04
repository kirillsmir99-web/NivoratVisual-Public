package rtx.nv.utils.render.post.handsflame;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;
import rtx.nv.NV;
import rtx.nv.api.drags.Position;
import rtx.nv.api.modules.impl.Visuals.NameTags;
import rtx.nv.api.modules.impl.Visuals.ShaderHands;
import rtx.nv.utils.color.ColorUtil;
import rtx.nv.utils.render.post.shaderhands.ShaderHandsRenderer;
import rtx.nv.utils.render.render2d.ClientPalette;

public final class HandsFlameRenderer {
    private static final Identifier TRAIL_PIPELINE_ID = HandsFlameRenderer.id("pipeline/effects/hands_flame_trail");
    private static final Identifier COMPOSITE_PIPELINE_ID = HandsFlameRenderer.id("pipeline/effects/hands_flame_composite");
    private static final Identifier RESTORE_PIPELINE_ID = HandsFlameRenderer.id("pipeline/effects/hands_flame_restore");
    private static final Identifier FULLSCREEN_VERTEX_SHADER = HandsFlameRenderer.id("effects/hands_flame/fullscreen");
    private static final Identifier TRAIL_FRAGMENT_SHADER = HandsFlameRenderer.id("effects/hands_flame/trail");
    private static final Identifier COMPOSITE_FRAGMENT_SHADER = HandsFlameRenderer.id("effects/hands_flame/composite");
    private static final Identifier RESTORE_FRAGMENT_SHADER = HandsFlameRenderer.id("effects/hands_flame/restore");
    private static final int UNIFORM_BYTES = 128;
    private static final int IRIS_CAPTURE_SLOTS = 2;
    private static boolean flameEnabled;
    private static float flameStrength;
    private static float flameRiseSpeed;
    private static float flameWobble;
    private static float flameLength;
    private static float flameBrightness;
    private static int flameColorMode;
    private static int flameColor;
    private static boolean flameItemsOnly;
    private static boolean flameClientGradient;
    private static RenderPipeline trailPipeline;
    private static RenderPipeline compositePipeline;
    private static RenderPipeline restorePipeline;
    private static GpuBuffer uniformBuffer;
    private static GpuBuffer dummyVertexBuffer;
    private static ByteBuffer dataBuffer;
    private static GpuTexture beforeTexture;
    private static GpuTexture sceneTexture;
    private static GpuTexture handTexture;
    private static GpuTexture trailTextureA;
    private static GpuTexture trailTextureB;
    private static GpuTextureView beforeTextureView;
    private static GpuTextureView sceneTextureView;
    private static GpuTextureView handTextureView;
    private static GpuTextureView trailTextureViewA;
    private static GpuTextureView trailTextureViewB;
    private static final GpuTexture[] irisDepthBeforeTextures;
    private static final GpuTexture[] irisDepthAfterTextures;
    private static final GpuTextureView[] irisDepthBeforeTextureViews;
    private static final GpuTextureView[] irisDepthAfterTextureViews;
    private static boolean useTrailAAsHistory;
    private static boolean capturedBeforeHands;
    private static int irisCaptureCount;
    private static int irisOpenCaptureSlot;
    private static int lastWidth;
    private static int lastHeight;
    private static int lastIrisDepthWidth;
    private static int lastIrisDepthHeight;
    private static TextureFormat lastIrisDepthFormat;
    private static RenderRegion lastRenderRegion;
    private static boolean disabledAfterError;
    private static final float[] protectedTagBounds;
    private static GpuTexture blackTexture;
    private static GpuTextureView blackTextureView;
    private static final float TRAIL_STEP_SECONDS = 0.008333334f;
    private static long trailClockNs;
    private static float trailAccumulator;
    private static float pendingTrailStep;
    private static float smoothR;
    private static float smoothG;
    private static float smoothB;

    private HandsFlameRenderer() {
    }

    static {
        flameStrength = 0.85f;
        flameWobble = 0.65f;
        flameLength = 0.95f;
        flameBrightness = 0.9f;
        flameColor = ColorUtil.rgba(255, 255, 255, 230);
        irisDepthBeforeTextures = new GpuTexture[2];
        irisDepthAfterTextures = new GpuTexture[2];
        irisDepthBeforeTextureViews = new GpuTextureView[2];
        irisDepthAfterTextureViews = new GpuTextureView[2];
        useTrailAAsHistory = true;
        irisOpenCaptureSlot = -1;
        lastWidth = -1;
        lastHeight = -1;
        lastIrisDepthWidth = -1;
        lastIrisDepthHeight = -1;
        protectedTagBounds = new float[512];
        smoothR = -1.0f;
    }

    public static void shutdown() {
        HandsFlameRenderer.closeTextures();
        HandsFlameRenderer.closeBlackTexture();
        if (uniformBuffer != null) {
            uniformBuffer.close();
        }
        if (dummyVertexBuffer != null) {
            dummyVertexBuffer.close();
        }
        if (dataBuffer != null) {
            MemoryUtil.memFree((Buffer)dataBuffer);
        }
        uniformBuffer = null;
        dummyVertexBuffer = null;
        dataBuffer = null;
        trailPipeline = null;
        compositePipeline = null;
        restorePipeline = null;
        capturedBeforeHands = false;
    }

    private static float clamp(float f, float f2, float f3) {
        return Math.max(f2, Math.min(f3, f));
    }

    public static record RenderRegion(int x, int y, int width, int height) {}

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static int channel(int n, int n2) {
        return n >>> n2 & 0xFF;
    }

    public static void configure(float f, float f2, float f3, float f4, float f5, int n, int n2, boolean bl, boolean bl2) {
        flameStrength = HandsFlameRenderer.clamp(f, 0.0f, 2.0f);
        flameRiseSpeed = HandsFlameRenderer.clamp(f2, 0.0f, 2.0f);
        flameWobble = HandsFlameRenderer.clamp(f3, 0.0f, 2.0f);
        flameLength = HandsFlameRenderer.clamp(f4, 0.1f, 2.5f);
        flameBrightness = HandsFlameRenderer.clamp(f5, 0.0f, 2.0f);
        flameColorMode = Math.max(0, Math.min(2, n));
        flameColor = n2;
        flameItemsOnly = bl;
        flameClientGradient = bl2;
    }

    public static void renderIrisCapturedHandsFlame() {
        int n = irisCaptureCount;
        irisCaptureCount = 0;
        irisOpenCaptureSlot = -1;
        if (n <= 0 || !HandsFlameRenderer.shouldRenderFlame()) {
            return;
        }
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!HandsFlameRenderer.isUsable(framebuffer) || !HandsFlameRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        try {
            RenderRegion renderRegion = HandsFlameRenderer.flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            HandsFlameRenderer.ensureRenderRegion(renderRegion);
            boolean bl = flameEnabled && (!flameItemsOnly || !MinecraftClient.getInstance().player.getMainHandStack().isEmpty() || !MinecraftClient.getInstance().player.getOffHandStack().isEmpty());
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), handTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            if (bl) {
                boolean bl2 = HandsFlameRenderer.advanceTrailClock();
                HandsFlameRenderer.writeUniforms(framebuffer.textureWidth, framebuffer.textureHeight, true, bl2 ? HandsFlameRenderer.consumeTrailStep() : 0.0f);
                commandEncoder.writeToBuffer(uniformBuffer.slice(0L, (long)dataBuffer.remaining()), dataBuffer);
                int n2 = -1;
                for (int i = 0; i < Math.min(n, 2); ++i) {
                    if (irisDepthBeforeTextureViews[i] == null || irisDepthAfterTextureViews[i] == null) continue;
                    if (bl2) {
                        HandsFlameRenderer.renderTrail(commandEncoder, framebuffer, irisDepthAfterTextureViews[i], irisDepthBeforeTextureViews[i], handTextureView, handTextureView);
                        HandsFlameRenderer.swapTrailHistory();
                    }
                    n2 = i;
                }
                if (n2 >= 0) {
                    HandsFlameRenderer.renderComposite(commandEncoder, framebuffer, irisDepthAfterTextureViews[n2], irisDepthBeforeTextureViews[n2], handTextureView, handTextureView, handTextureView);
                    HandsFlameRenderer.restoreProtectedNameTags(commandEncoder, framebuffer, handTextureView);
                }
            }
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    private static void ensureRenderRegion(RenderRegion renderRegion) {
        if (renderRegion.equals((Object)lastRenderRegion)) {
            return;
        }
        lastRenderRegion = renderRegion;
        HandsFlameRenderer.resetTrail();
    }

    public static boolean shouldRenderFlame() {
        if (disabledAfterError) {
            return false;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.player == null || minecraftClient.world == null) {
            return false;
        }
        if (!ShaderHands.isNewModeActive()) {
            return false;
        }
        return flameEnabled && (!flameItemsOnly || !minecraftClient.player.getMainHandStack().isEmpty() || !minecraftClient.player.getOffHandStack().isEmpty());
    }

    private static boolean ensureReady(int n, int n2) {
        if (trailPipeline == null || compositePipeline == null || restorePipeline == null || uniformBuffer == null || dummyVertexBuffer == null || dataBuffer == null) {
            HandsFlameRenderer.initPipelines();
        }
        HandsFlameRenderer.ensureTextures(n, n2);
        return trailPipeline != null && compositePipeline != null && restorePipeline != null && uniformBuffer != null && dummyVertexBuffer != null && dataBuffer != null && beforeTexture != null && sceneTexture != null && trailTextureA != null && trailTextureB != null && beforeTextureView != null && sceneTextureView != null && trailTextureViewA != null && trailTextureViewB != null;
    }

    private static void closeBlackTexture() {
        if (blackTextureView != null) {
            blackTextureView.close();
            blackTextureView = null;
        }
        if (blackTexture != null) {
            blackTexture.close();
            blackTexture = null;
        }
    }

    private static void writeUniforms(int n, int n2, boolean bl, float f) {
        boolean bl2;
        int n3 = flameColor;
        float f2 = (float)ColorUtil.alpha(n3) / 255.0f;
        float f3 = (float)HandsFlameRenderer.channel(n3, 16) / 255.0f;
        float f4 = (float)HandsFlameRenderer.channel(n3, 8) / 255.0f;
        float f5 = (float)HandsFlameRenderer.channel(n3, 0) / 255.0f;
        if (smoothR < 0.0f) {
            smoothR = f3;
            smoothG = f4;
            smoothB = f5;
        } else {
            float f6 = 1.0f - (float)Math.exp(-f * 10.0f);
            smoothR += (f3 - smoothR) * f6;
            smoothG += (f4 - smoothG) * f6;
            smoothB += (f5 - smoothB) * f6;
        }
        dataBuffer.clear();
        dataBuffer.putFloat(smoothR);
        dataBuffer.putFloat(smoothG);
        dataBuffer.putFloat(smoothB);
        dataBuffer.putFloat(f);
        dataBuffer.putFloat(flameStrength);
        dataBuffer.putFloat(flameRiseSpeed);
        dataBuffer.putFloat(flameWobble);
        dataBuffer.putFloat(flameLength);
        dataBuffer.putFloat(flameBrightness);
        dataBuffer.putFloat(HandsFlameRenderer.flameTime());
        dataBuffer.putFloat((float)flameColorMode + (flameItemsOnly ? 10.0f : 0.0f) + (bl ? 20.0f : 0.0f));
        dataBuffer.putFloat(f2);
        dataBuffer.putFloat(n);
        dataBuffer.putFloat(n2);
        dataBuffer.putFloat(1.0f / (float)Math.max(n, 1));
        dataBuffer.putFloat(1.0f / (float)Math.max(n2, 1));
        boolean bl3 = bl2 = flameClientGradient && ClientPalette.count() >= 2;
        if (bl2) {
            float f7 = ClientPalette.phase() * 20.0f;
            HandsFlameRenderer.putGradientColor(ClientPalette.loopColor(f7 + 0.75f));
            HandsFlameRenderer.putGradientColor(ClientPalette.loopColor(f7 + 0.5f));
            HandsFlameRenderer.putGradientColor(ClientPalette.loopColor(f7 + 0.25f));
            HandsFlameRenderer.putGradientColor(ClientPalette.loopColor(f7));
        } else {
            for (int i = 0; i < 16; ++i) {
                dataBuffer.putFloat(0.0f);
            }
        }
        dataBuffer.flip();
    }

    public static boolean hasCapturedHands() {
        return capturedBeforeHands;
    }

    private static float consumeTrailStep() {
        return pendingTrailStep;
    }

    private static void swapTrailHistory() {
        useTrailAAsHistory = !useTrailAAsHistory;
    }

    private static void restoreProtectedNameTags(CommandEncoder commandEncoder, Framebuffer framebuffer, GpuTextureView gpuTextureView) {
        int n = NameTags.copyUnderHandBounds(protectedTagBounds);
        float f = Position.screenWidth();
        float f2 = Position.screenHeight();
        if (n <= 0 || f <= 0.0f || f2 <= 0.0f || restorePipeline == null) {
            return;
        }
        float f3 = (float)framebuffer.textureWidth / f;
        float f4 = (float)framebuffer.textureHeight / f2;
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:hands_flame_restore_nametags", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty());){
            renderPass.setPipeline(restorePipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("SceneSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            for (int i = 0; i < n; ++i) {
                int n2 = i * 4;
                int n3 = Math.max(0, (int)Math.floor(protectedTagBounds[n2] * f3));
                int n4 = Math.max(0, (int)Math.floor(protectedTagBounds[n2 + 1] * f4));
                int n5 = Math.min(framebuffer.textureWidth, (int)Math.ceil((protectedTagBounds[n2] + protectedTagBounds[n2 + 2]) * f3));
                int n6 = Math.min(framebuffer.textureHeight, (int)Math.ceil((protectedTagBounds[n2 + 1] + protectedTagBounds[n2 + 3]) * f4));
                if (n5 <= n3 || n6 <= n4) continue;
                renderPass.enableScissor(n3, framebuffer.textureHeight - n6, n5 - n3, n6 - n4);
                renderPass.draw(0, 6);
            }
        }
    }

    private static void ensureTextures(int n, int n2) {
        if (beforeTexture != null && n == lastWidth && n2 == lastHeight) {
            return;
        }
        HandsFlameRenderer.closeTextures();
        int n3 = Math.max(1, n / 3);
        int n4 = Math.max(1, n2 / 3);
        beforeTexture = HandsFlameRenderer.createTexture("nv:hands_flame_before", n, n2, 7);
        sceneTexture = HandsFlameRenderer.createTexture("nv:hands_flame_scene", n, n2, 5);
        handTexture = HandsFlameRenderer.createTexture("nv:hands_flame_hand_temp", n, n2, 5);
        trailTextureA = HandsFlameRenderer.createTexture("nv:hands_flame_trail_a", n3, n4, 13);
        trailTextureB = HandsFlameRenderer.createTexture("nv:hands_flame_trail_b", n3, n4, 13);
        beforeTextureView = RenderSystem.getDevice().createTextureView(beforeTexture);
        sceneTextureView = RenderSystem.getDevice().createTextureView(sceneTexture);
        handTextureView = RenderSystem.getDevice().createTextureView(handTexture);
        trailTextureViewA = RenderSystem.getDevice().createTextureView(trailTextureA);
        trailTextureViewB = RenderSystem.getDevice().createTextureView(trailTextureB);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        commandEncoder.clearColorTexture(trailTextureA, 0);
        commandEncoder.clearColorTexture(trailTextureB, 0);
        useTrailAAsHistory = true;
        lastWidth = n;
        lastHeight = n2;
    }

    private static boolean ensureBlackTexture() {
        if (blackTexture != null && blackTextureView != null) {
            return true;
        }
        blackTexture = RenderSystem.getDevice().createTexture(() -> "nv:hands_flame_black", 13, TextureFormat.RGBA8, 1, 1, 1, 1);
        blackTextureView = RenderSystem.getDevice().createTextureView(blackTexture);
        RenderSystem.getDevice().createCommandEncoder().clearColorTexture(blackTexture, 0);
        return blackTextureView != null;
    }

    private static void ensureIrisDepthTextures(Framebuffer framebuffer) {
        GpuTexture gpuTexture = framebuffer.getDepthAttachment();
        TextureFormat textureFormat = gpuTexture.getFormat();
        if (irisDepthBeforeTextures[0] != null && framebuffer.textureWidth == lastIrisDepthWidth && framebuffer.textureHeight == lastIrisDepthHeight && textureFormat == lastIrisDepthFormat) {
            return;
        }
        HandsFlameRenderer.closeIrisDepthTextures();
        for (int i = 0; i < 2; ++i) {
            HandsFlameRenderer.irisDepthBeforeTextures[i] = RenderSystem.getDevice().createTexture(() -> "nv:hands_flame_iris_depth_before", 5, textureFormat, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
            HandsFlameRenderer.irisDepthAfterTextures[i] = RenderSystem.getDevice().createTexture(() -> "nv:hands_flame_iris_depth_after", 5, textureFormat, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
            HandsFlameRenderer.irisDepthBeforeTextureViews[i] = RenderSystem.getDevice().createTextureView(irisDepthBeforeTextures[i]);
            HandsFlameRenderer.irisDepthAfterTextureViews[i] = RenderSystem.getDevice().createTextureView(irisDepthAfterTextures[i]);
        }
        lastIrisDepthWidth = framebuffer.textureWidth;
        lastIrisDepthHeight = framebuffer.textureHeight;
        lastIrisDepthFormat = textureFormat;
    }

    private static void renderTrail(CommandEncoder commandEncoder, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4) {
        GpuTextureView gpuTextureView5 = ShaderHandsRenderer.handBoundsLeftView();
        GpuTextureView gpuTextureView6 = ShaderHandsRenderer.handBoundsRightView();
        if (gpuTextureView5 == null || gpuTextureView6 == null) {
            if (!HandsFlameRenderer.ensureBlackTexture()) {
                return;
            }
            gpuTextureView5 = blackTextureView;
            gpuTextureView6 = blackTextureView;
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:hands_flame_trail", HandsFlameRenderer.nextTrailView(), OptionalInt.empty(), null, OptionalDouble.empty());){
            renderPass.setPipeline(trailPipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("BeforeSampler", gpuTextureView3, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("AfterSampler", gpuTextureView4, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("PrevTrailSampler", HandsFlameRenderer.historyTrailView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("DepthSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("NoHandDepthSampler", gpuTextureView2, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("BoundsTexL", gpuTextureView5, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("BoundsTexR", gpuTextureView6, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }

    private static RenderRegion flameRegion(int n, int n2) {
        return new RenderRegion(0, 0, n, n2);
    }

    private static boolean advanceTrailClock() {
        long l = System.nanoTime();
        if (trailClockNs != 0L) {
            trailAccumulator += HandsFlameRenderer.clamp((float)(l - trailClockNs) / 1.0E9f, 0.0f, 0.1f);
        }
        trailClockNs = l;
        if (trailAccumulator >= 0.008333334f) {
            pendingTrailStep = Math.min(trailAccumulator, 0.05f);
            trailAccumulator = 0.0f;
            return true;
        }
        return false;
    }

    private static void closeTextures() {
        if (beforeTextureView != null) {
            beforeTextureView.close();
        }
        if (sceneTextureView != null) {
            sceneTextureView.close();
        }
        if (handTextureView != null) {
            handTextureView.close();
        }
        if (trailTextureViewA != null) {
            trailTextureViewA.close();
        }
        if (trailTextureViewB != null) {
            trailTextureViewB.close();
        }
        if (beforeTexture != null) {
            beforeTexture.close();
        }
        if (sceneTexture != null) {
            sceneTexture.close();
        }
        if (handTexture != null) {
            handTexture.close();
        }
        if (trailTextureA != null) {
            trailTextureA.close();
        }
        if (trailTextureB != null) {
            trailTextureB.close();
        }
        HandsFlameRenderer.closeIrisDepthTextures();
        beforeTextureView = null;
        sceneTextureView = null;
        handTextureView = null;
        trailTextureViewA = null;
        trailTextureViewB = null;
        beforeTexture = null;
        sceneTexture = null;
        handTexture = null;
        trailTextureA = null;
        trailTextureB = null;
        lastWidth = -1;
        lastHeight = -1;
        lastRenderRegion = null;
    }

    private static void putGradientColor(int n) {
        dataBuffer.putFloat((float)(n >> 16 & 0xFF) / 255.0f);
        dataBuffer.putFloat((float)(n >> 8 & 0xFF) / 255.0f);
        dataBuffer.putFloat((float)(n & 0xFF) / 255.0f);
        dataBuffer.putFloat(1.0f);
    }

    private static void initPipelines() {
        try {
            trailPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(TRAIL_PIPELINE_ID).withVertexShader(FULLSCREEN_VERTEX_SHADER).withFragmentShader(TRAIL_FRAGMENT_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER).withSampler("BeforeSampler").withSampler("AfterSampler").withSampler("PrevTrailSampler").withSampler("DepthSampler").withSampler("NoHandDepthSampler").withSampler("BoundsTexL").withSampler("BoundsTexR").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            compositePipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(COMPOSITE_PIPELINE_ID).withVertexShader(FULLSCREEN_VERTEX_SHADER).withFragmentShader(COMPOSITE_FRAGMENT_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("HandsFlameData", UniformType.UNIFORM_BUFFER).withSampler("SceneSampler").withSampler("BeforeSampler").withSampler("AfterSampler").withSampler("TrailSampler").withSampler("DepthSampler").withSampler("NoHandDepthSampler").withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            restorePipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(RESTORE_PIPELINE_ID).withVertexShader(FULLSCREEN_VERTEX_SHADER).withFragmentShader(RESTORE_FRAGMENT_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("SceneSampler").withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            dataBuffer = MemoryUtil.memAlloc((int)128);
            uniformBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:hands_flame_uniform", 136, 128L);
            ByteBuffer byteBuffer = MemoryUtil.memAlloc((int)4);
            byteBuffer.putInt(0);
            byteBuffer.flip();
            dummyVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:hands_flame_dummy_vertex", 40, byteBuffer);
            MemoryUtil.memFree((Buffer)byteBuffer);
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    private static void closeIrisDepthTextures() {
        for (int i = 0; i < 2; ++i) {
            if (irisDepthBeforeTextureViews[i] != null) {
                irisDepthBeforeTextureViews[i].close();
            }
            if (irisDepthAfterTextureViews[i] != null) {
                irisDepthAfterTextureViews[i].close();
            }
            if (irisDepthBeforeTextures[i] != null) {
                irisDepthBeforeTextures[i].close();
            }
            if (irisDepthAfterTextures[i] != null) {
                irisDepthAfterTextures[i].close();
            }
            HandsFlameRenderer.irisDepthBeforeTextureViews[i] = null;
            HandsFlameRenderer.irisDepthAfterTextureViews[i] = null;
            HandsFlameRenderer.irisDepthBeforeTextures[i] = null;
            HandsFlameRenderer.irisDepthAfterTextures[i] = null;
        }
        irisCaptureCount = 0;
        irisOpenCaptureSlot = -1;
        lastIrisDepthWidth = -1;
        lastIrisDepthHeight = -1;
        lastIrisDepthFormat = null;
    }

    private static void renderComposite(CommandEncoder commandEncoder, Framebuffer framebuffer, GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, GpuTextureView gpuTextureView3, GpuTextureView gpuTextureView4, GpuTextureView gpuTextureView5) {
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:hands_flame_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty(), null, OptionalDouble.empty());){
            renderPass.setPipeline(compositePipeline);
            renderPass.setVertexBuffer(0, dummyVertexBuffer);
            renderPass.bindTexture("SceneSampler", gpuTextureView5, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("BeforeSampler", gpuTextureView3, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("AfterSampler", gpuTextureView4, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("TrailSampler", HandsFlameRenderer.historyTrailView(), RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
            renderPass.bindTexture("DepthSampler", gpuTextureView, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.bindTexture("NoHandDepthSampler", gpuTextureView2, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
            renderPass.setUniform("HandsFlameData", uniformBuffer.slice());
            renderPass.draw(0, 6);
        }
    }

    private static GpuTextureView nextTrailView() {
        return useTrailAAsHistory ? trailTextureViewB : trailTextureViewA;
    }

    private static GpuTextureView historyTrailView() {
        return useTrailAAsHistory ? trailTextureViewA : trailTextureViewB;
    }

    public static void captureAfterHandRender() {
        if (!HandsFlameRenderer.shouldRenderFlame()) {
            return;
        }
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!HandsFlameRenderer.isUsable(framebuffer) || !HandsFlameRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        try {
            RenderRegion renderRegion = HandsFlameRenderer.flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            HandsFlameRenderer.ensureRenderRegion(renderRegion);
            if (!ShaderHandsRenderer.wasHandCapturedThisFrame()) {
                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getColorAttachment(), handTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            }
            capturedBeforeHands = true;
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    public static void captureBeforeHandRender() {
        capturedBeforeHands = false;
        if (!HandsFlameRenderer.shouldRenderFlame()) {
            return;
        }
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!HandsFlameRenderer.isUsable(framebuffer)) {
            return;
        }
        if (!HandsFlameRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        try {
            RenderRegion renderRegion = HandsFlameRenderer.flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            HandsFlameRenderer.ensureRenderRegion(renderRegion);
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getColorAttachment(), beforeTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    public static void renderCapturedHandsFlame() {
        if (!capturedBeforeHands) {
            return;
        }
        capturedBeforeHands = false;
        if (!HandsFlameRenderer.shouldRenderFlame()) {
            return;
        }
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!HandsFlameRenderer.isUsable(framebuffer) || !HandsFlameRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        try {
            RenderRegion renderRegion = HandsFlameRenderer.flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            HandsFlameRenderer.ensureRenderRegion(renderRegion);
            boolean bl = flameEnabled && (!flameItemsOnly || !MinecraftClient.getInstance().player.getMainHandStack().isEmpty() || !MinecraftClient.getInstance().player.getOffHandStack().isEmpty());
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (bl) {
                GpuTextureView gpuTextureView = ShaderHandsRenderer.capturedHandColorView();
                GpuTextureView gpuTextureView2 = ShaderHandsRenderer.capturedHandDepthView();
                boolean bl2 = gpuTextureView != null && gpuTextureView2 != null && HandsFlameRenderer.ensureBlackTexture();
                GpuTextureView gpuTextureView3 = bl2 ? blackTextureView : beforeTextureView;
                GpuTextureView gpuTextureView4 = bl2 ? gpuTextureView : handTextureView;
                GpuTextureView gpuTextureView5 = bl2 ? gpuTextureView2 : framebuffer.getDepthAttachmentView();
                boolean bl3 = HandsFlameRenderer.advanceTrailClock();
                HandsFlameRenderer.writeUniforms(framebuffer.textureWidth, framebuffer.textureHeight, false, bl3 ? HandsFlameRenderer.consumeTrailStep() : 0.0f);
                commandEncoder.writeToBuffer(uniformBuffer.slice(0L, (long)dataBuffer.remaining()), dataBuffer);
                if (bl3) {
                    HandsFlameRenderer.renderTrail(commandEncoder, framebuffer, gpuTextureView5, gpuTextureView5, gpuTextureView3, gpuTextureView4);
                    HandsFlameRenderer.swapTrailHistory();
                }
                commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneTexture, 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
                HandsFlameRenderer.renderComposite(commandEncoder, framebuffer, gpuTextureView5, gpuTextureView5, gpuTextureView3, gpuTextureView4, sceneTextureView);
            }
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    private static GpuTexture createTexture(String string, int n, int n2, int n3) {
        return RenderSystem.getDevice().createTexture(() -> string, n3, TextureFormat.RGBA8, n, n2, 1, 1);
    }

    public static void setFlameEnabled(boolean bl) {
        if (flameEnabled == bl) {
            return;
        }
        flameEnabled = bl;
        if (!bl) {
            HandsFlameRenderer.resetTrail();
            irisCaptureCount = 0;
            irisOpenCaptureSlot = -1;
        }
    }

    private static void disableAfterError(Throwable throwable) {
        disabledAfterError = true;
        NV.LOGGER.error("Hands flame renderer failed; disabling effect", throwable);
        HandsFlameRenderer.shutdown();
    }

    public static void beginIrisHandDepthCapture() {
        irisOpenCaptureSlot = -1;
        if (!HandsFlameRenderer.shouldRenderFlame()) {
            return;
        }
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!HandsFlameRenderer.isUsable(framebuffer) || !HandsFlameRenderer.ensureReady(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        try {
            RenderRegion renderRegion = HandsFlameRenderer.flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            HandsFlameRenderer.ensureRenderRegion(renderRegion);
            HandsFlameRenderer.ensureIrisDepthTextures(framebuffer);
            if (irisCaptureCount >= 2) {
                return;
            }
            irisOpenCaptureSlot = irisCaptureCount;
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getDepthAttachment(), irisDepthBeforeTextures[irisOpenCaptureSlot], 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
        }
        catch (Throwable throwable) {
            irisOpenCaptureSlot = -1;
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    public static void endIrisHandDepthCapture() {
        int n = irisOpenCaptureSlot;
        irisOpenCaptureSlot = -1;
        if (n < 0 || n >= 2) {
            return;
        }
        Framebuffer framebuffer = MinecraftClient.getInstance().getFramebuffer();
        if (!HandsFlameRenderer.isUsable(framebuffer) || irisDepthAfterTextures[n] == null) {
            return;
        }
        try {
            RenderRegion renderRegion = HandsFlameRenderer.flameRegion(framebuffer.textureWidth, framebuffer.textureHeight);
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getDepthAttachment(), irisDepthAfterTextures[n], 0, renderRegion.x, renderRegion.y, renderRegion.x, renderRegion.y, renderRegion.width, renderRegion.height);
            irisCaptureCount = Math.max(irisCaptureCount, n + 1);
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    public static void resetTrail() {
        capturedBeforeHands = false;
        if (trailTextureA == null || trailTextureB == null) {
            return;
        }
        try {
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.clearColorTexture(trailTextureA, 0);
            commandEncoder.clearColorTexture(trailTextureB, 0);
        }
        catch (Throwable throwable) {
            HandsFlameRenderer.disableAfterError(throwable);
        }
    }

    private static float flameTime() {
        return (float)(System.nanoTime() % 180000000000L) / 1.0E9f;
    }

    private static boolean isUsable(Framebuffer framebuffer) {
        return framebuffer != null && framebuffer.getColorAttachment() != null && framebuffer.getColorAttachmentView() != null && framebuffer.getDepthAttachmentView() != null && framebuffer.textureWidth > 0 && framebuffer.textureHeight > 0;
    }
}

