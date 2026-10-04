package rtx.nv.mixin;
import net.minecraft.client.gui.screen.DeathScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.game.DeathScreenEvent;

@Mixin(net.minecraft.client.gui.screen.DeathScreen.class)

public abstract class DeathScreenMixin {
    @Inject(method="init", at={@At(value="HEAD")}, require = 0)
    private void nv_onDeathScreenInit(CallbackInfo ci) {
        EventBus.get().post(new DeathScreenEvent());
    }
}

