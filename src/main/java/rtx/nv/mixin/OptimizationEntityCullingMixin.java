package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.modules.impl.Utils.Optimization;

@Mixin(net.minecraft.client.render.entity.EntityRenderManager.class)

public abstract class OptimizationEntityCullingMixin {
    @org.spongepowered.asm.mixin.injection.Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 0)
    private void nv_keepPlayerWithWingsVisible(Entity entity, Frustum frustum, double camX, double camY, double camZ, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof net.minecraft.client.network.AbstractClientPlayerEntity player) {
            rtx.nv.api.modules.impl.Visuals.Wings wings = rtx.nv.api.modules.impl.Visuals.Wings.getInstance();
            if (wings != null && wings.isEnabled() && wings.shouldRender(player)) {
                cir.setReturnValue(true);
            }
        }
    }

    @ModifyReturnValue(method="shouldRender", at={@At(value="RETURN")}, require = 0)
    private boolean nv_occlusionCull(boolean original, Entity entity, Frustum frustum, double camX, double camY, double camZ) {
        return Optimization.shouldRenderEntity(original, entity, camX, camY, camZ);
    }
}

