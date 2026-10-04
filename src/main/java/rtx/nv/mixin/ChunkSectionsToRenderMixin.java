package rtx.nv.mixin;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderPass;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.render.BlockRenderLayerGroup;
import net.minecraft.client.render.SectionRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.FogBlur;
import rtx.nv.utils.render.post.fogblur.FogBlurRenderer;
import rtx.nv.utils.render.wave.WindWaveRenderer;

@Mixin(net.minecraft.client.render.SectionRenderState.class)

public class ChunkSectionsToRenderMixin {
    @Inject(method="renderSection", at={@At(value="HEAD")}, require = 0)
    private void nv_captureOpaqueDepth(BlockRenderLayerGroup group, GpuSampler sampler, CallbackInfo ci) {
        try {
            if (group != BlockRenderLayerGroup.TRANSLUCENT) {
                return;
            }
            Framebuffer main = MinecraftClient.getInstance().getFramebuffer();
            FogBlur fogBlur = FogBlur.getInstance();
            if (fogBlur != null && fogBlur.isEnabled() && !FogBlurRenderer.isDisabledAfterError()) {
                FogBlurRenderer.captureOpaqueDepth(main);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    @Redirect(method="renderSection", at=@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderPass;setPipeline(Lcom/mojang/blaze3d/pipeline/RenderPipeline;)V"), require = 0)
    private void nv_swapWavePipeline(RenderPass pass, RenderPipeline pipeline) {
        try {
            RenderPipeline target = WindWaveRenderer.substitute(pipeline);
            if (target != pipeline) {
                pass.setPipeline(target);
                WindWaveRenderer.bindParams(pass);
                return;
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        pass.setPipeline(pipeline);
    }
}

