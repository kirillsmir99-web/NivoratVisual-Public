package rtx.nv.mixin;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.api.modules.impl.Visuals.KillEffect;

@Mixin(net.minecraft.client.render.SkyRendering.class)

public abstract class SkyRendererAmbienceMixin {
    @Inject(method="updateRenderState", at={@At(value="RETURN")}, require = 0)
    private void nv_ambienceSkyColor(ClientWorld level, float tickDelta, Camera camera, SkyRenderState skyRenderState, CallbackInfo ci) {
        float saturation;
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isEnabled() && Float.isFinite(saturation = KillEffect.getWorldSaturationMultiplier() * ambience.getSaturationFactor()) && Math.abs(saturation - 1.0f) > 5.0E-4f) {
            skyRenderState.skyColor = SkyRendererAmbienceMixin.applySaturation(skyRenderState.skyColor, saturation);
            skyRenderState.sunriseAndSunsetColor = SkyRendererAmbienceMixin.applySaturation(skyRenderState.sunriseAndSunsetColor, saturation);
        }
    }

    private static int applySaturation(int color, float saturation) {
        float r = (float)(color >> 16 & 0xFF) / 255.0f;
        float g = (float)(color >> 8 & 0xFF) / 255.0f;
        float b = (float)(color & 0xFF) / 255.0f;
        float lum = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        r = MathHelper.clamp((float)(lum + (r - lum) * saturation), (float)0.0f, (float)1.0f);
        g = MathHelper.clamp((float)(lum + (g - lum) * saturation), (float)0.0f, (float)1.0f);
        b = MathHelper.clamp((float)(lum + (b - lum) * saturation), (float)0.0f, (float)1.0f);
        int alpha = color >> 24 & 0xFF;
        return alpha << 24 | (int)(r * 255.0f) << 16 | (int)(g * 255.0f) << 8 | (int)(b * 255.0f);
    }
}

