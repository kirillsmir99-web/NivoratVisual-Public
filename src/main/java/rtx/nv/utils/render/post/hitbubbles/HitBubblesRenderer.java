package rtx.nv.utils.render.post.hitbubbles;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.UniformType;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import java.util.OptionalInt;
import rtx.nv.utils.render.others.RenderSampler;

public final class HitBubblesRenderer {
    private static final int PARAM_COUNT = 152;
    private static RenderPipeline pipeline;
    private static GpuBuffer uniforms;
    private static GpuTexture scene;
    private static GpuTextureView sceneView;
    private static int width, height;
    private static boolean disabledAfterError;
    private static long renderedPasses;

    private HitBubblesRenderer() {}
    public static long renderedPasses() { return renderedPasses; }
    public static boolean isDisabledAfterError() { return disabledAfterError; }

    public static void apply(Framebuffer framebuffer, float[] params) {
        if (disabledAfterError || framebuffer == null || params == null || params.length != PARAM_COUNT ||
                framebuffer.getColorAttachment() == null || framebuffer.getColorAttachmentView() == null ||
                framebuffer.textureWidth < 1 || framebuffer.textureHeight < 1 || params[0] < 1 || params[0] > 16) return;
        for (float value : params) if (!Float.isFinite(value)) return;
        try {
            var device = RenderSystem.getDevice();
            if (pipeline == null) {
                Identifier shader = Identifier.of("nv", "post/hitbubbles/hitbubbles");
                pipeline = RenderPipelines.register(RenderPipeline.builder()
                    .withLocation(Identifier.of("nv", "pipeline/post/hitbubbles"))
                    .withVertexShader(shader).withFragmentShader(shader)
                    .withVertexFormat(VertexFormats.EMPTY, VertexFormat.DrawMode.TRIANGLES)
                    .withUniform("Ripples", UniformType.UNIFORM_BUFFER).withSampler("Scene")
                    .withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(false).withCull(false).build());
            }
            if (uniforms == null) uniforms = device.createBuffer(() -> "nv:hitbubbles_uniforms",136,PARAM_COUNT * 4L);
            if (scene == null || width != framebuffer.textureWidth || height != framebuffer.textureHeight) {
                closeScene(); width = framebuffer.textureWidth; height = framebuffer.textureHeight;
                scene = device.createTexture(() -> "nv:hitbubbles_scene",5,TextureFormat.RGBA8,width,height,1,1);
                sceneView = device.createTextureView(scene);
            }
            var encoder = device.createCommandEncoder();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                var data = stack.malloc(PARAM_COUNT * 4);
                for (float value : params) data.putFloat(value);
                data.flip(); encoder.writeToBuffer(uniforms.slice(0L,PARAM_COUNT * 4L),data);
            }
            encoder.copyTextureToTexture(framebuffer.getColorAttachment(),scene,0,0,0,0,0,width,height);
            try (RenderPass pass = encoder.createRenderPass(() -> "nv:hitbubbles",framebuffer.getColorAttachmentView(),OptionalInt.empty())) {
                pass.setPipeline(pipeline); pass.setUniform("Ripples",uniforms);
                pass.bindTexture("Scene",sceneView,RenderSampler.linear()); pass.draw(0,6);
            }
            ++renderedPasses;
        } catch (RuntimeException | LinkageError failure) {
            clear(); disabledAfterError = true;
            rtx.nv.NV.LOGGER.error("[HitBubbles] Rendering failed; retry after resource reload",failure);
        }
    }
    private static void closeScene() {
        if (sceneView != null) { sceneView.close(); sceneView = null; }
        if (scene != null) { scene.close(); scene = null; }
        width = height = 0;
    }
    public static void clear() {
        closeScene(); if (uniforms != null) { uniforms.close(); uniforms = null; }
    }
    public static void reset() { clear(); disabledAfterError = false; }
}
