package rtx.nv.mixin;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.NoRender;

@Mixin(net.minecraft.client.render.entity.EntityRenderer.class)

public abstract class EntityRendererNoFireMixin {
    @Inject(method="updateRenderState", at={@At(value="TAIL")}, require = 0)
    private void nv_noEntityEffects(Entity entity, EntityRenderState state, float partialTick, CallbackInfo ci) {
        if (NoRender.isActive("\u041e\u0433\u043e\u043d\u044c \u043d\u0430 \u0441\u0443\u0449\u043d\u043e\u0441\u0442\u044f\u0445")) {
            state.onFire = false;
        }
        if (NoRender.isActive("\u0421\u0432\u0435\u0447\u0435\u043d\u0438\u0435")) {
            state.outlineColor = 0;
        }
    }
}

