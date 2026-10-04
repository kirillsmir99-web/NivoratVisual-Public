package rtx.nv.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.FakePlayer;

@Mixin(PlayerEntity.class)
public abstract class PlayerAttackSoundMixin {
    @Unique
    private boolean nv_attackingFakePlayer;
    @Unique
    private boolean nv_criticalFakePlayerAttack;
    @Unique
    private boolean nv_vanillaCriticalSound;

    @Inject(method="attack", at={@At(value="HEAD")}, require = 0)
    private void nv_beginFakePlayerAttack(Entity target, CallbackInfo ci) {
        this.nv_attackingFakePlayer = FakePlayer.isFakePlayer(target);
        PlayerEntity player = (PlayerEntity)(Object)this;
        boolean airborne = !player.isOnGround() || player.fallDistance > 0.0 || player.getVelocity().y > 0.05;
        this.nv_criticalFakePlayerAttack = this.nv_attackingFakePlayer && airborne && player.getAttackCooldownProgress(0.5f) > 0.9f && !player.isClimbing() && !player.isTouchingWater() && !player.hasBlindnessEffect() && !player.hasVehicle() && !player.isSprinting();
        this.nv_vanillaCriticalSound = false;
    }

    @Inject(method="attack", at={@At(value="RETURN")}, require = 0)
    private void nv_endFakePlayerAttack(Entity target, CallbackInfo ci) {
        if (this.nv_criticalFakePlayerAttack && !this.nv_vanillaCriticalSound) {
            ((PlayerEntity)(Object)this).addCritParticles(target);
        }
        this.nv_attackingFakePlayer = false;
        this.nv_criticalFakePlayerAttack = false;
    }
}
