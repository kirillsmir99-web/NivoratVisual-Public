package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.modules.impl.Utils.Optimization;

@Mixin(net.minecraft.client.MinecraftClient.class)

public abstract class OptimizationMinecraftMixin {
    @ModifyReturnValue(method="usesImprovedTransparency", at={@At(value="RETURN")}, require = 0)
    private static boolean nv_forceFastTransparency(boolean original) {
        return Optimization.shaderTransparency(original);
    }
}

