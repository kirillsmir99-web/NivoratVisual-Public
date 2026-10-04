package rtx.nv.mixin.accessor;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface MultiPlayerGameModeAccessor {
    @Invoker("syncSelectedSlot")
    public void nv_ensureHasSentCarriedItem();

    @Accessor("breakingBlock")
    public boolean nv_isDestroying();

    @Accessor("currentBreakingPos")
    public BlockPos nv_getDestroyBlockPos();

    @Accessor("currentBreakingPos")
    public void nv_setDestroyBlockPos(BlockPos pos);

    @Accessor("currentBreakingProgress")
    public float nv_getDestroyProgress();

    @Accessor("breakingBlock")
    public void nv_setDestroying(boolean var1);

    @Accessor("blockBreakingCooldown")
    public void nv_setDestroyDelay(int var1);

    @Accessor("currentBreakingProgress")
    public void nv_setDestroyProgress(float var1);
}
