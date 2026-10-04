package rtx.nv.mixin;

import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.Ambience;

@Mixin(ClientWorld.class)
public abstract class ClientLevelAmbienceMixin {
    @Shadow
    @Final
    private ClientWorld.Properties clientWorldProperties;

    @Inject(method = "tickTime", at = {@At("HEAD")}, cancellable = true, require = 0)
    private void nv_tickAmbienceTime(CallbackInfo ci) {
        Ambience ambience = Ambience.getInstance();
        if (ambience == null || !ambience.isEnabled()) {
            return;
        }
        ClientWorld level = (ClientWorld)(Object)this;
        this.clientWorldProperties.setTimeOfDay(ambience.getInternalTime());
        ambience.syncWeather(level, this.clientWorldProperties);
        ci.cancel();
    }
}
