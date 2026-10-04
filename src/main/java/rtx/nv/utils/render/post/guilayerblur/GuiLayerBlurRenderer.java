package rtx.nv.utils.render.post.guilayerblur;
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
import java.nio.ByteOrder;
import java.util.List;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.state.ColoredQuadGuiElementRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.TextureSetup;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import rtx.nv.NV;
import rtx.nv.api.modules.impl.Utils.guishare.RemoteGuiWorld;
import rtx.nv.api.ui.window.GuiShatterAnimation;
import rtx.nv.api.ui.window.GuiShatterAnimation.Remap;
import rtx.nv.api.ui.window.WorldGuiCloseAnimation;
import rtx.nv.mixin.accessor.GuiGraphicsExtractorAccessor;
import rtx.nv.utils.render.others.RenderSampler;
import rtx.nv.utils.render.post.guilayerblur.GuiCapture;

public final class GuiLayerBlurRenderer {
    private static final int BLUR_UNIFORM_BYTES = 16;
    private static final int COMPOSITE_UNIFORM_BYTES = 16;
    private static final int SLOT_BLIT_UNIFORM_BYTES = 64;
    private static final int WORLD_QUAD_UNIFORM_BYTES = 96;
    private static final int WORLD_WARP_UNIFORM_BYTES = 512;
    private static final int WORLD_WARP_MAX_SLOTS = 5;
    private static final int WORLD_WARP_MATS_OFFSET = 16;
    private static final int WORLD_WARP_RECT_OFFSET = 336;
    private static final int WORLD_WARP_LOCAL_OFFSET = 416;
    private static final float TAPS_HALF = 16.0f;
    private static final Identifier BLUR_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/gaussian");
    private static final Identifier COMPOSITE_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/composite");
    private static final Identifier WORLD_COMPOSITE_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/composite_world");
    private static final Identifier WORLD_BACKDROP_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/world_backdrop");
    private static final Identifier WORLD_SLOTS_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/world_backdrop_slots");
    private static final Identifier SLOT_BLIT_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/slot_blit");
    private static final Identifier SLOT_BLIT_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/slot_blit");
    private static final Identifier FULLSCREEN = GuiLayerBlurRenderer.id("post/guilayerblur/fullscreen");
    private static final Identifier SHARD_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/shard");
    private static final Identifier SHARD_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/shard");
    private static final Identifier SHARD_OCCLUDED_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/shard_occluded");
    private static final Identifier SHARD_OCCLUDED_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/shard_occluded");
    private static final Identifier WORLD_QUAD_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/world_quad");
    private static final VertexFormat SHARD_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("Color", VertexFormatElement.COLOR).build();
    private static final Identifier WORLD_BACKDROP_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/world_backdrop");
    private static final Identifier WORLD_SLOTS_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/world_backdrop_slots");
    private static final Identifier GAUSSIAN_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/gaussian");
    private static final Identifier COMPOSITE_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/composite");
    private static final Identifier WORLD_OCCLUDED_SHADER = GuiLayerBlurRenderer.id("post/guilayerblur/composite_world_occluded");
    private static final Identifier WORLD_OCCLUDED_PIPELINE_ID = GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/composite_world_occluded");
    private static final RenderPipeline PANEL_BOUNDARY_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/panel_boundary")).withVertexShader("core/position_color").withFragmentShader("core/position_color").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withColorWrite(false).withCull(false).build();
    private static final ColoredQuadGuiElementRenderState PANEL_BOUNDARY = new ColoredQuadGuiElementRenderState(PANEL_BOUNDARY_PIPELINE, TextureSetup.empty(), (Matrix3x2fc)new Matrix3x2f(), 0, 0, 1, 1, 0, 0, null);
    private static final RenderPipeline POPUP_BOUNDARY_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/popup_boundary")).withVertexShader("core/position_color").withFragmentShader("core/position_color").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withColorWrite(false).withCull(false).build();
    private static final ColoredQuadGuiElementRenderState POPUP_BOUNDARY = new ColoredQuadGuiElementRenderState(POPUP_BOUNDARY_PIPELINE, TextureSetup.empty(), (Matrix3x2fc)new Matrix3x2f(), 0, 0, 1, 1, 0, 0, null);
    private static final RenderPipeline REMOTE_BEGIN_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/remote_begin")).withVertexShader("core/position_color").withFragmentShader("core/position_color").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withColorWrite(false).withCull(false).build();
    private static final ColoredQuadGuiElementRenderState REMOTE_BEGIN = new ColoredQuadGuiElementRenderState(REMOTE_BEGIN_PIPELINE, TextureSetup.empty(), (Matrix3x2fc)new Matrix3x2f(), 0, 0, 1, 1, 0, 0, null);
    private static final RenderPipeline REMOTE_END_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/remote_end")).withVertexShader("core/position_color").withFragmentShader("core/position_color").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withColorWrite(false).withCull(false).build();
    private static final ColoredQuadGuiElementRenderState REMOTE_END = new ColoredQuadGuiElementRenderState(REMOTE_END_PIPELINE, TextureSetup.empty(), (Matrix3x2fc)new Matrix3x2f(), 0, 0, 1, 1, 0, 0, null);
    private static final RenderPipeline REMOTE_CARD_BEGIN_PIPELINE = GuiLayerBlurRenderer.markerPipeline("remote_card_begin");
    private static final ColoredQuadGuiElementRenderState REMOTE_CARD_BEGIN = GuiLayerBlurRenderer.markerState(REMOTE_CARD_BEGIN_PIPELINE);
    private static final RenderPipeline REMOTE_CARD_END_PIPELINE = GuiLayerBlurRenderer.markerPipeline("remote_card_end");
    private static final ColoredQuadGuiElementRenderState REMOTE_CARD_END = GuiLayerBlurRenderer.markerState(REMOTE_CARD_END_PIPELINE);
    private static RenderPipeline blurPipeline;
    private static RenderPipeline compositePipeline;
    private static RenderPipeline worldCompositePipeline;
    private static RenderPipeline worldOccludedPipeline;
    private static RenderPipeline worldBackdropPipeline;
    private static RenderPipeline shardPipeline;
    private static RenderPipeline shardOccludedPipeline;
    private static RenderPipeline worldSlotsPipeline;
    private static RenderPipeline slotBlitPipeline;
    private static GpuBuffer blurUniform;
    private static GpuBuffer compositeUniform;
    private static GpuBuffer worldQuadUniform;
    private static GpuBuffer worldSlotsUniform;
    private static GpuBuffer slotBlitUniform;
    private static GpuBuffer shardVertexBuffer;
    private static ByteBuffer shardVertexData;
    private static final Matrix4f IDENTITY_MATRIX;
    private static SimpleFramebuffer guiFbo;
    private static SimpleFramebuffer tempH;
    private static SimpleFramebuffer tempV;
    private static int texWidth;
    private static int texHeight;
    private static SimpleFramebuffer sceneSnapshot;
    private static boolean captureActive;
    private static boolean panelRangeActive;
    private static boolean disabledAfterError;
    private static SimpleFramebuffer remoteFbo;
    private static boolean remoteCaptureActive;
    private static boolean remoteRouting;
    private static GpuTexture worldDepthCopy;
    private static GpuTextureView worldDepthCopyView;
    private static boolean worldDepthCopyReady;
    private static boolean depthCopyFailureLogged;
    private static GpuTexture handDepthCopy;
    private static GpuTextureView handDepthCopyView;
    private static boolean handDepthCopyReady;
    private static boolean handCopyFailureLogged;
    private static GpuTexture preCompositeColor;
    private static GpuTextureView preCompositeColorView;
    private static boolean preCompositeColorReady;

    private GuiLayerBlurRenderer() {
    }

    static {
        IDENTITY_MATRIX = new Matrix4f();
        texWidth = -1;
        texHeight = -1;
    }

    public static void shutdown() {
        captureActive = false;
        GuiLayerBlurRenderer.closeTargets();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static SimpleFramebuffer destroy(SimpleFramebuffer simpleFramebuffer) {
        if (simpleFramebuffer != null) {
            simpleFramebuffer.delete();
        }
        return null;
    }

    public static boolean available() {
        return !disabledAfterError;
    }

    private static float clamp01(float f) {
        return Math.max(0.0f, Math.min(1.0f, f));
    }

    private static boolean drawShards(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, float f, float f2, float f3, boolean bl) {
        if (shardPipeline == null || shardVertexBuffer == null || shardVertexData == null || tempH == null) {
            return false;
        }
        int n = GuiShatterAnimation.buildGeometry(shardVertexData, f, f2, f3, bl);
        if (n <= 0 || shardVertexData.remaining() <= 0) {
            return false;
        }
        commandEncoder.writeToBuffer(shardVertexBuffer.slice(0L, (long)shardVertexData.remaining()), shardVertexData);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_capture_shards", tempH.getColorAttachmentView(), OptionalInt.of(0));){
            renderPass.setPipeline(shardPipeline);
            renderPass.setVertexBuffer(0, shardVertexBuffer);
            renderPass.bindTexture("uGui", gpuTextureView, RenderSampler.linear());
            renderPass.setUniform("WorldQuadData", worldQuadUniform);
            renderPass.draw(0, n);
        }
        return true;
    }

    public static boolean isRemoteCardEnd(RenderPipeline renderPipeline) {
        return renderPipeline == REMOTE_CARD_END_PIPELINE;
    }

    public static Framebuffer captureTarget() {
        return captureActive && panelRangeActive && guiFbo != null ? guiFbo : null;
    }

    public static void beginCapture(boolean bl, boolean bl2) {
        Framebuffer framebuffer;
        captureActive = false;
        panelRangeActive = false;
        remoteCaptureActive = false;
        remoteRouting = false;
        RemoteGuiWorld.prepareCardBlurDraw();
        if (!bl && !bl2 || disabledAfterError) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
            return;
        }
        try {
            RenderPass renderPass;
            if (!GuiLayerBlurRenderer.ensurePipelines() || !GuiLayerBlurRenderer.ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight)) {
                return;
            }
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (bl) {
                renderPass = commandEncoder.createRenderPass(() -> "nv:gui_capture_clear", guiFbo.getColorAttachmentView(), OptionalInt.of(0), guiFbo.getDepthAttachmentView(), OptionalDouble.of(1.0));
                if (renderPass != null) {
                    renderPass.close();
                }
                captureActive = true;
            }
            if (bl2 && GuiLayerBlurRenderer.ensureRemoteFbo(framebuffer.textureWidth, framebuffer.textureHeight)) {
                renderPass = commandEncoder.createRenderPass(() -> "nv:gui_share_clear", remoteFbo.getColorAttachmentView(), OptionalInt.of(0), remoteFbo.getDepthAttachmentView(), OptionalDouble.of(1.0));
                if (renderPass != null) {
                    renderPass.close();
                }
                remoteCaptureActive = true;
            }
        }
        catch (Throwable throwable) {
            GuiLayerBlurRenderer.disableAfterError(throwable);
        }
    }

    public static void setRemoteRouting(boolean bl) {
        remoteRouting = bl;
    }

    public static boolean isRemoteRouting() {
        return remoteRouting;
    }

    public static boolean isRemoteCardBegin(RenderPipeline renderPipeline) {
        return renderPipeline == REMOTE_CARD_BEGIN_PIPELINE;
    }

    public static boolean isPopupBoundary(RenderPipeline renderPipeline) {
        return renderPipeline == POPUP_BOUNDARY_PIPELINE;
    }

    public static Framebuffer remoteCaptureTarget() {
        return remoteCaptureActive && remoteFbo != null ? remoteFbo : null;
    }

    public static boolean isPanelBoundary(RenderPipeline renderPipeline) {
        return renderPipeline == PANEL_BOUNDARY_PIPELINE;
    }

    public static void markPanelRange() {
        if (captureActive) {
            panelRangeActive = true;
        }
    }

    public static boolean isRemoteEnd(RenderPipeline renderPipeline) {
        return renderPipeline == REMOTE_END_PIPELINE;
    }

    public static boolean isRemoteBegin(RenderPipeline renderPipeline) {
        return renderPipeline == REMOTE_BEGIN_PIPELINE;
    }

    public static void composite(float f, float f2) {
        Framebuffer framebuffer;
        if (!captureActive || guiFbo == null || disabledAfterError) {
            captureActive = false;
            return;
        }
        captureActive = false;
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null || compositePipeline == null) {
            return;
        }
        int n = framebuffer.textureWidth;
        int n2 = framebuffer.textureHeight;
        if (n != texWidth || n2 != texHeight) {
            return;
        }
        try {
            float f3;
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            float f4 = GuiCapture.shatterProgress();
            boolean bl = GuiShatterAnimation.isActive() && f4 > 0.0f && tempH != null && tempV != null;
            boolean bl2 = WorldGuiCloseAnimation.isActive();
            Matrix4f matrix4f = IDENTITY_MATRIX;
            if (bl2 && (matrix4f = WorldGuiCloseAnimation.compositeMatrix(n, n2)) == null) {
                return;
            }
            if (bl) {
                float f5 = bl2 ? WorldGuiCloseAnimation.screenScale() : f;
                float f6 = bl2 ? WorldGuiCloseAnimation.alpha() : 1.0f;
                GuiLayerBlurRenderer.writeWorldQuadUniform(commandEncoder, matrix4f, n, n2, bl2 ? 1.0f : 0.0f);
                if (!GuiLayerBlurRenderer.drawShards(commandEncoder, guiFbo.getColorAttachmentView(), f4, f6, f5, bl2)) {
                    return;
                }
                if (f2 >= 0.5f) {
                    float f7 = f2 / 16.0f;
                    GuiLayerBlurRenderer.gaussianPass(commandEncoder, tempH.getColorAttachmentView(), tempV, f7 / (float)n, 0.0f);
                    GuiLayerBlurRenderer.gaussianPass(commandEncoder, tempV.getColorAttachmentView(), tempH, 0.0f, f7 / (float)n2);
                }
                GuiLayerBlurRenderer.writeCompositeUniform(commandEncoder, 1.0f, 1.0f);
                try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_capture_shard_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty());){
                    renderPass.setPipeline(compositePipeline);
                    renderPass.bindTexture("uGui", tempH.getColorAttachmentView(), RenderSampler.linear());
                    renderPass.setUniform("CompositeData", compositeUniform);
                    renderPass.draw(0, 6);
                }
                return;
            }
            GpuTextureView gpuTextureView = guiFbo.getColorAttachmentView();
            if (f2 >= 0.5f && tempH != null && tempV != null) {
                f3 = f2 / 16.0f;
                GuiLayerBlurRenderer.gaussianPass(commandEncoder, guiFbo.getColorAttachmentView(), tempH, f3 / (float)n, 0.0f);
                GuiLayerBlurRenderer.gaussianPass(commandEncoder, tempH.getColorAttachmentView(), tempV, 0.0f, f3 / (float)n2);
                gpuTextureView = tempV.getColorAttachmentView();
            }
            if (bl2) {
                Matrix4f matrix4f2 = matrix4f;
                float f8 = WorldGuiCloseAnimation.screenScale();
                float f9 = f8 > 1.0E-4f ? 1.0f / f8 : 1.0f;
                GuiLayerBlurRenderer.writeCompositeUniform(commandEncoder, f9, WorldGuiCloseAnimation.alpha());
                GuiLayerBlurRenderer.writeWorldQuadUniform(commandEncoder, matrix4f2, n, n2, 1.0f);
                try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_capture_composite_world", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.empty());){
                    renderPass.setPipeline(worldCompositePipeline);
                    renderPass.bindTexture("uGui", gpuTextureView, RenderSampler.linear());
                    renderPass.setUniform("CompositeData", compositeUniform);
                    renderPass.setUniform("WorldQuadData", worldQuadUniform);
                    renderPass.draw(0, 6);
                }
                return;
            }
            f3 = f > 1.0E-4f ? 1.0f / f : 1.0f;
            GuiLayerBlurRenderer.writeCompositeUniform(commandEncoder, f3, 1.0f);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_capture_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty());){
                renderPass.setPipeline(compositePipeline);
                renderPass.bindTexture("uGui", gpuTextureView, RenderSampler.linear());
                renderPass.setUniform("CompositeData", compositeUniform);
                renderPass.draw(0, 6);
            }
        }
        catch (Throwable throwable) {
            GuiLayerBlurRenderer.disableAfterError(throwable);
        }
    }

    public static void snapshotWorldDepth() {
        block6: {
            Framebuffer framebuffer;
            if (disabledAfterError || !RemoteGuiWorld.depthSnapshotWanted()) {
                return;
            }
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
            if (framebuffer == null || framebuffer.getDepthAttachment() == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
                worldDepthCopyReady = false;
                return;
            }
            try {
                if (worldDepthCopy != null && (worldDepthCopy.getWidth(0) != framebuffer.textureWidth || worldDepthCopy.getHeight(0) != framebuffer.textureHeight)) {
                    GuiLayerBlurRenderer.closeWorldDepthCopy();
                }
                if (worldDepthCopy == null) {
                    GpuDevice gpuDevice = RenderSystem.getDevice();
                    worldDepthCopy = gpuDevice.createTexture(() -> "nv:gui_share_world_depth", 5, TextureFormat.DEPTH32, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
                    worldDepthCopyView = gpuDevice.createTextureView(worldDepthCopy);
                }
                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getDepthAttachment(), worldDepthCopy, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
                worldDepthCopyReady = true;
                handDepthCopyReady = false;
                preCompositeColorReady = false;
            }
            catch (Throwable throwable) {
                GuiLayerBlurRenderer.closeWorldDepthCopy();
                if (depthCopyFailureLogged) break block6;
                depthCopyFailureLogged = true;
                NV.LOGGER.warn("[GuiShare] world depth snapshot failed", throwable);
            }
        }
    }

    public static boolean captureActiveThisFrame() {
        return captureActive && guiFbo != null;
    }

    public static void compositeRemotePanels() {
        Framebuffer framebuffer;
        List<RemoteGuiWorld.PanelQuad> list = RemoteGuiWorld.consumeQuads();
        if (list.isEmpty() || remoteFbo == null || disabledAfterError) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        if (framebuffer.textureWidth != texWidth || framebuffer.textureHeight != texHeight) {
            return;
        }
        try {
            if (!GuiLayerBlurRenderer.ensurePipelines()) {
                return;
            }
            GuiLayerBlurRenderer.capturePreCompositeColor(framebuffer);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            boolean bl = worldDepthCopyReady && worldDepthCopyView != null && worldOccludedPipeline != null;
            for (RemoteGuiWorld.PanelQuad panelQuad : list) {
                Matrix4f matrix4f = RemoteGuiWorld.freshMatrix(panelQuad);
                if (matrix4f == null) {
                    matrix4f = panelQuad.matrix();
                }
                if (panelQuad.shatter() != null && GuiLayerBlurRenderer.drawRemoteShards(commandEncoder, framebuffer, panelQuad, matrix4f)) continue;
                GuiLayerBlurRenderer.writeCompositeUniform(commandEncoder, 1.0f, GuiLayerBlurRenderer.clamp01(panelQuad.opacity()));
                GuiLayerBlurRenderer.writeWorldQuadUniform(commandEncoder, matrix4f, panelQuad.width(), panelQuad.height(), 1.0f, panelQuad.u() / (float)texWidth, 1.0f - panelQuad.v() / (float)texHeight, (panelQuad.u() + panelQuad.uWidth()) / (float)texWidth, 1.0f - (panelQuad.v() + panelQuad.vHeight()) / (float)texHeight);
                GuiLayerBlurRenderer.drawRemoteQuad(commandEncoder, framebuffer, bl);
            }
        }
        catch (Throwable throwable) {
            GuiLayerBlurRenderer.disableAfterError(throwable);
        }
    }

    public static void snapshotHandDepth() {
        block6: {
            Framebuffer framebuffer;
            if (disabledAfterError || !worldDepthCopyReady) {
                handDepthCopyReady = false;
                return;
            }
            MinecraftClient minecraftClient = MinecraftClient.getInstance();
            Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
            if (framebuffer == null || framebuffer.getDepthAttachment() == null || framebuffer.textureWidth <= 0 || framebuffer.textureHeight <= 0) {
                handDepthCopyReady = false;
                return;
            }
            try {
                if (handDepthCopy != null && (handDepthCopy.getWidth(0) != framebuffer.textureWidth || handDepthCopy.getHeight(0) != framebuffer.textureHeight)) {
                    GuiLayerBlurRenderer.closeHandDepthCopy();
                }
                if (handDepthCopy == null) {
                    GpuDevice gpuDevice = RenderSystem.getDevice();
                    handDepthCopy = gpuDevice.createTexture(() -> "nv:gui_share_hand_depth", 5, TextureFormat.DEPTH32, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
                    handDepthCopyView = gpuDevice.createTextureView(handDepthCopy);
                }
                RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getDepthAttachment(), handDepthCopy, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
                handDepthCopyReady = true;
            }
            catch (Throwable throwable) {
                GuiLayerBlurRenderer.closeHandDepthCopy();
                if (handCopyFailureLogged) break block6;
                handCopyFailureLogged = true;
                NV.LOGGER.warn("[GuiShare] hand depth snapshot failed", throwable);
            }
        }
    }

    public static Framebuffer worldBackdropSource(Framebuffer framebuffer) {
        if (framebuffer == null || disabledAfterError || framebuffer.getColorAttachmentView() == null) {
            return framebuffer;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return framebuffer;
        }
        List list = RemoteGuiWorld.captureRequested() ? RemoteGuiWorld.peekQuads() : List.of();
        Matrix4f matrix4f = null;
        if (WorldGuiCloseAnimation.isActive() && minecraftClient.currentScreen == null) {
            matrix4f = WorldGuiCloseAnimation.compositeMatrix(framebuffer.textureWidth, framebuffer.textureHeight);
        }
        if (list.isEmpty() && matrix4f == null) {
            return framebuffer;
        }
        try {
            if (!GuiLayerBlurRenderer.ensurePipelines()) {
                return framebuffer;
            }
            GuiLayerBlurRenderer.ensureSceneSnapshotTarget(framebuffer.textureWidth, framebuffer.textureHeight);
            if (sceneSnapshot == null) {
                return framebuffer;
            }
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (list.isEmpty()) {
                GuiLayerBlurRenderer.writeWorldQuadUniform(commandEncoder, matrix4f, framebuffer.textureWidth, framebuffer.textureHeight, 1.0f);
                try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:world_backdrop_warp", sceneSnapshot.getColorAttachmentView(), OptionalInt.of(0));){
                    renderPass.setPipeline(worldBackdropPipeline);
                    renderPass.bindTexture("uGui", GuiLayerBlurRenderer.worldWarpSourceView(framebuffer), RenderSampler.linear());
                    renderPass.setUniform("WorldQuadData", worldQuadUniform);
                    renderPass.draw(0, 6);
                }
                return sceneSnapshot;
            }
            GuiLayerBlurRenderer.writeWorldWarpUniform(commandEncoder, list, matrix4f, framebuffer.textureWidth, framebuffer.textureHeight);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:world_backdrop_warp_slots", sceneSnapshot.getColorAttachmentView(), OptionalInt.of(0));){
                renderPass.setPipeline(worldSlotsPipeline);
                renderPass.bindTexture("uGui", GuiLayerBlurRenderer.worldWarpSourceView(framebuffer), RenderSampler.linear());
                renderPass.setUniform("WorldWarpData", worldSlotsUniform);
                renderPass.draw(0, 6);
            }
            return sceneSnapshot;
        }
        catch (Throwable throwable) {
            GuiLayerBlurRenderer.disableAfterError(throwable);
            return framebuffer;
        }
    }

    public static Framebuffer worldSnapshotWithPanels() {
        Framebuffer framebuffer;
        if (disabledAfterError || remoteFbo == null || remoteFbo.getColorAttachmentView() == null) {
            return null;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer2 = framebuffer = minecraftClient != null ? minecraftClient.getFramebuffer() : null;
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return null;
        }
        Framebuffer framebuffer3 = GuiLayerBlurRenderer.worldBackdropSource(framebuffer);
        if (framebuffer3 != sceneSnapshot || sceneSnapshot == null || sceneSnapshot.getColorAttachmentView() == null) {
            return framebuffer3;
        }
        try {
            if (!GuiLayerBlurRenderer.ensurePipelines()) {
                return null;
            }
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            GuiLayerBlurRenderer.writeCompositeUniform(commandEncoder, 1.0f, 1.0f);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_share_popup_backdrop", sceneSnapshot.getColorAttachmentView(), OptionalInt.empty());){
                renderPass.setPipeline(compositePipeline);
                renderPass.bindTexture("uGui", remoteFbo.getColorAttachmentView(), RenderSampler.linear());
                renderPass.setUniform("CompositeData", compositeUniform);
                renderPass.draw(0, 6);
            }
            GuiLayerBlurRenderer.blitPanelsIntoPopupSlots(commandEncoder);
            return sceneSnapshot;
        }
        catch (Throwable throwable) {
            GuiLayerBlurRenderer.disableAfterError(throwable);
            return null;
        }
    }

    public static void markPanelEnd(DrawContext drawContext) {
        if (drawContext == null || !GuiCapture.active() || !GuiCapture.emitPanelBoundary()) {
            return;
        }
        GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
        guiRenderState.createNewRootLayer();
        guiRenderState.addSimpleElement((SimpleGuiElementRenderState)PANEL_BOUNDARY);
        guiRenderState.createNewRootLayer();
    }

    public static void markPopupBoundary(DrawContext drawContext) {
        if (drawContext == null) {
            return;
        }
        GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
        guiRenderState.createNewRootLayer();
        guiRenderState.addSimpleElement((SimpleGuiElementRenderState)POPUP_BOUNDARY);
        guiRenderState.createNewRootLayer();
    }

    private static void closeHandDepthCopy() {
        handDepthCopyReady = false;
        if (handDepthCopyView != null) {
            handDepthCopyView.close();
            handDepthCopyView = null;
        }
        if (handDepthCopy != null) {
            handDepthCopy.close();
            handDepthCopy = null;
        }
    }

    private static void writeWorldWarpUniform(CommandEncoder commandEncoder, List<RemoteGuiWorld.PanelQuad> list, Matrix4f matrix4f, float f, float f2) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(512);
            int n = 0;
            for (RemoteGuiWorld.PanelQuad panelQuad : list) {
                if (n >= 4) break;
                panelQuad.matrix().get(16 + n * 64, byteBuffer);
                int n2 = 336 + n * 16;
                byteBuffer.putFloat(n2, panelQuad.u());
                byteBuffer.putFloat(n2 + 4, panelQuad.v());
                byteBuffer.putFloat(n2 + 8, panelQuad.uWidth());
                byteBuffer.putFloat(n2 + 12, panelQuad.vHeight());
                int n3 = 416 + n * 16;
                byteBuffer.putFloat(n3, panelQuad.width() / Math.max(panelQuad.uWidth(), 1.0f));
                byteBuffer.putFloat(n3 + 4, panelQuad.height() / Math.max(panelQuad.vHeight(), 1.0f));
                ++n;
            }
            if (matrix4f != null && n < 5) {
                matrix4f.get(16 + n * 64, byteBuffer);
                int n4 = 336 + n * 16;
                byteBuffer.putFloat(n4 + 8, f);
                byteBuffer.putFloat(n4 + 12, f2);
                int n5 = 416 + n * 16;
                byteBuffer.putFloat(n5, 1.0f);
                byteBuffer.putFloat(n5 + 4, 1.0f);
                ++n;
            }
            byteBuffer.putFloat(0, n);
            byteBuffer.putFloat(4, f);
            byteBuffer.putFloat(8, f2);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(worldSlotsUniform.slice(0L, 512L), byteBuffer);
        }
    }

    private static void closeTargets() {
        guiFbo = GuiLayerBlurRenderer.destroy(guiFbo);
        tempH = GuiLayerBlurRenderer.destroy(tempH);
        tempV = GuiLayerBlurRenderer.destroy(tempV);
        sceneSnapshot = GuiLayerBlurRenderer.destroy(sceneSnapshot);
        remoteFbo = GuiLayerBlurRenderer.destroy(remoteFbo);
        GuiLayerBlurRenderer.closeWorldDepthCopy();
        GuiLayerBlurRenderer.closeHandDepthCopy();
        GuiLayerBlurRenderer.closePreCompositeColor();
        texWidth = -1;
        texHeight = -1;
    }

    private static void closeWorldDepthCopy() {
        worldDepthCopyReady = false;
        if (worldDepthCopyView != null) {
            worldDepthCopyView.close();
            worldDepthCopyView = null;
        }
        if (worldDepthCopy != null) {
            worldDepthCopy.close();
            worldDepthCopy = null;
        }
    }

    private static RenderPipeline markerPipeline(String string) {
        return RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(GuiLayerBlurRenderer.id("pipeline/post/guilayerblur/" + string)).withVertexShader("core/position_color").withFragmentShader("core/position_color").withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withColorWrite(false).withCull(false).build();
    }

    private static GpuTextureView occlusionDepthView() {
        return worldDepthCopyReady && worldDepthCopyView != null ? worldDepthCopyView : null;
    }

    private static void ensureSceneSnapshotTarget(int n, int n2) {
        if (sceneSnapshot != null && GuiLayerBlurRenderer.sceneSnapshot.textureWidth == n && GuiLayerBlurRenderer.sceneSnapshot.textureHeight == n2) {
            return;
        }
        sceneSnapshot = GuiLayerBlurRenderer.destroy(sceneSnapshot);
        try {
            sceneSnapshot = new SimpleFramebuffer("nv_world_backdrop_snapshot", n, n2, false);
        }
        catch (Throwable throwable) {
            sceneSnapshot = null;
        }
    }

    private static void gaussianPass(CommandEncoder commandEncoder, GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, float f, float f2) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(blurUniform.slice(0L, 16L), byteBuffer);
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_capture_gaussian", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0))) {
            renderPass.setPipeline(blurPipeline);
            renderPass.bindTexture("uInput", gpuTextureView, RenderSampler.linear());
            renderPass.setUniform("BlurData", blurUniform);
            renderPass.draw(0, 6);
        }
    }

    private static void blitPanelsIntoPopupSlots(CommandEncoder commandEncoder) {
        if (slotBlitPipeline == null || slotBlitUniform == null || sceneSnapshot == null) {
            return;
        }
        List<RemoteGuiWorld.PanelQuad> list = RemoteGuiWorld.peekQuads();
        if (list.isEmpty()) {
            return;
        }
        float f = GuiLayerBlurRenderer.sceneSnapshot.textureWidth;
        float f2 = GuiLayerBlurRenderer.sceneSnapshot.textureHeight;
        for (RemoteGuiWorld.PanelQuad panelQuad : list) {
            RemoteGuiWorld.SlotBlit slotBlit = panelQuad.blit();
            if (slotBlit == null || panelQuad.uWidth() < 1.0f || panelQuad.vHeight() < 1.0f || slotBlit.srcW() < 1.0f || slotBlit.srcH() < 1.0f) continue;
            try (MemoryStack memoryStack = MemoryStack.stackPush();){
                ByteBuffer byteBuffer = memoryStack.calloc(64);
                byteBuffer.putFloat(0, panelQuad.u());
                byteBuffer.putFloat(4, panelQuad.v());
                byteBuffer.putFloat(8, panelQuad.uWidth());
                byteBuffer.putFloat(12, panelQuad.vHeight());
                byteBuffer.putFloat(16, slotBlit.srcX());
                byteBuffer.putFloat(20, slotBlit.srcY());
                byteBuffer.putFloat(24, slotBlit.srcW());
                byteBuffer.putFloat(28, slotBlit.srcH());
                byteBuffer.putFloat(32, slotBlit.clipX());
                byteBuffer.putFloat(36, slotBlit.clipY());
                byteBuffer.putFloat(40, slotBlit.clipW());
                byteBuffer.putFloat(44, slotBlit.clipH());
                byteBuffer.putFloat(48, f);
                byteBuffer.putFloat(52, f2);
                byteBuffer.position(0);
                commandEncoder.writeToBuffer(slotBlitUniform.slice(0L, 64L), byteBuffer);
            }
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_share_popup_slot_blit", sceneSnapshot.getColorAttachmentView(), OptionalInt.empty())) {
                renderPass.setPipeline(slotBlitPipeline);
                renderPass.bindTexture("uGui", remoteFbo.getColorAttachmentView(), RenderSampler.linear());
                renderPass.setUniform("SlotBlitData", slotBlitUniform);
                renderPass.draw(0, 6);
            }
        }
    }

    private static boolean ensureTargets(int n, int n2) {
        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        if (gpuDevice == null || n <= 0 || n2 <= 0) {
            return false;
        }
        if (guiFbo != null && tempH != null && tempV != null && n == texWidth && n2 == texHeight) {
            return true;
        }
        GuiLayerBlurRenderer.closeTargets();
        guiFbo = new SimpleFramebuffer("nv_gui_capture", n, n2, true);
        tempH = new SimpleFramebuffer("nv_gui_capture_h", n, n2, false);
        tempV = new SimpleFramebuffer("nv_gui_capture_v", n, n2, false);
        texWidth = n;
        texHeight = n2;
        return true;
    }

    private static GpuTextureView worldWarpSourceView(Framebuffer framebuffer) {
        return preCompositeColorReady && preCompositeColorView != null ? preCompositeColorView : framebuffer.getColorAttachmentView();
    }

    private static void writeCompositeUniform(CommandEncoder commandEncoder, float f, float f2) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, f);
            byteBuffer.putFloat(4, f2);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(compositeUniform.slice(0L, 16L), byteBuffer);
        }
    }

    private static void capturePreCompositeColor(Framebuffer framebuffer) {
        if (framebuffer.getColorAttachment() == null) {
            preCompositeColorReady = false;
            return;
        }
        try {
            if (preCompositeColor != null && (preCompositeColor.getWidth(0) != framebuffer.textureWidth || preCompositeColor.getHeight(0) != framebuffer.textureHeight)) {
                GuiLayerBlurRenderer.closePreCompositeColor();
            }
            if (preCompositeColor == null) {
                GpuDevice gpuDevice = RenderSystem.getDevice();
                preCompositeColor = gpuDevice.createTexture(() -> "nv:gui_share_scene_before", 5, TextureFormat.RGBA8, framebuffer.textureWidth, framebuffer.textureHeight, 1, 1);
                preCompositeColorView = gpuDevice.createTextureView(preCompositeColor);
            }
            RenderSystem.getDevice().createCommandEncoder().copyTextureToTexture(framebuffer.getColorAttachment(), preCompositeColor, 0, 0, 0, 0, 0, framebuffer.textureWidth, framebuffer.textureHeight);
            preCompositeColorReady = true;
        }
        catch (Throwable throwable) {
            GuiLayerBlurRenderer.closePreCompositeColor();
        }
    }

    private static boolean ensureRemoteFbo(int n, int n2) {
        if (remoteFbo != null && GuiLayerBlurRenderer.remoteFbo.textureWidth == n && GuiLayerBlurRenderer.remoteFbo.textureHeight == n2) {
            return remoteFbo.getColorAttachmentView() != null;
        }
        if (remoteFbo != null) {
            remoteFbo.delete();
            remoteFbo = null;
        }
        return (remoteFbo = new SimpleFramebuffer("nv_gui_share_capture", n, n2, true)).getColorAttachmentView() != null;
    }

    private static GpuTextureView occlusionHandView() {
        if (handDepthCopyReady && handDepthCopyView != null) {
            return handDepthCopyView;
        }
        return GuiLayerBlurRenderer.occlusionDepthView();
    }

    private static void writeWorldQuadUniform(CommandEncoder commandEncoder, Matrix4f matrix4f, float f, float f2, float f3, float f4, float f5, float f6, float f7) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(96);
            matrix4f.get(0, byteBuffer);
            byteBuffer.putFloat(64, f);
            byteBuffer.putFloat(68, f2);
            byteBuffer.putFloat(72, f3);
            byteBuffer.putFloat(80, f4);
            byteBuffer.putFloat(84, f5);
            byteBuffer.putFloat(88, f6);
            byteBuffer.putFloat(92, f7);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(worldQuadUniform.slice(0L, 96L), byteBuffer);
        }
    }

    private static void writeWorldQuadUniform(CommandEncoder commandEncoder, Matrix4f matrix4f, float f, float f2, float f3) {
        GuiLayerBlurRenderer.writeWorldQuadUniform(commandEncoder, matrix4f, f, f2, f3, 0.0f, 1.0f, 1.0f, 0.0f);
    }

    private static void drawRemoteQuad(CommandEncoder commandEncoder, Framebuffer framebuffer, boolean bl) {
        if (bl) {
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_share_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty());){
                renderPass.setPipeline(worldOccludedPipeline);
                renderPass.bindTexture("uGui", remoteFbo.getColorAttachmentView(), RenderSampler.linear());
                renderPass.bindTexture("uDepth", worldDepthCopyView, RenderSampler.nearest());
                renderPass.bindTexture("uHandDepth", GuiLayerBlurRenderer.occlusionHandView(), RenderSampler.nearest());
                renderPass.setUniform("CompositeData", compositeUniform);
                renderPass.setUniform("WorldQuadData", worldQuadUniform);
                renderPass.draw(0, 6);
            }
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_share_composite", framebuffer.getColorAttachmentView(), OptionalInt.empty(), framebuffer.getDepthAttachmentView(), OptionalDouble.empty());){
            renderPass.setPipeline(worldCompositePipeline);
            renderPass.bindTexture("uGui", remoteFbo.getColorAttachmentView(), RenderSampler.linear());
            renderPass.setUniform("CompositeData", compositeUniform);
            renderPass.setUniform("WorldQuadData", worldQuadUniform);
            renderPass.draw(0, 6);
        }
    }

    private static void closePreCompositeColor() {
        preCompositeColorReady = false;
        if (preCompositeColorView != null) {
            preCompositeColorView.close();
            preCompositeColorView = null;
        }
        if (preCompositeColor != null) {
            preCompositeColor.close();
            preCompositeColor = null;
        }
    }

    private static void submitMarker(DrawContext drawContext, ColoredQuadGuiElementRenderState coloredQuadGuiElementRenderState) {
        if (drawContext == null) {
            return;
        }
        GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
        guiRenderState.createNewRootLayer();
        guiRenderState.addSimpleElement((SimpleGuiElementRenderState)coloredQuadGuiElementRenderState);
        guiRenderState.createNewRootLayer();
    }

    private static boolean ensurePipelines() {
        if (blurPipeline != null && compositePipeline != null && worldCompositePipeline != null && worldBackdropPipeline != null && shardPipeline != null && worldSlotsPipeline != null && slotBlitPipeline != null && blurUniform != null && compositeUniform != null && worldQuadUniform != null && shardVertexBuffer != null && worldSlotsUniform != null && slotBlitUniform != null) {
            return true;
        }
        if (blurPipeline == null) {
            blurPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(BLUR_PIPELINE_ID).withVertexShader(FULLSCREEN).withFragmentShader(GAUSSIAN_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uInput").withUniform("BlurData", UniformType.UNIFORM_BUFFER).withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (compositePipeline == null) {
            compositePipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(COMPOSITE_PIPELINE_ID).withVertexShader(FULLSCREEN).withFragmentShader(COMPOSITE_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withUniform("CompositeData", UniformType.UNIFORM_BUFFER).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (worldCompositePipeline == null) {
            worldCompositePipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(WORLD_COMPOSITE_PIPELINE_ID).withVertexShader(WORLD_QUAD_SHADER).withFragmentShader(COMPOSITE_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withUniform("CompositeData", UniformType.UNIFORM_BUFFER).withUniform("WorldQuadData", UniformType.UNIFORM_BUFFER).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (worldOccludedPipeline == null) {
            worldOccludedPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(WORLD_OCCLUDED_PIPELINE_ID).withVertexShader(WORLD_QUAD_SHADER).withFragmentShader(WORLD_OCCLUDED_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withSampler("uDepth").withSampler("uHandDepth").withUniform("CompositeData", UniformType.UNIFORM_BUFFER).withUniform("WorldQuadData", UniformType.UNIFORM_BUFFER).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (worldBackdropPipeline == null) {
            worldBackdropPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(WORLD_BACKDROP_PIPELINE_ID).withVertexShader(FULLSCREEN).withFragmentShader(WORLD_BACKDROP_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withUniform("WorldQuadData", UniformType.UNIFORM_BUFFER).withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (shardPipeline == null) {
            shardPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(SHARD_PIPELINE_ID).withVertexShader(SHARD_SHADER).withFragmentShader(SHARD_SHADER).withVertexFormat(SHARD_FORMAT, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withUniform("WorldQuadData", UniformType.UNIFORM_BUFFER).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (shardOccludedPipeline == null) {
            shardOccludedPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(SHARD_OCCLUDED_PIPELINE_ID).withVertexShader(SHARD_SHADER).withFragmentShader(SHARD_OCCLUDED_SHADER).withVertexFormat(SHARD_FORMAT, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withSampler("uDepth").withSampler("uHandDepth").withUniform("WorldQuadData", UniformType.UNIFORM_BUFFER).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (worldSlotsPipeline == null) {
            worldSlotsPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(WORLD_SLOTS_PIPELINE_ID).withVertexShader(FULLSCREEN).withFragmentShader(WORLD_SLOTS_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withUniform("WorldWarpData", UniformType.UNIFORM_BUFFER).withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (slotBlitPipeline == null) {
            slotBlitPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(SLOT_BLIT_PIPELINE_ID).withVertexShader(FULLSCREEN).withFragmentShader(SLOT_BLIT_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("uGui").withUniform("SlotBlitData", UniformType.UNIFORM_BUFFER).withBlend(new BlendFunction(SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA)).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
        }
        if (shardVertexBuffer == null) {
            shardVertexBuffer = RenderSystem.getDevice().createBuffer(() -> "nv:guilayerblur_shard_vertices", 40, 44352L);
        }
        if (shardVertexData == null) {
            shardVertexData = ByteBuffer.allocateDirect(44352).order(ByteOrder.nativeOrder());
        }
        if (blurUniform == null) {
            blurUniform = RenderSystem.getDevice().createBuffer(() -> "nv:guilayerblur_blur_uniform", 136, 16L);
        }
        if (compositeUniform == null) {
            compositeUniform = RenderSystem.getDevice().createBuffer(() -> "nv:guilayerblur_composite_uniform", 136, 16L);
        }
        if (worldQuadUniform == null) {
            worldQuadUniform = RenderSystem.getDevice().createBuffer(() -> "nv:guilayerblur_world_quad_uniform", 136, 96L);
        }
        if (worldSlotsUniform == null) {
            worldSlotsUniform = RenderSystem.getDevice().createBuffer(() -> "nv:guilayerblur_world_warp_uniform", 136, 512L);
        }
        if (slotBlitUniform == null) {
            slotBlitUniform = RenderSystem.getDevice().createBuffer(() -> "nv:guilayerblur_slot_blit_uniform", 136, 64L);
        }
        return blurPipeline != null && compositePipeline != null && worldCompositePipeline != null && worldBackdropPipeline != null && shardPipeline != null && worldSlotsPipeline != null && slotBlitPipeline != null && blurUniform != null && compositeUniform != null && worldQuadUniform != null && shardVertexBuffer != null && shardVertexData != null && worldSlotsUniform != null && slotBlitUniform != null;
    }

    private static boolean drawRemoteShards(CommandEncoder commandEncoder, Framebuffer framebuffer, RemoteGuiWorld.PanelQuad panelQuad, Matrix4f matrix4f) {
        boolean bl;
        RemoteGuiWorld.Shatter shatter = panelQuad.shatter();
        if (shatter == null || shardVertexBuffer == null || shardVertexData == null) {
            return false;
        }
        boolean bl2 = bl = worldDepthCopyReady && worldDepthCopyView != null && shardOccludedPipeline != null;
        if (!bl && shardPipeline == null) {
            return false;
        }
        float f = panelQuad.uWidth() / (float)texWidth * shatter.width();
        float f2 = panelQuad.u() / (float)texWidth + panelQuad.uWidth() / (float)texWidth * shatter.x();
        float f3 = panelQuad.vHeight() / (float)texHeight * shatter.height();
        float f4 = 1.0f - panelQuad.v() / (float)texHeight - panelQuad.vHeight() / (float)texHeight * (shatter.y() + shatter.height());
        GuiShatterAnimation.Remap remap = new GuiShatterAnimation.Remap(shatter.x(), shatter.y(), shatter.width(), shatter.height(), f2, f4, f, f3);
        int n = ((rtx.nv.api.ui.window.GuiShatterAnimation.State)shatter.state()).buildGeometry(shardVertexData, shatter.progress(), shatter.alpha(), 1.0f, true, remap);
        if (n <= 0 || shardVertexData.remaining() <= 0) {
            return false;
        }
        GuiLayerBlurRenderer.writeWorldQuadUniform(commandEncoder, matrix4f != null ? matrix4f : panelQuad.matrix(), panelQuad.width(), panelQuad.height(), 1.0f);
        commandEncoder.writeToBuffer(shardVertexBuffer.slice(0L, (long)shardVertexData.remaining()), shardVertexData);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:gui_share_shards", framebuffer.getColorAttachmentView(), OptionalInt.empty());){
            renderPass.setPipeline(bl ? shardOccludedPipeline : shardPipeline);
            renderPass.setVertexBuffer(0, shardVertexBuffer);
            renderPass.bindTexture("uGui", remoteFbo.getColorAttachmentView(), RenderSampler.linear());
            if (bl) {
                renderPass.bindTexture("uDepth", worldDepthCopyView, RenderSampler.nearest());
                renderPass.bindTexture("uHandDepth", GuiLayerBlurRenderer.occlusionHandView(), RenderSampler.nearest());
            }
            renderPass.setUniform("WorldQuadData", worldQuadUniform);
            renderPass.draw(0, n);
        }
        return true;
    }

    private static void disableAfterError(Throwable throwable) {
        disabledAfterError = true;
        captureActive = false;
        GuiLayerBlurRenderer.closeTargets();
        NV.LOGGER.error("[GuiLayerBlur] disabled after error", throwable);
    }

    private static ColoredQuadGuiElementRenderState markerState(RenderPipeline renderPipeline) {
        return new ColoredQuadGuiElementRenderState(renderPipeline, TextureSetup.empty(), (Matrix3x2fc)new Matrix3x2f(), 0, 0, 1, 1, 0, 0, null);
    }

    public static void markRemoteBegin(DrawContext drawContext) {
        if (drawContext == null) {
            return;
        }
        GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
        guiRenderState.createNewRootLayer();
        guiRenderState.addSimpleElement((SimpleGuiElementRenderState)REMOTE_BEGIN);
        guiRenderState.createNewRootLayer();
    }

    public static void markRemoteCardBegin(DrawContext drawContext) {
        GuiLayerBlurRenderer.submitMarker(drawContext, REMOTE_CARD_BEGIN);
    }

    public static void markRemoteCardEnd(DrawContext drawContext) {
        GuiLayerBlurRenderer.submitMarker(drawContext, REMOTE_CARD_END);
    }

    public static void markPanelEndRemote(DrawContext drawContext) {
        if (drawContext == null) {
            return;
        }
        GuiRenderState guiRenderState = ((GuiGraphicsExtractorAccessor)drawContext).nv_getGuiRenderState();
        guiRenderState.createNewRootLayer();
        guiRenderState.addSimpleElement((SimpleGuiElementRenderState)REMOTE_END);
        guiRenderState.createNewRootLayer();
    }
}

