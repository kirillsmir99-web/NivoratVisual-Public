package rtx.nv.utils.render.post.hpfocus;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import rtx.nv.NV;
import rtx.nv.utils.render.others.RenderSampler;

public final class HPFocusRenderer {
    public static final String TEXTURE_KEY = "nv:hpfocus_scene";
    private static final Identifier TEXTURE_ID = Identifier.of((String)"nv", (String)"hpfocus_scene");
    private static final Identifier PIPELINE_ID = Identifier.of((String)"nv", (String)"pipeline/post/hpfocus/copy");
    private static final Identifier COPY_SHADER = Identifier.of((String)"nv", (String)"post/hpfocus/copy");
    private static RenderPipeline pipeline;
    private static GpuTexture sceneCopyTexture;
    private static GpuTextureView sceneCopyView;
    private static SimpleFramebuffer outTarget;
    private static int texWidth;
    private static int texHeight;
    private static boolean captureRequested;
    private static boolean hasCapture;
    private static boolean disabledAfterError;

    private HPFocusRenderer() {
    }

    private static boolean ensureInitialized() {
        if (pipeline != null) {
            return true;
        }
        try {
            pipeline = RenderPipelines.register((RenderPipeline)RenderPipeline.builder((RenderPipeline.Snippet[])new RenderPipeline.Snippet[0]).withLocation(PIPELINE_ID).withVertexShader(COPY_SHADER).withFragmentShader(COPY_SHADER).withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES).withSampler("Sampler0").withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false).build());
            return true;
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            pipeline = null;
            NV.LOGGER.error("[HPFocus] failed to initialise copy pipeline", throwable);
            return false;
        }
    }

    public static void requestCapture() {
        if (!disabledAfterError) {
            captureRequested = true;
        }
    }

    public static boolean hasCapture() {
        return hasCapture && !disabledAfterError;
    }

    public static void captureIfRequested() {
        if (!captureRequested) {
            return;
        }
        captureRequested = false;
        if (disabledAfterError) {
            return;
        }
        MinecraftClient minecraftClient = MinecraftClient.getInstance();
        Framebuffer framebuffer = minecraftClient.getFramebuffer();
        if (framebuffer == null || framebuffer.getColorAttachment() == null) {
            return;
        }
        int n = framebuffer.textureWidth;
        int n2 = framebuffer.textureHeight;
        if (n <= 0 || n2 <= 0 || !HPFocusRenderer.ensureInitialized()) {
            return;
        }
        try {
            HPFocusRenderer.ensureResources(n, n2);
            CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
            commandEncoder.copyTextureToTexture(framebuffer.getColorAttachment(), sceneCopyTexture, 0, 0, 0, 0, 0, n, n2);
            try (RenderPass renderPass = commandEncoder.createRenderPass(() -> "nv:hpfocus_copy", outTarget.getColorAttachmentView(), OptionalInt.empty());){
                renderPass.setPipeline(pipeline);
                renderPass.bindTexture("Sampler0", sceneCopyView, RenderSampler.linear());
                renderPass.draw(0, 6);
            }
            hasCapture = true;
        }
        catch (Throwable throwable) {
            disabledAfterError = true;
            hasCapture = false;
            HPFocusRenderer.closeResources();
            NV.LOGGER.error("[HPFocus] scene capture failed, disabling", throwable);
        }
    }

    private static void ensureResources(int n, int n2) {
        if (sceneCopyTexture != null && outTarget != null && n == texWidth && n2 == texHeight) {
            return;
        }
        HPFocusRenderer.closeResources();
        sceneCopyTexture = RenderSystem.getDevice().createTexture(() -> "nv:hpfocus_scene_copy", 5, TextureFormat.RGBA8, n, n2, 1, 1);
        sceneCopyView = RenderSystem.getDevice().createTextureView(sceneCopyTexture);
        outTarget = new SimpleFramebuffer(TEXTURE_KEY, n, n2, false);
        MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, (AbstractTexture)new HPFocusRenderer.SceneTexture(outTarget.getColorAttachment(), outTarget.getColorAttachmentView()));
        texWidth = n;
        texHeight = n2;
    }

    private static void closeResources() {
        if (sceneCopyView != null) {
            sceneCopyView.close();
            sceneCopyView = null;
        }
        if (sceneCopyTexture != null) {
            sceneCopyTexture.close();
            sceneCopyTexture = null;
        }
        if (outTarget != null) {
            outTarget.delete();
            outTarget = null;
        }
        texWidth = -1;
        texHeight = -1;
    }


    public static final class SceneTexture
    extends AbstractTexture {
        private final GpuTexture gpuTexture;
        private final GpuTextureView gpuTextureView;
    
        public SceneTexture(GpuTexture gpuTexture, GpuTextureView gpuTextureView) {
            this.gpuTexture = gpuTexture;
            this.gpuTextureView = gpuTextureView;
        }
    
        public GpuTextureView getGlTextureView() {
            return this.gpuTextureView;
        }
    
        public GpuTexture getGlTexture() {
            return this.gpuTexture;
        }
    }
}

