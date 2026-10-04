package rtx.nv.mixin.accessor;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftAccessor {
    @Invoker("setWorld")
    public void nv_updateLevelInEngines(ClientWorld var1);

    @Invoker("doAttack")
    public boolean nv_startAttack();

    @Invoker("doItemUse")
    public void nv_startUseItem();

    @Accessor("itemUseCooldown")
    public int nv_getRightClickDelay();

    @Accessor("itemUseCooldown")
    public void nv_setRightClickDelay(int var1);
}
