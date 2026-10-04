package rtx.nv.mixin;

import net.minecraft.client.render.debug.EntityHitboxDebugRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.Hitboxes;

@Mixin(EntityHitboxDebugRenderer.class)
public abstract class EntityHitboxDebugRendererMixin {
    @Inject(method = "drawHitbox", at = @At("HEAD"), cancellable = true)
    private void nv_drawCustomHitbox(Entity entity, float tickProgress, boolean inLocalServer, CallbackInfo ci) {
        Hitboxes hitboxes = Hitboxes.getInstance();
        if (hitboxes != null && hitboxes.isEnabled()) {
            if (hitboxes.drawStyledHitbox(entity, tickProgress, inLocalServer)) {
                ci.cancel();
            }
        }
    }
}
