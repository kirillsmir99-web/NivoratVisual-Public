package rtx.nv.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.api.modules.impl.Visuals.FogBlur;
import rtx.nv.api.modules.impl.Visuals.KillEffect;

@Mixin(FogRenderer.class)
public abstract class FogRendererAmbienceMixin {
    @ModifyArg(method="applyFog", at=@At(value="INVOKE", target="Lnet/minecraft/client/render/fog/FogRenderer;method_71110(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"), index=2, require = 0)
    private Vector4f nv_modifyAmbienceFogColor(Vector4f color) {
        return FogRendererAmbienceMixin.applyAmbienceToFog(color);
    }

    @ModifyArgs(method="applyFog", at=@At(value="INVOKE", target="Lnet/minecraft/client/render/fog/FogRenderer;method_71110(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V"), require = 0)
    private void nv_scaleFogDistances(Args args) {
        FogBlur fogBlur = FogBlur.getInstance();
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer != null ? mc.gameRenderer.getCamera() : null;
        if (fogBlur != null && fogBlur.shouldApplyFog(camera)) {
            float start = fogBlur.getEffectiveFogStart();
            float end = fogBlur.getEffectiveFogEnd();
            args.set(3, Float.valueOf(start));
            args.set(4, Float.valueOf(end));
            args.set(5, Float.valueOf(start));
            args.set(6, Float.valueOf(end));
        }
    }

    @Inject(method="applyFog", at={@At(value="RETURN")}, cancellable=true, require = 0)
    private void nv_setupAmbienceFog(Camera camera, int renderDistance, RenderTickCounter deltaTracker, float tickProgress, ClientWorld level, CallbackInfoReturnable<Vector4f> cir) {
        Vector4f modified = FogRendererAmbienceMixin.applyAmbienceToFog((Vector4f)cir.getReturnValue());
        if (modified != null && modified != cir.getReturnValue()) {
            cir.setReturnValue(modified);
        }
    }

    private static Vector4f applyAmbienceToFog(Vector4f fogColor) {
        float saturation;
        if (fogColor == null) {
            return null;
        }
        Ambience ambience = Ambience.getInstance();
        FogBlur fogBlur = FogBlur.getInstance();
        boolean ambienceOn = ambience != null && ambience.isEnabled();
        MinecraftClient mc = MinecraftClient.getInstance();
        Camera camera = mc.gameRenderer != null ? mc.gameRenderer.getCamera() : null;
        boolean fogBlurColor = fogBlur != null && fogBlur.hasCustomFogColor(camera);
        if (!ambienceOn && !fogBlurColor) {
            return fogColor;
        }
        float r = fogColor.x;
        float g = fogColor.y;
        float b = fogColor.z;
        boolean changed = false;
        if (fogBlurColor) {
            int customColor = fogBlur.getCustomFogColor();
            float a = (float)(customColor >> 24 & 0xFF) / 255.0f;
            float cr = (float)(customColor >> 16 & 0xFF) / 255.0f;
            float cg = (float)(customColor >> 8 & 0xFF) / 255.0f;
            float cb = (float)(customColor & 0xFF) / 255.0f;
            r = MathHelper.lerp(a, r, cr);
            g = MathHelper.lerp(a, g, cg);
            b = MathHelper.lerp(a, b, cb);
            changed = true;
        }
        if (Float.isFinite(saturation = KillEffect.getWorldSaturationMultiplier() * (ambienceOn ? ambience.getSaturationFactor() : 1.0f)) && Math.abs(saturation - 1.0f) > 5.0E-4f) {
            float lum = r * 0.2126f + g * 0.7152f + b * 0.0722f;
            r = MathHelper.clamp((float)(lum + (r - lum) * saturation), (float)0.0f, (float)1.0f);
            g = MathHelper.clamp((float)(lum + (g - lum) * saturation), (float)0.0f, (float)1.0f);
            b = MathHelper.clamp((float)(lum + (b - lum) * saturation), (float)0.0f, (float)1.0f);
            changed = true;
        }
        return changed ? new Vector4f(r, g, b, fogColor.w) : fogColor;
    }
}
