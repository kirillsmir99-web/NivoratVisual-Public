package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import rtx.nv.api.modules.impl.Visuals.NoRender;

@Mixin(net.minecraft.client.network.AbstractClientPlayerEntity.class)

public abstract class AbstractClientPlayerFovMixin {
    @ModifyReturnValue(method="getFovMultiplier", at={@At(value="RETURN")}, require = 0)
    private float nv_noFovDynamic(float original) {
        return NoRender.isActive("\u0414\u0438\u043d\u0430\u043c\u0438\u043a\u0430 \u043f\u043e\u043b\u044f \u0437\u0440\u0435\u043d\u0438\u044f") ? 1.0f : original;
    }
}

