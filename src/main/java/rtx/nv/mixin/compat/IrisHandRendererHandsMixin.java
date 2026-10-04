package rtx.nv.mixin.compat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.utils.render.post.handsflame.IrisShaderCompat;

@Mixin(targets = {"net.irisshaders.iris.compat.sodium.impl.shader_hand.ShaderHandRenderer"}, remap = false)

public abstract class IrisHandRendererHandsMixin {
    @Inject(method={"renderSolid", "renderTranslucent"}, at={@At(value="INVOKE", target="Lcom/mojang/blaze3d/systems/RenderSystem;backupProjectionMatrix()V")}, remap=false, require = 0)
    private void nv_beginHandsFlameDepthCapture(CallbackInfo ci) {
        IrisShaderCompat.beginHandDepthCapture();
    }

    @Inject(method={"renderSolid", "renderTranslucent"}, at={@At(value="TAIL")}, remap=false, require = 0)
    private void nv_endHandsFlameDepthCapture(CallbackInfo ci) {
        IrisShaderCompat.endHandDepthCapture();
    }
}

