package rtx.nv.mixin;

import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.wings.WingsFeatureRenderer;
import rtx.nv.mixin.accessor.LivingEntityRendererAccessor;

@Mixin(PlayerEntityRenderer.class)
public abstract class WingsPlayerRendererMixin {
    @Inject(method = "<init>(Lnet/minecraft/client/render/entity/EntityRendererFactory$Context;Z)V", at = @At("RETURN"), require = 0)
    private void nv_addWingsFeature(EntityRendererFactory.Context context, boolean slim, CallbackInfo ci) {
        PlayerEntityRenderer self = (PlayerEntityRenderer) (Object) this;
        ((LivingEntityRendererAccessor) self).nv_callAddFeature(new WingsFeatureRenderer(self));
        ((LivingEntityRendererAccessor) self).nv_callAddFeature(new rtx.nv.api.modules.impl.Visuals.crown.CrownFeatureRenderer(self));
    }
}
