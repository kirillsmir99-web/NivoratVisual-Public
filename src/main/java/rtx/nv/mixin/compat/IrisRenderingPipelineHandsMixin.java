package rtx.nv.mixin.compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.render.post.handsflame.IrisShaderCompat;

@Mixin(targets = {"net.irisshaders.iris.pipeline.IrisRenderingPipeline"}, remap = false)

public abstract class IrisRenderingPipelineHandsMixin {
    @Inject(method={"finalizeLevelRendering"}, at={@At(value="TAIL")}, remap=false, require = 0)
    private void nv_renderHandsFlameAfterIrisFinalPass(CallbackInfo ci) {
        IrisShaderCompat.renderHandsFlameAfterIrisFinalPass();
    }
}

