package rtx.nv.mixin;

import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.modules.impl.Visuals.NameTags;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererHologramMixin {
    @Inject(method = "shouldRender", at = {@At("HEAD")}, cancellable = true, require = 0)
    private void nv_hideHologram(Entity entity, Frustum frustum, double camX, double camY, double camZ, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof PlayerEntity) && NameTags.hidesNameTagFor(entity)) {
            cir.setReturnValue(false);
        }
    }
}
