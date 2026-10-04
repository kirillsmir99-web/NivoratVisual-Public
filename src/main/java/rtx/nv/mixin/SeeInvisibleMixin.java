package rtx.nv.mixin;

import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.SeeInvisible;
import rtx.nv.api.modules.impl.Visuals.seeinvisible.RevealTintHolder;

@Mixin(LivingEntityRenderer.class)
public abstract class SeeInvisibleMixin<T extends LivingEntity, S extends LivingEntityRenderState> {
    @Inject(method="updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at={@At(value="TAIL")}, require = 0)
    private void nv$revealInvisible(T entity, S state, float partialTick, CallbackInfo ci) {
        if (state instanceof RevealTintHolder holder) {
            holder.nv$setRevealTint(-1);
        }
        if (!state.invisible) {
            return;
        }
        SeeInvisible module = SeeInvisible.getInstance();
        if (module == null || !module.shouldReveal(entity)) {
            return;
        }
        state.invisibleToPlayer = false;
        if (module.isSolid()) {
            state.invisible = false;
            return;
        }
        if (state instanceof RevealTintHolder holder) {
            holder.nv$setRevealTint(module.ghostTint());
        }
    }

    @ModifyConstant(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", constant={@Constant(intValue=0x26FFFFFF)}, require = 0)
    private int nv$revealTint(int original, S state, MatrixStack poseStack, OrderedRenderCommandQueue collector, CameraRenderState camera) {
        int tint;
        if (state instanceof RevealTintHolder holder && (tint = holder.nv$getRevealTint()) != -1) {
            return tint;
        }
        return original;
    }
}
