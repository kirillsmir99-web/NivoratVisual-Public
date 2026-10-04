package rtx.nv.utils.render.underhand;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.ProjectionType;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.ProjectionMatrix2;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.BufferAllocator;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;
import rtx.nv.NV;
import rtx.nv.api.drags.Position;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.render.UnderHandRenderEvent;
import rtx.nv.utils.render.others.RenderSampler;
import rtx.nv.utils.render.render2d.Render2D;
import rtx.nv.utils.render.render2d.msdf.MsdfFont;
import rtx.nv.utils.render.render2d.msdf.MsdfFonts;
import rtx.nv.utils.render.render2d.msdf.MsdfQuadLayout;
import rtx.nv.utils.render.render2d.msdf.MsdfTextRenderer;

public final class UnderHand2D {
    private static final VertexFormat RECT_FORMAT = VertexFormat.builder().add("Position", VertexFormatElement.POSITION).add("UV0", VertexFormatElement.UV0).add("UV1", VertexFormatElement.UV1).add("Color", VertexFormatElement.COLOR).build();
    private static final RenderPipeline RECT_PIPELINE = RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(UnderHand2D.id("pipeline/underhand_rect")).withVertexShader(UnderHand2D.id("core/underhand_rect")).withFragmentShader(UnderHand2D.id("core/underhand_rect")).withVertexFormat(RECT_FORMAT, VertexFormat.DrawMode.QUADS).withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withCull(false).withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER).withUniform("Projection", UniformType.UNIFORM_BUFFER).build();
    private static final UnderHand2D INSTANCE = new UnderHand2D();
    private final ProjectionMatrix2 projection = new ProjectionMatrix2("nv_underhand", 1000.0f, 11000.0f, true);
    private final BufferAllocator rectBytes = new BufferAllocator(65536);
    private final Map<String, BufferAllocator> textArenas = new HashMap<String, BufferAllocator>();
    private final Map<String, BufferBuilder> textBuilders = new LinkedHashMap<String, BufferBuilder>();
    private final Map<String, GpuBuffer> vertexBuffers = new HashMap<String, GpuBuffer>();
    private BufferBuilder rectBuilder;
    private boolean active;
    private Framebuffer frameTarget;
    private final BufferAllocator nativeBytes = new BufferAllocator(65536);

    private UnderHand2D() {
    }

    private void flush() {
        this.drawPending();
        this.active = false;
        this.frameTarget = null;
    }

    private void begin(Framebuffer framebuffer) {
        this.active = true;
        this.frameTarget = framebuffer;
        this.rectBuilder = null;
        this.textBuilders.clear();
    }

    private static Identifier id(String string) {
        return Identifier.of((String)"nv", (String)string);
    }

    public void msdfText(String string, String string2, float f, float f2, float f3, int n) {
        this.msdfText(string, string2, f, f2, f3, n, n, n, n);
    }

    public void msdfText(String string, String string3, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        int n5 = n | n2 | n3 | n4;
        if (!this.active || string3 == null || string3.isEmpty() || n5 >>> 24 == 0) {
            return;
        }
        MsdfFont msdfFont = MsdfFonts.get(string);
        if (msdfFont == null) {
            return;
        }
        BufferBuilder bufferBuilder = this.textBuilders.computeIfAbsent(string, string2 -> new BufferBuilder(this.textArenas.computeIfAbsent(string2, k -> new BufferAllocator(65536)), VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR));
        MsdfQuadLayout.layout((MsdfFont)msdfFont, (String)string3, (float)f, (float)f2, (float)f3, (int)n, (int)n2, (int)n3, (int)n4, (VertexConsumer)bufferBuilder);
    }

    public float msdfWidth(String string, String string2, float f) {
        return Render2D.msdfWidth(string, string2, f);
    }

    public void rect(float f, float f2, float f3, float f4, float f5, int n) {
        if (!this.active || f3 <= 0.0f || f4 <= 0.0f || n >>> 24 == 0) {
            return;
        }
        if (this.rectBuilder == null) {
            this.rectBuilder = new BufferBuilder(this.rectBytes, VertexFormat.DrawMode.QUADS, RECT_FORMAT);
        }
        float f6 = Math.min(f5, Math.min(f3, f4) * 0.5f);
        int n2 = Math.min(Short.MAX_VALUE, Math.round(f3 * 8.0f));
        int n3 = Math.min(Short.MAX_VALUE, Math.round(f4 * 8.0f));
        this.rectBuilder.vertex(f, f2, f6).texture(0.0f, 0.0f).overlay(n2, n3).color(n);
        this.rectBuilder.vertex(f, f2 + f4, f6).texture(0.0f, f4).overlay(n2, n3).color(n);
        this.rectBuilder.vertex(f + f3, f2 + f4, f6).texture(f3, f4).overlay(n2, n3).color(n);
        this.rectBuilder.vertex(f + f3, f2, f6).texture(f3, 0.0f).overlay(n2, n3).color(n);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public void nativeText(Text text, float f, float f2, float f3, int n) {
        if (!this.active || text == null || n >>> 24 == 0) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        float f4 = Position.screenWidth();
        float f5 = Position.screenHeight();
        if (f4 < 1.0f || f5 < 1.0f) {
            return;
        }
        this.drawPending();
        Matrix4fStack matrix4fStack = RenderSystem.getModelViewStack();
        matrix4fStack.pushMatrix();
        matrix4fStack.identity();
        try {
            RenderSystem.backupProjectionMatrix();
            RenderSystem.setProjectionMatrix((GpuBufferSlice)(Object)this.projection.set(f4, f5), (ProjectionType)ProjectionType.ORTHOGRAPHIC);
            Matrix4f matrix4f = new Matrix4f().translation(f, f2, -2000.0f).scale(f3, f3, 1.0f);
            VertexConsumerProvider.Immediate immediate = VertexConsumerProvider.immediate((BufferAllocator)(Object)this.nativeBytes);
            minecraftClient.textRenderer.draw(text, 0.0f, 0.0f, n, false, matrix4f, (VertexConsumerProvider)immediate, TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
            immediate.draw();
        }
        catch (Throwable throwable) {
            NV.LOGGER.error("[UnderHand2D] nativeText failed", throwable);
        }
        finally {
            RenderSystem.restoreProjectionMatrix();
            matrix4fStack.popMatrix();
        }
    }

    public void barrier() {
        if (!this.active) {
            return;
        }
        this.drawPending();
    }

    public static void renderNow() {
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        if (minecraftClient == null || minecraftClient.player == null || minecraftClient.world == null || minecraftClient.options.hudHidden) {
            return;
        }
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachmentView() == null) {
            return;
        }
        UnderHand2D underHand2D = INSTANCE;
        underHand2D.begin(framebuffer);
        try {
            EventBus.get().post(new UnderHandRenderEvent(underHand2D));
        }
        finally {
            underHand2D.flush();
        }
    }

    private void discardBuilders() {
        if (this.rectBuilder != null) {
            BuiltBuffer builtBuffer = this.rectBuilder.endNullable();
            if (builtBuffer != null) {
                builtBuffer.close();
            }
            this.rectBuilder = null;
        }
        for (BufferBuilder bufferBuilder : this.textBuilders.values()) {
            BuiltBuffer builtBuffer = bufferBuilder.endNullable();
            if (builtBuffer == null) continue;
            builtBuffer.close();
        }
        this.textBuilders.clear();
    }

    private GpuTextureView resolveFontTexture(String string) {
        MsdfFont msdfFont = MsdfFonts.get(string);
        if (msdfFont == null) {
            return null;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        AbstractTexture abstractTexture = minecraftClient.getTextureManager().getTexture(msdfFont.atlasTexture());
        return abstractTexture != null ? abstractTexture.getGlTextureView() : null;
    }

    private GpuBuffer ensureVertexBuffer(String string, int n) {
        GpuBuffer gpuBuffer = this.vertexBuffers.get(string);
        if (gpuBuffer != null && !gpuBuffer.isClosed() && gpuBuffer.size() >= (long)n) {
            return gpuBuffer;
        }
        if (gpuBuffer != null) {
            gpuBuffer.close();
        }
        int n2 = Math.max(n, 4096);
        GpuBuffer gpuBuffer2 = RenderSystem.getDevice().createBuffer(() -> "nv:underhand_vb_" + string, 40, (long)n2);
        this.vertexBuffers.put(string, gpuBuffer2);
        return gpuBuffer2;
    }

    private void drawPending() {
        Framebuffer framebuffer = this.frameTarget;
        if (framebuffer == null || this.rectBuilder == null && this.textBuilders.isEmpty()) {
            return;
        }
        float f = Position.screenWidth();
        float f2 = Position.screenHeight();
        if (f < 1.0f || f2 < 1.0f) {
            this.discardBuilders();
            return;
        }
        try {
            GpuBufferSlice gpuBufferSlice = this.projection.set(f, f2);
            GpuBufferSlice gpuBufferSlice2 = RenderSystem.getDynamicUniforms().write((Matrix4fc)new Matrix4f().translation(0.0f, 0.0f, -11000.0f), (Vector4fc)new Vector4f(1.0f, 1.0f, 1.0f, 1.0f), (Vector3fc)new Vector3f(), (Matrix4fc)new Matrix4f());
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            if (this.rectBuilder != null) {
                this.drawMesh(commandEncoder, framebuffer, "rect", this.rectBuilder.endNullable(), RECT_PIPELINE, null, gpuBufferSlice, gpuBufferSlice2);
                this.rectBuilder = null;
            }
            for (Map.Entry<String, BufferBuilder> entry : this.textBuilders.entrySet()) {
                BuiltBuffer builtBuffer = entry.getValue().endNullable();
                GpuTextureView gpuTextureView = this.resolveFontTexture(entry.getKey());
                if (gpuTextureView == null) {
                    if (builtBuffer == null) continue;
                    builtBuffer.close();
                    continue;
                }
                this.drawMesh(commandEncoder, framebuffer, "text_" + entry.getKey(), builtBuffer, MsdfTextRenderer.MSDF_PIPELINE, gpuTextureView, gpuBufferSlice, gpuBufferSlice2);
            }
            this.textBuilders.clear();
        }
        catch (Throwable throwable) {
            NV.LOGGER.error("[UnderHand2D] flush failed", throwable);
            this.discardBuilders();
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void drawMesh(CommandEncoder commandEncoder, Framebuffer framebuffer, String string, BuiltBuffer builtBuffer, RenderPipeline renderPipeline, GpuTextureView gpuTextureView, GpuBufferSlice gpuBufferSlice, GpuBufferSlice gpuBufferSlice2) {
        if (builtBuffer == null) {
            return;
        }
        try {
            int n = builtBuffer.getDrawParameters().indexCount();
            ByteBuffer byteBuffer = builtBuffer.getBuffer();
            GpuBuffer gpuBuffer = this.ensureVertexBuffer(string, byteBuffer.remaining());
            if (gpuBuffer == null) {
                return;
            }
            commandEncoder.writeToBuffer(gpuBuffer.slice(0L, (long)byteBuffer.remaining()), byteBuffer);
            RenderSystem.ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer((VertexFormat.DrawMode)VertexFormat.DrawMode.QUADS);
            GpuBuffer gpuBuffer2 = shapeIndexBuffer.getIndexBuffer(n);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:underhand_" + string, framebuffer.getColorAttachmentView(), OptionalInt.empty());){
                renderPass.setPipeline(renderPipeline);
                renderPass.setVertexBuffer(0, gpuBuffer);
                renderPass.setIndexBuffer(gpuBuffer2, shapeIndexBuffer.getIndexType());
                renderPass.setUniform("Projection", gpuBufferSlice);
                renderPass.setUniform("DynamicTransforms", gpuBufferSlice2);
                if (gpuTextureView != null) {
                    renderPass.bindTexture("Sampler0", gpuTextureView, RenderSampler.linear());
                }
                renderPass.drawIndexed(0, 0, n, 1);
            }
        }
        finally {
            builtBuffer.close();
        }
    }
}

