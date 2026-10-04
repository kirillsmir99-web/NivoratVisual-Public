package rtx.nv.mixin;
import net.minecraft.client.render.CloudRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.Ambience;

@Mixin(net.minecraft.client.render.CloudRenderer.class)

public abstract class CloudRendererMixin {
    @Inject(method="renderClouds", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_cutCloudsForCustomSky(CallbackInfo ci) {
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isCustomSkyActive()) {
            ci.cancel();
        }
    }
}

