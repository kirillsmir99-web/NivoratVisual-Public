package rtx.nv.mixin;

import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.ItemPhysics;

@Mixin(ItemEntityRenderer.class)
public abstract class ItemEntityRendererShadowMixin {
    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("TAIL"))
    private void nv_scaleItemShadow(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        ItemPhysics physics = ItemPhysics.getInstance();
        if (physics == null || !physics.isEnabled() || !physics.isNormalMode()) {
            return;
        }
        float scale = physics.groundItemScale();
        if (Math.abs(scale - 1.0f) > 0.001f) {
            state.shadowRadius *= scale;
        }
    }
}
