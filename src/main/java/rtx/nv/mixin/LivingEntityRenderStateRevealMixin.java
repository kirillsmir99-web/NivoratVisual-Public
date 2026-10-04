package rtx.nv.mixin;

import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import rtx.nv.api.modules.impl.Visuals.seeinvisible.RevealTintHolder;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateRevealMixin implements RevealTintHolder {
    @Unique
    private int nv_revealTint = -1;

    @Override
    public int nv$getRevealTint() {
        return this.nv_revealTint;
    }

    @Override
    public void nv$setRevealTint(int tint) {
        this.nv_revealTint = tint;
    }
}
