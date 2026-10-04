package rtx.nv.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.WorldBorderRendering;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.utils.render.post.customsky.CustomSkyRenderer;

@Mixin(WorldBorderRendering.class)
public abstract class WorldBorderCustomSkyMixin {
    @Unique
    private static RenderPipeline nv_noDepthPipeline;
    @Unique
    private static boolean nv_pipelineFailed;

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void nv_applySkyBeforeBorder(CallbackInfo ci) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isCustomSkyActive()) {
            CustomSkyRenderer.applyPending(MinecraftClient.getInstance().getFramebuffer());
        }
    }

    @Redirect(method="render", at=@At(value="FIELD", target="Lnet/minecraft/client/gl/RenderPipelines;RENDERTYPE_WORLD_BORDER:Lcom/mojang/blaze3d/pipeline/RenderPipeline;", opcode=178), require = 0)
    private RenderPipeline nv_borderPipeline() {
        Ambience ambience = Ambience.getInstance();
        if (ambience == null || !ambience.isCustomSkyActive()) {
            return RenderPipelines.RENDERTYPE_WORLD_BORDER;
        }
        RenderPipeline pipeline = WorldBorderCustomSkyMixin.nv_noDepthWrite();
        return pipeline != null ? pipeline : RenderPipelines.RENDERTYPE_WORLD_BORDER;
    }

    @Unique
    private static RenderPipeline nv_noDepthWrite() {
        if (nv_pipelineFailed) {
            return null;
        }
        if (nv_noDepthPipeline != null) {
            return nv_noDepthPipeline;
        }
        try {
            RenderPipeline vanilla = RenderPipelines.RENDERTYPE_WORLD_BORDER;
            RenderPipeline.Builder builder = RenderPipeline.builder(new RenderPipeline.Snippet[0])
                    .withLocation(Identifier.of("nv", "pipeline/world_border_no_depth"))
                    .withVertexShader(Identifier.of("nv", "core/world_border_fade"))
                    .withFragmentShader(Identifier.of("nv", "core/world_border_fade"))
                    .withVertexFormat(vanilla.getVertexFormat(), vanilla.getVertexFormatMode())
                    .withCull(vanilla.isCull())
                    .withDepthTestFunction(vanilla.getDepthTestFunction())
                    .withDepthBias(vanilla.getDepthBiasScaleFactor(), vanilla.getDepthBiasConstant())
                    .withDepthWrite(false);
            vanilla.getBlendFunction().ifPresent(builder::withBlend);
            for (String sampler : vanilla.getSamplers()) {
                builder.withSampler(sampler);
            }
            for (RenderPipeline.UniformDescription uniform : vanilla.getUniforms()) {
                if (uniform.textureFormat() != null) {
                    builder.withUniform(uniform.name(), uniform.type(), uniform.textureFormat());
                    continue;
                }
                builder.withUniform(uniform.name(), uniform.type());
            }
            nv_noDepthPipeline = RenderPipelines.register(builder.build());
        }
        catch (Throwable throwable) {
            nv_pipelineFailed = true;
        }
        return nv_noDepthPipeline;
    }
}
