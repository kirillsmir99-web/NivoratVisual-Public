package rtx.nv.utils.render.post;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.*;
import com.mojang.blaze3d.textures.*;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.*;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryStack;
import rtx.nv.utils.render.others.RenderSampler;
import java.util.*;

/** Reusable scene copy and bounded uniform storage for existing full-screen shaders. */
public final class FullscreenPostPass {
    private static final List<FullscreenPostPass> INSTANCES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final String name, block, sampler, fragment, vertex;
    private final int floats;
    private final boolean depth;
    private RenderPipeline pipeline;
    private GpuBuffer uniforms;
    private GpuTexture scene;
    private GpuTextureView sceneView;
    private int width, height;
    private boolean disabled;
    private long passes;
    public FullscreenPostPass(String name,String fragment,String vertex,String block,String sampler,int floats,boolean depth) {
        if (floats <= 0 || floats % 4 != 0) throw new IllegalArgumentException("std140 buffer alignment");
        this.name=name; this.fragment=fragment; this.vertex=vertex; this.block=block; this.sampler=sampler; this.floats=floats; this.depth=depth;
        INSTANCES.add(this);
    }
    public long passes() { return passes; }
    public boolean disabled() { return disabled; }
    public void apply(Framebuffer framebuffer,float[] values) {
        if (disabled || framebuffer==null || values==null || values.length!=floats ||
            framebuffer.getColorAttachment()==null || framebuffer.getColorAttachmentView()==null ||
            framebuffer.textureWidth<1 || framebuffer.textureHeight<1 || (depth && framebuffer.getDepthAttachmentView()==null)) return;
        for (float value: values) if (!Float.isFinite(value)) return;
        try {
            var device=RenderSystem.getDevice();
            if (pipeline==null) {
                var builder=RenderPipeline.builder().withLocation(Identifier.of("nv","pipeline/post/"+name))
                    .withVertexShader(Identifier.of("nv",vertex)).withFragmentShader(Identifier.of("nv",fragment))
                    .withVertexFormat(VertexFormats.EMPTY,VertexFormat.DrawMode.TRIANGLES)
                    .withUniform(block,UniformType.UNIFORM_BUFFER).withSampler(sampler)
                    .withoutBlend().withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST).withDepthWrite(false).withCull(false);
                if (depth) builder.withSampler("DepthSampler");
                pipeline=RenderPipelines.register(builder.build());
            }
            if (uniforms==null) uniforms=device.createBuffer(() -> "nv:"+name+"_uniforms",136,floats*4L);
            if (scene==null || width!=framebuffer.textureWidth || height!=framebuffer.textureHeight) {
                closeScene(); width=framebuffer.textureWidth; height=framebuffer.textureHeight;
                scene=device.createTexture(() -> "nv:"+name+"_scene",5,TextureFormat.RGBA8,width,height,1,1);
                sceneView=device.createTextureView(scene);
            }
            var encoder=device.createCommandEncoder();
            try (MemoryStack stack=MemoryStack.stackPush()) {
                var bytes=stack.malloc(floats*4); for (float value:values) bytes.putFloat(value);
                bytes.flip(); encoder.writeToBuffer(uniforms.slice(0,floats*4L),bytes);
            }
            encoder.copyTextureToTexture(framebuffer.getColorAttachment(),scene,0,0,0,0,0,width,height);
            try (RenderPass pass=encoder.createRenderPass(() -> "nv:"+name,framebuffer.getColorAttachmentView(),OptionalInt.empty())) {
                pass.setPipeline(pipeline); pass.setUniform(block,uniforms);
                pass.bindTexture(sampler,sceneView,RenderSampler.linear());
                if (depth) pass.bindTexture("DepthSampler",framebuffer.getDepthAttachmentView(),RenderSampler.nearest());
                pass.draw(0,6);
            }
            ++passes;
        } catch (RuntimeException | LinkageError failure) {
            clear(); disabled=true; rtx.nv.NV.LOGGER.error("[PostPass] "+name+" disabled until resource reload",failure);
        }
    }
    private void closeScene() {
        if (sceneView!=null) { sceneView.close(); sceneView=null; }
        if (scene!=null) { scene.close(); scene=null; }
        width=height=0;
    }
    public void clear() { closeScene(); if (uniforms!=null) { uniforms.close(); uniforms=null; } }
    public static void clearAll() { INSTANCES.forEach(FullscreenPostPass::clear); }
    public static void resetAll() { for (FullscreenPostPass pass:INSTANCES) { pass.clear(); pass.disabled=false; } }
}
