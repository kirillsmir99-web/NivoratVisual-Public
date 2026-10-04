package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.option.GameOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.modules.impl.Utils.Optimization;

@Mixin(net.minecraft.client.option.GameOptions.class)

public abstract class OptimizationOptionsMixin {
    @ModifyReturnValue(method="getClampedViewDistance", at={@At(value="RETURN")}, require = 0)
    private int nv_capRenderDistance(int original) {
        return Optimization.capEffectiveRenderDistance(original);
    }
}

