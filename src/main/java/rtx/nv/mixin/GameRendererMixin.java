package rtx.nv.mixin;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.ProjectionMatrix3;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rtx.nv.api.modules.impl.Utils.CameraSettings;
import rtx.nv.api.modules.impl.Visuals.Ambience;
import rtx.nv.api.modules.impl.Visuals.AspectRatio;
import rtx.nv.api.modules.impl.Visuals.KillEffect;
import rtx.nv.api.modules.impl.Visuals.NoRender;
import rtx.nv.api.ui.UI;
import rtx.nv.api.ui.theme.ClientAccent;
import rtx.nv.utils.FreezeProfiler;
import rtx.nv.utils.render.others.RenderCompatibility;
import rtx.nv.utils.render.post.guilayerblur.GuiCapture;
import rtx.nv.utils.render.post.guilayerblur.GuiLayerBlurRenderer;
import rtx.nv.utils.render.post.guimotionblur.GuiMotionBlurRenderer;
import rtx.nv.utils.render.post.hpfocus.HPFocusRenderer;
import rtx.nv.utils.render.post.itemoutline.ItemOutlineRenderer;
import rtx.nv.utils.render.post.saturation.Saturation2D;
import rtx.nv.utils.render.render2d.ClientPalette;

@Mixin(net.minecraft.client.render.GameRenderer.class)

public abstract class GameRendererMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method="render", at={@At(value="HEAD")}, require = 0)
    private void nv_primeRenderCompatibility(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        FreezeProfiler.markFrame();
        ClientAccent.beginFrame();
        ClientPalette.update();
        RenderCompatibility.primeFromCurrentContext();
    }

    @Inject(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.BEFORE)}, require = 0)
    private void nv_preGuiRender(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        if (this.client == null || this.client.player == null || this.client.world == null) {
            return;
        }
        this.nv_applyWorldSaturation();
        if (UI.motionBlurCapturePending()) {
            GuiMotionBlurRenderer.captureBackground(UI.motionBlurCaptureRadius());
        }
    }

    @Inject(method="render", at={@At(value="INVOKE", target="Lnet/minecraft/client/gui/render/GuiRenderer;method_70890(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift=At.Shift.AFTER)}, require = 0)
    private void nv_postGuiRender(RenderTickCounter deltaTracker, boolean tick, CallbackInfo ci) {
        if (GuiLayerBlurRenderer.captureActiveThisFrame()) {
            UI.dropPendingBlurs();
            if (GuiCapture.active()) {
                GuiLayerBlurRenderer.composite(GuiCapture.scale(), GuiCapture.blurRadius());
            }
        } else {
            UI.flushMotionBlur();
        }
        HPFocusRenderer.captureIfRequested();
    }

    @Inject(method="close", at={@At(value="RETURN")}, require = 0)
    private void nv_closeMotionBlur(CallbackInfo ci) {
        GuiMotionBlurRenderer.shutdown();
        GuiLayerBlurRenderer.shutdown();
        ItemOutlineRenderer.clear();
    }

    @ModifyReturnValue(method="getFov", at={@At(value="RETURN")}, require = 0)
    private float nv_killZoomFov(float original) {
        float scale = KillEffect.getKillZoomFovScale();
        return scale == 1.0f ? original : original * scale;
    }

    @ModifyReturnValue(method="getFov", at={@At(value="RETURN")}, require = 0)
    private float nv_cameraZoomFov(float original) {
        float scale = CameraSettings.getFovScale();
        return scale == 1.0f ? original : original * scale;
    }

    @ModifyReturnValue(method="getBasicProjectionMatrix", at={@At(value="RETURN")}, require = 0)
    private Matrix4f nv_aspectRatioProjection(Matrix4f original) {
        AspectRatio aspectRatio = AspectRatio.getInstance();
        if (aspectRatio == null || !aspectRatio.isEnabled() || original == null) {
            return original;
        }
        return AspectRatio.copyAdjusted((Matrix4fc)original);
    }

    @WrapOperation(method="renderWorld", at={@At(value="INVOKE", target="Lnet/minecraft/client/render/ProjectionMatrix3;method_71095(IIF)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;")}, require = 0)
    private GpuBufferSlice nv_handAspectRatio(ProjectionMatrix3 buffer, int width, int height, float fov, Operation<GpuBufferSlice> original) {
        int adjustedWidth = Math.max(1, Math.round(AspectRatio.resolveRatio(width, height)));
        return (GpuBufferSlice)original.call(new Object[]{buffer, adjustedWidth, height, Float.valueOf(fov)});
    }

    @ModifyReturnValue(method="getBasicProjectionMatrix", at={@At(value="RETURN")}, require = 0)
    private Matrix4f nv_killCameraShake(Matrix4f original) {
        if (original == null) {
            return original;
        }
        float shake = KillEffect.getKillShakeDegrees();
        if (shake == 0.0f) {
            return original;
        }
        return new Matrix4f((Matrix4fc)original).rotateZ((float)Math.toRadians(shake));
    }

    @Inject(method="tiltViewWhenHurt", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_noRenderCameraShake(MatrixStack stack, float tickDelta, CallbackInfo ci) {
        if (NoRender.isActive("\u0422\u0440\u044f\u0441\u043a\u0430 \u043a\u0430\u043c\u0435\u0440\u044b")) {
            ci.cancel();
        }
    }

    @Inject(method="bobView", at={@At(value="HEAD")}, cancellable=true, require = 0)
    private void nv_noRenderViewBobbing(MatrixStack stack, float tickDelta, CallbackInfo ci) {
        if (NoRender.isActive("\u041f\u043e\u043a\u0430\u0447\u0438\u0432\u0430\u043d\u0438\u0435 \u043a\u0430\u043c\u0435\u0440\u044b")) {
            ci.cancel();
        }
    }

    private void nv_applyWorldSaturation() {
        float saturation = KillEffect.getWorldSaturationMultiplier();
        Ambience ambience = Ambience.getInstance();
        if (ambience != null && ambience.isEnabled()) {
            saturation *= ambience.getSaturationFactor();
        }
        if (!Float.isFinite(saturation) || Math.abs(saturation - 1.0f) <= 5.0E-4f) {
            return;
        }
        Saturation2D.applyWithCopy((float)Math.clamp(saturation, 0.0f, 2.0f));
    }
}

