package rtx.nv.mixin;
import net.minecraft.client.render.LightmapTextureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import rtx.nv.api.modules.impl.Visuals.Ambience;

@Mixin(net.minecraft.client.render.LightmapTextureManager.class)

public abstract class LightTextureAmbienceMixin {
    @Redirect(method="update", at=@At(value="INVOKE", target="Ljava/lang/Double;floatValue()F", ordinal=1), require = 0)
    private float nv_ambienceBrightness(Double value) {
        float baseValue = value.floatValue();
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isEnabled()) {
            float brightness = ambience.getBrightnessValue();
            if (brightness >= 0.0f) {
                return Math.max(baseValue, brightness * 10.0f);
            }
            return Math.max(baseValue * (1.0f + brightness), 0.08f);
        }
        return baseValue;
    }
}

