package rtx.nv.mixin;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.memory.ObjectAllocator;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.drags.DragSystem;
import rtx.nv.api.events.EventBus;
import rtx.nv.api.events.impl.render.WorldRenderEvent;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.api.modules.impl.Visuals.FogBlur;
import rtx.nv.api.modules.impl.Visuals.HitBubbles;
import rtx.nv.api.modules.impl.Visuals.JumpCircle;
import rtx.nv.api.modules.impl.Visuals.KillEffect;
import rtx.nv.api.ui.window.WorldGuiCloseAnimation;
import rtx.nv.utils.render.post.customsky.CustomSkyRenderer;
import rtx.nv.utils.render.post.fogblur.FogBlurRenderer;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.render2d.blur.BlurFramebuffer;
import rtx.nv.utils.render.wave.WindWaveRenderer;

@Mixin(net.minecraft.client.render.WorldRenderer.class)

public abstract class LevelRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;
    @Unique
    private static final Matrix4f nv_skyViewProj = new Matrix4f();

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void nv_captureFogBlurFallback(ObjectAllocator allocator, RenderTickCounter deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return;
        }
        this.applyAmbienceFog(fogColor);
        if (fogColor != null) {
            FogBlurRenderer.setFallbackColor(fogColor.x, fogColor.y, fogColor.z);
            BlurFramebuffer.setSkyFallbackColor(fogColor.x, fogColor.y, fogColor.z);
        }
        FogBlurRenderer.beginFrame();
        WindWaveRenderer.beginFrame();
    }

    @Inject(method="render", at={@At(value="RETURN")}, require = 0)
    private void nv_worldRenderEvent(ObjectAllocator allocator, RenderTickCounter deltaTracker, boolean renderBlockOutline, Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix, GpuBufferSlice fog, Vector4f fogColor, boolean renderSky, CallbackInfo ci) {
        if (this.client == null || this.client.world == null || this.client.player == null) {
            return;
        }
        MatrixStack stack = new MatrixStack();
        stack.multiplyPositionMatrix((Matrix4fc)new Matrix4f((Matrix4fc)positionMatrix));
        WorldGuiCloseAnimation.captureWorldMatrices(projectionMatrix, positionMatrix, camera.getCameraPos());
        GuiLayerBlurRenderer.snapshotWorldDepth();
        this.applyCustomSky(positionMatrix, projectionMatrix);
        this.applyFogBlur();
        WorldRenderEvent worldRenderEvent = new WorldRenderEvent(stack, deltaTracker.getTickProgress(true), camera, new Matrix4f((Matrix4fc)positionMatrix), new Matrix4f((Matrix4fc)projectionMatrix));
        EventBus.get().post(worldRenderEvent);
        this.applyHitBubbles(camera, positionMatrix, projectionMatrix);
        this.applyJumpCircleDistortion(camera, positionMatrix, projectionMatrix, frustumMatrix);
        DragSystem.get().applyDragDistortion(this.client.getFramebuffer());
    }

    private void applyCustomSky(Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        Ambience ambience = Ambience.getInstance();
        if (ambience == null || !ambience.isCustomSkyActive()) {
            if (CustomSkyRenderer.isAllocated()) {
                CustomSkyRenderer.clear();
            }
            return;
        }
        CustomSkyRenderer.beginFrame();
        nv_skyViewProj.set((Matrix4fc)projectionMatrix).mul((Matrix4fc)positionMatrix);
        float time = (float)((double)(System.currentTimeMillis() % 20000000L) / 1000.0);
        int color = ambience.skyColorRGB();
        int color2 = ambience.skyColor2RGB();
        CustomSkyRenderer.apply(this.client.getFramebuffer(), nv_skyViewProj, time, ambience.skyTypeIndex(), (float)(color >> 16 & 0xFF) / 255.0f, (float)(color >> 8 & 0xFF) / 255.0f, (float)(color & 0xFF) / 255.0f, (float)(color2 >> 16 & 0xFF) / 255.0f, (float)(color2 >> 8 & 0xFF) / 255.0f, (float)(color2 & 0xFF) / 255.0f, ambience.skyGradientMode(), ambience.skyBrightness(), ambience.isThemeTint(), ambience.skyDensity(), (float)ambience.skyLayers());
    }

    private void applyFogBlur() {
        FogBlur fogBlur = FogBlur.getInstance();
        if (fogBlur != null && fogBlur.isEnabled()) {
            fogBlur.onAfterTranslucent(this.client.getFramebuffer());
        }
    }

    private void applyHitBubbles(Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix) {
        HitBubbles hitBubbles = HitBubbles.getInstance();
        if (hitBubbles != null && hitBubbles.isEnabled()) {
            hitBubbles.onAfterWorld(this.client.getFramebuffer(), positionMatrix, projectionMatrix, camera);
        }
    }

    private void applyJumpCircleDistortion(Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix, Matrix4f frustumMatrix) {
        JumpCircle jumpCircle = JumpCircle.getInstance();
        if (jumpCircle != null && jumpCircle.isEnabled()) {
            jumpCircle.onAfterWorld(this.client.getFramebuffer(), positionMatrix, projectionMatrix, frustumMatrix, camera);
        }
    }

    private void applyAmbienceFog(Vector4f fogColor) {
        if (fogColor == null) {
            return;
        }
        Ambience ambience = Ambience.getInstance();
        float saturation = KillEffect.getWorldSaturationMultiplier();
        if (ambience != null && ambience.isEnabled()) {
            saturation *= ambience.getSaturationFactor();
        }
        if (!Float.isFinite(saturation) || Math.abs(saturation - 1.0f) <= 5.0E-4f) {
            return;
        }
        float r = fogColor.x;
        float g = fogColor.y;
        float b = fogColor.z;
        float luminance = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        fogColor.set(MathHelper.clamp((float)(luminance + (r - luminance) * saturation), (float)0.0f, (float)1.0f), MathHelper.clamp((float)(luminance + (g - luminance) * saturation), (float)0.0f, (float)1.0f), MathHelper.clamp((float)(luminance + (b - luminance) * saturation), (float)0.0f, (float)1.0f), fogColor.w);
    }
}

