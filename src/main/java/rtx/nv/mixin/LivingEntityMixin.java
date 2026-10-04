package rtx.nv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.player.JumpEvent;
import rtx.nv.api.modules.impl.Visuals.KillEffect;
import rtx.nv.api.modules.impl.Visuals.SwingAnimation;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method="jump", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_jumpEvent(CallbackInfo ci) {
        if (!this.nv_isLocalPlayer()) {
            return;
        }
        JumpEvent event = EventBus.get().post(new JumpEvent((PlayerEntity)MinecraftClient.getInstance().player));
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method="getHandSwingDuration", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_swingDuration(CallbackInfoReturnable<Integer> cir) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || (Object)this != player) {
            return;
        }
        Integer duration = SwingAnimation.currentSwingDuration();
        if (duration != null) {
            cir.setReturnValue(duration);
        }
    }

    @Inject(method="onDeath", at={@At(value="HEAD")}, require = 0)
    private void nv_onDeath(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity)(Object)this;
        KillEffect.notifyEntityDied(self, source);
    }

    @Inject(method="setHealth", at={@At(value="HEAD")}, require = 0)
    private void nv_onSetHealth(float health, CallbackInfo ci) {
        if (health > 0.0f) {
            return;
        }
        LivingEntity self = (LivingEntity)(Object)this;
        if (self.getHealth() > 0.0f) {
            KillEffect.notifyEntityDied(self, null);
        }
    }

    @Unique
    private boolean nv_isLocalPlayer() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        return player != null && (Object)this == player;
    }
}
