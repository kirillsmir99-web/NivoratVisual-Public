package rtx.nv.mixin.accessor;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    @Accessor("handSwingTicks")
    public void nv_setSwingTime(int var1);

    @Accessor("handSwingProgress")
    public void nv_setAttackAnim(float var1);

    @Accessor("handSwinging")
    public void nv_setSwinging(boolean var1);

    @Accessor("jumpingCooldown")
    public void nv_setNoJumpDelay(int var1);

    @Accessor("preferredHand")
    public void nv_setSwingingArm(Hand var1);
}
