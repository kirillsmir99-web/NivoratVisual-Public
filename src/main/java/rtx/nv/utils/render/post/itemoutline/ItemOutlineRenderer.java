package rtx.nv.utils.render.post.itemoutline;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.nio.ByteBuffer;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import rtx.nv.api.modules.impl.Visuals.ViewModel;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.render.others.RenderSampler;

public final class ItemOutlineRenderer {
    private static final float MAX_DIST = 20.0f;
    private static final float THICKNESS = 1.8f;
    private static final int DT_CONFIG_SIZE = 16;
    private static final int ISO_CONFIG_SIZE = 32;
    private static final Identifier VERTEX = ItemOutlineRenderer.id("post/itemoutline/itemoutline");
    private static final Identifier DT_H_SHADER = ItemOutlineRenderer.id("post/itemoutline/dt_h");
    private static final Identifier DT_V_SHADER = ItemOutlineRenderer.id("post/itemoutline/dt_v");
    private static final Identifier ISO_SHADER = ItemOutlineRenderer.id("post/itemoutline/isoline");
    private static final Identifier DT_H_PIPELINE_ID = ItemOutlineRenderer.id("pipeline/post/itemoutline/dt_h");
    private static final Identifier DT_V_PIPELINE_ID = ItemOutlineRenderer.id("pipeline/post/itemoutline/dt_v");
    private static final Identifier ISO_PIPELINE_ID = ItemOutlineRenderer.id("pipeline/post/itemoutline/isoline");
    private static RenderPipeline dtHPipeline;
    private static RenderPipeline dtVPipeline;
    private static RenderPipeline isoPipeline;
    private static GpuBuffer dtConfigBuffer;
    private static GpuBuffer isoConfigBuffer;
    private static SimpleFramebuffer silhouette;
    private static SimpleFramebuffer swap;
    private static int targetWidth;
    private static int targetHeight;
    private static boolean disabledAfterError;

    private ItemOutlineRenderer() {
    }

    static {
        targetWidth = -1;
        targetHeight = -1;
    }

    public static void run() {
        if (disabledAfterError) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null) {
            return;
        }
        OutlineVertexConsumerProvider outlineVertexConsumerProvider = minecraftClient.getBufferBuilders() != null ? minecraftClient.getBufferBuilders().getOutlineVertexConsumers() : null;
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (outlineVertexConsumerProvider == null || framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        float f = Math.max(ViewModel.outlineAlpha(Hand.MAIN_HAND), ViewModel.outlineAlpha(Hand.OFF_HAND));
        if (f <= 0.001f) {
            return;
        }
        ItemOutlineRenderer.init();
        if (dtHPipeline == null || dtVPipeline == null || isoPipeline == null) {
            return;
        }
        if (!ItemOutlineRenderer.ensureTargets(framebuffer.textureWidth, framebuffer.textureHeight)) {
            return;
        }
        try {
            ItemOutlineRenderer.drainSilhouette(outlineVertexConsumerProvider);
            GpuSampler gpuSampler = RenderSampler.linear();
            ItemOutlineRenderer.distanceTransformH(silhouette.getColorAttachmentView(), swap, gpuSampler);
            ItemOutlineRenderer.distanceTransformV(swap.getColorAttachmentView(), silhouette, gpuSampler);
            ItemOutlineRenderer.isolines(framebuffer.getColorAttachmentView(), silhouette.getColorAttachmentView(), f, gpuSampler);
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            ItemOutlineRenderer.closeTargets();
            ItemOutlineRenderer.closeBuffers();
        }
    }

    public static void clear() {
        ItemOutlineRenderer.closeTargets();
        ItemOutlineRenderer.closeBuffers();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    private static void init() {
        if (disabledAfterError) {
            return;
        }
        try {
            if (dtHPipeline == null) {
                dtHPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(DT_H_PIPELINE_ID).withVertexShader(VERTEX).withFragmentShader(DT_H_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("DtConfig", UniformType.UNIFORM_BUFFER).withSampler("MaskTex").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (dtVPipeline == null) {
                dtVPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(DT_V_PIPELINE_ID).withVertexShader(VERTEX).withFragmentShader(DT_V_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("DtConfig", UniformType.UNIFORM_BUFFER).withSampler("DistH").withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            if (isoPipeline == null) {
                isoPipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(ISO_PIPELINE_ID).withVertexShader(VERTEX).withFragmentShader(ISO_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withUniform("IsoConfig", UniformType.UNIFORM_BUFFER).withSampler("DistTex").withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            }
            dtConfigBuffer = ItemOutlineRenderer.ensureBuffer(dtConfigBuffer, 16, "nv:vm_outline_dt_config");
            isoConfigBuffer = ItemOutlineRenderer.ensureBuffer(isoConfigBuffer, 32, "nv:vm_outline_iso_config");
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            isoPipeline = null;
            dtVPipeline = null;
            dtHPipeline = null;
            ItemOutlineRenderer.closeBuffers();
        }
    }

    private static GpuBuffer closeBuffer(GpuBuffer gpuBuffer) {
        if (gpuBuffer != null) {
            gpuBuffer.close();
        }
        return null;
    }

    public static int outlineColor() {
        return ClientAccent.accentOpaque();
    }

    private static void closeBuffers() {
        dtConfigBuffer = ItemOutlineRenderer.closeBuffer(dtConfigBuffer);
        isoConfigBuffer = ItemOutlineRenderer.closeBuffer(isoConfigBuffer);
    }

    private static GpuBuffer ensureBuffer(GpuBuffer gpuBuffer, int n, String string) {
        if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= (long)n) {
            return gpuBuffer;
        }
        if (gpuBuffer != null) {
            gpuBuffer.close();
        }
        return RenderSystem.getDevice().createBuffer(() -> string, 136, (long)n);
    }

    private static void closeTargets() {
        if (silhouette != null) {
            silhouette.delete();
            silhouette = null;
        }
        if (swap != null) {
            swap.delete();
            swap = null;
        }
        targetWidth = -1;
        targetHeight = -1;
    }

    private static boolean ensureTargets(int n, int n2) {
        GpuDevice gpuDevice = RenderSystem.tryGetDevice();
        if (gpuDevice == null || n <= 0 || n2 <= 0) {
            return false;
        }
        if (silhouette != null && swap != null && targetWidth == n && targetHeight == n2) {
            return true;
        }
        ItemOutlineRenderer.closeTargets();
        silhouette = new SimpleFramebuffer("nv_vm_outline_silhouette", n, n2, true);
        swap = new SimpleFramebuffer("nv_vm_outline_swap", n, n2, false);
        targetWidth = n;
        targetHeight = n2;
        return true;
    }

    private static void isolines(GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2, float f, GpuSampler gpuSampler) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (org.lwjgl.system.MemoryStack memoryStack = org.lwjgl.system.MemoryStack.stackPush()) {
            float f2 = (float)(System.currentTimeMillis() % 1000L) / 1000.0f;
            int clientColor = ClientAccent.accentOpaque();
            float cr = ((clientColor >> 16) & 0xFF) / 255.0f;
            float cg = ((clientColor >> 8) & 0xFF) / 255.0f;
            float cb = (clientColor & 0xFF) / 255.0f;
            ByteBuffer byteBuffer = memoryStack.calloc(32);
            byteBuffer.putFloat(0, cr);
            byteBuffer.putFloat(4, cg);
            byteBuffer.putFloat(8, cb);
            byteBuffer.putFloat(12, 1.0f);
            byteBuffer.putFloat(16, MAX_DIST);
            byteBuffer.putFloat(20, THICKNESS);
            byteBuffer.putFloat(24, f);
            byteBuffer.putFloat(28, f2);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(isoConfigBuffer.slice(0L, 32L), byteBuffer);
        }
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:vm_outline_iso", gpuTextureView, OptionalInt.empty())) {
            renderPass.setPipeline(isoPipeline);
            renderPass.bindTexture("DistTex", gpuTextureView2, gpuSampler);
            renderPass.setUniform("IsoConfig", isoConfigBuffer);
            renderPass.draw(0, 6);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static void drainSilhouette(OutlineVertexConsumerProvider outlineVertexConsumerProvider) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:vm_outline_clear", silhouette.getColorAttachmentView(), OptionalInt.of(0));
        if (renderPass != null) {
            renderPass.close();
        }
        GpuTextureView oldColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView gpuTextureView = RenderSystem.outputDepthTextureOverride;
        RenderSystem.outputColorTextureOverride = silhouette.getColorAttachmentView();
        RenderSystem.outputDepthTextureOverride = silhouette.getDepthAttachmentView();
        try {
            outlineVertexConsumerProvider.draw();
        }
        finally {
            RenderSystem.outputColorTextureOverride = oldColor;
            RenderSystem.outputDepthTextureOverride = gpuTextureView;
        }
    }

    private static void distanceTransformH(GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, GpuSampler gpuSampler) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        ItemOutlineRenderer.writeDtConfig(commandEncoder, simpleFramebuffer);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:vm_outline_dt_h", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0));){
            renderPass.setPipeline(dtHPipeline);
            renderPass.bindTexture("MaskTex", gpuTextureView, gpuSampler);
            renderPass.setUniform("DtConfig", dtConfigBuffer);
            renderPass.draw(0, 6);
        }
    }

    private static void writeDtConfig(CommandEncoder commandEncoder, SimpleFramebuffer simpleFramebuffer) {
        try (MemoryStack memoryStack = MemoryStack.stackPush();){
            ByteBuffer byteBuffer = memoryStack.calloc(16);
            byteBuffer.putFloat(0, 1.0f / (float)simpleFramebuffer.textureWidth);
            byteBuffer.putFloat(4, 1.0f / (float)simpleFramebuffer.textureHeight);
            byteBuffer.putFloat(8, 20.0f);
            byteBuffer.putFloat(12, 20.0f);
            byteBuffer.position(0);
            commandEncoder.writeToBuffer(dtConfigBuffer.slice(0L, 16L), byteBuffer);
        }
    }

    private static void distanceTransformV(GpuTextureView gpuTextureView, SimpleFramebuffer simpleFramebuffer, GpuSampler gpuSampler) {
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        ItemOutlineRenderer.writeDtConfig(commandEncoder, simpleFramebuffer);
        try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:vm_outline_dt_v", simpleFramebuffer.getColorAttachmentView(), OptionalInt.of(0));){
            renderPass.setPipeline(dtVPipeline);
            renderPass.bindTexture("DistH", gpuTextureView, gpuSampler);
            renderPass.setUniform("DtConfig", dtConfigBuffer);
            renderPass.draw(0, 6);
        }
    }
}

